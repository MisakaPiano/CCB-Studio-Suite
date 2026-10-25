package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

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
            repeat(2){verify(libraryRecordedTimes(emptyList(),jobs))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=libraryRecordedTimes(emptyList(),jobs)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("jobs",count);put("resultReferences",jobs.sumOf{it.results.size+it.composedResults.size});put("dates",actual.size);put("serializedStateBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("RECORDED_JOB_TIMES="+JsonArray(rows))
    }
}
