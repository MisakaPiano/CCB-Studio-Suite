package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.*
import java.nio.file.*
import kotlin.test.*

class RestoredUpdateSnapshotTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun account(ref:String)=AccountProfile(alias="synthetic",service="LLM",provider="SYNTHETIC",model="fixture",secretRef=ref)

    @Test fun restoredWorkspaceCanBeVerifiedWithoutRecreatingExcludedCredential() {
        val destination=root()
        val original=StudioWorkspace.openStudio(root()).use { w ->
            w.importImage("fixture.png",EditorRenderer.synthetic(32,32))
            val ref=SecretVault(w.root.resolve("secrets")).put("synthetic restore update".toByteArray())
            w.save(w.state.copy(accounts=listOf(account(ref))))
            val before=updateSnapshot(w)
            StudioRestore.restore(w.backup(),destination)
            assertEquals(before,updateSnapshot(w))
            before
        }
        val restored=StudioWorkspace.openStudio(destination).use { w ->
            val snapshot=updateSnapshot(w)
            assertEquals(original["assets"],snapshot["assets"])
            assertEquals(original["stateSha256"],snapshot["stateSha256"])
            val credential=snapshot["credentials"]!!.jsonArray.single().jsonObject
            assertEquals("MISSING_BEFORE_UPDATE",credential["status"]!!.jsonPrimitive.content)
            assertNull(credential["sha256"])
            withPlan(w,snapshot) { plan -> assertEquals(snapshot,verifyUpdatedWorkspace(w,plan)["snapshot"]) }
            assertEquals(0,Files.list(destination.resolve("secrets")).use{it.count()}.toInt())
            snapshot
        }
        StudioWorkspace.openStudio(destination).use { assertEquals(restored,updateSnapshot(it)) }
    }

    @Test fun previouslyReadableCredentialCannotDisappearDuringUpdate() {
        StudioWorkspace.openStudio(root()).use { w ->
            val ref=SecretVault(w.root.resolve("secrets")).put("synthetic retained credential".toByteArray())
            w.save(w.state.copy(accounts=listOf(account(ref))))
            val before=updateSnapshot(w)
            withPlan(w,before) { plan ->
                assertEquals(before,verifyUpdatedWorkspace(w,plan)["snapshot"])
                // Keep the ciphertext outside its expected slot to simulate loss without deleting it.
                Files.move(w.root.resolve("secrets/$ref.dpapi"),w.root.resolve("retained-ciphertext"))
                assertFailsWith<IllegalArgumentException>{verifyUpdatedWorkspace(w,plan)}
                assertTrue(Files.isRegularFile(w.root.resolve("retained-ciphertext")))
            }
        }
    }

    @Test fun unreadableOrCorruptCredentialCannotBeTreatedAsMissing() {
        StudioWorkspace.openStudio(root()).use { w ->
            val ref=newId();w.save(w.state.copy(accounts=listOf(account(ref))))
            val vault=Files.createDirectories(w.root.resolve("secrets"))
            val path=vault.resolve("$ref.dpapi")
            Files.write(path,byteArrayOf(1,2,3))
            assertEquals("READ_ERROR",assertFailsWith<DataProblem>{updateSnapshot(w)}.status)
            assertContentEquals(byteArrayOf(1,2,3),Files.readAllBytes(path))
            Files.move(path,w.root.resolve("retained-invalid-ciphertext"));Files.createDirectory(path)
            assertEquals("READ_ERROR",assertFailsWith<DataProblem>{updateSnapshot(w)}.status)
        }
    }

    private fun withPlan(w:StudioWorkspace,snapshot:JsonObject,action:(Path)->Unit) {
        val installed=Files.createDirectories(root());val cache=Files.createDirectories(root())
        val config=buildJsonObject{put("version",9);put("sourceSha","a".repeat(40));put("reader",w.state.version)}
        writeUpdateJson(installed.resolve("update-config.json"),config)
        val path=Files.createDirectories(cache.resolve("handoffs/fixture")).resolve("plan.json")
        writeUpdateJson(path,buildJsonObject {
            put("format",1);put("nonce","b".repeat(64));put("cache",cache.toString());put("dataRoot",w.root.toString())
            put("approved",config);put("snapshot",snapshot)
        })
        val previous=System.getProperty("jpackage.app-path")
        try {System.setProperty("jpackage.app-path",installed.resolve("fixture.exe").toString());action(path)}
        finally {if(previous==null)System.clearProperty("jpackage.app-path") else System.setProperty("jpackage.app-path",previous)}
    }
}
