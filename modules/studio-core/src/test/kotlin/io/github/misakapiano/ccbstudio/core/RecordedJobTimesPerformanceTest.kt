package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

private fun recordedJobTimes(assets:List<AssetStore.Asset>,jobs:List<JobRecord>):Map<String,Long> {
    val imported=assets.mapNotNull{a->runCatching {
        val opaque=a.request().recipe()
        if(opaque.size>16384)return@runCatching null
        val receipt=Json.parseToJsonElement(opaque.decodeToString()).jsonObject
        if(receipt["kind"]?.jsonPrimitive?.content!="image-ingress" || receipt["version"]?.jsonPrimitive?.intOrNull!=1)return@runCatching null
        a.request().id().toString() to requireNotNull(receipt["createdAt"]?.jsonPrimitive?.longOrNull)
    }.getOrNull()}.toMap()
    return imported+jobs.flatMap{job->(job.results+job.composedResults).map{it to job.createdAt}}.toMap()
}

/** Pure job-result date metadata, no provider/queue execution/assets/filesystem. */
class RecordedJobTimesPerformanceTest {
    private fun image(i:Int)=UUID(12L,i.toLong()+1).toString()
    @Test fun measuredActualResultDatesKeepFirstKeyOrderAndLastJobValue() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(128,512,2048)) {
            val account=AccountProfile(id=UUID(13L,1L).toString(),alias="synthetic-only",service="NAI",provider="SYNTHETIC",model="synthetic")
            val jobs=(0 until count).shuffled(Random(179+count)).map{i->JobRecord(id=UUID(14L,i.toLong()+1).toString(),account=account,recipe="synthetic-unused",state=if(i%2==0)"COMPLETED"else"FAILED",
                results=if(i%7==0)emptyList()else List(4){t->image((i*3+t)%(count*2))},composedResults=List(2){t->image((i+t+count/2)%(count*2))},createdAt=1_700_000_000_000L+i)}
            val state=StudioState(version=3,accounts=listOf(account),jobs=jobs);state.validate()
            val before=documentJson.encodeToString(state).toByteArray();assertTrue(before.size<=4*1024*1024)
            val expectedOrder=linkedSetOf<String>();jobs.forEach{job->job.results.forEach{expectedOrder.add(it)};job.composedResults.forEach{expectedOrder.add(it)}}
            val expected=expectedOrder.associateWith{id->jobs.last{job->id in job.results||id in job.composedResults}.createdAt}
            fun verify(actual:Map<String,Long>){assertEquals(expected,actual);assertEquals(expectedOrder.toList(),actual.keys.toList());assertContentEquals(before,documentJson.encodeToString(state).toByteArray())}
            repeat(2){verify(recordedJobTimes(emptyList(),jobs));verify(libraryRecordedTimes(emptyList(),jobs))}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=if(kind=="recorded")recordedJobTimes(emptyList(),jobs)else libraryRecordedTimes(emptyList(),jobs)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("kind",kind);put("jobs",count);put("resultReferences",jobs.sumOf{it.results.size+it.composedResults.size});put("dates",actual.size);put("serializedStateBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("RECORDED_JOB_TIMES="+JsonArray(rows))
    }
    @Test fun importsOverlapsEmptyResultsAndIndependentSnapshotsKeepOrderAndDates() {
        fun asset(i:Int,time:Long):AssetStore.Asset {
            val receipt=buildJsonObject{put("kind","image-ingress");put("version",1);put("createdAt",time)}.toString().toByteArray()
            return AssetStore.Asset(AssetStore.Request(UUID.fromString(image(i)),"synthetic-$i.png",AssetStore.Kind.ORIGINAL,null,receipt,byteArrayOf()),0,"0".repeat(64))
        }
        val assets=listOf(asset(0,10),asset(1,-5),asset(0,30));val before=assets.map{it.request().recipe()}
        val account=AccountProfile(alias="synthetic-only",service="NAI",provider="SYNTHETIC",model="synthetic")
        val jobs=listOf(JobRecord(account=account,recipe="synthetic-unused",state="COMPLETED",results=listOf(image(1),image(2),image(2)),composedResults=listOf(image(0),image(3)),createdAt=100),
            JobRecord(account=account,recipe="synthetic-unused",state="FAILED",results=listOf(image(2),image(4)),composedResults=listOf(image(1),image(2)),createdAt=-200))
        val state=StudioState(version=3,accounts=listOf(account),jobs=jobs);state.validate();val stateBefore=documentJson.encodeToString(state)
        val expected=linkedMapOf(image(0) to 100L,image(1) to -200L,image(2) to -200L,image(3) to 100L,image(4) to -200L)
        val first=libraryRecordedTimes(assets,jobs)
        assertEquals(expected,first);assertEquals(expected.keys.toList(),first.keys.toList())
        assertEquals(recordedJobTimes(assets,jobs),first)
        assertTrue(libraryRecordedTimes(emptyList(),emptyList()).isEmpty())
        assertEquals(linkedMapOf(image(0) to 30L,image(1) to -5L),libraryRecordedTimes(assets,emptyList()))
        assertEquals(recordedJobTimes(assets,jobs.reversed()),libraryRecordedTimes(assets,jobs.reversed()))
        val next=libraryRecordedTimes(assets,jobs.map{it.copy(createdAt=it.createdAt+1)})
        assertEquals(expected.mapValues{it.value+1},next);assertEquals(expected,first)
        assertEquals(stateBefore,documentJson.encodeToString(state));assets.forEachIndexed{i,a->assertContentEquals(before[i],a.request().recipe())}
    }
}
