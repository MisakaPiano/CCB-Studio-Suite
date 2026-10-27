package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** A catalog state, never an exported bundle or duplicate project snapshot. */
@Serializable data class TypesetProjectRecord(val projectId:String,val state:String="ACTIVE",val archivedAt:Long?=null,val parent:String?=null) {
    fun validate(){UUID.fromString(projectId);require(state in setOf("ACTIVE","ARCHIVED","REMOVED","DELETED"));archivedAt?.let{require(it>0)};if(state=="ARCHIVED")require(archivedAt!=null);parent?.let(UUID::fromString)}
}
/** References only. These IDs do not confer exclusive ownership or permission to delete files. */
data class TypesetManagedResources(val projectId:String,val originals:Set<String>,val documents:Set<String>,val exchangeSources:Set<String>,val fonts:Set<String>)
fun typesetProjectTitle(project:TypesetProject,record:TypesetProjectRecord?):String=record?.archivedAt?.let{project.name+" · "+DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))}?:project.name

class TypesetProjectArchives(private val w:StudioWorkspace) {
    private fun extras()=TypesetEditorExtrasStore.load(w)
    fun active():List<TypesetProject> = w.operation{val hidden=extras().projectRecords.filter{it.state!="ACTIVE"}.map{it.projectId}.toSet();w.state.typesetProjects.filterNot{it.id in hidden}}
    fun archived():List<TypesetProject> = w.operation{val entries=extras().projectRecords.filter{it.state=="ARCHIVED"}.associateBy{it.projectId};w.state.typesetProjects.filter{it.id in entries}.sortedByDescending{entries[it.id]!!.archivedAt}}
    fun record(id:String)=w.operation{extras().projectRecords.firstOrNull{it.projectId==id}}
    fun title(project:TypesetProject):String=typesetProjectTitle(project,record(project.id))
    private fun current(id:String,revision:Long):TypesetProject {
        val p=w.state.typesetProjects.single{it.id==id};require(p.revision==revision){"Stale editor revision; catalog unchanged"};return p
    }
    fun create(name:String,ids:List<String>,parent:String?,change:TypesetOrganizationChange?=null):TypesetProject=w.operation {
        val org=TypesetOrganizationChanges.resolve(w,change);require(parent==null||org.folders.any{it.id==parent})
        val old=TypesetOrganizationChanges.relocate(extras(),w.state.generationOrganization,org)
        val p=TypesetRepository(w).prepareCreate(name.trim(),ids)
        TypesetEditorExtrasStore.save(w,old.copy(lastProjectId=p.id,projectSelectionRecorded=true,projectRecords=old.projectRecords+TypesetProjectRecord(p.id,parent=parent)),w.state.typesetProjects+p,org);p
    }
    fun archive(id:String,revision:Long,name:String,parent:String?,at:Long=System.currentTimeMillis(),change:TypesetOrganizationChange?=null):TypesetProject=w.operation {
        val org=TypesetOrganizationChanges.resolve(w,change)
        val alias=name.trim();require(alias.isNotEmpty()&&alias.length<=100&&at>0)
        require(parent==null||org.folders.any{it.id==parent}){"上级组织已改变，请重新选择"}
        val p=current(id,revision);val old=TypesetOrganizationChanges.relocate(extras(),w.state.generationOrganization,org);val previous=old.projectRecords.firstOrNull{it.projectId==id}
        require(previous?.state!="REMOVED"){"工程已移出列表"}
        if(previous?.state=="ARCHIVED"&&p.name==alias&&previous.parent==parent)return@operation p
        require(previous?.state!="ARCHIVED"){"请先重新打开工程"}
        TypesetRepository(w).load(p) // Verify the retained recipe, without rewriting its bytes.
        val next=p.copy(name=alias,revision=p.revision+1)
        val value=old.copy(lastProjectId=old.lastProjectId.takeUnless{it==id},projectSelectionRecorded=true,projectRecords=old.projectRecords.filterNot{it.projectId==id}+TypesetProjectRecord(id,"ARCHIVED",at,parent))
        TypesetEditorExtrasStore.save(w,value,w.state.typesetProjects.map{if(it.id==id)next else it},org);next
    }
    fun reopen(id:String,revision:Long):TypesetProject=w.operation {
        val p=current(id,revision);val old=extras();val r=old.projectRecords.firstOrNull{it.projectId==id}
        require(r?.state !in setOf("REMOVED","DELETED")){"工程已移出或删除"}
        TypesetRepository(w).load(p)
        val next=old.copy(lastProjectId=id,projectSelectionRecorded=true,projectRecords=old.projectRecords.map{if(it.projectId==id)it.copy(state="ACTIVE")else it})
        if(next!=old)TypesetEditorExtrasStore.save(w,next);p
    }
    /** Logical removal retains the project/reference graph for future recycle/file operations. */
    fun removeFromList(id:String,revision:Long)=w.operation {
        current(id,revision);val old=extras();val r=old.projectRecords.firstOrNull{it.projectId==id}?:TypesetProjectRecord(id)
        val next=old.copy(lastProjectId=old.lastProjectId.takeUnless{it==id},projectSelectionRecorded=true,projectRecords=old.projectRecords.filterNot{it.projectId==id}+r.copy(state="REMOVED"))
        if(next!=old)TypesetEditorExtrasStore.save(w,next)
    }
    fun resources(id:String):TypesetManagedResources=w.operation {
        val p=w.state.typesetProjects.single{it.id==id};val refs=(listOf(p.current)+p.undo+p.redo).distinct()
        val originals=linkedSetOf<String>();val fonts=linkedSetOf<String>()
        refs.forEach{ref->val d=TypesetRepository(w).load(p.copy(current=ref));originals+=d.pages.map{it.assetId};TypesetFonts.documentFonts(d).forEach{alias->w.state.typesetFonts.firstOrNull{it.alias==alias}?.let{fonts+=it.sourceId;fonts+=it.decodedId}}}
        TypesetManagedResources(id,originals,refs.flatMap{it.chunks}.toSet(),setOfNotNull(p.sourceExchangeId),fonts)
    }
}
