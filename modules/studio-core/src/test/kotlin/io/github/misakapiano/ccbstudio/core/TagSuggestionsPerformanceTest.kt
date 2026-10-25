package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

private fun legacySuggestions(counts:Map<String,Int>,text:String):List<String> = counts.keys.filter{it.contains(text.trim(),ignoreCase=true)}
        .sortedWith { a,b->
            val prefixA=a.startsWith(text.trim(),true);val prefixB=b.startsWith(text.trim(),true)
            if(prefixA!=prefixB)if(prefixA)-1 else 1 else galleryNaturalCompare(a,b)
        }

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
            repeat(2){verify(legacySuggestions(index.counts,query));verify(index.suggestions(query))}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=if(kind=="legacy")legacySuggestions(index.counts,query)else index.suggestions(query)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("kind",kind);put("assets",count);put("tagCount",index.counts.size);put("matchedTags",actual.size);put("serializedBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("TAG_SUGGESTIONS_ALLOCATION="+JsonArray(rows))
    }
    @Test fun callLocalNaturalComparatorMatchesEveryPairAndPreservesStableTies() {
        val values=listOf("","tag0","TAG0","tag00","tag2","tag02","tag10","stage2","stage10","生日2","生日10","Ω2","ω02","tag"+"0".repeat(40)+"2","tag9x","tag9X","tag٩","tag٩٠")
        val comparator=galleryNaturalComparator()
        values.forEach{a->values.forEach{b->assertEquals(galleryNaturalCompare(a,b),comparator.compare(a,b))}}
        assertEquals(values.sortedWith(Comparator{a,b->galleryNaturalCompare(a,b)}),values.sortedWith(galleryNaturalComparator()))
        assertEquals(listOf("tag2","TAG02","tag0002"),listOf("tag2","TAG02","tag0002").sortedWith(galleryNaturalComparator()))
    }
    @Test fun suggestionQueriesRetainPrefixCaseUnicodeAndIndependentScopeWithoutMutation() {
        val tags=listOf("xabc10","ABC2","xabc","abc","生日10","生日2","abc02","abc2")
        val labels=mapOf("a" to AssetLabels(tags=tags),"hidden" to AssetLabels(tags=listOf("not-in-scope")))
        val index=LibraryTagIndex(listOf("a","a","missing"),labels);val before=documentJson.encodeToString(labels);val counts=index.counts.toMap()
        assertEquals(listOf("abc","ABC2","abc02","abc2","xabc","xabc10"),index.suggestions(" abc "))
        for(query in listOf(""," ","abc"," AbC ","2","生日","unmatched"))assertEquals(legacySuggestions(index.counts,query),index.suggestions(query))
        assertEquals(listOf("newtag"),LibraryTagIndex(listOf("other"),mapOf("other" to AssetLabels(tags=listOf("newtag")))).suggestions("tag"))
        assertTrue(index.suggestions("newtag").isEmpty());assertTrue(LibraryTagIndex(emptyList(),labels).suggestions("").isEmpty())
        assertEquals(counts,index.counts);assertEquals(counts.keys.toList(),index.counts.keys.toList());assertEquals(before,documentJson.encodeToString(labels))
    }
}
