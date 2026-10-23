package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.util.UUID

@Serializable data class DesignApplicationRef(val assetId:String,val sha256:String) {
    fun validate(){UUID.fromString(assetId);require(sha256.matches(Regex("[0-9a-f]{64}")))}
}
/** Only fields used to explain an application, independent of request serialization. */
@Serializable data class DesignPromptSnapshot(val style:String,val base:String,val extra:String,val negative:String,
    val characters:List<NovelAiCharacterPromptDraft>,val model:NovelAiImageModel) {
    companion object {
        fun of(d:NovelAiStudioDraft)=DesignPromptSnapshot(d.stylePrompt,d.basePrompt,d.extraPrompt,d.negativePrompt,d.characters,d.selectedModel)
        fun of(r:NovelAiGenerationRecipe)=DesignPromptSnapshot(r.stylePrompt,r.basePrompt,r.extraPrompt,r.negativePrompt,r.characters,r.settings.model)
    }
}
@Serializable data class DesignApplicationSnapshot(val version:Int=1,val appliedAt:Long=System.currentTimeMillis(),
    val conversation:NovelAiDesignConversation,val turnId:String,val before:DesignPromptSnapshot,val applied:DesignPromptSnapshot)
@Serializable private data class CreationLineage(val version:Int=1,val creationRevision:String,val creationSha256:String,
    val current:DesignApplicationRef?=null,val undo:List<DesignApplicationRef?> = emptyList(),val redo:List<DesignApplicationRef?> = emptyList())
@Serializable data class GenerationDesignLink(val version:Int=1,val jobId:String,val jobRecipeSha256:String,val wireSha256:String,val application:DesignApplicationRef)
enum class DesignLineageOperation { PRESERVE, EDIT, APPLY, UNDO, REDO }

/** Associations are explicit action receipts bound to the committed creation revision.
 * No association is inferred from time, equal prompt text, or orphaned applications. */
