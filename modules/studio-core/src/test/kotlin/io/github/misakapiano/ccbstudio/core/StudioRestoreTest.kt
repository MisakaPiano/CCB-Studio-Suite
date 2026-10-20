package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class StudioRestoreTest {
    private fun root()=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
    @Test fun completeSnapshotRestoresOriginalProjectUndoAndSecretReferenceOnly() {
        val root=root();val image=EditorRenderer.synthetic(128,128)
        val backup=StudioWorkspace.open(root).use {w->
            val id=w.importImage("cup.png",image);val p=w.newProject("Recovered",listOf(id))
            w.applyLayers(p,setOf(id),listOf(Layer(kind="TEXT",text="合成")))
            val ref=SecretVault(root.resolve("secrets")).put("SYNTHETIC-RESTORE".toByteArray())
            w.save(w.state.copy(accounts=listOf(AccountProfile(alias="test",provider="SYNTHETIC",service="NAI",model="fixture",secretRef=ref))))
            w.backup()
        }
        val destination=root();StudioRestore.restore(backup,destination)
        StudioWorkspace.open(destination).use {w->
            assertContentEquals(image,w.bytes(w.state.projects.single().current.pages.single().assetId))
            assertEquals("合成",w.state.projects.single().current.pages.single().layers.single().text)
            assertEquals(1,w.state.projects.single().undo.size)
            assertFails {SecretVault(destination.resolve("secrets")).use(w.state.accounts.single().secretRef!!) {}}
        }
        assertEquals("CONFLICT",assertFailsWith<DataProblem>{StudioRestore.restore(backup,destination)}.status)
    }
    @Test fun corruptIncompleteAndUnknownSnapshotsNeverPublishOrOverwrite() {
        val backup=StudioWorkspace.open(root()).use {w->w.importImage("test.png",EditorRenderer.synthetic(128,128));w.backup()}
        val destination=root();val blob=Files.list(backup.resolve("assets/objects")).use {it.findFirst().get()}
        val bytes=Files.readAllBytes(blob);Files.writeString(blob,"broken")
        assertEquals("CORRUPT",assertFailsWith<DataProblem>{StudioRestore.restore(backup,destination)}.status);assertFalse(Files.exists(destination))
        Files.write(blob,bytes);Files.writeString(backup.resolve("unexplained.bin"),"retained")
        assertFails {StudioRestore.restore(backup,destination)};assertEquals("retained",Files.readString(backup.resolve("unexplained.bin")))
        val incomplete=root();Files.createDirectory(incomplete)
        assertEquals("MISSING",assertFailsWith<DataProblem>{StudioRestore.restore(incomplete,destination)}.status)
    }
    @Test fun interruptedRestoreRetainsStagingAndCanRetryIntoFreshRoot() {
        val backup=StudioWorkspace.open(root()).use {w->w.save(w.state);w.backup()}
        val destination=root()
        assertFails {StudioRestore.restore(backup,destination){if(it=="RESTORE_DOCUMENT")throw java.io.IOException("injected")}}
        assertFalse(Files.exists(destination))
        val staged=Files.list(destination.parent).use {it.filter {p->p.fileName.toString().startsWith(".studio-restore-")}.toList()}
        assertTrue(staged.isNotEmpty())
        assertFails {StudioWorkspace.open(staged.last())}
        StudioRestore.restore(backup,destination);StudioWorkspace.open(destination).use {assertTrue(it.state.projects.isEmpty())}
    }
}
