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
                assertEquals("UPDATE_SNAPSHOT_MISMATCH",assertFailsWith<DataProblem>{verifyUpdatedWorkspace(w,plan)}.status)
                assertTrue(Files.isRegularFile(w.root.resolve("retained-ciphertext")))
            }
        }
    }
    @Test fun frozenAndEditedAccountCredentialsBothVerifyAndMissingFrozenCredentialRejectsB() {
        StudioWorkspace.openStudio(root()).use{w->
            val vault=SecretVault(w.root.resolve("secrets"))
            val a=vault.put("synthetic queued A".toByteArray());val b=vault.put("synthetic edited B".toByteArray())
            val frozen=AccountProfile(alias="queued",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="fixture",secretRef=a)
            val edited=frozen.copy(alias="edited",secretRef=b)
            w.save(w.state.copy(accounts=listOf(edited),batch=w.state.batch.copy(accountId=frozen.id,accountSnapshot=frozen)))
            val before=updateSnapshot(w);val credentials=before["credentials"]!!.jsonArray.map{it.jsonObject}
            assertEquals(setOf(a,b),credentials.map{it["ref"]!!.jsonPrimitive.content}.toSet())
            assertTrue(credentials.all{it["sha256"]!=null})
            withPlan(w,before){plan->
                assertEquals(before,verifyUpdatedWorkspace(w,plan)["snapshot"])
                Files.move(w.root.resolve("secrets/$a.dpapi"),w.root.resolve("retained-frozen-ciphertext"))
                assertEquals("UPDATE_SNAPSHOT_MISMATCH",assertFailsWith<DataProblem>{verifyUpdatedWorkspace(w,plan)}.status)
                assertContentEquals("synthetic edited B".toByteArray(),vault.use(b){it.copyOf()})
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

    @Test fun preBatchWriterSnapshotVerifiesWithoutHashingNewReaderDefaults() {
        val r=root()
        StudioWorkspace.openStudio(r).use{w->w.importImage("synthetic.png",EditorRenderer.synthetic(32,32))}
        val file=Files.list(r.resolve("documents")).use{it.sorted().toList().last()}
        val envelope=Json.parseToJsonElement(Files.readString(file)).jsonObject
        val oldBody=JsonObject(Json.parseToJsonElement(envelope["body"]!!.jsonPrimitive.content).jsonObject.filterKeys{it!="batch"}).toString()
        Files.writeString(file,JsonObject(envelope+mapOf("body" to JsonPrimitive(oldBody),"sha256" to JsonPrimitive(sha256(oldBody.toByteArray())))).toString())
        var first:StudioState?=null
        repeat(2){StudioWorkspace.openStudio(r).use{w->
            if(first==null)first=w.state else assertEquals(first,w.state,"Absent new fields must not generate a different batch identity on every read")
            assertFalse(oldBody.contains("\"batch\""))
            val expected=JsonObject(updateSnapshot(w).filterKeys{it!="stateEncoding"}+mapOf("stateSha256" to JsonPrimitive(sha256(oldBody.toByteArray()))))
            withPlan(w,expected){plan->assertEquals(expected,verifyUpdatedWorkspace(w,plan)["snapshot"])}
            val falseSnapshot=JsonObject(expected+mapOf("stateSha256" to JsonPrimitive("0".repeat(64))))
            withPlan(w,falseSnapshot){plan->assertEquals("UPDATE_SNAPSHOT_MISMATCH",assertFailsWith<DataProblem>{verifyUpdatedWorkspace(w,plan)}.status)}
        }}
    }

    @Test fun formatCopyUsesAuthenticatedSourceCommitAndRejectsLaterChanges() {
        val source=root();Files.createDirectories(source);Files.createFile(source.resolve(".owner.lock"))
        Files.writeString(source.resolve("preview-root.txt"),"CCBStudioSuite/WindowsStudio/v2")
        io.github.misakapiano.ccbstudio.assets.AssetStore.open(source.resolve("assets")).close()
        val old=StateJournal(source.resolve("documents"));old.commit(StudioState(version=2))
        val destination=root();StudioFormatMigration.copyVersionTwo(source,destination)
        StudioWorkspace.openStudio(destination).use{w->
            for(legacy in listOf(false,true)) {
                val snapshot=updateSnapshot(w,2,legacy)
                assertEquals(sha256(requireNotNull(old.committedBody).toByteArray()),snapshot["stateSha256"]!!.jsonPrimitive.content)
                withPlan(w,snapshot,2){plan->assertEquals(snapshot,verifyUpdatedWorkspace(w,plan)["snapshot"])}
            }
            w.save(w.state.copy(accounts=listOf(account(newId()))))
            assertEquals("UPDATE_SNAPSHOT_MISMATCH",assertFailsWith<DataProblem>{updateSnapshot(w,2)}.status)
        }
    }

    private fun withPlan(w:StudioWorkspace,snapshot:JsonObject,sourceVersion:Int=w.state.version,action:(Path)->Unit) {
        val installed=Files.createDirectories(root());val cache=Files.createDirectories(root())
        val config=buildJsonObject{put("version",9);put("sourceSha","a".repeat(40));put("reader",w.state.version)}
        writeUpdateJson(installed.resolve("update-config.json"),config)
        val path=Files.createDirectories(cache.resolve("handoffs/fixture")).resolve("plan.json")
        writeUpdateJson(path,buildJsonObject {
            put("format",1);put("nonce","b".repeat(64));put("cache",cache.toString());put("dataRoot",w.root.toString())
            put("sourceDataVersion",sourceVersion);put("targetDataVersion",w.state.version)
            put("approved",config);put("snapshot",snapshot)
        })
        val previous=System.getProperty("jpackage.app-path")
        try {System.setProperty("jpackage.app-path",installed.resolve("fixture.exe").toString());action(path)}
        finally {if(previous==null)System.clearProperty("jpackage.app-path") else System.setProperty("jpackage.app-path",previous)}
    }
}
