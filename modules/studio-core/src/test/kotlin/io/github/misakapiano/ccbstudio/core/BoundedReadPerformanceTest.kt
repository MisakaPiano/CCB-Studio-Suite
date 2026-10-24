package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.security.MessageDigest
import kotlin.test.*

private fun legacyBoundedRead(path:Path,max:Int):ByteArray {
    safePath(path)
    try {
        Files.newInputStream(path,NOFOLLOW_LINKS).use {
            val bytes=it.readNBytes(max+1)
            if(bytes.size>max) throw DataProblem("CORRUPT","File exceeds budget; retained")
            return bytes
        }
    } catch(e:NoSuchFileException) { throw DataProblem("MISSING","Required file missing",e) }
    catch(e:DataProblem) { throw e }
    catch(e:java.io.IOException) { throw DataProblem("READ_ERROR","File unreadable; retained",e) }
}

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
            assertEquals(expected,sha256(readBounded(path,32*1024*1024)));assertEquals(expected,sha256(legacyBoundedRead(path,32*1024*1024)))
            repeat(4){round->
                for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val bytes=if(kind=="legacy")legacyBoundedRead(path,32*1024*1024)else readBounded(path,32*1024*1024)
                    // Sample the operation before creating any JSON/diagnostic output.
                    val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    val row=buildJsonObject{put("payloadBytes",size);put("kind",kind);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                    println("BOUNDED_READ_SAMPLE="+row)
                    assertEquals(size,bytes.size);assertEquals(expected,sha256(bytes));assertEquals(size.toLong(),Files.size(path))
                    assertTrue(allocatedBytes>=size,"Allocation instrumentation must include the returned byte array")
                    if(kind=="current")assertTrue(allocatedBytes<size+256L*1024,"Stable read allocation=$allocatedBytes,source=$size exceeds one input-sized array with bounded overhead")
                    rows+=row
                }
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

    @Test fun boundedReadRequestsCannotCreateOriginalSizedNativeBufferAndZeroReadKeepsBytes() {
        val payload=ByteArray(3*65536+17){(it*37+11).toByte()}
        for(hint in listOf(0L,payload.size.toLong(),Long.MAX_VALUE)) {
            var consumed=0;var largest=0;var zeroOnce=true
            val input=object:java.io.InputStream() {
                override fun read():Int=if(consumed==payload.size)-1 else payload[consumed++].toInt() and 255
                override fun read(target:ByteArray,offset:Int,length:Int):Int {
                    largest=maxOf(largest,length)
                    if(length==0)return 0
                    if(zeroOnce){zeroOnce=false;return 0}
                    if(consumed==payload.size)return -1
                    val n=minOf(length,payload.size-consumed);System.arraycopy(payload,consumed,target,offset,n);consumed+=n;return n
                }
            }
            assertContentEquals(payload,readBoundedInput(input,payload.size,hint))
            assertEquals(payload.size,consumed);assertTrue(largest<=65536,"File-channel requests must stay bounded independently of original size")
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
