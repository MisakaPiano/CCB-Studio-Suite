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

class TypesetOriginalPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(path:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun original(parent:Path,size:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("typeset-$size.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(size)
            val type="paDd".toByteArray();val crc=CRC32();crc.update(type);out.write(type)
            val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    @Test fun measuredOriginalBatchAllocation() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val input=original(base,size);val expected=digest(input);val id=UUID.randomUUID()
                Files.newInputStream(input).use{w.assets.put(AssetStore.Request(id,"source.png",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
                val repository=TypesetRepository(w);val project=repository.create("Synthetic originals",listOf(id.toString()));val document=repository.load(project)
                val parent=Files.createDirectory(base.resolve("exports-$size"));val request=TypesetExportRequest(document.pages.map{it.id},variant="original",includeRecipe=true)
                TypesetExport.create(w,project,document,parent,request)
                repeat(3){round->
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val output=TypesetExport.create(w,project,document,parent,request)
                    val row=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                    val receipt=TypesetExport.verify(output);assertEquals(2,receipt.files.size)
                    assertEquals(expected,digest(output.resolve("001.png")));assertEquals(id.toString(),receipt.assetIds["001.png"])
                    assertEquals("original-byte-identical",receipt.metadataStatuses["001.png"])
                    assertEquals(document,repository.load(project));assertEquals(expected,w.assets.read(id).require().sha256())
                    rows+=row
                }
            }
        }
        println("TYPESET_ORIGINAL_ALLOCATION="+JsonArray(rows))
    }
}
