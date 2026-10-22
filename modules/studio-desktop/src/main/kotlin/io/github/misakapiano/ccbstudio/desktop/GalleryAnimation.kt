package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.*

/** Uses the pinned Desktop codec/animation semantics. Never treats a static cover as the restored content. */
internal suspend fun animationTransform(bytes:ByteArray,mode:String,cover:ByteArray?=null):ByteArray {
    require(mode in setOf("disguise","restore"))
    if(mode=="disguise" && ImageFormats.format(bytes)=="webp")org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->
        org.jetbrains.skia.Codec.makeFromData(data).use{require(it.frameCount<=1){"动态WebP不能静默转成静态伪装，请保留原件"}}
    }
    return if(mode=="disguise")DesktopImageTools.disguise(bytes,cover)else DesktopImageTools.restore(bytes)
}
/** PNG's default IDAT image is the disguise cover; an animation renderer may show the content instead. */
internal fun animationDefaultPreview(bytes:ByteArray):java.awt.image.BufferedImage {
    require(bytes.size.toLong() in 1..com.example.chatbar.domain.image.ApngDisguiseCodec.MAX_OUTPUT_BYTES)
    javax.imageio.ImageIO.createImageInputStream(java.io.ByteArrayInputStream(bytes)).use{input->
        val reader=javax.imageio.ImageIO.getImageReadersByFormatName("png").next()
        try {
            reader.input=input;val width=reader.getWidth(0);val height=reader.getHeight(0)
            require(width>0&&height>0&&width.toLong()*height<=DesktopImageEditing.MAX_PIXELS)
            val parameters=reader.defaultReadParam
            val sample=kotlin.math.ceil(maxOf(width,height)/2048.0).toInt().coerceAtLeast(1)
            parameters.setSourceSubsampling(sample,sample,0,0)
            return reader.read(0,parameters)
        } finally {reader.dispose()}
    }
}
internal data class AnimationCoverSnapshot(val preset:DisguiseCover,val bytes:ByteArray)
internal fun needsStaticEditingConsent(c:WorkbenchController,id:String):Boolean {
    val bytes=c.workspace.bytes(id)
    return when(ImageFormats.format(bytes)){
        "gif"->true
        "png"->c.metadataFile(id){com.example.chatbar.domain.image.ApngDisguiseCodec.containsAnimationControl(it.toFile())}
        "webp"->org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->org.jetbrains.skia.Codec.makeFromData(data).use{it.frameCount>1}}
        else->false
    }
}
internal suspend fun exportAnimationDeck(c:WorkbenchController,ids:List<String>,mode:String,parent:Path,progress:(Int)->Unit={},hook:(String)->Unit={},cover:AnimationCoverSnapshot?=null):Path {
    require(ids.isNotEmpty() && ids.size<=1000 && ids.distinct().size==ids.size)
    safePath(parent);require(Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS))
    require(!parent.toAbsolutePath().normalize().startsWith(c.workspace.root.toAbsolutePath().normalize())){"不能向资料根导出"}
    val name="Studio-APNG-${newId()}";val stage=Files.createDirectory(parent.resolve(".$name.incomplete"));val receipts=mutableListOf<JsonElement>()
    for((index,id) in ids.withIndex()){
        currentCoroutineContext().ensureActive();hook("BEFORE_FILE")
        val asset=c.workspace.assets.read(java.util.UUID.fromString(id)).require()
        val bytes=animationTransform(c.workspace.bytes(id),mode,cover?.bytes);val file="${(index+1).toString().padStart(4,'0')}.png"
        writeNewForced(stage.resolve(file),bytes)
        receipts+=buildJsonObject{put("file",file);put("sourceId",id);put("sourceSha256",asset.sha256());put("bytes",bytes.size);put("sha256",sha256(bytes))}
        progress(index+1);hook("FILE_WRITTEN")
    }
    currentCoroutineContext().ensureActive()
    writeNewForced(stage.resolve("COMPLETE.json"),buildJsonObject{put("version",1);put("mode",mode);put("files",JsonArray(receipts));if(mode=="disguise"){put("cover",cover?.preset?.id?:"CCB_DEFAULT");cover?.let{put("coverSha256",it.preset.sha256)}}}.toString().toByteArray(Charsets.UTF_8))
    hook("BEFORE_PUBLISH");val target=parent.resolve(name);Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE);return target
}

