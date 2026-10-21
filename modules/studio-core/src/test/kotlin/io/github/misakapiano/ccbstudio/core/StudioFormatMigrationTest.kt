package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import io.github.misakapiano.ccbstudio.assets.AssetBackup
import java.io.ByteArrayInputStream
import java.nio.channels.FileChannel
import java.nio.file.*
import java.util.UUID
import kotlin.test.*

class StudioFormatMigrationTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private val pixels=EditorRenderer.synthetic(48,48)
    private fun fixture(p:Path,interrupted:Boolean=false):String {
        Files.createDirectories(p);Files.createFile(p.resolve(".owner.lock"))
        Files.writeString(p.resolve("preview-root.txt"),"CCBStudioSuite/WindowsStudio/v2")
        val id=UUID.randomUUID()
        AssetStore.open(p.resolve("assets")).use{it.put(AssetStore.Request(id,"fixture.png",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),ByteArrayInputStream(pixels))}
        val account=AccountProfile(alias="fixture",service="NAI",provider="SYNTHETIC",model="fixture")
        val state=StudioState(version=2,creation=CreationState(),projects=listOf(EditProject(name="preserved",current=EditSnapshot(listOf(ProjectPage(id.toString()))))),
            jobs=if(interrupted)listOf(JobRecord(account=account,recipe="fixture",state="RUNNING"))else emptyList())
        StateJournal(p.resolve("documents")).commit(state)
        return id.toString()
    }
    private fun inventory(p:Path)=Files.walk(p).use{it.filter{f->Files.isRegularFile(f)}.toList().associate{f->p.relativize(f).toString() to fingerprintFile(f)}}
    @Test fun copyPreservesOriginalBytesHistoryAndEncryptedBlobsAndCanBackupRestore() {
        val source=root();val id=fixture(source);val target=root()
        Files.createDirectories(source.resolve("secrets"));val secret=newId()+".dpapi"
        Files.write(source.resolve("secrets/$secret"),byteArrayOf(1,4,9,16)) // opaque synthetic bytes, no credential access
        Files.createDirectories(source.resolve("exports"));Files.writeString(source.resolve("exports/keep.txt"),"old export")
        val before=inventory(source);StudioFormatMigration.copyVersionTwo(source,target)
        assertEquals(before,inventory(source));assertContentEquals(byteArrayOf(1,4,9,16),Files.readAllBytes(target.resolve("secrets/$secret")))
        assertFalse(Files.exists(target.resolve("exports/keep.txt")));assertTrue(Files.exists(target.resolve("exports/migrations/format-2-to-3.json")))
        val backup=StudioWorkspace.openStudio(target).use{w->assertEquals(3,w.state.version);assertContentEquals(pixels,w.bytes(id));assertEquals("preserved",w.state.projects.single().name);w.backup()}
        val restored=root();StudioRestore.restore(backup,restored)
        StudioWorkspace.openStudio(restored).use{assertContentEquals(pixels,it.bytes(id))}
    }
    @Test fun allPrePublicationFaultsRetainSourceAndRetryToFreshTarget() {
        for(point in listOf("SOURCE_LOCKED","MIGRATION_COPIED","MIGRATION_COMMITTED","MIGRATION_VALIDATED")){
            val source=root();val id=fixture(source);val target=root();val before=inventory(source)
            assertFails{StudioFormatMigration.copyVersionTwo(source,target){if(it==point)error("fixture interruption $point")}}
            assertFalse(Files.exists(target));assertEquals(before,inventory(source))
            StudioFormatMigration.copyVersionTwo(source,target)
            StudioWorkspace.openStudio(target).use{assertContentEquals(pixels,it.bytes(id))}
        }
    }
    @Test fun publishedCopyIsNotOverwrittenOnRetry() {
        val source=root();fixture(source);val target=root()
        assertFails{StudioFormatMigration.copyVersionTwo(source,target){if(it=="MIGRATION_PUBLISHED")error("after publish")}}
        val before=inventory(target);assertFails{StudioFormatMigration.copyVersionTwo(source,target)};assertEquals(before,inventory(target))
    }
    @Test fun abruptProcessExitAtEveryMigrationCommitBoundaryRetainsSourcesAndPublishedWork() {
        val report=mutableListOf<String>()
        for(point in listOf("SOURCE_LOCKED","MIGRATION_COPIED","MIGRATION_COMMITTED","MIGRATION_VALIDATED","MIGRATION_PUBLISHED")) {
            val source=root();val id=fixture(source,true);val target=root();val before=inventory(source)
            val javaExecutable=Path.of(System.getProperty("java.home"),"bin/java.exe")
            val child=ProcessBuilder(javaExecutable.toString(),"-Xmx128m","-XX:ActiveProcessorCount=2","-cp",System.getProperty("java.class.path"),
                MigrationProcessProbe::class.java.name,source.toString(),target.toString(),point).redirectErrorStream(true).start()
            try {
                assertTrue(child.waitFor(30,java.util.concurrent.TimeUnit.SECONDS),"Migration child timeout at $point")
                assertEquals(73,child.exitValue(),child.inputStream.readBytes().decodeToString())
            } finally {if(child.isAlive){child.destroyForcibly();child.waitFor()}}
            assertEquals(before,inventory(source),"Source changed at $point")
            // A fresh process death releases its actual OS locks; no library close/finally ran.
            if(point=="MIGRATION_PUBLISHED") {
                val published=inventory(target)
                assertFails{StudioFormatMigration.copyVersionTwo(source,target)}
                assertEquals(published,inventory(target),"Published copy overwritten after crash")
            } else {
                assertFalse(Files.exists(target),"Unverified copy published at $point")
                StudioFormatMigration.copyVersionTwo(source,target)
            }
            StudioWorkspace.openStudio(target).use{w->
                assertContentEquals(pixels,w.bytes(id));assertEquals("UNCERTAIN",w.state.jobs.single().state)
                assertEquals(3,w.state.version)
            }
            assertEquals(before,inventory(source))
            report+="$point: childExit=73; sourceUnchanged=true; targetVerified=true; interruptedRequest=UNCERTAIN; replay=none"
        }
        val evidence=Path.of("build/migration-process");Files.createDirectories(evidence)
        Files.write(evidence.resolve("commit-boundaries.txt"),report)
    }
    @Test fun bothOwnershipLocksExcludeMigration() {
        val source=root();fixture(source);val before=inventory(source)
        for(name in listOf(".owner.lock","assets/.owner.lock"))FileChannel.open(source.resolve(name),StandardOpenOption.WRITE).use{c->c.lock().use{
            val target=root();assertEquals("IN_USE",assertFailsWith<DataProblem>{StudioFormatMigration.copyVersionTwo(source,target)}.status);assertFalse(Files.exists(target))
        }}
        assertEquals(before,inventory(source))
    }
    @Test fun unknownAuthoritativeContentAndDirectoriesAreNeverIgnored() {
        for(name in listOf("unexplained","documents/.pending-unknown","assets/records/unknown","assets/records/future.rec")){
            val source=root();fixture(source);val target=root();val extra=source.resolve(name)
            if(name.endsWith("unknown"))Files.createDirectory(extra)else Files.writeString(extra,"unknown")
            val before=inventory(source);assertFails{StudioFormatMigration.copyVersionTwo(source,target)}
            assertFalse(Files.exists(target));assertTrue(Files.exists(extra));assertEquals(before,inventory(source))
        }
    }
    @Test fun crashRecoveryOccursOnlyWhenPublishedCopyIsOpened() {
        val source=root();fixture(source,true);val target=root();val before=inventory(source)
        StudioFormatMigration.copyVersionTwo(source,target)
        assertEquals("RUNNING",StateJournal(target.resolve("documents")).state.jobs.single().state)
        StudioWorkspace.openStudio(target).use{assertEquals("UNCERTAIN",it.state.jobs.single().state)}
        assertEquals(before,inventory(source));assertEquals("RUNNING",StateJournal(source.resolve("documents")).state.jobs.single().state)
    }
    @Test fun restoredVersionTwoBackupCanMigrateWithoutOpeningInOldApplication() {
        val source=root();val id=fixture(source);val backup=Files.createDirectories(root())
        AssetStore.open(source.resolve("assets")).use{AssetBackup.create(it,backup.resolve("assets"))}
        Files.createDirectory(backup.resolve("documents"))
        Files.list(source.resolve("documents")).use{it.forEach{p->Files.copy(p,backup.resolve("documents").resolve(p.fileName))}}
        val lines=Files.walk(backup).use{it.filter{p->Files.isRegularFile(p)}.sorted().toList().map{p->
            val f=fingerprintFile(p);"${backup.relativize(p)} ${f.size} ${f.sha256}"
        }}
        Files.writeString(backup.resolve("COMPLETE.txt"),"Secrets excluded; references remain OS-bound.\n"+lines.joinToString("\n"))
        val restored=root();StudioRestore.restore(backup,restored)
        assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{StudioWorkspace.openStudio(restored)}.status)
        val before=inventory(restored);val target=root();StudioFormatMigration.copyVersionTwo(restored,target)
        assertEquals(before,inventory(restored));StudioWorkspace.openStudio(target).use{assertContentEquals(pixels,it.bytes(id))}
    }
}

object MigrationProcessProbe {
    @JvmStatic fun main(args:Array<String>) {
        StudioFormatMigration.copyVersionTwo(Path.of(args[0]),Path.of(args[1])) {
            if(it==args[2])Runtime.getRuntime().halt(73)
        }
        error("Requested migration fault point was not reached")
    }
}
