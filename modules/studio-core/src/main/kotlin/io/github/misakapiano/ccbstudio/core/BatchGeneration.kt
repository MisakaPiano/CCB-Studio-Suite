package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable data class BatchDirection(val label:String,val width:Int,val height:Int,val repeats:Int=4)
@Serializable data class BatchPolicy(val intervalSeconds:Int=60,val runMinutes:Int=60,val restMinutes:Int=5,
    val roundRobin:Boolean=false,val anlasLimit:Int=0,val intervalJitterSeconds:Int=20,val restJitterMinutes:Int=2) {
    fun validate(){require(intervalSeconds in 10..3600 && runMinutes in 1..240 && restMinutes in 1..120 && anlasLimit in 0..100000 && intervalJitterSeconds in 0..600 && restJitterMinutes in 0..60)}
}
@Serializable data class BatchSlot(val id:String=newId(),val direction:BatchDirection,val round:Int,val seed:Long,
    val status:String="READY",val attempts:List<String> = emptyList(),val results:List<String> = emptyList())
@Serializable data class BatchGroup(val id:String=newId(),val name:String,val draft:NovelAiStudioDraft,
    val slots:List<BatchSlot>,val designApplication:DesignApplicationRef?=null) {
    val started get()=slots.any{it.attempts.isNotEmpty()}
    companion object {
        fun create(name:String,draft:NovelAiStudioDraft,directions:List<BatchDirection>,sharedSeeds:Boolean=false,
            designApplication:DesignApplicationRef?=null):BatchGroup {
            require(name.isNotBlank() && name.length<=100 && draft.basePrompt.isNotBlank())
            require(draft.imageGuidance.action==NovelAiGenerationAction.TEXT_TO_IMAGE && draft.imageGuidance.effectiveReferenceMode(draft.selectedModel)==NovelAiReferenceMode.NONE){"批次首版仅支持文生图；图像引导必须显式移除"}
            require(directions.size in 1..3 && directions.map{it.label}.distinct().size==directions.size)
            val seeds=List(8){kotlin.random.Random.nextLong(NovelAiGenerationSettings.MAX_SEED+1)}
            val slots=directions.flatMap{d->
                require(d.repeats in 0..8);require(NovelAiStudioSizePolicy.validationError(d.width,d.height)==null)
                List(d.repeats){r->BatchSlot(direction=d,round=r+1,seed=if(sharedSeeds)seeds[r] else kotlin.random.Random.nextLong(NovelAiGenerationSettings.MAX_SEED+1))}
            }
            require(slots.isNotEmpty())
            val frozen=draft.copy(continuousModeEnabled=false).withActiveSettings(draft.activeSettings.copy(count=1))
            slots.forEach{require(frozen.withSlot(it).activeSettings.validationError(frozen.activeCharacters.size)==null)}
            return BatchGroup(name=name.trim(),draft=frozen,slots=slots,designApplication=designApplication)
        }
    }
}
fun NovelAiStudioDraft.withSlot(slot:BatchSlot)=withActiveSettings(activeSettings.copy(count=1,customWidth=slot.direction.width,
    customHeight=slot.direction.height,seedMode=NovelAiSeedMode.FIXED,seed=slot.seed))
@Serializable data class BatchFailure(val code:String,val message:String,val slotId:String?=null,val notBefore:Long=0,val uncertain:Boolean=false)
@Serializable data class BatchArchive(val name:String,val total:Int,val completed:Int,val receipt:JobReceiptRef)
@Serializable data class BatchState(val id:String=newId(),val name:String="批量生图",val accountId:String?=null,
    val groups:List<BatchGroup> = emptyList(),val policy:BatchPolicy=BatchPolicy(),val mode:String="PAUSED",
    val error:BatchFailure?=null,val lastStartedAt:Long=0,val runStartedAt:Long=0,val restUntil:Long=0,
    val reservedAnlas:Int=0,val pauseRequested:Boolean=false,val nextRequestAt:Long=0,val archives:List<BatchArchive> = emptyList(),val endRequested:Boolean=false,val createdAt:Long=0,val accountSnapshot:AccountProfile?=null) {
    val total get()=groups.sumOf{it.slots.size}
    val completed get()=groups.sumOf{g->g.slots.count{it.status=="DONE"}}
    val skipped get()=groups.sumOf{g->g.slots.count{it.status=="SKIPPED"}}
    val running get()=mode in setOf("RUNNING","WAITING","RESTING")
    fun validate(){
        accountSnapshot?.let{it.validate();require(it.id==accountId&&it.service=="NAI")};policy.validate();require(groups.size<=40 && total<=1000 && name.length in 1..100 && reservedAnlas>=0 && archives.size<=1000)
        require(groups.map{it.id}.distinct().size==groups.size)
        val slots=groups.flatMap{it.slots};require(slots.map{it.id}.distinct().size==slots.size)
        require(slots.count{it.status=="RUNNING"}<=1)
        require(mode in setOf("PAUSED","RUNNING","WAITING","RESTING","ERROR","DONE","ENDED"))
        slots.forEach{require(it.status in setOf("READY","RUNNING","DONE","FAILED","SKIPPED"));require(it.attempts.size<=100)}
        require(mode!="ERROR" || error!=null)
    }
}
@Serializable data class BatchResultOrigin(val batchId:String,val groupId:String,val groupName:String,val slotId:String,
    val direction:String,val round:Int,val seed:Long,val batchName:String="")
