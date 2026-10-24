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
        val bySize=rows.groupBy{it.getValue("payloadBytes").jsonPrimitive.int}
        for(round in 0..2) {
            val small=bySize.getValue(3*1024*1024).single{it.getValue("round").jsonPrimitive.int==round}.getValue("allocatedBytes").jsonPrimitive.long
            val large=bySize.getValue(12*1024*1024).single{it.getValue("round").jsonPrimitive.int==round}.getValue("allocatedBytes").jsonPrimitive.long
            assertTrue(large-small<2L*1024*1024,"Encoded original growth must not add whole-file buffers; metadata cost is separate")
        }
        println("INGRESS_COPY_ALLOCATION="+JsonArray(rows))
    }

    @Test fun modifiedIntakeRemainsPendingWithoutReplacingItOrOriginal() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val repo=LibraryRepository(w);repo.pauseCollections(true);val bytes=EditorRenderer.synthetic(16,16);val id=w.importImage("source.png",bytes)
            val output=repo.retainIngress(id);Files.writeString(output,"unknown-keep",StandardOpenOption.TRUNCATE_EXISTING)
            assertFailsWith<IllegalArgumentException>{repo.retainIngress(id)}
            assertEquals("unknown-keep",Files.readString(output));assertContentEquals(bytes,w.bytes(id))
            assertEquals("COPY_PENDING",repo.load().observations.values.single{it.assetId==id}.status)
            assertTrue(repo.automaticFolders().isEmpty())
        }
    }

    @Test fun existing32MiBCopyBudgetStillRejectsAndRetainsOversizeSecondaryFile() {
        val base=root();val input=base.resolve("oversize.bin");val block=ByteArray(65536){(it*29).toByte()}
        Files.newOutputStream(input,StandardOpenOption.CREATE_NEW).use{out->repeat(512){out.write(block)};out.write(11)}
        val expected=digest(input)
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val id=UUID.randomUUID();Files.newInputStream(input).use{w.assets.put(AssetStore.Request(id,"opaque.bin",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),SharedAssets.DOCUMENT.toByteArray()),it)}
            val repo=LibraryRepository(w);repo.pauseCollections(true)
            assertEquals("CORRUPT",assertFailsWith<DataProblem>{repo.retainIngress(id.toString())}.status)
            val state=repo.load();assertEquals("COPY_PENDING",state.observations.values.single().status)
            val secondary=Path.of(state.folders.single{it.category=="imports"}.path).resolve("$id-opaque.bin")
            assertEquals(32L*1024*1024+1,Files.size(secondary));assertEquals(expected,digest(secondary));assertEquals(expected,w.assets.read(id).require().sha256())
            assertTrue(repo.automaticFolders().isEmpty())
        }
    }
}
