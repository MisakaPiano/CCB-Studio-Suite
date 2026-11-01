package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GallerySingleOriginalTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun actualDirectoryIntakeSharesOnePayloadAndKeepsNestedAlbumReferencesAfterReopen()=runBlocking(awt) {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val input=Files.createDirectories(root.resolve("source"));val raw=EditorRenderer.synthetic(36,40)
        val sources=listOf(input.resolve("a.png"),Files.createDirectories(input.resolve("A")).resolve("same.png"),Files.createDirectories(input.resolve("B")).resolve("same.png"));sources.forEach{Files.write(it,raw)}
        val w=StudioWorkspace.openStudio(root.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        var ids=emptySet<String>()
        try {
            c.gallery.chooseImportDirectory={input};c.importImageDirectory(true);c.active!!.join();assertNull(c.problem)
            ids=c.gallery.imports.mapNotNull{it.assetId}.toSet();assertEquals(3,ids.size);assertTrue(c.gallery.imports.all{it.status=="IMPORTED_ORIGINAL"})
            val albums=c.library.repository.load().virtualAlbums;assertEquals(ids,VirtualAlbums.members(albums,albums.single{it.name=="source"}.id))
            assertFalse(Files.exists(root.resolve("workspace-collections/imports")));assertTrue(c.library.repository.load().observations.isEmpty())
            assertTrue(Files.list(w.assets.root().resolve("transactions")).use{dirs->dirs.noneMatch{Files.exists(it.resolve("payload.part"))}})
            assertTrue(Files.isRegularFile(w.assets.root().resolve("objects/${sha256(raw)}.blob")));ids.forEach{assertContentEquals(raw,w.bytes(it))}
        }finally{c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root.resolve("workspace")).use{again->val albums=LibraryRepository(again).load().virtualAlbums;assertEquals(ids,VirtualAlbums.members(albums,albums.single{it.name=="source"}.id));ids.forEach{assertContentEquals(raw,again.bytes(it))}}
        sources.forEach{assertContentEquals(raw,Files.readAllBytes(it))}
    }
}
