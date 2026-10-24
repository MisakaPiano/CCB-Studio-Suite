package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.io.DataOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import kotlin.test.*

/** Calls the existing edit-project original export, not TypeSet or a replacement benchmark. */
class EditorOriginalPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(path:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun original(parent:Path,size:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("editor-$size.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(size)
            val type="paDd".toByteArray();val crc=CRC32();crc.update(type);out.write(type)
            val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    private fun admit(w:StudioWorkspace,input:Path):String {
        val id=UUID.randomUUID()
        Files.newInputStream(input).use{w.assets.put(AssetStore.Request(id,input.fileName.toString(),AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
        return id.toString()
    }
    @Test fun measuredOriginalEditorExportAllocation() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val input=original(base,size);val expected=digest(input);val id=admit(w,input);val project=w.newProject("Synthetic original $size",listOf(id));val state=w.state
                w.export(project,listOf(id),"ORIGINAL")
                repeat(3){round->
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val output=w.export(project,listOf(id),"ORIGINAL")
                    val row=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                    val name="001-$id.png";assertEquals(expected,digest(output.resolve(name)))
                    assertEquals("CCB Studio Suite ORIGINAL\n$name ${Files.size(input)} $expected",Files.readString(output.resolve("COMPLETE.txt")))
                    assertEquals(state,w.state);assertEquals(expected,w.assets.read(UUID.fromString(id)).require().sha256())
                    assertFalse(Files.list(output).use{it.anyMatch{p->p.fileName.toString().endsWith(".pending")}})
                    rows+=row
                }
            }
        }
        println("EDITOR_ORIGINAL_ALLOCATION="+JsonArray(rows))
    }
}
