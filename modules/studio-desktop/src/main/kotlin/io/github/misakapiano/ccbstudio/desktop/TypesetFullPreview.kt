package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*

/** Local preview navigation has no project, selection, export or filesystem side effects. */
@Composable internal fun TypesetFullPreview(c:WorkbenchController,dismiss:()->Unit) {
    val document=c.editor.document?:return
    var index by remember{mutableStateOf(document.pages.indexOfFirst{it.id==c.editor.pageId}.coerceAtLeast(0))}
    val page=document.pages.getOrNull(index)?:return
    val focus=remember{FocusRequester()};var error by remember{mutableStateOf<String?>(null)}
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,page,document.mode){value=null;error=null
        try{value=withContext(Dispatchers.IO){val bytes=c.workspace.bytes(page.assetId);val raster=if(document.mode=="storybook")TypesetRenderer.story(bytes,page,document.startNumber+index)else TypesetRenderer.render(bytes,page)
            try{raster.toComposeImageBitmap()}finally{raster.flush()}}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
    }
    TypesetPreviewDialog("完整预览 · ${TypesetQueue.number(document,index)} · ${page.name}",dismiss,content={
        Box(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent{event->
            if(event.type!=KeyEventType.KeyDown)false else when(event.key){
                Key.DirectionLeft->{index=(index-1).coerceAtLeast(0);true}
                Key.DirectionRight->{index=(index+1).coerceAtMost(document.pages.lastIndex);true}
                Key.Escape->{dismiss();true}
                else->false
            }
        }.focusable(),contentAlignment=Alignment.Center){image?.let{Image(it,"完整页面预览",Modifier.fillMaxSize())}?:Text(error?:"正在排版…")}
        LaunchedEffect(Unit){focus.requestFocus()}
    },actions={
        TextButton(enabled=index>0,onClick={index--}){Text("上一页 · ←")}
        Text("${index+1} / ${document.pages.size}")
        TextButton(enabled=index<document.pages.lastIndex,onClick={index++}){Text("下一页 · →")}
        TextButton(onClick=dismiss){Text("关闭完整预览 · Esc")}
    })
}
