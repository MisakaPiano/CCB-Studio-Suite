package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.nio.file.*
import java.util.*
import java.lang.management.ManagementFactory
import kotlin.test.*

class GalleryPerformanceTest {
    private fun signature(albums:List<com.example.chatbar.ui.imageprompt.NovelAiHistoryAlbum>)=albums.map{a->a.label to a.images.map{listOf(it.entry.id,it.image.path,it.key)}}
    private fun assets(count:Int):List<AssetStore.Asset> = (0 until count).map { n->
        val name=listOf("image$n.png","IMAGE00$n.PNG","生日$n.webp","file${n%31}.jpg", "no-extension")[n%5]
        AssetStore.Asset(AssetStore.Request(UUID(0,n.toLong()+1),name,AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),(n%97).toLong(),"0".repeat(64))
    }.shuffled(kotlin.random.Random(107))
    @Test fun everySortKeepsLegacyOrderingIncludingAliasesTiesAndLongNumbers() {
        val assets=assets(500);val ids=assets.map{it.request().id().toString()}
        val names=ids.mapIndexed{i,id->id to listOf("", " ","image000000000000000000000000000000000002.png","image2.png","École100.png","生日９.png")[i%6]}.toMap()
        val added=ids.filterIndexed{i,_->i%3!=0}.associateWith{42L}
        GallerySortField.entries.forEach{field->listOf(false,true).forEach{descending->
            val sort=GallerySort(field,descending)
            assertEquals(legacySortGalleryAssets(assets,sort,added,names),sortGalleryAssets(assets,sort,added,names))
        }}
        val changed=names+(ids.first() to "new1")
        assertEquals(legacySortGalleryAssets(assets,GallerySort(GallerySortField.NAME,false),added,changed),sortGalleryAssets(assets,GallerySort(GallerySortField.NAME,false),added,changed))
    }
    @Test fun preparedDatesAndLinearSelectionPreserveFilterAndOutsideSelectionSemantics() {
        val dates=listOf("","2026","2026-02","2026-02-28","2026-02-30","bad date")
        dates.forEach{date->val predicate=libraryDatePredicate(date);listOf(null,0L,1772236800000L,1792713600000L).forEach{time->assertEquals(runCatching{libraryDateMatches(time,date)}.getOrDefault(false),predicate(time))}}
        val all=(0..300).map{"id-$it"};val active=all.filterIndexed{i,_->i%5!=0}.toSet();val selected=all.filterIndexed{i,_->i%3==0}.toSet()+"unknown"
        val visible=all.take(75).reversed()+all.take(5)
        assertEquals((visible+all).distinct().filter{it in selected&&it in active.toList()},orderedLibrarySelection(visible,all,active,selected))
        assertTrue(orderedLibrarySelection(visible,all,active,emptySet()).isEmpty())
    }
    @Test fun tagGroupingRetainsExactKeysOrderCountsAndUntaggedItems() {
        val ids=(0..100).map{"id-$it"};val labels=ids.filterIndexed{i,_->i%3!=0}.associateWith{AssetLabels(tags=listOf("生日","旅行"))}
        assertEquals(signature(legacyTagAlbums(ids,labels)),signature(galleryTagAlbums(ids,labels)))
    }
    @Test fun tenThousandMetadataBenchmarkWithEquivalentResults() {
        val assets=assets(10000);val ids=assets.map{it.request().id().toString()};val active=ids.toSet();val selected=ids.filterIndexed{i,_->i%2==0}.toSet()
        val labels=ids.associateWith{AssetLabels(tags=listOf("生日","旅行","待编辑"))}
        val sort=GallerySort(GallerySortField.NAME,false);val measurements=mutableListOf<JsonObject>()
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        fun measure(phase:String,operation:()->Any):Any {
            val thread=Thread.currentThread().id;val cpu=bean.currentThreadCpuTime;val allocated=bean.getThreadAllocatedBytes(thread);val start=System.nanoTime()
            val result=operation()
            measurements+=buildJsonObject{put("phase",phase);put("wallMs",(System.nanoTime()-start)/1e6);put("threadCpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
            return result
        }
        // Warm only the same small input for both implementations; large corpus remains measurement work.
        repeat(2){legacySortGalleryAssets(assets.take(100),sort,emptyMap());sortGalleryAssets(assets.take(100),sort,emptyMap())}
        assertEquals(measure("106-name-sort"){legacySortGalleryAssets(assets,sort,emptyMap())},measure("107-name-sort"){sortGalleryAssets(assets,sort,emptyMap())})
        val added=ids.withIndex().associate{it.value to it.index.toLong()}
        assertEquals(measure("106-added-sort"){legacySortGalleryAssets(assets,GallerySort(),added)},measure("107-added-sort"){sortGalleryAssets(assets,GallerySort(),added)})
        assertEquals(measure("106-search-sort"){legacySortGalleryAssets(assets,sort,emptyMap()).filter{it.request().name().contains("123")}},measure("107-search-sort"){sortGalleryAssets(assets.filter{it.request().name().contains("123")},sort,emptyMap())})
        assertEquals(measure("106-selection"){(ids+ids).distinct().filter{it in selected&&it in ids}},measure("107-selection"){orderedLibrarySelection(ids,ids,active,selected)})
        @Suppress("UNCHECKED_CAST")
        val old=measure("106-tag-fold"){legacyTagAlbums(ids,labels)} as List<com.example.chatbar.ui.imageprompt.NovelAiHistoryAlbum>
        @Suppress("UNCHECKED_CAST")
        val new=measure("107-tag-fold"){galleryTagAlbums(ids,labels)} as List<com.example.chatbar.ui.imageprompt.NovelAiHistoryAlbum>
        assertEquals(signature(old),signature(new))
        val result=buildJsonObject{put("syntheticOnly",true);put("assets",assets.size);put("scope","in-memory metadata calculations; excludes disk validation, UI frames and process RSS");put("java",System.getProperty("java.version"));put("heapLimitBytes",Runtime.getRuntime().maxMemory());put("measurements",JsonArray(measurements));put("humanAcceptance","NOT TESTED")}
        val out=Path.of("build/performance-107/catalog-comparison.json");Files.createDirectories(out.parent);Files.writeString(out,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),result))
    }
}
