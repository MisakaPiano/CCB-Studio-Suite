package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.*

/** Uses the pinned Desktop codec/animation semantics. Never treats a static cover as the restored content. */
internal suspend fun animationTransform(bytes:ByteArray,mode:String):ByteArray {
    require(mode in setOf("disguise","restore"))
    if(mode=="disguise" && ImageFormats.format(bytes)=="webp")org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->
        org.jetbrains.skia.Codec.makeFromData(data).use{require(it.frameCount<=1){"动态WebP不能静默转成静态伪装，请保留原件"}}
    }
    return if(mode=="disguise")DesktopImageTools.disguise(bytes)else DesktopImageTools.restore(bytes)
}
internal fun needsStaticEditingConsent(c:WorkbenchController,id:String):Boolean {
    val bytes=c.workspace.bytes(id)
    return when(ImageFormats.format(bytes)){
        "gif"->true
        "png"->c.metadataFile(id){com.example.chatbar.domain.image.ApngDisguiseCodec.containsAnimationControl(it.toFile())}
        "webp"->org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->org.jetbrains.skia.Codec.makeFromData(data).use{it.frameCount>1}}
        else->false
    }
}
internal suspend fun exportAnimationDeck(c:WorkbenchController,ids:List<String>,mode:String,parent:Path,progress:(Int)->Unit={},hook:(String)->Unit={}):Path {
    require(ids.isNotEmpty() && ids.size<=1000 && ids.distinct().size==ids.size)
    safePath(parent);require(Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS))
    require(!parent.toAbsolutePath().normalize().startsWith(c.workspace.root.toAbsolutePath().normalize())){"不能向资料根导出"}
    val name="Studio-APNG-${newId()}";val stage=Files.createDirectory(parent.resolve(".$name.incomplete"));val receipts=mutableListOf<JsonElement>()
    for((index,id) in ids.withIndex()){
        currentCoroutineContext().ensureActive();hook("BEFORE_FILE")
        val asset=c.workspace.assets.read(java.util.UUID.fromString(id)).require()
        val bytes=animationTransform(c.workspace.bytes(id),mode);val file="${(index+1).toString().padStart(4,'0')}.png"
        writeNewForced(stage.resolve(file),bytes)
        receipts+=buildJsonObject{put("file",file);put("sourceId",id);put("sourceSha256",asset.sha256());put("bytes",bytes.size);put("sha256",sha256(bytes))}
        progress(index+1);hook("FILE_WRITTEN")
    }
    currentCoroutineContext().ensureActive()
    writeNewForced(stage.resolve("COMPLETE.json"),buildJsonObject{put("version",1);put("mode",mode);put("files",JsonArray(receipts))}.toString().toByteArray(Charsets.UTF_8))
    hook("BEFORE_PUBLISH");val target=parent.resolve(name);Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE);return target
}

internal class GalleryAnimationController(private val c:WorkbenchController,private val temp:Path) {
    var ids by mutableStateOf<List<String>?>(null)
    var mode by mutableStateOf("disguise");private set
    var index by mutableStateOf(0);private set
    var preview by mutableStateOf<ByteArray?>(null);private set
    var error by mutableStateOf<String?>(null);private set
    var completed by mutableStateOf<Path?>(null);private set
    var progress by mutableStateOf(0);private set
    var copyFiles:(List<Path>)->Unit=ExportClipboard::copy
    var chooseDirectory:()->Path?={StudioFileDialogs.folder("选择APNG输出目录（新建独立文件夹）")}
    fun open(selected:List<String>){ids=selected;index=0;preview=null;error=null;completed=null}
    fun selectMode(value:String){check(!c.busy);mode=value;preview=null;error=null;completed=null}
    fun page(delta:Int){check(!c.busy);index=(index+delta).coerceIn(0,ids.orEmpty().lastIndex);preview=null;error=null}
    fun process()=c.run("预览APNG处理"){
        preview=null;error=null
        try{c.workspace.beginTask().use{preview=animationTransform(c.workspace.bytes(requireNotNull(ids)[index]),mode)}}
        catch(e:CancellationException){throw e}catch(e:Exception){error="处理失败 · ${e.javaClass.simpleName}；原件未变";throw e}
    }
    fun saveCopy()=c.run("保存APNG工具副本到图库"){
        val bytes=requireNotNull(preview);val source=requireNotNull(ids)[index]
        c.workspace.beginTask().use{
            val receipt=buildJsonObject{put("kind","animation-copy");put("mode",mode);put("sourceId",source)}.toString()
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
                val output=exportAnimationDeck(c,requireNotNull(ids),mode,destination,{progress=it});completed=output
                if(copy)copyFiles(Files.list(output).use{stream->stream.filter{it.fileName.toString().endsWith(".png")}.sorted().toList()})
            }}catch(e:CancellationException){throw e}catch(e:Exception){error="未完成 · ${e.javaClass.simpleName}；已有输出保留，未自动重试";throw e}
        }
    }
}

@Composable internal fun GalleryAnimationWorkspace(c:WorkbenchController) {
    val a=c.animation;val ids=a.ids?:return;val id=ids[a.index]
    val cover by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,a.preview){value=withContext(Dispatchers.IO){a.preview?.let{bytes->val image=EditorRenderer.decode(bytes);try{image.toComposeImageBitmap()}finally{image.flush()}}}}
    var animated by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        StudioActions{StudioAction("返回图库",!c.busy){a.ids=null};StudioAction("生成伪装 APNG",!c.busy,selected=a.mode=="disguise"){a.selectMode("disguise")};StudioAction("还原伪装 APNG",!c.busy,selected=a.mode=="restore"){a.selectMode("restore")}}
        Text("${a.index+1} / ${ids.size} · 原件保留。伪装使用CCB默认封面，播放时显示内容，不是加密或隐私清除。还原保留像素/动画语义，不承诺恢复原编码和所有元数据。")
        Text("静态图片 / GIF可伪装；动态WebP明确拒绝静态化。已有APNG先还原；当前不自定义伪装封面。",style=MaterialTheme.typography.caption)
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Column(Modifier.weight(1f).fillMaxHeight()){Text("来源 · $id");AssetImage(c,id,Modifier.fillMaxSize())}
            Column(Modifier.weight(1f).fillMaxHeight()){
                StudioSwitch("播放处理结果",animated){animated=!animated}
                a.preview?.let{bytes->if(animated)DesktopOwnedImage(sha256(bytes),{bytes},Modifier.fillMaxSize())else cover?.let{Image(it,"处理结果静态封面",Modifier.fillMaxSize())}}
            }
        }
        StudioActions{StudioAction("上一张",!c.busy&&a.index>0){a.page(-1)};StudioAction("下一张",!c.busy&&a.index<ids.lastIndex){a.page(1)};StudioAction("预览当前处理",!c.busy){a.process()};StudioAction("保存当前副本到图库",!c.busy&&a.preview!=null){a.saveCopy()}}
        StudioActions{StudioAction("选择目录并导出全部",!c.busy){a.export()};StudioAction("复制全部处理文件",!c.busy){a.export(true)}}
        Text("批次已写 ${a.progress} / ${ids.size}；完整目录与完成清单发布后才成功。失败.incomplete目录保留，不覆盖旧输出。复制使用文件列表保留动画。",style=MaterialTheme.typography.caption)
        a.error?.let{Text(it,color=MaterialTheme.colors.error)};a.completed?.let{Text("完成文件目录：$it",style=MaterialTheme.typography.caption)}
    }
}
