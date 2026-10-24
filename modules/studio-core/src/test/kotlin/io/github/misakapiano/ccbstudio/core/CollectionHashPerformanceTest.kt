package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.io.DataOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import java.util.zip.CRC32
import kotlin.test.*

/** Explicit scans of two newly owned synthetic folders; never enables a user folder/watcher. */
class CollectionHashPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(path:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun original(parent:Path,size:Int,seed:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("source-$seed.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(size)
            val type="paDd".toByteArray();val crc=CRC32();crc.update(type);out.write(type)
            val block=ByteArray(65536){(it*37+seed*11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    @Test fun measuredUnchangedScanAllocationAndIdentity() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        for(size in listOf(3*1024*1024,12*1024*1024)) {
            val folder=Files.createDirectory(base.resolve("source-$size"));val files=(1..2).map{original(folder,size,it)};val expected=files.associateWith(::digest)
            StudioWorkspace.openStudio(base.resolve("workspace-$size")).use{w->
                val repo=LibraryRepository(w);val registration=repo.addFolder(folder,"images",false).folders.single();repo.pauseCollections(true)
                assertTrue(repo.automaticFolders().isEmpty())
                assertTrue(repo.scan(registration.id).all{it.status=="IMPORTED"});assertTrue(repo.scan(registration.id).all{it.status=="UNCHANGED"})
                val before=repo.load();val count=w.catalog().size
                repeat(3){round->
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val result=repo.scan(registration.id)
                    rows+=buildJsonObject{put("payloadBytes",size);put("files",2);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                    assertEquals(2,result.size);assertTrue(result.all{it.status=="UNCHANGED"});assertEquals(before,repo.load());assertEquals(count,w.catalog().size)
                    files.forEach{path->assertEquals(expected.getValue(path),digest(path))};assertTrue(repo.automaticFolders().isEmpty())
                }
            }
        }
        println("COLLECTION_HASH_ALLOCATION="+JsonArray(rows))
    }
}
