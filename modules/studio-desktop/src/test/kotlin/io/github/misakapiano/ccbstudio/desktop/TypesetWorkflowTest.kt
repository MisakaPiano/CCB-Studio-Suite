@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private suspend fun ImageComposeScene.frames(){repeat(8){render().close();yield()};delay(80);render().close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().firstOrNull{it.matches(s) && it.config.getOrNull(SemanticsActions.OnClick)!=null}?:error("Missing action: $s; available="+nodes().mapNotNull{it.config.getOrNull(SemanticsProperties.Text)?.joinToString{it.text}});assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(s:String,value:String){val n=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null && (it.matches(s)||it.children.any{it.matches(s)})};assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)));frames()}
    private suspend fun WorkbenchController.idle(){withTimeout(30000){active?.join()};assertNull(problem,problem)}
    private suspend fun ImageComposeScene.rendered(){withTimeout(30000){while(nodes().none{it.matches("排版编辑画布") && it.config.getOrNull(SemanticsProperties.StateDescription)=="已更新"}){delay(40);frames()}}}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/ux-r2a-workbench/$name.png");Files.createDirectories(p.parent);render().use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun namedProjectLiveVerticalEditingHistoryAndRestart()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(600,400);val id=w.importImage("fixture.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.selection=setOf(id);c.view="编辑"
        val scene=ImageComposeScene(1320,900){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("新建编辑工程");scene.field("工程名称","排版回归册");scene.click("建立工程");c.idle();scene.frames()
            assertEquals("排版回归册",c.editor.project!!.name)
            withTimeout(30000){while(scene.nodes().none{it.matches("添加文字")}){delay(100);scene.frames()}}
            scene.click("添加文字");scene.field("文字内容","「你好，世界！」[b]粗体[/]123ABC");scene.click("竖排")
            scene.field("竖排框高度","240")
            assertTrue(c.editor.text!!.vertical)
            scene.rendered();scene.shot("vertical-live");c.editor.drain();assertFalse(c.editor.dirty)
            val styleBeforePreset=c.editor.text!!
            scene.click("▸ 预设与默认");scene.field("预设名称","竖排字幕");scene.click("保存新文字预设");c.idle();scene.frames()
            assertEquals("",w.state.typesetPreferences.textPresets.single().text.text)
            scene.field("字号","18");c.editor.drain();scene.click("应用文字预设");c.idle();scene.frames()
            assertEquals(styleBeforePreset.style,c.editor.text!!.style);assertEquals(styleBeforePreset.text,c.editor.text!!.text);assertEquals(styleBeforePreset.x,c.editor.text!!.x)
            scene.click("▾ 预设与默认")
            val beforeMove=c.editor.text!!
            scene.click("锁定对象");scene.frames()
            suspend fun dragText(){
                val b=scene.nodes().first{it.matches("排版编辑画布")}.boundsInRoot
                val scale=minOf(b.width/600,b.height/400)
                val from=Offset(b.left+b.width/2,b.top+(b.height-400*scale)/2+(beforeMove.y.toFloat()+20)*scale)
                scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
                for(i in 1..4){scene.sendPointerEvent(PointerEventType.Move,from+Offset(i*15f,0f),button=PointerButton.Primary);scene.frames()}
                scene.sendPointerEvent(PointerEventType.Release,from+Offset(60f,0f),button=PointerButton.Primary);scene.frames();scene.rendered()
            }
            dragText();assertEquals(beforeMove.x,c.editor.page!!.texts.single().x)
            scene.click("文字 1 · 已锁定");scene.click("解锁对象");dragText()
            assertTrue(c.editor.page!!.texts.single().x>beforeMove.x)
            c.editor.drain()
            val text=c.editor.page!!.texts.single();assertEquals(text,TypesetRepository(w).load(w.state.typesetProjects.single()).pages.single().texts.single())
            scene.field("文字内容","即时改字");c.editor.drain();scene.click("撤销编辑");c.idle();scene.frames();assertEquals(text.text,c.editor.page!!.texts.single().text)
            scene.click("重做编辑");c.idle();scene.frames();assertEquals("即时改字",c.editor.page!!.texts.single().text)
            scene.constraints=Constraints.fixed(700,900);scene.frames();scene.shot("compact-canvas");scene.click("属性");scene.frames();scene.shot("compact-properties")
            assertEquals("即时改字",c.editor.page!!.texts.single().text);assertContentEquals(bytes,w.bytes(id))
            assertEquals(1,c.visibleAssets().size)
            scene.click("另存工程");scene.field("新工程名称","独立副本");scene.click("确认工程操作");c.idle();scene.frames()
            assertEquals(2,w.state.typesetProjects.size);assertEquals("独立副本",c.editor.project!!.name);assertTrue(c.editor.project!!.undo.isEmpty())
            scene.click("删除工程");scene.click("确认工程操作");c.idle();scene.frames();assertNull(c.editor.project)
            scene.click("继续最近工程");c.idle();scene.frames();assertEquals("排版回归册",c.editor.project!!.name)

        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->val p=again.state.typesetProjects.single();assertEquals("排版回归册",p.name);val t=TypesetRepository(again).load(p).pages.single().texts.single();assertEquals("即时改字",t.text);assertTrue(t.vertical);assertContentEquals(bytes,again.bytes(id))}
    }
    @Test fun pointerDrawnMasksChangeEffectSaveUndoAndReopen()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(600,400);val id=w.importImage("mask-fixture.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.selection=setOf(id);c.view="编辑"
        val scene=ImageComposeScene(1320,900){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("新建编辑工程");scene.field("工程名称","打码回归");scene.click("建立工程");c.idle();scene.frames();scene.rendered()
            for(tool in listOf("矩形","画笔","圈选")) {
                scene.click(tool)
                val bounds=scene.nodes().first{it.matches("排版编辑画布")}.boundsInRoot
                val start=Offset(bounds.left+bounds.width*.3f,bounds.top+bounds.height*.4f)
                val points=listOf(start,start+Offset(60f,20f),start+Offset(110f,65f),start+Offset(30f,90f))
                scene.sendPointerEvent(PointerEventType.Press,start,button=PointerButton.Primary)
                for(point in points.drop(1)){scene.sendPointerEvent(PointerEventType.Move,point,button=PointerButton.Primary);scene.frames()}
                scene.sendPointerEvent(PointerEventType.Release,points.last(),button=PointerButton.Primary);scene.frames();scene.rendered()
            }
            assertEquals(listOf("rect","brush","lasso"),c.editor.page!!.masks.map{it.shape})
            scene.click("打码效果 · 马赛克 ▾");scene.click("黑条 / 黑色");scene.frames();scene.rendered()
            assertEquals("black",c.editor.mask!!.effect)
            scene.click("▸ 预设与默认");scene.click("覆盖打码预设");scene.click("确认预设变更");c.idle();scene.frames()
            assertEquals("black",w.state.typesetPreferences.maskOverrides["light_mosaic"]!!.effect)
            scene.click("恢复内置打码预设");scene.click("确认预设变更");c.idle();scene.frames()
            assertTrue(w.state.typesetPreferences.maskOverrides.isEmpty());assertEquals("black",c.editor.mask!!.effect)
            scene.click("启动默认 · 已保存自定义参数 ▾");scene.click("重马赛克");c.idle();scene.frames()
            assertEquals("heavy_mosaic",w.state.typesetPreferences.preferredMaskSlot);scene.click("▾ 预设与默认")

            scene.shot("mask-workflow");c.editor.drain();val before=c.editor.page!!.masks
            scene.click("移除打码");c.editor.drain();assertEquals(2,c.editor.page!!.masks.size)
            scene.click("撤销编辑");c.idle();scene.frames();assertEquals(before,c.editor.page!!.masks)
            assertContentEquals(bytes,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->val masks=TypesetRepository(again).load(again.state.typesetProjects.single()).pages.single().masks;assertEquals(3,masks.size);assertEquals("black",masks.last().effect);assertEquals("heavy_mosaic",again.state.typesetPreferences.preferredMaskSlot);assertContentEquals(bytes,again.bytes(id))}
    }

    @Test fun queueControlSelectionDragOrderNumberingAndRestart()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(60,40)
        val ids=(1..4).map{w.importImage("page-$it.png",bytes)};var expectedOrder=emptyList<String>()
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.selection=ids.toSet();c.view="编辑"
        val scene=ImageComposeScene(1320,1000){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("新建编辑工程");scene.field("工程名称","队列回归");scene.click("建立工程");c.idle();scene.frames()
            val orderedIds=c.editor.document!!.pages.map{it.assetId};assertEquals(ids.toSet(),orderedIds.toSet())
            fun position(number:Int)=scene.nodes().first{it.matches("队列页 $number")}.boundsInRoot.center
            suspend fun choose(number:Int,ctrl:Boolean=false,shift:Boolean=false){val p=position(number);val modifiers=PointerKeyboardModifiers(isCtrlPressed=ctrl,isShiftPressed=shift)
                scene.sendPointerEvent(PointerEventType.Press,p,button=PointerButton.Primary,keyboardModifiers=modifiers)
                scene.sendPointerEvent(PointerEventType.Release,p,button=PointerButton.Primary,keyboardModifiers=modifiers);scene.frames()
            }
            choose(2);choose(4,ctrl=true);assertEquals(2,c.editor.selectedPages.size)
            val from=position(4);val to=position(1)-Offset(0f,20f)
            scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
            for(i in 1..6){scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*(i/6f),button=PointerButton.Primary);scene.frames()}
            scene.sendPointerEvent(PointerEventType.Release,to,button=PointerButton.Primary);scene.frames()
            assertEquals(listOf(orderedIds[1],orderedIds[3],orderedIds[0],orderedIds[2]),c.editor.document!!.pages.map{it.assetId})
            c.editor.drain();scene.click("撤销编辑");c.idle();scene.frames();assertEquals(orderedIds,c.editor.document!!.pages.map{it.assetId})
            choose(1);choose(3,shift=true);assertEquals(3,c.editor.selectedPages.size)
            expectedOrder=listOf(orderedIds[3],orderedIds[0],orderedIds[1],orderedIds[2]);scene.click("置底页");assertEquals(expectedOrder,c.editor.document!!.pages.map{it.assetId})
            scene.click("▸ 队列设置");scene.field("开始编号","98");scene.field("编号位数","4");scene.field("缩略图尺寸","64");scene.field("队列栏宽度","260")
            c.editor.drain();val current=c.editor.pageId;val selected=c.editor.selectedPages;val history=c.editor.project!!.undo.size
            c.editor.open(c.editor.project!!.id);c.idle();scene.frames()
            assertEquals(current,c.editor.pageId);assertEquals(selected,c.editor.selectedPages);assertEquals(history,c.editor.project!!.undo.size)
            scene.shot("queue-workflow")
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->val d=TypesetRepository(again).load(again.state.typesetProjects.single());assertEquals(expectedOrder,d.pages.map{it.assetId});assertEquals(98,d.startNumber);assertEquals(4,d.numberDigits);assertEquals(64,d.thumbnailSize);assertEquals(260,d.queueWidth);assertEquals(3,again.state.typesetProjects.single().selectedPageIds.size);assertNotNull(again.state.typesetProjects.single().currentPageId);ids.forEach{assertContentEquals(bytes,again.bytes(it))}}
    }

}
