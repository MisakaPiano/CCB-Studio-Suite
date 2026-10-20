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

private val Ink=Color(0xff101a26)
private val Panel=Color(0xff192635)
private val Gold=Color(0xffedbc77)
private val Quiet=Color(0xff9caec0)

fun main(args:Array<String>) {
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
        Window(onCloseRequest={closeSafely()},icon=StudioBrand.painter,title="CCB Studio Suite · Windows Preview",state=rememberWindowState(width=1260.dp,height=830.dp)) {
            window.minimumSize=java.awt.Dimension(980,640)
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
                Surface(Modifier.fillMaxSize(),color=Ink,contentColor=Color(0xffedf2f7)) { Studio(c,closing,frames) }
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
                        closeSafely()
                    }
                }
            }
        }
    }
}

private class StudioController(val workspace:StudioWorkspace,val scope:CoroutineScope,val active:(Job)->Unit) {
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

@Composable private fun Studio(c:StudioController,closing:Boolean,frames:java.util.concurrent.atomic.AtomicLong) {
    Column(Modifier.fillMaxSize().drawWithContent {drawContent();frames.incrementAndGet()}.background(Ink).onPreviewKeyEvent {e->
        if(e.type==KeyEventType.KeyDown && e.isCtrlPressed && e.key==Key.Z){c.undo(e.isShiftPressed);true}
        else if(e.type==KeyEventType.KeyDown && e.isCtrlPressed && e.key==Key.Y){c.undo(true);true}else false
    }) {
        Row(Modifier.fillMaxWidth().padding(22.dp,14.dp),verticalAlignment=Alignment.CenterVertically) {
            Image(StudioBrand.painter,"CCB 蓝色 logo",Modifier.size(42.dp));Spacer(Modifier.width(12.dp))
            Column {Text("CCB  /  STUDIO SUITE",fontWeight=FontWeight.Bold,fontSize=21.sp,color=Gold);Text("WINDOWS PREVIEW  ·  0.1.0",fontSize=10.sp,color=Quiet)}
            Spacer(Modifier.weight(1f));Text("隔离合成工作区",color=Gold,fontSize=12.sp)
            Spacer(Modifier.width(18.dp));OutlinedButton(onClick={c.popup="ACCOUNT"},enabled=!c.busy){Text("账户档案")}
            Spacer(Modifier.width(8.dp));OutlinedButton(onClick={c.run("备份图库与工程"){c.popup=c.workspace.backup().toString()}},enabled=!c.busy){Text("备份")}
            Spacer(Modifier.width(8.dp));OutlinedButton(onClick={c.restoreBackup()},enabled=!c.busy){Text("恢复到新目录")}
        }
        Divider(color=Color(0xff314052))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Library(c,Modifier.width(232.dp).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(c.project?.name?:"原图查看",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f));TextButton(onClick={c.undo()},enabled=!c.busy&&c.project!=null){Text("撤销")};TextButton(onClick={c.undo(true)},enabled=!c.busy&&c.project!=null){Text("重做")}
                }
                CanvasView(c,Modifier.weight(1f).fillMaxWidth())
                if(c.warnings.isNotBlank())Text(c.warnings,color=Gold,fontSize=11.sp)
                Row(Modifier.fillMaxWidth().padding(top=10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("${c.selected.size} 项已选 · ${c.page?.layers?.size?:0} 图层",color=Quiet,fontSize=12.sp)
                    Text(if(c.intermediate!=null)"中间结果 · 尚未保存" else "原件与工程分开保存",color=Quiet,fontSize=12.sp)
                }
            }
            Inspector(c,Modifier.width(340.dp).fillMaxHeight().background(Panel))
        }
        Divider(color=Color(0xff314052))
        Row(Modifier.fillMaxWidth().padding(16.dp,9.dp),verticalAlignment=Alignment.CenterVertically) {
            if(c.busy||closing)CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp)
            Spacer(Modifier.width(8.dp));Text(if(closing)"正在取消任务并排水退出…" else c.message,maxLines=2,fontSize=11.sp,color=Quiet)
        }
    }
    c.popup?.let {content->
        if(content=="ACCOUNT") AccountDialog(c)
        else AlertDialog(onDismissRequest={c.popup=null},title={Text("Studio")},text={SelectionContainer {Text(content,Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()))}},confirmButton={TextButton(onClick={c.popup=null}){Text("关闭")}})
    }
}

