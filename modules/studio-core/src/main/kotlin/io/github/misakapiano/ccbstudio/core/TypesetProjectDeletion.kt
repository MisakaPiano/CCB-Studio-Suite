package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import java.nio.file.*
import java.util.UUID

@Serializable data class TypesetManagedOutput(val id:String=newId(),val projectId:String,val path:String,val size:Long,val sha256:String,val assetId:String?=null,val status:String="ACTIVE",val detail:String="") {
    fun validate(){UUID.fromString(id);UUID.fromString(projectId);assetId?.let(UUID::fromString);require(path.length in 1..4096&&size>=0&&sha256.matches(Regex("[0-9a-f]{64}")));require(status in setOf("ACTIVE","DELETE_PENDING","DELETED","RETAINED"));require(detail.length<=300)}
}
data class TypesetDeletionEntry(val output:TypesetManagedOutput,val eligible:Boolean,val reason:String)
data class TypesetDeletionPlan(val project:TypesetProject,val entries:List<TypesetDeletionEntry>)

/** Explicit project destruction, distinct from reversible catalog removal. Immutable audit
 * records and asset-store history are not silently rewritten or treated as garbage. */
class TypesetProjectDeletion(private val w:StudioWorkspace) {
    fun register(projectId:String,files:List<TypesetManagedOutput>)=w.operation {
        require(w.state.typesetProjects.any{it.id==projectId});require(files.all{it.projectId==projectId&&it.status=="ACTIVE"})
        files.forEach{it.validate();val p=Path.of(it.path);require(p.isAbsolute);safePath(p);val f=fingerprintFile(p);require(f.size==it.size&&f.sha256==it.sha256)}
        val old=TypesetEditorExtrasStore.load(w)
        TypesetEditorExtrasStore.save(w,old.copy(outputs=old.outputs+files.filter{f->old.outputs.none{it.projectId==f.projectId&&it.path==f.path&&it.sha256==f.sha256}}))
    }
    private fun referencedAssets():Set<String> {
        val result=mutableSetOf<String>()
        w.state.typesetProjects.forEach{p->(listOf(p.current)+p.undo+p.redo).distinct().forEach{ref->result+=TypesetRepository(w).load(p.copy(current=ref)).pages.map{it.assetId}}}
        w.state.projects.forEach{p->(listOf(p.current)+p.undo+p.redo).forEach{result+=it.pages.map{it.assetId}}}
        result+=w.state.jobs.flatMap{it.results+it.composedResults};return result
    }
    private fun reason(f:TypesetManagedOutput,extras:TypesetEditorExtras,assets:Set<String>):String? {
        val p=Path.of(f.path).toAbsolutePath().normalize()
        if(f.assetId in assets)return "被工程或生成记录作为原图引用，保留"
        if(extras.outputs.any{it.id!=f.id&&it.projectId!=f.projectId&&it.path.equals(f.path,true)&&it.status!="DELETED"})return "其他工程也记录了该文件，保留"
        if(p.startsWith(w.root.toAbsolutePath().normalize())&&!p.startsWith(w.root.resolve("exports").toAbsolutePath().normalize()))return "工作区内部原图、记录、配置或备份，保留"
        if(p.any{it.toString().lowercase() in setOf(".git","assets","documents","secrets","backups","backup","plugins","src","source")})return "受保护目录，保留"
        return try {safePath(p);if(!Files.exists(p,LinkOption.NOFOLLOW_LINKS))"文件已不存在"else {val a=fingerprintFile(p);if(a.size!=f.size||a.sha256!=f.sha256)"文件已被修改，保留"else null}}catch(ex:Exception){"文件不可安全访问，保留"}
    }
    fun plan(id:String):TypesetDeletionPlan=w.operation {
        val p=w.state.typesetProjects.single{it.id==id};val extras=TypesetEditorExtrasStore.load(w);val assets=referencedAssets()
        TypesetDeletionPlan(p,extras.outputs.filter{it.projectId==id&&it.status!="DELETED"}.map{f->val r=reason(f,extras,assets);TypesetDeletionEntry(f,r==null,r?:"本工程生成文件；确认后永久删除")})
    }
    fun delete(plan:TypesetDeletionPlan,selected:Set<String>,confirmed:Boolean,hook:(String)->Unit={}):List<TypesetManagedOutput> = w.operation {
        require(confirmed){"删除必须明确确认"};val p=w.state.typesetProjects.single{it.id==plan.project.id};require(p==plan.project){"工程已改变，请重新检查删除范围"}
        val old=TypesetEditorExtrasStore.load(w);val live=plan(p.id);require(selected.all{id->plan.entries.any{it.output.id==id&&it.eligible}})
        require(selected.all{id->live.entries.any{it.output.id==id&&it.eligible&&it.output==plan.entries.single{it.output.id==id}.output}}){"生成文件或引用已改变，请重新确认"}
        val retirement=TypesetSnapshotRetirements.prepare(w,p)
        val next=old.copy(lastProjectId=old.lastProjectId.takeUnless{it==p.id},projectSelectionRecorded=true,retirements=old.retirements+retirement,
            projectRecords=old.projectRecords.filterNot{it.projectId==p.id}+TypesetProjectRecord(p.id,"DELETED"),outputs=old.outputs.map{if(it.id in selected)it.copy(status="DELETE_PENDING",detail="用户已确认删除")else it})
        hook("BEFORE_PROJECT_COMMIT")
        TypesetEditorExtrasStore.save(w,next,w.state.typesetProjects.filterNot{it.id==p.id})
        hook("PROJECT_REMOVED")
        TypesetSnapshotRetirements.finish(w,p.id)
        finish(selected,hook)
    }
    /** Pending intents survive interruption. Only explicitly confirmed IDs are ever retried. */
    fun retryPending(selected:Set<String>,confirmed:Boolean):List<TypesetManagedOutput> = w.operation {require(confirmed);val old=TypesetEditorExtrasStore.load(w);require(selected.all{id->old.outputs.any{it.id==id&&it.status in setOf("DELETE_PENDING","RETAINED")}});TypesetEditorExtrasStore.save(w,old.copy(outputs=old.outputs.map{if(it.id in selected)it.copy(status="DELETE_PENDING")else it}));finish(selected){}}
    fun retryRetirement(id:String,confirmed:Boolean):TypesetSnapshotRetirement=w.operation {require(confirmed);TypesetSnapshotRetirements.finish(w,id)}
    private fun finish(selected:Set<String>,hook:(String)->Unit):List<TypesetManagedOutput> {
        val old=TypesetEditorExtrasStore.load(w);val refs=referencedAssets()
        val updated=old.outputs.map{f->if(f.id !in selected||f.status!="DELETE_PENDING")f else {
            val block=reason(f,old,refs)
            val result=if(block=="文件已不存在")f.copy(status="DELETED",detail=block)else if(block!=null)f.copy(status="RETAINED",detail=block)else try{
                WindowsOwnedOutputDeletion.delete(Path.of(f.path),f.size,f.sha256)
                f.copy(status="DELETED",detail="已按确认清单永久删除导出副本")
            }catch(ex:Exception){f.copy(status="RETAINED",detail="占用、权限或归属复核未通过，文件保留：${ex.message?.take(160)}")}
            hook("OUTPUT_PROCESSED");result
        }}
        TypesetEditorExtrasStore.save(w,old.copy(outputs=updated));return updated.filter{it.id in selected}
    }
}
