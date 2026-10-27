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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
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
internal data class QueueHoverRaster(val page:TypesetPage,val mode:String,val number:Int,val bitmap:ImageBitmap,val width:Int,val height:Int)
internal data class TypesetQueueRasterKey(val page:TypesetPage,val mode:String,val number:Int,val maxSide:Int)
/** Controller-owned display bitmaps survive route changes; originals/full rasters are not cached. */
internal class TypesetQueueRasterCache {
    private val entries=LinkedHashMap<TypesetQueueRasterKey,QueueHoverRaster>(24,.75f,true)
    internal var renders=0;private set
    @Synchronized fun get(key:TypesetQueueRasterKey)=entries[key]
    @Synchronized fun put(key:TypesetQueueRasterKey,value:QueueHoverRaster){
        entries[key]=value;renders++
        while(entries.size>24||entries.values.sumOf{it.bitmap.width.toLong()*it.bitmap.height}>2_000_000L)entries.remove(entries.keys.first())
    }
    @Synchronized internal fun size()=entries.size
}
private val queueRasterDispatcher=Dispatchers.IO.limitedParallelism(1)

/** Render one visible queue image at a time and retain only a display-sized bitmap. */
@Composable private fun queueRaster(c:WorkbenchController,page:TypesetPage,mode:String,number:Int,maxSide:Int):Pair<QueueHoverRaster?,String?> {
    val key=TypesetQueueRasterKey(page,mode,number,maxSide)
    var error by remember(page,mode,number,maxSide){mutableStateOf<String?>(null)}
    val raster by produceState<QueueHoverRaster?>(c.editor.queueRasters.get(key),key){
        c.editor.queueRasters.get(key)?.let{value=it;return@produceState}
        // Coalesce rapid property edits before running non-cooperative full-quality rendering.
        delay(120)
        try{val next=withContext(queueRasterDispatcher){val image=typesetPreviewRaster(c.workspace,page,mode=="storybook",number)
            try{
                val scale=minOf(1.0,maxSide.toDouble()/maxOf(image.width,image.height))
                val small=BufferedImage((image.width*scale).roundToInt().coerceAtLeast(1),(image.height*scale).roundToInt().coerceAtLeast(1),BufferedImage.TYPE_INT_ARGB)
                try{val graphics=small.createGraphics();try{graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(image,0,0,small.width,small.height,null)}finally{graphics.dispose()}
                    QueueHoverRaster(page,mode,number,small.toComposeImageBitmap(),image.width,image.height)
                }finally{small.flush()}
            }finally{image.flush()}}
            currentCoroutineContext().ensureActive();c.editor.queueRasters.put(key,next);value=next
        }
        catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
    }
    return raster?.takeIf{it.page==page&&it.mode==mode&&it.number==number} to error
}

