package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class LibraryMonitorDiscoveryTest {
    @Test fun idlePeriodicDiscoveryDoesNotRequestAnotherContentScan() {
        val root=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
        repeat(8){Files.writeString(root.resolve("$it.bin"),"synthetic-$it")}
        val folder=LibraryFolder(path=root.toString(),category="images")
        LibraryDirectoryMonitor(listOf(folder),reconcileMillis=0).use { monitor->
            assertEquals(setOf(folder.id),monitor.changes(0))
            repeat(3){assertTrue(monitor.changes(0).isEmpty())}
            assertEquals("synthetic-0",Files.readString(root.resolve("0.bin")))
        }
    }
    @Test fun metadataDiscoveryFindsChangesWithoutConsumingWatchEvents() {
        val root=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
        val file=Files.writeString(root.resolve("image.bin"),"synthetic")
        val folder=LibraryFolder(path=root.toString(),category="images",recursive=true)
        LibraryDirectoryMonitor(listOf(folder)).use { monitor->
            assertTrue(monitor.reconcileMetadata().isEmpty())
            Files.writeString(file,"synthetic-changed")
            assertEquals(setOf(folder.id),monitor.reconcileMetadata())
            assertTrue(monitor.reconcileMetadata().isEmpty())
            val nested=Files.createDirectory(root.resolve("nested"));Files.writeString(nested.resolve("new.bin"),"new")
            assertEquals(setOf(folder.id),monitor.reconcileMetadata())
            Files.move(file,root.resolveSibling(newId()+".retained.bin"))
            assertEquals(setOf(folder.id),monitor.reconcileMetadata())
        }
    }
}
