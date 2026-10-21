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
}
