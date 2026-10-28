package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class TypesetImportStorageTest {
    @Test fun newImportsRetainOnlyEditableContentAndNeverModifyExistingImportsOrExternalPackage(){
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());Files.createDirectories(root)
        val raw=javaClass.getResourceAsStream("/typeset/source-workspace.json")!!.readBytes();val external=root.resolve("external.json");Files.write(external,raw)
        val inspection=TypesetSourceExchange.inspect(Files.readAllBytes(external));val backup=StudioWorkspace.openStudio(root.resolve("workspace")).use{w->
            val sourceId=java.util.UUID.randomUUID()
            w.assets.put(io.github.misakapiano.ccbstudio.assets.AssetStore.Request(sourceId,"legacy.json",io.github.misakapiano.ccbstudio.assets.AssetStore.Kind.ORIGINAL,null,byteArrayOf(),TypesetSourceExchange.TAG.toByteArray()),java.io.ByteArrayInputStream(raw))
            val repo=TypesetRepository(w);val legacy=repo.importDocument("Existing import",inspection.document.copy(pages=inspection.document.pages.mapIndexed{i,p->p.copy(assetId=w.importImage(p.name,inspection.originals[i]))}),sourceId.toString(),0)
            val before=w.catalog().map{it.request().id()}.toSet();val p=TypesetSourceExchange.import(w,inspection)
            assertNull(p.sourceExchangeId);val added=w.catalog().filter{it.request().id() !in before};assertTrue(added.none{it.request().project().contentEquals(TypesetSourceExchange.TAG.toByteArray())})
            assertTrue(added.none{w.bytes(it.request().id().toString()).contentEquals(raw)})
            assertContentEquals(raw,TypesetSourceExchange.originalExchange(w,legacy));assertContentEquals(raw,Files.readAllBytes(external))
            val d=repo.load(p);assertContentEquals(inspection.originals.single(),w.bytes(d.pages.single().assetId));assertEquals(inspection.document.pages.single().texts,d.pages.single().texts)
            val rebuilt=TypesetSourceExchange.inspect(TypesetSourceExchange.export(w,p,d));assertEquals(d.pages.single().texts,rebuilt.document.pages.single().texts);assertContentEquals(inspection.originals.single(),rebuilt.originals.single());w.backup()
        }
        StudioRestore.restore(backup,root.resolve("restore"));StudioWorkspace.openStudio(root.resolve("restore")).use{w->
            assertContentEquals(raw,TypesetSourceExchange.originalExchange(w,w.state.typesetProjects.first()));val p=w.state.typesetProjects.last();assertNull(p.sourceExchangeId);assertContentEquals(inspection.originals.single(),w.bytes(TypesetRepository(w).load(p).pages.single().assetId))
        };assertContentEquals(raw,Files.readAllBytes(external))
    }
}
