package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.ui.imageprompt.*
import com.example.chatbar.domain.image.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class GalleryFoldAtomicPreferenceTest {
    @Test fun emptyHistoryIsNotAnIntegrityAuditAndCatalogStillRejectsCorruption() {
        val root=Path.of(System.getProperty("preview.test.root"),newId())
        StudioWorkspace.openStudio(root).use{w->
            val id=w.importImage("synthetic.png",EditorRenderer.synthetic(32,32))
            val record=root.resolve("assets/records/$id.rec")
            assertTrue(Files.isRegularFile(record))
            Files.write(record,byteArrayOf(0,1))
            assertTrue(w.state.jobs.isEmpty())
            assertEquals(GalleryHistoryProjection(emptyList(),emptyMap()),readGalleryHistory(w))
            assertFails{w.catalog()};assertFails{w.bytes(id)}
            assertContentEquals(byteArrayOf(0,1),Files.readAllBytes(record))
        }
    }

    @Test fun namespacedFoldDirectoryAndTagsCommitTogetherAndRejectInvalidDepthBeforeWriting() {
        val root=Path.of(System.getProperty("preview.test.root"),newId())
        val value=NovelAiHistoryFoldPreference(0,true,NovelAiHistoryFoldType.BASE)
        StudioWorkspace.openStudio(root).use{w->
            val repo=LibraryRepository(w);repo.update{it.copy(preferences=mapOf("other" to "retained"))}
            val preference=GalleryFoldPreferences(repo,"archive")
            val before=w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size
            preference.save(value,true,setOf(0,2))
            assertEquals(before+1,w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size)
            assertEquals(value,preference.load().getValue(0));assertEquals(setOf(0),preference.directoryDepths())
            val saved=repo.load();assertEquals("[0,2]",saved.preferences["archive.galleryTagFoldDepths"]);assertEquals("retained",saved.preferences["other"])
            assertFailsWith<IllegalArgumentException>{preference.save(value,false,setOf(-1))}
            assertEquals(saved,repo.load())
            // Existing callers without tagDepths must preserve the tag setting.
            preference.save(value,false);assertEquals("[0,2]",repo.load().preferences["archive.galleryTagFoldDepths"])
            assertTrue(preference.directoryDepths().isEmpty())
        }
        StudioWorkspace.openStudio(root).use{w->
            val preference=GalleryFoldPreferences(LibraryRepository(w),"archive")
            assertEquals(value,preference.load().getValue(0));assertTrue(preference.directoryDepths().isEmpty())
            assertEquals("[0,2]",LibraryRepository(w).load().preferences["archive.galleryTagFoldDepths"])
        }
    }
}
