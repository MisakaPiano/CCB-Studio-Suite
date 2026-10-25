package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.*

private fun legacySha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

class HashEncodingPerformanceTest {
    @Test fun measuredActualShaAllocationAndCanonicalOutputs() {
        val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        for(size in listOf(0,1024,65536)) {
            val input=ByteArray(size){(it*37+11).toByte()};val expected=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input))
            repeat(5){assertEquals(expected,sha256(input));assertEquals(expected,legacySha256(input))}
            repeat(4){round->
                for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {
                    val results=ArrayList<String>(64);val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    repeat(64){results+=if(kind=="legacy")legacySha256(input)else sha256(input)}
                    val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    assertEquals(64,results.size);assertTrue(results.all{it==expected})
                    if(kind=="current")assertTrue(allocatedBytes<64*2048,"SHA hex text must avoid per-byte Formatter allocations")
                    rows+=buildJsonObject{put("payloadBytes",size);put("kind",kind);put("calls",64);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
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
    private fun root()=java.nio.file.Files.createDirectories(java.nio.file.Path.of(System.getProperty("preview.test.root"),newId()))
    @Test fun legacyDigestEnvelopesAcrossTwoVersionsRemainReadableAndCorruptionStillBlocks() {
        val directory=root().resolve("documents");java.nio.file.Files.createDirectory(directory)
        val old=StudioState(version=2,creation=CreationState(composer="legacy synthetic 中文 \uD83C\uDF19"));val states=listOf(old,old.copy(version=3))
        var previous="";val snapshots=mutableListOf<Pair<java.nio.file.Path,ByteArray>>()
        val bodies=states.mapIndexed{index,state->
            val body=documentJson.encodeToString(StudioState.serializer(),state)
            val envelope=buildJsonObject{put("version",1);put("sequence",index+1);put("previous",previous);put("body",body);put("sha256",legacySha256(body.toByteArray()))}
            val bytes=documentJson.encodeToString(JsonObject.serializer(),envelope).toByteArray();val path=directory.resolve("%012d-%s.json".format(index+1,newId()))
            writeNewForced(path,bytes);snapshots+=path to bytes;previous=legacySha256(bytes);body
        }
        val journal=StateJournal(directory);assertEquals(states.last(),journal.state);assertEquals(bodies[0],journal.committedBodyForVersion(2));assertEquals(bodies[1],journal.committedBodyForVersion(3))
        snapshots.forEach{(path,bytes)->assertContentEquals(bytes,java.nio.file.Files.readAllBytes(path));assertEquals(legacySha256(bytes),sha256(bytes))}
        val last=snapshots.last().first;val value=Json.parseToJsonElement(snapshots.last().second.decodeToString()).jsonObject
        val hash=value.getValue("sha256").jsonPrimitive.content;val changed=JsonObject(value+("sha256" to JsonPrimitive((if(hash[0]=='0')"1"else"0")+hash.drop(1))))
        val corrupt=documentJson.encodeToString(JsonObject.serializer(),changed).toByteArray();java.nio.file.Files.write(last,corrupt,java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)
        assertEquals("CORRUPT",assertFailsWith<DataProblem>{StateJournal(directory)}.status);assertContentEquals(corrupt,java.nio.file.Files.readAllBytes(last))
    }
    @Test fun fileFingerprintMatchesLegacyDigestAndKeepsBytesAndBudget() {
        val input=ByteArray(4097){(it*37+11).toByte()};val path=java.nio.file.Files.write(root().resolve("synthetic.bin"),input,java.nio.file.StandardOpenOption.CREATE_NEW)
        assertEquals(FileFingerprint(input.size.toLong(),legacySha256(input)),fingerprintFile(path,input.size.toLong()))
        assertEquals(legacySha256(input),sha256(input));assertEquals("CORRUPT",assertFailsWith<DataProblem>{fingerprintFile(path,input.size.toLong()-1)}.status)
        assertContentEquals(input,java.nio.file.Files.readAllBytes(path))
    }

}
