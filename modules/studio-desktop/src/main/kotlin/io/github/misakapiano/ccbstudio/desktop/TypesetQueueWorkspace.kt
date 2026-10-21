@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import java.awt.image.BufferedImage

private data class QueueMoveRequest(val projectId:String?,val order:List<String>,val start:Int,val selected:Set<String>)
private data class QueueHoverRaster(val page:TypesetPage,val mode:String,val number:Int,val bitmap:ImageBitmap,val width:Int,val height:Int)
private val queueRasterDispatcher=Dispatchers.IO.limitedParallelism(1)

/** Render one visible queue image at a time and retain only a display-sized bitmap. */
@Composable private fun queueRaster(c:WorkbenchController,page:TypesetPage,mode:String,number:Int,maxSide:Int):Pair<QueueHoverRaster?,String?> {
    var error by remember(page,mode,number,maxSide){mutableStateOf<String?>(null)}
    val raster by produceState<QueueHoverRaster?>(null,page,mode,number,maxSide){value=null
        try{value=withContext(queueRasterDispatcher){val image=typesetPreviewRaster(c.workspace,page,mode=="storybook",number)
            try{
                val scale=minOf(1.0,maxSide.toDouble()/maxOf(image.width,image.height))
                val small=BufferedImage((image.width*scale).roundToInt().coerceAtLeast(1),(image.height*scale).roundToInt().coerceAtLeast(1),BufferedImage.TYPE_INT_ARGB)
                try{val graphics=small.createGraphics();try{graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(image,0,0,small.width,small.height,null)}finally{graphics.dispose()}
                    QueueHoverRaster(page,mode,number,small.toComposeImageBitmap(),image.width,image.height)
                }finally{small.flush()}
            }finally{image.flush()}}}
        catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
    }
    return raster?.takeIf{it.page==page&&it.mode==mode&&it.number==number} to error
}

