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
import javax.swing.JFileChooser
import kotlinx.coroutines.*

internal class LibraryController(private val c:WorkbenchController) {
    var tab by mutableStateOf("Prompt 资产")
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
    fun retryCollections(){repository.load();c.workspace.catalog();collectionBlocked=false;monitorEpoch++}
    private var collectionTask:Job?=null
    suspend fun cancelAndDrain(){collectionTask?.cancelAndJoin()}
    fun synchronize(ids:Set<String>,scope:CoroutineScope):Boolean {
        if(c.busy || c.maintenance || collectionBlocked || collectionTask?.isActive==true)return false
        collectionTask=scope.launch {
            try {
                val results=withContext(Dispatchers.IO){ids.flatMap{id->ensureActive();c.workspace.operation {
                    if(repository.automaticFolders().any{it.id==id})repository.scan(id)else emptyList()
                }}}
                scanResults=results;refresh();c.refreshLibraryAssets()
                automaticNotice=if(results.any{it.status !in setOf("IMPORTED","NEW_VERSION","UNCHANGED")})"部分目录项目未导入，详见逐项回执；已保存资产保留"else"目录已同步 · 新增或更新 ${results.count{it.status=="IMPORTED" || it.status=="NEW_VERSION"}} 项"
            }catch(e:CancellationException){throw e}catch(e:Exception){collectionBlocked=true;automaticNotice="目录自动同步已停止：${e.javaClass.simpleName}；原件保留，请检查入口后重试"}
        };return true
    }
    init{refresh()}
    fun refresh(){runCatching{repository.load() to repository.prompts()}.onSuccess{(loaded,all)->
        state=loaded;error=null
        runCatching{archive.ids(loaded)}.onSuccess{archived=it;archiveError=null}.onFailure{archived=emptySet();archiveError="回收站记录不可读取，暂停移入与恢复；原件仍可查看：${it.javaClass.simpleName}"}
        prompts=all.filterNot{it.request().id().toString() in archived}
    }.onFailure{error="资产管理记录读取失败，原件保留，管理写入已停止：${it.javaClass.simpleName}";state=null;archiveError=error;archived=emptySet();prompts=emptyList()}}
    fun changeArchive(ids:Set<String>,archived:Boolean)=c.run(if(archived)"移入资产回收站"else"恢复资产"){
        archive.change(ids,archived);refresh()
        if(archived)c.selection=c.selection-ids
    }
    fun labels(id:String)=state?.labels?.get(id)?:AssetLabels()
    fun saveLabels(id:String,alias:String,tags:String)=c.run("保存资产别名与标签"){repository.label(id,alias,tags.split(',', '，'));refresh()}
    fun savePrompt(text:String,name:String,previous:String?=null)=c.run("保存独立 Prompt 资产"){repository.savePrompt(text,name,"manual",previous);refresh()}
    fun scan(id:String)=c.run("同步资产目录"){scanResults=repository.scan(id);refresh()}
    fun defaults()=c.run("启用资产收集目录"){repository.defaultFolders();refresh()}
    fun addFolder(category:String,recursive:Boolean){
        val chooser=JFileChooser();chooser.dialogTitle="添加只读资产目录入口";chooser.fileSelectionMode=JFileChooser.DIRECTORIES_ONLY
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)c.run("添加资产目录入口"){repository.addFolder(chooser.selectedFile.toPath(),category,recursive);refresh()}
    }
    fun importPrompt(){
        val chooser=JFileChooser();chooser.dialogTitle="导入独立 Prompt 文件";chooser.fileFilter=javax.swing.filechooser.FileNameExtensionFilter("Prompt 文本","txt","md","prompt")
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)c.run("导入 Prompt 资产"){
            val path=chooser.selectedFile.toPath();val text=readBounded(path,1024*1024).decodeToString(throwOnInvalidSequence=true)
            repository.savePrompt(text,path.fileName.toString(),"file:"+path.toAbsolutePath());refresh()
        }
    }
}

