package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.runtime.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*

internal class BatchController(private val c:WorkbenchController) {
    val queue=BatchQueue(c.workspace)
    var state by mutableStateOf(queue.state);private set
    var shown by mutableStateOf(false)
    var detached by mutableStateOf(false)
    var batchMode by mutableStateOf(false)
    var selected by mutableStateOf(setOf<String>())
    var current by mutableStateOf<String?>(state.groups.firstOrNull()?.id)
    var checkMode by mutableStateOf(false)
    var follow by mutableStateOf(true)
    var detailFraction by mutableStateOf(.36f)
    var changing by mutableStateOf(false);private set
    var phase by mutableStateOf("");private set
    var directions by mutableStateOf(defaultDirections(c.draft))
    private var runner:Job?=null
    private var editJob:Job?=null
    private var undo:List<BatchGroup>?=null
    val canUndo get()=undo!=null && !state.running && state.error==null
    val editing get()=!state.running && state.error==null && !changing
    init{queue.recover();state=queue.state}
    fun refresh(){state=queue.state}
    fun open(){shown=true;c.view="创作"}
    fun action(body:()->Unit) {
        if(changing || c.maintenance)return
        changing=true
        editJob=c.scope.launch {try{withContext(Dispatchers.IO){body()};refresh()}catch(e:Exception){c.problem=e.message?.take(180)?:"批次操作未完成"}finally{changing=false}}
    }
    fun add(draft:NovelAiStudioDraft=c.draft,name:String="分组 ${state.groups.size+1}",lineage:DesignApplicationRef?=null)=action {
        val group=BatchGroup.create(name,draft,directions,designApplication=lineage)
        undo=queue.state.groups;queue.replaceGroups(queue.state.groups+group)
        if(queue.state.accountId==null)queue.configure(c.naiId,queue.state.policy)
        current=group.id;selected=setOf(group.id)
    }
    fun addCurrent()=add(lineage=DesignProvenance(c.workspace).current())
    fun replace(group:BatchGroup)=action{undo=queue.state.groups;queue.replaceGroups(queue.state.groups.map{if(it.id==group.id)group else it})}
    fun remove()=action{undo=queue.state.groups;queue.replaceGroups(queue.state.groups.filterNot{it.id in selected})}
    fun duplicate()=action {
        val copies=queue.state.groups.filter{it.id in selected}.map{g->g.copy(id=newId(),name=(g.name+" 副本").take(100),slots=g.slots.map{it.copy(id=newId(),status="READY",attempts=emptyList(),results=emptyList(),seed=kotlin.random.Random.nextLong(NovelAiGenerationSettings.MAX_SEED+1))})}
        undo=queue.state.groups;queue.replaceGroups(queue.state.groups+copies);selected=copies.map{it.id}.toSet();current=copies.firstOrNull()?.id?:current
    }
    fun reorder(ids:Set<String>,before:String)=action {
        val groups=queue.state.groups;if(before !in ids){val rest=groups.filterNot{it.id in ids}.toMutableList();val index=rest.indexOfFirst{it.id==before};if(index>=0){rest.addAll(index,groups.filter{it.id in ids});undo=groups;queue.replaceGroups(rest)}}
    }
    fun undo()=action{undo?.let{prior->val old=queue.state.groups;queue.replaceGroups(prior);undo=old}}
    fun configure(account:String?,policy:BatchPolicy)=action{queue.configure(account,policy)}
    fun select(id:String,ctrl:Boolean=false,shift:Boolean=false) {
        val ids=state.groups.map{it.id};val anchor=ids.indexOf(current);val target=ids.indexOf(id)
        selected=when{shift&&anchor>=0&&target>=0->selected+ids.subList(minOf(anchor,target),maxOf(anchor,target)+1);ctrl||checkMode->if(id in selected)selected-id else selected+id;else->setOf(id)};current=id
    }
    fun start(retry:Boolean=false,skip:Boolean=false) {
        if(runner?.isActive==true || changing || c.maintenance)return
        runner=c.scope.launch {
            try {
                withContext(Dispatchers.IO){queue.resume(System.currentTimeMillis(),retry,skip)};refresh()
                while(queue.state.running && !c.maintenance){
                    if(c.busy || c.library.collectionBusy){phase="等待当前操作完成";delay(250);continue}
                    val next=withContext(Dispatchers.IO){queue.next(System.currentTimeMillis())};refresh()
                    if(next==null){phase=if(state.mode=="RESTING")"定时休息" else "间隔等待";delay(250);continue}
                    phase="正在确认额度"
                    try{c.executeBatch(queue,next.first,next.second){phase=it}}
                    catch(e:CancellationException){throw e}
                    catch(e:Exception){
                        val running=queue.state.groups.flatMap{it.slots}.firstOrNull{it.status=="RUNNING"}
                        val failure=e as? GenerationFailure
                        withContext(Dispatchers.IO){
                            val saved=running?.attempts?.lastOrNull()?.let{id->c.workspace.state.jobs.firstOrNull{it.id==id}}?.results.orEmpty()
                            if(running!=null&&saved.size==1)queue.complete(running.id,saved)
                            queue.fail(BatchFailure(failure?.code?:if(e is DataProblem)e.status else "UNCERTAIN",
                            if(failure?.code=="429")"429：整批暂停；冷却结束后仍需手动确认" else if(e is DataProblem) e.message?:"操作未完成" else "请求或结果未确认；检查后决定重试或跳过",
                            running?.id?.takeIf{saved.size!=1},failure?.notBefore?:0,saved.size!=1&&(failure?.uncertain?: (running!=null))))}
                    };refresh()
                }
            }catch(e:CancellationException){withContext(NonCancellable+Dispatchers.IO){
                val running=queue.state.groups.flatMap{it.slots}.firstOrNull{it.status=="RUNNING"}
                if(running!=null)queue.fail(BatchFailure("UNCERTAIN","请求已中止，服务端结果未确认；不会自动重发",running.id,uncertain=true))else queue.pause()
            };refresh()}
            catch(e:Exception){c.problem="批次已停止：${e.message?.take(160)}；已有结果保留";runCatching{refresh()}}
            finally{phase=""}
        }
    }
    fun pause()=action{queue.pause()}
    fun cancel(){runner?.cancel()}
    fun end()=action{queue.end()}
    suspend fun drain(){editJob?.join();runner?.cancelAndJoin();withContext(Dispatchers.IO){queue.pause()};refresh()}
    fun newBatch()=action{queue.newBatch();undo=null;selected=emptySet();current=null}
    fun results(){val ids=state.groups.flatMap{g->g.slots.flatMap{it.results}}.toSet();c.gallery.enterBatch(ids,state.name,"batch:${state.id}");c.selection=emptySet();c.current=ids.lastOrNull();c.view="图库";shown=false}
    companion object {
        fun defaultDirections(draft:NovelAiStudioDraft)=listOf("横" to NovelAiAspectRatio.LANDSCAPE,"纵" to NovelAiAspectRatio.PORTRAIT,"方" to NovelAiAspectRatio.SQUARE).map{(label,aspect)->
            val size=draft.activeSettings.copy(sizeTier=if(draft.activeSettings.sizeTier==NovelAiSizeTier.WALLPAPER&&aspect==NovelAiAspectRatio.SQUARE)NovelAiSizeTier.NORMAL else draft.activeSettings.sizeTier,aspectRatio=aspect,customWidth=null,customHeight=null).imageSize()
            BatchDirection(label,size.width,size.height)
        }
    }
}
