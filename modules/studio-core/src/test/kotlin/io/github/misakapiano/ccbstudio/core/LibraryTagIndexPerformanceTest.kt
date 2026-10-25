package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

private class LegacyLibraryTagIndex(ids:Collection<String>,labels:Map<String,AssetLabels>) {
    val assetCount=ids.toSet().size
    val counts:Map<String,Int> = ids.toSet().flatMap{labels[it]?.tags.orEmpty().distinct()}.groupingBy{it}.eachCount()
    fun suggestions(text:String):List<String> = counts.keys.filter{it.contains(text.trim(),ignoreCase=true)}
        .sortedWith { a,b->
            val prefixA=a.startsWith(text.trim(),true);val prefixB=b.startsWith(text.trim(),true)
            if(prefixA!=prefixB)if(prefixA)-1 else 1 else galleryNaturalCompare(a,b)
        }
}

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
            fun verify(assetCount:Int,counts:Map<String,Int>) {
                assertEquals(count+1,assetCount);assertEquals(expected,counts)
                assertEquals((0 until 64).map{"tag$it"},counts.keys.toList())
                assertContentEquals(before,documentJson.encodeToString(state).toByteArray())
            }
            repeat(2){val legacy=LegacyLibraryTagIndex(scope,labels);verify(legacy.assetCount,legacy.counts);val current=LibraryTagIndex(scope,labels);verify(current.assetCount,current.counts)}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=if(kind=="legacy")LegacyLibraryTagIndex(scope,labels)else LibraryTagIndex(scope,labels)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                when(actual){is LegacyLibraryTagIndex->verify(actual.assetCount,actual.counts);is LibraryTagIndex->verify(actual.assetCount,actual.counts);else->error("Unknown measurement mode")}
                rows+=buildJsonObject {put("kind",kind);put("uniqueLabeledAssets",count);put("scopeReferences",scope.size);put("tagsPerAsset",16);put("distinctTags",64);put("serializedBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("TAG_INDEX_ALLOCATION="+JsonArray(rows))
    }
    @Test fun duplicateReferencesTagsCaseAndSuggestionOrderMatchLegacyWithoutMutation() {
        val labels=linkedMapOf(
            "a" to AssetLabels(alias="ignored alias",tags=listOf("Tag2","tag2","Tag10","Tag2")),
            "b" to AssetLabels(tags=listOf("tag10","Tag2","中文","tag0")),
            "hidden" to AssetLabels(tags=listOf("outside scope")))
        val scope=listOf("b","b","missing","a");val before=documentJson.encodeToString(labels)
        val index=LibraryTagIndex(scope,labels);val legacy=LegacyLibraryTagIndex(scope,labels)
        assertEquals(3,index.assetCount)
        assertEquals(linkedMapOf("tag10" to 1,"Tag2" to 2,"中文" to 1,"tag0" to 1,"tag2" to 1,"Tag10" to 1),index.counts)
        assertEquals(listOf("tag10","Tag2","中文","tag0","tag2","Tag10"),index.counts.keys.toList())
        assertEquals(legacy.counts,index.counts);assertEquals(legacy.counts.keys.toList(),index.counts.keys.toList())
        assertEquals(listOf("tag0","Tag2","tag2","tag10","Tag10"),index.suggestions(" taG "))
        for(query in listOf(""," ","taG","2","中文","unmatched"))assertEquals(legacy.suggestions(query),index.suggestions(query))
        assertEquals(before,documentJson.encodeToString(labels));assertEquals(listOf("b","b","missing","a"),scope)
    }
    @Test fun emptyAndRebuiltScopesDoNotRetainOtherScopesOrMutableInput() {
        val tags=mutableListOf("first");val labels=mutableMapOf("a" to AssetLabels(tags=tags),"b" to AssetLabels(tags=listOf("second")))
        val first=LibraryTagIndex(listOf("a","a"),labels)
        tags+="new";labels["b"]=AssetLabels(tags=listOf("changed"))
        val second=LibraryTagIndex(listOf("a","b"),labels)
        assertEquals(1,first.assetCount);assertEquals(mapOf("first" to 1),first.counts);assertTrue(first.suggestions("new").isEmpty())
        assertEquals(2,second.assetCount);assertEquals(linkedMapOf("first" to 1,"new" to 1,"changed" to 1),second.counts)
        val missing=LibraryTagIndex(listOf("unknown","unknown"),labels)
        assertEquals(1,missing.assetCount);assertTrue(missing.counts.isEmpty());assertTrue(missing.suggestions("").isEmpty())
        val empty=LibraryTagIndex(emptyList(),labels)
        assertEquals(0,empty.assetCount);assertTrue(empty.counts.isEmpty());assertTrue(empty.suggestions("").isEmpty())
    }
}
