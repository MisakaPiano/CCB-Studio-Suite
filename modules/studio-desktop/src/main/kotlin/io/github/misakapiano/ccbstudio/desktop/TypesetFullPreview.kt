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
@Composable internal fun TypesetFullPreview(c:WorkbenchController,initialPageId:String?=null,dismiss:()->Unit) {
    val document=c.editor.document?:return
    var index by remember{mutableStateOf(document.pages.indexOfFirst{it.id==(initialPageId?:c.editor.pageId)}.coerceAtLeast(0))}
    val page=document.pages.getOrNull(index)?:return
    var error by remember{mutableStateOf<String?>(null)}
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null,page,document.mode){value=null;error=null
        try{value=withContext(Dispatchers.IO){val raster=typesetPreviewRaster(c.workspace,page,document.mode=="storybook",document.startNumber+index)
            try{raster.toComposeImageBitmap()}finally{raster.flush()}}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
    }
    TypesetPreviewDialog("完整预览 · ${TypesetQueue.number(document,index)} · ${page.name}",dismiss,content={
        image?.let{TypesetImageViewport(it,page.id,"完整页面预览",Modifier.fillMaxSize(),
            previous=if(index>0)({index--})else null,next=if(index<document.pages.lastIndex)({index++})else null,dismiss=dismiss)}?:Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(error?:"正在排版…")}
    },actions={
        TextButton(enabled=index>0,onClick={index--}){Text("上一页 · ←")}
        Text("${index+1} / ${document.pages.size}")
        TextButton(enabled=index<document.pages.lastIndex,onClick={index++}){Text("下一页 · →")}
    })
}