internal class GalleryAnimationController(private val c:WorkbenchController,private val temp:Path) {
    private val covers=DisguiseCovers(c.workspace)
    var coverState by mutableStateOf<DisguiseCoverState?>(null);private set
    var coverError by mutableStateOf<String?>(null);private set
    var playResult by mutableStateOf(false);private set
    var previewBusy by mutableStateOf(false);private set
    private var previewTask:Job?=null
    private val previewJobs=java.util.concurrent.ConcurrentHashMap.newKeySet<Job>()
    private val previewEpoch=java.util.concurrent.atomic.AtomicLong()
    internal var previewTransform:suspend (ByteArray,String,ByteArray?)->ByteArray=::animationTransform
    var chooseCoverFile:()->Path?={StudioFileDialogs.open("选择静态伪装封面",listOf("png","jpg","jpeg","webp","bmp"))}
    var ids by mutableStateOf<List<String>?>(null)
    var mode by mutableStateOf("disguise");private set
    var index by mutableStateOf(0);private set
    var preview by mutableStateOf<ByteArray?>(null);private set
    var error by mutableStateOf<String?>(null);private set
    var completed by mutableStateOf<Path?>(null);private set
    var progress by mutableStateOf(0);private set
    var copyFiles:(List<Path>)->Unit=ExportClipboard::copy
    var chooseDirectory:()->Path?={StudioFileDialogs.folder("选择APNG输出目录（新建独立文件夹）")}
    init {reloadCovers()}
    fun reloadCovers(){runCatching{covers.load()}.onSuccess{coverState=it;coverError=null}.onFailure{coverState=null;coverError="封面配置读取失败，暂停伪装处理；原配置和原件保留"}}
    private fun invalidate(){previewEpoch.incrementAndGet();previewTask?.cancel();preview=null;error=null;completed=null;playResult=false}
    fun open(selected:List<String>){require(selected.isNotEmpty());ids=selected.toList();index=selected.lastIndex;invalidate();reloadCovers();requestPreview()}
    @Synchronized fun close(){ids=null;invalidate();previewJobs.forEach{it.cancel()};previewBusy=false}
    suspend fun cancelAndDrain(){val jobs=synchronized(this){previewEpoch.incrementAndGet();previewJobs.toList().also{tasks->tasks.forEach{it.cancel()}}};jobs.joinAll();previewBusy=false}
    suspend fun awaitPreview(){previewTask?.join()}
    fun cancelPreview(){previewTask?.cancel()}
    fun selectMode(value:String){check(!c.busy);require(value in setOf("disguise","restore"));mode=value;invalidate();requestPreview()}
    fun selectIndex(value:Int){if(c.busy||c.maintenance)return;index=value.coerceIn(0,ids.orEmpty().lastIndex);invalidate();requestPreview()}
    fun page(delta:Int)=selectIndex(index+delta)
    fun playback(value:Boolean){playResult=value}
    fun selectCover(id:String?)=c.run("选择伪装封面"){covers.select(id);reloadCovers();invalidate();requestPreview()}
    fun removeCover(id:String)=c.run("移除伪装封面配置"){covers.remove(id);reloadCovers();invalidate();requestPreview()}
    fun renameCover(id:String,name:String)=c.run("重命名伪装封面"){covers.rename(id,name);reloadCovers()}
    fun addCover(name:String) {
        if(c.busy||c.maintenance)return
        val path=chooseCoverFile()?:return
        c.run("保存伪装封面"){
            val bytes=readBounded(path,DesktopImageEditing.MAX_BYTES)
            DesktopImageEditing.requireStatic(bytes)
            if(ImageFormats.format(bytes)=="webp")org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->org.jetbrains.skia.Codec.makeFromData(data).use{require(it.frameCount<=1){"封面需要静态图片"}}}
            DesktopImageEditing.decode(bytes).flush()
            covers.add(name.ifBlank{path.fileName.toString().substringBeforeLast('.')}.take(100),path.fileName.toString(),bytes)
            reloadCovers();invalidate();requestPreview()
        }
    }
    private fun selectedCover():AnimationCoverSnapshot? {
        val state=covers.load();val selected=state.covers.firstOrNull{it.id==state.selected}?:return null
        return AnimationCoverSnapshot(selected,covers.bytes(selected))
    }
    @Synchronized fun requestPreview() {
        if(c.maintenance)return
        val source=ids?.getOrNull(index)?:return
        val selectedMode=mode;val epoch=previewEpoch.incrementAndGet();previewTask?.cancel()
        preview=null;error=null;previewBusy=true;playResult=false
        val task=c.scope.launch(start=CoroutineStart.UNDISPATCHED) {
            try {
                delay(120)
                val output=withContext(Dispatchers.IO){
                    val input=c.workspace.operation{c.workspace.bytes(source) to if(selectedMode=="disguise")selectedCover()?.bytes else null}
                    previewTransform(input.first,selectedMode,input.second)
                }
                ensureActive()
                if(previewEpoch.get()==epoch){preview=output;playResult=false}
            }catch(e:CancellationException){if(previewEpoch.get()==epoch)error="预览已停止，可重新预览";throw e}
            catch(e:Exception){if(previewEpoch.get()==epoch)error="预览未完成 · ${e.javaClass.simpleName}；原件未变，可重新预览"}
            finally{previewJobs.remove(currentCoroutineContext().job);if(previewEpoch.get()==epoch)previewBusy=false}
        }
        previewTask=task;previewJobs.add(task);task.invokeOnCompletion{previewJobs.remove(task)}
    }
    fun saveCopy()=c.run("保存APNG工具副本到图库"){
        val bytes=requireNotNull(preview);val source=requireNotNull(ids)[index]
        c.workspace.beginTask().use{
            val cover=if(mode=="disguise")selectedCover()else null
            val receipt=buildJsonObject{put("kind","animation-copy");put("mode",mode);put("sourceId",source);if(mode=="disguise"){put("cover",cover?.preset?.id?:"CCB_DEFAULT");cover?.let{put("coverSha256",it.preset.sha256)}}}.toString()
            val id=c.workspace.importDerived("$mode-copy.png",bytes,source,receipt)
            c.workspace.operation{c.workspace.save(c.workspace.state.copy(savedCopies=c.workspace.state.savedCopies+id))};c.current=id;c.selection=setOf(id)
        }
    }
    fun export(copy:Boolean=false){
        if(c.busy)return
        val destination=try{if(copy)Files.createDirectories(temp)else chooseDirectory()?:return}catch(e:Exception){c.problem="输出目录无法使用，未写入文件";return}
        c.run(if(copy)"复制APNG文件批次"else"导出APNG文件批次"){
            completed=null;progress=0;error=null
            try{c.workspace.beginTask().use{
                val output=exportAnimationDeck(c,requireNotNull(ids),mode,destination,{progress=it},cover=if(mode=="disguise")selectedCover()else null);completed=output
                if(copy)copyFiles(Files.list(output).use{stream->stream.filter{it.fileName.toString().endsWith(".png")}.sorted().toList()})
            }}catch(e:CancellationException){throw e}catch(e:Exception){error="未完成 · ${e.javaClass.simpleName}；已有输出保留，未自动重试";throw e}
        }
    }
}