@Composable private fun Library(c:StudioController,modifier:Modifier) {
    Column(modifier.background(Panel).padding(12.dp)) {
        Text("资料库",fontWeight=FontWeight.Bold,fontSize=17.sp)
        Text("拖入 PNG / JPEG 合成图片",fontSize=11.sp,color=Quiet,modifier=Modifier.padding(vertical=8.dp))
        Row {
            Button(onClick={val chooser=JFileChooser();chooser.isMultiSelectionEnabled=true;if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)c.importFiles(chooser.selectedFiles.toList())},enabled=!c.busy){Text("导入")}
            Spacer(Modifier.width(8.dp));OutlinedButton(onClick={c.fixture()},enabled=!c.busy){Text("样例")}
        }
        OutlinedButton(onClick={c.createProject()},enabled=!c.busy&&c.current!=null,modifier=Modifier.fillMaxWidth()){Text("所选图片 → 新工程")}
        if(c.state.projects.isNotEmpty()) {
            Text("工程",color=Quiet,fontSize=11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState())) {c.state.projects.forEach {p->TextButton(onClick={c.projectId=p.id;c.current=p.current.pages.firstOrNull()?.assetId;c.selected=p.current.pages.map {it.assetId}.toSet();c.refreshImage()}){Text(p.name,color=if(c.projectId==p.id)Gold else Quiet)}}}
        }
        Divider();Spacer(Modifier.height(8.dp))
        val ordered=c.project?.current?.pages?.mapNotNull {p->c.assets.find {it.request().id().toString()==p.assetId}}?:c.assets
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            items(ordered,key={it.request().id().toString()}) {asset->
                val id=asset.request().id().toString()
                var rowHeight by remember {mutableStateOf(60)}
                Row(Modifier.fillMaxWidth().background(if(id==c.current)Color(0xff304354)else Ink,RoundedCornerShape(6.dp)).clickable{c.select(id)}.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(id in c.selected,onCheckedChange={c.select(id)},modifier=Modifier.size(24.dp));Spacer(Modifier.width(6.dp))
                    if(c.project!=null)Text("⋮⋮",color=Gold,modifier=Modifier.onSizeChanged {rowHeight=it.height.coerceAtLeast(40)}.padding(5.dp).pointerInput(c.projectId,id,c.busy) {
                        var distance=0f
                        detectDragGestures(onDragStart={distance=0f},onDragEnd={if(!c.busy&&kotlin.math.abs(distance)>20)c.reorder(id,if(distance>0)kotlin.math.ceil(distance/rowHeight).toInt() else kotlin.math.floor(distance/rowHeight).toInt())},onDrag={change,amount->change.consume();distance+=amount.y})
                    })
                    Column(Modifier.weight(1f)){Text(asset.request().name(),maxLines=2,fontSize=12.sp);Text("${asset.size()/1024} KB · ${asset.sha256().take(8)}",fontSize=10.sp,color=Quiet)}
                }
            }
        }
        if(c.project!=null)Row {TextButton(onClick={c.run("排序上移"){c.workspace.edit(c.projectId!!){p->val pages=p.current.pages.toMutableList();val i=pages.indexOfFirst {it.assetId==c.current};if(i>0)java.util.Collections.swap(pages,i,i-1);p.change(p.current.copy(pages=pages))}}},enabled=!c.busy){Text("↑ 上移")};TextButton(onClick={c.run("排序下移"){c.workspace.edit(c.projectId!!){p->val pages=p.current.pages.toMutableList();val i=pages.indexOfFirst {it.assetId==c.current};if(i>=0&&i<pages.lastIndex)java.util.Collections.swap(pages,i,i+1);p.change(p.current.copy(pages=pages))}}},enabled=!c.busy){Text("↓ 下移")}}
        TextButton(onClick={c.selected=ordered.map {it.request().id().toString()}.toSet()}){Text("全选原图")}
    }
}

