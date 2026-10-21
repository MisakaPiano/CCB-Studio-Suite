@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.datatransfer.*
import java.awt.image.BufferedImage
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryIngressTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun transfer(vararg values:Pair<DataFlavor,Any>)=object:Transferable{
        override fun getTransferDataFlavors()=values.map{it.first}.toTypedArray()
        override fun isDataFlavorSupported(f:DataFlavor)=values.any{it.first==f}
        override fun getTransferData(f:DataFlavor)=values.firstOrNull{it.first==f}?.second?:throw UnsupportedFlavorException(f)
    }
    @Test fun filesWinOverRasterAndTextPasteRemainsText() {
        val path=Path.of("synthetic.apng");val raster=BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB)
        val files=galleryTransfer(transfer(DataFlavor.javaFileListFlavor to listOf(path.toFile()),DataFlavor.imageFlavor to raster),true)!!
        assertEquals(listOf(path),files.files);assertNull(files.raster)
        assertNull(galleryTransfer(transfer(DataFlavor.stringFlavor to "ordinary text",DataFlavor.imageFlavor to raster),true))
        assertNotNull(galleryTransfer(transfer(DataFlavor.imageFlavor to raster),true)?.raster)
        assertFailsWith<IllegalArgumentException>{galleryTransfer(transfer(DataFlavor.javaFileListFlavor to List(1001){path.toFile()}),true)}
        assertFailsWith<IllegalArgumentException>{galleryTransfer(transfer(DataFlavor.javaFileListFlavor to listOf("not a File")),true)}
        raster.flush()
    }
    @Test fun guiPasteBatchReportsFailuresRetainsExactOriginalAndRasterProvenanceAfterRestart()=runBlocking(awt){
        val root=root();val files=Files.createDirectories(root.resolve("input"));val bytes=EditorRenderer.synthetic(96,64)
        val first=Files.write(files.resolve("原件.png"),bytes);val bad=Files.writeString(files.resolve("broken.png"),"not png")
        val second=Files.write(files.resolve("second.png"),bytes)
        val w=StudioWorkspace.openStudio(root.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")});c.view="图库"
        val scene=ImageComposeScene(1100,800){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(5){scene.render().close();yield()};delay(80)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun click(text:String){val n=nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==text}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke())}
        var originals=emptyList<String>();var rasterId=""
        try{
            c.gallery.pasteContents={transfer(DataFlavor.javaFileListFlavor to listOf(first.toFile(),bad.toFile(),second.toFile()))}
            frames();click("粘贴图片");c.active!!.join();frames()
            assertEquals(listOf("IMPORTED_ORIGINAL","FAILED · UNSUPPORTED_IMAGE","IMPORTED_ORIGINAL"),c.gallery.imports.map{it.status})
            originals=c.gallery.imports.mapNotNull{it.assetId};assertEquals(originals.toSet(),c.selection);assertEquals(originals.last(),c.current)
            originals.forEach{assertContentEquals(bytes,w.bytes(it))};assertContentEquals(bytes,Files.readAllBytes(first))
            assertTrue(c.message.contains("有未完成项"));assertNotNull(c.problem);click("关闭");frames();assertNull(c.problem)
            val today=java.time.LocalDate.now().toString();assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==today}==true})
            click("导入结果 2 / 3");frames();val shot=Path.of("build/ux-r3-ingress/partial-import.png");Files.createDirectories(shot.parent);scene.render().use{it.encodeToData()!!.use{Files.write(shot,it.bytes)}}
            val raster=BufferedImage(16,20,BufferedImage.TYPE_INT_ARGB).apply{setRGB(5,6,0xff00ff00.toInt())}
            c.gallery.pasteContents={transfer(DataFlavor.imageFlavor to raster)};click("粘贴图片");c.active!!.join();frames()
            rasterId=c.gallery.imports.single().assetId!!;assertTrue(c.gallery.imports.single().status.startsWith("IMPORTED_RASTER"))
            val saved=EditorRenderer.decode(w.bytes(rasterId));try{assertEquals(raster.getRGB(5,6),saved.getRGB(5,6))}finally{saved.flush();raster.flush()}
            c.gallery.pasteContents={StringSelection("normal text")};assertFalse(c.pasteImages());assertEquals(3,w.catalog().size)
            assertFalse(c.receiveTransfer(StringSelection("drop text"),false))
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root.resolve("workspace")).use{reopened->
            originals.forEach{assertContentEquals(bytes,reopened.bytes(it))}
            val recipe=reopened.assets.read(java.util.UUID.fromString(rasterId)).require().request().recipe().decodeToString()
            assertEquals("clipboard-raster",Json.parseToJsonElement(recipe).jsonObject.getValue("source").jsonPrimitive.content)
        }
    }
}
