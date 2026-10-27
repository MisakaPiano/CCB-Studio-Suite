package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.runtime.*
import com.example.chatbar.desktop.StudioFileDialogs
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor

/** UI draft is separate from the committed immutable snapshot. A failed save keeps the draft visible. */
internal data class StaticEditingChoice(val name:String?,val ids:List<String>,val projectId:String?=null,val revision:Long?=null,val organization:TypesetOrganizationChange?=null,val parent:String?=null)
internal data class TypesetCanvasSource(val pageId:String,val assetId:String,val width:Int,val height:Int)
internal class TypesetEditorController(private val c:WorkbenchController) {
    private val ui=c.scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    private val repository=TypesetRepository(c.workspace)
    val archives=TypesetProjectArchives(c.workspace)
    private val commit=Mutex()
    private var pending:Job?=null
    // Pending styles are usable immediately, before the debounced disk write.
    private var pendingTextStyle:TypesetText?=null
    private var pendingMaskStyle:MaskToolSettings?=null
    var project by mutableStateOf<TypesetProject?>(null);private set
    var document by mutableStateOf<TypesetDocument?>(null);private set
    var dirty by mutableStateOf(false);private set
    var pageId by mutableStateOf<String?>(null)
    var selectedPages by mutableStateOf(setOf<String>())
    var objectId by mutableStateOf<String?>(null)
    var tool by mutableStateOf("select")
    var propertySection by mutableStateOf("文字")
    val autoMask by lazy{TypesetAutoMaskController(c,this)}
    internal val queueRasters=TypesetQueueRasterCache()
    private var rememberedProject:String?=null
    private var rememberedProjectLoaded=false
    private suspend fun rememberProject(id:String?) {
        if(rememberedProjectLoaded&&rememberedProject==id)return
        withContext(Dispatchers.IO){c.workspace.operation {
            val old=TypesetEditorExtrasStore.load(c.workspace)
            if(old.lastProjectId!=id||!old.projectSelectionRecorded)TypesetEditorExtrasStore.save(c.workspace,old.copy(lastProjectId=id,projectSelectionRecorded=true))
        }}
        rememberedProject=id;rememberedProjectLoaded=true
    }
    fun resumeLastProject(){
        if(project!=null||c.busy||c.maintenance)return
        c.run("恢复编辑工程") {
            val extras=TypesetEditorExtrasStore.load(c.workspace)
            withContext(ui){editorExtras=extras}
            if(extras.projectSelectionRecorded&&extras.lastProjectId==null)return@run
            val id=extras.lastProjectId?:archives.active().lastOrNull()?.id?:return@run
            if(archives.active().none{it.id==id})throw DataProblem("EDITOR_PROJECT_MISSING","最近编辑工程不可用，原资料保留，请选择工程")
            loadProject(id)
        }
    }
    fun archiveCurrent(name:String,parent:String?,at:Long=System.currentTimeMillis(),completed:()->Unit={},organization:TypesetOrganizationChange?=null)=c.run("归档编辑工程") {
        drain();val p=project?:return@run
        archives.archive(p.id,p.revision,name,parent,at,organization)
        val extras=TypesetEditorExtrasStore.load(c.workspace)
        withContext(ui){clearCurrent();editorExtras=extras;rememberedProject=null;rememberedProjectLoaded=true;completed();c.operationNotice="已归档同一工程；原图与工程未复制，编辑历史保留"}
    }
    fun collapseCurrent(completed:()->Unit={})=c.run("收起当前工程") {
        drain();rememberProject(null);val extras=TypesetEditorExtrasStore.load(c.workspace)
        withContext(ui){clearCurrent();editorExtras=extras;completed();c.operationNotice="进度已保存并收起；可从打开本机工程继续"}
    }
    var deletionPlan by mutableStateOf<TypesetDeletionPlan?>(null);private set
    fun prepareDeletion(id:String)=c.run("检查工程删除范围") {
        pending?.cancelAndJoin()
        commit.withLock {val plan=TypesetProjectDeletion(c.workspace).plan(id);withContext(ui){deletionPlan=plan}}
    }
    fun cancelDeletion(){deletionPlan=null;if(dirty)pending=c.scope.launch{delay(400);try{flush()}catch(ex:Exception){c.problem="工程尚未保存，请重试"}}}
    fun deleteProject(selected:Set<String>,confirmed:Boolean,completed:()->Unit={})=c.run("废弃编辑工程") {
        pending?.cancelAndJoin()
        commit.withLock {
            val plan=deletionPlan?:return@withLock
            val result=TypesetProjectDeletion(c.workspace).delete(plan,selected,confirmed)
            val extras=TypesetEditorExtrasStore.load(c.workspace)
            withContext(NonCancellable+ui){if(project?.id==plan.project.id){pendingTextStyle=null;pendingMaskStyle=null;clearCurrent();rememberedProject=null;rememberedProjectLoaded=true};editorExtras=extras;deletionPlan=null;completed();c.operationNotice="工程已废弃；"+(if(extras.retirements.any{it.projectId==plan.project.id&&it.state!="COMPLETE"})"独占进度回收待重试；"else"独占进度已回收；")+"导出文件删除 ${result.count{it.status=="DELETED"}} 个，保留 ${result.count{it.status!="DELETED"}} 个"}
        }
    }
    fun retryRetirement(id:String)=c.run("重试已确认的进度回收") {
        val result=TypesetProjectDeletion(c.workspace).retryRetirement(id,true);val extras=TypesetEditorExtrasStore.load(c.workspace)
        withContext(ui){editorExtras=extras;c.operationNotice=result.detail}
    }
    fun retryOutputDeletion(ids:Set<String>)=c.run("重试已确认的文件删除") {
        val result=TypesetProjectDeletion(c.workspace).retryPending(ids,true);val extras=TypesetEditorExtrasStore.load(c.workspace)
        withContext(ui){editorExtras=extras;c.operationNotice="文件删除 ${result.count{it.status=="DELETED"}} 个，其余保留；请查看删除记录"}
    }
    fun reopenArchived(id:String,completed:()->Unit={})=c.run("重新打开归档工程") {
        drain();val p=c.workspace.state.typesetProjects.single{it.id==id};archives.reopen(p.id,p.revision)
        rememberedProject=id;rememberedProjectLoaded=true;loadProject(id)
        val extras=TypesetEditorExtrasStore.load(c.workspace)
        withContext(ui){editorExtras=extras;completed();c.operationNotice="已继续原工程；页面选择和编辑历史保留"}
    }
    private fun clearCurrent(){project=null;document=null;pageId=null;selectedPages=emptySet();objectId=null;canvasSource=null;dirty=false;exportDirectory=null;exportProblem=null;exportProgress=null;exchangeMessage=null}
    var zoom by mutableStateOf(1f)
    var queueView by mutableStateOf("grid")
    var canvasSource by mutableStateOf<TypesetCanvasSource?>(null)
    var editorExtras by mutableStateOf(TypesetEditorExtras());private set
    fun refreshEditorExtras()=c.scope.launch {
        val source=c.workspace.state.typesetPreferences.sourceExchangeId
        try{val value=withContext(Dispatchers.IO){TypesetEditorExtrasStore.load(c.workspace)}
            if(c.workspace.state.typesetPreferences.sourceExchangeId==source)editorExtras=value
        }catch(cancelled:CancellationException){throw cancelled}catch(ex:Exception){c.problem="编辑预设暂无法读取；原资料保留：${ex.message}"}
    }
    fun saveNamedMask(name:String,replace:String?=null)=c.run("保存命名打码预设") {
        drain();val next=TypesetEditorExtrasStore.load(c.workspace).saveMask(name,MaskToolSettings.from(mask?:maskDefault),replace)
        TypesetEditorExtrasStore.save(c.workspace,next);withContext(ui){editorExtras=next}
    }
    fun renameNamedMask(id:String,name:String)=c.run("重命名打码预设") {
        val next=TypesetEditorExtrasStore.load(c.workspace).renameMask(id,name);TypesetEditorExtrasStore.save(c.workspace,next);withContext(ui){editorExtras=next}
    }
    fun deleteNamedMask(id:String)=c.run("删除命名打码预设") {
        val next=TypesetEditorExtrasStore.load(c.workspace).removeMask(id);TypesetEditorExtrasStore.save(c.workspace,next);withContext(ui){editorExtras=next}
    }
    fun renameTextPreset(id:String,name:String)=c.run("重命名文字预设") {
        require(name.isNotBlank()&&name.length<=100);preferences{p->require(p.textPresets.any{it.id==id});p.copy(textPresets=p.textPresets.map{if(it.id==id)it.copy(name=name)else it})}
    }
    fun setTextPresetDefault(id:String)=c.run("保存文字新建默认") {
        preferences{p->val saved=p.textPresets.single{it.id==id};p.copy(textDefault=saved.text,selectedTextPresetId=id)}
    }
    fun applyNamedMask(id:String,newDefault:Boolean=false)=c.run("应用命名打码预设") {
        drain();val settings=TypesetEditorExtrasStore.load(c.workspace).namedMasks.single{it.id==id}.settings
        val p=project
        if(!newDefault&&p!=null){val next=repository.update(p.id,p.revision){d->d.copy(pages=d.pages.map{page->if(page.id==pageId)page.copy(toolDefaults=settings.apply(TypesetMask()),masks=page.masks.map{if(it.id==objectId)settings.apply(it)else it})else page})}
            val d=repository.load(next);withContext(ui){project=next;document=d;dirty=false}}
        withContext(ui){maskDefault=settings.apply(maskDefault)}
        preferences{if(newDefault)it.copy(currentMask=settings,preferredMaskSlot="custom",batchToolDefault=settings)else it.copy(currentMask=settings)}
    }
    var viewportReset by mutableStateOf(0);private set
    fun fitCanvas(){zoom=1f;viewportReset++}
    var staticChoice by mutableStateOf<StaticEditingChoice?>(null);private set
    fun cancelStaticChoice(){staticChoice=null}
    fun confirmStaticChoice(){
        val choice=staticChoice?:return;staticChoice=null
        if(choice.name!=null)create(choice.name,choice.ids,true,choice.organization,choice.parent)
        else if(project?.id==choice.projectId && project?.revision==choice.revision && !dirty)appendSelected(true,choice.ids)
        else c.problem="工程已改变，请重新选择追加图片"
    }
    var exportDirectory by mutableStateOf<java.nio.file.Path?>(null);private set
    var exportProblem by mutableStateOf<String?>(null);private set
    var exportProgress by mutableStateOf<TypesetExportProgress?>(null);private set
    internal var exportTrace:(TypesetExportProgress)->Unit={}
    internal var copyExportFiles:(List<java.nio.file.Path>)->Unit=ExportClipboard::copy
    internal var previewTrace:(String)->Unit={}
    var exchangeInspection by mutableStateOf<TypesetSourceExchange.Inspection?>(null);private set
    var presetInspection by mutableStateOf<TypesetSourceExchange.PresetInspection?>(null);private set
    var recipeInspection by mutableStateOf<TypesetEditRecipe?>(null);private set
    private var recipeTarget:Pair<String,String>?=null
    internal var chooseRecipeFile:()->java.nio.file.Path?={StudioFileDialogs.open("导入编辑 Recipe",listOf("json"))}
    fun inspectRecipe()=c.run("预读编辑 Recipe") {
        drain();val p=project?:return@run;val page=page?:return@run;val path=withContext(ui){chooseRecipeFile()}?:return@run
        val r=TypesetEditRecipes.inspect(readBounded(path,TypesetEditRecipes.MAX_BYTES));withContext(ui){recipeInspection=r;recipeTarget=p.id to page.id}
    }
    fun cancelRecipe(){recipeInspection=null;recipeTarget=null}
    fun applyRecipe()=c.run("应用编辑 Recipe") {
        drain();val r=recipeInspection?:return@run;val p=project?:return@run;require(recipeTarget==p.id to pageId)
        val next=TypesetEditRecipes.apply(c.workspace,p,pageId!!,r);val d=repository.load(next)
        withContext(ui){project=next;document=d;dirty=false;objectId=null;cancelRecipe();exchangeMessage="编辑Recipe已应用，可撤销；原图和源请求元数据未改动"}
    }
    var exchangeMessage by mutableStateOf<String?>(null);private set
    internal var chooseExchangeFile:(Boolean)->java.nio.file.Path? = {save->
        if(save)StudioFileDialogs.save("保存 TypeSet 工程交换文件（不覆盖已有文件）","Studio.typesetter-workspace.json",listOf("json"))
        else StudioFileDialogs.open("导入 TypeSet 工程交换文件",listOf("json"))
    }
    internal var choosePresetFile:(Boolean)->java.nio.file.Path? = {save->
        if(save)StudioFileDialogs.save("保存 TypeSet 预设文件（不覆盖已有文件）","Studio.typesetter-presets.json",listOf("json"))
        else StudioFileDialogs.open("导入 TypeSet 预设文件",listOf("json"))
    }
    fun inspectPresets()=c.run("预读 TypeSet 预设") {drain();val path=withContext(ui){choosePresetFile(false)}?:return@run
        val inspection=TypesetSourceExchange.inspectPresets(readBounded(path,4*1024*1024));withContext(ui){presetInspection=inspection}}
    fun cancelPresetImport(){presetInspection=null}
    fun importPresets(merge:Boolean)=c.run("导入 TypeSet 预设") {drain();val inspection=presetInspection?:return@run;TypesetSourceExchange.importPresets(c.workspace,inspection,merge);withContext(ui){presetInspection=null;exchangeMessage="预设已导入；已有页面保持原样"}}
    fun exportPresets()=c.run("保存 TypeSet 预设文件") {drain();val bytes=TypesetSourceExchange.exportPresets(c.workspace);val path=withContext(ui){choosePresetFile(true)}?:return@run
        TypesetSourceExchange.saveFile(path,bytes);withContext(ui){exchangeMessage="预设文件已保存：$path · ${bytes.size} bytes · SHA-256 ${sha256(bytes)}"}}
    fun inspectExchange()=c.run("预读 TypeSet 工程") {
        drain();val path=withContext(ui){chooseExchangeFile(false)}?:return@run
        val inspection=TypesetSourceExchange.inspect(readBounded(path,TypesetSourceExchange.MAX_BYTES))
        withContext(ui){exchangeInspection=inspection}
    }
    fun cancelExchange(){exchangeInspection=null}
    fun importExchange()=c.run("导入 TypeSet 工程") {
        drain();val inspection=exchangeInspection?:return@run;val next=TypesetSourceExchange.import(c.workspace,inspection);val d=repository.load(next)
        rememberProject(next.id)
        withContext(ui){project=next;document=d;dirty=false;pageId=next.currentPageId;selectedPages=next.selectedPageIds.toSet();objectId=null;exchangeInspection=null;exchangeMessage="已建立独立工程，原始交换文件与图片已保全";c.view="编辑"}
    }
    fun exportExchange(original:Boolean=false)=c.run("保存 TypeSet 工程文件") {
        drain();val p=project?:return@run;val d=document?:return@run
        val bytes=if(original)TypesetSourceExchange.originalExchange(c.workspace,p)else TypesetSourceExchange.export(c.workspace,p,d)
        val path=withContext(ui){chooseExchangeFile(true)}?:return@run
        TypesetSourceExchange.saveFile(path,bytes);withContext(ui){exchangeMessage="工程文件已保存：$path · ${bytes.size} bytes · SHA-256 ${sha256(bytes)}"}
        recordOutputs(p.id,listOf(TypesetManagedOutput(projectId=p.id,path=path.toAbsolutePath().normalize().toString(),size=bytes.size.toLong(),sha256=sha256(bytes))))
    }
    internal var chooseExportDirectory:()->java.nio.file.Path? = {
        StudioFileDialogs.folder("选择导出位置（将新建独立输出文件夹）")
    }
    fun export(request:TypesetExportRequest,clipboard:Boolean=false)=c.run(if(clipboard)"复制批量输出文件"else"导出编辑工程") {
        drain();val p=project?:return@run;val d=document?:return@run
        val parent=if(clipboard)java.nio.file.Files.createDirectories(c.workspace.root.resolve("exports/clipboard"))else withContext(ui){chooseExportDirectory()}?:return@run
        withContext(ui){exportProblem=null;exportDirectory=null;exportProgress=TypesetExportProgress("PREPARING",0,request.pageIds.size*request.variants.size)}
        val context=currentCoroutineContext()
        try{
            if(d.mode!="storybook"){
                preferences{it.copy(exportFormat=request.format,preserveSourceMetadata=request.metadata=="preserve",exportVariants=request.variants.filterNot{it=="plain"},exportZip=request.zip,exportRecipe=request.includeRecipe)}
                val extras=TypesetEditorExtrasStore.load(c.workspace);val sorted="plain" in request.variants
                if(extras.originalSorted!=sorted){val next=extras.copy(originalSorted=sorted);TypesetEditorExtrasStore.save(c.workspace,next);withContext(ui){editorExtras=next}}
            }
            val output=TypesetExport.create(c.workspace,p,d,parent,request,progress={value->
                context.ensureActive();exportProgress=value;exportTrace(value);context.ensureActive()
            })
            exportProgress=exportProgress?.copy(stage="VERIFYING",currentFile=null)
            val receipt=TypesetExport.verify(output)
            withContext(NonCancellable+ui){exportDirectory=output;exportProgress=exportProgress?.copy(stage="COMPLETE")}
            recordOutputs(p.id,receipt.files.filter{receipt.metadataStatuses[it.name]!="original-byte-identical"}.map{f->TypesetManagedOutput(projectId=p.id,path=output.resolve(f.name).toAbsolutePath().normalize().toString(),size=f.size,sha256=f.sha256,assetId=receipt.assetIds[f.name])})
            if(clipboard){context.ensureActive();copyExportFiles(receipt.files.map{output.resolve(it.name)})}}
        catch(ex:Exception){
            val cancelled=(ex as? CancellationException)?:ex.cause as? CancellationException
            withContext(NonCancellable+ui){
                if(exportDirectory==null){
                    exportProgress=exportProgress?.copy(stage=if(cancelled!=null)"CANCELLED"else"FAILED")
                    exportProblem=if(cancelled!=null)"导出已停止；未完成目录和已生成文件保留，未发布完成批次。可明确重新导出。"else ex.message?:"导出未完成，已有文件保留"
                }else exportProblem=if(clipboard)"输出文件已完成并校验，但复制未完成；文件仍在显示的目录。"else"输出文件已完成并校验，后续操作已停止。"
            }
            if(cancelled!=null)throw cancelled else throw ex
        }
    }
    private suspend fun recordOutputs(projectId:String,outputs:List<TypesetManagedOutput>){
        try{TypesetProjectDeletion(c.workspace).register(projectId,outputs);val value=TypesetEditorExtrasStore.load(c.workspace);withContext(ui){editorExtras=value}}
        catch(ex:Exception){withContext(ui){exportProblem="导出文件已保存，但管理记录未能建立；这些文件不会被工程删除自动处理。"}}
    }
    fun nextCaption() {val d=document?:return;val next=pageId?.let{TypesetBook.next(d,it)}?:return;selectPage(next)}
    var maskDefault by mutableStateOf(c.state.typesetPreferences.resolveMask(c.state.typesetPreferences.preferredMaskSlot).apply(TypesetMask()))
    val page get()=document?.pages?.firstOrNull{it.id==pageId}
    val text get()=page?.texts?.firstOrNull{it.id==objectId}
    val mask get()=page?.masks?.firstOrNull{it.id==objectId}
    fun change(transform:(TypesetDocument)->TypesetDocument) {
        if(c.busy||c.maintenance)return
        val old=document?:return;val next=transform(old)
        try{next.validate()}catch(e:Exception){c.problem="编辑值无效，原内容保留：${e.message}";return}
        if(next==old)return
        document=next;dirty=true;pending?.cancel()
        pending=c.scope.launch{delay(400);try{flush()}catch(e:CancellationException){throw e}catch(e:Exception){c.problem="工程未能保存，当前编辑仍保留；请重试保存或导出工程"}}
    }
    internal suspend fun commitAutoMasks(expectedProject:String,before:TypesetDocument,next:TypesetDocument)=withContext(NonCancellable+ui){commit.withLock {
        val p=project?:error("编辑工程已关闭")
        if(p.id!=expectedProject||document!=before||dirty)throw DataProblem("AI_EDITOR_CHANGED","工程已改变，未应用自动打码结果")
        next.validate();if(next==before)return@withLock
        val updated=withContext(Dispatchers.IO){repository.update(p.id,p.revision){live->if(live!=before)throw DataProblem("AI_SNAPSHOT_CHANGED","已保存工程快照不同，未应用自动打码结果");next}}
        project=updated;document=next;dirty=false
    }}
    fun pageChange(transform:(TypesetPage)->TypesetPage)=change{d->d.copy(pages=d.pages.map{if(it.id==pageId)transform(it)else it})}
    fun textChange(transform:(TypesetText)->TypesetText) {
        val before=text?:return
        pageChange{p->p.copy(texts=p.texts.map{if(it.id==objectId)transform(it)else it})}
        val after=text?:return
        if(TypesetPreferences.styleOnly(before)!=TypesetPreferences.styleOnly(after))pendingTextStyle=TypesetPreferences.styleOnly(after)
    }
    fun updateMaskSettings(transform:(TypesetMask)->TypesetMask) {
        if(c.busy||c.maintenance)return
        val next=transform(mask?:maskDefault)
        try{MaskToolSettings.from(next)}catch(e:Exception){c.problem="打码参数无效，原设置保留";return}
        val before=document
        pageChange{p->p.copy(toolDefaults=next,masks=p.masks.map{if(it.id==objectId)transform(it)else it})}
        if(document!=before){maskDefault=next;pendingMaskStyle=MaskToolSettings.from(next)}
    }
    suspend fun flush()=withContext(NonCancellable) {commit.withLock {
        val p=project?:return@withLock;val snapshot=document?:return@withLock
        val ids=snapshot.pages.map{it.id}.toSet();val current=pageId?.takeIf{it in ids};val selected=selectedPages.filter{it in ids}
        if(dirty || p.currentPageId!=current || p.selectedPageIds!=selected){
            val next=withContext(Dispatchers.IO){val updated=repository.update(p.id,p.revision){snapshot};repository.view(updated.id,updated.revision,current,selected)}
            project=next;dirty=document!=snapshot
        }
        val textStyle=pendingTextStyle;val maskStyle=pendingMaskStyle
        if(textStyle!=null || maskStyle!=null){
            try{withContext(Dispatchers.IO){preferences{it.copy(textDefault=textStyle?:it.textDefault,currentMask=maskStyle?:it.currentMask)}}}
            catch(e:Exception){dirty=true;throw e}
            if(pendingTextStyle==textStyle)pendingTextStyle=null
            if(pendingMaskStyle==maskStyle)pendingMaskStyle=null
        }
        rememberProject(p.id)
    }}
    suspend fun drain(){pending?.cancelAndJoin();flush()}
    fun create(name:String,ids:List<String>,staticConfirmed:Boolean=false,organization:TypesetOrganizationChange?=null,parent:String?=null)=c.run("建立编辑工程") {
        drain()
        if(!staticConfirmed && ids.any{needsStaticEditingConsent(c,it)}){withContext(ui){staticChoice=StaticEditingChoice(name,ids,organization=organization,parent=parent);c.view="编辑";c.operationNotice="等待确认静态编辑；尚未建立工程"};return@run}
        val p=archives.create(name,ids,parent,organization);val d=repository.load(p)
        rememberProject(p.id)
        withContext(ui){project=p;document=d;dirty=false;pageId=d.pages.firstOrNull()?.id;selectedPages=setOfNotNull(pageId);objectId=null;maskDefault=page?.toolDefaults?:c.workspace.state.typesetPreferences.resolveMask(c.workspace.state.typesetPreferences.preferredMaskSlot).apply(TypesetMask());c.view="编辑"}
    }
    fun open(id:String)=c.run("打开编辑工程") {
        drain();loadProject(id)
    }
    private suspend fun loadProject(id:String) {
        require(archives.record(id)?.state.let{it==null||it=="ACTIVE"}){"请从历史工程重新打开；原工程保留"}
        val p=c.workspace.state.typesetProjects.first{it.id==id};val d=repository.load(p)
        val fontProblem=runCatching{TypesetFonts.activate(c.workspace,TypesetFonts.documentFonts(d))}.exceptionOrNull()
        rememberProject(p.id)
        withContext(ui){project=p;document=d;dirty=false;pageId=p.currentPageId?.takeIf{id->d.pages.any{it.id==id}}?:d.pages.firstOrNull()?.id;selectedPages=p.selectedPageIds.filter{id->d.pages.any{it.id==id}}.toSet();objectId=null;maskDefault=page?.toolDefaults?:c.workspace.state.typesetPreferences.resolveMask(c.workspace.state.typesetPreferences.preferredMaskSlot).apply(TypesetMask());c.view="编辑"}
        if(fontProblem!=null)c.problem="字体资源不可用，工程已保留并打开。请重新导入字体或选择可用字体；导出前必须解决。"
    }
    fun manageProject(action:String,name:String="")=c.run("编辑工程管理") {
        drain();val p=project?:return@run
        when(action){
            "rename"->{val next=repository.rename(p.id,p.revision,name);withContext(ui){project=next}}
            "copy"->{val next=repository.copy(p.id,p.revision,name);rememberProject(next.id);withContext(ui){project=next;dirty=false}}
            "remove"->{archives.removeFromList(p.id,p.revision);val extras=TypesetEditorExtrasStore.load(c.workspace);withContext(ui){clearCurrent();editorExtras=extras;rememberedProject=null;rememberedProjectLoaded=true}}
            else->error("Unknown project operation")
        }
    }
    private fun preferences(change:(TypesetPreferences)->TypesetPreferences)=c.workspace.operation {
        val next=change(c.workspace.state.typesetPreferences);next.validate();c.workspace.save(c.workspace.state.copy(typesetPreferences=next))
    }
    fun rememberQueueDefaults()=c.run("保存新工程队列默认") {drain();val d=document?:return@run;preferences{it.copy(sidebarWidth=d.queueWidth,queueThumbSize=d.thumbnailSize)}}
    fun rememberStoryDefault()=c.run("保存新页面小人书默认") {drain();val s=page?.story?:return@run;preferences{it.copy(storyDefault=s)}}
    fun rememberLaterMaskDefault()=c.run("保存连续工作打码默认") {drain();val settings=MaskToolSettings.from(mask?:maskDefault);preferences{it.copy(batchToolDefault=settings)}}
    fun rememberTextDefault()=c.run("保存新建文字默认") {drain();val selected=text?:return@run;preferences{it.copy(textDefault=TypesetPreferences.styleOnly(selected))}}
    fun rememberMaskDefault()=c.run("保存自定义打码参数") {drain();val settings=MaskToolSettings.from(mask?:maskDefault);preferences{it.copy(currentMask=settings)}}
    fun saveTextPreset(name:String,replace:String?=null)=c.run("保存文字预设") {
        drain();val style=text?:return@run;preferences{it.saveText(name,style,replace)}
    }
    fun deleteTextPreset(id:String)=c.run("删除文字预设") {drain();preferences{it.removeText(id)}}
    fun applyTextPreset(id:String)=c.run("应用文字预设") {
        drain();val p=project?:return@run;val selected=objectId?:return@run
        val preset=c.workspace.state.typesetPreferences.textPresets.single{it.id==id}
        TypesetFonts.activate(c.workspace,listOf(preset.text.style.fontFamily))
        val next=repository.update(p.id,p.revision){d->d.copy(pages=d.pages.map{page->if(page.id==pageId)page.copy(texts=page.texts.map{if(it.id==selected)TypesetPreferences.applyText(preset.text,it)else it})else page})}
        val d=repository.load(next);withContext(ui){project=next;document=d;dirty=false}
        preferences{it.copy(selectedTextPresetId=id,textDefault=preset.text)}
    }
    fun maskPreset(action:String,slot:String)=c.run("打码预设") {
        drain();val current=MaskToolSettings.from(mask?:maskDefault)
        when(action){
            "overwrite"->preferences{it.copy(maskOverrides=it.maskOverrides+(slot to current))}
            "reset"->preferences{it.copy(maskOverrides=it.maskOverrides-slot)}
            "prefer"->preferences{it.copy(preferredMaskSlot=slot)}
            "apply"->{
                val settings=c.workspace.state.typesetPreferences.copy(currentMask=current).resolveMask(slot)
                val p=project
                if(p!=null){val next=repository.update(p.id,p.revision){d->d.copy(pages=d.pages.map{page->if(page.id==pageId)page.copy(toolDefaults=settings.apply(TypesetMask()),masks=page.masks.map{if(it.id==objectId)settings.apply(it)else it})else page})}
                    val d=repository.load(next);withContext(ui){project=next;document=d;dirty=false}}
                withContext(ui){maskDefault=settings.apply(maskDefault)};preferences{it.copy(currentMask=settings)}
            }
            else->error("Unknown preset operation")
        }
    }
    fun importFont()=c.run("导入本地字体") {
        drain()
        val file=withContext(ui){StudioFileDialogs.open("导入字体（仅保存在 Studio）",listOf("ttf","otf","woff","woff2"))?.toFile()}?:return@run
        TypesetFonts.import(c.workspace,file.name,readBounded(file.toPath(),64*1024*1024))
    }
    fun history(redo:Boolean=false)=c.run(if(redo)"重做编辑"else"撤销编辑") {
        drain();val p=project?:return@run;val next=repository.history(p.id,p.revision,redo);val d=repository.load(next)
        withContext(ui){project=next;document=d;dirty=false;if(d.pages.none{it.id==pageId})pageId=d.pages.firstOrNull()?.id;objectId=null;selectedPages=selectedPages.intersect(d.pages.map{it.id}.toSet())}
    }
    fun togglePageSelection(id:String) {
        require(document?.pages?.any{it.id==id}==true)
        selectedPages=if(id in selectedPages)selectedPages-id else selectedPages+id
    }
    fun selectPage(id:String,extend:Boolean=false,range:Boolean=false,keepSelection:Boolean=false) {
        val pages=document?.pages.orEmpty();val previous=pageId
        require(pages.any{it.id==id})
        text?.let{pendingTextStyle=TypesetPreferences.styleOnly(it)}
        mask?.let{maskDefault=it}
        selectedPages=when{keepSelection->selectedPages;range && previous!=null->{val a=pages.indexOfFirst{it.id==previous};val b=pages.indexOfFirst{it.id==id};selectedPages+pages.subList(minOf(a,b),maxOf(a,b)+1).map{it.id}}
            extend->if(id in selectedPages)selectedPages-id else selectedPages+id
            else->setOf(id)}
        // Page selection changes the view, never replaces the user's current tool recipe.
        pageId=id;objectId=null
    }
    fun addText(width:Int,height:Int) {
        val template=text?:pendingTextStyle?:c.workspace.state.typesetPreferences.textDefault?:document?.presets?.firstOrNull()?.text
        val next=TypesetPreferences.newText(width,height,template)
        pageChange{it.copy(texts=it.texts+next)};objectId=next.id;tool="select";propertySection="文字"
    }
    fun removeObject()=pageChange{TypesetObjects.remove(it,objectId?:return@pageChange it)}.also{objectId=null}
    fun copyObject(){val id=objectId?:return;val next=newId();pageChange{TypesetObjects.copy(it,id,next)};objectId=next}
    fun moveObject(direction:Int)=pageChange{TypesetObjects.move(it,objectId?:return@pageChange it,direction)}
    fun lockObject(locked:Boolean)=pageChange{TypesetObjects.lock(it,objectId?:return@pageChange it,locked)}
    fun movePages(direction:Int)=change{d->
        val list=d.pages.toMutableList();val indexes=if(direction<0)list.indices else list.indices.reversed()
        for(i in indexes){val j=i+direction;if(j in list.indices && list[i].id in selectedPages && list[j].id !in selectedPages){val tmp=list[i];list[i]=list[j];list[j]=tmp}}
        d.copy(pages=list)
    }
    fun movePagesTo(anchor:String?,after:Boolean)=change{TypesetQueue.move(it,selectedPages,anchor,after)}
    fun movePagesByNumber(number:Double,after:Boolean,ids:Set<String> = selectedPages)=change{TypesetQueue.moveByNumber(it,ids,number,after)}
    fun appendSelected(staticConfirmed:Boolean=false,confirmedIds:List<String>?=null)=c.run("追加图库图片") {
        drain();val p=project?:return@run
        val ids=confirmedIds?:c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()}
        if(!staticConfirmed && ids.any{needsStaticEditingConsent(c,it)}){withContext(ui){staticChoice=StaticEditingChoice(null,ids,p.id,p.revision);c.operationNotice="等待确认静态编辑；尚未追加图片"};return@run}
        val added=ids.map{id->val asset=c.workspace.assets.read(java.util.UUID.fromString(id)).require();TypesetPage(assetId=id,name=asset.request().name(),coordinates=TypesetImage.DISPLAY,story=c.workspace.state.typesetPreferences.storyDefault?:StoryStyle(),toolDefaults=c.workspace.state.typesetPreferences.batchToolDefault?.apply(TypesetMask()))}
        val next=repository.update(p.id,p.revision){it.copy(pages=it.pages+added)};val d=repository.load(next)
        withContext(ui){project=next;document=d;dirty=false;pageId=added.firstOrNull()?.id?:pageId;selectedPages=added.map{it.id}.toSet();objectId=null}
    }
    fun removePages() {change{it.copy(pages=it.pages.filterNot{it.id in selectedPages})};pageId=document?.pages?.firstOrNull()?.id;selectedPages=emptySet();objectId=null}
    fun applyStyleToSelection(previous:Boolean=false,kind:String="both")=c.run("应用批次样式") {
        drain();val p=project?:return@run;val snapshot=document?:return@run
        val selected=selectedPages.toSet();val textStyle=text?:snapshot.presets.firstOrNull()?.text
        val widths=mutableMapOf<String,Int>()
        val updated=TypesetBatch.apply(snapshot,selected,previous,textStyle,mask?:maskDefault,kind){page->
            widths.getOrPut(page.assetId+page.coordinates){val image=TypesetImage.decode(c.workspace.bytes(page.assetId),page.coordinates);try{image.width}finally{image.flush()}}
        }
        if(updated!=snapshot){val next=repository.update(p.id,p.revision){updated}
            withContext(ui){project=next;document=updated;dirty=false;page?.toolDefaults?.let{maskDefault=it}}
        }
    }
}
