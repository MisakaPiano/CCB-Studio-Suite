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

/** Queue and gallery share the same bounded static derivatives. */
internal object QueueThumbnailLoader {
    suspend fun load(c:WorkbenchController,id:String):ImageBitmap=c.thumbnails.load(id)
}
@Composable internal fun QueueStaticThumbnail(c:WorkbenchController,id:String,modifier:Modifier=Modifier) {
    GalleryStaticThumbnail(c,id,modifier)
}