data class BatchDispatch(val group:BatchGroup,val slot:BatchSlot,val jobId:String,val origin:BatchResultOrigin)

/** All transitions commit before becoming visible. No timer writes, request construction or automatic retries here. */
class BatchQueue(private val workspace:StudioWorkspace,private val random:(Int)->Int={kotlin.random.Random.nextInt(it)}) {
    val state get()=workspace.state.batch
    private fun update(change:(BatchState)->BatchState):BatchState=workspace.operation {
        val next=change(workspace.state.batch);next.validate();workspace.save(workspace.state.copy(batch=next));next
    }
    private fun BatchState.slot(id:String,change:(BatchSlot)->BatchSlot)=copy(groups=groups.map{g->g.copy(slots=g.slots.map{if(it.id==id)change(it)else it})})
    fun recover() {
        val old=state
        if(!old.running && old.groups.none{g->g.slots.any{it.status=="RUNNING"}})return
        update {s->
            var result=s.copy(mode="PAUSED",pauseRequested=false)
            s.groups.flatMap{it.slots}.filter{it.status=="RUNNING"}.forEach {slot->
                val job=workspace.state.jobs.lastOrNull{it.id==slot.attempts.lastOrNull()}
                if(job?.results?.size==1)result=result.slot(slot.id){it.copy(status="DONE",results=job.results)}
                else {
                    val code=job?.failureCode?:"UNCERTAIN"
                    result=result.slot(slot.id){it.copy(status="FAILED")}.copy(mode="ERROR",error=BatchFailure(code,
                        if(code=="429")"429：整批暂停，等待手动确认" else "上次请求结果未确认；重试可能再次计费",slot.id,job?.retryNotBefore?:0,code!="429"))
                }
            };if(s.endRequested&&result.error==null)result.copy(mode="ENDED",endRequested=false)else result.copy(endRequested=false)
        }
    }
    fun configure(accountId:String?,policy:BatchPolicy)=update{require(!it.running);it.copy(accountId=accountId,accountSnapshot=workspace.state.accounts.find{a->a.id==accountId&&a.service=="NAI"},policy=policy)}
    fun dispatchAccount():AccountProfile=workspace.operation {
        val s=state
        s.accountSnapshot?:workspace.state.accounts.singleOrNull{it.id==s.accountId&&it.service=="NAI"}
            ?.also{account->update{it.copy(accountSnapshot=account)}}?:throw DataProblem("ACCOUNT","请选择有效的 NAI 账户")
    }
    fun ensureCreatedAt(){if(state.groups.isNotEmpty()&&state.createdAt==0L)update{s->s.copy(createdAt=workspace.state.jobs.filter{job->s.groups.any{g->g.slots.any{job.id in it.attempts}}}.minOfOrNull{it.createdAt}?:System.currentTimeMillis())}}
    fun replaceGroups(groups:List<BatchGroup>)=update {s->
        require(!s.running && s.error==null && s.mode!="ENDED"){"请先处理暂停原因；已结束批次需新建"}
        s.groups.filter{it.started}.forEach{old->require(groups.any{it==old}){"已开始分组保留，请复制为新分组"}}
        s.copy(id=s.id.ifBlank{newId()},groups=groups,mode="PAUSED",createdAt=s.createdAt.takeIf{it>0}?:System.currentTimeMillis())
    }
    fun resume(now:Long,retry:Boolean=false,skip:Boolean=false)=update {s->
        require(!s.running && s.mode!="ENDED" && s.accountId!=null && s.total>0)
        val failure=s.error
        require(failure==null || retry || skip){"错误需要手动确认"}
        require(now>=(failure?.notBefore?:0)){"尚在服务端冷却期"}
        require(!skip || failure?.code!="429"){"429 不允许跳过继续"}
        var next=s
        failure?.slotId?.let{id->next=next.slot(id){it.copy(status=if(skip)"SKIPPED" else "READY")}}
        next.copy(mode="WAITING",error=null,pauseRequested=false,endRequested=false,runStartedAt=now)
    }
    fun pause()=update{if(it.mode=="RUNNING")it.copy(pauseRequested=true)else if(it.running)it.copy(mode="PAUSED",pauseRequested=false)else it}
    fun end()=update{if(it.mode=="RUNNING")it.copy(pauseRequested=true,endRequested=true)else it.copy(mode="ENDED",pauseRequested=false,endRequested=false)}
    fun newBatch()=update {archivedNext(it.name)}
    internal fun archivedNext(alias:String):BatchState {
        val s=state
        require(!s.running && (s.error==null || s.mode=="ENDED"&&System.currentTimeMillis()>=s.error.notBefore)){"请先结束并等待冷却；429 不能通过新建批次绕过"}
        return if(s.total==0)s else {
            val bytes=documentJson.encodeToString(s.copy(archives=emptyList())).toByteArray()
            require(bytes.size<=1024*1024)
            val ref=JobReceiptRef(workspace.requestPayload(bytes,null),sha256(bytes))
            BatchState(accountId=s.accountId,accountSnapshot=s.accountSnapshot,policy=s.policy,archives=s.archives+BatchArchive(alias,s.total,s.completed,ref),lastStartedAt=s.lastStartedAt,nextRequestAt=s.nextRequestAt,restUntil=s.restUntil)
        }
    }
    fun next(now:Long):Pair<BatchGroup,BatchSlot>? =workspace.operation {
        val s=state;if(!s.running || s.mode=="RUNNING")return@operation null
        if(now<s.lastStartedAt){fail(BatchFailure("CLOCK","系统时钟倒退，检查时间后手动继续"));return@operation null}
        if(s.restUntil>now || now<maxOf(s.nextRequestAt,s.lastStartedAt+s.policy.intervalSeconds*1000L))return@operation null
        if(s.restUntil>0){update{it.copy(mode="WAITING",restUntil=0,runStartedAt=now)};return@operation null}
        if(now-s.runStartedAt>=s.policy.runMinutes*60000L){update{it.copy(mode="RESTING",restUntil=now+(s.policy.restMinutes+random(s.policy.restJitterMinutes+1))*60000L)};return@operation null}
        val candidates=s.groups.mapNotNull{g->g.slots.firstOrNull{it.status=="READY"}?.let{g to it}}
        val next=if(s.policy.roundRobin)candidates.minByOrNull{(g,_)->g.slots.count{it.status in setOf("DONE","SKIPPED")}}else candidates.firstOrNull()
        if(next==null)update{it.copy(mode="DONE")}
        next
    }
    fun begin(slotId:String,now:Long,cost:Int):BatchDispatch =workspace.operation {
        val s=state;if(!s.running || s.pauseRequested || s.endRequested)throw GenerationNotSent();require(s.mode!="RUNNING" && s.error==null && now>=maxOf(s.nextRequestAt,s.lastStartedAt+s.policy.intervalSeconds*1000L))
        require(cost>=0 && cost<=s.policy.anlasLimit-s.reservedAnlas){"批次 Anlas 上限不足"}
        val group=s.groups.single{g->g.slots.any{it.id==slotId}};val slot=group.slots.single{it.id==slotId};require(slot.status=="READY")
        val jobId=newId()
        update{it.slot(slotId){item->item.copy(status="RUNNING",attempts=item.attempts+jobId)}.copy(mode="RUNNING",lastStartedAt=now,
            nextRequestAt=now+(s.policy.intervalSeconds+random(s.policy.intervalJitterSeconds+1))*1000L,reservedAnlas=it.reservedAnlas+cost)}
        BatchDispatch(group,slot,jobId,BatchResultOrigin(s.id,group.id,group.name,slot.id,slot.direction.label,slot.round,slot.seed,s.name))
    }
    fun complete(slotId:String,results:List<String>)=update {s->
        require(results.size==1 && s.groups.flatMap{it.slots}.single{it.id==slotId}.status=="RUNNING")
        val next=s.slot(slotId){it.copy(status="DONE",results=results)}
        next.copy(mode=if(s.endRequested)"ENDED" else if(next.completed+next.skipped==next.total)"DONE" else if(s.pauseRequested)"PAUSED" else "WAITING",pauseRequested=false,endRequested=false)
    }
    fun withdrawUnsent(slotId:String,cost:Int)=update{s->
        val slot=s.groups.flatMap{it.slots}.single{it.id==slotId}
        require(slot.status=="RUNNING")
        s.slot(slotId){it.copy(status="READY")}.copy(mode=if(s.endRequested)"ENDED"else"PAUSED",pauseRequested=false,endRequested=false,reservedAnlas=(s.reservedAnlas-cost).coerceAtLeast(0))
    }
    fun submitted(now:Long)=update{s->if(s.mode!="RUNNING"||s.pauseRequested||s.endRequested)throw GenerationNotSent();s.copy(lastStartedAt=now,nextRequestAt=now+(s.nextRequestAt-s.lastStartedAt).coerceAtLeast(s.policy.intervalSeconds*1000L))}
    fun fail(failure:BatchFailure)=update {s->
        val next=failure.slotId?.let{id->s.slot(id){it.copy(status="FAILED")}}?:s
        next.copy(mode="ERROR",error=failure,pauseRequested=false,endRequested=false)
    }
}
