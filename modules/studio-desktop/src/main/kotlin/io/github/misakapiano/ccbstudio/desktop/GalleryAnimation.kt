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
internal data class AnimationOutputSnapshot(val epoch:Long,val source:String,val mode:String,val bytes:ByteArray,val cover:AnimationCoverSnapshot?)
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
    var coverManagerOpen by mutableStateOf(false);private set
    fun openCoverManager(){if(!c.maintenance)coverManagerOpen=true}
    fun closeCoverManager(){coverManagerOpen=false}
    var playResult by mutableStateOf(false);private set
    var previewBusy by mutableStateOf(false);private set
    private var previewTask:Job?=null
    private val previewJobs=java.util.concurrent.ConcurrentHashMap.newKeySet<Job>()
    private val previewEpoch=java.util.concurrent.atomic.AtomicLong()
    private var previewSource:String?=null
    private var readyOutput:AnimationOutputSnapshot?=null
    val outputGeneration get()=previewEpoch.get()
    val outputReady get()=readyOutput?.let{it.epoch==previewEpoch.get()&&it.source==ids?.getOrNull(index)&&it.mode==mode}==true&&!previewBusy
    internal var previewTransform:suspend (ByteArray,String,ByteArray?)->ByteArray=::animationTransform
    var chooseCoverFile:()->Path?={StudioFileDialogs.open("选择静态伪装封面",listOf("png","jpg","jpeg","webp","bmp"))}
    var queueSelection by mutableStateOf<Set<String>>(emptySet());private set
    var itemStatus by mutableStateOf<Map<String,String>>(emptyMap());private set
    private var queueAnchor:String?=null
    fun toggleQueue(id:String,range:Boolean=false){
        if(c.busy||c.maintenance||id !in ids.orEmpty())return
        val values=ids.orEmpty();val anchor=values.indexOf(queueAnchor);val at=values.indexOf(id)
        queueSelection=if(range&&anchor>=0)queueSelection+values.subList(minOf(anchor,at),maxOf(anchor,at)+1)else if(id in queueSelection)queueSelection-id else queueSelection+id
        queueAnchor=id
    }
    fun selectQueue(all:Boolean){if(!c.busy&&!c.maintenance)queueSelection=if(all)ids.orEmpty().toSet()else emptySet()}
    fun removeQueue(selected:Set<String> = queueSelection){
        if(c.busy||c.maintenance||selected.isEmpty())return
        val current=ids?.getOrNull(index);val remaining=ids.orEmpty().filterNot{it in selected}
        ids=remaining;queueSelection=queueSelection-selected;itemStatus=itemStatus-selected
        index=remaining.indexOf(current).takeIf{it>=0}?:index.coerceAtMost(remaining.lastIndex).coerceAtLeast(0)
        invalidate();requestPreview()
    }
    var ids by mutableStateOf<List<String>?>(null)
    var mode by mutableStateOf("disguise");private set
    var index by mutableStateOf(0);private set
    var preview by mutableStateOf<ByteArray?>(null);private set
    var error by mutableStateOf<String?>(null);private set
    var completed by mutableStateOf<Path?>(null);private set
    var progress by mutableStateOf(0);private set
    var progressTotal by mutableStateOf(0);private set
    var copyFiles:(List<Path>)->Unit=ExportClipboard::copy
    var chooseDirectory:()->Path?={StudioFileDialogs.folder("选择APNG输出目录（新建独立文件夹）")}
    init {reloadCovers()}
    fun reloadCovers(){runCatching{covers.load()}.onSuccess{coverState=it;coverError=null}.onFailure{coverState=null;coverError="封面配置读取失败，暂停伪装处理；原配置和原件保留"}}
    private fun invalidate(){previewSource?.takeIf{itemStatus[it]=="正在预览"}?.let{itemStatus=itemStatus+(it to "等待预览")};previewEpoch.incrementAndGet();previewTask?.cancel();preview=null;previewBusy=false;readyOutput=null;error=null;completed=null;progress=0;progressTotal=0;playResult=false}
    fun open(selected:List<String>){require(selected.size<=1000);ids=selected.distinct();queueSelection=emptySet();itemStatus=emptyMap();index=ids.orEmpty().lastIndex.coerceAtLeast(0);invalidate();requestPreview()}
    @Synchronized fun close(){ids=null;coverManagerOpen=false;invalidate();previewJobs.forEach{it.cancel()};previewBusy=false}
    suspend fun cancelAndDrain(){val jobs=synchronized(this){previewEpoch.incrementAndGet();previewJobs.toList().also{tasks->tasks.forEach{it.cancel()}}};jobs.joinAll();previewBusy=false}
    suspend fun awaitPreview(){previewTask?.join()}
    fun cancelPreview(){previewTask?.cancel()}
    fun selectMode(value:String){check(!c.busy);itemStatus=emptyMap();require(value in setOf("disguise","restore"));mode=value;invalidate();requestPreview()}
    fun selectIndex(value:Int){if(c.busy||c.maintenance||ids.isNullOrEmpty())return;val next=value.coerceIn(0,ids.orEmpty().lastIndex);if(next==index)return;index=next;invalidate();requestPreview()}
    fun page(delta:Int)=selectIndex(index+delta)
    fun playback(value:Boolean){playResult=value}
    var chooseInputFiles:()->List<Path> = {StudioFileDialogs.openMany("添加APNG处理图片",listOf("png","apng","jpg","jpeg","webp","gif","bmp"))}
    var chooseInputDirectory:()->Path?={StudioFileDialogs.folder("添加目录本层图片（不含子目录）")}
    var chooseOutputFile:suspend ()->Path?={c.picker.pickSaveFile(DesktopFileType("当前处理结果",listOf("png")),"$mode-result.png")}
    fun addFiles(){if(!c.busy&&!c.maintenance)chooseInputFiles().takeIf{it.isNotEmpty()}?.let{intake(GalleryIngress(files=it,channel="apng-file-dialog"))}}
    fun addDirectory(){if(!c.busy&&!c.maintenance)chooseInputDirectory()?.let{intake(GalleryIngress(files=listOf(it),channel="apng-directory-dialog"))}}
    fun paste():Boolean=try{if(c.busy||c.maintenance)false else c.gallery.pasteContents()?.let{receive(it,true)}?:false}catch(e:Exception){c.problem="剪贴板读取失败；原件和批次保留";true}
    fun receive(transfer:java.awt.datatransfer.Transferable,paste:Boolean):Boolean=try {
        if(c.busy||c.maintenance)false else galleryTransfer(transfer,paste)?.let{intake(it);true}?:false
    }catch(e:Exception){c.problem="图片传入失败，原件和批次保留";true}
    fun intake(input:GalleryIngress){
        if(c.busy||c.maintenance)return
        val previous=ids?:return
        c.importIngress(input,expandDirectories=true,maxImages=1000-previous.size){added->
            val combined=(previous+added).distinct();require(combined.size<=1000){"批次最多1000张，已导入原件保留在图库"}
            ids=combined;index=combined.lastIndex;invalidate();requestPreview()
        }
    }
    fun removeCurrent(){ids?.getOrNull(index)?.let{removeQueue(setOf(it))}}
    fun selectCover(id:String?)=c.run("选择伪装封面"){covers.select(id);reloadCovers();itemStatus=emptyMap();invalidate();requestPreview()}
    fun removeCover(id:String)=c.run("移除伪装封面配置"){covers.remove(id);reloadCovers();itemStatus=emptyMap();invalidate();requestPreview()}
    fun renameCover(id:String,name:String)=c.run("重命名伪装封面"){covers.rename(id,name);reloadCovers()}
    fun addCover(name:String,choose:()->Path?=chooseCoverFile) {
        if(c.busy||c.maintenance)return
        val path=choose()?:return
        c.run("保存伪装封面"){
            val bytes=readBounded(path,DesktopImageEditing.MAX_BYTES)
            DesktopImageEditing.requireStatic(bytes)
            if(ImageFormats.format(bytes)=="webp")org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->org.jetbrains.skia.Codec.makeFromData(data).use{require(it.frameCount<=1){"封面需要静态图片"}}}
            DesktopImageEditing.decode(bytes).flush()
            covers.add(name.ifBlank{path.fileName.toString().substringBeforeLast('.')}.take(100),path.fileName.toString(),bytes)
            reloadCovers();itemStatus=emptyMap();invalidate();requestPreview()
        }
    }
    private fun selectedCover():AnimationCoverSnapshot? {
        val state=requireNotNull(coverState){"封面配置暂不可用"};val selected=state.covers.firstOrNull{it.id==state.selected}?:return null
        val bytes=c.workspace.bytes(selected.id);require(sha256(bytes)==selected.sha256){"封面校验失败，原件保留"}
        return AnimationCoverSnapshot(selected,bytes)
    }
    @Synchronized fun requestPreview() {
        if(c.maintenance)return
        val source=ids?.getOrNull(index)?:return
        val selectedMode=mode;val epoch=previewEpoch.incrementAndGet();previewTask?.cancel()
        previewSource=source;itemStatus=itemStatus+(source to "正在预览");preview=null;readyOutput=null;error=null;previewBusy=true;playResult=false
        val task=c.scope.launch(start=CoroutineStart.UNDISPATCHED) {
            try {
                delay(120)
                val output=withContext(Dispatchers.IO){
                    val input=c.workspace.operation{c.workspace.bytes(source) to if(selectedMode=="disguise")selectedCover() else null}
                    AnimationOutputSnapshot(epoch,source,selectedMode,previewTransform(input.first,selectedMode,input.second?.bytes),input.second)
                }
                ensureActive()
                if(previewEpoch.get()==epoch){itemStatus=itemStatus+(source to "已预览");readyOutput=output;preview=output.bytes;playResult=false}
            }catch(e:CancellationException){if(previewEpoch.get()==epoch){itemStatus=itemStatus+(source to "已停止");error="预览已停止，可重新预览"};throw e}
            catch(e:Exception){if(previewEpoch.get()==epoch){itemStatus=itemStatus+(source to "预览失败");error="预览未完成 · ${e.javaClass.simpleName}；原件未变，可重新预览"}}
            finally{previewJobs.remove(currentCoroutineContext().job);if(previewEpoch.get()==epoch)previewBusy=false}
        }
        previewTask=task;previewJobs.add(task);task.invokeOnCompletion{previewJobs.remove(task)}
    }
    private fun outputSnapshot():AnimationOutputSnapshot?=readyOutput?.takeIf{outputReady&&!c.busy&&!c.maintenance}
    fun saveCopy(){val output=outputSnapshot()?:return
      c.run("保存APNG工具副本到图库"){
        check(output.epoch==previewEpoch.get());val bytes=output.bytes;val source=output.source
        c.workspace.beginTask().use{
            val cover=output.cover
            val receipt=buildJsonObject{put("kind","animation-copy");put("mode",output.mode);put("sourceId",source);if(output.mode=="disguise"){put("cover",cover?.preset?.id?:"CCB_DEFAULT");cover?.let{put("coverSha256",it.preset.sha256)}}}.toString()
            val id=c.workspace.importDerived("${output.mode}-copy.png",bytes,source,receipt)
            c.workspace.operation{c.workspace.save(c.workspace.state.copy(savedCopies=c.workspace.state.savedCopies+id))};c.current=id;c.selection=setOf(id)
        }
      }
    }
    /** File-list clipboard keeps APNG bytes. There is deliberately no original/raster fallback. */
    fun currentFile(copy:Boolean){
        val output=outputSnapshot()?:return
        c.run(if(copy)"复制当前处理文件"else"另存当前处理文件"){
            val destination=if(copy)null else chooseOutputFile()?:return@run
            if(destination!=null)require(!destination.toAbsolutePath().normalize().startsWith(c.workspace.root.toAbsolutePath().normalize())){"不能向资料根另存处理文件"}
            check(output.epoch==previewEpoch.get()){"处理结果已改变，请重新选择"}
            c.workspace.beginTask().use {
                if(copy){Files.createDirectories(temp);val file=temp.resolve("${output.mode}-${newId()}.png");writeNewForced(file,output.bytes);copyFiles(listOf(file))}
                else DesktopExternalFileWriter.writeBytes(requireNotNull(destination),output.bytes)
            }
        }
    }
    fun export(copy:Boolean=false,selectedOnly:Boolean=false){
        if(c.busy||c.maintenance)return
        val batch=ids.orEmpty().filter{!selectedOnly||it in queueSelection}.toList()
        if(batch.isEmpty())return
        val destination=try{if(copy)Files.createDirectories(temp)else chooseDirectory()?:return}catch(e:Exception){c.problem="输出目录无法使用，未写入文件";return}
        c.run(if(copy)"复制APNG文件批次"else"导出APNG文件批次"){
            completed=null;progress=0;progressTotal=batch.size;error=null
            try{c.workspace.beginTask().use{
                val output=exportAnimationDeck(c,batch,mode,destination,{progress=it},cover=if(mode=="disguise")selectedCover()else null);completed=output
                itemStatus=itemStatus+batch.associateWith{"已导出"}
                if(copy)copyFiles(Files.list(output).use{stream->stream.filter{it.fileName.toString().endsWith(".png")}.sorted().toList()})
            }}catch(e:CancellationException){throw e}catch(e:Exception){error="未完成 · ${e.javaClass.simpleName}；已有输出保留，未自动重试";throw e}
        }
    }
}
