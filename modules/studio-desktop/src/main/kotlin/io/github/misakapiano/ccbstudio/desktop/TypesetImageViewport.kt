@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import kotlin.math.*

/** View-only transforms: never touches a document, current edit page or selection. */
@Composable internal fun TypesetImageViewport(image:ImageBitmap,identity:Any,label:String,modifier:Modifier=Modifier,
    previous:(()->Unit)?=null,next:(()->Unit)?=null,dismiss:(()->Unit)?=null) {
    var zoom by remember(identity){mutableStateOf(1f)};var pan by remember(identity){mutableStateOf(Offset.Zero)}
    var size by remember{mutableStateOf(IntSize.Zero)};var hovered by remember{mutableStateOf(false)}
    val focus=remember{FocusRequester()};val base=minOf(size.width.toFloat()/image.width,size.height.toFloat()/image.height)
    fun fit(){zoom=1f;pan=Offset.Zero}
    fun scale(value:Float,anchor:Offset=Offset(size.width/2f,size.height/2f)) {
        val old=(base*zoom).coerceAtLeast(.0001f);val oldOffset=Offset((size.width-image.width*old)/2,(size.height-image.height*old)/2)+pan
        val target=value.coerceIn(.1f,16f);val actual=base*target
        pan=anchor-Offset((size.width-image.width*actual)/2,(size.height-image.height*actual)/2)-(anchor-oldOffset)*(actual/old);zoom=target
    }
    Column(modifier){
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().onSizeChanged{size=it}.focusRequester(focus)
            .semantics{contentDescription=label;stateDescription="zoom=$zoom;pan=${pan.x},${pan.y}"}
            .onKeyEvent{e->if(e.type!=KeyEventType.KeyDown)false else when(e.key){
                Key.DirectionLeft->{previous?.invoke();true};Key.DirectionRight->{next?.invoke();true}
                Key.Escape->{dismiss?.invoke();true};Key.Zero,Key.F->{fit();true}
                Key.Equals,Key.NumPadAdd->{scale(zoom*1.25f);true};Key.Minus,Key.NumPadSubtract->{scale(zoom/1.25f);true};else->false}}
            .onPointerEvent(PointerEventType.Enter){hovered=true}.onPointerEvent(PointerEventType.Exit){hovered=false}
            .onPointerEvent(PointerEventType.Press){focus.requestFocus()}
            .onPointerEvent(PointerEventType.Scroll){e->e.changes.firstOrNull()?.let{p->scale(zoom*exp(-p.scrollDelta.y*.18f),p.position);e.changes.forEach{it.consume()}}}
            .focusable()) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().pointerInput(identity){detectDragGestures{p,delta->p.consume();pan+=delta}}){
                val s=base*zoom;val start=Offset((size.width-image.width*s)/2,(size.height-image.height*s)/2)+pan
                drawImage(image,dstOffset=IntOffset(start.x.roundToInt(),start.y.roundToInt()),dstSize=IntSize((image.width*s).roundToInt().coerceAtLeast(1),(image.height*s).roundToInt().coerceAtLeast(1)))
            }
            if(hovered){previous?.let{IconButton(onClick=it,modifier=Modifier.align(Alignment.CenterStart).background(Panel.copy(alpha=.85f))){Icon(TypesetIcons.Previous,"上一张图片")}}
                next?.let{IconButton(onClick=it,modifier=Modifier.align(Alignment.CenterEnd).background(Panel.copy(alpha=.85f))){Icon(TypesetIcons.Next,"下一张图片")}}}
        }
        Row(Modifier.fillMaxWidth().padding(top=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            TextButton(onClick=::fit){Text("适应画布")};IconButton(onClick={scale(zoom/1.25f)},modifier=Modifier.size(30.dp)){Text("−")}
            Text("${(zoom*100).roundToInt()}%",style=MaterialTheme.typography.caption)
            IconButton(onClick={scale(zoom*1.25f)},modifier=Modifier.size(30.dp)){Text("＋")}
            Spacer(Modifier.weight(1f));Text("滚轮缩放 · 拖动平移 · ← → 翻页 · 0 复位",style=MaterialTheme.typography.caption)
        }
    }
    LaunchedEffect(identity){focus.requestFocus()}
}
