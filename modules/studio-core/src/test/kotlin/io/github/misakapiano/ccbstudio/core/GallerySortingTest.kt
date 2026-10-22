package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.NovelAiHistoryFoldType
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.test.*

class GallerySortingTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun naturalOrderIndependentAlbumsAndNewArrivalsKeepPreferencesAcrossReopen() {
        val root=root();var first="";var second="";val original=EditorRenderer.synthetic(16,16)
        StudioWorkspace.openStudio(root).use{w->
            fun add(dir:String,name:String)=w.importImage(name,original,buildJsonObject{put("kind","image-ingress");put("version",1);put("source","file");put("sourcePath",root.resolve(dir).resolve(name).toString())}.toString())
            val ids=listOf(add("A","image10.png"),add("A","image2.png"),add("B","image1.png"),add("B","image20.jpg"))
            fun identity(album:com.example.chatbar.ui.imageprompt.NovelAiHistoryAlbum)=galleryAlbumIdentity("root",album,NovelAiHistoryFoldType.FULL,true,w.catalog())
            val groups=galleryDirectoryAlbums(ids,w.catalog(),emptyList());first=identity(groups[0]);second=identity(groups[1]);assertNotEquals(first,second)
            val prefs=GallerySortPreferences(LibraryRepository(w));prefs.save("root",GallerySort(GallerySortField.SIZE,false));prefs.save(first,GallerySort(GallerySortField.NAME,false));prefs.save(second,GallerySort(GallerySortField.TYPE,true))
            val ordered=sortGalleryAssets(w.catalog().filter{it.request().id().toString() in ids.take(2)},prefs.load().getValue(first),emptyMap())
            assertEquals(listOf("image2.png","image10.png"),ordered.map{it.request().name()})
            val newest=add("A","image100.png");val updated=galleryDirectoryAlbums(listOf(newest)+ids,w.catalog(),emptyList())
            assertEquals(first,identity(updated.first()));assertEquals(first,identity(galleryDirectoryAlbums(ids.reversed(),w.catalog(),emptyList()).last()))
            assertEquals(GallerySortField.NAME,prefs.load().getValue(first).field);ids.forEach{assertContentEquals(original,w.bytes(it))}
            assertTrue(galleryNaturalCompare("image9.png","image100.png")<0)
            assertTrue(galleryNaturalCompare("image000000000000000000002.png","image100.png")<0)
        }
        StudioWorkspace.openStudio(root).use{w->val prefs=GallerySortPreferences(LibraryRepository(w)).load();assertEquals(3,prefs.size);assertEquals(GallerySort(GallerySortField.NAME,false),prefs[first]);assertEquals(GallerySort(GallerySortField.TYPE,true),prefs[second])}
    }
    @Test fun futureSortMetadataRefusesOverwriteAndCachedFoldDecodingDoesNotReadRepository() {
        StudioWorkspace.openStudio(root()).use{w->
            val repo=LibraryRepository(w);val p=GalleryFoldPreferences(repo)
            assertTrue(p.load(LibraryState()).isEmpty());assertTrue(p.directoryDepths(LibraryState()).isEmpty())
            val raw="""{"version":2,"albums":{}}""";repo.update{it.copy(preferences=mapOf("gallerySorts" to raw))}
            assertFails{GallerySortPreferences(repo).save("root",GallerySort())}
            assertEquals(raw,repo.load().preferences["gallerySorts"])
        }
    }
}
