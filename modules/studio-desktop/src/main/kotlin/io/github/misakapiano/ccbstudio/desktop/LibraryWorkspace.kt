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

internal class LibraryController(private val c:WorkbenchController) {
    var tab by mutableStateOf("Prompt 资产")
    val repository=LibraryRepository(c.workspace)
    var state by mutableStateOf<LibraryState?>(null);private set
    var error by mutableStateOf<String?>(null);private set
    var prompts by mutableStateOf(listOf<io.github.misakapiano.ccbstudio.assets.AssetStore.Asset>());private set
    var scanResults by mutableStateOf(listOf<FolderScanResult>());private set
    init{refresh()}
    fun refresh(){runCatching{repository.load()}.onSuccess{state=it;error=null;prompts=repository.prompts()}.onFailure{error="资产管理记录读取失败，原件保留，管理写入已停止：${it.javaClass.simpleName}";state=null}}
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
        StudioChips("资产管理",listOf("Prompt 资产","多 Prompt 参照","目录入口"),tab,{it}){tab=it}
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
            selected?.let{id->AssetLabelEditor(c,id);StudioAction("加入多 Prompt 参照",!c.busy){c.run("加入参照板"){PromptReferences(library.repository).add(id);tab="多 Prompt 参照";library.refresh()}}}
        } else if(tab=="多 Prompt 参照") {PromptReferenceBoard(c)} else {
            Text("收集目录与不可变资产库分开。同步只读外部目录；外部改动生成新资产版本，移除目录或外部文件不会删除已入库原件。")
            StudioAction("启用默认收集目录",!c.busy){library.defaults()}
            CompactChoice("入口类型",listOf("images","imports","prompts"),category,{when(it){"images"->"图片资产收集";"imports"->"导入图片收集";else->"独立 Prompt 收集"}}){category=it}
            StudioSwitch("包含子目录",recursive){recursive=!recursive}
            StudioAction("添加另一个目录指向",!c.busy){library.addFolder(category,recursive)}
            library.state?.folders.orEmpty().forEach { folder->
                Text("${folder.category} · ${folder.path}" )
                StudioActions {
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
