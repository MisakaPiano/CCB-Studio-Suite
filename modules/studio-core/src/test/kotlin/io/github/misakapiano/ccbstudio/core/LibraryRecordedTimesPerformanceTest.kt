package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

private fun legacyRecordedTimes(assets:List<AssetStore.Asset>,jobs:List<JobRecord>):Map<String,Long> {
    val imported=assets.mapNotNull{a->runCatching {
        if(a.request().recipe().size>16384)return@runCatching null
        val receipt=Json.parseToJsonElement(a.request().recipe().decodeToString()).jsonObject
        if(receipt["kind"]?.jsonPrimitive?.content!="image-ingress" || receipt["version"]?.jsonPrimitive?.intOrNull!=1)return@runCatching null
        a.request().id().toString() to requireNotNull(receipt["createdAt"]?.jsonPrimitive?.longOrNull)
    }.getOrNull()}.toMap()
    return imported+jobs.flatMap{job->(job.results+job.composedResults).map{it to job.createdAt}}.toMap()
}

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
            repeat(2){verify(legacyRecordedTimes(assets,emptyList()));verify(libraryRecordedTimes(assets,emptyList()))}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=if(kind=="legacy")legacyRecordedTimes(assets,emptyList())else libraryRecordedTimes(assets,emptyList())
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject {put("kind",kind);put("assets",count);put("paddingBytes",padding);put("recipeBytes",before[0].size);put("totalRecipeBytes",before.sumOf{it.size.toLong()});put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("RECORDED_TIMES_ALLOCATION="+JsonArray(rows))
    }
    @Test fun malformedOversizedUnknownReceiptsAndJobOverridesMatchLegacyWithoutPayloadAccess() {
        fun raw(text:String)=text.toByteArray()
        fun exact(size:Int,time:Long):ByteArray {val value=receipt(time,0);return value+" ".repeat(size-value.size).toByteArray()}
        val assets=listOf(
            metadata(0,receipt(10,0)),
            metadata(1,raw("""{"kind":"other","version":1,"createdAt":11}""")),
            metadata(2,raw("""{"kind":"image-ingress","version":2,"createdAt":12}""")),
            metadata(3,raw("""{"kind":"image-ingress","version":1}""")),
            metadata(4,raw("""{"kind":"image-ingress","version":1,"createdAt":"bad"}""")),
            metadata(5,raw("[]")),metadata(6,raw("{")),metadata(7,raw("null")),
            metadata(8,receipt(0,0)),metadata(9,receipt(-5,0)),
            metadata(10,exact(16384,17)),metadata(11,exact(16385,18)),metadata(12,byteArrayOf()),
            metadata(13,raw("""{"kind":"image-ingress","createdAt":19}""")),
            metadata(0,receipt(21,0)))
        val before=assets.map{it.request().recipe()}
        val account=AccountProfile(alias="synthetic-only",service="NAI",provider="SYNTHETIC",model="synthetic")
        val jobs=listOf(
            JobRecord(account=account,recipe="synthetic-unused",state="COMPLETED",results=listOf(id(0).toString()),composedResults=listOf(id(14).toString()),createdAt=100),
            JobRecord(account=account,recipe="synthetic-unused",state="FAILED",results=listOf(id(0).toString(),id(15).toString()),composedResults=listOf(id(14).toString()),createdAt=200))
        val expected=linkedMapOf(id(0).toString() to 200L,id(8).toString() to 0L,id(9).toString() to -5L,id(10).toString() to 17L,id(14).toString() to 200L,id(15).toString() to 200L)
        val actual=libraryRecordedTimes(assets,jobs)
        assertEquals(expected,actual);assertEquals(expected.keys.toList(),actual.keys.toList())
        assertEquals(legacyRecordedTimes(assets,jobs),actual)
        assertEquals(legacyRecordedTimes(assets,jobs).keys.toList(),actual.keys.toList())
        assets.forEachIndexed{i,a->assertContentEquals(before[i],a.request().recipe())}
    }
    @Test fun defensiveRecipeApiAndRebuiltRecordedDatesRemainIndependent() {
        val original=receipt(10,0);val asset=metadata(0,original);val preserved=original.copyOf()
        original.fill(0);val exposed=asset.request().recipe();exposed.fill(0)
        val first=libraryRecordedTimes(listOf(asset),emptyList())
        assertEquals(mapOf(id(0).toString() to 10L),first)
        assertContentEquals(preserved,asset.request().recipe())
        val second=libraryRecordedTimes(listOf(metadata(0,receipt(20,0))),emptyList())
        assertEquals(mapOf(id(0).toString() to 20L),second)
        assertEquals(mapOf(id(0).toString() to 10L),first)
        assertTrue(libraryRecordedTimes(emptyList(),emptyList()).isEmpty())
    }
}
