package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

private fun recordedTagAlbums(ids:List<String>,labels:Map<String,AssetLabels>):List<NovelAiHistoryAlbum> {
    val groups=linkedMapOf<String,MutableList<NovelAiHistoryImageItem>>()
    val tagHashes=mutableMapOf<String,String>()
    ids.distinct().forEach{id->
        labels[id]?.tags.orEmpty().ifEmpty{listOf("")}.forEach{tag->
            val image=NovelAiGenerationHistoryImage(path=id)
            val hash=tagHashes.getOrPut(tag){sha256(tag.toByteArray())}
            val item=NovelAiHistoryImageItem(NovelAiGenerationHistoryEntry(id="tag:$hash:$id",images=listOf(image)),image,0)
            groups.getOrPut(tag){mutableListOf()}.add(item)
        }
    }
    return groups.map{(tag,images)->NovelAiHistoryAlbum(images,tag.ifEmpty{"未标记（无标签）"})}
}

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
            repeat(2){for(kind in listOf("recorded","current")){val clock=System.currentTimeMillis();val albums=if(kind=="recorded")recordedTagAlbums(ids,labels)else galleryTagAlbums(ids,labels);verify(albums,clock,System.currentTimeMillis())}}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                val clock=System.currentTimeMillis();val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val albums=if(kind=="recorded")recordedTagAlbums(ids,labels)else galleryTagAlbums(ids,labels)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated;val clockAfter=System.currentTimeMillis()
                verify(albums,clock,clockAfter)
                rows+=buildJsonObject{put("kind",kind);put("assets",count);put("memberships",expected.values.sumOf{it.size});put("serializedStateBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("TAG_ALBUM_DEFAULTS="+JsonArray(rows))
    }
    @Test fun emptyMissingAndRepeatedInputsKeepLabelsMembershipAndRecipeValues() {
        assertTrue(galleryTagAlbums(emptyList(),emptyMap()).isEmpty())
        val labels=mapOf("a" to AssetLabels(tags=listOf("one","two")),"b" to AssetLabels())
        val result=galleryTagAlbums(listOf("a","b","a","c"),labels)
        assertEquals(listOf("one","two","未标记（无标签）"),result.map{it.label})
        assertEquals(listOf(listOf("a"),listOf("a"),listOf("b","c")),result.map{a->a.images.map{it.image.path}})
        fun signature(albums:List<NovelAiHistoryAlbum>)=albums.map{a->a.label to a.images.map{it.copy(entry=it.entry.copy(createdAt=0))}}
        assertEquals(signature(recordedTagAlbums(listOf("a","b","a","c"),labels)),signature(result))
        val doubled=mapOf("x" to AssetLabels(tags=listOf("same","same")))
        assertEquals(2,galleryTagAlbums(listOf("x"),doubled).single().images.size) // Preserve helper behavior, not an admitted-label claim.
    }
    @Test fun copiedMetadataCannotChangeSiblingRecipesOrAnotherProjection() {
        val labels=mapOf("a" to AssetLabels(tags=listOf("one","two")))
        val first=galleryTagAlbums(listOf("a"),labels);val item=first.first().images.single()
        val changed=item.entry.recipe.copy(stylePrompt="Synthetic changed copy",settings=item.entry.recipe.settings.copy(steps=31))
        assertNotEquals(changed,item.entry.recipe)
        first.flatMap{it.images}.forEach{assertEquals(NovelAiGenerationRecipe(),it.entry.recipe)}
        val next=galleryTagAlbums(listOf("a"),labels)
        assertNotSame(item.entry.recipe,next.first().images.single().entry.recipe)
        next.flatMap{it.images}.forEach{assertEquals(NovelAiGenerationRecipe(),it.entry.recipe)}
        assertEquals(listOf("one","two"),labels.getValue("a").tags)
    }
}
