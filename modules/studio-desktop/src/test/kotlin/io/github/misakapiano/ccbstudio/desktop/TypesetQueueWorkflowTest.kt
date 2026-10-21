@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
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
        c.editor.create("long queue",List(28){asset});c.idle()
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
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/typeset-queue/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}
}
