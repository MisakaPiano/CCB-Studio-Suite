package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class VisionCredentialsTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())

    @Test fun independentVisionAndGlobalRotationReachActualVisionTransport()=runBlocking(awt) {
        val requests=mutableListOf<Pair<String,String?>>()
        val http=studioHttpClient().newBuilder().addInterceptor{chain->
            val r=chain.request();requests+=r.url.host to r.header("Authorization")
            val body=if(r.url.encodedPath.endsWith("/models"))"""{"data":[{"id":"vision-fixture"}]}""" else
                "data: {\"choices\":[{\"delta\":{\"content\":\"fixture description\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n"
            Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody("text/event-stream".toMediaType())).build()
        }.build()
        val dir=root();val w=StudioWorkspace.openStudio(dir);val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http,RequestAdmission{})
        try {
            val e=c.modelAccounts
            e.globalKey="GLOBAL_SYNTHETIC_ONE";e.saveGlobal();c.active?.join();assertNull(c.problem);assertFalse(e.dirty)
            e.edit{it.copy(alias="designer",model="text",endpoint="https://text.invalid/v1",key="TEXT_SYNTHETIC")};e.save();c.active?.join()
            val text=w.state.accounts.single();assertEquals(text.id,c.llmId)
            e.newVision();e.edit{it.copy(alias="vision",model="vision-fixture",endpoint="https://vision.invalid/v1")}
            e.discover();withTimeout(5000){while(e.searching)delay(10)};assertEquals(listOf("vision-fixture"),e.ids)
            e.save(false);c.active?.join();assertNull(c.problem);assertEquals(text.id,c.llmId)
            val vision=w.state.accounts.single{it.isMultimodal};assertEquals(SecretVault.GLOBAL_LLM_REF,vision.secretRef)
            suspend fun describe() {
                val service=studioVision(root(),w.state.accounts,c.vault,RequestAdmission{},http)
                val result=service.prepare(listOf("SYNTHETIC_IMAGE"),text.copy(visionAccountId=vision.id).modelConfig(),true)
                assertTrue(result.descriptions.single().contains("fixture description"))
            }
            describe();assertEquals("vision.invalid" to "Bearer GLOBAL_SYNTHETIC_ONE",requests.last())
            e.globalKey="GLOBAL_SYNTHETIC_TWO";e.saveGlobal();c.active?.join();describe()
            assertEquals("vision.invalid" to "Bearer GLOBAL_SYNTHETIC_TWO",requests.last())
            e.edit{it.copy(useGlobalKey=false,key="VISION_PRIVATE_SYNTHETIC")};e.save(false);c.active?.join();describe()
            assertEquals("vision.invalid" to "Bearer VISION_PRIVATE_SYNTHETIC",requests.last())
            assertEquals(text.id,c.llmId)
            e.edit{it.copy(useGlobalKey=true,key="")};e.save(false);c.active?.join()
            e.open(w.state.accounts.single{it.id==text.id});e.edit{it.copy(visionId=vision.id)};e.save();c.active?.join()
            val backup=w.backup();assertFalse(Files.exists(backup.resolve("secrets")))
            Files.walk(dir.resolve("documents")).use{paths->paths.filter{Files.isRegularFile(it)}.forEach{p->
                val body=Files.readString(p);assertFalse(body.contains("GLOBAL_SYNTHETIC"));assertFalse(body.contains("PRIVATE_SYNTHETIC"))
            }}
            c.close()
            StudioWorkspace.openStudio(dir).use{reopened->
                assertEquals(vision.id,reopened.state.accounts.single{it.id==text.id}.visionAccountId)
                assertEquals(SecretVault.GLOBAL_LLM_REF,reopened.state.accounts.single{it.id==vision.id}.secretRef)
                SecretVault(dir.resolve("secrets")).use(SecretVault.GLOBAL_LLM_REF){assertEquals("GLOBAL_SYNTHETIC_TWO",it.decodeToString())}
            }
        }finally{if(!c.maintenance){c.modelAccounts.discard();c.close()};scope.cancel()}
    }

    @Test fun missingGlobalFailsBeforeNetworkAndDoesNotInheritForNai():Unit=runBlocking(awt) {
        val w=StudioWorkspace.openStudio(root());val scope=CoroutineScope(SupervisorJob()+awt)
        var requests=0
        val http=studioHttpClient().newBuilder().addInterceptor{requests++;error("must not send")}.build()
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http,RequestAdmission{})
        try {
            val e=c.modelAccounts;e.newVision();e.edit{it.copy(alias="missing",model="v")};e.save(false);c.active?.join();assertNull(c.problem)
            e.discover();withTimeout(5000){while(e.searching)delay(10)};assertNotNull(e.error);assertEquals(0,requests)
            e.globalKey="unsaved";assertEquals("ACCOUNT_DRAFT_UNSAVED",assertFailsWith<DataProblem>{c.enterMaintenance()}.status)
            assertFailsWith<IllegalArgumentException>{AccountProfile(alias="nai",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="m",secretRef=SecretVault.GLOBAL_LLM_REF).validate()}
        }finally{c.modelAccounts.discard();c.close();scope.cancel()}
    }
}
