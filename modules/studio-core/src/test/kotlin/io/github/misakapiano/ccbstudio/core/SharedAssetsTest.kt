package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class SharedAssetsTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun exportReturnsToSharedLibraryWithExactBytesAndReopenProvenance() {
        val r=root();val bytes=EditorRenderer.synthetic(96,96);var source="";var derived="";var project=""
        StudioWorkspace.openStudio(r).use { w->
            source=w.importImage("original.png",bytes);val repo=TypesetRepository(w)
            val p=repo.create("Reusable",listOf(source));project=p.id;val d=repo.load(p)
            val out=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(d.pages.map{it.id},zip=true,variants=listOf("all","original")))
            val receipt=TypesetExport.verify(out);assertEquals(3,receipt.assetIds.size)
            assertEquals(source,receipt.assetIds.entries.single{it.key.startsWith("original/")}.value)
            derived=receipt.assetIds.entries.single{it.key.startsWith("all/")}.value
            assertTrue(SharedAssets.images(w).any{it.request().id().toString()==derived})
            assertEquals(1,SharedAssets.documents(w).size)
            receipt.assetIds.forEach{(file,id)->assertContentEquals(Files.readAllBytes(out.resolve(file)),w.bytes(id))}
            val a=SharedAssets.images(w).single{it.request().id().toString()==derived};val provenance=SharedAssets.provenance(a)!!
            assertEquals(listOf(source),provenance.sourceIds);assertEquals(p.current,provenance.snapshot)
            val count=w.catalog().size
            val file=receipt.files.single{it.name.startsWith("all/")}
            assertEquals(derived,SharedAssets.retain(w,out.resolve(file.name),file,provenance));assertEquals(count,w.catalog().size)
            val reused=repo.create("Second project",listOf(derived));assertEquals(derived,repo.load(reused).pages.single().assetId)
        }
        StudioWorkspace.openStudio(r).use { w->
            assertContentEquals(bytes,w.bytes(source));assertEquals(project,SharedAssets.provenance(SharedAssets.images(w).single{it.request().id().toString()==derived})!!.projectId)
            val backup=w.backup();assertTrue(Files.exists(backup.resolve("COMPLETE.txt")))
        }
    }
    @Test fun interruptedAssetRetentionNeverPublishesBatchOrDeletesOriginal() {
        StudioWorkspace.openStudio(root()).use { w->
            val bytes=EditorRenderer.synthetic(48,48);val id=w.importImage("original.png",bytes);val repo=TypesetRepository(w);val p=repo.create("Fault",listOf(id));val d=repo.load(p)
            val parent=Files.createDirectories(root())
            assertFails{TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id})){if(it=="ASSET_RETAINED")error("fault after durable asset")}}
            assertContentEquals(bytes,w.bytes(id));assertEquals(2,SharedAssets.images(w).size)
            Files.list(parent).use{paths->paths.forEach{assertTrue(it.fileName.toString().endsWith(".incomplete"));assertFails{TypesetExport.verify(it)}}}
        }
    }
}
