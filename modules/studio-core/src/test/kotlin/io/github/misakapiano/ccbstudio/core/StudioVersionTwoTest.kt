package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class StudioVersionTwoTest {
    private fun root()=Paths.get(System.getProperty("preview.test.root")).resolve(newId())
    private fun account()=AccountProfile(alias="Test LLM",service="LLM",provider="OPENAI_COMPATIBLE",endpoint="https://example.invalid/v1",model="test-model")
    @Test fun unknownRootWithoutOwnerMarkerIsRejectedWithoutCreatingAnyFile() {
        for(studio in listOf(false,true)) {
            val r=root();Files.createDirectories(r)
            Files.writeString(r.resolve("preview-root.txt"),"CCBStudioSuite/WindowsStudio/v99")
            Files.writeString(r.resolve("unique-original.bin"),"Synthetic original retained")
            fun inventory()=Files.walk(r).use{paths->paths.filter{Files.isRegularFile(it)}.toList().associate{r.relativize(it).toString() to fingerprintFile(it)}}
            val before=inventory()
            assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{if(studio)StudioWorkspace.openStudio(r)else StudioWorkspace.open(r)}.status)
            assertEquals(before,inventory());assertFalse(Files.exists(r.resolve(".owner.lock")))
        }
    }
    @Test fun realProfilesRequireExplicitV2RootAndPersistDraft() {
        val old=root()
        StudioWorkspace.open(old).use {w -> assertFails {w.save(w.state.copy(accounts=listOf(account())))} }
        assertFailsWith<DataProblem> {StudioWorkspace.openStudio(old)}
        val r=root()
        StudioWorkspace.openStudio(r).use {w ->
            val c=w.state.creation!!
            w.save(w.state.copy(accounts=listOf(account()),creation=c.change(c.draft.copy(basePrompt="synthetic input",stylePrompt="ink")) ))
        }
        StudioWorkspace.openStudio(r).use {w ->
            assertEquals(StudioWorkspace.DATA_VERSION,w.state.version);assertEquals("ink",w.state.creation!!.draft.stylePrompt)
            assertEquals("",w.state.creation!!.undoDraft().draft.basePrompt)
            assertEquals("OPENAI_COMPATIBLE",w.state.accounts.single().provider)
        }
        assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{StudioWorkspace.open(r)}.status)
    }
    @Test fun endpointsCannotContainCredentialsOrInsecureSchemes() {
        for(url in listOf("http://example.invalid","https://user:password@example.invalid","https://example.invalid?key=fixture","https://example.invalid#fragment"))
            assertFails {account().copy(endpoint=url).validate()}
        account().validate()
        assertFails {account().copy(service="NAI",provider="NOVELAI").validate()}
    }
    @Test fun copyMigrationPreservesLegacyAndRestoresV2Backup() {
        val source=root();val bytes=EditorRenderer.synthetic(80,80);var id=""
        val backup=StudioWorkspace.open(source).use {w -> id=w.importImage("fixture.png",bytes);w.newProject("old",listOf(id));w.backup()}
        val dest=root();StudioMigration.fromLegacyBackup(backup,dest)
        StudioWorkspace.open(source).use {assertContentEquals(bytes,it.bytes(id));assertEquals(1,it.state.version)}
        val v2Backup=StudioWorkspace.openStudio(dest).use {assertContentEquals(bytes,it.bytes(id));assertEquals("old",it.state.projects.single().name);it.backup()}
        val restored=root();StudioRestore.restore(v2Backup,restored)
        StudioWorkspace.openStudio(restored).use {assertContentEquals(bytes,it.bytes(id));assertEquals(StudioWorkspace.DATA_VERSION,it.state.version)}
    }
    @Test fun failedMigrationDoesNotPublishOrOverwriteTarget() {
        val backup=StudioWorkspace.open(root()).use {it.backup()}
        val dest=root()
        assertFails {StudioMigration.fromLegacyBackup(backup,dest){if(it=="MIGRATION_VALIDATED")throw java.io.IOException("fixture fault")}}
        assertFalse(Files.exists(dest));assertTrue(Files.exists(backup.resolve("COMPLETE.txt")))
        Files.createDirectory(dest);Files.writeString(dest.resolve("keep"),"fixture")
        assertFails {StudioMigration.fromLegacyBackup(backup,dest)}
        assertEquals("fixture",Files.readString(dest.resolve("keep")))
    }
}
