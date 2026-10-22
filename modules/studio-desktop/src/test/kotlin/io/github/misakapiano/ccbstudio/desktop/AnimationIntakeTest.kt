@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.datatransfer.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class AnimationIntakeTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun emptyToolAcceptsFileDialogAndDroppedDirectoryAndRefusesWorkspaceOutput()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        val a=c.animation;a.open(emptyList());assertEquals(emptyList(),a.ids)
        val bytes=EditorRenderer.synthetic(32,48);val input=Files.createDirectories(r.resolve("input"));val first=Files.write(input.resolve("one.png"),bytes)
        try {
            a.chooseInputFiles={listOf(first)};a.addFiles();c.active!!.join();a.awaitPreview();assertNull(c.problem);assertEquals(1,a.ids!!.size)
            val transfer=object:Transferable {
                override fun getTransferDataFlavors()=arrayOf(DataFlavor.javaFileListFlavor)
                override fun isDataFlavorSupported(flavor:DataFlavor)=flavor==DataFlavor.javaFileListFlavor
                override fun getTransferData(flavor:DataFlavor):Any=listOf(input.toFile())
            }
            assertTrue(a.receive(transfer,false));c.active!!.join();a.awaitPreview();assertNull(c.problem);assertEquals(2,a.ids!!.size);assertEquals(1,a.index)
            assertEquals("drag-files",kotlinx.serialization.json.Json.parseToJsonElement(w.assets.read(java.util.UUID.fromString(a.ids!!.last())).require().request().recipe().decodeToString()).jsonObject.getValue("channel").jsonPrimitive.content)
            a.chooseOutputFile={w.root.resolve("unexpected-output.png")};a.currentFile(false);c.active!!.join();assertNotNull(c.problem);assertFalse(Files.exists(w.root.resolve("unexpected-output.png")))
            assertFails{animationIngressFiles((1..1001).map{r.resolve("not-created-$it.png")})}
            a.ids!!.forEach{assertContentEquals(bytes,w.bytes(it))};assertContentEquals(bytes,Files.readAllBytes(first));assertTrue(w.state.savedCopies.isEmpty())
        }finally{c.close();scope.cancel()}
    }
    @Test fun currentToolIntakeNavigationAndOutputOnlyClipboardNeverFallBackToOriginal()=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(64,64)
        val original=w.importImage("original.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        val a=c.animation;a.open(listOf(original));val before=c.draft
        val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){GalleryAnimationWorkspace(c)}}}
        suspend fun frames(){repeat(4){scene.render().close();yield()};delay(80)}
        suspend fun idle(){c.active?.join();a.awaitPreview();assertNull(c.problem,c.problem);frames()}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        var copies=emptyList<Path>();a.copyFiles={copies=it}
        try {
            assertFalse(a.outputReady);a.currentFile(true);assertTrue(copies.isEmpty());assertTrue(animationMenu(c).none{it.label.contains("复制")})
            idle();val oldMenu=animationMenu(c);val firstOutput=a.preview!!
            oldMenu.first{it.label=="复制当前处理文件"}.onClick();idle();assertEquals(1,copies.size);assertContentEquals(firstOutput,Files.readAllBytes(copies.single()));assertFalse(bytes.contentEquals(Files.readAllBytes(copies.single())))
            val transferable=ExportClipboard.contents(copies);assertFalse(transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
            val dir=Files.createDirectories(r.resolve("incoming"));Files.write(dir.resolve("image2.png"),bytes);Files.write(dir.resolve("image10.png"),bytes);Files.writeString(dir.resolve("note.txt"),"skip")
            Files.write(Files.createDirectories(dir.resolve("child")).resolve("ignored.png"),bytes)
            a.chooseInputDirectory={dir};a.addDirectory();idle();assertEquals(3,a.ids!!.size);assertEquals(2,a.index)
            assertEquals(2,c.gallery.imports.size);assertEquals(3,SharedAssets.images(w).size)
            copies=emptyList();oldMenu.first{it.label=="复制当前处理文件"}.onClick();assertTrue(copies.isEmpty(),"An old open context menu must not target a new result")
            assertTrue(animationNavigation(a,KeyEvent(Key.DirectionLeft,KeyEventType.KeyDown)));a.awaitPreview();assertEquals(1,a.index)
            assertTrue(animationNavigation(a,KeyEvent(Key.DirectionRight,KeyEventType.KeyDown)));a.awaitPreview();assertEquals(2,a.index)
            a.chooseOutputFile={r.resolve("result.png")};a.currentFile(false);idle();assertContentEquals(a.preview,Files.readAllBytes(r.resolve("result.png")))
            c.gallery.pasteContents={StringSelection("ordinary text")};assertFalse(a.paste())
            val input=java.awt.image.BufferedImage(480,640,java.awt.image.BufferedImage.TYPE_INT_ARGB).apply{createGraphics().let{g->try{g.color=java.awt.Color(36,110,92);g.fillRect(0,0,width,height)}finally{g.dispose()}}}
            c.gallery.pasteContents={object:Transferable {
                override fun getTransferDataFlavors()=arrayOf(DataFlavor.imageFlavor)
                override fun isDataFlavorSupported(flavor:DataFlavor)=flavor==DataFlavor.imageFlavor
                override fun getTransferData(flavor:DataFlavor):Any=input
            }}
            assertTrue(a.paste());idle();assertEquals(4,a.ids!!.size);assertEquals(3,a.index)
            assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("结果缩略图滚动条")==true})
            frames();val shot=Path.of("build/animation-intake/compact-tool.png");Files.createDirectories(shot.parent);scene.render().use{it.encodeToData()!!.use{Files.write(shot,it.bytes)}}
            a.previewTransform={_,_,_->error("Synthetic preview failure")};a.selectIndex(0);a.awaitPreview();assertFalse(a.outputReady);copies=emptyList();a.currentFile(true);a.saveCopy();assertTrue(copies.isEmpty());assertTrue(w.state.savedCopies.isEmpty());assertTrue(animationMenu(c).none{it.label.contains("复制")})
            a.removeCurrent();assertEquals(3,a.ids!!.size);assertContentEquals(bytes,w.bytes(original));assertEquals(before,c.draft);assertTrue(w.state.jobs.isEmpty())
        }finally{scene.close();c.close();scope.cancel()}
    }
}
