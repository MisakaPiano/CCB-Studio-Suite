package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

internal class LibraryController(private val c:WorkbenchController) {
    val archiveView=GalleryViewState("archive","回收站")
    var archiveSelection by mutableStateOf<Set<String>>(emptySet())
    private data class Edit(val label:String,val undo:()->Unit,val redo:()->Unit)
    private var undoEdits by mutableStateOf<List<Edit>>(emptyList())
    private var redoEdits by mutableStateOf<List<Edit>>(emptyList())
    val canUndo get()=undoEdits.isNotEmpty() && !c.busy
    val canRedo get()=redoEdits.isNotEmpty() && !c.busy
    private fun rememberEdit(edit:Edit){undoEdits=(undoEdits+edit).takeLast(100);redoEdits=emptyList()}
    fun undo(){val edit=undoEdits.lastOrNull()?:return;c.run("撤销 ${edit.label}"){edit.undo();refresh();undoEdits=undoEdits.dropLast(1);redoEdits=redoEdits+edit}}
    fun redo(){val edit=redoEdits.lastOrNull()?:return;c.run("重做 ${edit.label}"){edit.redo();refresh();redoEdits=redoEdits.dropLast(1);undoEdits=undoEdits+edit}}
    var tab by mutableStateOf("目录入口")
    val repository=LibraryRepository(c.workspace)
    var state by mutableStateOf<LibraryState?>(null);private set
    var error by mutableStateOf<String?>(null);private set
    var prompts by mutableStateOf(listOf<io.github.misakapiano.ccbstudio.assets.AssetStore.Asset>());private set
    var archived by mutableStateOf(setOf<String>());private set
    var archiveError by mutableStateOf<String?>(null);private set
    var archiveRequest by mutableStateOf<Set<String>?>(null)
    val archive=AssetArchive(c.workspace)
    var scanResults by mutableStateOf(listOf<FolderScanResult>());private set
    var automaticNotice by mutableStateOf("")
    var collectionBlocked by mutableStateOf(false);private set
    var monitorEpoch by mutableStateOf(0);private set
    var sessionPaused by mutableStateOf(c.pauseCollectionsOnStart);private set
    var collectionBusy by mutableStateOf(false);private set
    var chooseCollectionDirectory:()->Path?={StudioFileDialogs.folder("选择自动收集的文件夹")}
    fun retryCollections(){repository.load();c.workspace.catalog();collectionBlocked=false;sessionPaused=false;monitorEpoch++}
    private var collectionTask:Job?=null
    suspend fun cancelAndDrain(){collectionTask?.cancelAndJoin()}
    fun stopCollection(){sessionPaused=true;collectionTask?.cancel();automaticNotice="正在停止目录收集，等待当前文件安全写入；已入库原件保留"}
    fun pauseCollections(paused:Boolean) {
        if(paused)stopCollection()
        c.run("设置目录收集"){cancelAndDrain();repository.pauseCollections(paused);sessionPaused=paused;refresh()}
    }
    fun synchronize(ids:Set<String>,scope:CoroutineScope,automatic:Boolean=true):Boolean {
        if(c.busy || c.maintenance || collectionBlocked || collectionBusy || collectionTask?.isCompleted==false || (automatic && sessionPaused))return false
        collectionBusy=true;automaticNotice="正在检查目录文件…"
        collectionTask=scope.launch(start=CoroutineStart.UNDISPATCHED) {
            val events=Channel<FolderScanProgress>(Channel.CONFLATED)
            val display=launch{for(p in events)automaticNotice="目录收集 ${p.completed} / ${p.total} 项 · 可切换页面，已入库原件保留"}
            try {
                val results=withContext(Dispatchers.IO){val context=currentCoroutineContext();ids.flatMap{id->context.ensureActive()
                    if((if(automatic)repository.automaticFolders()else repository.load().folders).any{it.id==id})
                        repository.scan(id,{context.ensureActive()},{events.trySend(it);Unit}) else emptyList()
                }}
                events.close();display.join();scanResults=results
                automaticNotice=if(results.any{it.status !in setOf("IMPORTED","NEW_VERSION","UNCHANGED")})"部分目录项目未导入，详见逐项回执；已保存资产保留"else"目录已同步 · 新增或更新 ${results.count{it.status=="IMPORTED" || it.status=="NEW_VERSION"}} 项"
            }catch(e:CancellationException){automaticNotice="目录收集已停止；已入库原件保留，可检查后恢复收集";throw e}
            catch(e:Exception){collectionBlocked=true;automaticNotice="目录自动同步已停止：${e.javaClass.simpleName}；原件保留，请检查入口后重试"}
            finally {
                withContext(NonCancellable) {
                    events.close();display.cancelAndJoin()
                    try {withContext(Dispatchers.IO){refresh();runCatching{c.refreshLibraryAssets()}.onFailure{collectionBlocked=true}}}
                    finally {collectionBusy=false}
                }
            }
        };return true
    }
    init{refresh()}
    fun refresh(){runCatching{repository.load() to repository.prompts()}.onSuccess{(loaded,all)->
        state=loaded;error=null
        runCatching{archive.ids(loaded)}.onSuccess{archived=it;archiveError=null}.onFailure{archived=emptySet();archiveError="回收站记录不可读取，暂停移入与恢复；原件仍可查看：${it.javaClass.simpleName}"}
        prompts=all.filterNot{it.request().id().toString() in archived}
    }.onFailure{error="资产管理记录读取失败，原件保留，管理写入已停止：${it.javaClass.simpleName}";state=null;archiveError=error;archived=emptySet();prompts=emptyList()}}
    fun changeArchive(ids:Set<String>,archived:Boolean)=c.run(if(archived)"移入资产回收站"else"恢复资产"){
        val changed=if(archived)ids-archive.ids()else ids.intersect(archive.ids())
        if(changed.isNotEmpty())archive.change(changed,archived);refresh()
        if(changed.isNotEmpty()) {
            fun change(expected:Boolean,to:Boolean){val current=archive.ids();require(changed.all{(it in current)==expected}){"回收站状态已改变，请刷新后重试"};archive.change(changed,to)}
            rememberEdit(Edit(if(archived)"移入回收站"else"恢复资产",{change(archived,!archived)},{change(!archived,archived)}))
        }
        if(archived){c.selection=c.selection-ids;if(c.current in ids)c.current=c.assets.firstOrNull{it.request().id().toString() !in this.archived}?.request()?.id()?.toString()}
        else archiveSelection=archiveSelection-ids
    }
    fun changeAlbums(onSaved:()->Unit={},transform:(List<VirtualAlbum>)->List<VirtualAlbum>)=c.run("调整虚拟相册") {
        val before=repository.load().virtualAlbums;val after=transform(before);VirtualAlbums.validate(after)
        fun write(expected:List<VirtualAlbum>,next:List<VirtualAlbum>){repository.update{require(it.virtualAlbums==expected){"相册已被其他操作修改"};it.copy(virtualAlbums=next)}}
        write(before,after);refresh()
        if(before!=after)rememberEdit(Edit("虚拟相册",{write(after,before)},{write(before,after)}));onSaved()
    }
    fun changeOrganization(onSaved:()->Unit={},transform:(GenerationOrganization)->GenerationOrganization)=c.run("调整项目与批次组织") {
        val repo=GenerationOrganizations(c.workspace);val before=repo.state;val after=transform(before);repo.change(before,after)
        if(before!=after)rememberEdit(Edit("项目与批次组织",{repo.change(after,before)},{repo.change(before,after)}))
        onSaved()
    }
    fun labels(id:String)=state?.labels?.get(id)?:AssetLabels()
    fun saveLabels(id:String,alias:String,tags:String)=changeLabels(setOf(id)){AssetLabels(alias.trim(),tags.split(',', '，').map{it.trim()}.filter{it.isNotEmpty()}.distinct())}
    fun changeLabels(ids:Set<String>,onSaved:()->Unit={},transform:(AssetLabels)->AssetLabels)=c.run("保存资产标签") {
        val old=repository.load().labels
        val before=ids.associateWith{old[it]};val after=ids.associateWith{transform(old[it]?:AssetLabels())}
        repository.changeLabels(before,after);refresh()
        if(before!=after)rememberEdit(Edit("资产标签",{repository.changeLabels(after,before)},{repository.changeLabels(before,after)}))
        onSaved()
    }
    fun savePrompt(text:String,name:String,previous:String?=null)=c.run("保存独立 Prompt 资产"){repository.savePrompt(text,name,"manual",previous);refresh()}
    fun savePromptReference(text:String,name:String,previous:String?=null)=c.run("保存独立 Prompt 参照"){
        val board=PromptReferences(repository)
        val existing=previous?.takeIf{repository.prompt(it).let{p->p.text==text && p.sections.isEmpty() && p.image==null}}
        require(existing in board.load().ids || board.load().ids.size<16){"参照板最多保留 16 个来源，请先移除一项"}
        val id=existing?:repository.savePrompt(text,name,"manual",previous)
        board.add(id);refresh();tab="多 Prompt 参照"
    }
    fun scan(id:String){synchronize(setOf(id),c.scope,automatic=false)}
    fun registerCollection(path:Path,recursive:Boolean,category:String="images")=c.run("添加自动收集文件夹") {
        if(repository.load().folders.any{it.category==category && Path.of(it.path)==path.toAbsolutePath().normalize()}) {c.problem="这个文件夹已经在自动收集列表中。";return@run}
        repository.addFolder(path,category,recursive);refresh();monitorEpoch++
    }
    fun setFolderPaused(id:String,paused:Boolean) {
        collectionTask?.cancel()
        c.run(if(paused)"暂停文件夹收集"else"继续文件夹收集") {
            cancelAndDrain();repository.pauseFolder(id,paused);refresh();monitorEpoch++
            automaticNotice=if(paused)"此文件夹已暂停，图库图片保留"else"此文件夹已开启自动收集"
        }
    }
    fun removeCollection(id:String) {
        collectionTask?.cancel()
        c.run("停止文件夹收集") {cancelAndDrain();repository.removeFolder(id);refresh();monitorEpoch++;automaticNotice="已停止收集，图库图片与源文件保留"}
    }
    fun defaults()=c.run("启用资产收集目录"){repository.defaultFolders();refresh()}
    fun addFolder(category:String,recursive:Boolean){
        StudioFileDialogs.folder("添加只读资产目录入口")?.let{path->c.run("添加资产目录入口"){repository.addFolder(path,category,recursive);refresh()}}
    }
    fun importPrompt(){
        StudioFileDialogs.open("导入独立 Prompt 文件",listOf("txt","md","prompt"))?.let{path->c.run("导入 Prompt 资产"){
            val text=readBounded(path,1024*1024).decodeToString(throwOnInvalidSequence=true)
            repository.savePrompt(text,path.fileName.toString(),"file:"+path.toAbsolutePath());refresh()
        }}
    }
}

