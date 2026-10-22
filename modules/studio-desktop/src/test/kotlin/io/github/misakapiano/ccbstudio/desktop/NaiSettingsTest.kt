@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.input.key.*
import androidx.compose.runtime.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class NaiSettingsTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun singleUserDefaultHasNoFiveMinuteOrHourlyReservationGate()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt);val budget=root()
        var requests=0
        val http=OkHttpClient.Builder().addInterceptor{chain->requests++;Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body("""{"trainingStepsLeft":1000,"tier":3,"active":true}""".toResponseBody()).build()}.build()
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),budget,http)
        try {
            c.saveAccount(null,"No-gate fixture","NAI","https://image.novelai.net",NovelAiImageModel.V5_FULL.apiId,"SYNTHETIC_ONLY");c.active?.join()
            withTimeout(5000){repeat(2){c.refreshUsage();c.active?.join();assertNull(c.problem)}}
            assertEquals(2,requests);assertFalse(java.nio.file.Files.exists(budget.resolve(".budget.lock")))
            assertFalse(java.nio.file.Files.exists(budget.resolve("00000001.json")))
        }finally{c.close();scope.cancel()}
    }
    @Test fun buttonShowsAuthoritativeCostIncludingExtraFeesWithoutPromisingUnknownQuotaIsFree() {
        val usage=NovelAiAccountUsage(1000,3,true,80.0,false)
        val normal=NovelAiGenerationSettings(model=NovelAiImageModel.V5_FULL)
        val free=NovelAiImageCostEstimator.estimate(normal,usage)
        assertEquals("生成免费",naiGenerateLabel(false,true,free,true))
        val paid=NovelAiImageCostEstimator.estimate(normal.copy(sizeTier=NovelAiSizeTier.WALLPAPER),usage)
        assertEquals("生成消耗 60 Anlas",naiGenerateLabel(false,true,paid,true))
        val unknown=NovelAiImageCostEstimator.estimate(normal,null)
        assertEquals("生成预计 30 Anlas",naiGenerateLabel(false,true,unknown,false))
        val extras=free.copy(anlas=4,encodingAnlas=2,extraVibeAnlas=2)
        assertEquals("生成消耗 4 Anlas（含编码 2）（含额外 Vibe 2）",naiGenerateLabel(false,true,extras,true))
        assertTrue(naiGenerateLabel(false,true,paid,true,true).endsWith("/ 批"))
        assertEquals("停止",naiGenerateLabel(true,true,null,false))
        assertEquals("生成设置无效",naiGenerateLabel(false,true,null,true))
    }
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
        val http=OkHttpClient.Builder().addInterceptor{chain->Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body("""{"trainingStepsLeft":1000,"tier":3,"active":true,"usage":{"percent":80,"isNegative":false}}""".toResponseBody()).build()}.build()
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http,RequestAdmission{})
        val scene=ImageComposeScene(1280,1100,coroutineContext=awt){MaterialTheme{Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(4){scene.render(System.nanoTime()).close();yield()};delay(80)}
        fun nodes():List<SemanticsNode> {
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun texts()=nodes().flatMap{it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.map{it.text}
        suspend fun chooseV5() {
            val button=nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.any{label->label.startsWith("生图模型 ·")}==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
            assertTrue(button.config[SemanticsActions.OnClick].action!!.invoke());frames()
            scene.sendKeyEvent(KeyEvent(Key.DirectionDown,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(Key.DirectionDown,KeyEventType.KeyUp))
            scene.sendKeyEvent(KeyEvent(Key.Enter,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(Key.Enter,KeyEventType.KeyUp));frames()
        }
        try {
            c.saveAccount(null,"Fixture NAI","NAI","https://image.novelai.net",NovelAiImageModel.V4_5_FULL.apiId,"SYNTHETIC_ONLY");c.active?.join()
            frames();chooseV5();assertEquals(NovelAiImageModel.V5_FULL,c.draft.selectedModel)
            assertTrue(texts().any{it.contains("生图模型 · V5 Full")})
            assertTrue(texts().none{it.contains("新建创作默认模型")})
            assertTrue(texts().any{it.contains("V5 Opus 体力：未知")})
            assertTrue(texts().none{it.contains("nai-diffusion-4-5-full")})
            assertTrue(texts().any{it=="生成预计 30 Anlas"})
            c.refreshUsage();c.active?.join();frames();assertTrue(texts().any{it=="生成免费"})
            c.edit{it.withActiveSettings(it.activeSettings.copy(sizeTier=NovelAiSizeTier.WALLPAPER,aspectRatio=NovelAiAspectRatio.PORTRAIT))};frames()
            assertTrue(texts().any{it=="生成消耗 60 Anlas"})
            c.view="账户";c.modelAccounts.category="NAI 生图";c.modelAccounts.open(c.state.accounts.single());frames()
            assertTrue(texts().none{it=="模型 ID" || it=="HTTPS API 地址"})
            assertTrue(texts().any{it=="保存并选用此账户"})
            assertTrue(texts().any{it.contains("NAI 生图：Fixture NAI · V5 Full")})
            c.view="创作";c.edit{it.copy(selectedModel=NovelAiImageModel.V4_5_FULL,followDefaultNovelAiImageModel=false)}
            frames();assertTrue(texts().none{it.contains("V5 Opus 体力")})
            c.defaults(model=NovelAiImageModel.V5_FULL);c.active?.join();frames()
            assertEquals(NovelAiImageModel.V4_5_FULL,c.draft.selectedModel)
            assertTrue(texts().none{it.contains("新建创作默认模型")})
            assertNull(c.problem)
            chooseV5();c.flush()
        } finally {scene.close();c.modelAccounts.discard();c.close();scope.cancel()}
        StudioWorkspace.openStudio(w.root).use{assertEquals(NovelAiImageModel.V5_FULL,it.state.creation!!.draft.selectedModel);assertFalse(it.state.creation!!.draft.followDefaultNovelAiImageModel)}
    }
    @Test fun catalogTaskReturnsToWorkbenchWithoutBlockingEditingAndDrainsBeforeMaintenance()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        val infra=DesktopNovelAiInfrastructure(root());val sha=infra.tags.catalogMetadata().sourceSha
        var gate=CountDownLatch(1);var entered=CompletableDeferred<Unit>()
        val http=OkHttpClient.Builder().addInterceptor { chain ->
            val request=chain.request();assertNull(request.header("Authorization"))
            val body=if(request.url.encodedPath.endsWith("/commits")) {
                entered.complete(Unit);check(gate.await(20,TimeUnit.SECONDS));"""[{"sha":"fixture-commit"}]"""
            }else """{"sha":"$sha","size":1,"download_url":"https://raw.githubusercontent.com/fixture"}"""
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(body.toResponseBody()).build()
        }.build()
        val c=WorkbenchController(w,scope,infra,root(),http,RequestAdmission{})
        var settings by mutableStateOf(true)
        val scene=ImageComposeScene(1280,1000,coroutineContext=awt){MaterialTheme{if(settings)NaiSettingsContent(c){settings=false}else Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frames(){repeat(4){scene.render(System.nanoTime()).close();yield()};delay(60)}
        fun nodes():List<SemanticsNode> {
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        try {
            c.naiSettings.checkCatalog();withTimeout(5000){entered.await()};frames()
            val task=c.naiSettings.task!!;assertTrue(c.naiSettings.busy);assertFalse(c.busy)
            val back=nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("返回创作（后台继续）")==true}
            assertNull(back.config.getOrNull(SemanticsProperties.Disabled));assertTrue(back.config[SemanticsActions.OnClick].action!!.invoke());frames()
            assertFalse(settings);assertTrue(task.isActive)
            assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text.contains("正在检查 Danbooru") }==true})
            c.edit{it.copy(basePrompt="synthetic background draft")};c.defaults(translated=true);c.active?.join()
            assertEquals("synthetic background draft",c.draft.basePrompt);assertTrue(c.naiSettings.busy);assertFalse(c.busy)
            gate.countDown();withTimeout(5000){task.join()};assertFalse(c.naiSettings.busy);assertEquals("词库已是最新版本",c.naiSettings.status)
            gate=CountDownLatch(1);entered=CompletableDeferred();c.naiSettings.checkCatalog();withTimeout(5000){entered.await()}
            val drain=async{c.enterMaintenance()};delay(60);assertFalse(drain.isCompleted)
            gate.countDown();withTimeout(5000){drain.await()};assertFalse(c.naiSettings.busy);assertNull(c.naiSettings.task)
            assertTrue(c.maintenance,"Maintenance must stay active after drainage")
            assertTrue(c.naiSettings.status.contains("已停止"),c.naiSettings.status)
        } finally {gate.countDown();scene.close();c.close();scope.cancel()}
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
