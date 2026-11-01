package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlinx.serialization.json.*
import kotlin.test.*

class CollectionPausePolicyTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun pausingOneFolderPersistsWithoutChangingOtherBindingsAndStoppingKeepsAssetsAndAlbums() {
        val root=root();val first=Files.createDirectories(root.resolveSibling(root.fileName.toString()+"-A"));val second=Files.createDirectories(root.resolveSibling(root.fileName.toString()+"-B"));val raw=EditorRenderer.synthetic(35,40);val source=Files.write(first.resolve("keep.png"),raw)
        var firstId="";var secondId="";var asset="";var album=""
        StudioWorkspace.openStudio(root).use{w->val repo=LibraryRepository(w)
            firstId=repo.addFolder(first,"images",false).folders.single().id;secondId=repo.addFolder(second,"images",false).folders.last().id
            asset=repo.scan(firstId).single().assetId!!;val a=VirtualAlbum(name="keep-album",images=listOf(asset));album=a.id;repo.update{it.copy(virtualAlbums=listOf(a))}
            repo.pauseFolder(firstId,true);assertTrue(repo.isFolderPaused(firstId));assertEquals(listOf(secondId),repo.automaticFolders().map{it.id})
            val bindings=Json.parseToJsonElement(repo.load().preferences.getValue("collectionBindings")).jsonObject
            assertFalse(firstId in bindings);assertTrue(secondId in bindings)
            repo.pauseCollections(true);assertTrue(repo.automaticFolders().isEmpty());repo.pauseCollections(false);assertEquals(listOf(secondId),repo.automaticFolders().map{it.id})
        }
        StudioWorkspace.openStudio(root).use{w->val repo=LibraryRepository(w);assertTrue(repo.isFolderPaused(firstId));assertEquals(listOf(secondId),repo.automaticFolders().map{it.id})
            repo.pauseFolder(firstId,false);assertEquals(setOf(firstId,secondId),repo.automaticFolders().map{it.id}.toSet());repo.pauseFolder(firstId,true);repo.removeFolder(firstId)
            assertEquals(listOf(secondId),repo.automaticFolders().map{it.id});assertFalse(repo.isFolderPaused(firstId));assertEquals(setOf(asset),VirtualAlbums.members(repo.load().virtualAlbums,album));assertContentEquals(raw,w.bytes(asset));assertContentEquals(raw,Files.readAllBytes(source))
        }
    }
    @Suppress("DEPRECATION")
    @Test fun pausedLegacyCopyCanMigrateToReferencesWithoutLeavingAPauseOrBinding() {
        StudioWorkspace.openStudio(root()).use{w->val repo=LibraryRepository(w);val raw=EditorRenderer.synthetic(37,40);val id=w.importImage("legacy.png",raw);val copy=repo.retainIngress(id);val folder=repo.load().folders.single().id
            repo.pauseFolder(folder,true);val result=repo.migrateIngressCopiesToReferences(folder)
            assertFalse(Files.exists(copy));assertTrue(repo.isReferenceFolder(folder));assertFalse(repo.isFolderPaused(folder));assertTrue(repo.automaticFolders().isEmpty());assertEquals(setOf(id),VirtualAlbums.members(repo.load().virtualAlbums,result.albumId));assertContentEquals(raw,w.bytes(id));assertFails{repo.pauseFolder(folder,false)}
        }
    }
}
