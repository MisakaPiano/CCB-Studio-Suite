@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.foundation.ExperimentalFoundationApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*

@Composable internal fun LibrarySplitPane(g:GalleryViewState,modifier:Modifier=Modifier,main:@Composable ()->Unit,detail:@Composable ()->Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val availableWidth=maxWidth
        val density=androidx.compose.ui.platform.LocalDensity.current
        if(maxWidth<700.dp)main()else Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()){main()}
            Box(Modifier.width(10.dp).fillMaxHeight().pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
                .semantics{contentDescription="调整图片详情宽度"}.pointerInput(availableWidth){detectDragGestures{change,delta->change.consume();g.detailWidth=(g.detailWidth-with(density){delta.x.toDp().value}).coerceIn(190f,availableWidth.value*.6f)}}
                .combinedClickable(onClick={},onDoubleClick={g.detailWidth=250f})) {
                Box(Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight().background(LibraryLine))
            }
            Box(Modifier.width(g.detailWidth.coerceIn(190f,availableWidth.value*.6f).dp).fillMaxHeight()){detail()}
        }
    }
}

internal class LibraryDragState {
    val cards=mutableMapOf<String,Pair<Rect,List<String>>>()
    val targets=mutableMapOf<String,Pair<Rect,(List<String>)->Unit>>()
    var grid=Rect.Zero
    var origin by mutableStateOf<Offset?>(null)
    var position by mutableStateOf(Offset.Zero)
    var dragging by mutableStateOf<List<String>>(emptyList())
    var marquee by mutableStateOf<Rect?>(null)
    var source:List<String>?=null
    var before:Set<String> = emptySet()
    var ctrl=false;var shift=false
    var activeTarget by mutableStateOf<String?>(null)
    fun cancel(){origin=null;dragging=emptyList();marquee=null;activeTarget=null;source=null}
}

/** Keep the thumbnail's grab point at the pointer; constrain using the measured preview only. */
internal fun libraryDragPreviewPosition(pointer:Offset,preview:androidx.compose.ui.unit.IntSize,container:androidx.compose.ui.unit.IntSize,hotspot:Offset):Offset = Offset(
    (pointer.x-hotspot.x).coerceIn(0f,(container.width-preview.width).coerceAtLeast(0).toFloat()),
    (pointer.y-hotspot.y).coerceIn(0f,(container.height-preview.height).coerceAtLeast(0).toFloat()))

/** A single root gesture owns threshold, fixed payload and marquee; no image IO during pointer moves. */
internal fun Modifier.libraryDrag(d:LibraryDragState,selected:()->Set<String>,select:(Set<String>)->Unit,order:()->List<String>,onFocus:()->Unit={}):Modifier =
    onPointerEvent(PointerEventType.Press,PointerEventPass.Initial){e->
        val p=e.changes.firstOrNull()?.position?:return@onPointerEvent
        if(!e.buttons.isPrimaryPressed||!d.grid.contains(p))return@onPointerEvent
        onFocus()
        d.origin=p;d.position=p;d.before=selected();d.ctrl=e.keyboardModifiers.isCtrlPressed;d.shift=e.keyboardModifiers.isShiftPressed
        d.source=d.cards.values.firstOrNull{it.first.contains(p)}?.second
    }.onPointerEvent(PointerEventType.Move,PointerEventPass.Initial){e->
        val p=e.changes.firstOrNull()?.position?:return@onPointerEvent
        val origin=d.origin?:return@onPointerEvent
        if(!e.buttons.isPrimaryPressed){d.cancel();return@onPointerEvent}
        d.position=p
        if((p-origin).getDistance()<6f && d.dragging.isEmpty()&&d.marquee==null)return@onPointerEvent
        val source=d.source
        if(source!=null) {
            if(d.dragging.isEmpty()){val ids=if(source.all{it in d.before})d.before else source.toSet();select(ids);d.dragging=order().filter{it in ids}.distinct()}
            d.activeTarget=d.targets.entries.firstOrNull{it.value.first.contains(p)}?.key
        }else {
            val rect=Rect(minOf(origin.x,p.x),minOf(origin.y,p.y),maxOf(origin.x,p.x),maxOf(origin.y,p.y));d.marquee=rect
            val hit=d.cards.values.filter{it.first.overlaps(rect)}.flatMap{it.second}.toSet()
            select(if(d.ctrl)d.before symmetricDifference hit else if(d.shift)d.before+hit else hit)
        }
        e.changes.forEach{it.consume()}
    }.onPointerEvent(PointerEventType.Release,PointerEventPass.Initial){e->
        if(d.origin==null)return@onPointerEvent
        val ids=d.dragging;val point=e.changes.firstOrNull()?.position?:d.position
        val target=d.targets.values.firstOrNull{it.first.contains(point)}?.second
        if(ids.isNotEmpty()||d.marquee!=null)e.changes.forEach{it.consume()}
        else if(d.source==null&&!d.ctrl&&!d.shift)select(emptySet())
        d.cancel();if(ids.isNotEmpty())target?.invoke(ids)
    }
private infix fun <T> Set<T>.symmetricDifference(other:Set<T>)=(this-other)+(other-this)

internal fun Modifier.libraryCardBounds(d:LibraryDragState,key:String,ids:List<String>,rootOffset:()->Offset)=onGloballyPositioned{layout->val r=layout.boundsInRoot();d.cards[key]=r.translate(-rootOffset()) to ids}
@Composable internal fun Modifier.libraryTarget(d:LibraryDragState,label:String,action:(List<String>)->Unit,rootOffset:()->Offset,enabled:Boolean=true):Modifier {
    var bounds by remember(d,label){mutableStateOf<Rect?>(null)}
    SideEffect {val rect=bounds;if(enabled&&rect!=null)d.targets[label]=rect to action else d.targets.remove(label)}
    DisposableEffect(d,label){onDispose{d.targets.remove(label)}}
    return onGloballyPositioned{layout->bounds=layout.boundsInRoot().translate(-rootOffset())}
}
