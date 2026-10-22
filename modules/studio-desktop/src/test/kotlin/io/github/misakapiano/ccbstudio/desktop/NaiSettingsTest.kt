@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class NaiSettingsTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun unknownAndNonFiniteAllowanceNeverPretendToBeFull() {
        assertNull(naiAllowanceDisplay(null).fraction)
        assertNull(naiAllowanceDisplay(NovelAiAccountUsage(50,3,true,Double.NaN,false)).fraction)
        assertNull(naiAllowanceDisplay(NovelAiAccountUsage(50,2,true,49.0,false)).fraction)
        assertEquals(0.49f,naiAllowanceDisplay(NovelAiAccountUsage(50,3,true,49.0,false)).fraction)
        assertTrue(naiAllowanceDisplay(NovelAiAccountUsage(50,3,true,49.0,false)).text.contains("848"))
        assertEquals(0f,naiAllowanceDisplay(NovelAiAccountUsage(50,3,true,49.0,true)).fraction)
        assertEquals(1f,naiAllowanceDisplay(NovelAiAccountUsage(50,3,true,150.0,false)).fraction)
    }
    @Test fun naiAccountDoesNotOwnGenerationModelAndOnlyV5ShowsAllowance()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root())
        val scene=ImageComposeScene(1280,1100,coroutineContext=awt){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(4){scene.render(System.nanoTime()).close();yield()};delay(80)}
        fun nodes():List<SemanticsNode> {
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun texts()=nodes().flatMap{it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.map{it.text}
        try {
            c.saveAccount(null,"Fixture NAI","NAI","https://image.novelai.net",NovelAiImageModel.V4_5_FULL.apiId,"SYNTHETIC_ONLY");c.active?.join()
            c.edit{it.copy(selectedModel=NovelAiImageModel.V5_FULL,followDefaultNovelAiImageModel=false)}
            frames();assertTrue(texts().any{it=="当前生图模型：V5 Full"})
            assertTrue(texts().any{it.contains("V5 Opus 体力：未知")})
            assertTrue(texts().none{it.contains("nai-diffusion-4-5-full")})
            c.view="账户";c.modelAccounts.category="NAI 生图";c.modelAccounts.open(c.state.accounts.single());frames()
            assertTrue(texts().none{it=="模型 ID" || it=="HTTPS API 地址"})
            assertTrue(texts().any{it=="保存并选用此账户"})
            assertTrue(texts().any{it.contains("NAI 生图：Fixture NAI · V5 Full")})
            c.view="创作";c.edit{it.copy(selectedModel=NovelAiImageModel.V4_5_FULL,followDefaultNovelAiImageModel=false)}
            frames();assertTrue(texts().none{it.contains("V5 Opus 体力")})
            c.defaults(model=NovelAiImageModel.V5_FULL);c.active?.join();frames()
            assertEquals(NovelAiImageModel.V4_5_FULL,c.draft.selectedModel)
            assertTrue(texts().any{it.contains("本次已指定 V4.5 Full")})
            assertNull(c.problem)
        } finally {scene.close();c.modelAccounts.discard();c.close();scope.cancel()}
    }
    @Test fun actualBundledChineseAnnotationsKeepTheRawPrompt()=runBlocking {
        val infrastructure=DesktopNovelAiInfrastructure(root())
        try {
            val raw="blue sky, cloud"
            val before=infrastructure.translations
            val annotations=before.resolve(NovelAiPromptTranslationParser.parse(raw,false)).annotations
            assertTrue(annotations.isNotEmpty(),"Bundled dictionary/tag lookup must produce annotations")
            assertEquals("blue sky, cloud",raw)
            infrastructure.refreshTranslations()
            assertNotSame(before,infrastructure.translations)
            assertEquals(annotations,infrastructure.translations.resolve(NovelAiPromptTranslationParser.parse(raw,false)).annotations)
        } finally {infrastructure.closeAndDrain()}
    }
}
