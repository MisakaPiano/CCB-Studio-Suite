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

class IngressCopyPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(path:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun original(parent:Path,size:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("ingress-$size.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(size);val type="paDd".toByteArray();out.write(type)
            val crc=CRC32();crc.update(type);val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    @Test fun measuredManagedCopyAllocationAndReplay() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        for(size in listOf(3*1024*1024,12*1024*1024)) {
            val input=original(base,size);val expected=digest(input)
            StudioWorkspace.openStudio(base.resolve("workspace-$size")).use{w->
                val repository=LibraryRepository(w);repository.pauseCollections(true)
                fun admit(name:String):String {val id=UUID.randomUUID();Files.newInputStream(input).use{w.assets.put(AssetStore.Request(id,name,AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)};return id.toString()}
                repository.retainIngressBatch(listOf(admit("warm.png")))
                repeat(3){round->
                    val id=admit("round-$round.png");val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val output=repository.retainIngressBatch(listOf(id)).single()
                    rows+=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                    assertEquals(expected,digest(output));assertEquals(Files.size(input),Files.size(output));assertEquals(expected,w.assets.read(UUID.fromString(id)).require().sha256())
                    val observation=repository.load().observations.values.single{it.assetId==id};assertEquals("PRESENT",observation.status);assertEquals(expected,observation.sha256)
                    if(round==2){assertEquals(output,repository.retainIngressBatch(listOf(id)).single());assertEquals(expected,digest(output))}
                }
                assertTrue(repository.automaticFolders().isEmpty(),"Fixture collection stays paused; no scan was started")
            }
        }
        println("INGRESS_COPY_ALLOCATION="+JsonArray(rows))
    }
}
