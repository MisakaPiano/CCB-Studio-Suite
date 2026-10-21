package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.nio.file.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import javax.swing.JFileChooser
import java.io.File

internal val Ink=Color(0xff0d1117)
internal val Panel=Color(0xff161b22)
internal val Gold=Color(0xff7db5ff)
internal val Quiet=Color(0xffaab6c5)

fun main(args:Array<String>) {
    if("--synthetic-root" !in args) {launchStudio(args);return}
    fun option(name:String)=args.indexOf(name).takeIf {it>=0}?.let {args.getOrNull(it+1)}
    StudioBrand.configureDefaultWindows()
    val root=option("--synthetic-root")?.let {Paths.get(it)} ?: Paths.get(System.getenv("LOCALAPPDATA"),"CCBStudioSuitePreview","synthetic-v1")
    val workspace=try {StudioWorkspace.open(root)} catch(e:Exception) {
        javax.swing.JOptionPane.showMessageDialog(null,"工作区无法安全打开，现有文件已保留。\n${if(e is DataProblem)e.status else e.javaClass.simpleName}\n$root","CCB Studio Suite",javax.swing.JOptionPane.ERROR_MESSAGE);return
    }
    if(workspace.state.accounts.isEmpty())workspace.save(workspace.state.copy(accounts=listOf(
        AccountProfile(alias="设计 · 合成测试",service="LLM",provider="SYNTHETIC",model="fixture-designer"),
        AccountProfile(alias="生图 · 合成测试",service="NAI",provider="SYNTHETIC",model="nai-diffusion-4-5-full"))))
    application {
        val frames=remember {java.util.concurrent.atomic.AtomicLong(0)}
        val scope=rememberCoroutineScope();var active by remember {mutableStateOf<Job?>(null)}
        var closing by remember {mutableStateOf(false)}
        val c=remember {StudioController(workspace,scope){active=it}}
        fun closeSafely() {if(!closing){closing=true;scope.launch {active?.cancelAndJoin();withContext(Dispatchers.IO){workspace.close()};exitApplication()}}}
        Window(onCloseRequest={closeSafely()},undecorated=DesktopWindowChrome.isWindows,icon=StudioBrand.painter,title="CCB Studio Suite · Windows Preview",state=rememberWindowState(width=1260.dp,height=830.dp)) {
            val chrome=rememberDesktopWindowChrome(window){closeSafely()}
            SideEffect {chrome.appearance(true)}
            DisposableEffect(Unit) {
                val target=DropTarget(window.contentPane,DnDConstants.ACTION_COPY,object:DropTargetAdapter(){
                    override fun drop(event:DropTargetDropEvent) {
                        if(!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){event.rejectDrop();return}
                        event.acceptDrop(DnDConstants.ACTION_COPY)
                        @Suppress("UNCHECKED_CAST") val files=event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        c.importFiles(files);event.dropComplete(true)
                    }
                },true)
                onDispose {target.isActive=false}
            }
            MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=Color(0xffedf2f7))) {
                Surface(Modifier.fillMaxSize(),color=Ink,contentColor=Color(0xffedf2f7)) { Studio(c,closing,frames,chrome) }
            }
            val report=option("--self-check")?:option("--smoke-report")
            report?.let {name->
                LaunchedEffect(Unit) {
                    val result=linkedMapOf<String,JsonElement>()
                    val p=Paths.get(name);Files.createDirectories(p.parent)
                    try {
                        withTimeout(20000) {while(frames.get()==0L)delay(50)}
                        check(window.isShowing && window.width>=980 && window.height>=640)
                        result["firstFrame"]=JsonPrimitive("DRAW_COMPLETED")
                        check(chrome.state.value.failure==null)
                        check(chrome.layout.captions.keys.containsAll(listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE)))
                        if(option("--self-check")!=null) {
                            chrome.command(DesktopWindowCommand.MAXIMIZE);withTimeout(5000){while(!chrome.state.value.maximized)delay(25)}
                            chrome.command(DesktopWindowCommand.RESTORE);withTimeout(5000){while(chrome.state.value.maximized)delay(25)}
                            chrome.command(DesktopWindowCommand.MINIMIZE);withTimeout(5000){while(!chrome.state.value.minimized)delay(25)}
                            chrome.command(DesktopWindowCommand.RESTORE);withTimeout(5000){while(chrome.state.value.minimized)delay(25)}
                            result["nativeWindowCommands"]=JsonPrimitive("PASS: maximize/restore/minimize/restore")
                        }
                        if(option("--self-check")!=null) {
                            suspend fun completed(action:()->Unit) {action();withTimeout(15000){while(c.busy)delay(25)};check(c.popup==null){"Controller operation did not complete"}}
                            completed {c.fixture()};completed {c.fixture()}
                            c.selected=c.assets.map {it.request().id().toString()}.toSet()
                            completed {c.createProject()}
                            c.tab=1;c.tool="RECT"
                            completed {c.addLayer(emptyList(),20f,20f,140f,90f)}
                            c.tool="TEXT";c.text="Preview 合成验证";c.vertical=true
                            completed {c.addLayer(emptyList(),80f,40f,180f,150f)}
                            check(c.project!!.current.pages.all {it.layers.size==2})
                            completed {c.undo()};check(c.project!!.current.pages.all {it.layers.size==1})
                            completed {c.undo(true)};check(c.project!!.current.pages.all {it.layers.size==2})
                            val before=c.project!!.current.pages.map {it.assetId}
                            completed {c.reorder(before[0],1)};check(c.project!!.current.pages.map {it.assetId}==before.reversed())
                            c.tab=0;completed {c.design(false)};completed {c.design(true)};completed {c.generate()}
                            check(c.state.designs.size==2 && c.state.jobs.last().state=="COMPLETED")
                            val pages=c.project!!.current.pages.map {it.assetId}
                            val exports=withContext(Dispatchers.IO){listOf("PRIVACY","NORMAL","ORIGINAL").map {workspace.export(c.projectId!!,pages,it)}}
                            check(exports.all {Files.exists(it.resolve("COMPLETE.txt"))})
                            val backup=withContext(Dispatchers.IO){workspace.backup()}
                            result["backup"]=JsonPrimitive(backup.toString());result["exports"]=JsonPrimitive(exports.size)
                            result["controllerWorkflows"]=JsonPrimitive("PASS")
                            c.tab=1;c.current=pages[0];c.selected=pages.toSet();c.refreshImage()
                            val beforeDraw=frames.get();withTimeout(10000){while(frames.get()<=beforeDraw || c.image==null)delay(50)}
                        }
                        result["status"]=JsonPrimitive("APP_SELF_CHECK_PASS")
                    } catch(e:Exception) {
                        result["status"]=JsonPrimitive("APP_SELF_CHECK_FAILED");result["failureType"]=JsonPrimitive(e.javaClass.simpleName)
                        result["detail"]=JsonPrimitive(e.message?.take(500)?:"")
                    } finally {
                        result["drawCount"]=JsonPrimitive(frames.get());result["windowShowing"]=JsonPrimitive(window.isShowing)
                        result["width"]=JsonPrimitive(window.width);result["height"]=JsonPrimitive(window.height)
                        result["assets"]=JsonPrimitive(c.assets.size);result["projects"]=JsonPrimitive(c.state.projects.size)
                        result["stateSha256"]=JsonPrimitive(sha256(documentJson.encodeToString(c.state).toByteArray()))
                        result["originals"]=buildJsonObject {c.assets.forEach {a->put(a.request().id().toString(),a.sha256())}}
                        result["designs"]=JsonPrimitive(c.state.designs.size);result["jobs"]=JsonPrimitive(c.state.jobs.size)
                        result["visualAndMouseAcceptance"]=JsonPrimitive("NOT TESTED: screen capture/input unavailable")
                        withContext(Dispatchers.IO){writeNewForced(p,JsonObject(result).toString().toByteArray())}
                        chrome.command(DesktopWindowCommand.CLOSE)
                    }
                }
            }
        }
    }
}

