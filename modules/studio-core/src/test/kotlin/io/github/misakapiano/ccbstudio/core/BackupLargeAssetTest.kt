package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import java.nio.file.*
import java.util.UUID
import java.util.zip.*
import java.security.MessageDigest
import kotlin.test.*

class BackupLargeAssetTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun digest(path:Path):String {val d=MessageDigest.getInstance("SHA-256");Files.newInputStream(path).use{input->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;d.update(b,0,n)}};return d.digest().joinToString(""){"%02x".format(it)}}
    @Test fun largeArchiveBackupRestoresExactBytesWithoutImageSizeLimit() {
        val file=Files.createDirectories(root()).resolve("large-synthetic.zip");val size=33*1024*1024;val block=ByteArray(65536){(it*31).toByte()}
        val crc=CRC32();repeat(size/block.size){crc.update(block)}
        ZipOutputStream(Files.newOutputStream(file,StandardOpenOption.CREATE_NEW)).use{out->val entry=ZipEntry("synthetic.bin");entry.method=ZipEntry.STORED;entry.size=size.toLong();entry.compressedSize=size.toLong();entry.crc=crc.value;out.putNextEntry(entry);repeat(size/block.size){out.write(block)};out.closeEntry()}
        val expected=digest(file);val data=root();val destination=root();val id=UUID.randomUUID();var backup:Path?=null
        StudioWorkspace.openStudio(data).use{w->
            Files.newInputStream(file).use{w.assets.put(AssetStore.Request(id,"large-synthetic.zip",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),SharedAssets.DOCUMENT.toByteArray()),it)}
            backup=w.backup()
        }
        StudioRestore.restore(backup!!,destination)
        StudioWorkspace.openStudio(destination).use{w->val output=Files.createDirectories(root()).resolve("restored.zip");Files.newOutputStream(output,StandardOpenOption.CREATE_NEW).use{w.assets.export(id,it)};assertEquals(expected,digest(output));assertEquals(Files.size(file),Files.size(output));ZipFile(output.toFile()).use{assertEquals(size.toLong(),it.getEntry("synthetic.bin").size)}}
        StudioWorkspace.openStudio(data).use{w->assertEquals(expected,w.assets.read(id).require().sha256())}
        val blob=Files.list(backup!!.resolve("assets/objects")).use{it.findFirst().get()}
        java.nio.channels.FileChannel.open(blob,StandardOpenOption.WRITE).use{it.position(1024);it.write(java.nio.ByteBuffer.wrap(byteArrayOf(42)))}
        val blocked=root();assertEquals("CORRUPT",assertFailsWith<DataProblem>{StudioRestore.restore(backup!!,blocked)}.status);assertFalse(Files.exists(blocked))
        StudioWorkspace.openStudio(data).use{w->assertEquals(expected,w.assets.read(id).require().sha256())}
    }
    @Test fun streamingChecksEnforceExactBoundaryAndKeepFileOnFailure() {
        val file=Files.createDirectories(root()).resolve("bounded.bin");val bytes=ByteArray(1024){it.toByte()};Files.write(file,bytes)
        assertEquals(FileFingerprint(1024,sha256(bytes)),fingerprintFile(file,1024))
        assertEquals("CORRUPT",assertFailsWith<DataProblem>{fingerprintFile(file,1023)}.status)
        assertContentEquals(bytes,Files.readAllBytes(file))
        assertEquals("MISSING",assertFailsWith<DataProblem>{fingerprintFile(file.resolveSibling("missing.bin"))}.status)
        assertEquals("READ_ERROR",assertFailsWith<DataProblem>{fingerprintFile(file.parent)}.status)
    }
}
