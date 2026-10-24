package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*

/** View-local metadata index. Rebuild when organization or verified history changes.
 * No image reads, persisted state, global cache or changes to provenance admission.
 */
internal class GalleryBatchIndex(org:GenerationOrganization,p:GalleryHistoryProjection) {
    val batches=org.batches.associateBy{it.id}
    private val originsByBatch=linkedMapOf<String,MutableMap<String,BatchResultOrigin>>().apply {
        p.batchOrigins.forEach{(image,origin)->getOrPut(origin.batchId){linkedMapOf()}[image]=origin}
    }
    // Receipt order precedes organization-only batches, as in the original projection.
    val ids=(originsByBatch.keys+batches.keys).distinct()
    private val members=mutableMapOf<String,Set<String>>()
    fun kind(id:String)=batches[id]?.kind?:"auto"
    fun origins(id:String):Map<String,BatchResultOrigin> = originsByBatch[id].orEmpty()
    fun title(id:String)=batches[id]?.title?:origins(id).values.firstOrNull()?.batchName.orEmpty().ifBlank{"批次 ${id.take(8)}"}
    fun images(id:String):Set<String> {
        if(id !in batches&&id !in originsByBatch)return emptySet()
        return members.getOrPut(id){
            linkedSetOf<String>().apply {
                batches[id]?.groups.orEmpty().forEach{addAll(it.images)}
                addAll(origins(id).keys)
            }
        }
    }
}
