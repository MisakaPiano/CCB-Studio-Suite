@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetProjectManagementWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun controller(w:StudioWorkspace,scope:CoroutineScope,r:Path)=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("catalog")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No service calls")})
    private suspend fun idle(c:WorkbenchController){withTimeout(30000){c.active?.join()};assertNull(c.problem,c.problem)}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}}
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(70);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}
    private suspend fun ImageComposeScene.click(s:String){if(nodes().none{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}&&(s in setOf("打开本机工程","收起当前工程","导入工程文件")||s.startsWith("历史工程 · ")))click("工程菜单");val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.enter(s:String,value:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.SetText)!=null};assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)));frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of(System.getProperty("preview.test.root")).resolve("editor-project-management/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{image->image.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun visibleCreateTreeCancelAndDeepPlacementThenCollapsePreserveExactDocumentAndRestartSelection()=runBlocking(awt){
        val r=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(480,320);val id=w.importImage("synthetic.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,r.resolve("runtime"));c.selection=setOf(id)
        val scene=ImageComposeScene(1320,960,coroutineContext=awt){StudioTheme{Surface{TypesetWorkspace(c)}}}
        try{
            scene.frames();idle(c);scene.click("新建编辑工程");scene.enter("工程名称","Cancelled");scene.click("新建下级");scene.enter("新节点名称","Not committed");scene.click("保存节点");scene.click("取消新建");assertTrue(w.state.generationOrganization.folders.isEmpty());assertTrue(w.state.typesetProjects.isEmpty())
            scene.click("新建编辑工程");scene.enter("工程名称","Deep project")
            for(name in listOf("Project","Volume","Chapter","Section")){scene.click("新建下级");scene.enter("新节点名称",name);scene.click("保存节点")}
            scene.shot("create-deep-tree");scene.click("建立工程");idle(c);scene.frames();val project=c.editor.project!!;assertEquals(4,w.state.generationOrganization.folders.size);assertEquals("Project / Volume / Chapter / Section",w.state.generationOrganization.path(TypesetProjectArchives(w).record(project.id)!!.parent))
            c.editor.pageChange{it.copy(texts=listOf(TypesetText(text="SAVE ON COLLAPSE")))};val document=c.editor.document!!
            scene.click("收起当前工程");idle(c);scene.frames();assertNull(c.editor.project);assertEquals(document,TypesetRepository(w).load(w.state.typesetProjects.single()));assertNull(TypesetEditorExtrasStore.load(w).lastProjectId)
            scene.click("打开本机工程");scene.shot("open-local-projects");scene.click("打开本机工程 · Deep project");idle(c);scene.frames();assertEquals(project.id,c.editor.project!!.id);assertEquals(document,c.editor.document);assertContentEquals(bytes,w.bytes(id));scene.shot("direct-project-actions")
            c.editor.collapseCurrent();idle(c);c.editor.resumeLastProject();idle(c);assertNull(c.editor.project)
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r.resolve("workspace")).use{again->assertNull(TypesetEditorExtrasStore.load(again).lastProjectId);assertEquals(1,TypesetProjectArchives(again).active().size)}
    }
    @Test fun visibleFullExportHelpAndNativePickerHookRoundTripKeepOriginalBytesAndCurrentEditing(){runBlocking(awt){
        val r=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(200,120);val id=w.importImage("synthetic.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,r.resolve("runtime"));c.editor.create("Portable",listOf(id));idle(c);c.editor.pageChange{it.copy(texts=listOf(TypesetText(text="CONTINUE ME")),masks=listOf(TypesetMask(effect="black")))};c.editor.drain();val previous=c.editor.project!!.id
        val file=r.resolve("Portable.typesetter-workspace.json");c.editor.chooseExchangeFile={file}
        val scene=ImageComposeScene(1320,960,coroutineContext=awt){StudioTheme{Surface{TypesetWorkspace(c)}}}
        try{
            scene.frames();scene.click("导出完整工程");assertTrue(scene.nodes().any{it.config.getOrNull(SemanticsProperties.Text)?.any{t->"本机撤销／重做历史" in t.text}==true});scene.shot("full-export-help");scene.click("选择位置并导出完整工程");idle(c);scene.frames();assertTrue(Files.exists(file));val inspection=TypesetSourceExchange.inspect(Files.readAllBytes(file));assertContentEquals(bytes,inspection.originals.single());assertEquals("CONTINUE ME",inspection.document.pages.single().texts.single().text)
            scene.click("导入工程文件");scene.click("选择外部工程文件");idle(c);scene.frames();assertNotNull(c.editor.exchangeInspection);scene.click("导入为新工程");idle(c);scene.frames();assertNotEquals(previous,c.editor.project!!.id);assertEquals("CONTINUE ME",c.editor.document!!.pages.single().texts.single().text);assertEquals(2,w.state.typesetProjects.size);assertTrue(c.editor.project!!.undo.isEmpty());assertEquals(c.editor.project!!.id,TypesetEditorExtrasStore.load(w).lastProjectId);assertContentEquals(bytes,w.bytes(c.editor.document!!.pages.single().assetId))
        }finally{scene.close();c.close();scope.cancel()}
    }}
    @Test fun confirmedCurrentDiscardCancelsUnwrittenDraftPhysicallyRetiresProgressAndDefaultsKeepOutputs()=runBlocking(awt){
        val r=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(160,100);val id=w.importImage("original.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,r.resolve("runtime"));c.editor.create("Keep other",listOf(id));idle(c);val other=c.editor.project!!;c.editor.create("Discard current",listOf(id));idle(c);val p=c.editor.project!!
        val dir=Files.createDirectories(r.resolve("workspace/exports/synthetic"));val file=dir.resolve("keep-by-default.txt");Files.writeString(file,"synthetic output retained by default");val content=Files.readAllBytes(file);TypesetProjectDeletion(w).register(p.id,listOf(TypesetManagedOutput(projectId=p.id,path=file.toString(),size=content.size.toLong(),sha256=sha256(content))))
        val scene=ImageComposeScene(1320,960,coroutineContext=awt){StudioTheme{Surface{TypesetWorkspace(c)}}}
        try{
            scene.frames();c.editor.pageChange{it.copy(caption="UNWRITTEN DRAFT MUST NOT BE SAVED")};c.editor.prepareDeletion(p.id);idle(c);scene.frames();assertTrue(c.editor.dirty);assertEquals(p.revision,w.state.typesetProjects.single{it.id==p.id}.revision)
            assertNotNull(c.editor.deletionPlan);assertTrue(scene.nodes().first{it.matches("确认废弃工程")}.config.getOrNull(SemanticsProperties.Disabled)!=null);scene.shot("discard-confirm-default")
            scene.click("取消删除");assertEquals(p.id,c.editor.project!!.id);assertTrue(c.editor.dirty);c.editor.prepareDeletion(p.id);idle(c);scene.frames();scene.click("确认不再需要工程进度");scene.click("确认废弃工程");idle(c);scene.frames()
            assertNull(c.editor.project);assertNull(c.editor.document);assertEquals(listOf(other.id),w.state.typesetProjects.map{it.id});assertTrue(Files.exists(file));assertContentEquals(bytes,w.bytes(id));assertEquals("COMPLETE",TypesetEditorExtrasStore.load(w).retirements.single().state);assertEquals(other.current,w.state.typesetProjects.single().current);assertNull(c.editor.deletionPlan)
            val retired=TypesetEditorExtrasStore.load(w).retirements.single();assertTrue(retired.assets.isNotEmpty());retired.assets.forEach{assertFalse(Files.exists(w.root.resolve("assets/objects/${it.sha256}.blob")))};scene.shot("discard-completed-empty")
        }finally{scene.close();c.close();scope.cancel()}
    }
    @Test fun archiveDeleteRequiresScopeConfirmationAndOnlySelectedOwnedOutputIsDeleted()=runBlocking(awt){
        val r=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"));val bytes=EditorRenderer.synthetic(160,100);val id=w.importImage("original.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,r.resolve("runtime"));c.editor.create("Archive deletion",listOf(id));idle(c);val p=c.editor.project!!;val dir=Files.createDirectories(w.root.resolve("exports/synthetic"));val owned=dir.resolve("owned.txt");val shared=dir.resolve("shared.txt");Files.writeString(owned,"synthetic owned");Files.writeString(shared,"synthetic protected")
        fun record(file:Path,asset:String?=null)=Files.readAllBytes(file).let{TypesetManagedOutput(projectId=p.id,path=file.toString(),size=it.size.toLong(),sha256=sha256(it),assetId=asset)}
        TypesetProjectDeletion(w).register(p.id,listOf(record(owned),record(shared,id)));c.editor.archiveCurrent("Archive deletion",null);idle(c)
        val scene=ImageComposeScene(1320,960,coroutineContext=awt){StudioTheme{Surface{TypesetWorkspace(c)}}}
        try{
            scene.frames();scene.click("历史工程 · 1");withTimeout(30000){while(scene.nodes().none{it.matches("删除归档工程…")})scene.frames()};scene.click("删除归档工程…");idle(c);scene.frames();scene.click("选择删除生成文件");scene.click("删除文件 · owned.txt");assertNotNull(scene.nodes().first{it.matches("删除文件 · shared.txt")}.config.getOrNull(SemanticsProperties.Disabled));scene.shot("archive-delete-selected-output")
            scene.click("确认不再需要工程进度");scene.click("确认废弃工程");idle(c);scene.frames();assertTrue(w.state.typesetProjects.isEmpty());assertFalse(Files.exists(owned));assertTrue(Files.exists(shared));assertContentEquals(bytes,w.bytes(id));assertTrue(c.editor.archives.archived().isEmpty());assertEquals("DELETED",TypesetEditorExtrasStore.load(w).outputs.single{it.path==owned.toString()}.status)
        }finally{scene.close();c.close();scope.cancel()}
    }

}
