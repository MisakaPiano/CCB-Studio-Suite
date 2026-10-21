package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.chat.*
import com.example.chatbar.domain.prompt.AuxiliaryMessageAssembler
import com.example.chatbar.data.local.entity.ModelConfig
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.*
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import okhttp3.*
import org.msgpack.core.MessagePack
import kotlin.test.*

class CreationRuntimeTest {
    private fun root()=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
    private val lookup=object:NovelAiTagSearchClient {override suspend fun search(query:String)=NovelAiTagSearchOutcome(query,emptyList())}
    private class Server(private val response:(Int)->Pair<Int,ByteArray>):AutoCloseable {
        val bodies=CopyOnWriteArrayList<String>()
        private val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0).also {s ->
            s.createContext("/") {exchange ->
                bodies+=exchange.requestBody.readBytes().decodeToString()
                val (status,bytes)=response(bodies.size)
                exchange.responseHeaders.set("Content-Type","text/event-stream")
                exchange.sendResponseHeaders(status,bytes.size.toLong());exchange.responseBody.use {it.write(bytes)}
            };s.start()
        }
        // Test-only endpoint rewrite; production account policy and adapter have no cleartext switch.
        val client=studioHttpClient().newBuilder().addInterceptor {chain ->
            chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())
        }.build()
        override fun close(){server.stop(0);client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
    }
    private fun sse(text:String,finish:String="stop",done:Boolean=true)= buildString {
        append("data: "+buildJsonObject {put("choices",buildJsonArray {add(buildJsonObject {put("delta",buildJsonObject {put("content",text)});put("finish_reason",if(finish.isEmpty()) JsonNull else JsonPrimitive(finish))})})}.toString()+"\n\n")
        if(done)append("data: [DONE]\n\n")
    }.toByteArray()
    private fun profile(vault:SecretVault,service:String="LLM")=AccountProfile(alias="Loopback",service=service,
        provider=if(service=="LLM")"OPENAI_COMPATIBLE" else "NOVELAI",endpoint=if(service=="LLM")"https://example.invalid/v1" else "https://image.novelai.net",
        model="fixture",secretRef=vault.put("LOCAL_TEST_ONLY".toByteArray()))

    @Test fun normalTransportUsesAuthorityEnvelopeAndCannotAcceptPartialStream()=runBlocking {
        val vault=SecretVault(root());val a=profile(vault);val admitted=mutableListOf<String>()
        val messages=listOf(ChatApiMessage.text("system","Synthetic system"),ChatApiMessage.text("user","Synthetic input"))
        val model=ModelConfig(a.id,a.alias,a.endpoint,"",a.model,createdAt=1)
        Server {200 to sse("synthetic answer")}.use {server ->
            val result=StudioTextTransport(a,vault,RequestAdmission {admitted+=it},server.client)
                .streamText(NovelAiTextStage.GENERATE,messages,model).toList()
            assertTrue(result.last() is StreamEvent.Done)
            assertEquals(Json.parseToJsonElement(OpenAiChatRequestSerializer.serialize(AuxiliaryMessageAssembler.assemble(messages,true),model,true)),Json.parseToJsonElement(server.bodies.single()))
            assertEquals(listOf("LLM"),admitted)
        }
        Server {200 to sse("partial",finish="",done=false)}.use {server ->
            assertFailsWith<DataProblem>{StudioTextTransport(a,vault,RequestAdmission {},server.client).streamText(NovelAiTextStage.PLAN,messages,model).toList()}
            assertEquals(1,server.bodies.size)
        }
    }
    @Test fun httpErrorsNeverPersistProviderBodyOrRetry()=runBlocking {
        val vault=SecretVault(root());val a=profile(vault)
        Server {429 to "PRIVATE_SERVER_ECHO".toByteArray()}.use {s ->
            val e=assertFailsWith<DataProblem> {StudioTextTransport(a,vault,RequestAdmission {},s.client)
                .streamText(NovelAiTextStage.GENERATE,listOf(ChatApiMessage.text("user","fixture")),ModelConfig(a.id,a.alias,a.endpoint,"",a.model,createdAt=1)).toList()}
            assertEquals("HTTP_429",e.status);assertFalse(e.toString().contains("PRIVATE_SERVER_ECHO"));assertEquals(1,s.bodies.size)
        }
    }
    @Test fun designCandidateRequiresApplyBranchesAndSurvivesRestart()=runBlocking {
        val r=root()
        StudioWorkspace.openStudio(r).use {w ->
            val vault=SecretVault(r.resolve("secrets"));val a=profile(vault);var attempts=0
            Server {n -> 200 to sse(if(n==1)"{\"sceneDescription\":\"A quiet room\",\"queries\":[]}" else "{\"baseCaption\":\"quiet room, window\",\"characters\":[]}")}.use {server ->
                val t=StudioTextTransport(a,vault,RequestAdmission {attempts++},server.client)
                val designer=NovelAiPromptDesigner(t,NovelAiTagResearchService(LlmNovelAiTagSearchPlanner(t),lookup))
                val session=CreationSession(w);session.editDraft {it.copy(basePrompt="manually edited")}
                session.update {it.copy(composer="Synthetic quiet room")}
                session.design(a,designer)
                assertEquals("manually edited",w.state.creation!!.draft.basePrompt)
                val c=w.state.creation!!.conversations.single();assertEquals(NovelAiDesignTurnStatus.COMPLETED,c.turns.single().status)
                session.applyReply(c.id,c.turns.single().id);assertEquals("quiet room, window",w.state.creation!!.draft.basePrompt)
                session.update {it.copy(composer="Add soft light")};session.design(a,designer)
                assertEquals(2,w.state.creation!!.conversations.single().turns.size)
                session.branch(c.id,c.turns.single().id,"New branch")
                assertEquals(2,w.state.creation!!.conversations.size);assertEquals(2,w.state.creation!!.conversations.first().turns.size)
                assertEquals(server.bodies.size,attempts);assertTrue(attempts>=3)
            }
        }
        StudioWorkspace.openStudio(r).use {w -> assertEquals("New branch",w.state.creation!!.composer);assertEquals(2,w.state.creation!!.conversations.size) }
    }
    @Test fun waitingCancellationRetainsLockAndDoesNotSend()=runBlocking {
        val r=root();val w=StudioWorkspace.openStudio(r)
        try {
            val vault=SecretVault(r.resolve("secrets"));val a=profile(vault,"NAI");val entered=CompletableDeferred<Unit>()
            Server {200 to byteArrayOf(1)}.use {server ->
                val job=launch {StudioGenerator(w,vault,RequestAdmission {entered.complete(Unit);awaitCancellation()},server.client).generate(a,NovelAiStudioDraft(basePrompt="fixture"))}
                entered.await()
                assertEquals("DRAIN_REQUIRED",assertFailsWith<DataProblem>{w.close()}.status)
                assertFailsWith<DataProblem>{StudioWorkspace.openStudio(r)}
                job.cancelAndJoin();assertTrue(server.bodies.isEmpty());assertEquals("CANCELLED",w.state.jobs.single().state)
            }
        } finally {w.close()}
        StudioWorkspace.openStudio(r).use {assertEquals("CANCELLED",it.state.jobs.single().state)}
    }
    private fun frame(image:ByteArray):ByteArray {
        val p=MessagePack.newDefaultBufferPacker();p.packMapHeader(2);p.packString("event_type");p.packString("final");p.packString("image");p.packBinaryHeader(image.size);p.writePayload(image);p.close()
        val b=p.toByteArray();return ByteBuffer.allocate(4+b.size).putInt(b.size).put(b).array()
    }
    @Test fun partialGenerationRetainsOriginalAndFullReceiptOnFailure()=runBlocking {
        val r=root();val png=EditorRenderer.synthetic(96,96);var id=""
        StudioWorkspace.openStudio(r).use {w ->
            val vault=SecretVault(r.resolve("secrets"));val a=profile(vault,"NAI")
            Server {200 to frame(png)}.use {server ->
                val draft=NovelAiStudioDraft(basePrompt="fixture").withActiveSettings(NovelAiGenerationSettings(count=2,seedMode=NovelAiSeedMode.FIXED,seed=123))
                assertFails {StudioGenerator(w,vault,RequestAdmission {},server.client).generate(a,draft,onResult={id=it})}
                assertTrue(id.isNotBlank());assertContentEquals(png,w.bytes(id));assertEquals("FAILED",w.state.jobs.single().state)
                val actual=Json.parseToJsonElement(server.bodies.single())
                assertEquals(actual,Json.parseToJsonElement(w.state.jobs.single().recipe).jsonObject["request"])
                assertFalse(w.state.jobs.single().recipe.contains("LOCAL_TEST_ONLY"));assertEquals(1,server.bodies.size)
            }
        }
        StudioWorkspace.openStudio(r).use {assertContentEquals(png,it.bytes(id));assertEquals(id,it.state.jobs.single().results.single())}
    }
    @Test fun interruptedDesignIsNotReplayedOnOpen() {
        val r=root();val c=NovelAiDesignConversation(turns=listOf(NovelAiDesignTurn(userText="fixture")))
        StudioWorkspace.openStudio(r).use {w -> CreationSession(w).update {it.copy(conversations=listOf(c),currentConversationId=c.id)} }
        StudioWorkspace.openStudio(r).use {w -> val turn=w.state.creation!!.conversations.single().turns.single()
            assertEquals(NovelAiDesignTurnStatus.FAILED,turn.status);assertTrue(turn.error.contains("结果未知")) }
    }
    @Test fun composedResultIsDerivedAndRawResponseSurvivesRestart()=runBlocking {
        val r=root();val raw=EditorRenderer.synthetic(128,128);val composite=EditorRenderer.synthetic(256,128)
        StudioWorkspace.openStudio(r).use {w->
            val vault=SecretVault(r.resolve("secrets"));val a=profile(vault,"NAI")
            Server {200 to frame(raw)}.use {server->
                val job=StudioGenerator(w,vault,RequestAdmission{},server.client).generate(a,NovelAiStudioDraft(basePrompt="fixture"),compose={composite})
                assertEquals("COMPLETED",job.state);assertEquals(1,job.results.size);assertEquals(1,job.composedResults.size)
                assertContentEquals(raw,w.bytes(job.results.single()));assertContentEquals(composite,w.bytes(job.composedResults.single()))
                val derived=w.assets.read(java.util.UUID.fromString(job.composedResults.single())).require()
                assertEquals(java.util.UUID.fromString(job.results.single()),derived.request().parentId())
            }
        }
        StudioWorkspace.openStudio(r).use {w->val job=w.state.jobs.single();assertContentEquals(raw,w.bytes(job.results.single()));assertContentEquals(composite,w.bytes(job.composedResults.single()))}
    }
    @Test fun compositingFailurePreservesUniqueRawFinal()=runBlocking {
        val r=root();val raw=EditorRenderer.synthetic(128,128)
        StudioWorkspace.openStudio(r).use {w->
            val vault=SecretVault(r.resolve("secrets"));val a=profile(vault,"NAI")
            Server {200 to frame(raw)}.use {server->
                assertFails {StudioGenerator(w,vault,RequestAdmission{},server.client).generate(a,NovelAiStudioDraft(basePrompt="fixture"),compose={error("Injected compositing failure")})}
                val job=w.state.jobs.single();assertEquals("FAILED",job.state);assertTrue(job.composedResults.isEmpty());assertContentEquals(raw,w.bytes(job.results.single()))
            }
        }
    }
    @Test fun largeGuidanceReceiptKeepsExactWireAndSurvivesBackup()=runBlocking {
        val r=root();val output=EditorRenderer.synthetic(128,128);var backup:Path?=null;var request=""
        StudioWorkspace.openStudio(r).use {w->
            val source=java.awt.image.BufferedImage(640,640,java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val random=java.util.Random(42);for(y in 0 until 640)for(x in 0 until 640)source.setRGB(x,y,random.nextInt())
            val bytes=java.io.ByteArrayOutputStream().also{javax.imageio.ImageIO.write(source,"png",it)}.toByteArray();source.flush();assertTrue(bytes.size>1024*1024)
            val id=w.importImage("large-guidance.png",bytes)
            val g=NovelAiImageGuidanceDraft(action=NovelAiGenerationAction.IMAGE_TO_IMAGE,baseImage=NovelAiStudioAssetRef(id=id,path="asset:$id",sha256=sha256(bytes),width=640,height=640))
            val draft=NovelAiStudioDraft(basePrompt="fixture",imageGuidance=g)
            val prepared=NovelAiPreparedImageGuidance(action=g.action,imageBase64=java.util.Base64.getEncoder().encodeToString(bytes))
            val vault=SecretVault(r.resolve("secrets"));val a=profile(vault,"NAI")
            Server{200 to frame(output)}.use {server->
                val job=StudioGenerator(w,vault,RequestAdmission{},server.client).generate(a,draft,prepared)
                request=server.bodies.single();assertTrue(job.recipe.length<10000)
                val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
                assertEquals(Json.parseToJsonElement(request),receipt.expandedRequest(w));assertContentEquals(bytes,w.bytes(id))
            };backup=w.backup()
        }
        val restored=root();StudioRestore.restore(backup!!,restored)
        StudioWorkspace.openStudio(restored).use {w->val receipt=documentJson.decodeFromString<GenerationReceipt>(w.state.jobs.single().recipe);assertEquals(Json.parseToJsonElement(request),receipt.expandedRequest(w));assertContentEquals(output,w.bytes(w.state.jobs.single().results.single()))}
    }
    @Test fun documentLimitPreflightDoesNotPoisonReadableWorkspace() {
        StudioWorkspace.openStudio(root()).use {w->
            val before=w.state
            val tooLarge=before.copy(creation=before.creation!!.copy(draft=NovelAiStudioDraft(basePrompt="x".repeat(5*1024*1024))))
            assertEquals("DOCUMENT_LIMIT",assertFailsWith<DataProblem>{w.save(tooLarge)}.status)
            assertEquals(before,w.state);CreationSession(w).editDraft{it.copy(basePrompt="after rejected oversized edit")};assertEquals("after rejected oversized edit",w.state.creation!!.draft.basePrompt)
        }
    }
    @Test fun requestPayloadChunksRestoreAndDetectAlteredDescriptors() {
        val r=root();val text="A".repeat(9*1024*1024);lateinit var descriptor:JsonObject;lateinit var backup:Path
        StudioWorkspace.openStudio(r).use {w->descriptor=RequestEvidence(w,null).retainText(text);assertEquals(2,descriptor.getValue("ccbPayloadChunks").jsonArray.size);backup=w.backup()}
        val dest=root();StudioRestore.restore(backup,dest)
        StudioWorkspace.openStudio(dest).use {w->
            assertEquals(text,RequestEvidence.expand(w,descriptor).jsonPrimitive.content)
            assertFails {RequestEvidence.expand(w,JsonObject(descriptor+ ("sha256" to JsonPrimitive("bad"))))}
        }
    }
}
