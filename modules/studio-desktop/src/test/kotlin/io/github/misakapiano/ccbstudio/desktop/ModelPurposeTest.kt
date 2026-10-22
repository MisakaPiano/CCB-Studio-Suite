package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import com.example.chatbar.domain.image.NovelAiTextStage
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class ModelPurposeTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun conservativeHintsDoNotCertifyUnknownModels() {
        assertEquals(ModelPurpose.EMBEDDING,modelPurpose("Qwen/Qwen3-Embedding-8B"))
        assertEquals(ModelPurpose.EMBEDDING,modelPurpose("BAAI/bge-m3"))
        assertEquals(ModelPurpose.RERANK,modelPurpose("Qwen/Qwen3-Reranker-8B"))
        assertEquals(ModelPurpose.AUDIO,modelPurpose("openai/whisper-large"))
        assertEquals(ModelPurpose.IMAGE_GENERATION,modelPurpose("Qwen/Qwen-Image"))
        assertEquals(ModelPurpose.VISION_HINT,modelPurpose("Qwen/Qwen2.5-VL-72B-Instruct"))
        assertEquals(ModelPurpose.UNKNOWN,modelPurpose("my-custom-chat-model"))
    }
    @Test fun manualEmbeddingCannotSaveOrWriteCredentials()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root())
        try {
            c.modelAccounts.newVision();c.modelAccounts.edit{it.copy(alias="wrong purpose",model="Qwen/Qwen3-Embedding-8B",key="SYNTHETIC_ONLY",useGlobalKey=false)}
            assertFalse(c.modelAccounts.canSave)
            c.modelAccounts.save(false);c.active?.join();assertNotNull(c.problem)
            assertTrue(w.state.accounts.isEmpty())
            Files.list(w.root.resolve("secrets")).use{assertEquals(0,it.count())}
        }finally{c.modelAccounts.discard();c.close();scope.cancel()}
    }
    @Test fun legacyWrongPurposeFailsBeforeBudgetVaultAndNetwork():Unit=runBlocking {
        var requests=0;var reservations=0
        val account=AccountProfile(alias="legacy",service="LLM",provider="OPENAI_COMPATIBLE",endpoint="https://fixture.invalid/v1",model="Qwen/Qwen3-Embedding-8B",isMultimodal=true)
        val http=studioHttpClient().newBuilder().addInterceptor{requests++;error("must not send")}.build()
        val transport=StudioTextTransport(account,SecretVault(root()),RequestAdmission{reservations++},http)
        assertFailsWith<IllegalArgumentException>{transport.streamText(NovelAiTextStage.PLAN,emptyList(),account.modelConfig()).collect()}
        assertEquals(0,requests);assertEquals(0,reservations)
    }
}
