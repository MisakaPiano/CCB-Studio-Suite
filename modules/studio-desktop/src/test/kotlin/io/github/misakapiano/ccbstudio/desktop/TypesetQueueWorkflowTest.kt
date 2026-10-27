@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.*
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetQueueWorkflowTest {
    @Test fun popupPositionFlipsAndClampsEvenInSmallWindow(){
        val anchor=IntRect.Zero
        assertEquals(IntOffset(32,22),QueuePopupPosition(Offset(250f,190f),18).calculatePosition(anchor,IntSize(300,240),LayoutDirection.Ltr,IntSize(200,150)))
        assertEquals(IntOffset(0,0),QueuePopupPosition(Offset(90f,90f),18).calculatePosition(anchor,IntSize(100,100),LayoutDirection.Ltr,IntSize(200,150)))
        assertEquals(IntOffset(30,30),QueuePopupPosition(Offset(12f,12f),18).calculatePosition(anchor,IntSize(400,400),LayoutDirection.Ltr,IntSize(200,150)))
    }
    @Test fun displayedNumberMoveRejectsInvalidAndStaleTargetsPreservesSelectionAndRestart()=runBlocking(awt){
        val root=root();val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(120,80);val asset=w.importImage("queue.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("numbered queue",List(6){asset});c.idle();c.editor.change{it.copy(startNumber=98,numberDigits=4)}
        val original=c.editor.document!!.pages.map{it.id};c.editor.selectedPages=setOf(original[1],original[3]);c.editor.pageId=original[3]
        val scene=scene(c);var expected=emptyList<String>()
        try {
            scene.frames();scene.click("移动到指定序号");scene.field("目标显示序号","NaN");assertFalse(scene.enabled("插入前面"))
            scene.field("目标显示序号","97");assertFalse(scene.enabled("插入后面"))
            scene.field("目标显示序号","102");scene.click("插入前面")
            expected=listOf(original[0],original[2],original[1],original[3],original[4],original[5]);assertEquals(expected,c.editor.document!!.pages.map{it.id})
            assertEquals(original[3],c.editor.pageId);assertEquals(setOf(original[1],original[3]),c.editor.selectedPages)
            c.editor.drain();scene.click("撤销编辑");c.idle();scene.frames();assertEquals(original,c.editor.document!!.pages.map{it.id})
            c.editor.selectedPages=setOf(original[1],original[3]);scene.frames();scene.click("移动到指定序号");scene.field("目标显示序号","101.5");scene.click("插入后面")
            expected=listOf(original[0],original[2],original[4],original[1],original[3],original[5]);assertEquals(expected,c.editor.document!!.pages.map{it.id})
            scene.click("移动到指定序号");scene.field("目标显示序号","98");c.editor.change{it.copy(startNumber=198)};scene.frames();assertFalse(scene.enabled("插入前面"))
            scene.shot("stale-number-target");scene.click("取消移动");assertEquals(expected,c.editor.document!!.pages.map{it.id})
            c.editor.change{it.copy(startNumber=98)};c.editor.drain()
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root).use{next->val d=TypesetRepository(next).load(next.state.typesetProjects.single());assertEquals(expected,d.pages.map{it.id});assertEquals(98,d.startNumber);assertContentEquals(bytes,next.bytes(asset))}
    }
    @Test fun longQueueDragAutoScrollsKeepsBlockAndHoverUsesEditedPageWithoutSelecting()=runBlocking(awt){
        val root=root();val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(120,80);val asset=w.importImage("long-queue.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("long queue",List(28){asset});c.idle();c.editor.queueView="list"
        c.editor.change{it.copy(thumbnailSize=48,pages=it.pages.mapIndexed{i,p->p.copy(name="Page ${i+1}",texts=listOf(TypesetText(text="edited ${i+1}",x=60.0,y=10.0,style=TypesetStyle(fontSize=16.0))))})}
        c.editor.drain();val original=c.editor.document!!.pages.map{it.id};val scene=scene(c);var primaryFailure:Throwable?=null;var lastPointer=Offset.Zero
        try {
            scene.frames();val selected=c.editor.selectedPages;val current=c.editor.pageId;val revision=c.editor.project!!.revision
            val hover=scene.node("队列页 2").boundsInRoot.center;scene.sendPointerEvent(PointerEventType.Move,hover);scene.frames()
            withTimeout(30000){while(scene.nodes().none{it.matches("队列处理后预览")}){scene.frames()}}
            assertEquals(selected,c.editor.selectedPages);assertEquals(current,c.editor.pageId);assertEquals(revision,c.editor.project!!.revision);scene.shot("processed-hover")
            scene.sendPointerEvent(PointerEventType.Move,Offset(900f,400f));scene.frames()
            suspend fun choose(n:Int,ctrl:Boolean=false){val p=scene.node("队列页 $n").boundsInRoot.center;val mods=PointerKeyboardModifiers(isCtrlPressed=ctrl)
                scene.sendPointerEvent(PointerEventType.Press,p,button=PointerButton.Primary,keyboardModifiers=mods);scene.sendPointerEvent(PointerEventType.Release,p,button=PointerButton.Primary,keyboardModifiers=mods);scene.frames()}
            choose(1);choose(2,true);assertEquals(setOf(original[0],original[1]),c.editor.selectedPages)
            val from=scene.node("队列页 2").boundsInRoot.center;val queue=scene.node("排版页面队列").boundsInRoot;val edge=Offset(from.x,queue.bottom-4);lastPointer=edge
            scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
            for(i in 1..8){scene.sendPointerEvent(PointerEventType.Move,from+(edge-from)*(i/8f),button=PointerButton.Primary);scene.frames()}
            // Semantics bounds are clipped: even a last-row sliver has a visible center.
            // Wait on the actual scroll range, not the clipped row's center or its presence.
            try{withTimeout(15000){while(true){val range=scene.node("排版页面队列").config[SemanticsProperties.VerticalScrollAxisRange];if(range.value()>=range.maxValue())break;scene.frames()}}}
            catch(t:Throwable){val range=scene.node("排版页面队列").config[SemanticsProperties.VerticalScrollAxisRange];println("Queue scroll at failure: value=${range.value()} max=${range.maxValue()} edge=$edge rows="+scene.nodes().mapNotNull{it.config.getOrNull(SemanticsProperties.ContentDescription)?.filter{it.startsWith("队列页 ")}});throw t}
            assertEquals(original,c.editor.document!!.pages.map{it.id},"holding drag must not commit partial orders")
            scene.sendPointerEvent(PointerEventType.Release,edge,button=PointerButton.Primary);scene.frames()
            assertEquals(original.drop(2)+original.take(2),c.editor.document!!.pages.map{it.id})
            c.editor.drain();scene.shot("long-queue-drop");scene.click("撤销编辑");c.idle();scene.frames();assertEquals(original,c.editor.document!!.pages.map{it.id})
            assertContentEquals(bytes,w.bytes(asset))
        }catch(t:Throwable){primaryFailure=t;throw t}finally{
            try{scene.sendPointerEvent(PointerEventType.Release,lastPointer,button=PointerButton.Primary);scene.frames();scene.close()}
            catch(t:Throwable){if(primaryFailure!=null)primaryFailure.addSuppressed(t)else throw t}
            finally{c.close();scope.cancel();w.close()}
        }
    }
    @Test fun dividerResizeCommitsOnceAndContextMoveTargetsClickedPage()=runBlocking(awt){
        val root=root();val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(120,80);val asset=w.importImage("resize.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("resize queue",List(4){asset});c.idle();c.editor.drain();val original=c.editor.document!!.pages.map{it.id};val revision=c.editor.project!!.revision
        val scene=scene(c)
        try{
            scene.frames();val start=scene.node("调整队列栏宽度").boundsInRoot.center;val end=start+Offset(100f,0f)
            scene.sendPointerEvent(PointerEventType.Press,start,button=PointerButton.Primary)
            for(i in 1..8){scene.sendPointerEvent(PointerEventType.Move,start+(end-start)*(i/8f),button=PointerButton.Primary);scene.frames()}
            assertEquals(revision,c.editor.project!!.revision);assertEquals(220,c.editor.document!!.queueWidth)
            scene.sendPointerEvent(PointerEventType.Release,end,button=PointerButton.Primary);scene.frames();assertTrue(c.editor.document!!.queueWidth in 300..325)
            c.editor.drain();assertEquals(revision+1,c.editor.project!!.revision)
            c.editor.selectedPages=setOf(original[0],original[1]);scene.frames()
            val p=scene.node("队列页 2").boundsInRoot.center
            scene.sendPointerEvent(PointerEventType.Press,p,button=PointerButton.Secondary);scene.sendPointerEvent(PointerEventType.Release,p,button=PointerButton.Secondary);scene.frames()
            scene.click("移动此页到指定序号");scene.field("目标显示序号","4");scene.click("插入后面")
            assertEquals(listOf(original[0],original[2],original[3],original[1]),c.editor.document!!.pages.map{it.id});c.editor.drain();scene.shot("resized-queue")
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root).use{next->val d=TypesetRepository(next).load(next.state.typesetProjects.single());assertTrue(d.queueWidth in 300..325);assertEquals(original[1],d.pages.last().id);assertContentEquals(bytes,next.bytes(asset))}
    }
    @Test fun gridAndListSwitchAndIndependentPreviewKeepEditorState()=runBlocking(awt){
        val root=root();val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(160,100);val asset=w.importImage("views.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("views",List(12){asset});c.idle();c.editor.drain()
        val ids=c.editor.document!!.pages.map{it.id};c.editor.selectedPages=setOf(ids[0],ids[1]);val original=c.editor.document!!;val revision=c.editor.project!!.revision
        val scene=scene(c)
        try{
            scene.frames();assertTrue(scene.node("队列页 2").boundsInRoot.left>scene.node("队列页 1").boundsInRoot.left)
            scene.click("缩略图与条目视图");assertEquals("list",c.editor.queueView)
            assertTrue(scene.node("队列页 2").boundsInRoot.top>scene.node("队列页 1").boundsInRoot.top)
            assertEquals(setOf(ids[0],ids[1]),c.editor.selectedPages);assertEquals(ids[0],c.editor.pageId)
            scene.click("预览队列页 2");assertEquals(ids[0],c.editor.pageId)
            withTimeout(15000){while(scene.nodes().none{it.matches("完整页面预览")})scene.frames()}
            scene.click("下一页 · →");scene.click("关闭预览窗口")
            assertEquals(ids[0],c.editor.pageId);assertEquals(setOf(ids[0],ids[1]),c.editor.selectedPages)
            assertEquals(original,c.editor.document);assertEquals(revision,c.editor.project!!.revision)
            scene.click("网格视图");assertEquals("grid",c.editor.queueView);scene.shot("native-grid-and-groups")
            withTimeout(15000){while(!scene.enabled("添加文字"))scene.frames()}
            scene.click("添加文字");scene.field("文字内容","竖排，AB！");scene.click("竖排");c.editor.drain()
            assertTrue(c.editor.text!!.vertical);assertEquals("竖排，AB！",c.editor.text!!.text)
            val before=c.editor.document;val reset=c.editor.viewportReset;c.editor.zoom=2f
            scene.click("适应画布");assertEquals(1f,c.editor.zoom);assertEquals(reset+1,c.editor.viewportReset);assertEquals(before,c.editor.document)
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root).use{next->assertContentEquals(bytes,next.bytes(asset))}
    }
    @Test fun gridDragCancelsWithEscapeThenCommitsStableBlock()=runBlocking(awt){
        val root=root();val w=StudioWorkspace.openStudio(root);val asset=w.importImage("grid-drag.png",EditorRenderer.synthetic(120,80))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("grid drag",List(8){asset});c.idle();c.editor.drain();val ids=c.editor.document!!.pages.map{it.id}
        c.editor.selectedPages=setOf(ids[0],ids[1]);val scene=scene(c)
        try{
            scene.frames()
            suspend fun begin(){val from=scene.node("队列页 2").boundsInRoot.center;val target=scene.node("队列页 4").boundsInRoot
                val to=Offset(target.right-4,target.center.y)
                scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
                for(i in 1..8){scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*(i/8f),button=PointerButton.Primary);scene.frames()}
            }
            begin();assertEquals(ids,c.editor.document!!.pages.map{it.id})
            scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyUp));scene.frames()
            scene.sendPointerEvent(PointerEventType.Release,scene.node("队列页 4").boundsInRoot.center,button=PointerButton.Primary);scene.frames()
            assertEquals(ids,c.editor.document!!.pages.map{it.id})
            begin();val target=scene.node("队列页 4").boundsInRoot
            scene.sendPointerEvent(PointerEventType.Release,Offset(target.right-4,target.center.y),button=PointerButton.Primary);scene.frames()
            assertEquals(listOf(ids[2],ids[3],ids[0],ids[1])+ids.drop(4),c.editor.document!!.pages.map{it.id})
            assertEquals(setOf(ids[0],ids[1]),c.editor.selectedPages);c.editor.drain();scene.shot("native-grid-drop")
            scene.click("撤销编辑");c.idle();scene.frames();assertEquals(ids,c.editor.document!!.pages.map{it.id})
        }finally{scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun namedMaskPresetUiPreservesObjectsAndNewDefaultIsSeparate()=runBlocking(awt){
        val root=root();val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(140,100);val asset=w.importImage("presets.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("named masks",listOf(asset));c.idle()
        val mask=TypesetMask(x=12.0,y=14.0,width=40.0,height=30.0,strength=10)
        c.editor.pageChange{it.copy(masks=listOf(mask))};c.editor.objectId=mask.id;c.editor.drain()
        val scene=scene(c)
        try{
            scene.frames();scene.click("预设");scene.click("打码配置");scene.field("预设名称","柔和");scene.click("保存新预设");c.idle();scene.frames()
            val id=c.editor.editorExtras.namedMasks.single().id;assertEquals(10,c.editor.editorExtras.namedMasks.single().settings.strength)
            c.editor.updateMaskSettings{it.copy(effect="blur",strength=24)};c.editor.drain();scene.frames()
            scene.click("应用到当前对象");c.idle();scene.frames()
            assertEquals("mosaic",c.editor.mask!!.effect);assertEquals(10,c.editor.mask!!.strength)
            assertEquals(mask.x,c.editor.mask!!.x);assertEquals(mask.width,c.editor.mask!!.width)
            scene.field("预设名称","改名");scene.click("重命名预设");c.idle();scene.frames();assertEquals(id,c.editor.editorExtras.namedMasks.single().id)
            assertEquals("改名",c.editor.editorExtras.namedMasks.single().name)
            c.editor.updateMaskSettings{it.copy(effect="blur",strength=32)};c.editor.drain();scene.frames()
            scene.click("覆盖所选预设");scene.click("确认预设变更");c.idle();scene.frames()
            assertEquals("blur",c.editor.editorExtras.namedMasks.single().settings.effect)
            c.editor.updateMaskSettings{it.copy(effect="white")};c.editor.drain();scene.frames()
            val existing=c.editor.page!!;scene.click("设为新建默认");c.idle();scene.frames()
            assertEquals(existing,c.editor.page);assertEquals("blur",c.editor.maskDefault.effect);assertEquals("white",c.editor.mask!!.effect)
            scene.click("删除预设");scene.click("确认预设变更");c.idle();scene.frames()
            assertTrue(c.editor.editorExtras.namedMasks.isEmpty());assertEquals(existing,c.editor.page)
            c.editor.drain();scene.shot("native-named-presets")
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root).use{next->assertTrue(TypesetEditorExtrasStore.load(next).namedMasks.isEmpty());assertEquals("blur",next.state.typesetPreferences.batchToolDefault!!.effect);assertContentEquals(bytes,next.bytes(asset))}
    }
    @Test fun categoryExportUiKeepsAllPagesOrSelectedRangeAndPersistsIndependentChoices()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(160,100);val asset=w.importImage("export-ui.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("export UI",List(3){asset});c.idle();c.editor.drain()
        val ids=c.editor.document!!.pages.map{it.id};c.editor.selectedPages=setOf(ids[0],ids[2]);val parent=Files.createDirectories(root());c.editor.chooseExportDirectory={parent}
        val scene=scene(c)
        suspend fun open(){scene.frames();scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")})scene.frames()}}
        try{
            open();scene.click("02_仅打码");scene.click("04_仅嵌字");scene.click("另存原图副本（原字节）")
            scene.click("导出范围 · 全部页 ▾");scene.click("所选页");scene.field("图片文件名前缀（可留空）","测试")
            scene.shot("native-export-categories");scene.click("选择位置并导出");c.idle();scene.frames()
            val out=c.editor.exportDirectory!!;val receipt=TypesetExport.verify(out);assertEquals(4,receipt.version)
            assertEquals(listOf(ids[0],ids[2]),receipt.pageIds);assertEquals(10,receipt.files.size)
            assertEquals(listOf("plain","mask","all","text","original"),receipt.variants)
            assertContentEquals(bytes,Files.readAllBytes(out.resolve("原图副本/测试_001.png")))
            open();scene.click("01_原始排序（按所选格式编码）");scene.click("02_仅打码");scene.click("03_打码并嵌字");scene.click("04_仅嵌字")
            scene.click("选择位置并导出");c.idle();scene.frames()
            val only=TypesetExport.verify(c.editor.exportDirectory!!);assertEquals(listOf("original"),only.variants);assertEquals(3,only.pageIds.size)
            assertFalse(c.editor.editorExtras.originalSorted);assertEquals(listOf("original"),w.state.typesetPreferences.exportVariants)
            assertEquals(3,c.editor.document!!.pages.size);assertContentEquals(bytes,w.bytes(asset))
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r).use{next->assertFalse(TypesetEditorExtrasStore.load(next).originalSorted);assertEquals(listOf("original"),next.state.typesetPreferences.exportVariants)}
    }
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private fun scene(c:WorkbenchController)=ImageComposeScene(1320,1000,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
    private suspend fun ImageComposeScene.frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(180);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private fun ImageComposeScene.node(s:String)=nodes().first{it.matches(s)}
    private fun ImageComposeScene.enabled(s:String)=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config.getOrNull(SemanticsProperties.Disabled)==null
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(s:String,v:String){val n=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(it.matches(s)||it.children.any{it.matches(s)})};assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(v)));frames()}
    private suspend fun WorkbenchController.idle(){withTimeout(30000){active?.join()};assertNull(problem,problem)}
    private fun ImageComposeScene.shot(name:String){val p=Path.of(System.getProperty("preview.test.root")).resolve("typeset-queue/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}
}
