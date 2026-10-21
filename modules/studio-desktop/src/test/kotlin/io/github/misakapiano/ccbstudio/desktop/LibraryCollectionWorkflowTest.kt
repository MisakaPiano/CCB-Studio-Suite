@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class LibraryCollectionWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun unreadableLocalRegistrationStopsMonitoringWithoutHidingAssetLabels():Unit=runBlocking(awt){
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        val repo=c.library.repository;val image=w.importImage("retained.png",EditorRenderer.synthetic(32,32))
        repo.label(image,"保留别名",listOf("收藏"));repo.addFolder(Files.createDirectories(root()),"images",false)
        val marker=w.root.resolve("exports/.local-bindings/collections.id");Files.writeString(marker,"damaged-local-registration")
        c.library.refresh();c.library.tab="目录入口";c.view="资产管理"
        val scene=ImageComposeScene(1200,850){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            repeat(5){scene.render().close();yield();delay(40)}
            assertNull(c.library.error);assertEquals(AssetLabels("保留别名",listOf("收藏")),c.library.labels(image))
            assertTrue(c.library.automaticNotice.contains("登记不可读取"));assertEquals("damaged-local-registration",Files.readString(marker))
            assertEquals(1,SharedAssets.images(w).size);assertFails{repo.automaticFolders()}
        }finally{scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun queuedRemovedRegistrationIsIgnoredAndUncertainMetadataStopsAutomaticRetries()=runBlocking(awt){
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        try {
            val folder=Files.createDirectories(root());Files.write(folder.resolve("must-not-import.png"),EditorRenderer.synthetic(32,32))
            val repo=c.library.repository;val id=repo.addFolder(folder,"images",false).folders.single().id;repo.removeFolder(id)
            assertTrue(c.library.synchronize(setOf(id),scope))
            withTimeout(5000){while(c.library.automaticNotice.isEmpty())delay(20)}
            assertFalse(c.library.collectionBlocked);assertTrue(SharedAssets.images(w).isEmpty())
            val next=repo.addFolder(folder,"images",false).folders.single().id
            val header="{\"version\":2,\"sequence\":999,\"previous\":null}".toByteArray()
            w.assets.put(io.github.misakapiano.ccbstudio.assets.AssetStore.Request(java.util.UUID.randomUUID(),"future.json",io.github.misakapiano.ccbstudio.assets.AssetStore.Kind.ORIGINAL,null,header,LibraryRepository.TAG.toByteArray()),java.io.ByteArrayInputStream("{}".toByteArray()))
            assertTrue(c.library.synchronize(setOf(next),scope))
            withTimeout(5000){while(!c.library.collectionBlocked)delay(20)}
            val count=w.catalog().size;assertFalse(c.library.synchronize(setOf(next),scope));assertEquals(count,w.catalog().size);assertTrue(SharedAssets.images(w).isEmpty())
        }finally{c.close();scope.cancel();w.close()}
    }
    @Test fun workbenchAutomaticallyCollectsImagesAndPromptVersionsAndDrainsForMaintenance()=runBlocking(awt){
        val data=root();val folder=Files.createDirectories(root());val prompts=Files.createDirectories(root())
        val w=StudioWorkspace.openStudio(data);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        val repo=c.library.repository;repo.addFolder(folder,"images",false);repo.addFolder(prompts,"prompts",false);c.library.refresh();c.view="图库"
        val before=c.draft;val scene=ImageComposeScene(1200,850){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(2){scene.render().close();yield()};delay(100)}
        suspend fun until(check:()->Boolean){
            try{withTimeout(20000){while(!check())frames()};frames()}
            catch(e:TimeoutCancellationException){throw AssertionError("Collection condition timed out: assets=${c.assets.size}, prompts=${c.library.prompts.size}, blocked=${c.library.collectionBlocked}, busy=${c.busy}, maintenance=${c.maintenance}, notice=${c.library.automaticNotice}, error=${c.library.error}, observations=${c.library.state?.observations}, eligible=${runCatching{repo.automaticFolders().map{it.id}}}",e)}
        }
        var first="";var firstPrompt=""
        try {
            frames();val bytes=EditorRenderer.synthetic(80,80);val file=Files.write(folder.resolve("automatic.png"),bytes)
            Files.writeString(prompts.resolve("standalone.prompt"),"first independent prompt")
            until{c.assets.size==1 && c.library.prompts.size==1};assertFalse(c.busy)
            first=c.assets.single().request().id().toString();firstPrompt=c.library.prompts.single().request().id().toString()
            repo.label(firstPrompt,"保留的别名",listOf("收藏"));c.library.refresh()
            Files.writeString(prompts.resolve("standalone.prompt"),"second independent prompt")
            Files.write(file,EditorRenderer.synthetic(88,88))
            until{c.assets.size==2 && c.library.prompts.size==2}
            val next=c.library.prompts.single{it.request().id().toString()!=firstPrompt}.request().id().toString()
            assertEquals(AssetLabels("保留的别名",listOf("收藏")),repo.load().labels[next]);assertEquals("first independent prompt",repo.prompt(firstPrompt).text)
            assertContentEquals(bytes,w.bytes(first));assertEquals(before,c.draft)
            c.enterMaintenance();Files.write(folder.resolve("during-maintenance.png"),bytes)
            repeat(16){frames()};assertEquals(2,SharedAssets.images(w).size);assertFails{StudioWorkspace.openStudio(data)}
            c.resumeAfterPreflightFailure();until{c.assets.size==3};assertEquals(before,c.draft)
            val shot=Path.of("build/library-collections/auto-collected.png");Files.createDirectories(shot.parent);scene.render().use{it.encodeToData()!!.use{Files.write(shot,it.bytes)}}
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(data).use{r->assertEquals(3,SharedAssets.images(r).size);assertEquals("first independent prompt",LibraryRepository(r).prompt(firstPrompt).text);assertTrue(r.bytes(first).isNotEmpty())}
    }
}
