package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Static, bounded queue thumbnails: no per-card animation players or full-result cache. */
internal object QueueThumbnailLoader {
    private val permits=Semaphore(2)
    suspend fun load(c:WorkbenchController,id:String):ImageBitmap=permits.withPermit {
        withContext(Dispatchers.IO){
            currentCoroutineContext().ensureActive()
            val image=DesktopImageEditing.decode(c.workspace.bytes(id),longestSide=128)
            try{image.toComposeImageBitmap()}finally{image.flush()}
        }
    }
}
@Composable internal fun QueueStaticThumbnail(c:WorkbenchController,id:String,modifier:Modifier=Modifier) {
    val bitmap by produceState<ImageBitmap?>(null,id){try{value=QueueThumbnailLoader.load(c,id)}catch(e:CancellationException){throw e}catch(e:Exception){value=null}}
    Box(modifier,contentAlignment=Alignment.Center){bitmap?.let{Image(it,"队列静态缩略图",Modifier.fillMaxSize())}?:TextPlaceholder()}
}
@Composable private fun TextPlaceholder(){androidx.compose.material.Text("…",color=Quiet)}
