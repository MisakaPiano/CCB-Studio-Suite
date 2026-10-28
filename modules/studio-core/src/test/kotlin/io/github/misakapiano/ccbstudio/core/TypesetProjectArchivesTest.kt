package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class TypesetProjectArchivesTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun currentNameAndNestedOrganizationSaveTogetherWithoutCopyingRecipes(){
        StudioWorkspace.openStudio(root()).use{w->
            val bytes=EditorRenderer.synthetic(100,80);val id=w.importImage("original.png",bytes);val repo=TypesetRepository(w);val p=repo.create("before",listOf(id))
            val before=w.state.generationOrganization;val parent=GenerationFolder(name="Project");val chapter=GenerationFolder(name="Chapter",parent=parent.id);val nextOrg=before.copy(folders=before.folders+parent+chapter)
            val a=TypesetProjectArchives(w);val next=a.place(p.id,p.revision,"renamed",chapter.id,TypesetOrganizationChange(before,nextOrg))
            assertEquals("renamed",next.name);assertEquals(p.current,next.current);assertEquals(p.undo,next.undo);assertEquals(p.redo,next.redo)
            assertEquals(chapter.id,a.record(p.id)!!.parent);assertEquals(nextOrg,w.state.generationOrganization);assertContentEquals(bytes,w.bytes(id))
            val count=w.catalog().size;assertEquals(next,a.place(next.id,next.revision,next.name,chapter.id));assertEquals(count,w.catalog().size)
            val state=w.state;assertFails{a.place(p.id,p.revision,"stale",null)};assertFails{a.place(next.id,next.revision," ",null)};assertFails{a.place(next.id,next.revision,"bad",newId())};assertEquals(state,w.state)
        }
    }
    @Test fun archiveReopenAndBackupRetainIdentityRecipesHistoryAndOriginalBytes(){
        val path=root();lateinit var backup:Path;lateinit var retained:TypesetProject
        lateinit var originalId:String;val bytes=EditorRenderer.synthetic(160,100)
        StudioWorkspace.openStudio(path).use{w->
            originalId=w.importImage("synthetic.png",bytes);val repo=TypesetRepository(w);var p=repo.create("original",listOf(originalId,originalId))
            p=repo.update(p.id,p.revision){it.copy(pages=it.pages.map{page->page.copy(texts=listOf(TypesetText(text="retained")),masks=listOf(TypesetMask(effect="black")))})}
            p=repo.history(p.id,p.revision,false);p=repo.view(p.id,p.revision,repo.load(p).pages.last().id,listOf(repo.load(p).pages.last().id))
            val parent=GenerationFolder(name="Project");val chapter=GenerationFolder(name="Chapter",kind="chapter",parent=parent.id)
            w.save(w.state.copy(generationOrganization=GenerationOrganization(folders=listOf(parent,chapter))))
            val before=p;val document=repo.load(p);val recipeCount=w.catalog().count{it.request().project().decodeToString()==TypesetRepository.TAG};val total=w.catalog().size
            val a=TypesetProjectArchives(w);retained=a.archive(p.id,p.revision,"named",chapter.id,1793100000000)
            assertEquals(before.id,retained.id);assertEquals(before.current,retained.current);assertEquals(before.undo,retained.undo);assertEquals(before.redo,retained.redo);assertEquals(before.currentPageId,retained.currentPageId);assertEquals(before.selectedPageIds,retained.selectedPageIds)
            assertEquals(document,repo.load(retained));assertTrue(a.active().isEmpty());assertEquals(listOf(retained),a.archived());assertEquals(1,w.state.typesetProjects.size);assertEquals(total+1,w.catalog().size,"Only one small settings revision, no image/project clone")
            assertEquals(recipeCount,w.catalog().count{it.request().project().decodeToString()==TypesetRepository.TAG});assertContentEquals(bytes,w.bytes(originalId))
            val count=w.catalog().size;assertEquals(retained,a.archive(retained.id,retained.revision,"named",chapter.id));assertEquals(count,w.catalog().size,"Repeated identical archive is a no-op")
            a.reopen(retained.id,retained.revision);val reopenedCount=w.catalog().size;a.reopen(retained.id,retained.revision);assertEquals(reopenedCount,w.catalog().size)
            assertEquals(before.current,repo.load(a.active().single()).let{a.active().single().current});assertEquals(before.undo,a.active().single().undo);assertEquals(before.redo,a.active().single().redo)
            retained=a.archive(retained.id,retained.revision,"named again",null);backup=w.backup()
        }
        StudioWorkspace.openStudio(path).use{w->assertEquals(retained,TypesetProjectArchives(w).archived().single());assertContentEquals(bytes,w.bytes(originalId))}
        val restored=root();StudioRestore.restore(backup,restored)
        StudioWorkspace.openStudio(restored).use{w->assertEquals(retained,TypesetProjectArchives(w).archived().single());assertContentEquals(bytes,w.bytes(originalId));assertNull(TypesetEditorExtrasStore.load(w).lastProjectId);assertTrue(TypesetEditorExtrasStore.load(w).projectSelectionRecorded)}
    }
    @Test fun invalidInputsStaleRevisionAndCorruptRecipeDoNotPublishArchive(){
        StudioWorkspace.openStudio(root()).use{w->val id=w.importImage("synthetic.png",EditorRenderer.synthetic(100,80));val repo=TypesetRepository(w);val p=repo.create("guards",listOf(id));val a=TypesetProjectArchives(w);val before=w.state;val count=w.catalog().size
            assertFails{a.archive(p.id,p.revision," ",null)};assertFails{a.archive(p.id,p.revision,"ok",newId())};assertFails{a.archive(p.id,p.revision+1,"ok",null)}
            assertEquals(before,w.state);assertEquals(count,w.catalog().size)
            val broken=p.copy(current=p.current.copy(sha256="0".repeat(64)));w.save(w.state.copy(typesetProjects=listOf(broken)));val brokenState=w.state
            assertFailsWith<DataProblem>{a.archive(p.id,p.revision,"ok",null)};assertEquals(brokenState,w.state);assertEquals(count,w.catalog().size)
        }
    }
    @Test fun logicalRemovalRetainsResourceGraphAndSharedOriginalsWithoutFileDeletion(){
        StudioWorkspace.openStudio(root()).use{w->val bytes=EditorRenderer.synthetic(100,80);val id=w.importImage("shared.png",bytes);val repo=TypesetRepository(w);var p=repo.create("one",listOf(id));val other=repo.create("two",listOf(id));p=repo.update(p.id,p.revision){it.copy(startNumber=2)}
            val a=TypesetProjectArchives(w);val refs=a.resources(p.id);assertEquals(setOf(id),refs.originals);assertEquals(setOf(id),a.resources(other.id).originals);assertEquals((listOf(p.current)+p.undo+p.redo).flatMap{it.chunks}.toSet(),refs.documents)
            a.removeFromList(p.id,p.revision);assertEquals(listOf(other),a.active());assertTrue(a.archived().isEmpty());assertEquals(2,w.state.typesetProjects.size);assertEquals(refs,a.resources(p.id));assertContentEquals(bytes,w.bytes(id))
            assertFails{a.reopen(p.id,p.revision)}
            val exchange=TypesetSourceExchange.exportPresets(w);TypesetSourceExchange.importPresets(w,TypesetSourceExchange.inspectPresets(exchange),false)
            assertEquals("REMOVED",a.record(p.id)!!.state);assertEquals(refs,a.resources(p.id))
            val exported=TypesetEditorExtrasStore.inspect(documentJson.parseToJsonElement(exchange.decodeToString()).let{it as kotlinx.serialization.json.JsonObject})
            assertTrue(exported.projectRecords.isEmpty());assertFalse(exported.projectSelectionRecorded);assertNull(exported.lastProjectId)
        }
    }
    @Test fun interruptedJournalCommitKeepsPriorFilesAndDoesNotSilentlyRecover(){
        val path=root();var fail=false;val w=StudioWorkspace.openStudio(path){if(fail&&it=="DOCUMENT_STAGED")error("synthetic interrupted archive")}
        val id=w.importImage("synthetic.png",EditorRenderer.synthetic(100,80));val p=TypesetRepository(w).create("before",listOf(id))
        val committed=Files.list(path.resolve("documents")).use{it.toList()}.associate{it.fileName.toString() to Files.readAllBytes(it)}
        fail=true;assertFails{TypesetProjectArchives(w).archive(p.id,p.revision,"after",null)};w.close()
        committed.forEach{(name,bytes)->assertContentEquals(bytes,Files.readAllBytes(path.resolve("documents").resolve(name)))}
        assertTrue(Files.list(path.resolve("documents")).use{it.anyMatch{it.fileName.toString().startsWith(".pending-")}})
        assertFailsWith<DataProblem>{StudioWorkspace.openStudio(path)}
        Files.writeString(path.parent.resolve("archive-interruption-${p.id}.txt"),"Expected injected DOCUMENT_STAGED interruption: prior committed files retained; pending retained; reopen refused, no silent cleanup.\n")
    }
}