@Composable private fun CanvasView(c:StudioController,modifier:Modifier) {
    val bitmap=c.image
    BoxWithConstraints(modifier.background(Color(0xff0a111b),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {
        if(bitmap==null){Text("从样例开始，或拖入合成图片",color=Quiet);return@BoxWithConstraints}
        val ratio=minOf(maxWidth.value/bitmap.width,maxHeight.value/bitmap.height)
        var points by remember {mutableStateOf(listOf<Point>())}
        val density=androidx.compose.ui.platform.LocalDensity.current.density
        Image(bitmap,"当前图像",contentScale=ContentScale.Fit,modifier=Modifier.size((bitmap.width*ratio).dp,(bitmap.height*ratio).dp).pointerInput(c.tool,c.projectId,c.current,c.busy,ratio) {
            detectDragGestures(onDragStart={p->points=listOf(Point(p.x/(density*ratio),p.y/(density*ratio)))},onDragEnd={
                if(!c.busy&&c.projectId!=null&&points.size>=2){val xs=points.map{it.x};val ys=points.map{it.y};val actual=if(c.tool=="POLYGON"&&points.size<3)points+Point(points.last().x,points.first().y)else points;c.addLayer(actual,xs.min(),ys.min(),xs.max()-xs.min(),ys.max()-ys.min())};points=emptyList()
            },onDrag={change,_->change.consume();points=points+Point(change.position.x/(density*ratio),change.position.y/(density*ratio))})
        })
    }
}

@Composable private fun Inspector(c:StudioController,modifier:Modifier) {
    Column(modifier) {
        TabRow(c.tab,backgroundColor=Panel,contentColor=Gold){listOf("生成","编辑","记录").forEachIndexed {i,t->Tab(c.tab==i,onClick={c.tab=i},text={Text(t)})}}
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            when(c.tab) {
                0->{
                    Text("设计与继续修改",fontWeight=FontWeight.Bold)
                    ProfilePicker("LLM",c.state.accounts,c.llmId){c.llmId=it}
                    Field("画面描述 / 修改要求",c.instruction,{c.instruction=it},3)
                    Row {Button(onClick={c.design(false)},enabled=!c.busy){Text("AI 设计")};Spacer(Modifier.width(8.dp));OutlinedButton(onClick={c.design(true)},enabled=!c.busy){Text("继续修改")}}
                    Divider();Field("正向 Prompt",c.prompt,{c.prompt=it},4);Field("负向 Prompt",c.negative,{c.negative=it},3)
                    Field("完整角色列表 JSON · 顺序 / 坐标 / 负向",c.characters,{c.characters=it},3)
                    ProfilePicker("NAI",c.state.accounts,c.naiId){c.naiId=it}
                    Row {NovelAiImageModel.entries.forEach {m->TextButton(onClick={c.imageModel=m}){Text(if(m==NovelAiImageModel.V4_5_FULL)"V4.5"else"V5",color=if(c.imageModel==m)Gold else Quiet)}}}
                    Row {Box(Modifier.weight(1f)){Field("宽",c.width,{c.width=it})};Spacer(Modifier.width(8.dp));Box(Modifier.weight(1f)){Field("高",c.height,{c.height=it})}}
                    Row {Box(Modifier.weight(1f)){Field("Steps",c.steps,{c.steps=it})};Spacer(Modifier.width(8.dp));Box(Modifier.weight(1f)){Field("Guidance",c.guidance,{c.guidance=it})}}
                    Field("固定 Seed",c.seed,{c.seed=it})
                    Button(onClick={c.generate()},enabled=!c.busy,modifier=Modifier.fillMaxWidth()){Text("生成合成结果")}
                    Text("当前界面固定使用 SYNTHETIC。真实限额验证由独立受控入口执行；不会因点击产生付费请求。",color=Quiet,fontSize=11.sp)
                }
                1->{
                    Text("非破坏图层",fontWeight=FontWeight.Bold)
                    Text("先建立工程；在图像上拖绘。动作应用于工程中已选图片。",fontSize=12.sp,color=Quiet)
                    Row {listOf("RECT" to "矩形","BRUSH" to "画笔","POLYGON" to "圈选","TEXT" to "文字").forEach {(id,label)->TextButton(onClick={c.tool=id},contentPadding=PaddingValues(5.dp)){Text(label,color=if(c.tool==id)Gold else Quiet)}}}
                    if(c.tool=="TEXT") {Field("文字",c.text,{c.text=it},3);Field("字体",c.font,{c.font=it});Field("字号",c.fontSize,{c.fontSize=it});Row(verticalAlignment=Alignment.CenterVertically){Checkbox(c.vertical,{c.vertical=it});Text("竖排")}}
                    else Row(verticalAlignment=Alignment.CenterVertically){Checkbox(c.mosaic,{c.mosaic=it});Text(if(c.mosaic)"马赛克" else "纯色遮盖")}
                    OutlinedButton(onClick={c.addLayer(if(c.tool=="POLYGON")listOf(Point(20f,20f),Point(180f,20f),Point(100f,130f))else listOf(Point(20f,20f),Point(180f,130f)),20f,20f,160f,110f)},enabled=!c.busy&&c.project!=null){Text("添加示例图层")}
                    c.page?.layers?.forEachIndexed {i,l->Column {Row(verticalAlignment=Alignment.CenterVertically){Text("${i+1}. ${l.kind} ${if(l.kind=="TEXT")l.text.take(10)else""}",Modifier.weight(1f),fontSize=12.sp);TextButton(onClick={c.run("隐藏 / 显示图层"){c.workspace.edit(c.projectId!!){p->p.change(p.current.copy(pages=p.current.pages.map{page->if(page.assetId==c.current)page.copy(layers=page.layers.map {if(it.id==l.id)it.copy(visible=!it.visible)else it})else page}))}}}){Text(if(l.visible)"隐藏"else"显示")}}
                        Row {
                            if(l.kind=="TEXT")TextButton(onClick={c.run("更新文字图层"){c.workspace.edit(c.projectId!!){p->p.change(p.current.copy(pages=p.current.pages.map {page->if(page.assetId==c.current)page.copy(layers=page.layers.map {if(it.id==l.id)it.copy(text=c.text,font=c.font,fontSize=c.fontSize.toInt(),vertical=c.vertical)else it})else page}))}}}){Text("应用文字设置",fontSize=10.sp)}
                            TextButton(onClick={c.run("移除图层 · 可撤销"){c.workspace.edit(c.projectId!!){p->p.change(p.current.copy(pages=p.current.pages.map {page->if(page.assetId==c.current)page.copy(layers=page.layers.filter {it.id!=l.id})else page}))}}}){Text("移除",fontSize=10.sp)}
                        }
                    }}
                    Divider();Text("分类导出",fontWeight=FontWeight.Bold)
                    Button(onClick={c.export("PRIVACY")},enabled=!c.busy&&c.project!=null,modifier=Modifier.fillMaxWidth()){Text("隐私渲染 PNG")}
                    OutlinedButton(onClick={c.export("NORMAL")},enabled=!c.busy&&c.project!=null,modifier=Modifier.fillMaxWidth()){Text("普通渲染 + Recipe")}
                    OutlinedButton(onClick={c.export("ORIGINAL")},enabled=!c.busy&&c.project!=null,modifier=Modifier.fillMaxWidth()){Text("原始文件 · 保留字节")}
                    Text("遮盖效果请检查后分享。Preview 尚未通过正式更新与真实图库长期保全门槛。",fontSize=11.sp,color=Quiet)
                }
                2->{
                    Text("元数据与来源",fontWeight=FontWeight.Bold)
                    c.assets.firstOrNull {it.request().id().toString()==c.current}?.let {a->
                        SelectionContainer {Text("${a.request().name()}\n${a.size()} bytes\nSHA-256 ${a.sha256()}",fontSize=11.sp)}
                        OutlinedButton(onClick={c.popup=a.request().recipe().decodeToString().ifBlank {"导入原图没有 Suite Recipe；可查看原始 PNG metadata。"}}){Text("完整 Recipe")}
                        OutlinedButton(onClick={c.run("读取 PNG 元数据"){val p=c.workspace.assets.root().resolve("objects/${a.sha256()}.blob");c.popup=NovelAiPngMetadataReader.readStudio(p.toString())?.toString()?:"未发现可识别的 NAI metadata；原件未改动。"}}){Text("PNG Metadata")}
                    }
                    OutlinedButton(onClick={val recipes=c.assets.filter{it.request().id().toString() in c.selected}.map{it.request().recipe().decodeToString()};c.popup=if(recipes.size==2&&recipes.all{it.isNotBlank()})compareRecipes(recipes[0],recipes[1]).joinToString("\n\n").ifBlank{"两份完整 Recipe 一致"}else"请选择两张有完整 Recipe 的生成图。"}){Text("比较所选两份 Recipe")}
                    Divider();Text("设计历史 ${c.state.designs.size} · 任务 ${c.state.jobs.size}",color=Quiet)
                    c.state.designs.takeLast(10).reversed().forEach {d->TextButton(onClick={val p=documentJson.decodeFromString<NovelAiPromptPlan>(d.resultPlan);c.prompt=p.baseCaption;c.characters=documentJson.encodeToString(p.characterCaptions);c.tab=0}){Text(d.instruction.take(30),maxLines=2)}}
                    c.state.jobs.takeLast(10).reversed().forEach {j->Text("${j.state} · ${j.account.provider}\n${j.account.alias} / ${j.account.model}\n${j.results.size} 已保存",fontSize=11.sp,color=Quiet)}
                    Divider();Text("固定核心 ${AUTHORITY_SHA.take(12)}\n应用内安装更新：CP4 NOT TESTED\n当前版本仅供集中 Preview 验收",fontSize=11.sp,color=Quiet)
                    TextButton(onClick={c.popup=c.workspace.root.toString()}){Text("查看数据根路径")}
                }
            }
        }
    }
}

@Composable private fun Field(label:String,value:String,change:(String)->Unit,lines:Int=1) {
    OutlinedTextField(value,change,label={Text(label,fontSize=11.sp)},modifier=Modifier.fillMaxWidth(),singleLine=lines==1,maxLines=if(lines==1)1 else 10,textStyle=LocalTextStyle.current.copy(fontSize=12.sp))
}
@Composable private fun ProfilePicker(service:String,accounts:List<AccountProfile>,selected:String,choose:(String)->Unit) {
    var expanded by remember {mutableStateOf(false)}
    Box {OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(accounts.firstOrNull{it.id==selected}?.let{"${it.alias} · ${it.provider}"}?:service,fontSize=11.sp)}
        DropdownMenu(expanded,{expanded=false}){accounts.filter{it.service==service}.forEach{a->DropdownMenuItem(onClick={choose(a.id);expanded=false}){Text("${a.alias} · ${a.provider} / ${a.model}")}}}}
}
@Composable private fun AccountDialog(c:StudioController) {
    var alias by remember {mutableStateOf("")};var service by remember {mutableStateOf("LLM")}
    AlertDialog(onDismissRequest={c.popup=null},title={Text("命名账户档案")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Preview 档案仅连接合成 transport。真实凭据验证独立限流，账户标识在任务创建时冻结。",fontSize=12.sp)
        c.state.accounts.forEach {Text("${it.alias} · ${it.service} · ${it.provider}",fontSize=12.sp)}
        Field("新档案名称",alias,{alias=it});Row {TextButton(onClick={service="LLM"}){Text("LLM",color=if(service=="LLM")Gold else Quiet)};TextButton(onClick={service="NAI"}){Text("NAI",color=if(service=="NAI")Gold else Quiet)}}
    }},confirmButton={Button(onClick={c.popup=null;c.run("保存命名档案"){c.workspace.save(c.state.copy(accounts=c.state.accounts+AccountProfile(alias=alias,service=service,provider="SYNTHETIC",model=if(service=="LLM")"fixture-designer" else "nai-diffusion-4-5-full"))) }},enabled=alias.isNotBlank()){Text("新增")}},dismissButton={TextButton(onClick={c.popup=null}){Text("关闭")}})
}
