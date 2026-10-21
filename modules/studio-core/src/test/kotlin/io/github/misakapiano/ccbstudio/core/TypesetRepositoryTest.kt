package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class TypesetRepositoryTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun thousandPagesHistoryRestartAndBackupPreserveOriginal() {
        val root=root();val original=EditorRenderer.synthetic(40,40);var id="";var project=""
        val backup=StudioWorkspace.openStudio(root).use{w->id=w.importImage("fixture.png",original);val repo=TypesetRepository(w)
            var p=repo.create("整册",List(1000){id});project=p.id
            p=repo.update(p.id,p.revision){d->d.copy(pages=d.pages.mapIndexed{i,page->page.copy(caption="第${i+1}页",texts=listOf(TypesetText(text="[b]中文[/]",vertical=true)))})}
            p=repo.update(p.id,p.revision){d->d.copy(pages=d.pages.reversed())}
            val previous=p;p=repo.history(p.id,p.revision,false);assertEquals("第1页",repo.load(p).pages.first().caption)
            p=repo.history(p.id,p.revision,true);assertEquals("第1000页",repo.load(p).pages.first().caption)
            assertFails{repo.update(p.id,previous.revision){it.copy(startNumber=5)}}
            assertEquals("第1000页",repo.load(p).pages.first().caption)
            assertContentEquals(original,w.bytes(id));w.backup()
        }
        StudioWorkspace.openStudio(root).use{w->val d=TypesetRepository(w).load(w.state.typesetProjects.single{it.id==project});assertEquals(1000,d.pages.size);assertTrue(d.pages.all{it.texts.single().vertical})}
        val restored=root();StudioRestore.restore(backup,restored)
        StudioWorkspace.openStudio(restored).use{w->assertEquals(1000,TypesetRepository(w).load(w.state.typesetProjects.single()).pages.size);assertContentEquals(original,w.bytes(id))}
    }
    @Test fun stagedDocumentFailureRetainsOriginalAndRefusesUnsafeReopen() {
        val root=root();val bytes=EditorRenderer.synthetic(20,20);var armed=false
        val w=StudioWorkspace.openStudio(root){if(armed && it=="DOCUMENT_STAGED")throw java.io.IOException("fixture")}
        val id=w.importImage("source.png",bytes);val repo=TypesetRepository(w);val p=repo.create("book",listOf(id));armed=true
        assertFails{repo.update(p.id,p.revision){it.copy(startNumber=9)}};w.close()
        assertEquals("RECOVERY_REQUIRED",assertFailsWith<DataProblem>{StudioWorkspace.openStudio(root)}.status)
        io.github.misakapiano.ccbstudio.assets.AssetStore.open(root.resolve("assets")).use{a->val out=java.io.ByteArrayOutputStream();a.export(java.util.UUID.fromString(id),out);assertContentEquals(bytes,out.toByteArray())}
    }
    @Test fun badSnapshotHashCannotReplaceCurrentOrDeleteOriginal() {
        StudioWorkspace.openStudio(root()).use{w->val bytes=EditorRenderer.synthetic(20,20);val id=w.importImage("source.png",bytes);val repo=TypesetRepository(w);val p=repo.create("book",listOf(id))
            val bad=p.copy(current=p.current.copy(sha256="0".repeat(64)))
            assertEquals("CORRUPT",assertFailsWith<DataProblem>{repo.load(bad)}.status)
            assertEquals(p,w.state.typesetProjects.single());assertContentEquals(bytes,w.bytes(id));assertEquals(id,repo.load(p).pages.single().assetId)
        }
    }
    @Test fun projectCopyRenameAndRemovalKeepIndependentHistoryAndOriginal() {
        val r=root();val bytes=EditorRenderer.synthetic(30,30)
        StudioWorkspace.openStudio(r).use{w->val id=w.importImage("fixture.png",bytes);val repo=TypesetRepository(w);val p=repo.create("第一册",listOf(id))
            val copy=repo.copy(p.id,p.revision,"第二册");assertNotEquals(p.id,copy.id);assertTrue(copy.undo.isEmpty())
            val edited=repo.update(copy.id,copy.revision){d->d.copy(pages=d.pages.map{it.copy(caption="副本旁白")})}
            assertEquals("",repo.load(p).pages.single().caption);assertEquals("副本旁白",repo.load(edited).pages.single().caption)
            val named=repo.rename(edited.id,edited.revision,"新册名");assertEquals("新册名",named.name)
            assertFails{repo.remove(named.id,edited.revision)};repo.remove(p.id,p.revision)
            assertEquals(listOf(named),w.state.typesetProjects);assertContentEquals(bytes,w.bytes(id))
        }
        StudioWorkspace.openStudio(r).use{w->assertEquals("新册名",w.state.typesetProjects.single().name);assertEquals("副本旁白",TypesetRepository(w).load(w.state.typesetProjects.single()).pages.single().caption)}
    }

}