class DesignProvenance(private val workspace:StudioWorkspace) {
    companion object {const val APPLICATION="CCB_DESIGN_APPLICATION_V1";const val LINEAGE="CCB_CREATION_LINEAGE_V1";const val GENERATION="CCB_GENERATION_DESIGN_V1"}
    private fun creationHash(s:CreationState)=sha256(documentJson.encodeToString(s).toByteArray())
    private fun lineage():CreationLineage? {
        val identity=workspace.creationRevision?:return null
        val all=workspace.catalog()
        if(all.any{it.request().project().decodeToString().let{tag->tag.startsWith("CCB_CREATION_LINEAGE_")&&tag!=LINEAGE}})
            throw DataProblem("UNSUPPORTED_VERSION","Newer creation provenance retained")
        val records=all.filter{it.request().project().decodeToString()==LINEAGE&&it.request().recipe().decodeToString()==identity}
        require(records.size<=1){"Conflicting creation provenance retained"}
        val asset=records.singleOrNull()?:return null
        require(asset.size()<=65536)
        val raw=documentJson.parseToJsonElement(workspace.bytes(asset.request().id().toString()).decodeToString(throwOnInvalidSequence=true))
        if(raw.jsonObject["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","Creation provenance version retained")
        return documentJson.decodeFromJsonElement<CreationLineage>(raw).also {v->
            require(v.creationRevision==identity&&v.creationSha256==creationHash(workspace.state.creation!!)) {"Creation provenance identity mismatch"}
            require(v.undo.size<=30&&v.redo.size<=30);(listOfNotNull(v.current)+v.undo.filterNotNull()+v.redo.filterNotNull()).forEach{it.validate()}
        }
    }
    fun current():DesignApplicationRef?=workspace.operation{lineage()?.current?.also{read(it)}}
    fun retain(conversation:NovelAiDesignConversation,turnId:String,before:NovelAiStudioDraft,applied:NovelAiStudioDraft):DesignApplicationRef=workspace.operation {
        val turn=conversation.turns.single{it.id==turnId};require(turn.status==NovelAiDesignTurnStatus.COMPLETED&&turn.reply!=null)
        // Later conversation turns were not part of this candidate's design.
        val snapshot=DesignApplicationSnapshot(conversation=conversation.copy(turns=conversation.turns.takeWhile{it.id!=turnId}+turn),turnId=turnId,before=DesignPromptSnapshot.of(before),applied=DesignPromptSnapshot.of(applied))
        val bytes=documentJson.encodeToString(snapshot).toByteArray();require(bytes.size<=4*1024*1024){"Design application exceeds evidence budget; draft unchanged"}
        val id=UUID.randomUUID();workspace.assets.put(AssetStore.Request(id,"design-application.json",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),APPLICATION.toByteArray()),ByteArrayInputStream(bytes))
        DesignApplicationRef(id.toString(),sha256(bytes))
    }
    fun read(ref:DesignApplicationRef):DesignApplicationSnapshot=workspace.operation {
        ref.validate();val a=workspace.assets.read(UUID.fromString(ref.assetId)).require()
        require(a.request().project().decodeToString()==APPLICATION&&a.sha256()==ref.sha256){"Design application identity mismatch"}
        require(a.size()<=4*1024*1024)
        val raw=documentJson.parseToJsonElement(workspace.bytes(ref.assetId).decodeToString(throwOnInvalidSequence=true))
        if(raw.jsonObject["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","Design application version retained")
        documentJson.decodeFromJsonElement<DesignApplicationSnapshot>(raw).also {
            require(it.conversation.turns.lastOrNull()?.id==it.turnId&&it.conversation.turns.last().reply!=null)
        }
    }
    /** A separate asset links the existing exact request receipt; its schema is not rewritten. */
    fun linkGeneration(job:JobRecord,application:DesignApplicationRef)=workspace.operation {
        read(application)
        val raw=job.receipt(workspace)
        val receipt=documentJson.decodeFromString<GenerationReceipt>(raw);receipt.expandedRequest(workspace)
        require(receipt.wireSha256.isNotBlank())
        val link=GenerationDesignLink(jobId=job.id,jobRecipeSha256=sha256(raw.toByteArray()),wireSha256=receipt.wireSha256,application=application)
        val id=UUID.nameUUIDFromBytes((GENERATION+":"+job.id).toByteArray())
        workspace.assets.put(AssetStore.Request(id,"generation-design-link.json",AssetStore.Kind.DERIVED,UUID.fromString(application.assetId),job.id.toByteArray(),GENERATION.toByteArray()),ByteArrayInputStream(documentJson.encodeToString(link).toByteArray()))
    }
    fun forGeneration(job:JobRecord):DesignApplicationSnapshot?=workspace.operation {
        val matches=workspace.catalog().filter{it.request().project().decodeToString().startsWith("CCB_GENERATION_DESIGN_")&&it.request().recipe().decodeToString()==job.id}
        require(matches.size<=1){"Conflicting generation provenance retained"}
        val asset=matches.singleOrNull()?:return@operation null
        if(asset.request().project().decodeToString()!=GENERATION)throw DataProblem("UNSUPPORTED_VERSION","Newer generation provenance retained")
        require(asset.size()<=65536)
        val raw=documentJson.parseToJsonElement(workspace.bytes(asset.request().id().toString()).decodeToString(throwOnInvalidSequence=true))
        if(raw.jsonObject["version"]?.jsonPrimitive?.intOrNull!=1)throw DataProblem("UNSUPPORTED_VERSION","Generation provenance version retained")
        val link=documentJson.decodeFromJsonElement<GenerationDesignLink>(raw)
        val recipeText=job.receipt(workspace)
        val receipt=documentJson.decodeFromString<GenerationReceipt>(recipeText);receipt.expandedRequest(workspace)
        require(link.jobId==job.id&&link.jobRecipeSha256==sha256(recipeText.toByteArray())&&link.wireSha256==receipt.wireSha256&&asset.request().parentId().toString()==link.application.assetId){"Generation provenance identity mismatch"}
        read(link.application)
    }
    /** Commit the existing state normally, then bind provenance to that exact committed revision.
     * An interruption before the receipt leaves the new draft usable but its origin unrecorded. */
    fun update(next:CreationState,operation:DesignLineageOperation,application:DesignApplicationRef?=null)=workspace.operation {
        val old=workspace.state.creation!!;val prior=lineage()
        val undo=List(old.undo.size){prior?.undo?.getOrNull(it)};val redo=List(old.redo.size){prior?.redo?.getOrNull(it)}
        val historyChanged=next.draft!=old.draft||next.undo!=old.undo||next.redo!=old.redo
        val provenance=when(operation){
            DesignLineageOperation.APPLY->{require(application!=null);read(application);CreationLineage(creationRevision="",creationSha256="",current=application,undo=(undo+prior?.current).takeLast(30))}
            DesignLineageOperation.EDIT->if(historyChanged)CreationLineage(creationRevision="",creationSha256="",current=prior?.current,undo=(undo+prior?.current).takeLast(30))else prior
            DesignLineageOperation.UNDO->if(old.undo.isEmpty())prior else CreationLineage(creationRevision="",creationSha256="",current=undo.last(),undo=undo.dropLast(1),redo=(redo+prior?.current).takeLast(30))
            DesignLineageOperation.REDO->if(old.redo.isEmpty())prior else CreationLineage(creationRevision="",creationSha256="",current=redo.last(),undo=(undo+prior?.current).takeLast(30),redo=redo.dropLast(1))
            DesignLineageOperation.PRESERVE->if(historyChanged)null else prior
        }
        workspace.save(workspace.state.copy(creation=next))
        // An absent receipt already means unknown for this exact revision. Do not add
        // empty provenance assets for ordinary typing before any design application.
        if(next!=old && provenance!=null && (listOfNotNull(provenance.current)+provenance.undo.filterNotNull()+provenance.redo.filterNotNull()).isNotEmpty()){
            val receipt=provenance.copy(creationRevision=workspace.creationRevision!!,creationSha256=creationHash(next))
            val bytes=documentJson.encodeToString(receipt).toByteArray()
            workspace.assets.put(AssetStore.Request(UUID.randomUUID(),"creation-lineage.json",AssetStore.Kind.ORIGINAL,null,receipt.creationRevision.toByteArray(),LINEAGE.toByteArray()),ByteArrayInputStream(bytes))
        }
    }
}
