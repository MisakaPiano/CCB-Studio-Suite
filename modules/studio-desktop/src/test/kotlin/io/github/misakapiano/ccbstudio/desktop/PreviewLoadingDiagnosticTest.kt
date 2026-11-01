package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.imageio.ImageIO
import kotlin.test.*

/** Distinct-content / cache working-set stage diagnostic, not native UI or RSS acceptance. */
class PreviewLoadingDiagnosticTest {
    @Test fun distinctContentPreviewStages():Unit=runBlocking {
        val root=Path.of(System.getProperty("preview.test.root"),newId())
        val rows=mutableListOf<JsonObject>();val hashes=mutableListOf<String>();val ids=mutableListOf<String>()
        fun cpu()=ProcessHandle.current().info().totalCpuDuration().orElse(java.time.Duration.ZERO).toNanos()
        suspend fun measure(name:String,body:suspend ()->Unit) {
            val t=System.nanoTime();val c=cpu();body()
            rows+=buildJsonObject{put("phase",name);put("wallMs",(System.nanoTime()-t)/1e6);put("processCpuMs",(cpu()-c)/1e6);put("heapUsedBytes",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used)}
        }
        StudioWorkspace.openStudio(root.resolve("workspace")).use { w->
            repeat(64){n->
                val image=BufferedImage(1024,1536,BufferedImage.TYPE_INT_RGB)
                val bytes=try {
                    val line=IntArray(1024)
                    for(y in 0 until 1536){for(x in line.indices)line[x]=(((x+n*7)%256) shl 16) or (((y+n*11)%256) shl 8) or ((x/8+y/8+n*13)%256);image.setRGB(0,y,1024,1,line,0,1024)}
                    ByteArrayOutputStream().also{check(ImageIO.write(image,"png",it))}.toByteArray()
                }finally{image.flush()}
                hashes+=sha256(bytes)
                ids+=w.importImage("synthetic-$n.png",bytes,buildJsonObject{put("kind","image-ingress");put("version",1);put("source","file");put("sourcePath",root.resolve("synthetic-directory-${n%8}/synthetic-$n.png").toString())}.toString())
            }
            assertEquals(64,hashes.toSet().size)
            val reads=AtomicInteger();val readNs=AtomicLong();val decodeNs=AtomicLong()
            val store=StaticThumbnailStore(this,{id->val t=System.nanoTime();try{reads.incrementAndGet();w.bytes(id)}finally{readNs.addAndGet(System.nanoTime()-t)}},decode={b->val t=System.nanoTime();try{DesktopImageEditing.decode(b,longestSide=256)}finally{decodeNs.addAndGet(System.nanoTime()-t)}})
            suspend fun thumbs(name:String,subset:List<String>) {
                val r=reads.get();val rt=readNs.get();val dt=decodeNs.get()
                measure(name){subset.map{async{store.load(it)}}.awaitAll()}
                rows+=buildJsonObject{put("phase","$name-counters");put("reads",reads.get()-r);put("summedReadMs",(readNs.get()-rt)/1e6);put("summedDecodeMs",(decodeNs.get()-dt)/1e6);put("retainedPixelBytes",store.retainedBytes)}
            }
            try{
                thumbs("first-12",ids.take(12));thumbs("immediate-return-12",ids.take(12))
                thumbs("next-52",ids.drop(12));thumbs("return-after-64",ids.take(12))
                assertEquals(0,store.pendingCount);assertTrue(store.retainedBytes<=8L*1024*1024)
            }finally{store.pauseAndDrain()}
            repeat(3){i->
                measure("verified-source-read-$i"){assertEquals(hashes[i],sha256(w.bytes(ids[i])))}
                val b=w.bytes(ids[i])
                measure("full-display-conversion-$i"){val displayed=desktopDisplayBitmap(b,null,null);assertEquals(1024,displayed.sourceDimensions.width)}
                measure("editor-full-render-$i"){val image=typesetPreviewRaster(w,TypesetPage(assetId=ids[i],name="synthetic"),false,i+1);try{assertEquals(1024,image.width)}finally{image.flush()}}
            }
            val assets=w.catalog()
            repeat(3){i->
                measure("directory-grouping-$i"){assertEquals(8,galleryDirectoryAlbums(ids,assets,emptyList()).size)}
                measure("history-projection-$i"){assertTrue(readGalleryHistory(w).entries.isEmpty())}
                measure("library-load-prompts-archive-$i"){val repo=LibraryRepository(w);val state=repo.load();repo.prompts();AssetArchive(w).ids(state)}
            }
            ids.forEachIndexed{i,id->assertEquals(hashes[i],sha256(w.bytes(id)))}
            val result=buildJsonObject{
                put("images",64);put("uniqueContentHashes",hashes.toSet().size);put("dimensions","1024x1536 PNG");put("providerCalls",0)
                put("sourceSha256",JsonArray(hashes.map{JsonPrimitive(it)}));put("measurements",JsonArray(rows))
                put("java",System.getProperty("java.version"));put("heapLimitBytes",Runtime.getRuntime().maxMemory())
                put("limits","Stage diagnostic only. OS cache not cleared. No native frames, UI readiness, RSS, real generation history, editor layers, preference writes or long-term resource acceptance. Summed concurrent phase durations are not exclusive wall time. Display conversions include neither compositor nor animation.")
            }
            val target=Path.of("build/preview-loading-20261028/diagnostic.json");Files.createDirectories(target.parent)
            Files.writeString(target,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),result))
        }
    }
}
