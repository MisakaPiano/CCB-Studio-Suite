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
    var variants by remember{mutableStateOf(prefs.exportVariants.toSet())};var metadata by remember{mutableStateOf(if(prefs.preserveSourceMetadata)"preserve"else"strip")};var recipe by remember{mutableStateOf(prefs.exportRecipe)}
    val orderedVariants=if(book)listOf("all")else listOf("original","mask","all","text").filter{it in variants}
    fun variantName(key:String)=when(key){"all"->"文字与打码";"text"->"仅文字";"mask"->"仅打码";else->"原始文件（字节保全）"}
    var zip by remember{mutableStateOf(book || prefs.exportZip)};var pdf by remember{mutableStateOf(book)};var previewId by remember{mutableStateOf(e.pageId)}
    val pages=document.pages.filter{when(range){"selected"->it.id in e.selectedPages;"current"->it.id==e.pageId;else->true}}
    val page=pages.firstOrNull{it.id==previewId}?:pages.firstOrNull()
    fun request()=TypesetExportRequest(pages.map{it.id},orderedVariants.first(),format,zip,book && pdf,orderedVariants,metadata,recipe)
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,page,variant,book){value=null;value=withContext(Dispatchers.IO){page?.let{p->
        runCatching{val bytes=c.workspace.bytes(p.assetId);val image=if(book)TypesetRenderer.story(bytes,p,document.startNumber+document.pages.indexOf(p))else TypesetRenderer.render(bytes,p,variant)
            try{image.toComposeImageBitmap()}finally{image.flush()}}.getOrNull()}}}
    val previewPane:@Composable ()->Unit={
        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(pages.isNotEmpty())CompactChoice("预览页",pages,page,{TypesetQueue.number(document,document.pages.indexOf(it))+" · "+it.name}){previewId=it.id}
            if(!book)CompactChoice("预览分类",listOf("all","text","mask","original"),variant,::variantName){variant=it}
            Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=androidx.compose.ui.Alignment.Center){
                preview?.let{Image(it,"导出页面预览",Modifier.fillMaxSize())}?:Text("预览未就绪；检查页面与字体。")
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                val index=pages.indexOf(page)
                TextButton(enabled=index>0,onClick={previewId=pages[index-1].id}){Text("上一张预览")}
                Text("${if(index<0)0 else index+1} / ${pages.size}")
                TextButton(enabled=index>=0 && index<pages.lastIndex,onClick={previewId=pages[index+1].id}){Text("下一张预览")}
            }
        }
    }
    val settings:@Composable ()->Unit={Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
        CompactChoice("导出范围",listOf("all","selected","current"),range,{when(it){"all"->"全部页";"selected"->"所选页";else->"当前页"}}){range=it}
        if(!book){listOf("original","mask","all","text").forEach{key->StudioSwitch("导出 · ${variantName(key)}",key in variants){variants=if(key in variants)variants-key else variants+key}}
            CompactChoice("源元数据",listOf("preserve","strip"),metadata,{if(it=="preserve")"保留源数据（PNG）"else"隐私清除文本与像素隐写"}){metadata=it}
        }
        if(orderedVariants.any{it!="original"})CompactChoice("图片格式",listOf("png","jpeg","webp","source"),format,{if(it=="source")"原格式（不支持时PNG）"else it.uppercase()}){format=it}
        StudioSwitch("附带可编辑 Recipe 文件",recipe){recipe=!recipe}
        StudioSwitch("同时打包 ZIP",zip){zip=!zip}
        if(book)StudioSwitch("同时生成整册 PDF",pdf){pdf=!pdf}
        Text("共 ${pages.size} 页，按队列顺序及编号输出。确认后选择位置，每次新建独立文件夹。")
        Text("每个所选分类都包含全部目标页，包括没有文字或打码的页面。JPEG 使用白色背景；JPEG/WebP不携带源元数据。原格式仅保留PNG/JPEG/WebP编码，GIF/BMP编辑结果转PNG。")
        Text(if(book)"小人书为合成页面，不携带源元数据。"else if(metadata=="preserve")"PNG 保留源文本块和 alpha 隐写；它们描述原图，原签名不能证明编辑后的图片。"else"隐私清除会重写像素低位并移除源元数据，像素值可能轻微变化。")
        if("original" in orderedVariants)Text("原始文件分类始终保留所有原始字节和元数据，不执行隐私清除。")
        if(recipe)Text("Recipe 附件包含原图关联信息、编辑内容及源 Recipe，不属于隐私清除范围。")
        Text("GIF、动态WebP仅编辑首帧，APNG使用默认静态图；动画只在原始文件分类中按原字节保留。")
        Text("复制会把所有所选输出文件放入剪贴板，可粘贴到资源管理器；单张静态图片还可粘贴到图像应用。")
        e.exportDirectory?.let{Text("最近完成位置：$it")}
    }}
    TypesetPreviewDialog(if(book)"整册预览与导出"else"批量预览与导出",dismiss,content={
        BoxWithConstraints(Modifier.fillMaxSize()){
            if(maxWidth>=700.dp)Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(20.dp)){
                Box(Modifier.weight(1f).fillMaxHeight()){previewPane()}
                Box(Modifier.width(330.dp).fillMaxHeight()){settings()}
            }else Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Box(Modifier.fillMaxWidth().weight(1f)){previewPane()}
                Box(Modifier.fillMaxWidth().weight(1f)){settings()}
            }
        }
    },actions={
        TextButton(onClick=dismiss){Text("返回编辑")}
        val ready=pages.isNotEmpty() && orderedVariants.isNotEmpty() && preview!=null && !c.busy
        TextButton(enabled=ready,onClick={e.export(request(),true);dismiss()}){Text("复制所选输出文件")}
        TextButton(enabled=ready,onClick={e.export(request());dismiss()}){Text("选择位置并导出")}
    })
}