@Composable internal fun AssetLabelEditor(c:WorkbenchController,id:String) {
    c.library.error?.let{Text(it,color=MaterialTheme.colors.error);return}
    val label=c.library.labels(id);var alias by remember(id,label){mutableStateOf(label.alias)};var tags by remember(id,label){mutableStateOf(label.tags)}
    val index=remember(c.assets,c.library.state){LibraryTagIndex(c.assets.map{it.request().id().toString()}.filter{it !in c.library.archived},c.library.state?.labels.orEmpty())}
    StudioDisclosure("别名与标签") {
        OutlinedTextField(alias,{alias=it.take(200)},label={Text("资产别名")},modifier=Modifier.fillMaxWidth())
        LibraryTagChips(tags,index.counts,{tags=tags-it},Modifier.fillMaxWidth())
        LibraryTagInput(index,tags,{tags=(tags+it).distinct()},Modifier.fillMaxWidth(),allowNew=true)
        StudioAction("保存资产分类",!c.busy && c.library.state!=null && tags.size<=32){c.library.changeLabels(setOf(id)){AssetLabels(alias.trim(),tags)}}
    }
}

@Composable internal fun LibraryWorkspace(c:WorkbenchController) {
    val library=c.library
    var tab by library::tab
    if(tab=="回收站"){LibraryArchiveWorkspace(c);return}
    var selected by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var text by remember{mutableStateOf("")}
    var query by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        StudioChips("资产管理",listOf("自动收集","Prompt 资产","多 Prompt 参照","回收站","备份与恢复"),if(tab=="目录入口")"自动收集"else tab,{it}){tab=if(it=="自动收集")"目录入口"else it}
        if(tab=="备份与恢复"){LibraryBackupWorkspace(c);return@Column}
        library.error?.let{Text(it,color=MaterialTheme.colors.error);StudioAction("重新读取管理记录"){library.refresh()};return@Column}
        if(tab=="Prompt 资产") {
            Text("Prompt 独立于图片保存；编辑保存为新版本，原版本保留。")
            StudioActions {
                StudioAction("新建 Prompt 资产"){selected=null;name="";text=""}
                StudioAction("导入 Prompt 文件",!c.busy){library.importPrompt()}
                StudioAction("粘贴 Prompt 文本",!c.busy){runCatching{java.awt.Toolkit.getDefaultToolkit().systemClipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String}.onSuccess{it?.let{value->text=value;selected=null}}.onFailure{c.problem="剪贴板文字不可读取"}}
                StudioAction("从当前创作取词"){selected=null;text=c.draft.copyPositivePrompt()}
            }
            OutlinedTextField(query,{query=it},label={Text("查找 Prompt 别名或标签")},modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).heightIn(max=240.dp).verticalScroll(rememberScrollState())) {
                    library.prompts.asReversed().filter{a->val l=library.labels(a.request().id().toString());query.isBlank() || l.alias.contains(query,true) || l.tags.any{it.contains(query,true)}}.forEach { a->
                        val id=a.request().id().toString();val l=library.labels(id)
                        StudioAction(l.alias.ifBlank{"Prompt ${id.take(8)}"},selected=id==selected){val prompt=library.repository.prompt(id);selected=id;name=l.alias;text=prompt.text}
                    }
                }
            }
            OutlinedTextField(name,{name=it.take(200)},label={Text("Prompt 名称")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(text,{text=it},label={Text("独立 Prompt 正文")},modifier=Modifier.fillMaxWidth().heightIn(min=160.dp,max=320.dp))
            StudioActions {
                StudioAction("保存 Prompt 新版本",!c.busy && text.isNotBlank()){library.savePrompt(text,name,selected)}
                StudioAction("保存并加入多 Prompt 参照",!c.busy && text.isNotBlank()){library.savePromptReference(text,name,selected)}
                StudioAction("应用到创作（可撤销）",!c.busy && text.isNotBlank()){c.pastePrompt(text);c.view="创作"}
                StudioAction("复制 Prompt",text.isNotEmpty()){desktopCopyText(text)}
            }
            selected?.let{id->AssetLabelEditor(c,id);StudioAction("加入多 Prompt 参照",!c.busy){c.run("加入参照板"){PromptReferences(library.repository).add(id);tab="多 Prompt 参照";library.refresh()}}
                StudioAction("Prompt 移入回收站",!c.busy&&library.archiveError==null&&id !in library.archived){library.archiveRequest=setOf(id)}
            }
        } else if(tab=="多 Prompt 参照") {PromptReferenceBoard(c)} else if(tab=="回收站") {AssetArchiveContents(c)} else {
            CollectionWorkspace(c)
        }
        if(tab!="目录入口" && tab!="自动收集")StudioAction("查看全部图片资产"){c.view="图库"}
    }
}