/** Queue interactions follow TypeSet 1.1; only a completed drop changes the document. */
@Composable internal fun TypesetQueue(c:WorkbenchController,e:TypesetEditorController,modifier:Modifier) {
    val document=e.document?:return
    val list=rememberLazyListState()
    val density=LocalDensity.current
    val edge=with(density){36.dp.toPx()}
    val checkboxWidth=with(density){54.dp.toPx()}
    var dragging by remember{mutableStateOf(false)}
    var dragY by remember{mutableStateOf(0f)}
    var insertion by remember{mutableStateOf<String?>(null)}
    var hoverId by remember{mutableStateOf<String?>(null)}
    var cursor by remember{mutableStateOf(Offset.Zero)}
    var moveRequest by remember{mutableStateOf<QueueMoveRequest?>(null)}
    var contextId by remember{mutableStateOf<String?>(null)}
    var contextPosition by remember{mutableStateOf(Offset.Zero)}
    fun updateInsertion(){
        insertion=(list.layoutInfo.visibleItemsInfo.firstOrNull{dragY<it.offset+it.size/2}?.key as? String)
            ?:e.document?.pages?.getOrNull((list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:-1)+1)?.id
    }
    fun requestMove(ids:Set<String>){
        val d=e.document?:return
        moveRequest=QueueMoveRequest(e.project?.id,d.pages.map{it.id},d.startNumber,ids.toSet())
        hoverId=null;contextId=null
    }
    LaunchedEffect(dragging,c.busy,c.maintenance,e.project?.id){
        if(c.busy||c.maintenance){dragging=false;insertion=null;hoverId=null;return@LaunchedEffect}
        while(dragging){
            val info=list.layoutInfo
            val distance=when {
                dragY<info.viewportStartOffset+edge->(dragY-info.viewportStartOffset-edge).coerceAtLeast(-edge)
                dragY>info.viewportEndOffset-edge->(dragY-info.viewportEndOffset+edge).coerceAtMost(edge)
                else->0f
            }
            if(distance!=0f){list.scrollBy(distance/3);updateInsertion()}
            delay(16)
        }
    }
    Column(modifier){
        LazyColumn(Modifier.weight(1f).fillMaxWidth().semantics{contentDescription="排版页面队列"}
            // The list owns capture: scrolling a dragged row offscreen must not cancel the gesture.
            .pointerInput(e.project?.id,c.busy,c.maintenance){detectDragGestures(
                onDragStart={pos->
                    if(!c.busy&&!c.maintenance&&pos.x>checkboxWidth){
                        dragging=true;hoverId=null;contextId=null;dragY=pos.y;updateInsertion()
                    }
                },onDragCancel={dragging=false;insertion=null},
                onDragEnd={if(dragging&&!c.busy&&!c.maintenance){updateInsertion();e.movePagesTo(insertion,insertion==null)};dragging=false;insertion=null},
                onDrag={change,_->if(dragging){change.consume();dragY=change.position.y;updateInsertion()}}
            )},state=list){itemsIndexed(document.pages,key={_,p->p.id}){index,page->
            var origin by remember(page.id){mutableStateOf(Offset.Zero)}
            Row(Modifier.fillMaxWidth().background(if(page.id==e.pageId)Color(0xff253c55)else Color.Transparent).padding(6.dp),verticalAlignment=Alignment.CenterVertically){
                Checkbox(page.id in e.selectedPages,{e.selectPage(page.id,extend=true)},enabled=!c.busy&&!c.maintenance)
                Row(Modifier.weight(1f).onGloballyPositioned{origin=it.positionInWindow()}
                    .semantics{contentDescription="队列页 ${index+1}";onClick{e.selectPage(page.id);true}}
                    .onPointerEvent(PointerEventType.Enter){event->if(!dragging&&!c.busy){hoverId=page.id;cursor=origin+event.changes.first().position}}
                    .onPointerEvent(PointerEventType.Move){event->if(!dragging){cursor=origin+event.changes.first().position}}
                    .onPointerEvent(PointerEventType.Exit){if(hoverId==page.id)hoverId=null}
                    .onPointerEvent(PointerEventType.Press){event->
                        if(!c.busy&&!c.maintenance){
                            hoverId=null
                            if(event.button==PointerButton.Primary){
                                if(page.id in e.selectedPages&&!event.keyboardModifiers.isCtrlPressed&&!event.keyboardModifiers.isShiftPressed){e.pageId=page.id;e.objectId=null}
                                else e.selectPage(page.id,event.keyboardModifiers.isCtrlPressed,event.keyboardModifiers.isShiftPressed)
                            }else if(event.button==PointerButton.Secondary){
                                if(page.id !in e.selectedPages)e.selectPage(page.id)
                                contextId=page.id;contextPosition=origin+event.changes.first().position
                            }
                        }
                    },verticalAlignment=Alignment.CenterVertically){
                    val (thumbnail,previewError)=queueRaster(c,page,document.mode,document.startNumber+index,document.thumbnailSize*2)
                    Box(Modifier.size(document.thumbnailSize.dp),contentAlignment=Alignment.Center){
                        if(thumbnail!=null)Image(thumbnail.bitmap,"页面 ${index+1} 编辑缩略图",Modifier.fillMaxSize())
                        else Text(if(previewError==null)"…"else"预览失败",style=MaterialTheme.typography.caption)
                    }
                    Column {if(dragging&&insertion==page.id)Text("── 插入此页之前 ──",color=Gold)
                        Text("${io.github.misakapiano.ccbstudio.core.TypesetQueue.number(document,index)} · ${page.name}",maxLines=2)
                        StatusText("${page.texts.size} 文字 · ${page.masks.size} 打码")}
                }
            }
        }}
        StudioAction("移动到指定序号",!c.busy&&!c.maintenance&&e.selectedPages.isNotEmpty()){requestMove(e.selectedPages)}
    }
    if(contextId!=null&&!dragging)Popup(popupPositionProvider=QueuePopupPosition(contextPosition,0),onDismissRequest={contextId=null},properties=PopupProperties(focusable=true)){
        Surface(elevation=8.dp){Column {DropdownMenuItem(onClick={requestMove(setOf(contextId!!))}){Text("移动此页到指定序号")}
            DropdownMenuItem(onClick={requestMove(e.selectedPages)}){Text("移动已选 ${e.selectedPages.size} 页到指定序号")}}}
    }
    val hovered=document.pages.firstOrNull{it.id==hoverId}
    if(hovered!=null&&!dragging&&contextId==null&&moveRequest==null&&!c.busy&&!c.maintenance)
        TypesetQueueHover(c,document,hovered,cursor)
    moveRequest?.let{request->
        var serial by remember(request){mutableStateOf(request.start.toString())}
        val number=serial.trim().toDoubleOrNull()?.takeIf{it.isFinite()}
        val rounded=number?.let{kotlin.math.floor(it+.5)}
        val validSnapshot=e.project?.id==request.projectId&&document.startNumber==request.start&&document.pages.map{it.id}==request.order
        val target=rounded?.takeIf{it>=request.start&&it<request.start+request.order.size}?.let{document.pages[(it-request.start).toInt()]}
        val canMove=validSnapshot&&target!=null&&!c.busy&&!c.maintenance
        fun apply(after:Boolean){if(canMove){e.movePagesByNumber(number!!,after,request.selected);moveRequest=null}}
        AlertDialog(onDismissRequest={moveRequest=null},title={Text("移动 ${request.selected.size} 页到指定序号")},text={Column{
            Text("按当前显示序号定位，保留所选页面的相对顺序。")
            Text("范围 ${request.start}–${request.start+request.order.size-1}")
            OutlinedTextField(serial,{serial=it},label={Text("目标显示序号")},singleLine=true)
            if(!validSnapshot)Text("队列或开始编号已改变，请关闭后重新选择。",color=MaterialTheme.colors.error)
            else if(target==null)Text("请输入范围内的序号。小数四舍五入。")
            else Text("目标 $rounded · ${target.name}"+(if(target.id in request.selected)"（在所选页内，位置保持不变）"else""))
        }},confirmButton={Row{TextButton(enabled=canMove,onClick={apply(false)}){Text("插入前面")};TextButton(enabled=canMove,onClick={apply(true)}){Text("插入后面")}}},
            dismissButton={TextButton(onClick={moveRequest=null}){Text("取消移动")}})
    }
}

