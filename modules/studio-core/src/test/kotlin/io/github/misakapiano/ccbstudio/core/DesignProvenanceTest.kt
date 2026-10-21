package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.msgpack.core.MessagePack
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.file.*
import java.io.IOException
import kotlin.test.*

class DesignProvenanceTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun conversation()=NovelAiDesignConversation(title="Synthetic design history",turns=listOf("candidate A","candidate B").map{caption->
        NovelAiDesignTurn(userText="Design $caption",reply=NovelAiDesignReply(NovelAiPromptPlan(caption,emptyList())),status=NovelAiDesignTurnStatus.COMPLETED)
    })
    @Test fun explicitApplicationUndoRedoAndUnknownWritesNeverInferFromEqualPrompt() {
        val root=root();val conv=conversation();lateinit var first:DesignApplicationRef;lateinit var latest:DesignApplicationRef
        StudioWorkspace.openStudio(root).use{w->
            val s=CreationSession(w);val p=DesignProvenance(w);assertNull(p.current())
            s.update{it.copy(conversations=listOf(conv),currentConversationId=conv.id)}
            s.applyReply(conv.id,conv.turns[0].id);first=p.current()!!
            assertEquals(listOf(conv.turns[0]),p.read(first).conversation.turns,"later designs must not enter an earlier application snapshot")
            s.editDraft{it.copy(basePrompt="manual edit",extraPrompt="extra")};assertEquals(first,p.current())
            s.undo();assertEquals("candidate A",w.state.creation!!.draft.basePrompt);assertEquals(first,p.current())
            s.undo();assertNull(p.current());s.undo(true);assertEquals(first,p.current())
            s.applyReply(conv.id,conv.turns[1].id);latest=p.current()!!;assertNotEquals(first,latest)
            s.undo();assertEquals(first,p.current());s.undo(true);assertEquals(latest,p.current())
            val revision=w.creationRevision;w.save(w.state.copy(savedCopies=emptyList()));assertEquals(revision,w.creationRevision);assertEquals(latest,p.current())
        }
        StudioWorkspace.openStudio(root).use{w->
            assertEquals(latest,DesignProvenance(w).current())
            val before=w.state.creation!!
            // An uninstrumented writer leaves the same visible prompt but a new committed creation identity.
            w.save(w.state.copy(creation=before.copy(composer="untracked writer")))
            assertNull(DesignProvenance(w).current());assertEquals("candidate B",w.state.creation!!.draft.basePrompt)
            assertEquals("candidate A",DesignProvenance(w).read(first).applied.base)
        }
    }
    @Test fun interruptionAfterDraftCommitCannotPromoteOrphanApplicationToVerifiedOrigin() {
        val root=root();val conv=conversation();var armed=false
        val w=StudioWorkspace.openStudio(root){stage->if(armed&&stage=="DOCUMENT_COMMITTED")throw IOException("Injected after commit")}
        try{CreationSession(w).update{it.copy(conversations=listOf(conv))};armed=true
            assertFailsWith<IOException>{CreationSession(w).applyReply(conv.id,conv.turns.first().id)}
        }finally{w.close()}
        StudioWorkspace.openStudio(root).use{again->
            assertEquals("candidate A",again.state.creation!!.draft.basePrompt)
            assertNull(DesignProvenance(again).current())
            assertEquals(1,again.catalog().count{it.request().project().decodeToString()==DesignProvenance.APPLICATION})
            CreationSession(again).applyReply(conv.id,conv.turns.first().id);assertNotNull(DesignProvenance(again).current())
        }
    }
    @Test fun exactGenerationLinkSurvivesLaterDesignBackupAndDetectsReceiptMismatch()=runBlocking {
        val root=root();val output=EditorRenderer.synthetic(128,128);val conv=conversation();lateinit var backup:Path;lateinit var first:DesignApplicationRef;var actualWire=""
        StudioWorkspace.openStudio(root).use{w->
            val session=CreationSession(w);session.update{it.copy(conversations=listOf(conv))};session.applyReply(conv.id,conv.turns.first().id)
            first=DesignProvenance(w).current()!!;session.editDraft{it.copy(basePrompt="manual after A").withActiveSettings(it.activeSettings.copy(seedMode=NovelAiSeedMode.FIXED,seed=42))}
            val draft=w.state.creation!!.draft;val vault=SecretVault(root.resolve("secrets"));val account=AccountProfile(alias="Synthetic local",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=draft.selectedModel.apiId,secretRef=vault.put("LOCAL_TEST_ONLY".toByteArray()))
            var calls=0;val server=HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
            val packer=MessagePack.newDefaultBufferPacker();packer.packMapHeader(2);packer.packString("event_type");packer.packString("final");packer.packString("image");packer.packBinaryHeader(output.size);packer.writePayload(output);packer.close();val packed=packer.toByteArray();val framed=ByteBuffer.allocate(packed.size+4).putInt(packed.size).put(packed).array()
            server.createContext("/"){exchange->calls++;actualWire=exchange.requestBody.readBytes().decodeToString();exchange.sendResponseHeaders(200,framed.size.toLong());exchange.responseBody.use{it.write(framed)}};server.start()
            val client=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
            try{
                val generator=StudioGenerator(w,vault,RequestAdmission{},client)
                assertFails{generator.generate(account,draft,designApplication=DesignApplicationRef(newId(),"0".repeat(64)))};assertEquals(0,calls)
                val job=generator.generate(account,draft,designApplication=first,onResult={session.applyReply(conv.id,conv.turns[1].id)})
                assertEquals(1,calls);assertEquals(NovelAiImageService().buildRequestBody(draft.toPromptPlan(),draft.activeSettings.imageSize(),draft.activeSettings),actualWire)
                assertEquals("candidate A",DesignProvenance(w).forGeneration(job)!!.applied.base)
                assertEquals("candidate B",DesignProvenance(w).read(DesignProvenance(w).current()!!).applied.base)
                val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
                assertEquals("manual after A",receipt.recipe.basePrompt);assertEquals(Json.parseToJsonElement(actualWire),receipt.expandedRequest(w))
                assertFails{DesignProvenance(w).forGeneration(job.copy(recipe=documentJson.encodeToString(receipt.copy(recipe=receipt.recipe.copy(basePrompt="changed receipt")))))}
                assertNull(DesignProvenance(w).forGeneration(job.copy(id=newId())))
                assertContentEquals(output,w.bytes(job.results.single()));backup=w.backup()
            }finally{server.stop(0);client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
        }
        val restored=root();StudioRestore.restore(backup,restored)
        StudioWorkspace.openStudio(restored).use{w->val job=w.state.jobs.single();assertEquals("candidate A",DesignProvenance(w).forGeneration(job)!!.applied.base);assertEquals(Json.parseToJsonElement(actualWire),documentJson.decodeFromString<GenerationReceipt>(job.recipe).expandedRequest(w));assertContentEquals(output,w.bytes(job.results.single()))}
    }
}