@Composable internal fun GalleryAnimationWorkspace(c:WorkbenchController) {
    val a=c.animation;val ids=a.ids?:return;val id=ids[a.index]
    val cover by produceState<Result<androidx.compose.ui.graphics.ImageBitmap>?>(null,a.preview){value=withContext(Dispatchers.IO){a.preview?.let{bytes->runCatching{val image=animationDefaultPreview(bytes);try{image.toComposeImageBitmap()}finally{image.flush()}}}}}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        StudioActions{StudioAction("返回图库",!c.busy){a.close()};StudioAction("生成伪装 APNG",!c.busy,selected=a.mode=="disguise"){a.selectMode("disguise")};StudioAction("还原伪装 APNG",!c.busy,selected=a.mode=="restore"){a.selectMode("restore")}}
        Text("${a.index+1} / ${ids.size} · 原件保留。静止显示所选封面，主动播放才显示内容；不是加密或隐私清除。还原保留像素/动画语义。")
        Text("静态图片 / GIF可伪装；动态WebP明确拒绝静态化。已有APNG先还原。",style=MaterialTheme.typography.caption)
        if(a.mode=="disguise")DisguiseCoverControls(c)
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Column(Modifier.weight(1f).fillMaxHeight()){Text("来源 · $id");AssetImage(c,id,Modifier.fillMaxSize())}
            Column(Modifier.weight(1f).fillMaxHeight()){
                Text(if(a.mode=="disguise")"伪装后的封面预览"else"还原结果预览")
                StudioSwitch("播放原内容（查看动画）",a.playResult){a.playback(!a.playResult)}
                if(a.previewBusy){Text("正在自动更新预览…");LinearProgressIndicator(Modifier.fillMaxWidth());StudioAction("停止预览"){a.cancelPreview()}}
                a.preview?.let{bytes->if(a.playResult)DesktopOwnedImage(sha256(bytes),{bytes},Modifier.fillMaxSize())else cover?.onSuccess{Image(it,"处理结果静态封面",Modifier.fillMaxSize())}?.onFailure{Text("静态预览读取失败，未替换为原内容",color=MaterialTheme.colors.error)}}
            }
        }
        Column(Modifier.semantics{contentDescription="APNG 批次缩略图"}) {
            StudioFilmstrip(ids.map{"asset:$it"},"asset:$id",c::readAsset){path->a.selectIndex(ids.indexOf(path.removePrefix("asset:")))}
        }
        val canProcess=!c.busy&&(a.mode=="restore"||a.coverState!=null)
        StudioActions{StudioAction("上一张",!c.busy&&a.index>0){a.page(-1)};StudioAction("下一张",!c.busy&&a.index<ids.lastIndex){a.page(1)};if(a.error!=null)StudioAction("重新预览",canProcess){a.requestPreview()};StudioAction("保存当前副本到图库",!c.busy&&!a.previewBusy&&a.preview!=null){a.saveCopy()}}
        StudioActions{StudioAction("选择目录并导出全部",canProcess){a.export()};StudioAction("复制全部处理文件",canProcess){a.export(true)}}
        if(c.busy&&c.activeOperation in setOf("预览APNG处理","保存APNG工具副本到图库","复制APNG文件批次","导出APNG文件批次"))StudioAction("停止APNG处理"){c.active?.cancel()}
        Text("批次已写 ${a.progress} / ${ids.size}；完整目录与完成清单发布后才成功。失败.incomplete目录保留，不覆盖旧输出。复制使用文件列表保留动画。",style=MaterialTheme.typography.caption)
        a.error?.let{Text(it,color=MaterialTheme.colors.error)};a.completed?.let{Text("完成文件目录：$it",style=MaterialTheme.typography.caption)}
    }
}

