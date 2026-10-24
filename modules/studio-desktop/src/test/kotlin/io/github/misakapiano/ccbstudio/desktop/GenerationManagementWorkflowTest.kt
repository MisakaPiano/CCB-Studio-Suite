@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GenerationManagementWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun detachedHistoryDialogStaysWithBatchWindow()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(root)
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root),root,admissionOverride=RequestAdmission{error("No real calls")},pauseCollectionsOnStart=true)
        c.batch.detached=true;c.batch.shown=true
        val main=ImageComposeScene(1320,880){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        val batch=ImageComposeScene(1180,820){MaterialTheme{GenerationManagementDialogs(c,true);BatchWorkspace(c,true)}}
        fun nodes(scene:ImageComposeScene):List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun has(scene:ImageComposeScene,text:String)=nodes(scene).any{it.config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true}
        suspend fun frames(){repeat(3){main.render(System.nanoTime()).close();batch.render(System.nanoTime()).close();yield()};delay(80)}
        try{
            frames();c.generations.openHistory(true);frames()
            assertFalse(has(main,"暂无归档批次"));assertTrue(has(batch,"暂无归档批次"))
            nodes(batch).first{it.config.getOrNull(SemanticsProperties.Text)?.any{it.text=="关闭"}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()
            assertFalse(c.generations.history);assertFalse(has(batch,"暂无归档批次"))
        }finally{batch.close();main.close();c.close();scope.cancel();w.close()}
    }
    @Test fun realCurrentSessionNamingArchiveAndProjectTreePreserveImages()=runBlocking(awt){
        val root=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(root);val bytes=EditorRenderer.synthetic(48,48)
        val repo=GenerationOrganizations(w);val batch=repo.ensureManual();val project=GenerationFolder(name="插画集");repo.addFolder(project);val chapter=GenerationFolder(name="第一章",kind="chapter",parent=project.id);repo.addFolder(chapter)
        val account=AccountProfile(alias="Synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val raw=documentJson.encodeToString(GenerationReceipt(recipe=NovelAiStudioDraft(basePrompt="portrait, synthetic").toRecipe(),account=account,request=buildJsonObject{},manualBatchId=batch.id))
        val ids=(1..4).map{w.importImage("manual-$it.png",bytes,raw)};repeat(16){w.importImage("unrelated-$it.png",bytes)}
        w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe=raw,state="COMPLETED",results=ids,manualBatchId=batch.id))))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root),root,admissionOverride=RequestAdmission{error("No real calls")},pauseCollectionsOnStart=true)
        val scene=ImageComposeScene(1320,880){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(3){scene.render(System.nanoTime()).close();yield()};delay(80)}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun match(n:SemanticsNode,text:String)=n.config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true||n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
        suspend fun click(text:String){nodes().first{match(it,text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()}
        suspend fun idle(){c.active?.join();assertNull(c.problem);frames()}
        fun shot(name:String){val file=Path.of("build/organization-115/$name.png");Files.createDirectories(file.parent);scene.render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(file,it.bytes)}}}
        try{
            frames();assertTrue(nodes().any{match(it,"本次手动生成 4 张 · 原始字节保全")});assertEquals(ids,repo.manualImages());assertEquals(20,c.assets.size);shot("current-manual")
            click("保存并归档");click("取消");assertEquals(ids,repo.manualImages());click("保存并归档")
            val field=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(match(it,"自定义名称")||it.children.any{n->match(n,"自定义名称")})}
            field.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("光影练习"));frames()
            nodes().first{n->n.config.getOrNull(SemanticsProperties.ContentDescription)?.any{it.startsWith("上级组织 · ")}==true&&n.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames();click("插画集 / 第一章")
            click("新建组织…");assertFalse(nodes().any{match(it,"保存并归档手动批次")});click("取消")
            assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.EditableText)?.text=="光影练习"})
            click("新建组织…")
            nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(match(it,"组织名称")||it.children.any{n->match(n,"组织名称")})}.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString("归档新章节"));frames()
            click("创建组织");idle();val inlineFolder=repo.state.folders.single{it.name=="归档新章节"};assertEquals(chapter.id,inlineFolder.parent);assertEquals("chapter",inlineFolder.kind)
            assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.EditableText)?.text=="光影练习"});shot("archive-inline-organization")
            nodes().last{match(it,"保存并归档")&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();idle();assertNull(repo.state.currentManual);assertNull(c.current);assertTrue(nodes().any{match(it,"本次手动生成 0 张 · 原始字节保全")});assertEquals(20,c.assets.size)
            assertEquals(inlineFolder.id,repo.state.batches.single().parent)
            c.view="图库";frames();withTimeout(15000){while(c.gallery.historyCache==null||c.gallery.foldPreferences==null)frames()}
            assertTrue(c.gallery.expandedOrganizations.isEmpty());assertFalse(nodes().any{match(it,"插画集")});click("展开项目与章节");click("展开插画集");click("展开第一章");click("展开归档新章节");assertTrue(nodes().any{match(it,"光影练习 · "+batch.title.substringAfterLast(" · "))});shot("project-chapter-tree")
            click("插画集");assertEquals("folder",c.gallery.organizationKind);assertTrue(nodes().any{match(it,"第一章")})
            c.library.changeOrganization{it.copy(batches=it.batches.map{b->b.copy(parent=project.id)})};idle();c.library.undo();idle();assertEquals(inlineFolder.id,repo.state.batches.single().parent);c.library.redo();idle();assertEquals(project.id,repo.state.batches.single().parent)
            ids.forEach{assertContentEquals(bytes,w.bytes(it))};assertEquals(raw,w.state.jobs.single().recipe)
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(root).use{again->assertNull(GenerationOrganizations(again).state.currentManual);assertEquals(20,SharedAssets.images(again).size);assertEquals("光影练习",again.state.generationOrganization.batches.single().alias)}
    }
}
