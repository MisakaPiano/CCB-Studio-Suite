package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.util.UUID

@Serializable private data class ArchiveReference(val version:Int=1,val id:String,val sha256:String)
@Serializable private data class ArchiveSnapshot(val version:Int=1,val ids:Set<String>)

/** Reversible library visibility only. Source assets, project history and external
 * collection files are never deleted or rewritten. Uses the existing library commit. */
class AssetArchive(private val workspace:StudioWorkspace) {
    companion object {const val TAG="CCB_ASSET_ARCHIVE_V1";const val KEY="assetArchive"}
    private val library=LibraryRepository(workspace)
    private fun version(raw:String) {
        val v=documentJson.parseToJsonElement(raw).jsonObject["version"]?.jsonPrimitive?.intOrNull
        if(v!=1)throw DataProblem("UNSUPPORTED_VERSION","Asset archive version retained")
    }
    private fun reference(state:LibraryState)=state.preferences[KEY]?.let{raw->
        version(raw)
        documentJson.decodeFromString<ArchiveReference>(raw).also{r->
            if(r.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Asset archive reference retained")
            UUID.fromString(r.id);require(r.sha256.matches(Regex("[0-9a-f]{64}")))
        }
    }
    fun ids(state:LibraryState=library.load()):Set<String> = workspace.operation {
        val ref=reference(state)?:return@operation emptySet()
        val asset=workspace.assets.read(UUID.fromString(ref.id)).require()
        if(asset.request().project().decodeToString()!=TAG)throw DataProblem("UNSUPPORTED_VERSION","Asset archive format retained")
        require(asset.size()<=1024*1024 && asset.sha256()==ref.sha256){"Asset archive identity mismatch"}
        val raw=workspace.bytes(ref.id).decodeToString(throwOnInvalidSequence=true);version(raw)
        val snapshot=documentJson.decodeFromString<ArchiveSnapshot>(raw)
        if(snapshot.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Asset archive snapshot retained")
        require(snapshot.ids.size<=20000);snapshot.ids.forEach{UUID.fromString(it)};snapshot.ids
    }
    fun manageable()=workspace.operation{workspace.catalog().filter{it.request().project().decodeToString() in setOf("",SharedAssets.IMAGE,SharedAssets.DOCUMENT,LibraryRepository.PROMPT)}}
    fun change(ids:Set<String>,archived:Boolean,afterSnapshot:()->Unit={}):Unit=workspace.operation {
        require(ids.isNotEmpty());val old=library.load();val prior=this.ids(old)
        val allowed=manageable().map{it.request().id().toString()}.toSet()
        require(ids.all{it in allowed}){"Only image, Prompt and exported-document assets can be archived"}
        val next=if(archived)prior+ids else prior-ids
        if(next==prior)return@operation
        require(next.size<=20000)
        val bytes=documentJson.encodeToString(ArchiveSnapshot(ids=next.toSortedSet())).toByteArray();require(bytes.size<=1024*1024)
        val id=UUID.randomUUID();val parent=reference(old)?.id?.let(UUID::fromString)
        workspace.assets.put(AssetStore.Request(id,"asset-archive.json",if(parent==null)AssetStore.Kind.ORIGINAL else AssetStore.Kind.DERIVED,parent,byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(bytes))
        afterSnapshot()
        val ref=documentJson.encodeToString(ArchiveReference(id=id.toString(),sha256=sha256(bytes)))
        library.update{it.copy(preferences=it.preferences+(KEY to ref))}
        Unit
    }
}
