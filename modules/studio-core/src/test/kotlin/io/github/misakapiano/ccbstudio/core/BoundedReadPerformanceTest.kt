package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import kotlin.test.*

class BoundedReadPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    @Test fun measuredStableFileReadAllocationAndExactBytes() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        for(size in listOf(3*1024*1024,12*1024*1024)) {
            val path=base.resolve("synthetic-$size.bin");val md=MessageDigest.getInstance("SHA-256")
            Files.newOutputStream(path,StandardOpenOption.CREATE_NEW).use{out->val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
                while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);md.update(block,0,n);remaining-=n}}
            val expected=java.util.HexFormat.of().formatHex(md.digest())
            assertEquals(expected,sha256(readBounded(path,32*1024*1024)))
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                val bytes=readBounded(path,32*1024*1024)
                val row=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                assertEquals(size,bytes.size);assertEquals(expected,sha256(bytes));assertEquals(size.toLong(),Files.size(path));rows+=row
            }
        }
        println("BOUNDED_READ_ALLOCATION="+JsonArray(rows))
    }
}
