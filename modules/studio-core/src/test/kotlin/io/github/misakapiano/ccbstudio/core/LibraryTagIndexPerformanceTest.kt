package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

/** Scoped management labels only; no Prompt tags, filesystem, image decoding or UI. */
class LibraryTagIndexPerformanceTest {
    private fun image(index:Int)=UUID(3L,index.toLong()+1).toString()
    @Test fun measuredActualTagIndexCountsScopedUniqueAssetsWithinStorageBudget() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(128,1024,8192)) {
            val labels=List(count){i->image(i) to AssetLabels(tags=List(16){t->"tag${(i+t)%64}"})}.toMap()
            val state=LibraryState(labels=labels);val before=documentJson.encodeToString(state).toByteArray()
            assertTrue(before.size<=4*1024*1024)
            val unique=List(count,::image);val scope=unique+unique.filterIndexed{i,_->i%4==0}+image(count)
            val expected=(0 until 64).associate{"tag$it" to count/64*16}
            fun verify(index:LibraryTagIndex) {
                assertEquals(count+1,index.assetCount);assertEquals(expected,index.counts)
                assertEquals((0 until 64).map{"tag$it"},index.counts.keys.toList())
                assertContentEquals(before,documentJson.encodeToString(state).toByteArray())
            }
            verify(LibraryTagIndex(scope,labels))
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=LibraryTagIndex(scope,labels)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject {put("uniqueLabeledAssets",count);put("scopeReferences",scope.size);put("tagsPerAsset",16);put("distinctTags",64);put("serializedBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("TAG_INDEX_ALLOCATION="+JsonArray(rows))
    }
}
