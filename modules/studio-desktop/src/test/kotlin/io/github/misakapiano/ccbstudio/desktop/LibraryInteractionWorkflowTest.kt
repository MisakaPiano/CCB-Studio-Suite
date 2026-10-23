@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class LibraryInteractionWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(3){render(System.nanoTime()).close();yield()};delay(80);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}}
    private fun SemanticsNode.matches(s:String):Boolean=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private fun ImageComposeScene.node(s:String)=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
    private suspend fun ImageComposeScene.click(s:String){assertTrue(node(s).config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun WorkbenchController.idle(){active?.join();assertNull(problem,problem)}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/gallery-interaction/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun archiveSharesFiltersRestoresOnlyFixedFilteredIdsAndSupportsUndo()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(48,48)
        val a=w.importImage("birthday.png",bytes);val b=w.importImage("other.png",bytes);val repo=LibraryRepository(w)
        val prompt=repo.savePrompt("synthetic words","words","manual");repo.label(a,"生日图",listOf("生日"));repo.label(b,"其他",listOf("保留"));AssetArchive(w).change(setOf(a,b,prompt),true)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        c.view="资产管理";c.library.tab="回收站"
        val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            withTimeout(10000){while(scene.nodes().none{it.matches("回收站资产 $a")})scene.frames()}
            assertFalse(c.library.archiveView.checkboxMode);assertTrue(scene.nodes().any{it.matches("返回上层相册")})
            c.library.archiveView.filterTags=listOf("生日");scene.frames()
            assertTrue(scene.nodes().any{it.matches("回收站资产 $a")});assertFalse(scene.nodes().any{it.matches("回收站资产 $b")})
            scene.shot("archive-filtered")
            scene.click("恢复当前筛选 1 项");scene.click("确认恢复");c.idle();scene.frames();assertEquals(setOf(b,prompt),c.library.archived)
            scene.click("撤销");c.idle();scene.frames();assertEquals(setOf(a,b,prompt),c.library.archived)
            scene.click("重做");c.idle();scene.frames();assertEquals(setOf(b,prompt),c.library.archived)
            c.library.archiveView.filterTags=emptyList();c.library.archiveView.assetType="prompt";scene.frames()
            assertTrue(scene.nodes().any{it.matches("回收站资产 $prompt")});assertFalse(scene.nodes().any{it.matches("回收站资产 $b")})
            scene.shot("archive-mixed-type")
            assertContentEquals(bytes,w.bytes(a));assertContentEquals(bytes,w.bytes(b));assertEquals("synthetic words",repo.prompt(prompt).text)
        }finally{scene.close();c.close();scope.cancel()}
    }
    @Test fun gallerySingleSelectTagAlbumsHistoryAndFixedDragPayload()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(60,60)
        val ids=(1..4).map{w.importImage("image$it.png",bytes)};val repo=LibraryRepository(w)
        repo.changeLabels(ids.associateWith<String,AssetLabels?>{null},ids.associateWith{AssetLabels(tags=listOf("生日"))})
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        c.view="图库";val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            withTimeout(10000){while(c.gallery.foldPreferences==null)scene.frames()}
            assertFalse(c.gallery.checkboxMode);assertFalse(scene.nodes().any{it.matches("选择图库图片 ${ids[0]}")})
            scene.click("图库图片 ${ids[0]}");assertEquals(setOf(ids[0]),c.selection);assertNull(c.gallery.viewing)
            var opened:String?=null;c.gallery.previewOverride={ordered,at->opened=ordered[at]}
            val doublePosition=scene.node("图库图片 ${ids[0]}").boundsInRoot.center
            repeat(2){scene.sendPointerEvent(PointerEventType.Press,doublePosition,button=PointerButton.Primary);scene.render(System.nanoTime()).close();yield();scene.sendPointerEvent(PointerEventType.Release,doublePosition,button=PointerButton.Primary);scene.render(System.nanoTime()).close();yield();delay(45)}
            scene.frames();assertEquals(ids[0],opened)
            c.selection=emptySet();scene.frames()
            val firstCard=scene.node("图库图片 ${ids.first()}").boundsInRoot
            val lastCard=scene.node("图库图片 ${ids.last()}").boundsInRoot
            val blank=lastCard.bottomRight+Offset(35f,60f);val end=firstCard.topLeft+Offset(2f,2f)
            scene.sendPointerEvent(PointerEventType.Press,blank,button=PointerButton.Primary)
            for(i in 1..8){scene.sendPointerEvent(PointerEventType.Move,blank+(end-blank)*(i/8f),button=PointerButton.Primary);scene.frames()}
            scene.sendPointerEvent(PointerEventType.Release,end,button=PointerButton.Primary);scene.frames();assertEquals(ids.toSet(),c.selection)
            val from=scene.node("图库图片 ${ids[0]}").boundsInRoot.center
            val to=scene.node("APNG伪装 / 还原").boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
            for(i in 1..10){scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*(i/10f),button=PointerButton.Primary);scene.frames()}
            scene.shot("gallery-drag-tools")
            scene.sendPointerEvent(PointerEventType.Release,to,button=PointerButton.Primary);scene.frames()
            assertEquals(ids.toSet(),c.animation.ids!!.toSet());assertEquals(4,c.animation.ids!!.size)
            c.animation.close();scene.frames()
            c.gallery.tagFoldDepths=setOf(0);c.gallery.foldPreferences=mapOf(0 to com.example.chatbar.domain.image.NovelAiHistoryFoldPreference(0,true));scene.frames()
            val album=scene.nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{v->v.startsWith("图库相册 ")}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
            album.config[SemanticsActions.OnClick].action!!.invoke();scene.frames();assertEquals("生日",c.gallery.scope.tag);assertEquals(1,c.gallery.depth)
            scene.click("后退");assertEquals(0,c.gallery.depth);scene.click("前进");assertEquals(1,c.gallery.depth);scene.click("返回上层相册");assertEquals(0,c.gallery.depth)
            scene.shot("gallery-compact-tags");ids.forEach{assertContentEquals(bytes,w.bytes(it))};assertTrue(c.state.jobs.isEmpty())
        }finally{scene.close();c.close();scope.cancel()}
    }
    @Test fun pickerKeepsMultiSelectionAcrossSearchAndDoesNotApplyUntilConfirm()=runBlocking(awt) {
        val index=LibraryTagIndex(listOf("a","b"),mapOf("a" to AssetLabels(tags=listOf("生日","生日派对")),"b" to AssetLabels(tags=listOf("旅行"))))
        var result=listOf<String>();var cancelled=false
        val scene=ImageComposeScene(620,600,coroutineContext=awt){MaterialTheme{LibraryTagPickerContent(index,listOf("旅行"),{cancelled=true}){result=it}}}
        try {
            scene.frames();val field=scene.nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null}
            field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("生"));scene.frames();scene.click("选择检索结果");assertTrue(result.isEmpty())
            field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("旅行"));scene.frames();scene.click("添加所选标签")
            assertEquals(listOf("生日","生日派对"),result);assertFalse(cancelled)
        }finally{scene.close()}
    }
    @Test fun manyLongTagChipsScrollToLatestAndRemainRemovable()=runBlocking(awt) {
        var tags by mutableStateOf((1..100).map{"很长的标签名称-$it-用于检查滚动和省略"})
        val scene=ImageComposeScene(450,100,coroutineContext=awt){MaterialTheme{LibraryTagChips(tags,tags.associateWith{1},{tags=tags-it},androidx.compose.ui.Modifier.fillMaxWidth())}}
        try {
            scene.frames();assertTrue(scene.nodes().any{it.matches("标签横向滚动条")})
            val last=tags.last();scene.click("移除标签 $last");assertEquals(99,tags.size);assertFalse(last in tags)
            scene.shot("long-tag-strip")
        }finally{scene.close()}
    }
    @Test fun cancellingOperationsDrainsCatalogWithoutFalseRecoveryAndAllowsNextWrite()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val id=w.importImage("kept.png",EditorRenderer.synthetic(32,32))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")})
        try {
            repeat(5){val started=CompletableDeferred<Unit>();c.run("synthetic cancel"){started.complete(Unit);awaitCancellation()};started.await();c.active!!.cancelAndJoin();assertFalse(c.busy);assertNull(c.problem);assertEquals(id,c.assets.single().request().id().toString())}
            c.library.changeLabels(setOf(id)){it.copy(tags=listOf("still writable"))};c.idle();assertEquals(listOf("still writable"),c.library.labels(id).tags)
        }finally{c.close();scope.cancel()}
    }
    @Test fun clipboardCopyAndCutUseFixedIdentitiesAndPreserveUnrelatedLabels()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(48,48);val id=w.importImage("kept.png",bytes)
        val repo=LibraryRepository(w);repo.label(id,"alias",listOf("旧相册","保留"))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")})
        var clipboard:java.awt.datatransfer.Transferable?=null;c.gallery.pasteContents={clipboard};c.gallery.copyFiles={clipboard=ExportClipboard.contents(it)};c.gallery.writeTransferable={clipboard=it}
        try {
            copyLibrarySelection(c,listOf(id));c.idle();val copied=clipboard!!.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as List<*>
            assertContentEquals(bytes,Files.readAllBytes((copied.single() as java.io.File).toPath()))
            copyLibrarySelection(c,listOf(id),"旧相册");assertEquals(listOf("旧相册","保留"),repo.load().labels.getValue(id).tags)
            c.gallery.levels=listOf(GalleryScope(tag="新相册"));pasteLibrarySelection(c);c.idle()
            assertEquals(listOf("保留","新相册"),repo.load().labels.getValue(id).tags);assertNull(c.gallery.clipboard)
            c.library.undo();c.idle();assertEquals(listOf("旧相册","保留"),repo.load().labels.getValue(id).tags)
            c.library.redo();c.idle();assertEquals(listOf("保留","新相册"),repo.load().labels.getValue(id).tags)
            copyLibrarySelection(c,listOf(id),"新相册");clipboard=java.awt.datatransfer.StringSelection("external text")
            pasteLibrarySelection(c);assertEquals(listOf("保留","新相册"),repo.load().labels.getValue(id).tags)
            assertContentEquals(bytes,w.bytes(id));assertEquals("alias",repo.load().labels.getValue(id).alias)
        }finally{c.close();scope.cancel()}
    }
    @Test fun queueCtrlShiftSelectionDefaultCheckboxOffAndUndoDoesNotDeleteOriginals()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(48,48);val ids=(1..5).map{w.importImage("$it.png",bytes)}
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")})
        try {
            val a=c.animation;a.open(ids);assertFalse(a.checkboxMode)
            a.selectQueueItem(ids[0]);a.selectQueueItem(ids[2],shift=true);assertEquals(ids.take(3).toSet(),a.queueSelection)
            a.selectQueueItem(ids[4],ctrl=true);assertEquals((ids.take(3)+ids[4]).toSet(),a.queueSelection)
            a.removeQueue();assertEquals(listOf(ids[3]),a.ids);a.undo();assertEquals(ids,a.ids);a.redo();assertEquals(listOf(ids[3]),a.ids)
            a.open(ids.take(2));assertFalse(a.canUndo);ids.forEach{assertContentEquals(bytes,w.bytes(it))}
        }finally{c.close();scope.cancel()}
    }
}
