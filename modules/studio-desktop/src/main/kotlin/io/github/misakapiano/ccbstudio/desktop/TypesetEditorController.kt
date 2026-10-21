package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.runtime.*
import com.example.chatbar.desktop.StudioFileDialogs
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor

/** UI draft is separate from the committed immutable snapshot. A failed save keeps the draft visible. */
internal data class StaticEditingChoice(val name:String?,val ids:List<String>,val projectId:String?=null,val revision:Long?=null)
internal class TypesetEditorController(private val c:WorkbenchController) {
    private val ui=c.scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    private val repository=TypesetRepository(c.workspace)
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
    var zoom by mutableStateOf(1f)
    var staticChoice by mutableStateOf<StaticEditingChoice?>(null);private set
    fun cancelStaticChoice(){staticChoice=null}
    fun confirmStaticChoice(){
        val choice=staticChoice?:return;staticChoice=null
        if(choice.name!=null)create(choice.name,choice.ids,true)
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
        withContext(ui){project=next;document=d;dirty=false;pageId=next.currentPageId;selectedPages=next.selectedPageIds.toSet();objectId=null;exchangeInspection=null;exchangeMessage="已建立独立工程，原始交换文件与图片已保全";c.view="编辑"}
    }
    fun exportExchange(original:Boolean=false)=c.run("保存 TypeSet 工程文件") {
        drain();val p=project?:return@run;val d=document?:return@run
        val bytes=if(original)TypesetSourceExchange.originalExchange(c.workspace,p)else TypesetSourceExchange.export(c.workspace,p,d)
        val path=withContext(ui){chooseExchangeFile(true)}?:return@run
        TypesetSourceExchange.saveFile(path,bytes);withContext(ui){exchangeMessage="工程文件已保存：$path · ${bytes.size} bytes · SHA-256 ${sha256(bytes)}"}
    }
    internal var chooseExportDirectory:()->java.nio.file.Path? = {
        StudioFileDialogs.folder("选择导出位置（将新建独立输出文件夹）")
    }
    fun export(request:TypesetExportRequest,clipboard:Boolean=false)=c.run(if(clipboard)"复制批量输出文件"else"导出编辑工程") {
        drain();val p=project?:return@run;val d=document?:return@run
        val parent=if(clipboard)java.nio.file.Files.createDirectories(c.workspace.root.resolve("exports/clipboard"))else withContext(ui){chooseExportDirectory()}?:return@run
        withContext(ui){exportProblem=null;exportDirectory=null;exportProgress=TypesetExportProgress("PREPARING",0,request.pageIds.size*request.variants.size)}
        if(d.mode!="storybook")preferences{it.copy(exportFormat=request.format,preserveSourceMetadata=request.metadata=="preserve",exportVariants=request.variants,exportZip=request.zip,exportRecipe=request.includeRecipe)}
        val context=currentCoroutineContext()
        try{val output=TypesetExport.create(c.workspace,p,d,parent,request,progress={value->
                context.ensureActive();exportProgress=value;exportTrace(value);context.ensureActive()
            })
            exportProgress=exportProgress?.copy(stage="VERIFYING",currentFile=null)
            val receipt=TypesetExport.verify(output)
            withContext(NonCancellable+ui){exportDirectory=output;exportProgress=exportProgress?.copy(stage="COMPLETE")}
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
    fun nextCaption() {val d=document?:return;val next=pageId?.let{TypesetBook.next(d,it)}?:return;selectPage(next)}
    var maskDefault by mutableStateOf(c.workspace.state.typesetPreferences.resolveMask(c.workspace.state.typesetPreferences.preferredMaskSlot).apply(TypesetMask()))
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
    fun pageChange(transform:(TypesetPage)->TypesetPage)=change{d->d.copy(pages=d.pages.map{if(it.id==pageId)transform(it)else it})}
    fun textChange(transform:(TypesetText)->TypesetText) {
        val before=text?:return
        pageChange{p->p.copy(texts=p.texts.map{if(it.id==objectId)transform(it)else it})}
        val after=text?:return
        if(TypesetPreferences.styleOnly(before)!=TypesetPreferences.styleOnly(after))pendingTextStyle=TypesetPreferences.styleOnly(after)
    }
    fun updateMaskSettings(transform:(TypesetMask)->TypesetMask) {
        if(c.busy||c.maintenance)return
        val next=transform(maskDefault)
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
    }}
    suspend fun drain(){pending?.cancelAndJoin();flush()}
    fun create(name:String,ids:List<String>,staticConfirmed:Boolean=false)=c.run("建立编辑工程") {
        drain()
        if(!staticConfirmed && ids.any{needsStaticEditingConsent(c,it)}){withContext(ui){staticChoice=StaticEditingChoice(name,ids);c.view="编辑";c.operationNotice="等待确认静态编辑；尚未建立工程"};return@run}
        val p=repository.create(name,ids);val d=repository.load(p)
        withContext(ui){project=p;document=d;dirty=false;pageId=d.pages.firstOrNull()?.id;selectedPages=setOfNotNull(pageId);objectId=null;maskDefault=page?.toolDefaults?:c.workspace.state.typesetPreferences.resolveMask(c.workspace.state.typesetPreferences.preferredMaskSlot).apply(TypesetMask());c.view="编辑"}
    }
    fun open(id:String)=c.run("打开编辑工程") {
        drain();val p=c.workspace.state.typesetProjects.first{it.id==id};val d=repository.load(p)
        val fontProblem=runCatching{TypesetFonts.activate(c.workspace,TypesetFonts.documentFonts(d))}.exceptionOrNull()
        withContext(ui){project=p;document=d;dirty=false;pageId=p.currentPageId?.takeIf{id->d.pages.any{it.id==id}}?:d.pages.firstOrNull()?.id;selectedPages=p.selectedPageIds.filter{id->d.pages.any{it.id==id}}.toSet();objectId=null;maskDefault=page?.toolDefaults?:c.workspace.state.typesetPreferences.resolveMask(c.workspace.state.typesetPreferences.preferredMaskSlot).apply(TypesetMask());c.view="编辑"}
        if(fontProblem!=null)c.problem="字体资源不可用，工程已保留并打开。请重新导入字体或选择可用字体；导出前必须解决。"
    }
    fun manageProject(action:String,name:String="")=c.run("编辑工程管理") {
        drain();val p=project?:return@run
        when(action){
            "rename"->{val next=repository.rename(p.id,p.revision,name);withContext(ui){project=next}}
            "copy"->{val next=repository.copy(p.id,p.revision,name);withContext(ui){project=next;dirty=false}}
            "remove"->{repository.remove(p.id,p.revision);withContext(ui){project=null;document=null;pageId=null;selectedPages=emptySet();objectId=null;dirty=false}}
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
    fun selectPage(id:String,extend:Boolean=false,range:Boolean=false) {
        val pages=document?.pages.orEmpty();val previous=pageId
        selectedPages=when{range && previous!=null->{val a=pages.indexOfFirst{it.id==previous};val b=pages.indexOfFirst{it.id==id};selectedPages+pages.subList(minOf(a,b),maxOf(a,b)+1).map{it.id}}
            extend->if(id in selectedPages)selectedPages-id else selectedPages+id
            else->setOf(id)}
        pageId=id;objectId=null;maskDefault=page?.toolDefaults?:c.workspace.state.typesetPreferences.batchToolDefault?.apply(maskDefault)?:maskDefault
    }
    fun addText(width:Int,height:Int) {
        val template=text?:pendingTextStyle?:c.workspace.state.typesetPreferences.textDefault?:document?.presets?.firstOrNull()?.text
        val next=TypesetPreferences.newText(width,height,template)
        pageChange{it.copy(texts=it.texts+next)};objectId=next.id;tool="select"
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
    fun applyStyleToSelection(previous:Boolean=false)=c.run("应用批次样式") {
        drain();val p=project?:return@run;val snapshot=document?:return@run
        val selected=selectedPages.toSet();val textStyle=text?:snapshot.presets.firstOrNull()?.text
        val widths=mutableMapOf<String,Int>()
        val updated=TypesetBatch.apply(snapshot,selected,previous,textStyle,mask?:maskDefault){page->
            widths.getOrPut(page.assetId+page.coordinates){val image=TypesetImage.decode(c.workspace.bytes(page.assetId),page.coordinates);try{image.width}finally{image.flush()}}
        }
        if(updated!=snapshot){val next=repository.update(p.id,p.revision){updated}
            withContext(ui){project=next;document=updated;dirty=false;page?.toolDefaults?.let{maskDefault=it}}
        }
    }
}
