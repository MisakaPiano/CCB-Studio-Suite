package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import com.example.chatbar.desktop.DesktopImageEditing
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.awt.image.BufferedImage

/** Disposable visual derivatives only. Original reads/exports still verify the immutable asset.
 * Per workspace: 8 MiB decoded-pixel LRU, two decoders, coalesced requests, no animation clocks.
 * Composition owns a subscription; leaving the last card cancels queued/in-flight work.
 */
internal class StaticThumbnailStore(private val scope:CoroutineScope,private val read:(String)->ByteArray,
    private val budget:Long=8L*1024*1024,private val decode:(ByteArray)->BufferedImage={DesktopImageEditing.decode(it,longestSide=256)}) {
    private val lock=Any()
    private val permits=Semaphore(2)
    private val cache=LinkedHashMap<String,ImageBitmap>(16,.75f,true)
    private class Request(val job:Deferred<ImageBitmap>,var subscribers:Int=0)
    private val pending=mutableMapOf<String,Request>()
    private var owner=SupervisorJob(scope.coroutineContext[Job])
    private var paused=false
    internal val retainedBytes:Long get()=synchronized(lock){cache.values.sumOf{it.width.toLong()*it.height*4}}
    internal val pendingCount:Int get()=synchronized(lock){pending.size}
    suspend fun load(id:String):ImageBitmap {
        val request=synchronized(lock) {
            check(!paused){"Thumbnail store paused"}
            cache[id]?.let{return it}
            (pending[id]?:Request(scope.async(owner+Dispatchers.IO,start=CoroutineStart.LAZY) {
                permits.withPermit {
                    currentCoroutineContext().ensureActive()
                    val image=decode(read(id))
                    val bitmap=try {
                        // Some fallback codecs cannot subsample; bound the retained raster too.
                        val ratio=minOf(1.0,256.0/maxOf(image.width,image.height))
                        if(ratio==1.0)image.toComposeImageBitmap() else {
                            val small=BufferedImage((image.width*ratio).toInt().coerceAtLeast(1),(image.height*ratio).toInt().coerceAtLeast(1),BufferedImage.TYPE_INT_ARGB)
                            try {small.createGraphics().let{g->try{g.drawImage(image,0,0,small.width,small.height,null)}finally{g.dispose()}};small.toComposeImageBitmap()}finally{small.flush()}
                        }
                    }finally{image.flush()}
                    val context=currentCoroutineContext();context.ensureActive()
                    synchronized(lock) {
                        context.ensureActive()
                        if(!paused&&bitmap.width.toLong()*bitmap.height*4<=budget) {
                            cache[id]=bitmap
                            while(cache.values.sumOf{it.width.toLong()*it.height*4}>budget)cache.remove(cache.keys.first())
                        }
                    }
                    bitmap
                }
            }).also{pending[id]=it}).also{it.subscribers++}
        }
        try{return request.job.await()}finally {
            synchronized(lock) {
                request.subscribers--
                if(request.subscribers==0) {
                    if(pending[id]===request)pending.remove(id)
                    if(!request.job.isCompleted)request.job.cancel()
                }
            }
        }
    }
    suspend fun pauseAndDrain() {
        val job=synchronized(lock){paused=true;cache.clear();owner.also{it.cancel()}}
        job.join()
        synchronized(lock){pending.clear()}
    }
    fun resume()=synchronized(lock){if(paused){check(owner.isCompleted);owner=SupervisorJob(scope.coroutineContext[Job]);paused=false}}
}

@Composable internal fun GalleryStaticThumbnail(c:WorkbenchController,id:String,modifier:Modifier=Modifier) {
    val bitmap by produceState<ImageBitmap?>(null,c.thumbnails,id,c.maintenance) {
        value=null
        if(c.maintenance)return@produceState
        try{value=c.thumbnails.load(id)}catch(e:CancellationException){throw e}catch(e:Exception){value=null}
    }
    Box(modifier,contentAlignment=Alignment.Center) {
        bitmap?.let{Image(it,"静态缩略图",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}?:Text("…",color=Quiet)
    }
}
