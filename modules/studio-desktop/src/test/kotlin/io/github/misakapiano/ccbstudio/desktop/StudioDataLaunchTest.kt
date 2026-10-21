package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.nio.file.*
import kotlin.test.*

class StudioDataLaunchTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId()).toAbsolutePath()
    private fun fixture(p:Path) {
        Files.createDirectories(p);Files.createFile(p.resolve(".owner.lock"))
        Files.writeString(p.resolve("preview-root.txt"),"CCBStudioSuite/WindowsStudio/v2")
        AssetStore.open(p.resolve("assets")).close()
        StateJournal(p.resolve("documents")).commit(StudioState(version=2,creation=CreationState()))
    }
    @Test fun cancelledManualMigrationDoesNotPublishOrModifySource() {
        val source=root();fixture(source)
        assertEquals("MIGRATION_CANCELLED",assertFailsWith<DataProblem>{openStudioForLaunch(source,root(),null,{_,_->false})}.status)
        assertEquals(2,StateJournal(source.resolve("documents")).state.version)
        assertFalse(Files.exists(source.resolveSibling(source.fileName.toString()+"-format3")))
        openStudioForLaunch(source,root(),null,{a,b->assertEquals(source,a);assertNotEquals(a,b);true}).use{assertEquals(3,it.state.version)}
        assertFails{openStudioForLaunch(source,root(),null,{_,_->true})}
    }
    @Test fun installedIdentityAndApprovedDestinationBindMigration() {
        val source=root();fixture(source);val runtime=root();val installed=Files.createDirectories(root());val nonce="a".repeat(64)
        val target=updateDataDestination(source,2,3,nonce)
        Files.writeString(installed.resolve("update-config.json"),"""{"version":4,"sourceSha":"${"b".repeat(40)}","reader":3}""")
        val plan=Files.createDirectories(runtime.resolve("updates/handoffs/test")).resolve("plan.json")
        fun write(destination:Path,version:Int=4){Files.writeString(plan,buildJsonObject{
            put("format",1);put("nonce",nonce);put("dataRoot",source.toString());put("sourceDataVersion",2);put("targetDataVersion",3);put("destinationDataRoot",destination.toString())
            put("approved",buildJsonObject{put("version",version);put("sourceSha","b".repeat(40))})
        }.toString())}
        write(root());assertFails{openStudioForLaunch(source,runtime,plan,{_,_->error("not manual")},installed)};assertFalse(Files.exists(target))
        write(target,9);assertFails{openStudioForLaunch(source,runtime,plan,{_,_->error("not manual")},installed)};assertFalse(Files.exists(target))
        write(target);openStudioForLaunch(source,runtime,plan,{_,_->error("not manual")},installed).use{assertEquals(target,it.root);assertEquals(3,it.state.version)}
        assertEquals(2,StateJournal(source.resolve("documents")).state.version)
    }
}
