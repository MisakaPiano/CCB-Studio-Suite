package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.util.UUID

/** Follow only recorded single-image derivations. Never infer ancestry from names or pixels. */
data class ImageOrigin(val imageId:String,val sourceId:String,val chain:List<String>,val job:JobRecord?,val receipt:GenerationReceipt?,val seed:Long?) {
    val derived get()=imageId!=sourceId
}

fun imageOrigin(workspace:StudioWorkspace,imageId:String):ImageOrigin=workspace.operation {
    val chain=mutableListOf<String>();var current=imageId;var composedJob:JobRecord?=null
    while(true) {
        require(chain.size<128 && current !in chain){"Image ancestry is cyclic or exceeds the verification budget"}
        chain+=current
        val asset=workspace.assets.read(UUID.fromString(current)).require()
        val jobs=workspace.state.jobs.filter{current in it.results||current in it.composedResults}
        require(jobs.size<=1){"Conflicting image generation identities"}
        val job=jobs.singleOrNull()
        if(job!=null) {
            require(composedJob==null || composedJob.id==job.id){"Composed image parent belongs to a different generation"}
            if(Json.parseToJsonElement(job.recipe).jsonObject["kind"]!=null)
                return@operation ImageOrigin(imageId,current,chain.toList(),null,null,null)
            if(current in job.results) {
                val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
                receipt.expandedRequest(workspace)
                return@operation ImageOrigin(imageId,current,chain.toList(),job,receipt,receipt.recipe.settings.seed+job.results.indexOf(current))
            }
            composedJob=job
        }
        val tag=asset.request().project().decodeToString()
        if(tag.startsWith("CCB_SHARED_IMAGE_") && tag!=SharedAssets.IMAGE)
            throw DataProblem("UNSUPPORTED_VERSION","Newer image ancestry retained")
        val parent=if(tag==SharedAssets.IMAGE) {
            val origin=requireNotNull(SharedAssets.provenance(asset))
            if(origin.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Newer image ancestry retained")
            require(origin.sourceIds.size==1 && origin.sourceIds.single()==asset.request().parentId()?.toString()){"Ambiguous image ancestry"}
            origin.sourceIds.single()
        }else if(tag.isEmpty() && asset.request().kind()==AssetStore.Kind.DERIVED) asset.request().parentId()?.toString()
        else null
        if(parent==null) {
            require(composedJob==null){"Composed image generation parent is missing"}
            return@operation ImageOrigin(imageId,current,chain.toList(),null,null,null)
        }
        current=parent
    }
    @Suppress("UNREACHABLE_CODE") error("Unreachable")
}
