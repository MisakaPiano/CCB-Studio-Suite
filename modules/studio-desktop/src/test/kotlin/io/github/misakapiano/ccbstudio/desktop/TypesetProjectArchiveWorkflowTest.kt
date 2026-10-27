@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import io.github.misakapiano.ccbstudio.core.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class TypesetProjectArchiveWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun controller(w:StudioWorkspace,scope:CoroutineScope,r:Path)=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("catalog")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No service calls")})
    private suspend fun idle(c:WorkbenchController){withTimeout(30000){c.active?.join()};assertNull(c.problem,c.problem)}
    private suspend fun ImageComposeScene.frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close();yield()};delay(100);androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true||config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){if(nodes().none{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}&&(s in setOf("打开本机工程","收起当前工程","导入工程文件")||s.startsWith("历史工程 · ")))click("工程菜单");val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.enter(s:String,value:String){val n=nodes().first{it.matches(s)&&it.config.getOrNull(SemanticsActions.SetText)!=null};assertTrue(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)));frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of(System.getProperty("preview.test.root")).resolve("editor-project-archive/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{image->image.encodeToData()!!.use{Files.write(p,it.bytes)}}}

    @Test fun actualArchiveUiNamesOrganizesClearsAndReopensSameProjectWithHistory()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val bytes=EditorRenderer.synthetic(480,320);val asset=w.importImage("synthetic-archive.png",bytes)
        val parent=GenerationFolder(name="Review Project");val chapter=GenerationFolder(name="Chapter",kind="chapter",parent=parent.id);w.save(w.state.copy(generationOrganization=GenerationOrganization(folders=listOf(parent,chapter))))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,root.resolve("runtime"));c.editor.create("Synthetic Review",listOf(asset,asset));idle(c)
        c.editor.selectPage(c.editor.document!!.pages.last().id);c.editor.pageChange{it.copy(texts=listOf(TypesetText(text="SYNTHETIC ARCHIVE")),masks=listOf(TypesetMask(effect="black")))};c.editor.drain()
        val document=c.editor.document!!;val before=c.editor.project!!;val originalIds=SharedAssets.images(w).map{it.request().id()}.toSet()
        val scene=ImageComposeScene(1320,960,coroutineContext=awt){StudioTheme{Surface{TypesetWorkspace(c)}}}
        try{
            scene.frames();scene.click("命名并归档");scene.enter("归档工程名称","not saved");scene.click("取消归档");assertEquals(before,c.editor.project)
            scene.click("命名并归档");scene.enter("归档工程名称","Archived Review");scene.click("展开或收起组织 · Review Project");scene.click("组织节点 · Review Project / Chapter");scene.shot("archive-dialog")
            scene.click("保存并归档");idle(c);scene.frames();assertNull(c.editor.document);assertNull(c.editor.project);assertTrue(c.editor.archives.active().isEmpty());assertEquals(1,w.state.typesetProjects.size)
            val archived=c.editor.archives.archived().single();assertEquals(before.id,archived.id);assertEquals(before.undo,archived.undo);assertEquals(before.redo,archived.redo);assertEquals(before.current,archived.current);assertEquals(chapter.id,c.editor.archives.record(archived.id)!!.parent)
            scene.shot("archive-cleared-editor");scene.click("历史工程 · 1")
            withTimeout(30000){while(scene.nodes().none{it.matches("重新打开工程")&&it.config.getOrNull(SemanticsProperties.Disabled)==null})scene.frames()}
            scene.shot("archive-history");assertNull(c.editor.project,"Viewing a historical cover must not open/edit its project")
            scene.click("重新打开工程");idle(c);scene.frames();assertEquals(before.id,c.editor.project!!.id);assertEquals(document,c.editor.document);assertEquals(before.currentPageId,c.editor.pageId);assertEquals(before.undo,c.editor.project!!.undo);assertEquals(before.redo,c.editor.project!!.redo)
            assertEquals(originalIds,SharedAssets.images(w).map{it.request().id()}.toSet());assertContentEquals(bytes,w.bytes(asset));scene.shot("archive-reopened-editor")
        }finally{scene.close();c.close();scope.cancel()}
    }
    @Test fun archivedEmptySelectionSurvivesRestartAndOtherActiveProjectIsRetained()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val bytes=EditorRenderer.synthetic(120,80);val id=w.importImage("synthetic.png",bytes);val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,root.resolve("runtime"))
        c.editor.create("Other active",listOf(id));idle(c);val other=c.editor.project!!;c.editor.create("Archive me",listOf(id));idle(c);val archivedId=c.editor.project!!.id
        c.editor.archiveCurrent("Named",null);idle(c);assertNull(c.editor.project);assertEquals(listOf(other.id),c.editor.archives.active().map{it.id});c.editor.resumeLastProject();idle(c);assertNull(c.editor.project);c.close();scope.cancel()
        val again=StudioWorkspace.openStudio(root.resolve("workspace"));val scope2=CoroutineScope(SupervisorJob()+awt);val next=controller(again,scope2,root.resolve("runtime2"))
        try{
            next.editor.resumeLastProject();idle(next);assertNull(next.editor.project,"Archiving must persist an intentionally empty current editor")
            next.editor.open(other.id);idle(next);next.editor.reopenArchived(archivedId);idle(next);assertEquals(archivedId,next.editor.project!!.id);assertEquals(2,next.editor.archives.active().size);assertTrue(next.editor.archives.archived().isEmpty());assertContentEquals(bytes,again.bytes(id))
            next.editor.manageProject("remove");idle(next);assertNull(next.editor.project);assertEquals("REMOVED",next.editor.archives.record(archivedId)!!.state);assertEquals(2,again.state.typesetProjects.size);assertEquals(setOf(id),next.editor.archives.resources(archivedId).originals)
        }finally{next.close();scope2.cancel()}
    }
    @Test fun archiveFailureDoesNotClearCurrentEditorOrDiscardDraft()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val w=StudioWorkspace.openStudio(root.resolve("workspace"));val id=w.importImage("synthetic.png",EditorRenderer.synthetic(120,80));val scope=CoroutineScope(SupervisorJob()+awt);val c=controller(w,scope,root.resolve("runtime"))
        try{
            c.editor.create("keep",listOf(id));idle(c);c.editor.drain();val p=c.editor.project!!
            TypesetRepository(w).update(p.id,p.revision){it.copy(startNumber=2)};c.editor.pageChange{it.copy(caption="retained unsaved draft")};val draft=c.editor.document
            c.editor.archiveCurrent("Not archived",null);withTimeout(30000){c.active?.join()};assertNotNull(c.problem);assertEquals(p.id,c.editor.project!!.id);assertEquals(draft,c.editor.document);assertTrue(c.editor.dirty);assertTrue(c.editor.archives.archived().isEmpty())
        }finally{scope.cancel();w.close()}
    }
}
