package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

/** Management-label suggestion sorting only; query setup and index construction are not timed. */
class TagSuggestionsPerformanceTest {
    @Test fun measuredActualSuggestionsPreservePrefixPriorityAndNaturalOrderWithinStorageBudget() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(128,1024,4096)) {
            val labels=(0 until count).shuffled(Random(71+count)).associate {i->UUID(5L,i.toLong()+1).toString() to AssetLabels(tags=listOf("tag$i","stage$i","shared"))}
            val state=LibraryState(labels=labels);val before=documentJson.encodeToString(state).toByteArray();assertTrue(before.size<=4*1024*1024)
            val index=LibraryTagIndex(labels.keys,labels);val expected=List(count){"tag$it"}+List(count){"stage$it"};val query=" tag "
            fun verify(actual:List<String>) {assertEquals(expected,actual);assertContentEquals(before,documentJson.encodeToString(state).toByteArray())}
            repeat(2){verify(index.suggestions(query))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=index.suggestions(query)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("assets",count);put("tagCount",index.counts.size);put("matchedTags",actual.size);put("serializedBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("TAG_SUGGESTIONS_ALLOCATION="+JsonArray(rows))
    }
}
