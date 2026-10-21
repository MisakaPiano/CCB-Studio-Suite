@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.geometry.Offset
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

class PromptReferenceWorkflowTest {
    @Test fun editedImageUsesVerifiedSourcePromptWithoutClaimingAnUneditedResult()=runBlocking(awt) {
        val data=root();val w=StudioWorkspace.openStudio(data);val pixels=EditorRenderer.synthetic(120,90)
        val account=AccountProfile(alias="local fixture",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val draft=NovelAiStudioDraft(basePrompt="original scene")
        val wire=NovelAiImageService().buildRequestBody(draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings)
        val receipt=documentJson.encodeToString(GenerationReceipt(recipe=draft.toRecipe(),account=account,request=Json.parseToJsonElement(wire),wireSha256=sha256(wire.toByteArray())))
        val original=w.importImage("source.png",pixels,receipt)
        w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe=receipt,state="COMPLETED",results=listOf(original)))))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network")})
        var edited="";var copied="";c.gallery.copyText={copied=it}
        val scene=ImageComposeScene(1240,1100,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            c.editor.create("Edited source",listOf(original));c.idle()
            c.editor.chooseExportDirectory={Files.createDirectories(root())}
            c.editor.export(TypesetExportRequest(c.editor.document!!.pages.map{it.id}));c.idle()
            edited=TypesetExport.verify(c.editor.exportDirectory!!).assetIds.values.single()
            c.current=edited;c.selection=setOf(edited);c.view="图库";scene.frames()
            val reading=readImagePrompt(c,edited)
            assertEquals("VERIFIED_SOURCE_HISTORY",reading.status);assertEquals(original,reading.context.parameters["来源图片"])
            assertTrue(reading.explanation.contains("后续处理"));assertEquals("original scene",reading.sections.single{it.id=="base"}.text)
            scene.click("所选图片多 Prompt 参照");c.idle();scene.frames()
            val id=PromptReferences(c.library.repository).load().ids.single()
            assertTrue(scene.nodes().any{it.matches("参数来自源图 ${original.take(8)}；当前图包含后续处理。")})
            scene.click("发送请求快照 ${id.take(8)}");c.idle();scene.frames();scene.click("复制完整内容")
            assertEquals(Json.parseToJsonElement(wire),Json.parseToJsonElement(copied));scene.click("关闭");scene.shot("edited-image-source")
            assertContentEquals(pixels,w.bytes(original));assertEquals("original scene",c.historyRecipe(edited)!!.first.basePrompt)
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(data).use{again->assertEquals(original,imageOrigin(again,edited).sourceId);assertContentEquals(pixels,again.bytes(original))}
    }
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(180);render(System.nanoTime()).close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(text:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
    private suspend fun ImageComposeScene.click(text:String){val n=nodes().first{it.matches(text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun WorkbenchController.idle(){active?.join();assertNull(problem)}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/prompt-references/$name.png");Files.createDirectories(p.parent);render(System.nanoTime()).use{it.encodeToData()!!.use{Files.write(p,it.bytes)}}}
    @Test fun imagePreviewBoardRealRoleDragExtractionAndRestartKeepSourceIdentity()=runBlocking(awt) {
        val data=root();val w=StudioWorkspace.openStudio(data);val original=EditorRenderer.synthetic(160,120)
        val account=AccountProfile(alias="local fixture",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val jobs=mutableListOf<JobRecord>();val ids=listOf(false,true).map{reverse->
            val chars=listOf(NovelAiCharacterPromptDraft(prompt="red hair",negativePrompt=""),NovelAiCharacterPromptDraft(prompt="blue hair",negativePrompt=""))
            val draft=NovelAiStudioDraft(basePrompt="room, moonlight",negativePrompt="  ",characters=if(reverse)chars.reversed()else chars).withActiveSettings(NovelAiGenerationSettings(seedMode=NovelAiSeedMode.FIXED,seed=if(reverse)99 else 12))
            val wire=NovelAiImageService().buildRequestBody(draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings)
            val receipt=documentJson.encodeToString(GenerationReceipt(recipe=draft.toRecipe(),account=account,request=Json.parseToJsonElement(wire),wireSha256=sha256(wire.toByteArray())))
            val id=w.importImage("${if(reverse)"B"else"A"}.png",original,receipt);jobs+=JobRecord(account=account,recipe=receipt,state="COMPLETED",results=listOf(id));id
        }
        w.save(w.state.copy(jobs=jobs));val before=w.state.creation!!.draft
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No network authorized in this test")})
        c.view="图库";c.current=ids.first();c.selection=ids.toSet();var copied="";c.gallery.copyText={copied=it}
        val scene=ImageComposeScene(1300,1800){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        var promptIds=listOf<String>();var b="";var originalPrompt=byteArrayOf();var backup:Path?=null
        try {
            scene.frames();withTimeout(10000){while(scene.nodes().none{it.matches("保存图片 Prompt 资产")})scene.frames()}
            assertTrue(readImagePrompt(c,ids.first()).sections.none{it.text.isBlank()});scene.shot("gallery-image-prompt")
            scene.click("所选图片多 Prompt 参照");c.idle();scene.frames()
            val refs=PromptReferences(c.library.repository);promptIds=refs.load().ids;assertEquals(2,promptIds.size)
            val a=promptIds.single{c.library.repository.prompt(it).image!!.imageId==ids.first()}
            b=promptIds.single{c.library.repository.prompt(it).image!!.imageId==ids.last()};originalPrompt=w.bytes(b)
            assertEquals(a,saveImagePrompt(c,ids.first(),true));assertEquals(2,c.library.repository.prompts().size)
            assertEquals("blue hair",refs.rows().single{it.key=="role:0:角色正向"}.cells[b]!!.text)
            scene.shot("board-before-alignment")
            // Actual pointer drag, not invoking moveRole or an OnClick shortcut.
            val from=scene.nodes().first{it.matches("拖动参照角色 $b role:1")}.boundsInRoot.center
            val to=scene.nodes().first{it.matches("拖动参照角色 $b role:0")}.boundsInRoot.center-Offset(0f,12f)
            assertTrue(from.y in 0f..1800f && to.y in 0f..1800f,"Role headers must be reachable in this viewport")
            scene.sendPointerEvent(PointerEventType.Press,from,buttons=PointerButtons(isPrimaryPressed=true))
            repeat(14){i->scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*((i+1)/14f),buttons=PointerButtons(isPrimaryPressed=true));scene.frames()}
            scene.sendPointerEvent(PointerEventType.Release,to,buttons=PointerButtons());c.idle();scene.frames()
            assertEquals("red hair",refs.rows().single{it.key=="role:0:角色正向"}.cells[b]!!.text)
            assertContentEquals(originalPrompt,w.bytes(b));assertEquals(before,c.draft)
            scene.click("摘取 基础 Prompt ${a.take(8)}");scene.click("复制摘取");assertEquals("room, moonlight",copied)
            scene.click("生成参数");assertTrue(scene.nodes().any{it.matches("实际 Seed")});scene.shot("board-generation-parameters")
            scene.click("Prompt 对比");scene.click("复制摘取");assertEquals("room, moonlight",copied)
            assertEquals(listOf("role:1","role:0"),refs.roles(b));assertContentEquals(originalPrompt,w.bytes(b))
            scene.click("摘取保存为 Prompt 资产");c.idle();scene.frames()
            val excerpt=c.library.repository.prompts().single{c.library.repository.prompt(it.request().id().toString()).origin.startsWith("prompt-excerpts:")}.request().id().toString();assertEquals("room, moonlight",c.library.repository.prompt(excerpt).text);assertTrue(c.library.repository.prompt(excerpt).origin.contains("$a#base"))
            scene.click("发送请求快照 ${a.take(8)}");c.idle();scene.frames();scene.click("复制完整内容");assertEquals(documentJson.decodeFromString<GenerationReceipt>(jobs.first().recipe).request,Json.parseToJsonElement(copied));scene.click("关闭")
            scene.shot("board-aligned");assertEquals(before,c.draft);assertEquals(jobs,w.state.jobs);ids.forEach{assertContentEquals(original,w.bytes(it))}
            backup=w.backup()
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(data).use{r->assertEquals(listOf("role:1","role:0"),PromptReferences(LibraryRepository(r)).roles(b));assertContentEquals(originalPrompt,r.bytes(b))}
        val restored=root();StudioRestore.restore(backup!!,restored)
        StudioWorkspace.openStudio(restored).use{r->assertEquals(promptIds,PromptReferences(LibraryRepository(r)).load().ids);ids.forEach{assertContentEquals(original,r.bytes(it))}}
    }
    @Test fun narrowBoardScrollsSourcesAndEmptyPromptFieldsDoNotConsumeRows()=runBlocking(awt){
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        val repo=c.library.repository;val refs=PromptReferences(repo)
        val a=repo.savePrompt("left words","Left","manual",sections=listOf(PromptSection("body","基础 Prompt","left words"),PromptSection("empty","负向 Prompt","  ")))
        val b=repo.savePrompt("right words","Right","manual",sections=listOf(PromptSection("body","基础 Prompt","right words")))
        refs.add(a);refs.add(b);c.library.refresh();c.library.tab="多 Prompt 参照";c.view="资产管理"
        val scene=ImageComposeScene(600,1100){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();assertTrue(scene.nodes().none{it.matches("负向 Prompt")});assertEquals(1,refs.rows().size)
            val before=scene.nodes().first{it.matches("Right")}.boundsInRoot.left
            scene.sendPointerEvent(PointerEventType.Scroll,Offset(300f,350f),scrollDelta=Offset(8f,0f));scene.frames()
            val after=scene.nodes().first{it.matches("Right")}.boundsInRoot.left
            assertTrue(after<before,"Narrow reference columns must respond to horizontal scrolling")
            scene.shot("board-narrow-scrolled");assertEquals(listOf(a,b),refs.load().ids)
        }finally{scene.close();c.close();scope.cancel();w.close()}
    }
    @Test fun missingOrUnverifiableImagePromptCannotBecomeFalseCompleteReference()=runBlocking(awt){
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val id=w.importImage("no-prompt.png",EditorRenderer.synthetic(64,64))
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        try {
            val read=readImagePrompt(c,id);assertEquals("MISSING",read.status);assertTrue(read.sections.isEmpty());assertFails{saveImagePrompt(c,id,true)}
            assertTrue(PromptReferences(c.library.repository).load().ids.isEmpty())
            val account=AccountProfile(alias="fixture",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
            val bad=documentJson.encodeToString(GenerationReceipt(recipe=NovelAiGenerationRecipe(basePrompt="must not display"),account=account,request=JsonObject(emptyMap()),wireSha256="bad"))
            w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe=bad,state="COMPLETED",results=listOf(id)))))
            assertFails{readImagePrompt(c,id)};assertFails{saveImagePrompt(c,id,true)};assertTrue(c.library.repository.prompts().isEmpty())
        }finally{c.close();scope.cancel();w.close()}
    }
}
