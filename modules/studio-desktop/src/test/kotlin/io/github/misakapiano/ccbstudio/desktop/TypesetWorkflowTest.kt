@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
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
    @Test fun exportShowsRealPageProgressStopsWithoutPublishingAndRetriesExplicitly()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val original=EditorRenderer.synthetic(96,64);val id=w.importImage("progress.png",original)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("导出进度",listOf(id,id,id));c.idle()
        val parent=Files.createDirectories(root());c.editor.chooseExportDirectory={parent}
        val gate=java.util.concurrent.CountDownLatch(1)
        c.editor.exportTrace={p->if(p.stage=="GENERATING"&&p.completed==1)check(gate.await(20,java.util.concurrent.TimeUnit.SECONDS))}
        val scene=ImageComposeScene(1400,1000,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun begin(){scene.frames();scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(40);scene.frames()}};scene.click("选择位置并导出")}
        try {
            begin();withTimeout(20000){while(c.editor.exportProgress?.completed!=1)delay(20)};scene.frames()
            assertTrue(scene.nodes().any{it.matches("正在生成页面 · 1 / 3")});assertTrue(scene.nodes().any{it.matches("编辑工程 · 导出进度 ▾")});scene.shot("export-one-of-three")
            assertNull(c.editor.exportDirectory);scene.click("停止");gate.countDown();c.active?.join();scene.frames()
            assertEquals("CANCELLED",c.editor.exportProgress?.stage);assertNull(c.editor.exportDirectory);assertNull(c.problem)
            val incomplete=Files.list(parent).use{it.filter{p->p.fileName.toString().endsWith(".incomplete")}.toList()}.single()
            assertFalse(Files.exists(incomplete.resolve("COMPLETE.json")));assertFails{TypesetExport.verify(incomplete)}
            assertContentEquals(original,w.bytes(id));scene.shot("export-cancelled")
            c.editor.exportTrace={};begin();c.idle();scene.frames()
            val output=c.editor.exportDirectory!!;val receipt=TypesetExport.verify(output)
            assertEquals(3,receipt.files.count{it.name.endsWith(".png")});assertEquals("COMPLETE",c.editor.exportProgress?.stage)
            assertTrue(scene.nodes().any{it.matches("导出完成并已校验 · 3 / 3")});assertTrue(Files.exists(incomplete));scene.shot("export-verified")
            assertContentEquals(original,w.bytes(id))
            c.editor.copyExportFiles={error("synthetic clipboard unavailable")}
            scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(40);scene.frames()}}
            scene.click("复制所选输出文件");c.active?.join();scene.frames()
            assertNotNull(c.problem);assertContains(c.editor.exportProblem!!,"复制未完成")
            TypesetExport.verify(c.editor.exportDirectory!!);assertEquals("COMPLETE",c.editor.exportProgress?.stage)
            scene.shot("export-copy-failed")
        }finally{gate.countDown();scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun unreadablePreviewExplainsFontFailureAndOriginalRemainsViewable() {
        StudioWorkspace.openStudio(root()).use{w->
            val original=EditorRenderer.synthetic(48,32);val id=w.importImage("font.png",original)
            val missing=TypesetPage(assetId=id,name="font",texts=listOf(TypesetText(text="中文",style=TypesetStyle(fontFamily="CCB-NO-SUCH-FONT-928174"))))
            assertEquals("FONT_MISSING",assertFailsWith<DataProblem>{typesetPreviewRaster(w,missing,false,1)}.status)
            typesetPreviewRaster(w,missing,false,1,"original").flush()
            val glyph=missing.copy(texts=emptyList(),caption=String(Character.toChars(0x10ffff)))
            assertEquals("GLYPH_MISSING",assertFailsWith<DataProblem>{typesetPreviewRaster(w,glyph,true,1)}.status)
            typesetPreviewRaster(w,glyph.copy(caption="中文已恢复。"),true,1).flush();assertContentEquals(original,w.bytes(id))
        }
    }
    @Test fun storyCustomControlsSelectedAndAllStylesPreserveCaptionsAndUndoAsOneBatch()=runBlocking(awt) {
        val path=root();val w=StudioWorkspace.openStudio(path);val original=EditorRenderer.synthetic(180,120);val id=w.importImage("story.png",original)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("逐页旁白与批量样式",listOf(id,id,id));c.idle()
        val scene=ImageComposeScene(1400,1100,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        var saved:TypesetDocument?=null
        try {
            scene.frames();scene.click("小人书")
            val captions=listOf("第一张原文。","第二张旁白，不能被批量替换。","第三张。")
            for(i in 1..3){scene.click("队列页 $i");scene.field("本页旁白",captions[i-1])}
            scene.click("队列页 1");val first=c.editor.page!!.id
            for((label,value) in listOf("页面宽度" to "480","页边距" to "24","图文间距" to "18","文字内边距" to "16","文字区最小高度" to "80","旁白字号" to "22","旁白行距" to "1.4"))scene.field(label,value)
            scene.click("旁白对齐 · left ▾");scene.click("center")
            scene.click("首行缩进");scene.click("显示页码");scene.click("页面边框");scene.click("图片边框")
            scene.field("纸张颜色","#f0e0d0");scene.field("旁白颜色","#112233");scene.field("页面描边","#445566")
            val font=java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.first()
            scene.click("旁白字体 · SimSun ▾");scene.click(font)
            val expected=StoryStyle(pageWidth=480,pagePadding=24,gap=18,textPadding=16,textMinHeight=80,fontFamily=font,fontSize=22.0,lineHeight=1.4,textAlign="center",firstIndent=false,showPageNumber=false,pageBackground="#f0e0d0",textColor="#112233",borderColor="#445566",pageBorder=false,imageBorder=false)
            assertEquals(expected,c.editor.page!!.story);c.editor.drain();scene.frames()
            val checks=scene.nodes().filter{it.config.getOrNull(SemanticsProperties.Role)==Role.Checkbox}
            assertEquals(3,checks.size);assertTrue(checks[2].config[SemanticsActions.OnClick].action!!.invoke());scene.frames()
            assertEquals(first,c.editor.pageId,"Checking another page must not replace the active style source")
            val before=c.editor.document!!;scene.click("样式应用到所选页");c.editor.drain()
            assertEquals(listOf(expected,StoryStyle(),expected),c.editor.document!!.pages.map{it.story});assertEquals(captions,c.editor.document!!.pages.map{it.caption})
            scene.click("撤销编辑");c.idle();scene.frames();assertEquals(before,c.editor.document)
            scene.click("重做编辑");c.idle();scene.frames();assertEquals(expected,c.editor.document!!.pages[2].story)
            scene.click("样式应用到全册");c.editor.drain();assertTrue(c.editor.document!!.pages.all{it.story==expected});assertEquals(captions,c.editor.document!!.pages.map{it.caption})
            scene.click("撤销编辑");c.idle();scene.frames();assertEquals(StoryStyle(),c.editor.document!!.pages[1].story)
            scene.click("重做编辑");c.idle();scene.frames();scene.rendered();scene.shot("story-custom-batch")
            saved=c.editor.document;assertContentEquals(original,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(path).use{next->assertEquals(saved,TypesetRepository(next).load(next.state.typesetProjects.single()));assertContentEquals(original,next.bytes(id))}
    }
    @Test fun functionalAndInlineColorsPreviewExportAndReopenWithoutSilentFallback()=runBlocking(awt) {
        val path=root();val w=StudioWorkspace.openStudio(path);val pixels=EditorRenderer.synthetic(320,240);val id=w.importImage("colors.png",pixels)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("颜色保全",listOf(id));c.idle()
        val scene=ImageComposeScene(1400,1000,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        val rich="[color=rebeccapurple]彩色[/] [color=rgba(0,128,255,0.5)]半透明[/]"
        try {
            scene.frames();scene.rendered();scene.click("添加文字");scene.field("文字内容",rich)
            scene.field("文字颜色","hsl(210 50% 40% / 75%)");scene.field("描边颜色","#1234");scene.rendered()
            assertEquals("hsl(210 50% 40% / 75%)",c.editor.text!!.style.fill);assertEquals("#1234",c.editor.text!!.style.stroke)
            scene.shot("rich-colors")
            scene.field("文字颜色","not-a-color");assertEquals("hsl(210 50% 40% / 75%)",c.editor.text!!.style.fill)
            assertTrue(scene.nodes().any{n->n.config.getOrNull(SemanticsProperties.Text)?.any{it.text.startsWith("尚未应用：")}==true})
            scene.field("文字颜色","hsl(210 50% 40% / 75%)")
            scene.field("文字内容","[color=not-a-color]不能忽略[/]")
            withTimeout(30000){while(scene.nodes().none{n->n.config.getOrNull(SemanticsProperties.Text)?.any{it.text.startsWith("页面暂无法绘制：")}==true})scene.frames()}
            assertEquals("[color=not-a-color]不能忽略[/]",c.editor.text!!.text)
            scene.field("文字内容",rich);scene.rendered();c.editor.drain()
            c.editor.chooseExportDirectory={Files.createDirectories(root())}
            c.editor.export(TypesetExportRequest(listOf(c.editor.pageId!!)));c.idle()
            val output=TypesetExport.verify(c.editor.exportDirectory!!);assertEquals(1,output.assetIds.size)
            assertContentEquals(pixels,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(path).use{again->val t=TypesetRepository(again).load(again.state.typesetProjects.single()).pages.single().texts.single()
            assertEquals(rich,t.text);assertEquals("hsl(210 50% 40% / 75%)",t.style.fill);assertEquals("#1234",t.style.stroke);assertContentEquals(pixels,again.bytes(id))}
    }
    @Test fun durableDefaultFontLoadsOnFirstTextPreviewWithColdCache()=runBlocking(awt) {
        val r=root();var alias="";var imageId=""
        val fixture=Files.readString(Path.of("../studio-core/src/test/resources/fonts/MicrosoftPackedFont.hex")).trim().chunked(2).map{it.toInt(16).toByte()}.toByteArray()
        StudioWorkspace.openStudio(r).use{w->
            val font=TypesetFonts.import(w,"fixture.woff2",fixture);alias=font.alias
            imageId=w.importImage("font-page.png",EditorRenderer.synthetic(180,120))
            w.save(w.state.copy(typesetPreferences=w.state.typesetPreferences.copy(textDefault=TypesetText(style=TypesetStyle(fontFamily=alias)))))
        }
        // Simulate an empty process font cache without adding a production cache-control API.
        val cache=TypesetFonts::class.java.getDeclaredField("loaded").apply{isAccessible=true}.get(TypesetFonts) as MutableMap<*,*>
        cache.remove(alias);assertFailsWith<DataProblem>{TypesetFonts.font(alias)}
        val w=StudioWorkspace.openStudio(r);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("font restored",listOf(imageId));c.idle()
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.rendered();scene.click("添加文字");scene.rendered()
            assertEquals(alias,c.editor.text!!.style.fontFamily);assertNotNull(TypesetFonts.font(alias));c.editor.drain()
            cache.remove(alias);scene.click("完整预览")
            withTimeout(30000){while(scene.nodes().none{it.matches("完整页面预览")}){delay(80);scene.frames()}}
            assertNotNull(TypesetFonts.font(alias));scene.click("关闭完整预览 · Esc")
            cache.remove(alias);scene.click("预览与导出")
            withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(80);scene.frames()}}
            assertNotNull(TypesetFonts.font(alias));scene.click("返回编辑")
            val page=c.editor.page!!.copy(texts=c.editor.page!!.texts.map{it.copy(style=it.style.copy(fontFamily="ccb-font:"+newId()))})
            assertFailsWith<DataProblem>{typesetPreviewRaster(w,page,false,1)}
            typesetPreviewRaster(w,page,false,1,"original").flush()
            assertContentEquals(EditorRenderer.synthetic(180,120),w.bytes(imageId))
        }finally{scene.close();c.close();scope.cancel();w.close();cache.remove(alias)}
    }
    @Test fun maximumQueueWidthCannotStarveCanvasAndSurvivesReopen()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val id=w.importImage("wide-queue.png",EditorRenderer.synthetic(180,120));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("wide queue",listOf(id));c.idle();c.editor.change{it.copy(queueWidth=720)};c.editor.drain()
        val scene=ImageComposeScene(1100,900,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.rendered()
            val canvas=scene.nodes().first{it.matches("排版编辑画布")}.boundsInRoot
            assertTrue(canvas.width>=400,"maximum queue must not squeeze the canvas: $canvas")
            scene.click("队列");assertFalse(scene.nodes().any{it.matches("排版编辑画布")})
            scene.click("全不选页");assertTrue(c.editor.selectedPages.isEmpty());scene.click("全选页");assertEquals(1,c.editor.selectedPages.size)
            scene.click("画布");scene.rendered();scene.shot("maximum-queue-compact")
            assertEquals(720,c.editor.document!!.queueWidth);c.editor.drain()
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r).use{assertEquals(720,TypesetRepository(it).load(it.state.typesetProjects.single()).queueWidth);assertContentEquals(EditorRenderer.synthetic(180,120),it.bytes(id))}
    }
    @Test fun fullPreviewKeyboardAndExportDeckKeepEditorStateAndFitNarrowWindow()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val id=w.importImage("preview.png",EditorRenderer.synthetic(180,120));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("preview",listOf(id,id,id));c.idle();val project=c.editor.project;val selected=c.editor.selectedPages;val current=c.editor.pageId
        val trace=java.util.concurrent.ConcurrentLinkedQueue<String>();c.editor.previewTrace={trace.add(it)}
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun key(key:Key){scene.sendKeyEvent(KeyEvent(key,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(key,KeyEventType.KeyUp));scene.frames()}
        var wideClosed=false
        try {
            scene.frames();scene.click("完整预览");withTimeout(30000){while(scene.nodes().none{it.matches("完整页面预览")}){delay(80);scene.frames()}}
            assertTrue(scene.nodes().any{it.matches("1 / 3")});key(Key.DirectionRight);assertTrue(scene.nodes().any{it.matches("2 / 3")})
            key(Key.DirectionLeft);assertTrue(scene.nodes().any{it.matches("1 / 3")});key(Key.DirectionLeft);assertTrue(scene.nodes().any{it.matches("1 / 3")})
            val imageBounds=scene.nodes().first{it.matches("完整页面预览")}.boundsInRoot
            val titleBounds=scene.nodes().first{it.matches("完整预览 · 001 · preview.png")}.boundsInRoot
            val closeBounds=scene.nodes().first{it.matches("关闭完整预览 · Esc")}.boundsInRoot
            assertTrue(imageBounds.top>=titleBounds.bottom && imageBounds.bottom<=closeBounds.top,"preview must fit between title and controls: $imageBounds / $titleBounds / $closeBounds")
            scene.shot("full-preview");key(Key.Escape);assertFalse(scene.nodes().any{it.matches("完整页面预览")})
            assertEquals(project,c.editor.project);assertEquals(selected,c.editor.selectedPages);assertEquals(current,c.editor.pageId);assertNull(c.editor.exportDirectory)
            scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(80);scene.frames()}}
            var bounds=scene.nodes().first{it.matches("导出页面预览")}.boundsInRoot;assertTrue(bounds.width>400 && bounds.height>350,"wide preview $bounds")
            scene.click("下一张预览");assertTrue(scene.nodes().any{it.matches("2 / 3")});scene.shot("wide-export-deck")
            scene.click("返回编辑");assertEquals(current,c.editor.pageId);assertEquals(project,c.editor.project)
            // This checks two real viewport sizes, not concurrent offscreen window scheduling.
            scene.close();wideClosed=true
            val narrow=ImageComposeScene(700,800,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
            try{narrow.frames();narrow.click("预览与导出");withTimeout(30000){while(narrow.nodes().none{it.matches("导出页面预览")}){
                val errors=narrow.nodes().flatMap{it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.map{it.text}.filter{it.startsWith("预览失败")}
                if(errors.isNotEmpty()){narrow.shot("narrow-preview-failure");fail(errors.joinToString("\n"))}
                delay(80);narrow.frames()}}
                bounds=narrow.nodes().first{it.matches("导出页面预览")}.boundsInRoot
                assertTrue(bounds.left>=0 && bounds.right<=700 && bounds.top>=0 && bounds.bottom<=800 && bounds.height>100,"narrow preview $bounds")
                for(label in listOf("返回编辑","复制所选输出文件","选择位置并导出")){val b=narrow.nodes().first{it.matches(label)}.boundsInRoot;assertTrue(b.left>=0 && b.right<=700 && b.top>=0 && b.bottom<=800,"action $label at $b")}
                narrow.shot("narrow-export-deck")
            }catch(ex:Exception){
                narrow.shot("narrow-preview-timeout")
                println("NARROW_DIAGNOSTIC "+narrow.nodes().map{n->listOf(n.config.getOrNull(SemanticsProperties.Text),n.config.getOrNull(SemanticsProperties.ContentDescription),n.boundsInRoot)}.joinToString("\n"))
                println("NARROW_PROJECT pages=${c.editor.document?.pages?.size} current=${c.editor.pageId} problem=${c.problem}")
                println("PREVIEW_TRACE "+trace.joinToString("\n"))
                throw ex
            }finally{narrow.close()}
        }finally{if(!wideClosed)scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun continuousMaskDefaultsRespectPerPageSettings()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val id=w.importImage("defaults.png",EditorRenderer.synthetic(100,80));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        try{c.editor.create("defaults",listOf(id,id,id));c.idle();val pages=c.editor.document!!.pages
            c.editor.change{it.copy(pages=it.pages.mapIndexed{i,p->if(i==2)p.copy(toolDefaults=TypesetMask(strength=88))else p})};c.editor.drain()
            c.editor.maskDefault=TypesetMask(strength=51);c.editor.rememberLaterMaskDefault();c.idle();c.editor.selectPage(pages[1].id);assertEquals(51,c.editor.maskDefault.strength)
            c.editor.selectPage(pages[2].id);assertEquals(88,c.editor.maskDefault.strength);c.editor.selectPage(pages[0].id);assertEquals(51,c.editor.maskDefault.strength)
            assertTrue(c.editor.document!!.pages.all{it.masks.isEmpty()})
        }finally{c.close();scope.cancel();w.close()}
    }
    @Test fun webpPreviewExportAndOriginalReopen()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val raster=EditorRenderer.decode(EditorRenderer.synthetic(180,120));val bytes=try{ImageFormats.webp(raster)}finally{raster.flush()}
        val id=w.importImage("webp-source.webp",bytes);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("WebP编辑",listOf(id));c.idle();c.editor.chooseExportDirectory={Files.createDirectories(root())}
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.rendered();scene.click("添加文字");scene.field("文字内容","格式验证");c.editor.drain()
            scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(80);scene.frames()}}
            scene.click("图片格式 · PNG ▾");scene.click("WEBP");scene.shot("webp-export-preview");scene.click("选择位置并导出");c.idle()
            val output=c.editor.exportDirectory!!;assertEquals("webp",TypesetExport.verify(output).format);assertEquals("webp",w.state.typesetPreferences.exportFormat)
            val decoded=EditorRenderer.decode(Files.readAllBytes(output.resolve("001.webp")));assertEquals(180,decoded.width);decoded.flush();assertContentEquals(bytes,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r).use{next->assertEquals("webp",next.state.typesetPreferences.exportFormat);assertContentEquals(bytes,next.bytes(id));assertEquals("格式验证",TypesetRepository(next).load(next.state.typesetProjects.single()).pages.single().texts.single().text)}
    }
    @Test fun fourCategoryPreviewCopiesCompleteFileDeckWithoutSystemClipboard()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(180,120);val id=w.importImage("deck.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.editor.create("批量输出",listOf(id,id));c.idle();val clipboard=java.awt.datatransfer.Clipboard("synthetic isolated clipboard")
        c.editor.pageChange{it.copy(texts=listOf(TypesetText(text="可恢复",x=90.0,width=100.0)))};c.editor.drain()
        c.editor.copyExportFiles={paths->clipboard.setContents(ExportClipboard.contents(paths),null)}
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(80);scene.frames()}}
            scene.click("导出 · 原始文件（字节保全）");scene.click("导出 · 仅打码");scene.click("导出 · 仅文字");scene.click("附带可编辑 Recipe 文件");scene.click("同时打包 ZIP")
            scene.shot("batch-export-deck");scene.click("复制所选输出文件");c.idle();scene.frames()
            val files=clipboard.getData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as List<*>
            assertEquals(17,files.size);assertFalse(clipboard.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.imageFlavor))
            val output=c.editor.exportDirectory!!;assertEquals(17,TypesetExport.verify(output).files.size);assertContentEquals(bytes,Files.readAllBytes(output.resolve("original/001.png")))
            val single=ExportClipboard.contents(listOf(output.resolve("all/001.png")));assertTrue(single.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.imageFlavor))
            assertEquals(180,(single.getTransferData(java.awt.datatransfer.DataFlavor.imageFlavor) as java.awt.image.BufferedImage).width)
            c.editor.pageChange{page->page.copy(texts=page.texts.map{it.copy(text="修改")})};c.editor.drain();c.editor.chooseRecipeFile={output.resolve("all/001_recipe.json")}
            scene.click("导入编辑 Recipe");c.idle();scene.frames();assertEquals("修改",c.editor.page!!.texts.single().text)
            scene.click("确认应用编辑 Recipe");c.idle();scene.frames();assertEquals("可恢复",c.editor.page!!.texts.single().text)
            scene.click("撤销编辑");c.idle();assertEquals("修改",c.editor.page!!.texts.single().text)
        }finally{scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun sourceExchangePreviewConfirmExportAndReopen()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{});c.view="编辑"
        val input=Path.of("../studio-core/src/test/resources/typeset/source-workspace.json");val output=Files.createDirectories(root()).resolve("exchange.json")
        c.editor.chooseExchangeFile={save->if(save)output else input}
        val presetInput=Path.of("../studio-core/src/test/resources/typeset/source-presets.json");val presetOutput=output.resolveSibling("presets.json")
        c.editor.choosePresetFile={save->if(save)presetOutput else presetInput}
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("导入 TypeSet 工程");c.idle();scene.frames();assertTrue(w.state.typesetProjects.isEmpty());scene.click("取消导入");assertNull(c.editor.exchangeInspection)
            scene.click("导入 TypeSet 工程");c.idle();scene.frames();scene.shot("source-exchange-confirm");scene.click("建立独立导入工程");c.idle();scene.frames()
            assertEquals(1,c.assets.size);assertEquals(3,c.editor.page!!.masks.size);scene.click("▸ 工程交换文件");scene.click("导出当前 TypeSet 工程");c.idle();assertTrue(Files.exists(output))
            val roundTrip=TypesetSourceExchange.inspect(Files.readAllBytes(output));assertEquals(c.editor.page!!.texts,roundTrip.document.pages.single().texts)
            scene.click("▸ 预设与默认");scene.click("导入 TypeSet 预设");c.idle();scene.frames();assertTrue(w.state.typesetPreferences.textPresets.isEmpty());scene.click("合并并导入默认");c.idle();scene.frames()
            assertEquals("heavy_mosaic",w.state.typesetPreferences.preferredMaskSlot);assertEquals(1,w.state.typesetPreferences.textPresets.size)
            scene.click("导出 TypeSet 预设");c.idle();assertEquals(48.0,TypesetSourceExchange.inspectPresets(Files.readAllBytes(presetOutput)).preferences.textPresets.single().text.style.fontSize)
            c.editor.drain()
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r).use{next->assertContentEquals(Files.readAllBytes(input),TypesetSourceExchange.originalExchange(next,next.state.typesetProjects.single()));assertEquals("heavy_mosaic",next.state.typesetPreferences.preferredMaskSlot)}
    }
    @Test fun storyCaptionPresetPreviewExportAndRestart()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val original=EditorRenderer.synthetic(180,120);val id=w.importImage("book.png",original)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val destination=Files.createDirectories(root());var chooserCalls=0;c.editor.chooseExportDirectory={chooserCalls++;destination}
        c.editor.create("小人书回归",listOf(id,id));c.idle()
        val scene=ImageComposeScene(1320,950,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("小人书");scene.field("本页旁白","第一页，保留旁白。");scene.click("清爽画册")
            assertEquals("第一页，保留旁白。",c.editor.page!!.caption);assertEquals(TypesetBook.presets.getValue("clean"),c.editor.page!!.story)
            scene.click("下一页旁白 · Ctrl+Enter");scene.field("本页旁白","第二页。".repeat(100));scene.click("复古纸张");c.editor.drain();scene.click("记住新页面小人书样式");c.idle()
            scene.click("预览与导出");withTimeout(30000){while(scene.nodes().none{it.matches("导出页面预览")}){delay(80);scene.frames()}}
            scene.shot("book-export-preview");scene.click("选择位置并导出");c.idle();scene.frames()
            assertEquals(1,chooserCalls);val output=c.editor.exportDirectory!!;val receipt=TypesetExport.verify(output)
            assertEquals(4,receipt.files.size);assertTrue(Files.exists(output.resolve("book.pdf")));assertTrue(Files.exists(output.resolve("pages.zip")))
            assertContentEquals(original,w.bytes(id));c.editor.drain()
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r).use{next->val d=TypesetRepository(next).load(next.state.typesetProjects.single());assertEquals("storybook",d.mode);assertEquals("第一页，保留旁白。",d.pages.first().caption);assertEquals(TypesetBook.presets.getValue("vintage"),d.pages.last().story);assertEquals(TypesetBook.presets.getValue("vintage"),next.state.typesetPreferences.storyDefault)}
    }
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private suspend fun ImageComposeScene.frames(){repeat(8){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(80);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){val n=nodes().firstOrNull{it.matches(s) && it.config.getOrNull(SemanticsActions.OnClick)!=null}?:error("Missing action: $s; available="+nodes().mapNotNull{it.config.getOrNull(SemanticsProperties.Text)?.joinToString{it.text}});assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(s:String,value:String){val n=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null && (it.matches(s)||it.children.any{it.matches(s)})};assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)));frames()}
    private suspend fun WorkbenchController.idle(){withTimeout(30000){active?.join()};assertNull(problem,problem)}
    private suspend fun ImageComposeScene.rendered(){withTimeout(30000){while(nodes().none{it.matches("排版编辑画布") && it.config.getOrNull(SemanticsProperties.StateDescription)=="已更新"}){delay(40);frames()}}}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/ux-r2a-workbench/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun namedProjectLiveVerticalEditingHistoryAndRestart()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(600,400);val id=w.importImage("fixture.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        c.selection=setOf(id);c.view="编辑"
        val scene=ImageComposeScene(1320,900,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
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
        val scene=ImageComposeScene(1320,900,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
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
        val scene=ImageComposeScene(1320,1000,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
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
