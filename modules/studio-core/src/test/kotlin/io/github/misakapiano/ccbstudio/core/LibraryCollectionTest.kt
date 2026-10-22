package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class LibraryCollectionTest {
    @Test fun progressReleasesWorkspaceAndCancellationCheckpointsOnlyCompletedFiles() {
        val folder=Files.createDirectories(root());repeat(3){Files.write(folder.resolve("$it.png"),EditorRenderer.synthetic(32+it,32))}
        StudioWorkspace.openStudio(root()).use {w->
            val repo=LibraryRepository(w);val registration=repo.addFolder(folder,"images",false).folders.single()
            val pool=java.util.concurrent.Executors.newSingleThreadExecutor();var cancel=false
            try {
                assertFailsWith<java.util.concurrent.CancellationException>{repo.scan(registration.id,
                    checkCancelled={if(cancel)throw java.util.concurrent.CancellationException()},progress={p->
                        assertFalse(Thread.holdsLock(w),"Progress must not own the workspace monitor")
                        pool.submit<Int>{w.catalog().size}.get(3,java.util.concurrent.TimeUnit.SECONDS)
                        if(p.completed==1){repo.update{it.copy(preferences=it.preferences+("concurrent-setting" to "kept"))};cancel=true}
                    })}
                assertEquals(1,repo.load().observations.size);assertEquals("kept",repo.load().preferences["concurrent-setting"])
                assertTrue(repo.load().observations.values.all{it.status=="PRESENT"})
                assertEquals(1,SharedAssets.images(w).size)
                val retained=SharedAssets.images(w).single();val bytes=w.bytes(retained.request().id().toString())
                repo.scan(registration.id);assertEquals(3,SharedAssets.images(w).size)
                assertContentEquals(bytes,w.bytes(retained.request().id().toString()))
            }finally{pool.shutdownNow()}
        }
    }
    @Test fun rescanAdoptsOldCommittedImageWithoutObservationAndDoesNotImportItAgain() {
        val data=root();val folder=Files.createDirectories(root());val bytes=EditorRenderer.synthetic(33,35)
        Files.write(folder.resolve("retained.png"),bytes);var id="";var registration=""
        StudioWorkspace.openStudio(data).use{w->
            val repo=LibraryRepository(w);registration=repo.addFolder(folder,"images",false).folders.single().id
            id=w.importImage("retained.png",bytes,"""{"kind":"image-ingress","version":1,"source":"managed-folder","folderId":"$registration","relativePath":"retained.png","previousAssetId":""}""")
            assertTrue(repo.load().observations.isEmpty())
        }
        StudioWorkspace.openStudio(data).use{w->val repo=LibraryRepository(w)
            assertEquals(id,repo.scan(registration).single().assetId)
            assertEquals(1,SharedAssets.images(w).size);assertContentEquals(bytes,w.bytes(id))
            assertEquals("UNCHANGED",repo.scan(registration).single().status)
        }
    }
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun discoveringNewSubtreeKeepsOtherRegisteredRootLiveDuringConcurrentWrite() {
        val first=Files.createDirectories(root());val second=Files.createDirectories(root());val nested=first.resolve("new-subtree")
        val a=LibraryFolder(path=first.toString(),category="images",recursive=true)
        val b=LibraryFolder(path=second.toString(),category="prompts")
        var injected=false
        LibraryDirectoryMonitor(listOf(a,b),beforeNewRegistration={path->
            if(path==nested){Files.writeString(second.resolve("during-registration.prompt"),"captured while another root grows");injected=true}
        }).use {watch->
            assertEquals(setOf(a.id,b.id),watch.changes(0));Files.createDirectory(nested)
            val deadline=System.nanoTime()+5_000_000_000L
            while(!injected&&System.nanoTime()<deadline)watch.changes(100)
            assertTrue(injected,"Expected registration of the new subtree")
            var found=false
            while(!found&&System.nanoTime()<deadline)found=b.id in watch.changes(100)
            assertTrue(found,"The other root must retain its live registration and deliver the injected event")
        }
    }
    @Test fun restoringAtIdenticalPathStillRequiresLocalCollectionReauthorization() {
        val base=Path.of(System.getProperty("preview.test.root")).toAbsolutePath().normalize()
        val data=root().toAbsolutePath().normalize();val retained=root().toAbsolutePath().normalize();val folder=Files.createDirectories(root());var relativeBackup:Path?=null
        StudioWorkspace.openStudio(data).use{w->val repo=LibraryRepository(w);repo.addFolder(folder,"images",false);assertEquals(1,repo.automaticFolders().size);relativeBackup=data.relativize(w.backup())}
        require(data.parent==base && retained.parent==base && data!=retained && !Files.exists(retained))
        Files.move(data,retained) // Preserve the original synthetic tree; no deletion or overwrite.
        StudioRestore.restore(retained.resolve(relativeBackup!!),data)
        StudioWorkspace.openStudio(data).use{w->val repo=LibraryRepository(w);assertEquals(1,repo.load().folders.size);assertTrue(repo.automaticFolders().isEmpty());assertFalse(Files.exists(data.resolve("exports/.local-bindings/collections.id")));repo.enableHere(repo.load().folders.single().id);assertEquals(1,repo.automaticFolders().size)}
    }
    @Test fun watchCreatesVersionsAndRestoreRequiresExplicitReconnection() {
        val data=root();val folder=Files.createDirectories(root());val restored=root();var backup:Path?=null;var registration="";var image=""
        StudioWorkspace.openStudio(data).use{w->val repo=LibraryRepository(w)
            registration=repo.addFolder(folder,"images",true).folders.single().id
            LibraryDirectoryMonitor(repo.automaticFolders()).use{watch->
                assertEquals(setOf(registration),watch.changes(0))
                val nested=Files.createDirectory(folder.resolve("nested"));val file=Files.write(nested.resolve("first.png"),EditorRenderer.synthetic(48,48))
                fun waitChanged(){val deadline=System.nanoTime()+5_000_000_000L;var found=false;while(System.nanoTime()<deadline&&!found)found=registration in watch.changes(100);assertTrue(found,"Expected real filesystem event")}
                waitChanged();assertEquals("IMPORTED",repo.scan(registration).single().status);image=repo.load().observations.values.single().assetId
                val first=w.bytes(image);Files.write(file,EditorRenderer.synthetic(64,64));waitChanged();assertEquals("NEW_VERSION",repo.scan(registration).single().status);assertContentEquals(first,w.bytes(image))
                Files.move(file,nested.resolve("removed.txt"));waitChanged();repo.scan(registration);assertEquals("SOURCE_MISSING",repo.load().observations.values.single().status)
                assertEquals(2,SharedAssets.images(w).size)
            }
            backup=w.backup()
        }
        StudioRestore.restore(backup!!,restored)
        StudioWorkspace.openStudio(restored).use{w->val repo=LibraryRepository(w);assertTrue(repo.automaticFolders().isEmpty());assertNotNull(w.assets.read(java.util.UUID.fromString(image)).require());repo.enableHere(registration);assertEquals(1,repo.automaticFolders().size);repo.pauseCollections(true);assertTrue(repo.automaticFolders().isEmpty())}
    }
    @Test fun intakeCopyIsIdempotentPreservesOriginalAndNeverReplacesModifiedDestination() {
        StudioWorkspace.openStudio(root()).use{w->val repo=LibraryRepository(w);val bytes=EditorRenderer.synthetic(48,48);val id=w.importImage("original.png",bytes)
            val path=repo.retainIngress(id);assertContentEquals(bytes,Files.readAllBytes(path));assertEquals(path,repo.retainIngress(id))
            val folder=repo.load().folders.single();assertEquals("UNCHANGED",repo.scan(folder.id).single().status);assertEquals(1,SharedAssets.images(w).size)
            val changed=EditorRenderer.synthetic(50,50);Files.write(path,changed)
            assertFails{repo.retainIngress(id)};assertContentEquals(changed,Files.readAllBytes(path));assertContentEquals(bytes,w.bytes(id));assertEquals("COPY_PENDING",repo.load().observations.values.single().status)
            assertEquals("NEW_VERSION",repo.scan(folder.id).single().status);assertEquals(2,SharedAssets.images(w).size);assertContentEquals(bytes,w.bytes(id))
        }
    }
}