@Composable internal fun AssetArchiveContents(c:WorkbenchController) {
    val library=c.library
    Text("移入回收站的资产不再出现在新选用列表。已有工程、参照板、生成记录与原始字节继续保留；外部收集目录文件不受影响。恢复后可继续选用。")
    library.archiveError?.let{Text(it,color=MaterialTheme.colors.error);StudioAction("重读回收站"){library.refresh()};return}
    val contents by produceState<Result<List<io.github.misakapiano.ccbstudio.assets.AssetStore.Asset>>?>(null,library.state){value=withContext(Dispatchers.IO){runCatching{library.archive.manageable().filter{it.request().id().toString() in library.archived}}}}
    if(contents==null){Text("正在读取回收站…");return}
    contents!!.exceptionOrNull()?.let{Text("资产目录不可读取，未删除任何文件",color=MaterialTheme.colors.error);return}
    val assets=contents!!.getOrThrow()
    Text("${assets.size} 项资产 · 此处不执行永久删除")
    assets.asReversed().forEach{asset->
        val id=asset.request().id().toString();val label=library.labels(id)
        Text(label.alias.ifBlank{asset.request().name()})
        Text("${label.tags.joinToString(" · ")} · ${asset.size()} bytes · ${id.take(8)}")
        StudioAction("恢复资产 ${id.take(8)}",!c.busy){library.changeArchive(setOf(id),false)}
    }
}

