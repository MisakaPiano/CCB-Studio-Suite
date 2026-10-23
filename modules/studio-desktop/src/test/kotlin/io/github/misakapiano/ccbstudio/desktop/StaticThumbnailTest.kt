package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.example.chatbar.desktop.DesktopImageEditing
import io.github.misakapiano.ccbstudio.core.EditorRenderer
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.nio.file.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class StaticThumbnailTest {
    @Test fun concurrentCardsShareDecodeAndCacheWhileBudgetEvictsLeastRecentlyUsed()=runBlocking {
        val reads=AtomicInteger();val gate=CountDownLatch(1)
        val store=StaticThumbnailStore(this,{reads.incrementAndGet();check(gate.await(10,TimeUnit.SECONDS));byteArrayOf()},2L*256*256*4,{BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB)})
        try {
            val cards=(1..12).map{async{store.load("one")}};delay(50);gate.countDown();cards.awaitAll()
            assertEquals(1,reads.get());store.load("two");store.load("one");store.load("three")
            assertEquals(3,reads.get());store.load("two");assertEquals(4,reads.get())
            assertTrue(store.retainedBytes<=2L*256*256*4);assertEquals(0,store.pendingCount)
        }finally{gate.countDown();store.pauseAndDrain()}
    }
    @Test fun cancellationDrainsDecodersDoesNotCacheFailuresAndCanResume()=runBlocking {
        val entered=CountDownLatch(2);val release=CountDownLatch(1);val active=AtomicInteger();val peak=AtomicInteger()
        val store=StaticThumbnailStore(this,{active.incrementAndGet().let{peak.accumulateAndGet(it,::maxOf)};entered.countDown();try{check(release.await(10,TimeUnit.SECONDS));byteArrayOf()}finally{active.decrementAndGet()}},decode={BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB)})
        try {
            val cards=(1..12).map{async{store.load("$it")}};withContext(Dispatchers.IO){assertTrue(entered.await(10,TimeUnit.SECONDS))}
            val drain=async{store.pauseAndDrain()};delay(50);assertFalse(drain.isCompleted)
            release.countDown();drain.await();cards.forEach{assertFailsWith<CancellationException>{it.await()}}
            assertEquals(2,peak.get());assertEquals(0,active.get());assertEquals(0,store.pendingCount);assertEquals(0,store.retainedBytes)
            store.resume();store.load("after-resume");assertTrue(store.retainedBytes>0)
        }finally{release.countDown();store.pauseAndDrain()}
        var reads=0
        val retry=StaticThumbnailStore(this,{reads++;if(reads==1)error("damaged derivative");byteArrayOf()},decode={BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB)})
        try{assertFailsWith<IllegalStateException>{retry.load("x")};assertEquals(0,retry.retainedBytes);retry.load("x");assertEquals(2,reads)}finally{retry.pauseAndDrain()}
    }
    @Test fun unsubscribingLastCardCancelsQueuedWorkAndFallbackRasterIsBounded()=runBlocking {
        val entered=CountDownLatch(2);val release=CountDownLatch(1);val reads=AtomicInteger()
        val store=StaticThumbnailStore(this,{reads.incrementAndGet();entered.countDown();check(release.await(10,TimeUnit.SECONDS));byteArrayOf()},decode={BufferedImage(2048,1024,BufferedImage.TYPE_INT_ARGB)})
        try {
            val first=async{store.load("a")};val second=async{store.load("b")};withContext(Dispatchers.IO){assertTrue(entered.await(10,TimeUnit.SECONDS))}
            val queued=async{store.load("never-read")};delay(20);queued.cancelAndJoin();first.cancel();second.cancel();release.countDown();first.join();second.join()
            store.pauseAndDrain();assertEquals(2,reads.get());assertEquals(0,store.retainedBytes)
            store.resume();val bitmap=store.load("new");assertEquals(256,bitmap.width);assertEquals(128,bitmap.height)
        }finally{release.countDown();store.pauseAndDrain()}
    }
    @Test fun animatedSourcesUseStaticCoverAndNeverRewriteSourceBytes()=runBlocking {
        val source=EditorRenderer.synthetic(96,128);val cover=BufferedImage(96,128,BufferedImage.TYPE_INT_ARGB).let{image->try{for(y in 0 until 128)for(x in 0 until 96)image.setRGB(x,y,0xff00ff00.toInt());DesktopImageEditing.png(image)}finally{image.flush()}}
        val disguised=animationTransform(source,"disguise",cover)
        val inputs=mapOf("apng" to disguised,"gif" to Files.readAllBytes(Path.of("../studio-core/src/test/resources/codecs/animated.gif")),"webp" to Files.readAllBytes(Path.of("../studio-core/src/test/resources/codecs/animated.webp")))
        val originals=inputs.mapValues{it.value.copyOf()};val reads=AtomicInteger()
        val store=StaticThumbnailStore(this,{reads.incrementAndGet();inputs.getValue(it)})
        try {
            val bitmap=store.load("apng");assertEquals(1f,bitmap.toPixelMap()[48,64].green)
            inputs.keys.forEach{store.load(it)};delay(250);inputs.keys.forEach{store.load(it)};assertEquals(3,reads.get())
            inputs.forEach{(id,bytes)->assertContentEquals(originals[id],bytes)}
            val restored=animationTransform(disguised,"restore");assertEquals(96,EditorRenderer.decode(restored).let{try{it.width}finally{it.flush()}})
        }finally{store.pauseAndDrain()}
    }
    @Test fun syntheticTwentyFourCardResourceComparison():Unit=runBlocking {
        val bytes=EditorRenderer.synthetic(1536,2048);val ids=(1..24).map{"synthetic-$it.png"};val reads=AtomicInteger()
        val cpu=ProcessHandle.current();fun cpuNanos()=cpu.info().totalCpuDuration().orElse(java.time.Duration.ZERO).toNanos()
        val measurements=mutableListOf<JsonObject>()
        suspend fun measure(name:String,body:suspend ()->Long) {
            val before=System.nanoTime();val beforeCpu=cpuNanos();val pixels=body()
            measurements+=buildJsonObject{put("phase",name);put("wallMs",(System.nanoTime()-before)/1_000_000);put("processCpuMs",(cpuNanos()-beforeCpu)/1_000_000);put("retainedDecodedPixelsBytes",pixels);put("processRss","NOT MEASURED");put("scope","derivative decode/cache only, not full UI latency")}
        }
        // Conservative baseline: old card decoder only; excludes old PNG round-trip and animation loops.
        measure("old-static-decoder-1600-cold") {
            ids.map{DesktopImageEditing.decode(bytes,longestSide=1600).let{image->try{image.toComposeImageBitmap()}finally{image.flush()}}}.sumOf{it.width.toLong()*it.height*4}
        }
        val store=StaticThumbnailStore(this,{reads.incrementAndGet();bytes})
        try {
            measure("new-static-256-cold"){ids.map{async{store.load(it)}}.awaitAll();store.retainedBytes}
            val coldReads=reads.get();assertEquals(24,coldReads)
            measure("new-static-256-warm-10-visits"){repeat(10){ids.forEach{store.load(it)}};store.retainedBytes}
            assertEquals(coldReads,reads.get());assertTrue(store.retainedBytes<=8L*1024*1024)
            val report=buildJsonObject{put("syntheticOnly",true);put("images",24);put("format","PNG");put("sourceWidth",1536);put("sourceHeight",2048);put("eachEncodedBytes",bytes.size);put("java",System.getProperty("java.version"));put("os",System.getProperty("os.name"));put("processors",Runtime.getRuntime().availableProcessors());put("heapLimitBytes",Runtime.getRuntime().maxMemory());put("warmAdditionalReads",reads.get()-coldReads);put("measurements",JsonArray(measurements));put("humanAcceptance","NOT TESTED");put("largePrivateCorpus","NOT TESTED")}
            val target=Path.of("build/performance-106/thumbnail-comparison.json");Files.createDirectories(target.parent);Files.writeString(target,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),report))
        }finally{store.pauseAndDrain()}
    }
}
