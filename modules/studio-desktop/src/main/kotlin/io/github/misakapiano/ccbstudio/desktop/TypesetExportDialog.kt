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
    var zip by remember{mutableStateOf(book)};var pdf by remember{mutableStateOf(book)};var previewId by remember{mutableStateOf(e.pageId)}
    val pages=document.pages.filter{when(range){"selected"->it.id in e.selectedPages;"current"->it.id==e.pageId;else->true}}
    val page=pages.firstOrNull{it.id==previewId}?:pages.firstOrNull()
    val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,page,variant,book){value=null;value=withContext(Dispatchers.IO){page?.let{p->
        runCatching{val bytes=c.workspace.bytes(p.assetId);val image=if(book)TypesetRenderer.story(bytes,p,document.startNumber+document.pages.indexOf(p))else TypesetRenderer.render(bytes,p,variant)
            try{image.toComposeImageBitmap()}finally{image.flush()}}.getOrNull()}}}
    AlertDialog(onDismissRequest=dismiss,title={Text(if(book)"整册预览与导出"else"批量预览与导出")},text={Column(Modifier.width(620.dp).heightIn(max=650.dp).verticalScroll(rememberScrollState())){
        CompactChoice("导出范围",listOf("all","selected","current"),range,{when(it){"all"->"全部页";"selected"->"所选页";else->"当前页"}}){range=it}
        if(!book)CompactChoice("输出内容",listOf("all","text","mask","original"),variant,{when(it){"all"->"文字与打码";"text"->"仅文字";"mask"->"仅打码";else->"原始文件（字节保全）"}}){variant=it}
        if(variant!="original")CompactChoice("图片格式",listOf("png","jpeg"),format,{it.uppercase()}){format=it}
        StudioSwitch("同时打包 ZIP",zip){zip=!zip}
        if(book)StudioSwitch("同时生成整册 PDF",pdf){pdf=!pdf}
        Text("共 ${pages.size} 页，按队列顺序及编号输出。确认后选择位置，每次新建独立文件夹。")
        Text(if(variant=="original")"直接复制原始字节，包含原有元数据。"else"渲染图片不携带原图元数据；JPEG 使用白色背景。工程和原件保留在资料库。")
        if(pages.isNotEmpty())CompactChoice("预览页",pages,page,{TypesetQueue.number(document,document.pages.indexOf(it))+" · "+it.name}){previewId=it.id}
        preview?.let{Image(it,"导出页面预览",Modifier.fillMaxWidth().height(260.dp))}?:Text("预览未就绪；检查页面与字体。")
        e.exportDirectory?.let{Text("最近完成位置：$it")}
    }},confirmButton={TextButton(enabled=pages.isNotEmpty() && preview!=null && !c.busy,onClick={e.export(TypesetExportRequest(pages.map{it.id},variant,format,zip,book && pdf));dismiss()}){Text("选择位置并导出")}},dismissButton={TextButton(onClick=dismiss){Text("返回编辑")}})
}
