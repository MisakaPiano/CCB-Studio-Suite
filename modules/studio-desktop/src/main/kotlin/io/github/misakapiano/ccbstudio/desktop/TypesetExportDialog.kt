package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import com.example.chatbar.desktop.*

@Composable internal fun TypesetExportDialog(c:WorkbenchController,e:TypesetEditorController,dismiss:()->Unit) {
    val document=e.document?:return;val book=document.mode=="storybook"
    val prefs=c.state.typesetPreferences
    var range by remember{mutableStateOf("all")};var variant by remember{mutableStateOf("all")};var format by remember{mutableStateOf(prefs.exportFormat)}
    var variants by remember{mutableStateOf(prefs.exportVariants.toSet()+if(e.editorExtras.originalSorted)setOf("plain")else emptySet())};var metadata by remember{mutableStateOf(if(prefs.preserveSourceMetadata)"preserve"else"strip")};var recipe by remember{mutableStateOf(prefs.exportRecipe)}
    LaunchedEffect(e.editorExtras.originalSorted){variants=if(e.editorExtras.originalSorted)variants+"plain"else variants-"plain"}
    val orderedVariants=if(book)listOf("all")else listOf("plain","mask","all","text","original").filter{it in variants}
    fun variantName(key:String)=when(key){"plain"->"01_原始排序（按所选格式编码）";"all"->"03_打码并嵌字";"text"->"04_仅嵌字";"mask"->"02_仅打码";else->"另存原图副本（原字节）"}
    var prefix by remember{mutableStateOf("")};val prefixValid=TypesetExport.validPrefix(prefix)
    var zip by remember{mutableStateOf(book || prefs.exportZip)};var pdf by remember{mutableStateOf(false)};var previewId by remember{mutableStateOf(e.pageId)}
    val pages=document.pages.filter{if(book&&pdf)true else when(range){"selected"->it.id in e.selectedPages;"current"->it.id==e.pageId;else->true}}
    val page=pages.firstOrNull{it.id==previewId}?:pages.firstOrNull()
    val previewError=remember{mutableStateOf<String?>(null)}
    fun request()=TypesetExportRequest(pages.map{it.id},orderedVariants.first(),format,zip,book && pdf,orderedVariants,metadata,recipe,categoryNames=true,filePrefix=prefix)
    val preview=produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,page,variant,book){value=null;previewError.value=null;e.previewTrace("start:${page?.id}")
        try{value=withContext(Dispatchers.IO){page?.let{p->
            val image=typesetPreviewRaster(c.workspace,p,book,document.startNumber+document.pages.indexOf(p),variant)
            try{image.toComposeImageBitmap().also{e.previewTrace("converted:${p.id}")}}finally{image.flush()}}};e.previewTrace("assigned:${page?.id}:${value!=null}")}
        catch(ex:CancellationException){e.previewTrace("cancel:${page?.id}");throw ex}
        catch(ex:Exception){e.previewTrace("error:${page?.id}:${ex.javaClass.simpleName}");previewError.value="预览失败 · ${if(ex is DataProblem)ex.status else ex.javaClass.simpleName} · ${ex.message}"}
    }
    val settings:@Composable ()->Unit={Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(if(book)"小人书完整页／整册 PDF"else"普通图片 · 可组合选择输出分类",style=MaterialTheme.typography.subtitle1)
        if(book&&pdf)Text("整册 PDF 固定包含全部 ${document.pages.size} 页；图片页同时保留。")
        else CompactChoice("导出范围",listOf("all","selected","current"),range,{when(it){"all"->"全部页";"selected"->"所选页";else->"当前页"}}){range=it}
        if(!book){listOf("plain","mask","all","text").forEach{key->StudioSwitch(variantName(key),key in variants){variants=if(key in variants)variants-key else variants+key}}
            Text("每个所选分类都输出完整目标页序列；没有文字或打码的页面也包含在内。",style=MaterialTheme.typography.caption)
            Divider()
            StudioSwitch(variantName("original"),"original" in variants){variants=if("original" in variants)variants-"original"else variants+"original"}
            CompactChoice("源元数据",listOf("preserve","strip"),metadata,{if(it=="preserve")"保留源数据（PNG）"else"隐私清除文本与像素隐写"}){metadata=it}
        }
        if(orderedVariants.any{it!="original"})CompactChoice("图片格式",listOf("png","jpeg","webp","source"),format,{if(it=="source")"原格式（不支持时PNG）"else it.uppercase()}){format=it}
        OutlinedTextField(prefix,{prefix=it.take(80)},label={Text("图片文件名前缀（可留空）")},isError=!prefixValid,singleLine=true,modifier=Modifier.fillMaxWidth())
        if(!prefixValid)Text("只支持文字、数字、空格、下划线、点和短横线，不能以空格或点结尾。",color=MaterialTheme.colors.error)
        StudioSwitch("附带可编辑 Recipe 文件",recipe){recipe=!recipe}
        StudioSwitch("同时打包 ZIP",zip){zip=!zip}
        if(book)StudioSwitch("同时生成整册 PDF",pdf){pdf=!pdf}
        if(book && pdf)Text("PDF 2.0按实际页面高度输出；超长页需支持大页面的阅读器，不截断或缩小旁白。")
        Text("共 ${pages.size} 页，按队列顺序及编号输出。确认后选择位置，每次新建独立文件夹。")
        Text(if(book)"图片写入“小人书完整页/”，PDF 为 book.pdf。"else"各版本写入同名分类目录；原图副本单独保存。",style=MaterialTheme.typography.caption)
        if(metadata=="strip" && ("original" in orderedVariants || recipe))Text("原始文件和 Recipe 附件保留来源信息，不属于隐私清除范围。")
        StudioDisclosure("格式与文件说明") {
        Text("每个所选分类都包含全部目标页，包括没有文字或打码的页面。JPEG 使用白色背景；JPEG/WebP不携带源元数据。原格式仅保留PNG/JPEG/WebP编码，GIF/BMP编辑结果转PNG。")
        Text(if(book)"小人书为合成页面，不携带源元数据。"else if(metadata=="preserve")"PNG 保留源文本块和 alpha 隐写；它们描述原图，原签名不能证明编辑后的图片。"else"隐私清除会重写像素低位并移除源元数据，像素值可能轻微变化。")
        if("original" in orderedVariants)Text("原始文件分类始终保留所有原始字节和元数据，不执行隐私清除。")
        if(recipe)Text("Recipe 附件包含原图关联信息、编辑内容及源 Recipe，不属于隐私清除范围。")
        Text("GIF、动态WebP仅编辑首帧，APNG使用默认静态图；动画只在原始文件分类中按原字节保留。")
        Text("复制会把所有所选输出文件放入剪贴板，可粘贴到资源管理器；单张静态图片还可粘贴到图像应用。")
        }
        e.exportDirectory?.let{Text("最近完成位置：$it")}
        StudioDisclosure("工程／预设备份") {
            Text("工程交换文件含原图与编辑内容；预设文件只保存配置，均不会改写原图。")
            StudioAction("导出当前 TypeSet 工程",!c.busy){dismiss();e.exportExchange()}
            StudioAction("导出文字与打码预设",!c.busy){dismiss();e.exportPresets()}
        }
    }}
    TypesetPreviewDialog(if(book)"整册预览与导出"else"批量预览与导出",dismiss,content={
        BoxWithConstraints(Modifier.fillMaxSize()){
            if(maxWidth>=700.dp)Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(20.dp)){
                Box(Modifier.weight(1f).fillMaxHeight()){TypesetExportPreviewPane(document,pages,page,book,variant,{variant=it},{previewId=it},preview,previewError)}
                Box(Modifier.width(330.dp).fillMaxHeight()){settings()}
            }else Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Box(Modifier.fillMaxWidth().weight(1f)){TypesetExportPreviewPane(document,pages,page,book,variant,{variant=it},{previewId=it},preview,previewError)}
                Box(Modifier.fillMaxWidth().weight(1f)){settings()}
            }
        }
    },actions={
        TextButton(onClick=dismiss){Text("返回编辑")}
        val ready=pages.isNotEmpty() && orderedVariants.isNotEmpty() && prefixValid && preview.value!=null && !c.busy
        TextButton(enabled=ready,onClick={e.export(request(),true);dismiss()}){Text("复制所选输出文件")}
        TextButton(enabled=ready,onClick={e.export(request());dismiss()}){Text("选择位置并导出")}
    })
}

