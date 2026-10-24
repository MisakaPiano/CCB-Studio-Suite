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
                assertEquals(size,bytes.size);assertEquals(expected,sha256(bytes));assertEquals(size.toLong(),Files.size(path));assertTrue(row.getValue("allocatedBytes").jsonPrimitive.long<size+256L*1024,"Stable read must allocate one input-sized array with bounded overhead");rows+=row
            }
        }
        println("BOUNDED_READ_ALLOCATION="+JsonArray(rows))
    }
    @Test fun exactFileBudgetsBoundariesAndMissingOrUnreadableErrorsRemainCompatible() {
        val base=root()
        for(size in listOf(0,1,8191,8192,8193,65535,65536,65537)) {
            val bytes=ByteArray(size){(it*37+11).toByte()};val file=Files.write(base.resolve("boundary-$size.bin"),bytes,StandardOpenOption.CREATE_NEW)
            assertContentEquals(bytes,readBounded(file,size));assertContentEquals(bytes,readBounded(file,size+1))
            if(size>0)assertEquals("CORRUPT",assertFailsWith<DataProblem>{readBounded(file,size-1)}.status)
            assertContentEquals(bytes,Files.readAllBytes(file))
        }
        assertEquals("MISSING",assertFailsWith<DataProblem>{readBounded(base.resolve("missing.bin"),100)}.status)
        assertEquals("READ_ERROR",assertFailsWith<DataProblem>{readBounded(base,100)}.status)
        val empty=base.resolve("boundary-0.bin")
        assertEquals("CORRUPT",assertFailsWith<DataProblem>{readBounded(empty,-1)}.status)
        assertFailsWith<IllegalArgumentException>{readBounded(empty,-2)}
        assertFailsWith<IllegalArgumentException>{readBounded(empty,Int.MAX_VALUE)}
    }

    @Test fun staleSizeHintsNeverReplaceActualEofOrDynamicBudgetAndMatchLegacyStreamBytes() {
        for(size in listOf(0,1,2,8191,8192,8193))for(max in setOf(0,size,(size-1).coerceAtLeast(0),size+3))for(hint in setOf(0L,1L,(size/2).toLong(),size.toLong(),size.toLong()*2,Long.MAX_VALUE)) {
            val payload=ByteArray(size){(it*17+3).toByte()};var consumed=0
            // Partial reads also model a source shorter/longer than the size observed at open.
            val input=object:java.io.InputStream() {
                override fun read():Int=if(consumed==payload.size)-1 else payload[consumed++].toInt() and 255
                override fun read(target:ByteArray,offset:Int,length:Int):Int {
                    if(length==0)return 0;if(consumed==payload.size)return -1
                    val n=minOf(length,7,payload.size-consumed);System.arraycopy(payload,consumed,target,offset,n);consumed+=n;return n
                }
            }
            val legacy=payload.inputStream().readNBytes(max+1)
            if(legacy.size>max)assertEquals("CORRUPT",assertFailsWith<DataProblem>{readBoundedInput(input,max,hint)}.status)
            else assertContentEquals(legacy,readBoundedInput(input,max,hint))
            assertTrue(consumed<=max+1,"The allocation hint must not authorize reading past the existing sentinel budget")
        }
    }

    @Test fun ioFailureAfterInitialOrGrowthReadReturnsNoPartialResultAndKeepsException() {
        for(hint in listOf(0L,5L,100L)) {
            val fault=java.io.IOException("synthetic bounded input failure");var consumed=0
            val input=object:java.io.InputStream() {
                override fun read():Int {if(consumed==20)throw fault;consumed++;return 42}
                override fun read(target:ByteArray,offset:Int,length:Int):Int {
                    if(length==0)return 0;if(consumed==20)throw fault
                    val n=minOf(length,3,20-consumed);target.fill(42,offset,offset+n);consumed+=n;return n
                }
            }
            assertSame(fault,assertFailsWith<java.io.IOException>{readBoundedInput(input,200,hint)})
            assertEquals(20,consumed)
        }
    }

}
