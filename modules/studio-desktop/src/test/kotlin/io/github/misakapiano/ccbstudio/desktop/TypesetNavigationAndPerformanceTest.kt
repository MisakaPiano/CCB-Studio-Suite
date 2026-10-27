@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetNavigationAndPerformanceTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private suspend fun idle(c:WorkbenchController){withTimeout(30000){c.active?.join()};assertNull(c.problem,c.problem)}
    private fun controller(w:StudioWorkspace,scope:CoroutineScope,runtime:Path)=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(runtime.resolve("catalog")),runtime.resolve("budget"),admissionOverride=RequestAdmission{error("No services")})
    private suspend fun ImageComposeScene.frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(130);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}

    @Test fun routeDisposalKeepsDraftQueueSelectionAndSavedProjectThenRestartResumesIt()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val original=EditorRenderer.synthetic(320,200);val id=w.importImage("synthetic-navigation.png",original)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,root.resolve("runtime"));lateinit var saved:TypesetDocument;lateinit var projectId:String;lateinit var selected:String
        c.editor.create("retained project",listOf(id,id));idle(c)
        val scene=ImageComposeScene(1320,900,coroutineContext=awt){StudioTheme{Surface{if(c.view=="编辑")TypesetWorkspace(c)else Text("Other route")}}}
        try{
            withTimeout(30000){while(c.editor.queueRasters.size()<2||c.editor.canvasSource==null)scene.frames()}
            c.editor.selectPage(c.editor.document!!.pages.last().id);selected=c.editor.pageId!!;c.editor.tool="brush";c.editor.propertySection="打码"
            c.editor.pageChange{it.copy(texts=listOf(TypesetText(text="saved draft")),masks=listOf(TypesetMask(effect="black")))}
            saved=c.editor.document!!;projectId=c.editor.project!!.id
            // Navigate before the 400 ms debounce; route disposal must not discard the draft.
            c.navigate("图库");idle(c);assertEquals("图库",c.view);scene.frames()
            val p=w.state.typesetProjects.single();assertEquals(saved,TypesetRepository(w).load(p));assertEquals(projectId,TypesetEditorExtrasStore.load(w).lastProjectId)
            c.navigate("编辑");scene.frames();idle(c);assertEquals(saved,c.editor.document);assertEquals(selected,c.editor.pageId);assertEquals("brush",c.editor.tool);assertEquals("打码",c.editor.propertySection)
            withTimeout(30000){while(c.editor.queueRasters.size()<3)scene.frames()}
            val renders=c.editor.queueRasters.renders
            c.navigate("资产管理");idle(c);scene.frames();c.navigate("编辑");scene.frames();idle(c);scene.frames()
            assertEquals(renders,c.editor.queueRasters.renders,"Route return must reuse unchanged display thumbnails")
            assertContentEquals(original,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel()}
        val reopened=StudioWorkspace.openStudio(root.resolve("workspace"));val s2=CoroutineScope(SupervisorJob()+awt);val c2=controller(reopened,s2,root.resolve("runtime2"))
        try{assertNull(c2.editor.document);c2.editor.resumeLastProject();idle(c2);assertEquals(projectId,c2.editor.project!!.id);assertEquals(saved,c2.editor.document);assertEquals(selected,c2.editor.pageId);assertContentEquals(original,reopened.bytes(id))}
        finally{c2.close();s2.cancel()}
    }

    @Test fun failedSaveKeepsEditorVisibleAndDraftInsteadOfNavigatingAway()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val id=w.importImage("synthetic.png",EditorRenderer.synthetic(128,64));val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,root.resolve("runtime"))
        try{
            c.editor.create("stale",listOf(id));idle(c);val p=c.editor.project!!
            val advanced=TypesetRepository(w).update(p.id,p.revision){it.copy(startNumber=2)}
            c.editor.pageChange{it.copy(caption="unsaved draft")};val draft=c.editor.document
            c.navigate("图库");withTimeout(30000){c.active?.join()}
            assertNotNull(c.problem);assertEquals("编辑",c.view);assertEquals(draft,c.editor.document);assertTrue(c.editor.dirty);assertEquals(advanced,w.state.typesetProjects.single())
        }finally{scope.cancel();w.close()}
    }

    @Test fun aiFollowsCurrentToolEffectStrengthAndOpacityWhileKeepingDetectionGeometry()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val bytes=EditorRenderer.synthetic(128,64);val id=w.importImage("synthetic.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,root.resolve("runtime"))
        try{
            c.editor.create("tool follow",listOf(id));idle(c);val a=c.editor.autoMask;a.chooseModel={"synthetic.onnx" to LocalOnnxMaskDetectorTest.syntheticModel()};a.loadModel();idle(c);a.followTool=true;a.effect="blur";a.strength=3;a.opacity=.1;a.saveConfiguration();idle(c)
            for((index,effect)in listOf("black","white","mosaic","blur").withIndex()){
                c.editor.objectId=null;c.editor.tool="brush";c.editor.updateMaskSettings{it.copy(effect=effect,strength=20+index,opacity=.65,size=80.0,density=9.0,stripeThickness=4.0,pointDensity=7.0)}
                a.runConfigured();idle(c);val mask=c.editor.page!!.masks.last()
                assertEquals(effect,mask.effect);assertEquals(20+index,mask.strength);assertEquals(.65,mask.opacity);assertEquals("rect",mask.shape);assertTrue(mask.width>0&&mask.height>0);assertEquals(80.0,mask.size)
            }
            assertTrue(TypesetEditorExtrasStore.load(w).autoMask!!.followTool);assertEquals(4,c.editor.page!!.masks.size);assertContentEquals(bytes,w.bytes(id))
        }finally{c.close();scope.cancel()}
    }

    @Test fun fontValidationSkipsGeometryOnlyChangesAndThumbnailCacheIsBoundedAndRecipeSensitive(){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());StudioWorkspace.openStudio(root).use{w->
            val id=w.importImage("synthetic.png",EditorRenderer.synthetic(320,200));val original=TypesetText(text="Synthetic preview",style=TypesetStyle(fontFamily="Dialog"));val page=TypesetPage(assetId=id,name="synthetic",texts=listOf(original))
            val fonts=TypesetPreviewFonts(w);fonts.ensure(page);val start=System.nanoTime()
            repeat(60){i->fonts.ensure(page.copy(texts=listOf(original.copy(x=i.toDouble(),y=i.toDouble()))))}
            val hot=System.nanoTime()-start;assertEquals(1,fonts.checks)
            val changed=page.copy(texts=listOf(original.copy(text="Changed glyphs")));fonts.ensure(changed);assertEquals(2,fonts.checks)
            val baselineStart=System.nanoTime();repeat(10){TypesetFonts.verifyRenderable(w,TypesetDocument(pages=listOf(page)))};val baseline=System.nanoTime()-baselineStart
            Files.writeString(root.resolve("preview-font-work.txt"),"geometryMoves=60 validationChecks=1 hotNanos=$hot baseline10ChecksNanos=$baseline\nNot native frame latency or user acceptance.\n")
            val image=EditorRenderer.synthetic(64,32).let{TypesetImage.decode(it,TypesetImage.RAW)}
            try{
                val bitmap=image.toComposeImageBitmap();val cache=TypesetQueueRasterCache();val key=TypesetQueueRasterKey(page,"editor",1,64);val value=QueueHoverRaster(page,"editor",1,bitmap,64,32)
                cache.put(key,value);assertSame(value,cache.get(key));assertNull(cache.get(key.copy(page=changed)));assertNull(cache.get(key.copy(mode="storybook")));assertNull(cache.get(key.copy(maxSide=128)))
                repeat(40){cache.put(key.copy(number=it+2),value)};assertTrue(cache.size()<=24);assertNull(cache.get(key))
            }finally{image.flush()}
        }
    }
}
