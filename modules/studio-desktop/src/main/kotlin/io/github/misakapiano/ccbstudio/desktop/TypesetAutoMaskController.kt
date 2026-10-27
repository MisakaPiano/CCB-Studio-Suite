package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.runtime.*
import com.example.chatbar.desktop.StudioFileDialogs
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlin.coroutines.ContinuationInterceptor

internal data class AutoMaskReview(val projectId:String,val document:TypesetDocument,val modelSha256:String,val results:Map<String,List<AutoMaskRegion>>,val style:TypesetMask,val options:AutoMaskOptions)

/** Review first; applying is a single recipe change and never writes source images. */
internal class TypesetAutoMaskController(private val c:WorkbenchController,private val e:TypesetEditorController){
    private val ui=c.scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
    var model by mutableStateOf<LocalMaskModel?>(null);private set
    var classes by mutableStateOf(emptySet<String>())
    var options by mutableStateOf(AutoMaskOptions())
    var selectedPages by mutableStateOf(false)
    var effect by mutableStateOf("mosaic")
    var strength by mutableStateOf(16)
    var opacity by mutableStateOf(1.0)
    var followTool by mutableStateOf(true)
    var configured by mutableStateOf(false);private set
    var review by mutableStateOf<AutoMaskReview?>(null);private set
    var selectedRegions by mutableStateOf(emptySet<String>())
    var progress by mutableStateOf("");private set
    var chooseModel:suspend ()->Pair<String,ByteArray>?={
        val path=withContext(ui){StudioFileDialogs.open("选择本地 YOLO ONNX 模型",listOf("onnx"))}
        path?.let{it.fileName.toString() to readBounded(it,128*1024*1024)}
    }
    fun loadModel()=c.run("读取本地识别模型"){
        val input=chooseModel()?:return@run;val next=LocalOnnxMaskDetector.inspect(input.first,input.second)
        currentCoroutineContext().ensureActive()
        withContext(ui){model=next;classes=next.classes.filterNot{it.contains("face",true)}.toSet();review=null;selectedRegions=emptySet();progress="本地模型已就绪 · CPU识别"}
    }
    private suspend fun restoreSaved():TypesetAutoMaskSettings? {
        val saved=TypesetEditorExtrasStore.load(c.workspace).autoMask?:return null
        val loaded=if(model?.sha256==saved.modelSha256)model!!else {
            val input=TypesetAutoMaskResources.read(c.workspace,saved)
            LocalOnnxMaskDetector.inspect(input.first,input.second)
        }
        require(saved.classes.all{it in loaded.classes}){"模型类别不一致，请重新设置"}
        withContext(ui){model=loaded;classes=saved.classes.toSet();options=AutoMaskOptions(saved.confidence.toFloat(),saved.iou.toFloat(),saved.resolution,saved.expansion);effect=saved.effect;strength=saved.strength;opacity=saved.opacity;selectedPages=saved.selectedPages;followTool=saved.followTool;configured=true}
        return saved
    }
    fun readSettings()=c.run("读取 AI 打码配置"){restoreSaved()}
    fun saveConfiguration(onSaved:()->Unit={})=c.run("保存 AI 打码配置") {
        val chosen=model?:error("请先选择本地模型");require(classes.isNotEmpty()&&classes.all{it in chosen.classes});options.validate()
        val id=TypesetAutoMaskResources.retain(c.workspace,chosen.name,chosen.bytes)
        val saved=TypesetAutoMaskSettings(id,chosen.sha256,classes.sorted(),options.confidence.toString().toDouble(),options.iou.toString().toDouble(),options.resolution,options.expansion,effect,strength,opacity,selectedPages,followTool)
        saved.validate();val extras=TypesetEditorExtrasStore.load(c.workspace)
        TypesetEditorExtrasStore.save(c.workspace,extras.copy(autoMask=saved))
        withContext(ui){configured=true;progress="配置已保存，重开后可直接自动打码";onSaved()}
    }
    fun runConfigured(needsSettings:()->Unit={})=c.run("AI 自动打码") {
        val saved=restoreSaved()
        if(saved==null){withContext(ui){progress="请先保存一次 AI 打码配置";c.operationNotice=progress;needsSettings()};return@run}
        val style=withContext(ui){if(saved.followTool)e.mask?:e.maskDefault else TypesetMask(effect=saved.effect,strength=saved.strength,opacity=saved.opacity)}
        val snapshot=inspectSnapshot(style)
        var added=0
        val next=snapshot.document.copy(pages=snapshot.document.pages.map{page->
            val masks=page.masks.toMutableList();val ids=TypesetObjects.order(page).toMutableList();var pageAdded=false
            snapshot.results[page.id].orEmpty().forEach{region->
                val mask=LocalOnnxMaskDetector.mask(region,snapshot.style)
                if(masks.none{it.copy(id="")==mask.copy(id="")}){masks+=mask;ids+=mask.id;added++;pageAdded=true}
            }
            if(pageAdded)page.copy(masks=masks,layerOrder=ids)else page
        })
        currentCoroutineContext().ensureActive()
        if(added>0)e.commitAutoMasks(snapshot.projectId,snapshot.document,next)
        withContext(ui){review=null;selectedRegions=emptySet();progress=if(added>0)"自动打码完成 · 新增 $added 个区域，可撤销"else if(snapshot.results.values.all{it.isEmpty()})"识别完成 · 未检测到所选类别"else"识别完成 · 相同打码已存在，未重复添加";c.operationNotice=progress}
    }
    fun detect()=c.run("本地 AI 区域识别") {
        val snapshot=inspectSnapshot(e.maskDefault.copy(effect=effect))
        withContext(ui){review=snapshot;selectedRegions=snapshot.results.values.flatten().map{it.id}.toSet();progress="识别完成 · ${selectedRegions.size} 个区域，检查后再应用"}
    }
    private suspend fun inspectSnapshot(style:TypesetMask):AutoMaskReview {
        e.drain();val source=e.document?:error("尚无编辑工程");val project=e.project?:error("尚无编辑工程");val chosen=model?:error("请先选择本地模型")
        val ids=if(selectedPages)e.selectedPages.toSet()else setOfNotNull(e.pageId);require(ids.isNotEmpty()&&ids.size<=100){"每次请选择1至100页"}
        val opts=options;opts.validate();val enabled=classes.toSet();require(enabled.isNotEmpty())
        val pages=source.pages.filter{it.id in ids};require(pages.size==ids.size)
        val results=linkedMapOf<String,List<AutoMaskRegion>>();val context=currentCoroutineContext()
        withContext(ui){review=null;selectedRegions=emptySet()}
        LocalOnnxMaskDetector.open(chosen).use{detector->for((index,page)in pages.withIndex()){
            context.ensureActive();withContext(ui){progress="正在识别 ${index+1} / ${pages.size} · ${page.name}";c.message=progress}
            val image=TypesetImage.decode(c.workspace.bytes(page.assetId),page.coordinates)
            try{results[page.id]=detector.detect(image,opts,enabled){context.ensureActive()}}finally{image.flush()}
        }}
        context.ensureActive();return AutoMaskReview(project.id,source,chosen.sha256,results,style,opts)
    }
    fun apply():Boolean {
        if(c.busy||c.maintenance)return false
        val r=review?:return false
        if(e.project?.id!=r.projectId||e.document!=r.document){c.problem="工程已改变，请重新识别；未应用旧检测结果";return false}
        val selected=selectedRegions.toSet();if(selected.isEmpty())return false
        val updated=r.document.copy(pages=r.document.pages.map{page->
            val masks=r.results[page.id].orEmpty().filter{it.id in selected}.map{LocalOnnxMaskDetector.mask(it,r.style)}
            page.copy(masks=page.masks+masks,layerOrder=TypesetObjects.order(page)+masks.map{it.id})
        })
        try{updated.validate()}catch(ex:Exception){c.problem="检测区域超过当前工程容量，未应用；请减少所选区域";return false}
        e.change{updated};review=null;selectedRegions=emptySet();progress="已应用，可撤销；原图保留";return true
    }
}
