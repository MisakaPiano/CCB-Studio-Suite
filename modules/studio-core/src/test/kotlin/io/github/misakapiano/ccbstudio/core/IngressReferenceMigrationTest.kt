@file:Suppress("DEPRECATION")
package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import java.io.ByteArrayInputStream
import java.nio.file.*
import java.util.UUID
import kotlin.test.*

class IngressReferenceMigrationTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun migrationPreservesIdsLabelsProjectsAndAlbumsAndReopensWithoutCopyScanning() {
        val root=root();val raw=EditorRenderer.synthetic(30,40);var ids=emptyList<String>();var folder="";var project=""
        StudioWorkspace.openStudio(root).use{w->
            val repo=LibraryRepository(w);ids=listOf(w.importImage("a.png",raw),w.importImage("b.png",raw))
            val copies=repo.retainIngressBatch(ids);folder=repo.load().folders.single().id
            repo.label(ids[0],"keep",listOf("synthetic-tag"));project=w.newProject("retained",ids)
            val existing=VirtualAlbum(name="user-album",images=ids);repo.update{it.copy(virtualAlbums=listOf(existing))}
            val state=w.state;val result=repo.migrateIngressCopiesToReferences(folder)
            assertEquals(2,result.removedCopies);assertEquals(raw.size*2L,result.removedBytes);assertTrue(copies.none{Files.exists(it)})
            assertEquals(state,w.state);assertEquals(existing,repo.load().virtualAlbums.single{it.id==existing.id})
            assertEquals(ids.toSet(),VirtualAlbums.members(repo.load().virtualAlbums,result.albumId));assertTrue(repo.automaticFolders().isEmpty())
            assertTrue(repo.scan(folder).all{it.status=="REFERENCE_ONLY"});assertEquals(0,repo.migrateIngressCopiesToReferences(folder).removedCopies)
            ids.forEach{assertContentEquals(raw,w.bytes(it))};assertEquals("keep",repo.load().labels.getValue(ids[0]).alias)
        }
        StudioWorkspace.openStudio(root).use{w->val repo=LibraryRepository(w);assertTrue(repo.isReferenceFolder(folder));assertTrue(repo.automaticFolders().isEmpty());assertEquals(ids,w.state.projects.single{it.id==project}.current.pages.map{it.assetId});ids.forEach{assertContentEquals(raw,w.bytes(it))}}
    }
    @Test fun interruptionAfterReferenceCommitKeepsCopyAndRetryFinishesWithoutDuplicateAlbum() {
        StudioWorkspace.openStudio(root()).use{w->
            val raw=EditorRenderer.synthetic(32,40);val repo=LibraryRepository(w);val id=w.importImage("a.png",raw);val copy=repo.retainIngress(id);val folder=repo.load().folders.single().id
            val interrupted=LibraryRepository(w){if(it=="INGRESS_REFERENCES_COMMITTED")error("Synthetic interruption")}
            assertFailsWith<IllegalStateException>{interrupted.migrateIngressCopiesToReferences(folder)}
            assertTrue(Files.exists(copy));assertTrue(repo.isReferenceFolder(folder));assertEquals(1,repo.load().virtualAlbums.size)
            assertEquals(1,repo.migrateIngressCopiesToReferences(folder).removedCopies);assertEquals(1,repo.load().virtualAlbums.size);assertContentEquals(raw,w.bytes(id))
        }
    }
    @Test fun matchingFileAndObservationWithoutHistoricalCopyIntentIsNotDeletionAuthority() {
        StudioWorkspace.openStudio(root()).use{w->
            val raw=EditorRenderer.synthetic(31,40);val id=w.importImage("user-source.png",raw);val repo=LibraryRepository(w)
            val base=Files.createDirectories(w.root.resolveSibling(w.root.fileName.toString()+"-collections").resolve("imports"))
            repo.addFolder(base,"imports",false);val folder=repo.load().folders.single();val name="$id-user-source.png";val source=Files.write(base.resolve(name),raw)
            repo.update{it.copy(observations=mapOf(folder.id+":"+name to FolderObservation(id,sha256(raw),raw.size.toLong())))}
            val before=repo.load();assertFails{repo.migrateIngressCopiesToReferences(folder.id)}
            assertEquals(before,repo.load());assertContentEquals(raw,Files.readAllBytes(source));assertContentEquals(raw,w.bytes(id))
        }
    }
    @Test fun unknownChangedAndPostCommitMutationRefuseCleanupAndFutureStateIsRetained() {
        StudioWorkspace.openStudio(root()).use{w->
            val raw=EditorRenderer.synthetic(34,40);val repo=LibraryRepository(w);val id=w.importImage("a.png",raw);val copy=repo.retainIngress(id);val folder=repo.load().folders.single().id;val original=repo.load()
            val unknown=Files.writeString(copy.parent.resolve("unknown.keep"),"synthetic unknown")
            assertFails{repo.migrateIngressCopiesToReferences(folder)};assertEquals(original,repo.load());assertTrue(Files.exists(unknown));Files.delete(unknown)
            Files.write(copy,byteArrayOf(1,2,3));assertFails{repo.migrateIngressCopiesToReferences(folder)};assertEquals(original,repo.load());Files.write(copy,raw)
            assertFails{repo.migrateIngressCopiesToReferences(folder,beforeDelete={Files.write(it,byteArrayOf(4,5,6))})}
            assertContentEquals(byteArrayOf(4,5,6),Files.readAllBytes(copy));assertContentEquals(raw,w.bytes(id));assertTrue(repo.isReferenceFolder(folder))
            val revisions=w.assets.catalogProject(LibraryRepository.TAG.toByteArray());val previous=revisions.last();val revisionHeader="{\"version\":1,\"sequence\":${revisions.size+1},\"previous\":\"${previous.request().id()}\"}"
            // Sequence comes from the checked latest revision, not catalog UUID ordering.
            val latest=revisions.maxBy{kotlinx.serialization.json.Json.parseToJsonElement(it.request().recipe().decodeToString()).let{j->(j as kotlinx.serialization.json.JsonObject)["sequence"].toString().toLong()}}
            val header=revisionHeader.replace(previous.request().id().toString(),latest.request().id().toString())
            w.assets.put(AssetStore.Request(UUID.randomUUID(),"future.json",AssetStore.Kind.DERIVED,latest.request().id(),header.toByteArray(),LibraryRepository.TAG.toByteArray()),ByteArrayInputStream("{\"version\":2}".toByteArray()))
            assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{repo.migrateIngressCopiesToReferences(folder)}.status)
            assertContentEquals(byteArrayOf(4,5,6),Files.readAllBytes(copy));assertContentEquals(raw,w.bytes(id))
        }
    }
}
