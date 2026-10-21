@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
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

class GalleryWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render().close();yield()};delay(80);render().close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.matches(text:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(text)==true
    private suspend fun ImageComposeScene.click(text:String){val n=nodes().first{it.matches(text)&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}
    private suspend fun ImageComposeScene.field(label:String,value:String){val n=nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(it.matches(label)||it.children.any{it.matches(label)})};n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value));frames()}
    private fun ImageComposeScene.shot(name:String){val path=Path.of("build/ux-r3-gallery/$name.png");Files.createDirectories(path.parent);render().use{it.encodeToData()!!.use{Files.write(path,it.bytes)}}}
    @Test fun gridFilterTwoImageComparisonCopyAndMetadataReuseKeepOriginals()=runBlocking(awt){
        val r=root();val w=StudioWorkspace.openStudio(r);val bytes=EditorRenderer.synthetic(128,96)
        val account=AccountProfile(alias="Synthetic local",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val ids=mutableListOf<String>();val originals=mutableMapOf<String,String>();val jobs=mutableListOf<JobRecord>()
        for(prompt in listOf("red room","blue room")){val draft=NovelAiStudioDraft(basePrompt=prompt).withActiveSettings(NovelAiGenerationSettings(seedMode=NovelAiSeedMode.FIXED,seed=12))
            val wire=NovelAiImageService().buildRequestBody(draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings)
            val receipt=documentJson.encodeToString(GenerationReceipt(recipe=draft.toRecipe(),account=account,request=Json.parseToJsonElement(wire),wireSha256=sha256(wire.toByteArray())))
            val id=w.importImage("$prompt.png",bytes,receipt);ids+=id;originals[id]=receipt;jobs+=JobRecord(account=account,recipe=receipt,state="COMPLETED",results=listOf(id))
        }
        val text="Comment\u0000{\"future\":\"kept\"}".toByteArray();val type="tEXt".toByteArray();val crc=java.util.zip.CRC32().apply{update(type);update(text)}
        val chunk=java.nio.ByteBuffer.allocate(text.size+12).putInt(text.size).put(type).put(text).putInt(crc.value.toInt()).array()
        val imported=TypesetPngMetadata.injectChunks(bytes,TypesetPngMetadata.Source(listOf(chunk),null,0))
        val noRecipe=w.importImage("imported.png",imported);w.save(w.state.copy(jobs=jobs))
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")});c.view="图库"
        var copied="";c.gallery.copyText={copied=it}
        val scene=ImageComposeScene(1320,1000){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try{
            scene.frames();scene.field("查找文件名或历史 Prompt","room");scene.click("全选");assertEquals(ids.toSet(),c.selection);scene.shot("filtered-grid")
            scene.click("比较 2 张");withTimeout(10000){while(scene.nodes().none{it.matches("搜索完整字段或值")})scene.frames()};scene.field("搜索完整字段或值","/recipe/basePrompt")
            withTimeout(10000){while(scene.nodes().none{it.matches("复制 A 字段 /recipe/basePrompt")})scene.frames()}
            val left=c.gallery.compare!!.first();val right=c.gallery.compare!!.last()
            fun zoom(id:String)=scene.nodes().first{it.matches("比较图片 $id")}.config[SemanticsProperties.StateDescription].toFloat()
            val bounds=scene.nodes().first{it.matches("比较图片 $left")}.boundsInRoot
            scene.sendPointerEvent(PointerEventType.Scroll,bounds.center,scrollDelta=Offset(0f,-5f));scene.frames();assertTrue(zoom(left)>1f);assertEquals(zoom(left),zoom(right))
            scene.click("锁定两图缩放");val before=zoom(right);scene.sendPointerEvent(PointerEventType.Scroll,bounds.center,scrollDelta=Offset(0f,-5f));scene.frames();assertTrue(zoom(left)>before);assertEquals(before,zoom(right))
            scene.click("适应两图");assertEquals(1f,zoom(left));assertEquals(1f,zoom(right));scene.click("复制 A 字段 /recipe/basePrompt");assertEquals(Json.parseToJsonElement(originals.getValue(left)).jsonObject.getValue("recipe").jsonObject.getValue("basePrompt").toString(),copied)
            scene.shot("compare-fields");scene.click("A 参数选择与应用")
            withTimeout(10000){while(scene.nodes().none{it.matches("完整复现")})scene.frames()};scene.click("完整复现");scene.click("确认复用");c.active!!.join();assertNull(c.problem)
            assertEquals(Json.parseToJsonElement(originals.getValue(left)).jsonObject.getValue("recipe").jsonObject.getValue("basePrompt").jsonPrimitive.content,c.draft.basePrompt)
            val originalDraft=c.draft
            c.view="图库";c.gallery.compare=null;scene.frames();c.gallery.compare=listOf(left,right);scene.frames()
            scene.click("B 选择历史字段")
            withTimeout(10000){while(scene.nodes().none{it.matches("应用 基础 Prompt")})scene.frames()}
            scene.click("应用 基础 Prompt");scene.click("返回比较");assertEquals(originalDraft,c.draft);assertTrue(w.state.creation!!.recipeApplications.isEmpty())
            scene.click("B 选择历史字段");withTimeout(10000){while(scene.nodes().none{it.matches("应用 基础 Prompt")})scene.frames()}
            scene.click("应用 基础 Prompt");scene.shot("selected-recipe-fields");scene.click("确认应用 1 个字段");c.active!!.join();assertNull(c.problem)
            val expected=Json.parseToJsonElement(originals.getValue(right)).jsonObject.getValue("recipe").jsonObject.getValue("basePrompt").jsonPrimitive.content
            assertEquals(originalDraft.copy(basePrompt=expected),c.draft)
            assertEquals(right,w.state.creation!!.recipeApplications.single().sourceId)
            c.undo();c.active!!.join();assertEquals(originalDraft,c.draft);c.undo(true);c.active!!.join();assertEquals(expected,c.draft.basePrompt)
            val applied=c.draft;c.edit{it.copy(basePrompt="concurrent edit")};c.applyRecipeFields(left,setOf(RecipeDraftField.BASE),applied);c.active!!.join()
            assertNotNull(c.problem);assertEquals("concurrent edit",c.draft.basePrompt);assertEquals(1,w.state.creation!!.recipeApplications.size)
            c.undo();c.active!!.join();assertEquals(applied,c.draft)
            c.view="图库";c.gallery.metadata=null;c.gallery.compare=listOf(ids.first(),noRecipe);scene.frames();withTimeout(10000){while(scene.nodes().none{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text.startsWith("Studio Recipe MISSING")}==true})scene.frames()}
            scene.field("搜索完整字段或值","/pngMetadata/textChunks/0/parsedJson/future")
            scene.click("复制 B 字段 /pngMetadata/textChunks/0/parsedJson/future");assertEquals("\"kept\"",copied);assertContentEquals(imported,w.bytes(noRecipe));scene.shot("png-metadata-comparison")
            scene.click("返回图库");assertEquals("room",c.gallery.query);ids.forEach{assertContentEquals(bytes,w.bytes(it));assertEquals(originals[it],w.assets.read(java.util.UUID.fromString(it)).require().request().recipe().decodeToString())}
        }finally{scene.close();c.close();scope.cancel();w.close()}
        StudioWorkspace.openStudio(r).use{assertTrue(it.state.creation!!.draft.basePrompt.endsWith("room"));assertEquals(1,it.state.creation!!.recipeApplications.size);ids.forEach{id->assertContentEquals(bytes,it.bytes(id))}}
    }
}
