package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class ReverseWorkflowTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun sse(text:String)=("data: "+buildJsonObject{put("choices",buildJsonArray{add(buildJsonObject{put("delta",buildJsonObject{put("content",text)});put("finish_reason","stop")})})}.toString()+"\n\ndata: [DONE]\n\n").toByteArray()
    private suspend fun exercise(mode:String) {
        val r=root();val runtime=root();val requests=CopyOnWriteArrayList<JsonObject>();var attempts=0
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->val body=Json.parseToJsonElement(e.requestBody.readBytes().decodeToString()).jsonObject;requests+=body
            val model=body.getValue("model").jsonPrimitive.content
            val reply=if(model=="vision")"A synthetic blue square on a plain background" else if(requests.count{it.getValue("model").jsonPrimitive.content!="vision"}==1)"{\"sceneDescription\":\"Blue square\",\"queries\":[]}"else"{\"baseCaption\":\"blue square, plain background\",\"characters\":[]}"
            val response=sse(reply);e.sendResponseHeaders(200,response.size.toLong());e.responseBody.use{it.write(response)}};server.start()
        val http=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val w=StudioWorkspace.openStudio(r);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),runtime.resolve("budget"),http,RequestAdmission{attempts++})
        try {
            var vision:String?=null
            if(mode=="linked") {c.saveAccount(null,"Vision","LLM","https://fixture.invalid/v1","vision","LOCAL_TEST_ONLY",true);c.active!!.join();assertNull(c.problem);vision=c.llmId}
            c.saveAccount(null,"Designer","LLM","https://fixture.invalid/v1","designer","LOCAL_TEST_ONLY",mode=="direct",vision);c.active!!.join();assertNull(c.problem)
            c.edit{it.copy(basePrompt="user draft")};c.flush()
            val id=w.importImage("reverse.png",EditorRenderer.synthetic(96,96));c.reverseImage(id);withTimeout(30000){c.active!!.join()}
            assertEquals("user draft",c.draft.basePrompt)
            if(mode=="missing") {assertNotNull(c.problem);assertTrue(requests.isEmpty());assertEquals(0,attempts);assertEquals("FAILED",w.state.jobs.single().state)}
            else {
                assertNull(c.problem,c.problem);val candidate=w.state.creation!!.reverseCandidate!!;assertEquals(id,candidate.sourceId);assertEquals("blue square, plain background",candidate.plan.baseCaption)
                assertEquals(requests.size,attempts)
                if(mode=="linked") {assertEquals(3,requests.size);assertTrue(requests.first().toString().contains("data:image/"));assertTrue(requests.drop(1).none{it.toString().contains("data:image/")});assertTrue(requests.last().toString().contains("synthetic blue square"))}
                else {assertEquals(2,requests.size);assertTrue(requests.any{it.toString().contains("data:image/")})}
                c.applyReverse();c.active!!.join();assertNull(c.problem);assertEquals("blue square, plain background",c.draft.basePrompt)
            }
            Files.walk(runtime).use{files->files.filter{Files.isRegularFile(it)}.forEach{assertFalse(Files.readString(it).contains("LOCAL_TEST_ONLY"),it.toString())}}
        } finally {c.close();scope.cancel();server.stop(0)}
        StudioWorkspace.openStudio(r).use{again->if(mode!="missing"){assertNotNull(again.state.creation!!.reverseCandidate);assertEquals("blue square, plain background",again.state.creation!!.draft.basePrompt)}}
    }
    @Test fun directVisionRequiresApplyAndSurvivesRestart()=runBlocking{exercise("direct")}
    @Test fun linkedVisionUsesAuthorityDescriptionAndNeverLeaksKeyToSnapshot()=runBlocking{exercise("linked")}
    @Test fun missingVisionFailsBeforeAnyPaidAttempt()=runBlocking{exercise("missing")}
}
