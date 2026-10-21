package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.json.*

/** Keep authoritative binary request fields out of the small journal/asset descriptor.
 * The bytes still live in the verified AssetStore and are included in its backup/recovery. */
class RequestEvidence(private val workspace:StudioWorkspace,private val parentId:String?) {
    private val retained=mutableMapOf<String,JsonObject>()
    private val fields=setOf("image","mask","director_reference_images","reference_image_multiple")
    fun retainText(value:String):JsonObject {
        val bytes=value.toByteArray();require(bytes.size<=48*1024*1024)
        val hash=sha256(bytes)
        return retained.getOrPut(hash) {
            val ids=(bytes.indices step (8*1024*1024)).map{start->workspace.requestPayload(bytes.copyOfRange(start,minOf(bytes.size,start+8*1024*1024)),parentId)}
            buildJsonObject {put("ccbPayloadChunks",JsonArray(ids.map(::JsonPrimitive)));put("encoding","utf8");put("bytes",bytes.size);put("sha256",hash)}
        }
    }
    fun retainRequest(value:JsonElement):JsonElement=when(value) {
        is JsonObject->JsonObject(value.mapValues{(key,v)->if(key in fields)retainBinary(v)else retainRequest(v)})
        is JsonArray->JsonArray(value.map(::retainRequest))
        else->value
    }
    private fun retainBinary(value:JsonElement):JsonElement=when(value) {
        is JsonArray->JsonArray(value.map(::retainBinary))
        is JsonPrimitive->if(value.isString)retainText(value.content)else value
        else->value
    }
    companion object {
        fun expand(workspace:StudioWorkspace,value:JsonElement):JsonElement=when(value) {
            is JsonObject->if(value["ccbPayloadChunks"]!=null) {
                require(value.keys==setOf("ccbPayloadChunks","encoding","bytes","sha256") && value["encoding"]?.jsonPrimitive?.content=="utf8")
                val size=value.getValue("bytes").jsonPrimitive.int;require(size in 0..48*1024*1024)
                val ids=value.getValue("ccbPayloadChunks").jsonArray;require(ids.size<=6)
                val bytes=java.io.ByteArrayOutputStream().use {out->ids.forEach{id->
                    val chunk=workspace.bytes(id.jsonPrimitive.content);require(chunk.size<=8*1024*1024 && out.size().toLong()+chunk.size<=size)
                    out.write(chunk)
                };out.toByteArray()}
                require(bytes.size==value.getValue("bytes").jsonPrimitive.int && sha256(bytes)==value.getValue("sha256").jsonPrimitive.content)
                JsonPrimitive(bytes.decodeToString(throwOnInvalidSequence=true))
            }else JsonObject(value.mapValues{expand(workspace,it.value)})
            is JsonArray->JsonArray(value.map{expand(workspace,it)})
            else->value
        }
    }
}

fun GenerationReceipt.expandedRequest(workspace:StudioWorkspace):JsonElement {
    require(version in 2..3)
    val expanded=if(version==2)request else RequestEvidence.expand(workspace,request)
    if(wireSha256.isNotEmpty())require(sha256(expanded.toString().toByteArray())==wireSha256){"Request evidence hash mismatch"}
    return expanded
}
fun GenerationReceipt.recipeForReuse(workspace:StudioWorkspace):NovelAiGenerationRecipe {
    require(version in 2..3)
    return recipe.copy(imageGuidance=recipe.imageGuidance.copy(vibes=recipe.imageGuidance.vibes.map{v->
        vibePayloads[v.id]?.let{v.copy(encodedVibe=RequestEvidence.expand(workspace,it).jsonPrimitive.content)}?:v
    }))
}
