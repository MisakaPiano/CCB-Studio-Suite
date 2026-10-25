package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

/** Tag-album metadata only. No image bytes, Prompt requests, provider, window or filesystem. */
class TagAlbumDefaultsPerformanceTest {
    private fun id(i:Int)=UUID(11L,i.toLong()+1).toString()
    @Test fun measuredActualTagAlbumsPreserveLabelsKeysDefaultsAndInputSnapshots() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(128,512,2048)) {
            val ordered=(0 until count).shuffled(Random(163+count)).map(::id)
            val ids=ordered+ordered.filterIndexed{i,_->i%17==0}
            val labels=(0 until count).filter{it%13!=0}.associate{i->id(i) to AssetLabels(tags=if(i%11==0)emptyList() else List(8){t->"tag${(i+t)%64}"})}
            val state=LibraryState(labels=labels);val before=documentJson.encodeToString(state).toByteArray()
            assertTrue(before.size<=4*1024*1024);assertTrue(labels.size<=20000)
            labels.forEach{(key,l)->UUID.fromString(key);assertTrue(l.tags.size<=32&&l.tags.distinct().size==l.tags.size&&l.tags.all{it.isNotBlank()&&it.length<=64})}
            val expected=linkedMapOf<String,MutableList<String>>()
            for(key in ordered)for(tag in labels[key]?.tags.orEmpty().ifEmpty{listOf("")})expected.getOrPut(tag){mutableListOf()}.add(key)
            val defaults=NovelAiGenerationRecipe()
            fun verify(albums:List<NovelAiHistoryAlbum>,clockBefore:Long,clockAfter:Long) {
                assertEquals(expected.keys.map{it.ifEmpty{"未标记（无标签）"}},albums.map{it.label})
                albums.zip(expected.entries).forEach{(album,entry)->
                    assertEquals(entry.value,album.images.map{it.image.path})
                    album.images.forEach{item->
                        assertEquals(defaults,item.entry.recipe);assertTrue(item.entry.createdAt in clockBefore..clockAfter)
                        assertEquals(0L,item.image.seed);assertEquals(0,item.batchImageIndex)
                        assertEquals(listOf(item.image),item.entry.images)
                        assertEquals("tag:${sha256(entry.key.toByteArray())}:${item.image.path}",item.entry.id)
                        assertEquals(item.entry.id+'\u0000'+item.image.path,item.key)
                    }
                }
                assertContentEquals(before,documentJson.encodeToString(state).toByteArray())
            }
            repeat(2){val clock=System.currentTimeMillis();val albums=galleryTagAlbums(ids,labels);verify(albums,clock,System.currentTimeMillis())}
            repeat(4){round->
                val clock=System.currentTimeMillis();val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val albums=galleryTagAlbums(ids,labels)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated;val clockAfter=System.currentTimeMillis()
                verify(albums,clock,clockAfter)
                rows+=buildJsonObject{put("assets",count);put("memberships",expected.values.sumOf{it.size});put("serializedStateBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("TAG_ALBUM_DEFAULTS="+JsonArray(rows))
    }
}
