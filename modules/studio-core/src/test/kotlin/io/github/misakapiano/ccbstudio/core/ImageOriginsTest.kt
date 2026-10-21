package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.test.*

class ImageOriginsTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private val account=AccountProfile(alias="local fixture",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
    private fun receipt(prompt:String):String {
        val draft=NovelAiStudioDraft(basePrompt=prompt).withActiveSettings(NovelAiGenerationSettings(seedMode=NovelAiSeedMode.FIXED,seed=37))
        val wire=NovelAiImageService().buildRequestBody(draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings)
        return documentJson.encodeToString(GenerationReceipt(recipe=draft.toRecipe(),account=account,request=Json.parseToJsonElement(wire),wireSha256=sha256(wire.toByteArray())))
    }
    @Test fun editedAndComposedImagesKeepExactSourceAndSeedAfterRestore() {
        val path=root();val bytes=EditorRenderer.synthetic(80,60);lateinit var saved:Path;var edited="";var original=""
        StudioWorkspace.openStudio(path).use {w->
            val ids=(0..1).map{w.importImage("same-name.png",bytes)};original=ids.last()
            val composed=w.importDerived("composed.png",bytes,original)
            edited=w.importDerived("edited.png",EditorRenderer.synthetic(40,40),composed)
            val job=JobRecord(account=account,recipe=receipt("source scene"),state="COMPLETED",results=ids,composedResults=listOf(composed))
            w.save(w.state.copy(jobs=listOf(job)))
            val origin=imageOrigin(w,edited);assertEquals(listOf(edited,composed,original),origin.chain);assertEquals(38L,origin.seed);assertTrue(origin.derived)
            assertEquals("source scene",origin.receipt!!.recipe.basePrompt);assertContentEquals(bytes,w.bytes(original))
            assertFalse(imageOrigin(w,original).derived);saved=w.backup()
        }
        val restored=root();StudioRestore.restore(saved,restored)
        StudioWorkspace.openStudio(restored).use{w->assertEquals(original,imageOrigin(w,edited).sourceId);assertEquals(38L,imageOrigin(w,edited).seed);assertContentEquals(bytes,w.bytes(original))}
    }
    @Test fun invalidNearestReceiptCannotFallBackToAnOlderValidAncestor() {
        StudioWorkspace.openStudio(root()).use{w->
            val bytes=EditorRenderer.synthetic(30,30);val original=w.importImage("original.png",bytes)
            val edited=w.importDerived("edited.png",bytes,original);val again=w.importDerived("again.png",bytes,edited)
            val first=JobRecord(account=account,recipe=receipt("valid ancestor"),state="COMPLETED",results=listOf(original))
            val broken=documentJson.decodeFromString<GenerationReceipt>(receipt("newest generation")).copy(wireSha256="0".repeat(64))
            val second=JobRecord(account=account,recipe=documentJson.encodeToString(broken),state="COMPLETED",results=listOf(edited))
            w.save(w.state.copy(jobs=listOf(first,second)));assertFails{imageOrigin(w,again)}
            w.save(w.state.copy(jobs=listOf(first,second.copy(recipe=receipt("newest generation")))))
            assertEquals("newest generation",imageOrigin(w,again).receipt!!.recipe.basePrompt)
            assertEquals(edited,imageOrigin(w,again).sourceId)
            w.save(w.state.copy(jobs=listOf(first,first.copy(id=newId()))));assertFails{imageOrigin(w,original)}
            assertContentEquals(bytes,w.bytes(original))
        }
    }
}