@Composable private fun TypesetExportPreviewPane(
    document:TypesetDocument,pages:List<TypesetPage>,page:TypesetPage?,book:Boolean,variant:String,
    selectVariant:(String)->Unit,selectPage:(String)->Unit,
    preview:State<androidx.compose.ui.graphics.ImageBitmap?>,error:State<String?>
) {
    fun variantName(key:String)=when(key){"all"->"打码并嵌字";"text"->"仅嵌字";"mask"->"仅打码";"plain"->"原始排序（无编辑）";else->"原图副本"}
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(pages.isNotEmpty())CompactChoice("预览页",pages,page,{TypesetQueue.number(document,document.pages.indexOf(it))+" · "+it.name}){selectPage(it.id)}
            if(!book)CompactChoice("预览分类",listOf("all","text","mask","plain","original"),variant,::variantName){selectVariant(it)}
            Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=androidx.compose.ui.Alignment.Center){
                preview.value?.let{bitmap->val index=pages.indexOf(page);TypesetImageViewport(bitmap,(page?.id to variant),"导出页面预览",Modifier.fillMaxSize(),
                    previous=if(index>0)({selectPage(pages[index-1].id)})else null,next=if(index>=0&&index<pages.lastIndex)({selectPage(pages[index+1].id)})else null)}?:Text(error.value?:"预览未就绪；检查页面与字体。")
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                val index=pages.indexOf(page)
                TextButton(enabled=index>0,onClick={selectPage(pages[index-1].id)}){Text("上一张预览")}
                Text("${if(index<0)0 else index+1} / ${pages.size}")
                TextButton(enabled=index>=0 && index<pages.lastIndex,onClick={selectPage(pages[index+1].id)}){Text("下一张预览")}
            }
        }
    }
