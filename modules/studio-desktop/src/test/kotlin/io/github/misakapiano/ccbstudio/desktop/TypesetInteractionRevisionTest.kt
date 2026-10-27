@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
@file:Suppress("DEPRECATION")
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetInteractionRevisionTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private class FakeClipboard:ClipboardManager,Clipboard {
        private val memory=java.awt.datatransfer.Clipboard("isolated-synthetic-editor-test")
        override val nativeClipboard:Any get()=memory
        var value:AnnotatedString?
            get()=(memory.getContents(null) as? java.awt.datatransfer.Transferable)?.let{if(it.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor))AnnotatedString(it.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor) as String)else null}
            set(v){memory.setContents(java.awt.datatransfer.StringSelection(v?.text.orEmpty()),null)}
        override fun getText()=value
        override fun setText(annotatedString:AnnotatedString){value=annotatedString}
        override fun getClip()=memory.getContents(null)?.let{ClipEntry(it)}
        override fun setClip(clipEntry:ClipEntry?){memory.setContents(clipEntry?.nativeClipEntry as? java.awt.datatransfer.Transferable,null)}
        override suspend fun getClipEntry()=getClip()
        override suspend fun setClipEntry(clipEntry:ClipEntry?){setClip(clipEntry)}
    }
    private suspend fun <T> editor(block:suspend(WorkbenchController,ImageComposeScene,ByteArray,String,FakeClipboard)->T):T {
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root)
        val bytes=EditorRenderer.synthetic(480,320);val asset=w.importImage("synthetic.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("catalog")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No service calls")})
        c.editor.create("交互布局审阅",List(12){asset});c.idle();c.editor.queueView="list"
        val t=TypesetText(text="「中文竖排」 Example 123。",vertical=true,x=350.0,y=20.0,width=270.0,style=TypesetStyle(fontFamily="Microsoft YaHei",fontSize=22.0))
        c.editor.pageChange{it.copy(texts=listOf(t))};c.editor.objectId=t.id;c.editor.drain()
        val clipboard=FakeClipboard()
        val scene=ImageComposeScene(1320,900,coroutineContext=awt){CompositionLocalProvider(LocalClipboardManager provides clipboard,LocalClipboard provides clipboard){StudioTheme{Surface(color=Ink,contentColor=MaterialTheme.colors.onSurface){TypesetWorkspace(c)}}}}
        try{scene.frames();withTimeout(30000){while(c.editor.canvasSource==null)scene.frames()};return block(c,scene,bytes,asset,clipboard)}
        finally{scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun groupedLayoutQueueResizeAndThumbnailControlsUseActualRenderedBounds()=runBlocking(awt){editor{c,scene,bytes,asset,_->
        scene.shot("revision-layout-before-assertions")
        val canvas=scene.node("排版编辑画布").boundsInRoot
        val fit=scene.node("适应画布").boundsInRoot
        assertTrue(fit.top>=canvas.bottom-1,"Fit belongs below the canvas")
        assertTrue(scene.node("预览与导出").boundsInRoot.top>=canvas.bottom-1)
        val size=scene.fieldNode("字号").boundsInRoot;val font=scene.node("字体 · Microsoft YaHei ▾").boundsInRoot
        assertTrue(kotlin.math.abs(scene.node("字号").boundsInRoot.top-scene.node("字体").boundsInRoot.top)<1)
        assertTrue(kotlin.math.abs(size.center.y-font.center.y)<4,"Input content and button center align: $size / $font");assertTrue(size.left>font.left)
        val tabs=listOf("文字","打码","图层","预设").map{scene.node(it).boundsInRoot}
        assertTrue(tabs.all{kotlin.math.abs(it.top-tabs[0].top)<1});assertTrue(tabs.maxOf{it.width}-tabs.minOf{it.width}<2)
        scene.shot("revision-layout-list")
        val divider=scene.node("调整队列栏宽度").boundsInRoot.center;scene.drag(divider,divider+Offset(70f,0f));assertTrue(c.editor.document!!.queueWidth in 270..300)
        val before=c.editor.document!!.thumbnailSize;val queue=scene.node("排版页面队列").boundsInRoot
        scene.sendPointerEvent(PointerEventType.Scroll,queue.center,scrollDelta=Offset(0f,-2f),keyboardModifiers=PointerKeyboardModifiers(isCtrlPressed=true));scene.frames()
        assertEquals(before+16,c.editor.document!!.thumbnailSize)
        val slider=scene.node("缩略图大小滑条").boundsInRoot;scene.drag(Offset(slider.left+8,slider.center.y),Offset(slider.left+slider.width*.7f,slider.center.y))
        assertTrue(c.editor.document!!.thumbnailSize in 145..180)
        scene.click("网格视图");scene.shot("revision-layout-grid")
        scene.click("缩略图与条目视图");c.editor.drain();assertContentEquals(bytes,c.workspace.bytes(asset))
    }}
    @Test fun canvasAndQueueClipboardHistoryShortcutsDoNotInterceptTextEditing()=runBlocking(awt){editor{c,scene,bytes,asset,fieldClipboard->
        val write=TypesetClipboard.write;val read=TypesetClipboard.read;var clip:String?=null
        TypesetClipboard.write={clip=it};TypesetClipboard.read={clip}
        try{
            scene.focus("排版编辑画布");scene.key(Key.C,ctrl=true);assertNotNull(clip);scene.key(Key.V,ctrl=true)
            assertEquals(2,c.editor.page!!.texts.size);c.editor.drain();scene.key(Key.Z,ctrl=true);c.idle();scene.frames();assertEquals(1,c.editor.page!!.texts.size)
            scene.key(Key.Z,ctrl=true,shift=true);c.idle();scene.frames();assertEquals(2,c.editor.page!!.texts.size)
            c.editor.objectId=c.editor.page!!.texts.last().id;scene.frames();scene.focus("排版编辑画布");scene.key(Key.X,ctrl=true);assertEquals(1,c.editor.page!!.texts.size)
            scene.key(Key.V,ctrl=true);assertEquals(2,c.editor.page!!.texts.size);c.editor.drain()
            c.editor.objectId=c.editor.page!!.texts.last().id;scene.frames();scene.focus("文字内容")
            val count=c.editor.page!!.texts.size;val oldClip=clip
            scene.key(Key.A,ctrl=true);scene.key(Key.C,ctrl=true);assertEquals(c.editor.text!!.text,fieldClipboard.value?.text);assertEquals(oldClip,clip)
            fieldClipboard.value=AnnotatedString("正文输入区域");scene.key(Key.V,ctrl=true);assertEquals("正文输入区域",c.editor.text!!.text);assertEquals(count,c.editor.page!!.texts.size)
            val ids=c.editor.document!!.pages.map{it.id};scene.click("队列页 1");scene.key(Key.A,ctrl=true);assertEquals(ids.toSet(),c.editor.selectedPages)
            c.editor.selectedPages=setOf(ids.first());scene.key(Key.C,ctrl=true);scene.key(Key.V,ctrl=true);assertEquals(ids.size+1,c.editor.document!!.pages.size)
            assertEquals(ids.first(),c.editor.document!!.pages.first().id);c.editor.drain()
            scene.key(Key.Z,ctrl=true);c.idle();scene.frames();assertEquals(ids,c.editor.document!!.pages.map{it.id});assertContentEquals(bytes,c.workspace.bytes(asset))
        }finally{TypesetClipboard.write=write;TypesetClipboard.read=read}
    }}
    @Test fun continuousTabsTypeSpecificParametersPaletteAndLayerDragUseOneHistoryChange()=runBlocking(awt){editor{c,scene,bytes,asset,_->
        scene.click("打码");scene.click("画笔");c.editor.updateMaskSettings{it.copy(effect="black",density=14.0,stripeThickness=3.0)};scene.frames()
        assertTrue(scene.nodes().any{it.matches("条纹密度")});assertFalse(scene.nodes().any{it.matches("模糊强度")||it.matches("笔点密度")})
        scene.click("队列页 2");assertEquals("打码",c.editor.propertySection);assertEquals("brush",c.editor.tool);assertEquals(14.0,c.editor.maskDefault.density)
        c.editor.updateMaskSettings{it.copy(effect="mosaic",strength=28)};scene.frames()
        assertTrue(scene.nodes().any{it.matches("马赛克块大小")});assertTrue(scene.nodes().any{it.matches("笔点密度")});assertFalse(scene.nodes().any{it.matches("条纹密度")})
        c.editor.updateMaskSettings{it.copy(effect="blur")};scene.frames();assertTrue(scene.nodes().any{it.matches("模糊强度")})
        scene.click("矩形");scene.frames();assertFalse(scene.nodes().any{it.matches("画笔宽度")})
        c.editor.updateMaskSettings{it.copy(effect="white")};scene.frames();assertFalse(scene.nodes().any{it.matches("模糊强度")||it.matches("马赛克块大小")})
        scene.click("全选");assertEquals(12,c.editor.selectedPages.size);scene.click("反选");assertTrue(c.editor.selectedPages.isEmpty());scene.click("反选");scene.click("清除");assertTrue(c.editor.selectedPages.isEmpty())
        assertEquals(1,scene.nodes().count{it.matches("全选")&&it.config.getOrNull(SemanticsActions.OnClick)!=null})
        scene.click("队列页 1");c.editor.objectId=c.editor.page!!.texts.single().id;scene.click("文字");scene.click("▸ 颜色、描边与透明度")
        val old=c.editor.text!!.style.fill;scene.click("文字颜色 · 色盘取色");scene.click("常用颜色 #d15747");assertEquals(old,c.editor.text!!.style.fill)
        scene.click("取消取色");assertEquals(old,c.editor.text!!.style.fill)
        scene.click("文字颜色 · 色盘取色");scene.click("常用颜色 #2b9181");scene.click("应用所选颜色");assertEquals("#2b9181",c.editor.text!!.style.fill)
        c.editor.pageChange{p->p.copy(masks=List(3){TypesetMask(x=it*30.0,y=10.0,width=20.0,height=20.0)})};scene.click("图层");c.editor.drain();scene.frames()
        val original=TypesetObjects.order(c.editor.page!!);val revision=c.editor.project!!.revision
        val from=scene.node("拖动图层 文字 1").boundsInRoot.center;val last=scene.node("图层行 打码 1").boundsInRoot.bottom
        scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
        for(n in 1..12){scene.sendPointerEvent(PointerEventType.Move,Offset(from.x,from.y+(last+8-from.y)*n/12),button=PointerButton.Primary);scene.frames()}
        assertEquals(original,TypesetObjects.order(c.editor.page!!));assertEquals(revision,c.editor.project!!.revision)
        scene.sendPointerEvent(PointerEventType.Release,Offset(from.x,last+8),button=PointerButton.Primary);scene.frames()
        assertEquals(original.last(),TypesetObjects.order(c.editor.page!!).first());c.editor.drain();assertEquals(revision+1,c.editor.project!!.revision)
        scene.shot("continuous-layer-list");scene.click("撤销编辑");c.idle();scene.frames();assertEquals(original,TypesetObjects.order(c.editor.page!!))
        scene.click("图层行 文字 1");assertEquals("图层",c.editor.propertySection);scene.click("队列页 2");assertEquals("图层",c.editor.propertySection)
        assertContentEquals(bytes,c.workspace.bytes(asset))
    }}
    @Test fun reviewImportEntryCancelsSafelyImportsOriginalAndAddsImageForLocalInference()=runBlocking(awt){editor{c,scene,bytes,asset,_->
        val importScene=ImageComposeScene(1320,60,coroutineContext=awt){StudioTheme{Surface(color=Panel){Row{EditorReviewCandidate.ImportActions(c)}}}}
        val input=Path.of(System.getProperty("preview.test.root")).resolve("review-selected-synthetic.png")
        val importedBytes=EditorRenderer.synthetic(640,480);Files.write(input,importedBytes)
        try {
            importScene.frames();val count=c.assets.size;val pages=c.editor.document!!.pages.size
            c.gallery.chooseImportFiles={emptyList()};importScene.click("导入测试图片");assertEquals(count,c.assets.size);assertEquals(pages,c.editor.document!!.pages.size)
            c.gallery.chooseImportFiles={listOf(input)};importScene.click("导入测试图片");c.idle();scene.frames()
            val imported=c.assets.single{it.request().name()==input.fileName.toString()}.request().id().toString()
            assertEquals(count+1,c.assets.size);assertContentEquals(importedBytes,c.workspace.bytes(imported));assertContentEquals(importedBytes,Files.readAllBytes(input))
            scene.click("从资产库追加");scene.click("选用资产 $imported");scene.click("确认选用资产");c.idle();scene.frames()
            assertEquals(pages+1,c.editor.document!!.pages.size);assertEquals(imported,c.editor.page!!.assetId)
            val a=c.editor.autoMask;a.chooseModel={"synthetic-pipeline-only.onnx" to LocalOnnxMaskDetectorTest.syntheticModel()};a.loadModel();c.idle();a.detect();c.idle()
            assertTrue(a.selectedRegions.isNotEmpty());assertTrue(a.apply());c.editor.drain();c.editor.history();c.idle()
            assertTrue(c.editor.page!!.masks.isEmpty());assertContentEquals(importedBytes,c.workspace.bytes(imported));assertContentEquals(importedBytes,Files.readAllBytes(input));assertContentEquals(bytes,c.workspace.bytes(asset))
        } finally { importScene.close() }
    }}
    @Test fun saveAiSettingsThenToolbarAppliesAcrossPagesWithoutReopeningConfiguration()=runBlocking(awt){editor{c,scene,bytes,asset,_->
        val a=c.editor.autoMask;a.chooseModel={"synthetic.onnx" to LocalOnnxMaskDetectorTest.syntheticModel()}
        scene.click("AI 打码设置");c.idle();scene.frames();assertTrue(scene.nodes().any{it.matches("自动打码配置")})
        scene.click("选择本地 ONNX 模型");c.idle();scene.frames();assertEquals("0.3",scene.fieldNode("置信度").config[SemanticsProperties.EditableText].text)
        assertTrue(scene.nodes().none{it.matches("开始本地识别")});scene.shot("one-click-ai-settings")
        scene.click("保存配置");c.idle();scene.frames();assertTrue(scene.nodes().none{it.matches("自动打码配置")})
        scene.click("AI 自动打码");c.idle();scene.frames();assertEquals(1,c.editor.page!!.masks.size);assertTrue(scene.nodes().none{it.matches("自动打码配置")})
        scene.click("队列页 2");scene.click("AI 自动打码");c.idle();scene.frames();assertEquals(1,c.editor.page!!.masks.size)
        scene.click("AI 打码设置");c.idle();scene.frames();a.effect="black";scene.click("关闭");scene.click("AI 自动打码");c.idle();scene.frames()
        assertEquals(1,c.editor.page!!.masks.size);assertEquals("mosaic",c.editor.page!!.masks.single().effect)
        scene.shot("one-click-ai-toolbar-result");assertContentEquals(bytes,c.workspace.bytes(asset))
    }}
    @Test fun imageViewerWheelZoomDragPanHoverButtonsAndKeyboardAreViewOnly()=runBlocking(awt){
        val image=TypesetImage.decode(EditorRenderer.synthetic(480,320),TypesetImage.RAW);val bitmap=try{image.toComposeImageBitmap()}finally{image.flush()}
        var previous=0;var next=0;var closed=0
        val scene=ImageComposeScene(900,680,coroutineContext=awt){MaterialTheme{TypesetImageViewport(bitmap,"synthetic","浏览测试",Modifier.fillMaxSize(),previous={previous++},next={next++},dismiss={closed++})}}
        try{
            scene.frames();val bounds=scene.node("浏览测试").boundsInRoot;scene.sendPointerEvent(PointerEventType.Enter,bounds.center);scene.frames()
            assertTrue(scene.nodes().any{it.matches("上一张图片")});assertTrue(scene.nodes().any{it.matches("下一张图片")})
            val start=scene.node("浏览测试").config[SemanticsProperties.StateDescription]
            scene.sendPointerEvent(PointerEventType.Scroll,bounds.center,scrollDelta=Offset(0f,-2f));scene.frames()
            val zoomed=scene.node("浏览测试").config[SemanticsProperties.StateDescription];assertNotEquals(start,zoomed)
            scene.drag(bounds.center,bounds.center+Offset(70f,45f));assertNotEquals(zoomed,scene.node("浏览测试").config[SemanticsProperties.StateDescription])
            scene.key(Key.Zero);assertEquals(start,scene.node("浏览测试").config[SemanticsProperties.StateDescription])
            scene.key(Key.DirectionLeft);scene.key(Key.DirectionRight);scene.key(Key.Escape);assertEquals(1,previous);assertEquals(1,next);assertEquals(1,closed)
            scene.shot("revision-image-viewer")
        }finally{scene.close()}
    }
    private suspend fun WorkbenchController.idle(){withTimeout(30000){active?.join()};assertNull(problem,problem)}
    private suspend fun ImageComposeScene.frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(100);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private fun ImageComposeScene.node(s:String)=nodes().first{it.matches(s)}
    private fun ImageComposeScene.fieldNode(s:String)=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.SetText)!=null}
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.focus(s:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.RequestFocus)!=null};assertTrue(n.config[SemanticsActions.RequestFocus].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.key(k:Key,ctrl:Boolean=false,shift:Boolean=false){sendKeyEvent(KeyEvent(k,KeyEventType.KeyDown,isCtrlPressed=ctrl,isShiftPressed=shift));sendKeyEvent(KeyEvent(k,KeyEventType.KeyUp,isCtrlPressed=ctrl,isShiftPressed=shift));frames()}
    private suspend fun ImageComposeScene.drag(a:Offset,b:Offset){sendPointerEvent(PointerEventType.Press,a,button=PointerButton.Primary);for(i in 1..8){sendPointerEvent(PointerEventType.Move,a+(b-a)*(i/8f),button=PointerButton.Primary);frames()};sendPointerEvent(PointerEventType.Release,b,button=PointerButton.Primary);frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of(System.getProperty("preview.test.root")).resolve("typeset-queue/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}
}
