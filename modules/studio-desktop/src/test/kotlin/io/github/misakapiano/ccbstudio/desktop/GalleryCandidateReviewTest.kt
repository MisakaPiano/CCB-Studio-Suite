@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.runtime.snapshotFlow
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

/** Component checks only: no Windows input injection or native screen capture. */
class GalleryCandidateReviewTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun controller(w:StudioWorkspace,s:CoroutineScope,pause:Boolean)=WorkbenchController(w,s,DesktopNovelAiInfrastructure(root()),root(),pauseCollectionsOnStart=pause,admissionOverride=RequestAdmission{error("No provider calls")})
    private suspend fun ImageComposeScene.frames(){repeat(5){render(System.nanoTime()).close();yield()};delay(150);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private val evidence=Path.of("H:/CCB-Studio-Suite/.local/gallery-review-candidate-20261101/evidence/components")
    private fun ImageComposeScene.shot(name:String){val p=evidence.resolve(name+".png");Files.createDirectories(p.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}
    @Test fun galleryTilesAdvancedFiltersHelpAndExplicitEditorRangeAreReachable()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,true)
        val originals=(1..3).map{w.importImage("synthetic-$it.png",EditorRenderer.synthetic(64+it,80))}
        c.refreshLibraryAssets();c.library.refresh();c.editor.create("Synthetic UI",originals);c.active?.join();assertNull(c.problem)
        c.selection=originals.toSet();c.current=originals.first();c.view="图库"
        val scene=ImageComposeScene(1320,880){StudioTheme{Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();assertTrue(scene.nodes().any{it.matches("高级筛选与标签")})
            val tile=scene.nodes().first{it.matches("所选资产移入回收站")};val selection=scene.nodes().first{it.matches("清空选择")}
            assertTrue(tile.boundsInRoot.top>=selection.boundsInRoot.bottom,"Archive action must be below selection controls")
            scene.shot("gallery-default")
            scene.click("高级筛选与标签");assertTrue(scene.nodes().any{it.matches("收起高级筛选")});scene.shot("gallery-expanded")
            scene.click("收起高级筛选");scene.click("当前页面使用说明");assertTrue(scene.nodes().any{it.matches("图库 · 使用说明")});scene.shot("gallery-help");scene.click("知道了")
            c.view="编辑";scene.frames();c.active?.join();scene.frames()
            assertTrue(scene.nodes().any{it.matches("自动打码·当前图")});assertTrue(scene.nodes().any{it.matches("自动打码·所选 1 张")})
            c.editor.selectedPages=c.editor.document!!.pages.map{it.id}.toSet();scene.frames();assertTrue(scene.nodes().any{it.matches("自动打码·所选 3 张")});scene.shot("editor-explicit-range")
            scene.click("当前页面使用说明");assertTrue(scene.nodes().any{it.matches("编辑 · 使用说明")});scene.click("知道了")
            for(id in originals)assertTrue(w.bytes(id).isNotEmpty());assertTrue(w.state.accounts.isEmpty())
        } finally {scene.close();c.close();scope.cancel()}
    }
    @Test fun actualSynchronizationStaysIdleAcrossPeriodicBoundaryThenImportsAChange():Unit=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val folder=Files.createDirectories(root());val original=EditorRenderer.synthetic(48,64);val path=Files.write(folder.resolve("synthetic.png"),original)
        val repo=LibraryRepository(w);val registration=repo.addFolder(folder,"images",false).folders.single();val id=repo.scan(registration.id).single().assetId!!
        val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,false);var starts=0
        // Initialize the lazy controller before entering snapshotFlow's read-only snapshot.
        val library=c.library;assertNull(library.error)
        val observer=scope.launch(start=CoroutineStart.UNDISPATCHED){snapshotFlow{library.collectionBusy}.collect{if(it)starts++}}
        val scene=ImageComposeScene(1000,720){MaterialTheme{LibrarySynchronization(c)}}
        try {
            withTimeout(30000){while(starts==0||c.library.collectionBusy){scene.frames();delay(100)}}
            val baseline=starts;val until=System.nanoTime()+66_000_000_000L
            while(System.nanoTime()<until){delay(500);scene.render(System.nanoTime()).close()}
            assertEquals(baseline,starts,"Idle metadata reconciliation must not start a content scan")
            val idleStarts=starts-baseline
            assertContentEquals(original,w.bytes(id));assertContentEquals(original,Files.readAllBytes(path))
            Files.write(path,EditorRenderer.synthetic(49,65))
            withTimeout(30000){while(starts==baseline||c.library.collectionBusy||SharedAssets.images(w).size<2){scene.frames();delay(100)}}
            assertContentEquals(original,w.bytes(id));assertEquals(2,SharedAssets.images(w).size)
            val out=evidence.resolve("IDLE_SCAN.json");Files.createDirectories(out.parent)
            Files.writeString(out,"{\"idleSeconds\":66,\"idleScanStarts\":$idleStarts,\"baselineStarts\":$baseline,\"afterChangeStarts\":$starts,\"originalPreserved\":true}")
            Unit
        } finally {observer.cancelAndJoin();scene.close();c.close();scope.cancel()}
    }
}
