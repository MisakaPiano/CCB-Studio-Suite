package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.sun.net.httpserver.HttpServer
import io.github.misakapiano.ccbstudio.core.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.file.*
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.msgpack.core.MessagePack

class PostProcessWorkflowTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun frame(image:ByteArray):ByteArray {val p=MessagePack.newDefaultBufferPacker();p.packMapHeader(2);p.packString("event_type");p.packString("final");p.packString("image");p.packBinaryHeader(image.size);p.writePayload(image);p.close();val b=p.toByteArray();return ByteBuffer.allocate(b.size+4).putInt(b.size).put(b).array()}
    private suspend fun exercise(enhance:Boolean,invalidDimensions:Boolean=false) {
        val r=root();val original=EditorRenderer.synthetic(128,128);val output=EditorRenderer.synthetic(if(invalidDimensions)96 else if(enhance)128 else 256,if(invalidDimensions)96 else if(enhance)128 else 256)
        val requests=CopyOnWriteArrayList<String>();var attempts=0
        val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->requests+=e.requestBody.readBytes().decodeToString();val body=if(enhance)frame(output)else buildJsonObject{put("images",buildJsonArray{add(buildJsonObject{put("image",Base64.getEncoder().encodeToString(output))})})}.toString().toByteArray();e.sendResponseHeaders(200,body.size.toLong());e.responseBody.use{it.write(body)}};server.start()
        val http=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val w=StudioWorkspace.openStudio(r);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),http,RequestAdmission{attempts++})
        var delivered:ByteArray?=null;var sourceId=""
        try {
            c.saveAccount(null,"Local NAI","NAI","https://image.novelai.net",NovelAiImageModel.V4_5_FULL.apiId,"LOCAL_TEST_ONLY");c.active!!.join();assertNull(c.problem)
            sourceId=w.importImage("source.png",original)
            val source=if(enhance)NovelAiEnhanceSource(NovelAiPromptPlan(baseCaption="fixture",characterCaptions=emptyList()),NovelAiGenerationSettings(),buildJsonObject{put("noise_schedule","native")})else null
            c.postProcess(sourceId,original,if(enhance)NovelAiPostProcessTab.ENHANCE else NovelAiPostProcessTab.UPSCALE,source,NovelAiEnhanceOptions(),{delivered=it},{})
            withTimeout(30000){c.active!!.join()}
            val job=w.state.jobs.single();assertEquals(1,requests.size);assertEquals(1,attempts);assertEquals(1,job.results.size)
            assertContentEquals(original,w.bytes(sourceId));assertContentEquals(output,w.bytes(job.results.single()))
            val evidence=Json.parseToJsonElement(job.recipe).jsonObject
            assertEquals(Json.parseToJsonElement(requests.single()),RequestEvidence.expand(w,evidence.getValue("request")))
            assertEquals(sha256(requests.single().toByteArray()),evidence.getValue("wireSha256").jsonPrimitive.content)
            assertFalse(job.recipe.contains("LOCAL_TEST_ONLY"))
            if(invalidDimensions){assertEquals("FAILED",job.state);assertNull(delivered);assertNotNull(c.problem)}
            else {assertEquals("COMPLETED",job.state);assertContentEquals(output,delivered);assertNull(c.problem)}
            val body=Json.parseToJsonElement(requests.single()).jsonObject
            if(enhance){assertEquals("img2img",body.getValue("action").jsonPrimitive.content);assertEquals("fixture",body.getValue("input").jsonPrimitive.content)}
            else assertEquals("nai-diffusion-5-curated",body.getValue("model").jsonPrimitive.content)
        } finally {c.close();scope.cancel();server.stop(0)}
        StudioWorkspace.openStudio(r).use {again->assertContentEquals(original,again.bytes(sourceId));assertContentEquals(output,again.bytes(again.state.jobs.single().results.single()))}
    }
    @Test fun upscalePersistsExactRequestAndResult()=runBlocking{exercise(false)}
    @Test fun enhanceUsesAuthorityRequestWithoutChangingSource()=runBlocking{exercise(true)}
    @Test fun unexpectedDimensionsRetainReceivedOriginalAndReportFailure()=runBlocking{exercise(false,true)}
}
