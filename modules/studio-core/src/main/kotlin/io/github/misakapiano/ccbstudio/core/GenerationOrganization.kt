package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Serializable data class GenerationFolder(val id:String=newId(),val name:String,val kind:String="project",val parent:String?=null)
@Serializable data class GenerationBatchGroup(val id:String,val name:String,val images:List<String> = emptyList())
@Serializable data class GenerationBatch(val id:String=newId(),val kind:String,val alias:String,val createdAt:Long=System.currentTimeMillis(),val archivedAt:Long?=null,val parent:String?=null,val groups:List<GenerationBatchGroup> = emptyList()) {
    val title get()=alias+" · "+DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(createdAt))
}
@Serializable data class GenerationOrganization(val folders:List<GenerationFolder> = emptyList(),val batches:List<GenerationBatch> = emptyList(),val currentManual:String?=null) {
    fun validate(){
        require(folders.size<=1000 && batches.size<=10000)
        require(folders.distinctBy{it.id}.size==folders.size && batches.distinctBy{it.id}.size==batches.size)
        folders.forEach{f->UUID.fromString(f.id);require(f.name.isNotBlank()&&f.name.length<=100&&f.kind in setOf("project","chapter","folder"));val chain=mutableSetOf(f.id);var p=f.parent
            while(p!=null){require(chain.add(p)&&chain.size<=32){"组织不能循环或超过32层"};p=folders.single{it.id==p}.parent}}
        batches.forEach{UUID.fromString(it.id);require(it.kind in setOf("manual","auto")&&it.alias.isNotBlank()&&it.alias.length<=100&&it.createdAt>0);require(it.parent==null||folders.any{f->f.id==it.parent});require(it.groups.size<=40&&it.groups.distinctBy{g->g.id}.size==it.groups.size);it.groups.forEach{g->UUID.fromString(g.id);require(g.name.isNotBlank()&&g.name.length<=100&&g.images.size<=1000&&g.images.distinct().size==g.images.size);g.images.forEach{UUID.fromString(it)}}}
        require(currentManual==null||batches.any{it.id==currentManual&&it.kind=="manual"&&it.archivedAt==null})
    }
    fun descendants(id:String):Set<String> = setOf(id)+folders.filter{it.parent==id}.flatMap{descendants(it.id)}
    /** Same lazy eligibility as the view predicates; one subtree per nonempty selection. */
    fun batchesInSubtree(id:String?,archivedOnly:Boolean=false):List<GenerationBatch> {
        val eligible=if(archivedOnly)batches.filter{it.archivedAt!=null}else batches
        if(eligible.isEmpty())return emptyList()
        val scope=descendants(id!!)
        return eligible.filter{it.parent in scope}
    }
    fun path(id:String?):String {val f=folders.find{it.id==id}?:return "";return listOf(path(f.parent),f.name).filter{it.isNotEmpty()}.joinToString(" / ")}
}

/** Batch aliases/placement are mutable organization. Receipts and image bytes are never rewritten. */
class GenerationOrganizations(private val w:StudioWorkspace) {
    companion object {const val MANUAL_IMAGE_LIMIT=1000}
    val state get()=w.state.generationOrganization
    fun requireManualCapacity(id:String,additional:Int)=w.operation {
        require(state.currentManual==id)
        require(additional>=0 && manualImages().size+additional<=MANUAL_IMAGE_LIMIT){"当前手动批次最多1000张（含合成结果），请先命名并归档；没有发送请求"}
    }
    fun change(expected:GenerationOrganization,next:GenerationOrganization)=w.operation {require(state==expected){"组织记录已改变，请刷新"};next.validate();w.save(w.state.copy(generationOrganization=next))}
    fun addFolder(folder:GenerationFolder)=w.operation{val s=state;change(s,s.copy(folders=s.folders+folder))}
    fun ensureManual():GenerationBatch=w.operation {
        state.batches.find{it.id==state.currentManual}?:GenerationBatch(kind="manual",alias="手动批次").also{b->val s=state;change(s,s.copy(batches=s.batches+b,currentManual=b.id))}
    }
    fun manualImages():List<String> {val id=state.currentManual?:return emptyList();return w.state.jobs.filter{it.manualBatchId==id}.flatMap{it.results+it.composedResults}.distinct()}
    fun archiveManual(id:String,alias:String,parent:String?)=w.operation {
        val s=state;require(s.currentManual==id && manualImages().isNotEmpty())
        require(w.state.jobs.none{it.manualBatchId==id&&it.state in setOf("QUEUED","RUNNING")}){"请等待当前生成完成"}
        change(s,s.copy(currentManual=null,batches=s.batches.map{if(it.id==id)it.copy(alias=alias.trim(),parent=parent,archivedAt=System.currentTimeMillis(),groups=manualImages().chunked(MANUAL_IMAGE_LIMIT).mapIndexed{index,images->GenerationBatchGroup(if(index==0)id else newId(),if(index==0)"手动生成"else"手动生成 ${index+1}",images)})else it}))
    }
    fun archiveAutomatic(alias:String,parent:String?)=w.operation {
        val s=state;val batch=w.state.batch;require(batch.total>0)
        val metadata=GenerationBatch(batch.id,"auto",alias.trim(),batch.createdAt.takeIf{it>0}?:w.state.jobs.filter{job->batch.groups.any{g->g.slots.any{job.id in it.attempts}}}.minOfOrNull{it.createdAt}?:System.currentTimeMillis(),System.currentTimeMillis(),parent,batch.groups.map{g->GenerationBatchGroup(g.id,g.name,g.slots.flatMap{it.results}.distinct())})
        val organization=s.copy(batches=s.batches.filterNot{it.id==batch.id}+metadata);organization.validate()
        val next=BatchQueue(w).archivedNext(alias.trim())
        w.save(w.state.copy(batch=next,generationOrganization=organization))
    }
    fun reopenAutomatic(id:String)=w.operation {
        val current=w.state.batch;require(current.total==0&&!current.running&&System.currentTimeMillis()>=(current.error?.notBefore?:0)){"请先保存并归档当前批次"}
        val snapshot=current.archives.asSequence().map{a->val bytes=w.bytes(a.receipt.assetId);require(sha256(bytes)==a.receipt.sha256);documentJson.decodeFromString<BatchState>(bytes.decodeToString())}.first{it.id==id}
        val metadata=state.batches.single{it.id==id};val now=System.currentTimeMillis()
        val copy=snapshot.copy(id=newId(),name=(metadata.alias+" 副本").take(100),createdAt=now,mode="PAUSED",error=null,pauseRequested=false,endRequested=false,
            groups=snapshot.groups.map{g->g.copy(id=newId(),slots=g.slots.map{slot->slot.copy(id=newId(),status=if(slot.status=="DONE")"DONE"else"READY",attempts=if(slot.status=="DONE")slot.attempts else emptyList(),results=if(slot.status=="DONE")slot.results else emptyList())})},
            archives=current.archives,lastStartedAt=current.lastStartedAt,nextRequestAt=current.nextRequestAt,restUntil=current.restUntil,runStartedAt=0)
        copy.validate();w.save(w.state.copy(batch=copy))
    }
}

fun generationBatchImages(org:GenerationOrganization,id:String,p:GalleryHistoryProjection):Set<String> = org.batches.find{it.id==id}?.groups.orEmpty().flatMap{it.images}.toSet()+p.batchOrigins.filterValues{it.batchId==id}.keys
