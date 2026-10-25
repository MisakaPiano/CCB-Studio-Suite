package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

/** Catalog metadata only. No claimed image payload, store admission or original integrity test. */
class LibraryRecordedTimesPerformanceTest {
    private fun id(index:Int)=UUID(4L,index.toLong()+1)
    private fun receipt(time:Long,padding:Int)=buildJsonObject {
        put("kind","image-ingress");put("version",1);put("source","file")
        put("sourcePath","H:/CCB-Studio-Suite/.local/synthetic-only/image.png")
        put("createdAt",time);put("padding","p".repeat(padding))
    }.toString().toByteArray()
    private fun metadata(index:Int,bytes:ByteArray)=AssetStore.Asset(AssetStore.Request(id(index),"synthetic$index.png",AssetStore.Kind.ORIGINAL,null,bytes,byteArrayOf()),1,"0".repeat(64))
    @Test fun measuredActualRecordedTimesUsesCatalogMetadataWithoutPayloadReads() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for((count,padding) in listOf(128 to 256,1024 to 1024,1024 to 12288)) {
            val startTime=1_700_000_000_000L
            val assets=List(count){i->metadata(i,receipt(startTime+i,padding))}
            val before=assets.map{it.request().recipe()};assertTrue(before.all{it.size<=16384})
            val expected=(0 until count).associate{id(it).toString() to startTime+it}
            fun verify(actual:Map<String,Long>) {
                assertEquals(expected,actual);assertEquals(expected.keys.toList(),actual.keys.toList())
                assets.forEachIndexed{i,a->assertContentEquals(before[i],a.request().recipe())}
            }
            verify(libraryRecordedTimes(assets,emptyList()))
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=libraryRecordedTimes(assets,emptyList())
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject {put("assets",count);put("paddingBytes",padding);put("recipeBytes",before[0].size);put("totalRecipeBytes",before.sumOf{it.size.toLong()});put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("RECORDED_TIMES_ALLOCATION="+JsonArray(rows))
    }
}