@Composable private fun DisguiseCoverControls(c:WorkbenchController) {
    val a=c.animation;val state=a.coverState
    a.coverError?.let{Text(it,color=MaterialTheme.colors.error);StudioAction("重新读取封面配置",!c.busy){a.reloadCovers()}}
    if(state==null)return
    var name by remember{mutableStateOf("")}
    var manage by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().heightIn(max=100.dp).verticalScroll(rememberScrollState())){StudioActions {
        StudioAction("CCB 默认封面",!c.busy,selected=state.selected==null){a.selectCover(null)}
        state.covers.forEach{cover->StudioAction(cover.name,!c.busy,selected=state.selected==cover.id){a.selectCover(cover.id)}}
        StudioAction("管理自定义封面",selected=manage){manage=!manage}
    }}
    Text("当前封面：${state.covers.firstOrNull{it.id==state.selected}?.name?:"CCB 默认封面"} · 自定义图片等比适应、深色补边。切换后需重新预览。",style=MaterialTheme.typography.caption)
    if(manage)Column(Modifier.heightIn(max=180.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(name,{name=it.take(100)},label={Text("封面名称（留空用文件名）")},modifier=Modifier.fillMaxWidth())
        StudioActions {
            StudioAction("添加并保存封面",!c.busy&&state.covers.size<64){a.addCover(name)}
            StudioAction("恢复 CCB 默认封面",!c.busy){a.selectCover(null)}
            state.covers.firstOrNull{it.id==state.selected}?.let{selected->
                StudioAction("重命名所选封面",!c.busy&&name.isNotBlank()){a.renameCover(selected.id,name)}
                StudioAction("移除所选封面配置",!c.busy){a.removeCover(selected.id)}
            }
        }
        Text("封面随工作区保存，可保留多套；恢复默认不删除自定义封面。移除配置保留原件和已有导出。",style=MaterialTheme.typography.caption)
    }
}
