package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import kotlin.test.*

class JournalCommitPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun next(w:StudioWorkspace,detail:String)=w.state.copy(jobs=listOf(JobRecord(account=AccountProfile(alias="synthetic fixture",service="NAI",provider="SYNTHETIC",model="fixture"),recipe="",state="FAILED",detail=detail)))
    private fun body(root:Path):Pair<String,String> {
        val latest=Files.list(root.resolve("documents")).use{it.sorted().toList().last()}
        val envelope=Json.parseToJsonElement(readBounded(latest,4*1024*1024).decodeToString()).jsonObject
        return envelope.getValue("body").jsonPrimitive.content to envelope.getValue("sha256").jsonPrimitive.content
    }
    @Test fun measuredActualSaveAllocationAndPersistedBodyIdentity() {
        val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        for(size in listOf(64*1024,256*1024)) {
            val data=root();var saved:StudioState?=null
            StudioWorkspace.openStudio(data).use{w->
                val payload="SYNTHETIC-DETAIL:"+"a".repeat(size);w.save(next(w,payload+"warmup"))
                repeat(4){round->
                    val state=next(w,payload+round);val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    w.save(state)
                    val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    rows+=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                    val (actual,hash)=body(data);assertEquals(actual,w.committedStateBody);assertEquals(hash,sha256(actual.toByteArray()));assertEquals(state,documentJson.decodeFromString<StudioState>(actual));assertEquals(actual,w.committedStateBodyForVersion(3));saved=state
                }
            }
            StudioWorkspace.openStudio(data).use{assertEquals(saved,it.state)}
        }
        println("JOURNAL_COMMIT_ALLOCATION="+JsonArray(rows))
    }
    @Test fun unicodeAndUnpairedSurrogateBodyAndSnapshotGuardsMatchActualPersistedBytes() {
        for(value in listOf("ASCII", "中文", "quote\\\"\t\n", "emoji-\uD83C\uDF19", "lone-high-\uD800", "lone-low-\uDFFF")) {
            val data=root();var persisted:StudioState?=null;var validHash=false
            StudioWorkspace.openStudio(data).use{w->
                w.save(next(w,value));val (actual,hash)=body(data);persisted=documentJson.decodeFromString<StudioState>(actual);validHash=hash==sha256(actual.toByteArray())
                assertEquals(actual,w.committedStateBody,"In-memory committed body must represent actual UTF-8 bytes, including historical replacement/escaping behavior")
                if(persisted==w.state)assertEquals(actual,w.committedStateBodyForVersion(3))
                else assertEquals("UPDATE_SNAPSHOT_MISMATCH",assertFailsWith<DataProblem>{w.committedStateBodyForVersion(3)}.status)
            }
            if(validHash)StudioWorkspace.openStudio(data).use{assertEquals(persisted,it.state)}
            else assertEquals("CORRUPT",assertFailsWith<DataProblem>{StudioWorkspace.openStudio(data)}.status)
        }
    }
}
