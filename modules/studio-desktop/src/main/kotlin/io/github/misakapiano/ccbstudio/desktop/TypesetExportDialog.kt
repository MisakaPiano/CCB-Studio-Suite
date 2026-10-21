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
    var range by remember{mutableStateOf("all")};var variant by remember{mutableStateOf("all")};var format by remember{mutableStateOf("png")}
    var variants by remember{mutableStateOf(setOf("all"))};var metadata by remember{mutableStateOf("preserve")};var recipe by remember{mutableStateOf(false)}
    val orderedVariants=if(book)listOf("all")else listOf("original","mask","all","text").filter{it in variants}
    fun variantName(key:String)=when(key){"all"->"文字与打码";"text"->"仅文字";"mask"->"仅打码";else->"原始文件（字节保全）"}
    var zip by remember{mutableStateOf(book)};var pdf by remember{mutableStateOf(book)};var previewId by remember{mutableStateOf(e.pageId)}
    val pages=document.pages.filter{when(range){"selected"->it.id in e.selectedPages;"current"->it.id==e.pageId;else->true}}
    val page=pages.firstOrNull{it.id==previewId}?:pages.firstOrNull()
    fun request()=TypesetExportRequest(pages.map{it.id},orderedVariants.first(),format,zip,book && pdf,orderedVariants,metadata,recipe)
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,page,variant,book){value=null;value=withContext(Dispatchers.IO){page?.let{p->
        runCatching{val bytes=c.workspace.bytes(p.assetId);val image=if(book)TypesetRenderer.story(bytes,p,document.startNumber+document.pages.indexOf(p))else TypesetRenderer.render(bytes,p,variant)
            try{image.toComposeImageBitmap()}finally{image.flush()}}.getOrNull()}}}
    AlertDialog(onDismissRequest=dismiss,title={Text(if(book)"整册预览与导出"else"批量预览与导出")},text={Column(Modifier.width(620.dp).heightIn(max=650.dp).verticalScroll(rememberScrollState())){
        CompactChoice("导出范围",listOf("all","selected","current"),range,{when(it){"all"->"全部页";"selected"->"所选页";else->"当前页"}}){range=it}
        if(!book){listOf("original","mask","all","text").forEach{key->StudioSwitch("导出 · ${variantName(key)}",key in variants){variants=if(key in variants)variants-key else variants+key}}
            CompactChoice("预览分类",listOf("all","text","mask","original"),variant,::variantName){variant=it}
            CompactChoice("源元数据",listOf("preserve","strip"),metadata,{if(it=="preserve")"保留源数据（PNG）"else"隐私清除文本与像素隐写"}){metadata=it}
        }
        if(orderedVariants.any{it!="original"})CompactChoice("图片格式",listOf("png","jpeg"),format,{it.uppercase()}){format=it}
        StudioSwitch("附带可编辑 Recipe 文件",recipe){recipe=!recipe}
        StudioSwitch("同时打包 ZIP",zip){zip=!zip}
        if(book)StudioSwitch("同时生成整册 PDF",pdf){pdf=!pdf}
        Text("共 ${pages.size} 页，按队列顺序及编号输出。确认后选择位置，每次新建独立文件夹。")
        Text("每个所选分类都包含全部目标页，包括没有文字或打码的页面。JPEG 使用白色背景且丢弃源元数据。")
        Text(if(book)"小人书为合成页面，不携带源元数据。"else if(metadata=="preserve")"PNG 保留源文本块和 alpha 隐写；它们描述原图，原签名不能证明编辑后的图片。"else"隐私清除会重写像素低位并移除源元数据，像素值可能轻微变化。")
        if("original" in orderedVariants)Text("原始文件分类始终保留所有原始字节和元数据，不执行隐私清除。")
        if(recipe)Text("Recipe 附件包含原图关联信息、编辑内容及源 Recipe，不属于隐私清除范围。")
        Text("复制会把所有所选输出文件放入剪贴板，可粘贴到资源管理器；单张静态图片还可粘贴到图像应用。")
        if(pages.isNotEmpty())CompactChoice("预览页",pages,page,{TypesetQueue.number(document,document.pages.indexOf(it))+" · "+it.name}){previewId=it.id}
        preview?.let{Image(it,"导出页面预览",Modifier.fillMaxWidth().height(260.dp))}?:Text("预览未就绪；检查页面与字体。")
        e.exportDirectory?.let{Text("最近完成位置：$it")}
    }},confirmButton={Row{
        val ready=pages.isNotEmpty() && orderedVariants.isNotEmpty() && preview!=null && !c.busy
        TextButton(enabled=ready,onClick={e.export(request(),true);dismiss()}){Text("复制所选输出文件")}
        TextButton(enabled=ready,onClick={e.export(request());dismiss()}){Text("选择位置并导出")}
    }},dismissButton={TextButton(onClick=dismiss){Text("返回编辑")}})
}
