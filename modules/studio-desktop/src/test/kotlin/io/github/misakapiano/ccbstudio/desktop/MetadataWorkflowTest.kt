@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import java.io.ByteArrayOutputStream
import java.nio.file.*
import java.util.Base64
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class MetadataWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun png(comment:String):ByteArray {
        val image=DesktopImageEditing.decode(EditorRenderer.synthetic(96,96))
        val writer=ImageIO.getImageWritersByFormatName("png").next()
        return try {ByteArrayOutputStream().use{out->ImageIO.createImageOutputStream(out).use{stream->writer.output=stream
            val meta=writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier(image),writer.defaultWriteParam)
            val tree=IIOMetadataNode("javax_imageio_png_1.0");val text=IIOMetadataNode("tEXt");val entry=IIOMetadataNode("tEXtEntry")
            entry.setAttribute("keyword","Comment");entry.setAttribute("value",comment);text.appendChild(entry);tree.appendChild(text);meta.mergeTree("javax_imageio_png_1.0",tree)
            writer.write(null,javax.imageio.IIOImage(image,null,meta),writer.defaultWriteParam)
        };out.toByteArray()}}finally{writer.dispose();image.flush()}
    }
    private fun comment(extra:JsonObject=buildJsonObject{})=JsonObject(buildJsonObject{put("prompt","imported room");put("uc","bad");put("seed",123);put("width",512);put("height",512);put("model","nai-diffusion-4-5-full");put("steps",24);put("sampler","k_euler_ancestral")}+extra).toString()
    private suspend fun ImageComposeScene.frames(){repeat(4){render().close();yield()};delay(70);render().close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private suspend fun ImageComposeScene.click(label:String){val n=nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==label}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames()}

    @Test fun actualMetadataUiPreviewApplyUndoAndRestart()=runBlocking(awt) {
        val r=root();val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No HTTP")})
        val bytes=png(comment());val id=w.importImage("metadata.png",bytes)
        c.edit{it.copy(basePrompt="old input")};c.flush()
        val scene=ImageComposeScene(1100,1800){MaterialTheme(colors=darkColors()){MetadataWorkspace(c,id){}}}
        try {
            withTimeout(10000){while(scene.nodes().none{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text=="确认导入所选字段"}==true})scene.frames()}
            assertEquals("old input",c.draft.basePrompt)
            val shot=Path.of("build/ux-r1b-metadata/selection-before-apply.png");Files.createDirectories(shot.parent);scene.render().use{image->image.encodeToData()!!.use{Files.write(shot,it.bytes)}}
            scene.click("确认导入所选字段");c.active!!.join();assertNull(c.problem);assertEquals("imported room",c.draft.basePrompt);assertEquals(123L,c.draft.activeSettings.seed)
            c.undo();c.active!!.join();assertEquals("old input",c.draft.basePrompt)
            c.undo(true);c.active!!.join();assertEquals("imported room",c.draft.basePrompt);assertContentEquals(bytes,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{assertEquals("imported room",it.state.creation!!.draft.basePrompt);assertContentEquals(bytes,it.bytes(id))}
    }
    @Test fun embeddedImageRetainedAndUsedWithoutMutatingOriginal()=runBlocking {
        val r=root();val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val w=StudioWorkspace.openStudio(r);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val embedded=EditorRenderer.synthetic(128,128);val bytes=png(comment(buildJsonObject{put("action","img2img");put("image",Base64.getEncoder().encodeToString(embedded));put("strength",0.6);put("noise",0.1)}));val id=w.importImage("embedded.png",bytes)
        var base=""
        try {c.importMetadata(id,NovelAiStudioMetadataSelection());c.active!!.join();assertNull(c.problem)
            base=c.draft.imageGuidance.baseImage!!.id;assertEquals(NovelAiGenerationAction.IMAGE_TO_IMAGE,c.draft.imageGuidance.action)
            assertContentEquals(bytes,w.bytes(id));assertContentEquals(embedded,w.bytes(base))
        }finally{c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{assertEquals(base,it.state.creation!!.draft.imageGuidance.baseImage!!.id);assertContentEquals(embedded,it.bytes(base))}
    }
    @Test fun missingMalformedUnsupportedAndReadFailureAreDistinct() {
        val r=root();Files.createDirectories(r)
        fun inspect(name:String,bytes:ByteArray)=inspectMetadata(r.resolve(name).also{Files.write(it,bytes)}).status
        assertEquals("MISSING",inspect("plain.png",EditorRenderer.synthetic(96,96)))
        assertEquals("CORRUPT",inspect("bad.png",png("{broken")))
        assertEquals("UNSUPPORTED",inspect("new.png",png("{\"future\":true}")))
        assertEquals("READABLE",inspect("good.png",png(comment())))
        assertEquals("READ_ERROR",inspectMetadata(r.resolve("absent.png")).status)
    }
    @Test fun historyReuseChecksIdentityAndRetainsOriginal()=runBlocking {
        val r=root();val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val w=StudioWorkspace.openStudio(r);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val a=AccountProfile(alias="Local",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val d=NovelAiStudioDraft(basePrompt="history").withActiveSettings(NovelAiGenerationSettings(seedMode=NovelAiSeedMode.FIXED,seed=123))
        val wire=NovelAiImageService().buildRequestBody(d.toPromptPlan(),d.activeSettings.imageSize(),d.activeSettings)
        val receipt=documentJson.encodeToString(GenerationReceipt(recipe=d.toRecipe(),account=a,request=Json.parseToJsonElement(wire),wireSha256=sha256(wire.toByteArray())))
        val bytes=EditorRenderer.synthetic(96,96);val id=w.importImage("history.png",bytes,receipt)
        w.save(w.state.copy(jobs=listOf(JobRecord(account=a,recipe=receipt,state="COMPLETED",results=listOf(id)))))
        try {
            c.reuseHistory(id,NovelAiHistoryApplyMode.FULL);c.active!!.join();assertNull(c.problem);assertEquals("history",c.draft.basePrompt);assertEquals(123L,c.draft.activeSettings.seed)
            c.edit{it.copy(basePrompt="keep")};c.flush();c.reuseHistory(id,NovelAiHistoryApplyMode.SEED_ONLY);c.active!!.join();assertEquals("keep",c.draft.basePrompt)
            c.reuseHistory(id,NovelAiHistoryApplyMode.NEW_SEED);c.active!!.join();assertEquals(NovelAiSeedMode.RANDOM,c.draft.activeSettings.seedMode)
            val job=w.state.jobs.single();val corrupt=receipt.replace(sha256(wire.toByteArray()),"0".repeat(64));w.save(w.state.copy(jobs=listOf(job.copy(recipe=corrupt))))
            c.reuseHistory(id,NovelAiHistoryApplyMode.FULL);c.active!!.join();assertNotNull(c.problem);assertEquals(NovelAiSeedMode.RANDOM,c.draft.activeSettings.seedMode)
            assertContentEquals(bytes,w.bytes(id))
        }finally{c.close();scope.cancel()}
    }
}
