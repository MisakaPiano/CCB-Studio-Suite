@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.example.chatbar.desktop.*
import com.sun.net.httpserver.HttpServer
import io.github.misakapiano.ccbstudio.core.*
import java.net.InetSocketAddress
import java.nio.file.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*
import kotlinx.coroutines.*

class ModelAccountsTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(100)}
    private fun ImageComposeScene.nodes():List<SemanticsNode> {
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private fun SemanticsNode.label(s:String)=config.getOrNull(SemanticsProperties.Text)?.any{it.text==s}==true || config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s)==true
    private suspend fun ImageComposeScene.click(s:String){nodes().first{it.label(s)&&it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames()}
    private suspend fun ImageComposeScene.field(s:String,value:String){nodes().first{it.config.getOrNull(SemanticsActions.SetText)!=null&&(it.label(s)||it.children.any{n->n.label(s)})}.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value));frames()}

    @Test fun discoverySelectionSaveAndReopenUseRealUi()=runBlocking(awt) {
        val requests=CopyOnWriteArrayList<String>();val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->requests+="${e.requestMethod} ${e.requestURI} ${e.requestHeaders.getFirst("Authorization")}";val bytes="""{"data":[{"id":"zeta"},{"id":"alpha"},{"id":"alpha"},{"id":2}]}""".toByteArray();e.sendResponseHeaders(200,bytes.size.toLong());e.responseBody.use{it.write(bytes)}};server.start()
        val http=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}${chain.request().url.encodedPath}").build())}.build()
        val r=root();val w=StudioWorkspace.openStudio(r);val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http,RequestAdmission{})
        val scene=ImageComposeScene(1240,1000,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        try {
            scene.frames();scene.click("账户");scene.field("档案名称","Model test");scene.field("HTTPS API 地址","https://fixture.invalid/v1");scene.field("API 凭据","SYNTHETIC_ONLY")
            scene.click("获取模型列表");withTimeout(5000){while(c.modelAccounts.searching)delay(20)};scene.frames()
            assertEquals(listOf("alpha","zeta"),c.modelAccounts.ids)
            scene.field("筛选模型","ZET");scene.click("zeta");assertEquals("zeta",c.modelAccounts.draft.model)
            scene.field("筛选模型","");scene.click("alpha")
            assertTrue(w.state.accounts.isEmpty());assertEquals("alpha",c.modelAccounts.draft.model)
            scene.click("安全保存并选用");c.active?.join();scene.frames();assertNull(c.problem)
            val a=w.state.accounts.single();assertEquals("alpha",a.model);assertEquals(a.id,c.llmId);assertEquals("",c.modelAccounts.draft.key)
            scene.click("安全保存并选用");c.active?.join();scene.frames();assertEquals(1,w.state.accounts.size)
            scene.field("档案名称","Renamed");scene.click("安全保存并选用");c.active?.join();scene.frames();assertEquals(a.secretRef,w.state.accounts.single().secretRef)
            scene.click("获取模型列表");withTimeout(5000){while(c.modelAccounts.searching)delay(20)}
            assertEquals(listOf("GET /v1/models Bearer SYNTHETIC_ONLY","GET /v1/models Bearer SYNTHETIC_ONLY"),requests.toList())
            c.modelAccounts.edit{it.copy(model="manual-id")};c.view="创作";scene.frames();c.view="账户";scene.frames();assertEquals("manual-id",c.modelAccounts.draft.model)
            c.modelAccounts.open(null);assertNotNull(c.modelAccounts.pending);c.modelAccounts.continueEditing();assertEquals("manual-id",c.modelAccounts.draft.model)
            c.modelAccounts.save();c.active?.join();assertNull(c.problem);c.close()
            StudioWorkspace.openStudio(r).use{restored->assertEquals("manual-id",restored.state.accounts.single().model);assertEquals(a.id,restored.state.creation!!.llmAccountId);assertEquals(a.secretRef,restored.state.accounts.single().secretRef)}
            Files.walk(r.resolve("documents")).use{paths->paths.filter{Files.isRegularFile(it)}.forEach{assertFalse(Files.readString(it).contains("SYNTHETIC_ONLY"))}}
        } finally {scene.close();scope.cancel();w.close();server.stop(0)}
    }

    @Test fun errorsNeverExposeProviderBodyAndRedirectNeverForwardsCredential():Unit=runBlocking {
        for(code in listOf(401,403,404,405,302,500,200)) {
            var calls=0;val client=studioHttpClient().newBuilder().addInterceptor{chain->calls++;okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(code).message("private-provider-error").header("Location","https://other.invalid/").body(okhttp3.ResponseBody.create(null,"SECRET_PROVIDER_BODY")).build()}.build()
            val error=assertFailsWith<java.io.IOException>{discoverModelIds(client,"https://fixture.invalid/v1","SYNTHETIC_ONLY")}
            assertFalse(error.message.orEmpty().contains("SECRET"));assertEquals(1,calls)
        }
        assertFailsWith<IllegalArgumentException>{discoverModelIds(studioHttpClient(),"http://fixture.invalid/v1","key")}
        assertFailsWith<IllegalArgumentException>{discoverModelIds(studioHttpClient(),"https://fixture.invalid/v1?key=bad","key")}
    }

    @Test fun editedEndpointInvalidatesPendingResponseAndManualModelRemains()=runBlocking(awt) {
        val client=studioHttpClient().newBuilder().addInterceptor{chain->Thread.sleep(250);okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK").body(okhttp3.ResponseBody.create(null,"""{"data":[{"id":"old-model"}]}""")).build()}.build()
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),client,RequestAdmission{})
        try {val editor=c.modelAccounts;editor.edit{it.copy(model="manual",key="SYNTHETIC")};editor.discover();delay(40);editor.edit{it.copy(endpoint="https://new.invalid/v1")};delay(500);assertFalse(editor.searching);assertTrue(editor.ids.isEmpty());assertEquals("manual",editor.draft.model);assertNull(editor.error)}
        finally{c.modelAccounts.discard();c.close();scope.cancel()}
    }

    @Test fun unsavedAccountPreventsCloseAndInvalidSaveKeepsDraft()=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root())
        try {
            val editor=c.modelAccounts;editor.edit{it.copy(alias="unfinished",model="manual",endpoint="https://invalid.example/v1?key=not-allowed",key="SYNTHETIC")}
            assertEquals("ACCOUNT_DRAFT_UNSAVED",assertFailsWith<DataProblem>{c.enterMaintenance()}.status);assertFalse(c.maintenance)
            editor.save();withTimeout(10000){c.active?.join()};assertNotNull(c.problem);assertTrue(w.state.accounts.isEmpty());assertEquals("SYNTHETIC",editor.draft.key)
            Files.list(w.root.resolve("secrets")).use{assertEquals(0,it.count())}
            editor.discard();assertFalse(editor.dirty)
        }finally{c.modelAccounts.discard();c.close();scope.cancel()}
    }
}
