package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import io.github.misakapiano.ccbstudio.assets.EditorSnapshotRetirement
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable data class TypesetRetiredAsset(val id:String,val size:Long,val sha256:String)
@Serializable data class TypesetSnapshotRetirement(val projectId:String,val name:String,val assets:List<TypesetRetiredAsset>,val state:String="PENDING",val detail:String=""){
    fun validate(){UUID.fromString(projectId);require(name.length<=100&&assets.size<=50000&&assets.distinctBy{it.id}.size==assets.size&&state in setOf("PENDING","COMPLETE")&&detail.length<=300);assets.forEach{UUID.fromString(it.id);require(it.size>=0&&it.sha256.matches(Regex("[0-9a-f]{64}")))}}
}

/** Reference proof is constructed BEFORE the project is removed. Originals, other
 * projects (including their older snapshots), export provenance, fonts and presets
 * protect their content. Only tagged editor payloads enter the retirement allowlist. */
internal object TypesetSnapshotRetirements {
    fun prepare(w:StudioWorkspace,p:TypesetProject):TypesetSnapshotRetirement {
        val own=w.editorReferences(setOf(p.id));val other=w.editorReferences(w.state.typesetProjects.filterNot{it.id==p.id}.map{it.id}.toSet())
        val protected=(other.first.flatMap{it.chunks}+other.second).toMutableSet()
        w.state.typesetPreferences.sourceExchangeId?.let{protected+=it};w.state.typesetFonts.forEach{protected+=it.sourceId;protected+=it.decodedId}
        val catalog=w.catalog()
        catalog.mapNotNull{SharedAssets.provenance(it)}.forEach{protected+=it.snapshot.chunks;protected+=it.sourceIds}
        val candidates=(own.first.flatMap{it.chunks}+own.second).toSet()
        catalog.filter{it.request().id().toString() !in candidates}.mapNotNull{it.request().parentId()?.toString()}.forEach{protected+=it}
        val allowed=(candidates-protected).map{id->w.assets.read(UUID.fromString(id)).require()}.filter{a->
            val r=a.request();r.kind()==AssetStore.Kind.ORIGINAL&&r.parentId()==null&&r.recipe().isEmpty()&&
                (r.project().decodeToString()==TypesetRepository.TAG||(r.project().decodeToString()==TypesetSourceExchange.TAG&&r.name()=="source.typesetter-workspace.json"))
        }
        return TypesetSnapshotRetirement(p.id,p.name,allowed.map{TypesetRetiredAsset(it.request().id().toString(),it.size(),it.sha256())})
    }
    fun finish(w:StudioWorkspace,id:String):TypesetSnapshotRetirement {
        val old=TypesetEditorExtrasStore.load(w);val pending=old.retirements.single{it.projectId==id};if(pending.state=="COMPLETE")return pending
        require(w.state.typesetProjects.none{it.id==id}){"工程仍在使用，停止回收"}
        val result=try {
            val expected=pending.assets.map{item->EditorSnapshotRetirement.identity(w.assets,UUID.fromString(item.id)).also{require(it.size()==item.size&&it.sha256()==item.sha256)}}
            EditorSnapshotRetirement.retire(w.assets,expected){path,size,hash->WindowsOwnedOutputDeletion.delete(path,size,hash)}
            pending.copy(state="COMPLETE",detail="独占编辑内容已回收；原图、共享资源、备份和最小删除审计记录保留")
        }catch(ex:Exception){pending.copy(detail="独占进度回收未完成，保留重试记录：${ex.message?.take(180)}")}
        TypesetEditorExtrasStore.save(w,old.copy(retirements=old.retirements.map{if(it.projectId==id)result else it}));return result
    }
}