@Composable internal fun AssetArchiveConfirmation(c:WorkbenchController) {
    val ids=c.library.archiveRequest?:return
    AlertDialog(onDismissRequest={c.library.archiveRequest=null},title={Text("移入资产回收站？")},text={
        Text("将 ${ids.size} 项资产从图库、Prompt 列表和新选用入口移出。已有工程与参照仍可读取，原件和外部目录文件保留。可在资产管理 → 回收站恢复。")
    },confirmButton={TextButton(enabled=!c.busy&&c.library.error==null&&c.library.archiveError==null,onClick={c.library.archiveRequest=null;c.library.changeArchive(ids,true)}){Text("确认移入回收站")}},dismissButton={TextButton(onClick={c.library.archiveRequest=null}){Text("取消")}})
}

@Composable internal fun LibrarySynchronization(c:WorkbenchController) {
    val library=c.library;val scope=rememberCoroutineScope()
    val eligibility=remember(library.state,library.monitorEpoch,c.maintenance,library.sessionPaused){runCatching{if(c.maintenance || library.sessionPaused)emptyList()else library.state?.let{library.repository.automaticFolders(it)}.orEmpty()}}
    val folders=eligibility.getOrDefault(emptyList())
    LaunchedEffect(eligibility){if(eligibility.isFailure)library.automaticNotice="本机目录登记不可读取，自动收集已停止；已保存资产和分类保留，请检查目录登记。"}
    LaunchedEffect(c,folders,library.monitorEpoch) {
        if(folders.isEmpty())return@LaunchedEffect
        var monitor:LibraryDirectoryMonitor?=null
        try {
            withContext(Dispatchers.IO){monitor=LibraryDirectoryMonitor(folders)}
            requireNotNull(monitor).let{watcher->
                val pending=mutableSetOf<String>();var changedAt=0L
                while(isActive) {
                    val changed=withContext(Dispatchers.IO){watcher.changes()}
                    if(changed.isNotEmpty()){pending+=changed;changedAt=System.nanoTime()}
                    if(pending.isNotEmpty() && System.nanoTime()-changedAt>=750_000_000L && library.synchronize(pending.toSet(),scope))pending.clear()
                }
            }
        }catch(e:CancellationException){throw e}catch(e:Exception){library.automaticNotice="目录监听不可用：${e.javaClass.simpleName}；可手动同步，未删除资产"}finally{monitor?.close()}
    }
}
