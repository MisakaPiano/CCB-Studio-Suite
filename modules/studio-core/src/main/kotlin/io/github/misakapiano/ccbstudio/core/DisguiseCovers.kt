package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.util.UUID

@Serializable data class DisguiseCover(val id:String,val name:String,val sha256:String)
@Serializable data class DisguiseCoverState(val version:Int=1,val covers:List<DisguiseCover> = emptyList(),val selected:String?=null)

/** Immutable cover originals and versioned library preferences follow ordinary workspace backup. */
class DisguiseCovers(private val workspace:StudioWorkspace) {
    companion object {const val TAG="CCB_APNG_COVER_V1";const val KEY="apngDisguiseCovers"}
    private val library=LibraryRepository(workspace)
    fun load():DisguiseCoverState=workspace.operation {
        val raw=library.load().preferences[KEY]?:return@operation DisguiseCoverState()
        val state=documentJson.decodeFromString<DisguiseCoverState>(raw)
        if(state.version!=1)throw DataProblem("UNSUPPORTED_VERSION","Disguise cover settings retained")
        require(state.covers.size<=64 && state.covers.map{it.id}.distinct().size==state.covers.size)
        require(state.selected==null || state.covers.any{it.id==state.selected})
        state.covers.forEach{cover->
            require(cover.name.isNotBlank()&&cover.name.length<=100)
            val asset=workspace.assets.read(UUID.fromString(cover.id)).require()
            require(asset.request().project().decodeToString()==TAG && asset.sha256()==cover.sha256 && asset.size() in 1..32L*1024*1024){"Disguise cover identity mismatch"}
        };state
    }
    private fun save(state:DisguiseCoverState){library.update{it.copy(preferences=it.preferences+(KEY to documentJson.encodeToString(state)))}}
    /** Caller validates static image pixels before admission; bytes here are retained without re-encoding. */
    fun add(name:String,filename:String,bytes:ByteArray):DisguiseCover=workspace.operation {
        val state=load();val alias=name.trim();require(alias.isNotBlank()&&alias.length<=100 && bytes.size in 1..32*1024*1024)
        val hash=sha256(bytes);state.covers.firstOrNull{it.sha256==hash}?.let{save(state.copy(selected=it.id));return@operation it}
        require(state.covers.size<64){"最多保存64个封面，请先移除不再使用的配置"}
        val id=UUID.randomUUID()
        workspace.assets.put(AssetStore.Request(id,filename.take(200),AssetStore.Kind.ORIGINAL,null,byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(bytes))
        val cover=DisguiseCover(id.toString(),alias,hash)
        save(state.copy(covers=state.covers+cover,selected=cover.id));cover
    }
    fun select(id:String?)=workspace.operation {val state=load();require(id==null||state.covers.any{it.id==id});save(state.copy(selected=id))}
    fun rename(id:String,name:String)=workspace.operation {val state=load();val alias=name.trim();require(alias.isNotBlank()&&alias.length<=100&&state.covers.any{it.id==id});save(state.copy(covers=state.covers.map{if(it.id==id)it.copy(name=alias)else it}))}
    /** Removes a preset only; immutable bytes and old output provenance are not deleted. */
    fun remove(id:String)=workspace.operation {val state=load();require(state.covers.any{it.id==id});save(state.copy(covers=state.covers.filterNot{it.id==id},selected=state.selected.takeUnless{it==id}))}
    fun bytes(cover:DisguiseCover):ByteArray=workspace.operation {require(load().covers.any{it==cover});workspace.bytes(cover.id).also{require(sha256(it)==cover.sha256)}}
}
