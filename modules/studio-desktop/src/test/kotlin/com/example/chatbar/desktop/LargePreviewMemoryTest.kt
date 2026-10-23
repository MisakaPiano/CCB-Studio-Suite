@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package com.example.chatbar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntSize
import io.github.misakapiano.ccbstudio.core.EditorRenderer
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class LargePreviewMemoryTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private suspend fun ImageComposeScene.frame(){render(System.nanoTime()).close();yield()}
    private fun ImageComposeScene.hasImage():Boolean {
        fun walk(n:SemanticsNode):Boolean=n.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("图片")==true||n.children.any(::walk)
        return semanticsOwners.any{walk(it.rootSemanticsNode)}
    }
    private suspend fun ImageComposeScene.ready(){withTimeout(15000){while(!hasImage()){frame();delay(10)}};frame()}
    private fun save(name:String,report:JsonObject){val p=Path.of("build/performance-108/$name.json");Files.createDirectories(p.parent);Files.writeString(p,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),report))}

    @Test fun directDisplayKeepsAlphaDimensionsAndBoundedTargets() {
        val image=BufferedImage(63,47,BufferedImage.TYPE_INT_ARGB)
        for(y in 0 until image.height)for(x in 0 until image.width)image.setRGB(x,y,((x%4)*85 shl 24) or (120 shl 16) or (220 shl 8) or 60)
        val bytes=try{DesktopImageEditing.png(image)}finally{image.flush()};val copy=bytes.copyOf()
        for(target in listOf<((IntSize)->IntSize)?>(null,{IntSize(31,23)},{IntSize(200,200)})) {
            val old=Legacy107desktopDisplayBitmap(bytes,null,target);val new=desktopDisplayBitmap(bytes,null,target)
            assertEquals(old.sourceDimensions,new.sourceDimensions);assertEquals(old.bitmap.width,new.bitmap.width);assertEquals(old.bitmap.height,new.bitmap.height)
            val a=old.bitmap.toPixelMap();val b=new.bitmap.toPixelMap()
            for(y in 0 until a.height)for(x in 0 until a.width){assertTrue(kotlin.math.abs(a[x,y].alpha-b[x,y].alpha)<.015f);if(a[x,y].alpha>.1f){assertTrue(kotlin.math.abs(a[x,y].red-b[x,y].red)<.04f);assertTrue(kotlin.math.abs(a[x,y].green-b[x,y].green)<.04f)}}
        }
        assertContentEquals(copy,bytes)
    }
    @Test fun zoomReadsOnceAndChangingSourceNeverShowsStaleOrFailedImage():Unit=runBlocking(awt) {
        val bytes=EditorRenderer.synthetic(120,160);val reads=AtomicInteger();var source by mutableStateOf("a")
        val entered=CountDownLatch(1);val release=CountDownLatch(1)
        val scene=ImageComposeScene(400,300,coroutineContext=awt){DesktopImageZoomSurface(source,{id->reads.incrementAndGet();if(id=="slow"){entered.countDown();check(release.await(15,TimeUnit.SECONDS))};if(id=="bad")error("fixture unreadable");bytes},Modifier.fillMaxSize())}
        try {
            scene.ready();assertEquals(1,reads.get())
            source="slow";scene.frame();withContext(Dispatchers.IO){assertTrue(entered.await(10,TimeUnit.SECONDS))};assertFalse(scene.hasImage(),"Previous image must be detached while next source waits")
            source="c";scene.frame();scene.ready();release.countDown();delay(100);scene.frame();assertEquals(3,reads.get());assertTrue(scene.hasImage())
            source="bad";scene.frame();delay(100);scene.frame();assertFalse(scene.hasImage());assertEquals(4,reads.get())
        }finally{release.countDown();scene.close()}
    }
    @Test fun twentyFourLargeDisplaysCompareAllocationAndCpuWithoutEncodingRoundTrip() {
        val bytes=EditorRenderer.synthetic(2048,3072);val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val rows=mutableListOf<JsonObject>();val thread=Thread.currentThread().id
        for(old in listOf(true,false)) {
            val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
            repeat(24){val bitmap=if(old)Legacy107desktopDisplayBitmap(bytes,null,null).bitmap else desktopDisplayBitmap(bytes,null,null).bitmap;assertEquals(1024,bitmap.width);assertEquals(1536,bitmap.height)}
            rows+=buildJsonObject{put("version",if(old)107 else 108);put("wallMs",(System.nanoTime()-start)/1e6);put("threadCpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
        }
        save("large-display-conversion",buildJsonObject{put("syntheticOnly",true);put("source","24 conversions of a2048x3072PNG");put("scope","display conversion; not full UI latency or RSS");put("measurements",JsonArray(rows))})
        assertTrue(rows[1].getValue("allocatedBytes").jsonPrimitive.long <
            rows[0].getValue("allocatedBytes").jsonPrimitive.long * .85,
            "Bulk raster conversion must reduce allocation versus the shipping107 display path")
    }
    @Test fun sustainedPreviewMountFlipAndUnmountRecordsResourcesAndStopsReads():Unit=runBlocking(awt) {
        val bytes=EditorRenderer.synthetic(2048,3072);val original=bytes.copyOf();val rows=mutableListOf<JsonObject>()
        for(old in listOf(true,false)) {
            var source by mutableStateOf("initial");var mounted by mutableStateOf(true);val reads=AtomicInteger()
            val scene=ImageComposeScene(1000,800,coroutineContext=awt){if(mounted){if(old)Legacy107DesktopImageZoomSurface(source,{reads.incrementAndGet();bytes},Modifier.fillMaxSize())else DesktopImageZoomSurface(source,{reads.incrementAndGet();bytes},Modifier.fillMaxSize())}}
            suspend fun sample(phase:String) {
                val win=withContext(Dispatchers.IO){val p=ProcessBuilder("powershell.exe","-NoProfile","-Command","Get-Process -Id ${ProcessHandle.current().pid()} | Select-Object WorkingSet64,PrivateMemorySize64,HandleCount | ConvertTo-Json -Compress").start();val text=p.inputStream.bufferedReader().readText();check(p.waitFor(15,TimeUnit.SECONDS)&&p.exitValue()==0);Json.parseToJsonElement(text.trim()).jsonObject}
                rows+=buildJsonObject{put("version",if(old)107 else 108);put("phase",phase);put("reads",reads.get());put("heapUsed",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used);put("threads",ManagementFactory.getThreadMXBean().threadCount);put("process",win)}
            }
            try {
                scene.ready();sample("warm")
                repeat(60){cycle->
                    source="image-$cycle";scene.frame();scene.ready();delay(500)
                    mounted=false;scene.frame();delay(500)
                    if(cycle%20==19){System.gc();delay(150);sample("unmounted-${cycle+1}")}
                    mounted=true;scene.frame();scene.ready()
                }
                mounted=false;scene.frame();val before=reads.get();delay(1000);scene.frame();assertEquals(before,reads.get(),"No reads after page disposal")
            }finally{scene.close()}
            System.gc();delay(300);sample("closed")
        }
        assertContentEquals(original,bytes)
        save("sustained-preview",buildJsonObject{put("syntheticOnly",true);put("cyclesPerVersion",60);put("scope","offscreen1000x800 production zoom surface, testJVM process resources, same JVM legacy first; explicitGC only at stated unmounted checkpoints");put("measurements",JsonArray(rows));put("unboundedLeakConclusion","NOT CLAIMED; bounded two-minute scenario only")})
    }
}
