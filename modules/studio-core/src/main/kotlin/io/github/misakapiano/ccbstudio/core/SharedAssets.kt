package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Catalog projection over the existing immutable asset store; no second original store. */
@Serializable data class AssetProvenance(val version:Int=1,val role:String,val sourceIds:List<String>,
    val projectId:String,val projectRevision:Long,val snapshot:TypesetReference,val pageId:String?=null,
    val variant:String,val mediaType:String)
object SharedAssets {
    const val IMAGE="CCB_SHARED_IMAGE_V1"
    const val DOCUMENT="CCB_SHARED_DOCUMENT_V1"
    private val internalTags=setOf("CCB_REQUEST_PAYLOAD_V1",TypesetRepository.TAG,TypesetFonts.TAG,TypesetSourceExchange.TAG)
    fun images(workspace:StudioWorkspace)=workspace.catalog().filter { a ->
        val tag=a.request().project().decodeToString()
        tag !in internalTags && tag!=DOCUMENT && (tag.isEmpty() || tag==IMAGE)
    }
    fun documents(workspace:StudioWorkspace)=workspace.catalog().filter{it.request().project().decodeToString()==DOCUMENT}
    fun provenance(asset:AssetStore.Asset):AssetProvenance? {
        if(asset.request().project().decodeToString() !in setOf(IMAGE,DOCUMENT))return null
        return documentJson.decodeFromString<AssetProvenance>(asset.request().recipe().decodeToString())
    }
    /** Stable identity makes re-admission of a verified output safe without replacing an asset. */
    fun retain(workspace:StudioWorkspace,path:Path,expected:TypesetExportFile,origin:AssetProvenance):String=workspace.operation {
        require(origin.sourceIds.isNotEmpty());origin.sourceIds.forEach{workspace.assets.read(UUID.fromString(it)).require()}
        origin.snapshot.validate();origin.snapshot.chunks.forEach{workspace.assets.read(UUID.fromString(it)).require()}
        val recipe=documentJson.encodeToString(origin).toByteArray()
        val id=UUID.nameUUIDFromBytes(("CCB/export/"+expected.sha256+"/"+sha256(recipe)).toByteArray())
        val tag=if(origin.mediaType.startsWith("image/"))IMAGE else DOCUMENT
        val request=AssetStore.Request(id,path.fileName.toString(),AssetStore.Kind.DERIVED,UUID.fromString(origin.sourceIds.first()),recipe,tag.toByteArray())
        Files.newInputStream(path).use{workspace.assets.put(request,it)}
        val saved=workspace.assets.read(id).require()
        require(saved.size()==expected.size && saved.sha256()==expected.sha256){"Export asset identity mismatch"}
        id.toString()
    }
}