@Composable internal fun AssetLabelEditor(c:WorkbenchController,id:String) {
    c.library.error?.let{Text(it,color=MaterialTheme.colors.error);return}
    val label=c.library.labels(id);var alias by remember(id,label){mutableStateOf(label.alias)};var tags by remember(id,label){mutableStateOf(label.tags.joinToString(", "))}
    StudioDisclosure("别名与标签") {
        OutlinedTextField(alias,{alias=it.take(200)},label={Text("资产别名")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(tags,{tags=it},label={Text("标签（逗号分隔，可多个）")},modifier=Modifier.fillMaxWidth())
        StudioAction("保存资产分类",!c.busy && c.library.state!=null){c.library.saveLabels(id,alias,tags)}
    }
}

@Composable internal fun LibraryWorkspace(c:WorkbenchController) {
    val library=c.library
    var tab by library::tab
    var selected by remember{mutableStateOf<String?>(null)};var name by remember{mutableStateOf("")};var text by remember{mutableStateOf("")}
    var query by remember{mutableStateOf("")};var category by remember{mutableStateOf("images")};var recursive by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        StudioChips("资产管理",listOf("Prompt 资产","多 Prompt 参照","目录入口","回收站","备份与恢复"),tab,{it}){tab=it}
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
                StudioAction("应用到创作（可撤销）",!c.busy && text.isNotBlank()){c.pastePrompt(text);c.view="创作"}
                StudioAction("复制 Prompt",text.isNotEmpty()){desktopCopyText(text)}
            }
            selected?.let{id->AssetLabelEditor(c,id);StudioAction("加入多 Prompt 参照",!c.busy){c.run("加入参照板"){PromptReferences(library.repository).add(id);tab="多 Prompt 参照";library.refresh()}}
                StudioAction("Prompt 移入回收站",!c.busy&&library.archiveError==null&&id !in library.archived){library.archiveRequest=setOf(id)}
            }
        } else if(tab=="多 Prompt 参照") {PromptReferenceBoard(c)} else if(tab=="回收站") {AssetArchiveContents(c)} else {
            Text("收集目录与不可变资产库分开。同步只读外部目录；外部改动生成新资产版本，移除目录或外部文件不会删除已入库原件。")
            Text("拖放、剪贴板和普通导入另存到专用 imports 目录；在收集目录放入文件后自动入库。备份恢复到新工作区时，旧外部入口需要重新启用。")
            val paused=library.state?.preferences?.get("collectionsPaused")=="true"
            val eligibility=remember(library.state,library.monitorEpoch){runCatching{library.state?.let{library.repository.automaticFolders(it)}.orEmpty()}}
            if(eligibility.isFailure)Text("本机目录登记不可读取，自动收集已停止；已保存资产和分类可继续使用。",color=MaterialTheme.colors.error)
            StudioSwitch("暂停目录自动收集",paused){c.run("设置目录收集"){library.repository.pauseCollections(!paused);library.refresh()}}
            if(library.automaticNotice.isNotBlank())Text(library.automaticNotice)
            StudioAction("检查并重试目录自动收集",!c.busy){c.run("检查目录状态"){library.retryCollections();library.refresh()}}
            StudioAction("启用默认收集目录",!c.busy){library.defaults()}
            CompactChoice("入口类型",listOf("images","imports","prompts"),category,{when(it){"images"->"图片资产收集";"imports"->"导入图片收集";else->"独立 Prompt 收集"}}){category=it}
            StudioSwitch("包含子目录",recursive){recursive=!recursive}
            StudioAction("添加另一个目录指向",!c.busy){library.addFolder(category,recursive)}
            library.state?.folders.orEmpty().forEach { folder->
                Text("${folder.category} · ${folder.path}" )
                StudioActions {
                    if(folder !in eligibility.getOrDefault(emptyList()))StudioAction("在此工作区启用 ${folder.id.take(8)}",!c.busy && eligibility.isSuccess){c.run("确认重新连接目录"){library.repository.enableHere(folder.id);library.refresh()}}
                    StudioAction("同步目录 ${folder.id.take(8)}",!c.busy){library.scan(folder.id)}
                    StudioAction("打开目录 ${folder.id.take(8)}"){java.awt.Desktop.getDesktop().open(Path.of(folder.path).toFile())}
                    StudioAction("移除入口 ${folder.id.take(8)}",!c.busy){c.run("移除目录入口，保留资产"){library.repository.removeFolder(folder.id);library.refresh()}}
                }
            }
            library.scanResults.forEach{Text("${it.path} · ${it.status}")}
            val missing=library.state?.observations?.values?.count{it.status!="PRESENT"}?:0
            if(missing>0)Text("$missing 个来源暂不可用；已保存资产仍可用。")
        }
        StudioAction("查看全部图片资产"){c.view="图库"}
    }
}

@Composable internal fun AssetArchiveContents(c:WorkbenchController) {
    val library=c.library
    Text("移入回收站的资产不再出现在新选用列表。已有工程、参照板、生成记录与原始字节继续保留；外部收集目录文件不受影响。恢复后可继续选用。")
    library.archiveError?.let{Text(it,color=MaterialTheme.colors.error);StudioAction("重读回收站"){library.refresh()};return}
    val contents=remember(library.state){runCatching{library.archive.manageable().filter{it.request().id().toString() in library.archived}}}
    contents.exceptionOrNull()?.let{Text("资产目录不可读取，未删除任何文件",color=MaterialTheme.colors.error);return}
    val assets=contents.getOrThrow()
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
    val eligibility=remember(library.state,library.monitorEpoch,c.maintenance){runCatching{if(c.maintenance)emptyList()else library.state?.let{library.repository.automaticFolders(it)}.orEmpty()}}
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
