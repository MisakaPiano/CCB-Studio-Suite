package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.test.*

class TypesetAutoMaskOneClickTest {
    private suspend fun idle(c:WorkbenchController){withTimeout(30000){c.active?.join()};assertNull(c.problem,c.problem)}
    @Test fun saveOnceThenRepeatedPagesAndRestartUseLocalModelWithOneUndoAndNoDuplicateMasks()=runBlocking {
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val bytes=EditorRenderer.synthetic(128,64);val id=w.importImage("synthetic.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime/catalog")),root.resolve("runtime/budget"),admissionOverride=RequestAdmission{error("No services")})
        lateinit var projectId:String
        try{
            c.editor.create("one click",listOf(id,id,id));idle(c);val manual=TypesetMask(effect="black",x=1.0,y=1.0,width=4.0,height=4.0)
            c.editor.pageChange{it.copy(masks=listOf(manual))};c.editor.drain();projectId=c.editor.project!!.id
            val a=c.editor.autoMask;a.chooseModel={"synthetic.onnx" to LocalOnnxMaskDetectorTest.syntheticModel()};a.loadModel();idle(c);a.selectedPages=true;a.strength=24;a.opacity=.7;a.followTool=false
            a.saveConfiguration();idle(c);assertNotNull(TypesetEditorExtrasStore.load(w).autoMask)
            c.editor.selectedPages=c.editor.document!!.pages.map{it.id}.toSet();val untouched=c.editor.document!!.pages.last();c.editor.selectedPages=c.editor.document!!.pages.take(2).map{it.id}.toSet();val undo=c.editor.project!!.undo.size
            a.runConfigured{error("Configuration should be retained")};idle(c)
            assertEquals(undo+1,c.editor.project!!.undo.size);assertEquals(listOf(2,1,0),c.editor.document!!.pages.map{it.masks.size});assertEquals(untouched,c.editor.document!!.pages.last());assertTrue(c.editor.document!!.pages.flatMap{it.masks}.filter{it.id!=manual.id}.all{it.strength==24&&it.opacity==.7})
            val revision=c.editor.project!!.revision;a.runConfigured();idle(c);assertEquals(revision,c.editor.project!!.revision)
            c.editor.history();idle(c);assertEquals(listOf(1,0,0),c.editor.document!!.pages.map{it.masks.size});assertEquals(manual,c.editor.document!!.pages.first().masks.single());assertContentEquals(bytes,w.bytes(id))
        }finally{c.close();scope.cancel()}
        val reopened=StudioWorkspace.openStudio(root.resolve("workspace"));val s2=CoroutineScope(SupervisorJob()+Dispatchers.Default);val c2=WorkbenchController(reopened,s2,DesktopNovelAiInfrastructure(root.resolve("runtime/catalog2")),root.resolve("runtime/budget2"),admissionOverride=RequestAdmission{error("No services")})
        try{c2.editor.open(projectId);idle(c2);val a=c2.editor.autoMask;a.chooseModel={error("Saved model must not ask for a path again")};a.runConfigured();idle(c2);assertTrue(a.configured);assertEquals(listOf(2,1,0),c2.editor.document!!.pages.map{it.masks.size});assertContentEquals(bytes,reopened.bytes(id))}
        finally{c2.close();s2.cancel()}
    }
    @Test fun defaultConfigurationCancellationInvalidModelAndStaleCommitDoNotWriteMasks()=runBlocking {
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val bytes=EditorRenderer.synthetic(128,64);val id=w.importImage("synthetic.png",bytes)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime/catalog")),root.resolve("runtime/budget"),admissionOverride=RequestAdmission{error("No services")})
        try{
            c.editor.create("guards",listOf(id));idle(c);val a=c.editor.autoMask;a.readSettings();idle(c);assertTrue(a.configured);assertEquals(TypesetAutoMaskResources.BUNDLED_SHA256,a.model!!.sha256);assertTrue(c.editor.page!!.masks.isEmpty())
            a.chooseModel={"synthetic.onnx" to LocalOnnxMaskDetectorTest.syntheticModel()};a.loadModel();idle(c);a.saveConfiguration();idle(c)
            a.runConfigured();c.active?.cancelAndJoin();assertTrue(c.editor.page!!.masks.isEmpty())
            val before=c.editor.document!!;c.editor.pageChange{it.copy(caption="changed")}
            assertFailsWith<DataProblem>{c.editor.commitAutoMasks(c.editor.project!!.id,before,before.copy(pages=before.pages.map{it.copy(masks=listOf(TypesetMask()))}))};assertTrue(c.editor.page!!.masks.isEmpty());c.editor.drain()
            val settings=TypesetEditorExtrasStore.load(w).autoMask!!;TypesetEditorExtrasStore.save(w,TypesetEditorExtrasStore.load(w).copy(autoMask=settings.copy(modelAssetId=id,modelSha256=sha256(bytes))))
            a.runConfigured();withTimeout(30000){c.active?.join()};assertNotNull(c.problem);assertTrue(c.editor.page!!.masks.isEmpty());assertContentEquals(bytes,w.bytes(id))
        }finally{c.close();scope.cancel()}
    }
}