/** Queue interactions follow TypeSet 1.1; only a completed drop changes the document. */
@Composable internal fun TypesetQueue(c:WorkbenchController,e:TypesetEditorController,modifier:Modifier,onPreview:((String)->Unit)?=null) {
    val document=e.document?:return
    val list=rememberLazyListState()
    val density=LocalDensity.current
    val edge=with(density){36.dp.toPx()}
    val checkboxWidth=with(density){30.dp.toPx()}
    var queueSize by remember{mutableStateOf(IntSize.Zero)}
    val grid=e.queueView=="grid"
    var thumbnailDraft by remember(document.thumbnailSize){mutableStateOf(document.thumbnailSize.toFloat())}
    val columns=if(!grid)1 else maxOf(1,(with(density){queueSize.width.toDp().value}/maxOf(84f,document.thumbnailSize+16f)).toInt())
    val rows=document.pages.chunked(columns)
    val focus=remember{FocusRequester()}
    var dragging by remember{mutableStateOf(false)}
    var dragY by remember{mutableStateOf(0f)}
    var dragX by remember{mutableStateOf(0f)}
    var insertion by remember{mutableStateOf<String?>(null)}
    var hoverId by remember{mutableStateOf<String?>(null)}
    var cursor by remember{mutableStateOf(Offset.Zero)}
    var moveRequest by remember{mutableStateOf<QueueMoveRequest?>(null)}
    var contextId by remember{mutableStateOf<String?>(null)}
    var contextPosition by remember{mutableStateOf(Offset.Zero)}
    fun updateInsertion(){
        val d=e.document?:return
        val row=list.layoutInfo.visibleItemsInfo.firstOrNull{dragY<it.offset+it.size}
        val raw=if(row==null)d.pages.size else if(columns==1)row.index+if(dragY>=row.offset+row.size/2)1 else 0
            else {val cellWidth=queueSize.width.toFloat()/columns;val cell=(dragX/cellWidth.coerceAtLeast(1f)).coerceIn(0f,columns.toFloat())
                (row.index*columns+kotlin.math.floor(cell+.5f).toInt()).coerceAtMost(minOf(d.pages.size,(row.index+1)*columns))}
        insertion=d.pages.drop(raw.coerceIn(0,d.pages.size)).firstOrNull{it.id !in e.selectedPages}?.id
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
    Column(modifier.semantics{contentDescription="图片队列快捷键区域"}.focusRequester(focus).onPreviewKeyEvent{event->
        if(event.type==KeyEventType.KeyDown&&event.key==Key.Escape&&dragging){dragging=false;insertion=null;true}else false
    }.onKeyEvent{typesetShortcut(it,c,e,true)}.focusable()){
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("图片队列",style=MaterialTheme.typography.subtitle2);Text("${document.pages.size} 页",style=MaterialTheme.typography.caption)}
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            EditorCommand("条目",!c.busy&&!c.maintenance,TypesetIcons.List,selected=!grid,modifier=Modifier.weight(1f).semantics{contentDescription="缩略图与条目视图"}){e.queueView="list";hoverId=null;dragging=false;insertion=null}
            EditorCommand("网格",!c.busy&&!c.maintenance,TypesetIcons.Grid,selected=grid,modifier=Modifier.weight(1f).semantics{contentDescription="网格视图"}){e.queueView="grid";hoverId=null;dragging=false;insertion=null}
        }
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
            EditorCommand("全选",modifier=Modifier.weight(1f)){e.selectedPages=document.pages.map{it.id}.toSet()}
            EditorCommand("反选",modifier=Modifier.weight(1f)){e.selectedPages=document.pages.map{it.id}.toSet()-e.selectedPages}
            EditorCommand("清除",modifier=Modifier.weight(1f)){e.selectedPages=emptySet()}
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(Modifier.weight(1f).fillMaxHeight().onSizeChanged{queueSize=it}.semantics{contentDescription="排版页面队列"}
            .onPointerEvent(PointerEventType.Scroll,PointerEventPass.Initial){event->if(event.keyboardModifiers.isCtrlPressed&&!c.busy&&!dragging){
                val delta=event.changes.firstOrNull()?.scrollDelta?.y?:0f
                e.change{it.copy(thumbnailSize=(it.thumbnailSize-delta*8).roundToInt().coerceIn(32,220))};event.changes.forEach{it.consume()}
            }}
            // The list owns capture: scrolling a dragged row offscreen must not cancel the gesture.
            .pointerInput(e.project?.id,c.busy,c.maintenance,grid,columns){detectDragGestures(
                onDragStart={pos->
                    val cellWidth=(queueSize.width.toFloat()/columns).coerceAtLeast(1f)
                    if(!c.busy&&!c.maintenance&&pos.x%cellWidth>checkboxWidth){
                        focus.requestFocus();dragging=true;hoverId=null;contextId=null;dragY=pos.y;dragX=pos.x;updateInsertion()
                    }
                },onDragCancel={dragging=false;insertion=null},
                onDragEnd={if(dragging&&!c.busy&&!c.maintenance){updateInsertion();e.movePagesTo(insertion,insertion==null)};dragging=false;insertion=null},
                onDrag={change,_->if(dragging){change.consume();dragY=change.position.y;dragX=change.position.x;updateInsertion()}}
            )},state=list){itemsIndexed(rows,key={_,row->row.first().id}){rowIndex,row->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            row.forEachIndexed{columnIndex,page->key(page.id){
            val index=rowIndex*columns+columnIndex
            var origin by remember(page.id){mutableStateOf(Offset.Zero)}
            val ink=MaterialTheme.colors.onSurface
            Column(Modifier.weight(1f).padding(bottom=4.dp).background(if(page.id==e.pageId)Gold.copy(alpha=.22f)else Color.Transparent)){
                Box(Modifier.fillMaxWidth().height(2.dp).background(if(dragging&&insertion==page.id)Gold else Color.Transparent))
                Row(Modifier.padding(4.dp),verticalAlignment=Alignment.CenterVertically){
                if(!grid)Checkbox(page.id in e.selectedPages,{e.togglePageSelection(page.id)},enabled=!c.busy&&!c.maintenance,modifier=Modifier.size(24.dp))
                val item=Modifier.weight(1f).onGloballyPositioned{origin=it.positionInWindow()}
                    .semantics{contentDescription="队列页 ${index+1}";onClick{focus.requestFocus();e.selectPage(page.id);true}}
                    .onPointerEvent(PointerEventType.Enter){event->if(!dragging&&!c.busy){hoverId=page.id;cursor=origin+event.changes.first().position}}
                    .onPointerEvent(PointerEventType.Move){event->if(!dragging){cursor=origin+event.changes.first().position}}
                    .onPointerEvent(PointerEventType.Exit){if(hoverId==page.id)hoverId=null}
                    .onPointerEvent(PointerEventType.Press){event->
                        if(!c.busy&&!c.maintenance){
                            focus.requestFocus()
                            hoverId=null
                            if(event.button==PointerButton.Primary){
                                if(page.id in e.selectedPages&&!event.keyboardModifiers.isCtrlPressed&&!event.keyboardModifiers.isShiftPressed){e.selectPage(page.id,keepSelection=true)}
                                else e.selectPage(page.id,event.keyboardModifiers.isCtrlPressed,event.keyboardModifiers.isShiftPressed)
                            }else if(event.button==PointerButton.Secondary){
                                if(page.id !in e.selectedPages)e.selectPage(page.id)
                                contextId=page.id;contextPosition=origin+event.changes.first().position
                            }
                        }
                    }
                    val (thumbnail,previewError)=queueRaster(c,page,document.mode,document.startNumber+index,document.thumbnailSize*2)
                    @Composable fun thumbnail(){Box(if(grid)Modifier.fillMaxWidth().height(maxOf(64,document.thumbnailSize).dp)else Modifier.size(document.thumbnailSize.dp),contentAlignment=Alignment.Center){
                        if(thumbnail!=null)Image(thumbnail.bitmap,"页面 ${index+1} 编辑缩略图",Modifier.fillMaxSize())
                        else Text(if(previewError==null)"…"else"预览失败",style=MaterialTheme.typography.caption,color=ink)
                    }}
                    @Composable fun labels(){Column(Modifier.padding(horizontal=4.dp)){
                        Text("${io.github.misakapiano.ccbstudio.core.TypesetQueue.number(document,index)} · ${page.name}",maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.caption,color=ink)
                        Text("${page.texts.size} 文字 · ${page.masks.size} 打码",style=MaterialTheme.typography.caption,color=ink)
                    }}
                    if(grid)Column(Modifier.weight(1f)){Box{Column(item.fillMaxWidth()){thumbnail();labels()};Checkbox(page.id in e.selectedPages,{e.togglePageSelection(page.id)},enabled=!c.busy&&!c.maintenance,modifier=Modifier.size(24.dp))}
                        if(onPreview!=null)IconButton(onClick={hoverId=null;onPreview(page.id)},enabled=!c.busy&&!c.maintenance,modifier=Modifier.size(24.dp).align(Alignment.End).semantics{contentDescription="预览队列页 ${index+1}"}){Icon(TypesetIcons.Preview,null,Modifier.size(16.dp),tint=ink)}
                    }else {Row(item,verticalAlignment=Alignment.CenterVertically){thumbnail();Box(Modifier.weight(1f)){labels()}}
                        if(onPreview!=null)IconButton(onClick={hoverId=null;onPreview(page.id)},enabled=!c.busy&&!c.maintenance,modifier=Modifier.size(24.dp).semantics{contentDescription="预览队列页 ${index+1}"}){Icon(TypesetIcons.Preview,null,Modifier.size(16.dp),tint=ink)}
                    }
                }
            }
            }}
            repeat(columns-row.size){Spacer(Modifier.weight(1f))}
        }}}
        VerticalScrollbar(rememberScrollbarAdapter(list),Modifier.fillMaxHeight())
        }
        Column(Modifier.fillMaxWidth().padding(8.dp)){
            Text(if(dragging)"移动 ${e.selectedPages.size} 张 · 松开确认 / Esc 取消"else"滚轮浏览 · Ctrl＋滚轮调整缩略图",style=MaterialTheme.typography.caption)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("缩略图",style=MaterialTheme.typography.caption);Text("${thumbnailDraft.roundToInt()} px",style=MaterialTheme.typography.caption)}
            Slider(thumbnailDraft,{thumbnailDraft=it},enabled=!c.busy&&!dragging,valueRange=32f..220f,modifier=Modifier.fillMaxWidth().height(24.dp).semantics{contentDescription="缩略图大小滑条"},onValueChangeFinished={e.change{it.copy(thumbnailSize=thumbnailDraft.roundToInt())}})
        }
        StudioAction("移动到指定序号",!c.busy&&!c.maintenance&&e.selectedPages.isNotEmpty()){requestMove(e.selectedPages)}
    }
    if(contextId!=null&&!dragging)Popup(popupPositionProvider=QueuePopupPosition(contextPosition,0),onDismissRequest={contextId=null},properties=PopupProperties(focusable=true)){
        Surface(elevation=8.dp){Column {StudioMenuItem(onClick={requestMove(setOf(contextId!!))}){Text("移动此页到指定序号")}
            StudioMenuItem(onClick={requestMove(e.selectedPages)}){Text("移动已选 ${e.selectedPages.size} 页到指定序号")}}}
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
