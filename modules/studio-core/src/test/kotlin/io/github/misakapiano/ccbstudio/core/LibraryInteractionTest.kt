package io.github.misakapiano.ccbstudio.core

import kotlin.test.*
import java.nio.file.Path

class LibraryInteractionTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun tagPredictionCountsUniqueScopedAssetsAndFiltersExactLabels() {
        val labels=mapOf("a" to AssetLabels(tags=listOf("生日","生日派对")),"b" to AssetLabels(tags=listOf("生日")),"hidden" to AssetLabels(tags=listOf("生日派对")))
        val index=LibraryTagIndex(listOf("a","a","b"),labels)
        assertEquals(mapOf("生日" to 2,"生日派对" to 1),index.counts)
        assertEquals(listOf("生日","生日派对"),index.suggestions("生"))
        assertFalse(matchesLibraryTags(listOf("生日派对"),listOf("生日")))
        assertFalse(matchesLibraryTags(listOf("生日"),listOf("生日","生日派对")))
        assertTrue(matchesLibraryTags(listOf("生日"),listOf("生日","生日派对"),true))
    }
    @Test fun rangeSelectionUsesVisibleOrderAndExplicitModifiers() {
        val order=listOf("image1","image2","image10","image20")
        assertEquals(setOf("image2"),selectLibraryRange(order,setOf("hidden"),"image2",null,false,false))
        assertEquals(setOf("hidden","image1","image2","image10"),selectLibraryRange(order,setOf("hidden"),"image10","image1",true,true))
        assertEquals(setOf("image2","image10"),selectLibraryRange(order,setOf("hidden"),"image10","image2",false,true))
        assertEquals(setOf("image1"),selectLibraryRange(order,setOf("image1","image2"),"image2",null,true,false))
    }
    @Test fun batchLabelsOneRevisionUndoConflictIsAtomicAndReopenPreservesBytes() {
        val dir=root();val bytes=EditorRenderer.synthetic(32,32);var ids=listOf<String>()
        StudioWorkspace.openStudio(dir).use{w->
            ids=(1..3).map{w.importImage("$it.png",bytes)};val repo=LibraryRepository(w)
            val prior=w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size
            val before=ids.associateWith<String,AssetLabels?>{null}
            val after=ids.associateWith{AssetLabels(alias=it.take(8),tags=listOf("生日"))}
            repo.changeLabels(before,after)
            assertEquals(prior+1,w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size)
            val changed=after+(ids[1] to after.getValue(ids[1]).copy(tags=listOf("保留")))
            repo.changeLabels(after,changed)
            assertFails{repo.changeLabels(after,before)}
            assertEquals(changed,repo.load().labels)
            repo.changeLabels(changed,before);assertTrue(repo.load().labels.isEmpty())
            repo.changeLabels(before,after)
            assertFails{repo.changeLabels(after,after+(ids[0] to AssetLabels(tags=(1..33).map{"tag$it"})))}
            assertEquals(after,repo.load().labels)
        }
        StudioWorkspace.openStudio(dir).use{w->assertEquals(ids.toSet(),LibraryRepository(w).load().labels.keys);ids.forEach{assertContentEquals(bytes,w.bytes(it))}}
    }
    @Test fun multiTagAlbumsHaveUniqueKeysAndUnknownDatesStayUnknown() {
        val labels=mapOf("a" to AssetLabels(tags=listOf("生日","生日派对")),"b" to AssetLabels())
        val albums=galleryTagAlbums(listOf("a","b"),labels)
        assertEquals(listOf("生日","生日派对","未标记（无标签）"),albums.map{it.label})
        assertEquals(3,albums.map{it.key}.toSet().size)
        assertEquals(setOf("a","b"),albums.flatMap{it.images}.map{it.image.path}.toSet())
        assertTrue(libraryDateMatches(null,""));assertFalse(libraryDateMatches(null,"2026"));assertEquals("",libraryRecordedDay(null))
        assertFails{libraryDateMatches(null,"2026-02-30")}
    }
    @Test fun galleryAndArchiveFoldPreferencesRemainIndependent() {
        StudioWorkspace.openStudio(root()).use{w->
            val repo=LibraryRepository(w);val gallery=GalleryFoldPreferences(repo);val archive=GalleryFoldPreferences(repo,"archive")
            gallery.save(com.example.chatbar.domain.image.NovelAiHistoryFoldPreference(0,true),true)
            archive.save(com.example.chatbar.domain.image.NovelAiHistoryFoldPreference(0,false),false)
            assertTrue(gallery.load().getValue(0).enabled);assertEquals(setOf(0),gallery.directoryDepths())
            assertFalse(archive.load().getValue(0).enabled);assertTrue(archive.directoryDepths().isEmpty())
        }
    }
}