internal class StudioController(val workspace:StudioWorkspace,val scope:CoroutineScope,val active:(Job)->Unit) {
    var state by mutableStateOf(workspace.state)
    var assets by mutableStateOf(workspace.catalog().filter {it.request().kind()==AssetStore.Kind.ORIGINAL})
    var selected by mutableStateOf(setOf<String>())
    var current by mutableStateOf<String?>(assets.firstOrNull()?.request()?.id()?.toString())
    var projectId by mutableStateOf<String?>(state.projects.firstOrNull()?.id)
    var busy by mutableStateOf(false);var message by mutableStateOf("准备就绪 · 所有操作仅用于隔离测试资料")
    var prompt by mutableStateOf("ceramic cup, quiet desk, soft window light")
    var negative by mutableStateOf(NovelAiPromptPlan("",emptyList()).negativePrompt)
    var instruction by mutableStateOf("窗边桌面上的白色陶瓷杯")
    var characters by mutableStateOf("[]")
    var seed by mutableStateOf("20261021");var imageModel by mutableStateOf(NovelAiImageModel.V4_5_FULL)
    var width by mutableStateOf("512");var height by mutableStateOf("512");var steps by mutableStateOf("28");var guidance by mutableStateOf("6")
    var llmId by mutableStateOf(state.accounts.first {it.service=="LLM"}.id)
    var naiId by mutableStateOf(state.accounts.first {it.service=="NAI"}.id)
    var intermediate by mutableStateOf<ByteArray?>(null)
    var popup by mutableStateOf<String?>(null)
    var image by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    var warnings by mutableStateOf("")
    var compactPanel by mutableStateOf("CANVAS")
    var showLibrary by mutableStateOf(true)
    var zoom by mutableStateOf(1f)
    var tab by mutableStateOf(0)
    var tool by mutableStateOf("RECT");var text by mutableStateOf("合成预览");var vertical by mutableStateOf(false)
    var mosaic by mutableStateOf(true);var font by mutableStateOf("Dialog");var fontSize by mutableStateOf("28")
    val project get()=state.projects.firstOrNull {it.id==projectId}
    val page get()=project?.current?.pages?.firstOrNull {it.assetId==current}
    init {refreshImage()}
    fun run(label:String,body:suspend ()->Unit) {
        if(busy)return
        busy=true;message=label
        val job=scope.launch {
            try {withContext(Dispatchers.IO){body();state=workspace.state;assets=workspace.catalog().filter {it.request().kind()==AssetStore.Kind.ORIGINAL}};message="$label · 完成";refreshImage()}
            catch(e:CancellationException){message="已取消，收到的原图与已提交资料保留";throw e}
            catch(e:Exception){message="操作未完成：${if(e is DataProblem)e.status else e.javaClass.simpleName}。已有资料保留。";popup=e.message?.take(1500)?:"操作未完成"}
            finally {busy=false;intermediate=null}
        };active(job)
    }
    fun refreshImage() {
        val id=current ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val r=EditorRenderer.render(workspace.bytes(id),page?.layers?:emptyList())
                image=r.image.toComposeImageBitmap();warnings=r.warnings.joinToString("\n");r.image.flush()
            } catch(e:Exception){image=null;warnings="预览无法解码：${e.javaClass.simpleName}；原件保留"}
        }
    }
    fun reorder(id:String,delta:Int)=run("排列图片") {
        workspace.edit(projectId!!) {p->
            val pages=p.current.pages.toMutableList();val from=pages.indexOfFirst {it.assetId==id}
            if(from<0)p else {val to=(from+delta).coerceIn(0,pages.lastIndex);pages.add(to,pages.removeAt(from));p.change(p.current.copy(pages=pages))}
        }
    }
    fun restoreBackup() {
        val chooser=JFileChooser(workspace.root.resolve("backups").toFile());chooser.fileSelectionMode=JFileChooser.DIRECTORIES_ONLY
        if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) {
            val source=chooser.selectedFile.toPath()
            run("验证并恢复到新工作区") {
                val destination=workspace.root.parent.resolve("restored-preview-${newId()}")
                StudioRestore.restore(source,destination)
                popup="恢复已完成，当前工作区保持打开。下次启动使用：\n--synthetic-root \"$destination\"\n普通备份不包含凭据，请重新添加。"
            }
        }
    }
    fun select(id:String){current=id;selected=if(id in selected)selected-id else selected+id;refreshImage()}
    fun pickImport() {val chooser=JFileChooser();chooser.isMultiSelectionEnabled=true;if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)importFiles(chooser.selectedFiles.toList())}
    fun importFiles(files:List<File>)=run("导入原始文件") {
        val ids=files.map {f->workspace.importImage(f.name,readBounded(f.toPath(),EditorRenderer.MAX_BYTES))}
        current=ids.firstOrNull();selected=ids.toSet()
    }
    fun fixture()=run("创建合成样例") {val id=workspace.importImage("synthetic.png",EditorRenderer.synthetic());current=id;selected=setOf(id)}
    fun createProject()=run("新建非破坏工程") {val ids=selected.ifEmpty {setOfNotNull(current)}.toList();projectId=workspace.newProject("工程 ${state.projects.size+1}",ids)}
    fun plan()=NovelAiPromptPlan(prompt,documentJson.decodeFromString<List<NovelAiCharacterCaption>>(characters),negativePrompt=negative)
    fun design(revise:Boolean)=run(if(revise)"AI 继续修改 · 合成" else "AI 设计 · 合成") {
        val account=state.accounts.first {it.id==llmId}.copy()
        val result=GenerationEngine(workspace).design(instruction,account,if(revise)plan()else null){message=it.takeLast(300)}
        prompt=result.baseCaption;characters=documentJson.encodeToString(result.characterCaptions)
    }
    fun generate()=run("NAI 生图链路 · 合成") {
        val account=state.accounts.first {it.id==naiId}.copy(model=imageModel.apiId)
        val settings=NovelAiGenerationSettings(model=imageModel,customWidth=width.toInt(),customHeight=height.toInt(),steps=steps.toInt(),guidance=guidance.toFloat(),seedMode=NovelAiSeedMode.FIXED,seed=seed.toLong())
        val job=GenerationEngine(workspace).generate(plan(),settings,account){bytes->intermediate=bytes;image=EditorRenderer.decode(bytes).toComposeImageBitmap()}
        require(job.state=="COMPLETED"){job.detail};current=job.results.first();selected=job.results.toSet()
    }
    fun addLayer(points:List<Point>,x:Float,y:Float,w:Float,h:Float)=run("保存图层") {
        val pid=projectId?:error("先用所选原图建立工程")
        val targets=selected.filter {id->project!!.current.pages.any {it.assetId==id}}.toSet().ifEmpty {setOfNotNull(current)}
        workspace.applyLayers(pid,targets,listOf(Layer(kind=tool,x=x,y=y,width=if(tool=="BRUSH")18f else maxOf(w,8f),height=maxOf(h,8f),text=text,font=font,fontSize=fontSize.toInt(),vertical=vertical,mosaic=mosaic,points=points)))
    }
    fun undo(redo:Boolean=false)=run(if(redo)"重做" else "撤销") {workspace.edit(projectId?:error("没有工程")){if(redo)it.redo()else it.undo()}}
    fun export(mode:String)=run("导出 $mode") {val p=project?:error("先建立工程");val ids=p.current.pages.map {it.assetId}.filter {selected.isEmpty()||it in selected};popup=workspace.export(p.id,ids,mode).toString()}
}

