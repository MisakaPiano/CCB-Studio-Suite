package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*
import io.github.misakapiano.ccbstudio.assets.EditorSnapshotRetirement
import io.github.misakapiano.ccbstudio.assets.AssetStore
import com.sun.jna.platform.win32.*

class TypesetProjectManagementTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private fun output(p:TypesetProject,path:Path)=Files.readAllBytes(path).let{TypesetManagedOutput(projectId=p.id,path=path.toAbsolutePath().toString(),size=it.size.toLong(),sha256=sha256(it))}
    @Test fun inlineTreeCreateRenameRemoveCommitsOnceAndPreservesBatchAndOtherProjectPlacement(){
        StudioWorkspace.openStudio(root()).use{w->val id=w.importImage("synthetic.png",EditorRenderer.synthetic(80,60));val service=TypesetProjectArchives(w)
            val a=GenerationFolder(name="Project");val b=GenerationFolder(name="Volume",parent=a.id,kind="folder");val c=GenerationFolder(name="Chapter",parent=b.id,kind="chapter");val d=GenerationFolder(name="Section",parent=c.id,kind="folder")
            val base=GenerationOrganization(folders=listOf(a,b,c,d));w.save(w.state.copy(generationOrganization=base));val one=service.create("one",listOf(id),c.id);val two=service.create("two",listOf(id),d.id)
            val removed=TypesetOrganizationChanges.remove(base,c.id);val edited=removed.copy(folders=removed.folders.map{if(it.id==b.id)it.copy(name="Renamed Volume")else it})
            assertEquals(base,w.state.generationOrganization,"Staged changes do not publish themselves")
            val archived=service.archive(one.id,one.revision,"archive",b.id,1793100000000,TypesetOrganizationChange(base,edited))
            assertEquals(edited,w.state.generationOrganization);assertEquals(b.id,service.record(one.id)!!.parent);assertEquals(d.id,service.record(two.id)!!.parent);assertEquals(b.id,w.state.generationOrganization.folders.single{it.id==d.id}.parent)
            assertEquals(one.current,archived.current);assertContentEquals(EditorRenderer.synthetic(80,60),w.bytes(id))
            assertFails{service.create("stale",listOf(id),null,TypesetOrganizationChange(base,base))};assertEquals(2,w.state.typesetProjects.size)
            val cycle=edited.copy(folders=edited.folders.map{if(it.id==a.id)it.copy(parent=d.id)else it});assertFails{service.archive(two.id,two.revision,"bad",null,change=TypesetOrganizationChange(edited,cycle))};assertEquals(edited,w.state.generationOrganization)
        }
    }
    @Test fun archiveAndCreateFailedOrganizationOrJournalDoNotPublishPartialCatalog(){
        val r=root();var fail=false;StudioWorkspace.openStudio(r){if(fail&&it=="DOCUMENT_STAGED")error("synthetic interrupted creation")}.use{w->
            val id=w.importImage("synthetic.png",EditorRenderer.synthetic(80,60));val old=w.state.generationOrganization;val next=old.copy(folders=listOf(GenerationFolder(name="staged")));fail=true
            assertFails{TypesetProjectArchives(w).create("never committed",listOf(id),next.folders[0].id,TypesetOrganizationChange(old,next))}
        };assertFailsWith<DataProblem>{StudioWorkspace.openStudio(r)}
    }
    @Test fun explicitDiscardCannotBeUndoneOrReopenedAndDefaultsKeepOutputsOriginalsAndOtherProjects(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val bytes=EditorRenderer.synthetic(90,70);val original=w.importImage("original.png",bytes);val repo=TypesetRepository(w);var one=repo.create("discard",listOf(original));val other=repo.create("other",listOf(original));one=repo.update(one.id,one.revision){it.copy(startNumber=2)}
            val file=Files.createDirectories(r.resolve("exports/owned")).resolve("generated.txt");Files.writeString(file,"synthetic generated output");val deletion=TypesetProjectDeletion(w);deletion.register(one.id,listOf(output(one,file)));val plan=deletion.plan(one.id);assertFails{deletion.delete(plan,emptySet(),false)};assertEquals(2,w.state.typesetProjects.size)
            val oldProgress=(listOf(one.current)+one.undo+one.redo).flatMap{it.chunks}.map{w.assets.read(java.util.UUID.fromString(it)).require()}
            deletion.delete(plan,emptySet(),true);assertEquals(listOf(other),w.state.typesetProjects);assertEquals("DELETED",TypesetProjectArchives(w).record(one.id)!!.state);assertFails{TypesetProjectArchives(w).reopen(one.id,one.revision)};assertFails{repo.history(one.id,one.revision,false)};assertTrue(Files.exists(file));assertContentEquals(bytes,w.bytes(original))
            assertEquals("COMPLETE",TypesetEditorExtrasStore.load(w).retirements.single().state);oldProgress.forEach{assertFalse(Files.exists(w.root.resolve("assets/objects/${it.sha256()}.blob")),"Exclusive progress blob must be physically gone")};assertTrue(w.assets.recover().any{it.state()=="EDITOR_RETIRED"});assertEquals(1,TypesetProjectArchives(w).active().size)
        };StudioWorkspace.openStudio(r).use{w->assertEquals(1,w.state.typesetProjects.size);assertEquals(1,TypesetProjectArchives(w).active().size)}
    }
    @Test fun physicalRetirementIncludesDroppedUndoButPreservesSharedSnapshotsAndOldBackups(){
        val r=root();lateinit var backup:Path;lateinit var originalId:String;lateinit var keptId:String
        val bytes=EditorRenderer.synthetic(96,64)
        StudioWorkspace.openStudio(r).use{w->originalId=w.importImage("original.png",bytes);val repo=TypesetRepository(w);var p=repo.create("discard history",listOf(originalId));val first=w.assets.read(java.util.UUID.fromString(p.current.chunks.single())).require()
            repeat(34){n->p=repo.update(p.id,p.revision){it.copy(startNumber=n+2)}};assertFalse(p.undo.any{first.request().id().toString() in it.chunks});val copy=repo.copy(p.id,p.revision,"shared survivor");keptId=copy.id;val shared=p.current
            backup=w.backup();val beforeBackup=Files.readAllBytes(backup.resolve("COMPLETE.txt"));val deletion=TypesetProjectDeletion(w);deletion.delete(deletion.plan(p.id),emptySet(),true)
            assertFalse(Files.exists(w.root.resolve("assets/objects/${first.sha256()}.blob")),"Snapshot beyond undo limit is still proven by the journal")
            assertEquals(shared,w.state.typesetProjects.single().current);assertEquals(35,repo.load(copy).startNumber);assertContentEquals(bytes,w.bytes(originalId));assertContentEquals(beforeBackup,Files.readAllBytes(backup.resolve("COMPLETE.txt")));assertTrue(w.catalog().isNotEmpty());assertEquals("COMPLETE",TypesetEditorExtrasStore.load(w).retirements.single().state)
            val after=w.backup();val restored=root();StudioRestore.restore(after,restored);StudioWorkspace.openStudio(restored).use{fresh->assertEquals(keptId,fresh.state.typesetProjects.single().id);assertContentEquals(bytes,fresh.bytes(originalId));assertEquals(35,TypesetRepository(fresh).load(fresh.state.typesetProjects.single()).startNumber)}
        }
        val oldRestore=root();StudioRestore.restore(backup,oldRestore);StudioWorkspace.openStudio(oldRestore).use{w->assertEquals(2,w.state.typesetProjects.size);assertContentEquals(bytes,w.bytes(originalId))}
    }
    @Test fun heldGeneratedFileAndHardlinkAreRetainedAndExplicitRetryDoesNotChangeUnknownBytes(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val id=w.importImage("original.png",EditorRenderer.synthetic(80,60));val p=TypesetRepository(w).create("held output",listOf(id));val dir=Files.createDirectories(r.resolve("exports/held"));val file=dir.resolve("owned.txt");Files.writeString(file,"synthetic locked output");val d=TypesetProjectDeletion(w);val record=output(p,file);d.register(p.id,listOf(record));val plan=d.plan(p.id)
            val handle=Kernel32.INSTANCE.CreateFile(file.toString(),WinNT.GENERIC_READ,WinNT.FILE_SHARE_READ,null,WinNT.OPEN_EXISTING,WinNT.FILE_ATTRIBUTE_NORMAL,null);assertNotEquals(WinBase.INVALID_HANDLE_VALUE,handle)
            try{val result=d.delete(plan,setOf(record.id),true);assertEquals("RETAINED",result.single().status);assertTrue(Files.exists(file))}finally{Kernel32.INSTANCE.CloseHandle(handle)}
            val link=dir.resolve("same-file.txt");Files.createLink(link,file);assertEquals("RETAINED",d.retryPending(setOf(record.id),true).single().status);assertEquals("synthetic locked output",Files.readString(link));Files.delete(link)
            assertEquals("DELETED",d.retryPending(setOf(record.id),true).single().status);assertFalse(Files.exists(file))
        }
    }
    @Test fun interruptedPhysicalRetirementResumesWithoutResurrectingRetiredTransactionsOrDeletingImages(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val bytes=EditorRenderer.synthetic(80,60);val id=w.importImage("original.png",bytes);var p=TypesetRepository(w).create("physical interruption",listOf(id));p=TypesetRepository(w).update(p.id,p.revision){it.copy(startNumber=2)}
            val assets=(listOf(p.current)+p.undo).flatMap{it.chunks}.map{w.assets.read(java.util.UUID.fromString(it)).require()};w.save(w.state.copy(typesetProjects=emptyList()));var injected=true
            assertFails{EditorSnapshotRetirement.retire(w.assets,assets){_,_,_->if(injected){injected=false;error("synthetic physical interruption")}}}
            assertContentEquals(bytes,w.bytes(id));assertTrue(w.assets.recover().any{it.state()=="EDITOR_RETIRED"});assertTrue(w.catalog().none{it.request().id()==assets.first().request().id()})
            val result=EditorSnapshotRetirement.retire(w.assets,assets){path,size,hash->WindowsOwnedOutputDeletion.delete(path,size,hash)};assertEquals(assets.size,result.size);assets.forEach{assertFalse(Files.exists(w.root.resolve("assets/objects/${it.sha256()}.blob")))}
            assertContentEquals(bytes,w.bytes(id));assertFails{EditorSnapshotRetirement.retire(w.assets,listOf(w.assets.read(java.util.UUID.fromString(id)).require())){_,_,_->error("must never delete an image")}}
            // The isolated layer harness published project removal before exercising retirement.
        }
    }
    @Test fun parentSwappedToJunctionAfterConfirmationCannotDeleteOutsideTheRecordedDirectory(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val id=w.importImage("original.png",EditorRenderer.synthetic(80,60));val p=TypesetRepository(w).create("junction guard",listOf(id));val base=Files.createDirectories(w.root.resolve("exports"));val selected=Files.createDirectory(base.resolve("selected"));val held=base.resolve("held");val outside=Files.createDirectory(base.resolve("unselected"));val selectedFile=selected.resolve("output.txt");val outsideFile=outside.resolve("output.txt");Files.writeString(selectedFile,"same synthetic bytes");Files.writeString(outsideFile,"same synthetic bytes")
            val deletion=TypesetProjectDeletion(w);val record=output(p,selectedFile);deletion.register(p.id,listOf(record));val plan=deletion.plan(p.id)
            try{
                val result=deletion.delete(plan,setOf(record.id),true){point->if(point=="PROJECT_REMOVED"){
                    require(selected.toAbsolutePath().startsWith(r.toAbsolutePath())&&outside.toAbsolutePath().startsWith(r.toAbsolutePath()));Files.move(selected,held)
                    val command="New-Item -ItemType Junction -LiteralPath '${selected}' -Target '${outside}' -ErrorAction Stop | Out-Null"
                    // New-Item uses Path rather than LiteralPath on Windows PowerShell 5.
                    val process=ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-Command",command.replace("-LiteralPath","-Path")).redirectErrorStream(true).start()
                    require(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)&&process.exitValue()==0){"Synthetic junction setup failed: "+process.inputStream.bufferedReader().readText().take(300)}
                }}
                assertEquals("RETAINED",result.single().status);assertEquals("same synthetic bytes",Files.readString(outsideFile));assertEquals("same synthetic bytes",Files.readString(held.resolve("output.txt")));assertTrue(w.catalog().isNotEmpty())
            }finally{if(Files.exists(selected,LinkOption.NOFOLLOW_LINKS)&&Files.exists(held))Files.delete(selected)}
            assertTrue(Files.exists(outsideFile));assertContentEquals(EditorRenderer.synthetic(80,60),w.bytes(id))
        }
    }
    @Test fun confirmedOutputDeletionUsesHashAndHandleAndRetainsChangedSharedProtectedAndUnknownFiles(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val original=w.importImage("original.png",EditorRenderer.synthetic(80,60));val p=TypesetRepository(w).create("one",listOf(original));val folder=Files.createDirectories(r.resolve("exports/generated"));val owned=folder.resolve("owned.txt");val modified=folder.resolve("modified.txt");val shared=folder.resolve("shared.txt");val unknown=folder.resolve("unknown.txt");listOf(owned,modified,shared,unknown).forEach{Files.writeString(it,"SYNTHETIC "+it.fileName)}
            val deletion=TypesetProjectDeletion(w);val records=listOf(output(p,owned),output(p,modified),output(p,shared).copy(assetId=original));deletion.register(p.id,records);Files.writeString(modified,"changed after export");val plan=deletion.plan(p.id)
            assertTrue(plan.entries.single{it.output.path==owned.toString()}.eligible);assertFalse(plan.entries.single{it.output.path==modified.toString()}.eligible);assertFalse(plan.entries.single{it.output.path==shared.toString()}.eligible)
            val selected=records[0].id;val results=deletion.delete(plan,setOf(selected),true);assertEquals("DELETED",results.single().status,results.single().detail);assertFalse(Files.exists(owned));assertTrue(Files.exists(modified));assertTrue(Files.exists(shared));assertTrue(Files.exists(unknown));assertTrue(Files.isDirectory(folder))
        }
    }
    @Test fun interruptionRetainsConfirmedIntentAndRetryIsIdempotentWithoutDirectorySweeping(){
        val r=root();lateinit var outputId:String;lateinit var file:Path
        StudioWorkspace.openStudio(r).use{w->val id=w.importImage("original.png",EditorRenderer.synthetic(80,60));val p=TypesetRepository(w).create("one",listOf(id));file=Files.createDirectories(r.resolve("exports/generated")).resolve("pending.txt");Files.writeString(file,"synthetic pending deletion");val d=TypesetProjectDeletion(w);val record=output(p,file);outputId=record.id;d.register(p.id,listOf(record));assertFails{d.delete(d.plan(p.id),setOf(outputId),true){if(it=="PROJECT_REMOVED")error("injected after durable intent")}};assertTrue(Files.exists(file));assertTrue(w.state.typesetProjects.isEmpty())}
        StudioWorkspace.openStudio(r).use{w->val d=TypesetProjectDeletion(w);assertEquals("DELETE_PENDING",TypesetEditorExtrasStore.load(w).outputs.single().status);assertEquals("DELETED",d.retryPending(setOf(outputId),true).single().status);assertFalse(Files.exists(file));assertFails{d.retryPending(setOf(outputId),true)}}
    }
    @Test fun projectExchangeContainsOriginalAndCurrentProgressAndPresetExchangeDoesNotLeakLocalDeletionPaths(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val bytes=EditorRenderer.synthetic(80,60);val id=w.importImage("original.png",bytes);val repo=TypesetRepository(w);var p=repo.create("editable",listOf(id));p=repo.update(p.id,p.revision){it.copy(pages=it.pages.map{page->page.copy(texts=listOf(TypesetText(text="CURRENT SYNTHETIC")),masks=listOf(TypesetMask(effect="black")))})}
            val file=Files.createDirectories(r.resolve("exports")).resolve("safe.json");Files.writeString(file,"synthetic");TypesetProjectDeletion(w).register(p.id,listOf(output(p,file)))
            val export=TypesetSourceExchange.export(w,p,repo.load(p));val inspected=TypesetSourceExchange.inspect(export);assertContentEquals(bytes,inspected.originals.single());assertEquals("CURRENT SYNTHETIC",inspected.document.pages.single().texts.single().text)
            val imported=TypesetSourceExchange.import(w,inspected);assertNotEquals(p.id,imported.id);assertTrue(imported.undo.isEmpty());assertContentEquals(bytes,w.bytes(repo.load(imported).pages.single().assetId))
            val presets=TypesetSourceExchange.exportPresets(w);assertFalse(presets.decodeToString().contains(file.toString().replace("\\","\\\\")));val previous=TypesetEditorExtrasStore.load(w).outputs;TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(presets),false);assertEquals(previous,TypesetEditorExtrasStore.load(w).outputs)
        }
    }
    @Test fun deletingImportedProjectReclaimsEmbeddedSourceExchangeButKeepsExtractedOriginalsAndSourceProject(){
        val r=root();StudioWorkspace.openStudio(r).use{w->val bytes=EditorRenderer.synthetic(88,66);val image=w.importImage("source.png",bytes);val repo=TypesetRepository(w);var source=repo.create("source project",listOf(image));source=repo.update(source.id,source.revision){it.copy(pages=it.pages.map{page->page.copy(caption="SYNTHETIC imported progress")})}
            val exchange=TypesetSourceExchange.export(w,source,repo.load(source));val imported=TypesetSourceExchange.import(w,TypesetSourceExchange.inspect(exchange));val sourceAsset=w.assets.read(java.util.UUID.fromString(imported.sourceExchangeId!!)).require();val extracted=repo.load(imported).pages.single().assetId;val allOriginals=SharedAssets.images(w).map{it.request().id()}.toSet()
            val deletion=TypesetProjectDeletion(w);deletion.delete(deletion.plan(imported.id),emptySet(),true)
            assertEquals(listOf(source.id),w.state.typesetProjects.map{it.id});assertFalse(Files.exists(w.root.resolve("assets/objects/${sourceAsset.sha256()}.blob")));assertContentEquals(bytes,w.bytes(extracted));assertContentEquals(bytes,w.bytes(image));assertEquals(allOriginals,SharedAssets.images(w).map{it.request().id()}.toSet());assertEquals("SYNTHETIC imported progress",repo.load(source).pages.single().caption);assertEquals("COMPLETE",TypesetEditorExtrasStore.load(w).retirements.single().state)
        }
    }
}