internal class QueuePopupPosition(private val cursor:Offset,private val margin:Int):PopupPositionProvider {
    override fun calculatePosition(anchorBounds:IntRect,windowSize:IntSize,layoutDirection:LayoutDirection,popupContentSize:IntSize):IntOffset {
        fun axis(point:Float,size:Int,window:Int):Int {
            val forward=point.roundToInt()+margin
            val desired=if(forward+size+margin>window)point.roundToInt()-size-margin else forward
            val maximum=(window-size-margin).coerceAtLeast(0)
            return desired.coerceIn(margin.coerceAtMost(maximum),maximum)
        }
        return IntOffset(axis(cursor.x,popupContentSize.width,windowSize.width),axis(cursor.y,popupContentSize.height,windowSize.height))
    }
}

@Composable private fun TypesetQueueHover(c:WorkbenchController,document:TypesetDocument,page:TypesetPage,cursor:Offset){
    val index=document.pages.indexOf(page);val number=document.startNumber+index
    val (raster,error)=queueRaster(c,page,document.mode,number,720)
    val density=LocalDensity.current
    val width=with(density){(LocalWindowInfo.current.containerSize.width*.42f).toDp()}.coerceIn(1.dp,360.dp)
    Popup(popupPositionProvider=QueuePopupPosition(cursor,with(density){18.dp.roundToPx()}),properties=PopupProperties(focusable=false)){
        Surface(Modifier.width(width),elevation=8.dp){Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(page.name,maxLines=2)
            val current=raster?.takeIf{it.page==page&&it.mode==document.mode&&it.number==number}
            if(current!=null){Image(current.bitmap,"队列处理后预览",Modifier.fillMaxWidth().heightIn(max=320.dp));Text("排版 ${current.width} × ${current.height} · 第 ${index+1}/${document.pages.size} 张")}
            else Text(error?.let{"预览不可用：$it"}?:"正在排版预览…")
        }}
    }
}
