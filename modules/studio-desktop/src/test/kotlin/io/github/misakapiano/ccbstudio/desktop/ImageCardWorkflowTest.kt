package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import com.example.chatbar.domain.card.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.Path
import java.util.Base64
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

class ImageCardWorkflowTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun packageBytes()=documentJson.encodeToString(CharacterCardPackage(card=PackagedCharacterCard(name="Test card",defaultImagePrompt="watercolor",defaultImageNegativePrompt="bad",defaultNovelAiImageModel=NovelAiImageModel.V5_FULL,
        systemPrompt="CHAT_FIELD_MUST_NOT_PERSIST",characters=listOf(PackagedCharacter(name="A",imagePrompt="blue dress",profile="PRIVATE_PROFILE_MUST_NOT_PERSIST"))))).toByteArray()
    @Test fun packageAndPngProduceOnlyImageProjection() {
        val raw=packageBytes();val png=PngTextChunks.insertTextChunk(EditorRenderer.synthetic(96,96),PngTextChunks.CHATBAR_CHARACTER_KEYWORD,Base64.getEncoder().encodeToString(raw))
        val a=readImageCardSource(raw);val b=readImageCardSource(png)
        assertEquals(a.copy(id=b.id,sourceSha256=b.sourceSha256),b)
        val stored=documentJson.encodeToString(a);assertFalse(stored.contains("CHAT_FIELD"));assertFalse(stored.contains("PRIVATE_PROFILE"));assertEquals(sha256(raw),a.sourceSha256)
        assertFails{readImageCardSource(raw.decodeToString().replace("\"schemaVersion\":9","\"schemaVersion\":99").toByteArray())}
    }
    @Test fun cardDefaultGlobalOverrideClearAndRestart()=runBlocking {
        val r=root();val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val w=StudioWorkspace.openStudio(r);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{error("No wire")})
        try {
            c.edit{it.copy(basePrompt="keep",followDefaultNovelAiImageModel=true)};c.flush()
            c.selectImageCard(readImageCardSource(packageBytes()));c.active!!.join();assertNull(c.problem)
            assertEquals("watercolor",c.draft.stylePrompt);assertEquals("keep",c.draft.basePrompt);assertEquals(NovelAiImageModel.V5_FULL,c.draft.selectedModel)
            assertTrue(c.draft.characters.isEmpty());assertEquals("blue dress",c.draft.importedCharacterPromptSources.single().prompt)
            c.defaults(model=NovelAiImageModel.V4_5_FULL,translated=true);c.active!!.join();assertEquals(NovelAiImageModel.V5_FULL,c.draft.selectedModel)
            c.edit{it.copy(followDefaultNovelAiImageModel=false,selectedModel=NovelAiImageModel.V4_5_FULL)};c.flush();assertEquals(NovelAiImageModel.V4_5_FULL,c.draft.selectedModel)
            c.clearPromptDraft();c.flush();assertEquals("bad",c.draft.negativePrompt);assertEquals("",c.draft.basePrompt)
            c.pastePrompt("invalid plain text");c.active!!.join();assertNotNull(c.problem);assertEquals("",c.draft.basePrompt)
            c.pastePrompt(NovelAiStudioDraft(basePrompt="pasted").copyPositivePrompt());c.active!!.join();assertNull(c.problem);assertEquals("pasted",c.draft.basePrompt)
        }finally{c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->val s=again.state.creation!!;assertTrue(s.translatedTags);assertEquals("pasted",s.draft.basePrompt);assertEquals("watercolor",s.imageCards.single().stylePrompt);assertFalse(documentJson.encodeToString(s).contains("PRIVATE_PROFILE"))}
    }
}
