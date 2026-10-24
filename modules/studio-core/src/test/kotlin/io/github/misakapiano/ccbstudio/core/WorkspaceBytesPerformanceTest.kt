package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.*

class WorkspaceBytesPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun source(base:Path,size:Int):Pair<Path,String> {
        val path=base.resolve("synthetic-$size.bin");val md=MessageDigest.getInstance("SHA-256")
        Files.newOutputStream(path,StandardOpenOption.CREATE_NEW).use{out->val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);md.update(block,0,n);remaining-=n}}
        return path to java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun admit(w:StudioWorkspace,path:Path):String {
        val id=UUID.randomUUID();Files.newInputStream(path).use{w.assets.put(AssetStore.Request(id,path.fileName.toString(),AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)};return id.toString()
    }
    @Test fun measuredVerifiedAssetBytesAllocationAndExactBytes() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val (input,expected)=source(base,size);val id=admit(w,input);val state=w.state;assertEquals(expected,sha256(w.bytes(id)))
                repeat(4){round->
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val bytes=w.bytes(id)
                    val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    val row=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                    assertEquals(size,bytes.size);assertEquals(expected,sha256(bytes));assertEquals(state,w.state);assertEquals(expected,w.assets.read(UUID.fromString(id)).require().sha256());assertTrue(allocatedBytes<2L*size+512*1024,"Verified byte returns must avoid geometric full-array growth");rows+=row
                }
            }
        }
        println("WORKSPACE_BYTES_ALLOCATION="+JsonArray(rows))
    }
    @Test fun byteBoundariesReturnIndependentBuffersWithoutChangingStoreOrState() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(0,1,63,64,65535,65536,65537,131071,131072,131073)) {
                val (input,expected)=source(base,size);val id=admit(w,input);val state=w.state;val count=w.catalog().size
                val first=w.bytes(id);val second=w.bytes(id);assertNotSame(first,second);assertEquals(size,first.size);assertEquals(expected,sha256(first));assertContentEquals(first,second)
                if(first.isNotEmpty())first[0]=(first[0].toInt() xor 1).toByte()
                assertEquals(expected,sha256(second));assertEquals(expected,sha256(w.bytes(id)))
                assertEquals(expected,w.assets.read(UUID.fromString(id)).require().sha256());assertEquals(state,w.state);assertEquals(count,w.catalog().size)
            }
        }
    }
    @Test fun corruptVerifiedSourceCannotReturnPartialBytesOrReplaceSourceAndRecord() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val (input,expected)=source(base,4096);val id=admit(w,input);val state=w.state;val asset=w.assets.read(UUID.fromString(id)).require()
            val blob=w.root.resolve("assets/objects/${asset.sha256()}.blob");val changed=Files.readAllBytes(blob);changed[100]=(changed[100].toInt() xor 1).toByte();Files.write(blob,changed,StandardOpenOption.TRUNCATE_EXISTING)
            assertFails{w.bytes(id)};assertContentEquals(changed,Files.readAllBytes(blob));assertEquals(expected,sha256(Files.readAllBytes(input)));assertEquals(state,w.state)
        }
    }
    @Test fun reusedInputBlocksMixedWritesOffsetsAndBadRangesKeepByteOwnership() {
        val buffer=AssetBytesBuffer();val source=ByteArray(70003){(it*37+11).toByte()};val expected=java.io.ByteArrayOutputStream()
        buffer.write(source,3,65537);expected.write(source,3,65537);source.fill(0)
        buffer.write(255);expected.write(255);buffer.write(byteArrayOf(9,8,7),1,1);expected.write(8)
        buffer.write(byteArrayOf(),0,0);assertContentEquals(expected.toByteArray(),buffer.bytes())
        val before=buffer.bytes();assertFailsWith<IndexOutOfBoundsException>{buffer.write(source,-1,2)};assertFailsWith<IndexOutOfBoundsException>{buffer.write(source,source.size,1)};assertContentEquals(before,buffer.bytes())
    }

}
