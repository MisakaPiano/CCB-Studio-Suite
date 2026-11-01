package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import io.github.misakapiano.ccbstudio.assets.StoreException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.*
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.*

/** Metadata lookup scope: selected version families do not masquerade as full-store audits. */
class GenerationDesignLookupScopeTest {
    private val prefix="CCB_GENERATION_DESIGN_"
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun put(w:StudioWorkspace,tag:String,bytes:ByteArray,recipe:String=""):AssetStore.Asset {
        val id=UUID.randomUUID()
        w.assets.put(AssetStore.Request(id,"synthetic.bin",AssetStore.Kind.ORIGINAL,null,recipe.toByteArray(),tag.toByteArray()),ByteArrayInputStream(bytes))
        return w.assets.read(id).require()
    }
    private fun blob(w:StudioWorkspace,a:AssetStore.Asset)=w.root.resolve("assets/objects/${a.sha256()}.blob")
    private fun account()=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
    private fun job():JobRecord {
        val request=buildJsonObject{put("input","synthetic request")}
        val receipt=GenerationReceipt(recipe=NovelAiStudioDraft(basePrompt="synthetic").toRecipe(),account=account(),request=request,wireSha256=sha256(request.toString().toByteArray()))
        return JobRecord(account=account(),recipe=documentJson.encodeToString(receipt),state="COMPLETED")
    }
    @Test fun prefixSelectionPreservesExactFullAndSelectedPayloadIntegrity() {
        StudioWorkspace.openStudio(root()).use{w->
            val current=put(w,DesignProvenance.GENERATION,byteArrayOf(1,2));val future=put(w,prefix+"V99",byteArrayOf(3,4))
            val other=put(w,"OTHER",byteArrayOf(5,6))
            assertEquals(setOf(current.request().id(),future.request().id()),w.assets.catalogProjectPrefix(prefix.toByteArray()).map{it.request().id()}.toSet())
            assertEquals(listOf(current.request().id()),w.assets.catalogProject(DesignProvenance.GENERATION.toByteArray()).map{it.request().id()})
            assertEquals(3,w.catalog().size)
            assertFailsWith<IllegalArgumentException>{w.assets.catalogProjectPrefix(byteArrayOf())}
            Files.write(blob(w,other),byteArrayOf(9))
            assertEquals(2,w.assets.catalogProjectPrefix(prefix.toByteArray()).size)
            assertEquals(1,w.assets.catalogProject(DesignProvenance.GENERATION.toByteArray()).size)
            assertEquals("CORRUPT",assertFailsWith<StoreException>{w.catalog()}.code())
            Files.write(blob(w,future),byteArrayOf(8))
            assertEquals("CORRUPT",assertFailsWith<StoreException>{w.assets.catalogProjectPrefix(prefix.toByteArray())}.code())
            assertContentEquals(byteArrayOf(9),Files.readAllBytes(blob(w,other)))
            assertContentEquals(byteArrayOf(8),Files.readAllBytes(blob(w,future)))
        }
    }
    @Test fun unrelatedUnknownCorruptFutureAndPendingEnvelopesNeverBecomePartialSuccess() {
        for(kind in listOf("unknown","corrupt","future","pending"))StudioWorkspace.openStudio(root()).use{w->
            val a=put(w,"OTHER",byteArrayOf(1,2,3));val record=w.root.resolve("assets/records/${a.request().id()}.rec");val raw=Files.readAllBytes(record)
            val target=when(kind){
                "unknown"->record.parent.resolve("unknown.bin").also{Files.write(it,byteArrayOf(0))}
                "corrupt"->record.also{Files.write(it,byteArrayOf(0,1))}
                "future"->record.also{val changed=raw.clone();changed[7]=2;Files.write(it,changed)}
                else->record.parent.resolve(".pending-${a.request().id()}").also{Files.write(it,raw)}
            }
            val before=Files.readAllBytes(target)
            val expected=when(kind){"future"->"UNSUPPORTED_VERSION";"pending"->"RECOVERY_REQUIRED";else->"CORRUPT"}
            assertEquals(expected,assertFailsWith<StoreException>{w.assets.catalogProjectPrefix(prefix.toByteArray())}.code(),kind)
            assertContentEquals(before,Files.readAllBytes(target))
        }
    }
    @Test fun futureGenerationVersionAndConflictingLinksStillReject() {
        StudioWorkspace.openStudio(root()).use{w->
            val job=job();val p=DesignProvenance(w);assertNull(p.forGeneration(job))
            val future=put(w,prefix+"V99","{}".toByteArray(),job.id)
            assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{p.forGeneration(job)}.status)
            assertNull(p.forGeneration(job.copy(id=newId())))
            put(w,DesignProvenance.GENERATION,"{\"version\":1}".toByteArray(),job.id)
            assertFailsWith<IllegalArgumentException>{p.forGeneration(job)}
            assertContentEquals("{}".toByteArray(),Files.readAllBytes(blob(w,future)))
        }
    }
    @Test fun exactApplicationAndRecipeBindingRemainVerifiedWithoutAnyProvider() {
        StudioWorkspace.openStudio(root()).use{w->
            val draft=NovelAiStudioDraft(basePrompt="before");val applied=draft.copy(basePrompt="applied")
            val turn=NovelAiDesignTurn(userText="synthetic",reply=NovelAiDesignReply(NovelAiPromptPlan("applied",emptyList())),status=NovelAiDesignTurnStatus.COMPLETED)
            val conversation=NovelAiDesignConversation(title="synthetic",turns=listOf(turn))
            val p=DesignProvenance(w);val application=p.retain(conversation,turn.id,draft,applied);val job=job()
            p.linkGeneration(job,application)
            assertEquals(DesignPromptSnapshot.of(applied),p.forGeneration(job)!!.applied)
            val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
            val changedReceipt=receipt.copy(recipe=receipt.recipe.copy(basePrompt="changed"))
            val changedJob=job.copy(recipe=documentJson.encodeToString(changedReceipt))
            assertFailsWith<IllegalArgumentException>{p.forGeneration(changedJob)}
            assertNull(p.forGeneration(job.copy(id=newId())))
        }
    }
    @Test fun missingLinkPairedObservationRecordsBoundedLookupCost() {
        StudioWorkspace.openStudio(root()).use{w->
            val ids=mutableListOf<String>();val hashes=mutableListOf<String>()
            repeat(6){n->
                val image=BufferedImage(768,1024,BufferedImage.TYPE_INT_RGB)
                val bytes=try{
                    val line=IntArray(768)
                    for(y in 0 until 1024){for(x in line.indices)line[x]=(((x+n*13)%256) shl 16) or (((y+n*19)%256) shl 8) or ((x/8+y/8+n*23)%256);image.setRGB(0,y,768,1,line,0,768)}
                    ByteArrayOutputStream().also{check(ImageIO.write(image,"png",it))}.toByteArray()
                }finally{image.flush()}
                hashes+=sha256(bytes);ids+=w.importImage("lookup-$n.png",bytes)
            }
            assertEquals(6,hashes.toSet().size);val job=job().copy(results=listOf(ids.first()));w.save(w.state.copy(jobs=listOf(job)))
            // Exact old missing-link selection, not a copy of Prompt/request construction or link validation.
            val legacyStart=System.nanoTime()
            val legacy=w.operation{w.catalog().filter{it.request().project().decodeToString().startsWith(prefix)&&it.request().recipe().decodeToString()==job.id}}
            val legacyMs=(System.nanoTime()-legacyStart)/1e6;assertTrue(legacy.isEmpty())
            val actualStart=System.nanoTime();assertNull(DesignProvenance(w).forGeneration(job));val actualMs=(System.nanoTime()-actualStart)/1e6
            ids.forEachIndexed{i,id->assertEquals(hashes[i],sha256(w.bytes(id)))}
            val out=Path.of("build/generation-design-lookup-20261028/lookup.json");Files.createDirectories(out.parent)
            Files.writeString(out,buildJsonObject{put("syntheticImages",6);put("uniqueContentHashes",6);put("dimensions","768x1024 PNG")
                put("legacyFullCatalogMissingLinkMs",legacyMs);put("productionFamilyLookupMs",actualMs);put("providerCalls",0)
                put("scope","single paired missing-link observation; old selection only, not complete provenance implementation. Order/JIT/OS cache not controlled; no native/whole-page/RSS acceptance or speed threshold")}.toString())
        }
    }
}
