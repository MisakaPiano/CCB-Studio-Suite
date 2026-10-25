package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.*

class HashEncodingPerformanceTest {
    @Test fun measuredActualShaAllocationAndCanonicalOutputs() {
        val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        for(size in listOf(0,1024,65536)) {
            val input=ByteArray(size){(it*37+11).toByte()};val expected=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input))
            repeat(5){assertEquals(expected,sha256(input))}
            repeat(4){round->
                val results=ArrayList<String>(64);val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                repeat(64){results+=sha256(input)}
                val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                assertEquals(64,results.size);assertTrue(results.all{it==expected})
                rows+=buildJsonObject{put("payloadBytes",size);put("calls",64);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("HASH_ENCODING_ALLOCATION="+JsonArray(rows))
    }
    @Test fun allByteValuesLocalesAndKnownVectorsKeepLowercaseAsciiHex() {
        val bytes=ByteArray(256){it.toByte()};val expected=java.util.HexFormat.of().formatHex(bytes)
        for(locale in listOf(Locale.ROOT,Locale.US,Locale.CHINA,Locale.JAPAN,Locale.forLanguageTag("ar"),Locale.forLanguageTag("tr"))) {
            val legacy=bytes.joinToString(""){java.lang.String.format(locale,"%02x",it)}
            assertEquals(expected,legacy,"Hex format must retain every byte and locale without mutating JVM defaults")
        }
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",sha256(byteArrayOf()))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",sha256("abc".toByteArray()))
        assertTrue(sha256(bytes).matches(Regex("[0-9a-f]{64}")))
    }
}
