package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryDirectoryImportTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    @Test fun planningAccepts600AndMoreThan1000WithoutDroppingDirectoriesOrWeakeningMetadataLimits():Unit=runBlocking {
        val base=root();val input=Files.createDirectory(base.resolve("目录"))
        repeat(600){Files.write(input.resolve("%04d.PNG".format(it)),byteArrayOf())}
        assertEquals(600,planGalleryDirectory(input,false,LibraryState()).files.size)
        repeat(401){Files.write(input.resolve("more-$it.webp"),byteArrayOf())}
        Files.write(Files.createDirectories(input.resolve("A/子目录")).resolve("同名.png"),byteArrayOf())
        Files.write(Files.createDirectories(input.resolve("B/子目录")).resolve("同名.png"),byteArrayOf())
        Files.writeString(input.resolve("note.txt"),"synthetic ignored file")
        val plan=planGalleryDirectory(input,true,LibraryState())
        assertEquals(1003,plan.files.size);assertEquals(5,plan.folders.size)
        val twins=plan.folders.filter{it.name=="子目录"};assertEquals(2,twins.size);assertNotEquals(twins[0].parent,twins[1].parent)
        VirtualAlbums.validate(plan.folders)
        assertEquals(1001,planGalleryDirectory(input,false,LibraryState()).files.size)
        assertFailsWith<GalleryDirectoryImportProblem>{planGalleryDirectory(input,true,LibraryState(virtualAlbums=List(500){VirtualAlbum(name="existing-$it")}))}
        val long=Files.createDirectory(base.resolve("x".repeat(101)))
        assertFailsWith<GalleryDirectoryImportProblem>{planGalleryDirectory(long,true,LibraryState())}
        val task=async(start=CoroutineStart.LAZY){planGalleryDirectory(input,true,LibraryState())};task.cancel()
        assertFailsWith<CancellationException>{task.await()}
    }
    @Test fun chunkedDirectoryTreeRetainsAllReportsOriginalsAndDuplicateNamedHierarchyAfterRestart()=runBlocking(awt) {
        val base=root();val input=Files.createDirectory(base.resolve("树"));val bytes=EditorRenderer.synthetic(32,48)
        repeat(1001){Files.write(input.resolve("%04d.png".format(it)),if(it==0)bytes else byteArrayOf())}
        for(name in listOf("A","B"))Files.write(Files.createDirectory(input.resolve(name)).resolve("同名.png"),bytes)
        Files.createDirectory(input.resolve("空目录"))
        val w=StudioWorkspace.openStudio(base.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(base.resolve("runtime")),base.resolve("runtime/budget"),admissionOverride=RequestAdmission{error("No real calls")},pauseCollectionsOnStart=true)
        var ids=emptyList<String>()
        try {
            c.gallery.chooseImportDirectory={input};c.importImageDirectory(true);c.active!!.join()
            assertEquals(1003,c.gallery.imports.size)
            assertTrue(c.gallery.imports.none{it.status=="WAITING"})
            ids=c.gallery.imports.mapNotNull{it.assetId};assertEquals(3,ids.size)
            assertEquals(1000,c.gallery.imports.count{it.status.startsWith("FAILED")})
            assertTrue(c.problem!!.startsWith("部分文件"),c.problem)
            val albums=c.library.repository.load().virtualAlbums;val rootAlbum=albums.single{it.name=="树"}
            assertEquals(4,albums.size);assertEquals(ids.toSet(),VirtualAlbums.members(albums,rootAlbum.id))
            assertEquals(1,albums.single{it.name=="A"}.images.size);assertEquals(1,albums.single{it.name=="B"}.images.size)
            assertTrue(albums.single{it.name=="空目录"}.images.isEmpty())
            ids.forEach{assertContentEquals(bytes,w.bytes(it))}
            for(p in listOf(input.resolve("0000.png"),input.resolve("A/同名.png"),input.resolve("B/同名.png")))assertContentEquals(bytes,Files.readAllBytes(p))
            assertEquals(0,Files.size(input.resolve("0001.png")))
        }finally{c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(base.resolve("workspace")).use{again->
            val albums=LibraryRepository(again).load().virtualAlbums
            assertEquals(ids.toSet(),VirtualAlbums.members(albums,albums.single{it.name=="树"}.id))
            ids.forEach{assertContentEquals(bytes,again.bytes(it))}
        }
    }
    @Test fun cancellationKeepsCommittedImagesInTheirImportedAlbumAndMarksUnstartedEntries()=runBlocking(awt) {
        val base=root();val input=Files.createDirectory(base.resolve("取消"));val bytes=EditorRenderer.synthetic(32,32)
        repeat(251){Files.write(input.resolve("$it.png"),bytes)}
        val w=StudioWorkspace.openStudio(base.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(base.resolve("runtime")),base.resolve("runtime/budget"),admissionOverride=RequestAdmission{error("No real calls")},pauseCollectionsOnStart=true)
        try {
            c.gallery.chooseImportDirectory={input};c.importImageDirectory(true)
            withTimeout(20000){while(c.gallery.imports.none{it.assetId!=null})delay(5)}
            c.active!!.cancelAndJoin()
            val committed=c.gallery.imports.mapNotNull{it.assetId}.toSet();assertTrue(committed.isNotEmpty())
            assertTrue(c.gallery.imports.any{it.status.startsWith("NOT_IMPORTED")});assertTrue(c.gallery.imports.none{it.status=="WAITING"})
            assertEquals(committed,VirtualAlbums.members(c.library.repository.load().virtualAlbums,c.library.repository.load().virtualAlbums.single().id))
            committed.forEach{assertContentEquals(bytes,w.bytes(it))}
            assertContentEquals(bytes,Files.readAllBytes(input.resolve("0.png")))
        }finally{c.close();scope.cancel();w.close()}
    }
}
