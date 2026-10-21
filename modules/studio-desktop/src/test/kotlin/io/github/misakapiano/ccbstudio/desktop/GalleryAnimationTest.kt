@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.ApngDisguiseCodec
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.*
import java.awt.datatransfer.DataFlavor
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryAnimationTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun samePixels(a:ByteArray,b:ByteArray){val x=EditorRenderer.decode(a);val y=EditorRenderer.decode(b);try{
        assertEquals(x.width,y.width);assertEquals(x.height,y.height)
        assertContentEquals(x.getRGB(0,0,x.width,x.height,null,0,x.width),y.getRGB(0,0,y.width,y.height,null,0,y.width))
    }finally{x.flush();y.flush()}}
    @Test fun animatedGifRetainsTwoFrameColorsTimingAndLoopAfterDisguiseRestore()=runBlocking {
        val gif=Files.readAllBytes(Path.of("../studio-core/src/test/resources/codecs/animated.gif"))
        val disguised=animationTransform(gif,"disguise");val restored=animationTransform(disguised,"restore")
        val directory=Files.createDirectories(root())
        for(bytes in listOf(disguised,restored)){
            val file=Files.write(directory.resolve("${newId()}.png"),bytes)
            assertEquals(0,ApngDisguiseCodec.openAnimation(file.toFile()).playCount)
            val frames=mutableListOf<Pair<Int,Long>>()
            assertTrue(playDesktopApng(bytes,maxPlays=1){image,time->frames+=image.getRGB(5,4) to time})
            assertEquals(listOf(0xffff0000.toInt() to 100L,0xff0000ff.toInt() to 100L),frames)
        }
        assertFailsWith<IllegalArgumentException>{animationTransform(Files.readAllBytes(Path.of("../studio-core/src/test/resources/codecs/animated.webp")),"disguise")}
        Unit
    }
    @Test fun actualWorkspacePreviewRestoreBatchCopyAndReopenPreserveOriginals()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(96,64)
        val ids=listOf(w.importImage("one.png",bytes),w.importImage("two.png",bytes))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")});c.view="图库";c.selection=ids.toSet()
        val scene=ImageComposeScene(1320,900){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(5){scene.render().close();yield()};delay(80)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        suspend fun click(text:String){val n=nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==text}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
        var restored=""
        try{
            frames();click("APNG伪装 / 还原");click("预览当前处理");c.active!!.join();frames();assertNull(c.problem);assertNotNull(c.animation.preview)
            val preview=c.animation.preview!!;val tmp=Files.write(r.resolve("preview.png"),preview)
            assertNotNull(ApngDisguiseCodec.inspectDisguise(tmp.toFile()));assertFails{DesktopImageEditing.requireStatic(preview)}
            withTimeout(10000){while(nodes().none{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("处理结果静态封面")==true})frames()}
            val shot=Path.of("build/ux-r3-apng/disguise-preview.png");Files.createDirectories(shot.parent);scene.render().use{it.encodeToData()!!.use{Files.write(shot,it.bytes)}}
            c.animation.chooseDirectory={null};click("选择目录并导出全部");assertNull(c.animation.completed)
            val destination=Files.createDirectories(r.resolve("exports"));c.animation.chooseDirectory={destination}
            click("选择目录并导出全部");c.active!!.join();assertNull(c.problem)
            val output=c.animation.completed!!;assertTrue(Files.isRegularFile(output.resolve("COMPLETE.json")))
            val receipt=Json.parseToJsonElement(Files.readString(output.resolve("COMPLETE.json"))).jsonObject
            assertEquals(2,receipt.getValue("files").jsonArray.size)
            for(x in receipt.getValue("files").jsonArray){val f=x.jsonObject;assertEquals(f.getValue("sha256").jsonPrimitive.content,sha256(Files.readAllBytes(output.resolve(f.getValue("file").jsonPrimitive.content))))}
            var copied=emptyList<Path>();c.animation.copyFiles={copied=it};click("复制全部处理文件");c.active!!.join();assertEquals(2,copied.size);assertNull(c.problem)
            val transferable=ExportClipboard.contents(copied.take(1));assertTrue(transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor));assertFalse(transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
            click("保存当前副本到图库");c.active!!.join();val disguised=c.current!!;assertContentEquals(preview,w.bytes(disguised))
            c.editor.create("静态确认",listOf(disguised));c.active!!.join();frames();assertTrue(w.state.typesetProjects.isEmpty());assertNotNull(c.editor.staticChoice)
            assertTrue(c.message.contains("尚未建立工程"))
            click("取消，保留动画");assertTrue(w.state.typesetProjects.isEmpty());assertNull(c.editor.staticChoice)
            c.editor.create("静态确认",listOf(disguised));c.active!!.join();frames();click("同意仅编辑静态图");c.active!!.join();assertEquals(disguised,c.editor.document!!.pages.single().assetId)
            c.view="图库";frames()
            click("返回图库");click("APNG伪装 / 还原");click("还原伪装 APNG");click("预览当前处理");c.active!!.join();assertNull(c.problem);samePixels(bytes,c.animation.preview!!)
            click("保存当前副本到图库");c.active!!.join();restored=c.current!!;samePixels(bytes,w.bytes(restored));ids.forEach{assertContentEquals(bytes,w.bytes(it))}
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r.resolve("workspace")).use{reopened->samePixels(bytes,reopened.bytes(restored));assertTrue(restored in reopened.state.savedCopies);ids.forEach{assertContentEquals(bytes,reopened.bytes(it))}}
    }
    @Test fun failedBatchKeepsUnpublishedEvidenceAndNeverOverwritesPriorOutput():Unit=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(64,64);val id=w.importImage("original.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"))
        try{
            val parent=Files.createDirectories(r.resolve("exports"));val sentinel=Files.writeString(parent.resolve("keep.txt"),"keep")
            assertFailsWith<IllegalStateException>{exportAnimationDeck(c,listOf(id),"disguise",parent,hook={if(it=="FILE_WRITTEN")error("injected publish interruption")})}
            val partial=Files.list(parent).use{it.filter{p->p.fileName.toString().endsWith(".incomplete")}.toList()}.single()
            assertFalse(Files.exists(partial.resolve("COMPLETE.json")));assertTrue(Files.exists(partial.resolve("0001.png")))
            assertEquals("keep",Files.readString(sentinel));assertContentEquals(bytes,w.bytes(id))
            assertFailsWith<IllegalArgumentException>{exportAnimationDeck(c,listOf(id),"disguise",w.root)}
            assertFails{animationTransform(bytes,"restore")}
            Unit
        }finally{c.close();scope.cancel();w.close()}
    }
}
