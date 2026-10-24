package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.*

/** Synthetic metadata only: no workspace, image bytes, network, or native application. */
class OrganizationPerformanceTest {
    private fun id(n:Int)=UUID(0,n.toLong()+1).toString()
    private fun fixture(images:Int):Pair<GenerationOrganization,GalleryHistoryProjection> {
        val batches=(0 until images/10).map{n->GenerationBatch(id(n),if(n%2==0)"manual" else "auto","batch-$n",1,2,
            groups=listOf(GenerationBatchGroup(id(100000+n),"group-$n",(0 until 10).map{id(200000+n*10+it)})))}
        val origins=(0 until images).associate{n->id(200000+n) to BatchResultOrigin(id(n/10),id(100000+n/10),"group-${n/10}",id(500000+n),"横",1,n.toLong(),"batch-${n/10}")}
        return GenerationOrganization(batches=batches) to GalleryHistoryProjection(emptyList(),emptyMap(),origins)
    }
    @Test fun batchListingMetadataBaseline() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val records=mutableListOf<JsonObject>()
        val gallery=GalleryViewState().apply{enterBatch(emptySet(),"批次","org:batches:")}
        listOf(100,1000,10000).forEach{size->
            val (org,p)=fixture(size)
            repeat(2){organizationFolders(gallery,emptyList(),p,org)}
            repeat(3){round->
                val thread=Thread.currentThread().id
                val allocation=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                val result=organizationFolders(gallery,emptyList(),p,org)
                val wall=(System.nanoTime()-start)/1e6;val allocated=bean.getThreadAllocatedBytes(thread)-allocation;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                assertEquals(size/10,result.size);assertEquals(size,result.sumOf{it.ids.size})
                assertEquals(p.batchOrigins.keys,result.flatMap{it.ids}.toSet())
                records+=buildJsonObject{put("images",size);put("batches",org.batches.size);put("round",round);put("wallMs",wall);put("threadCpuMs",cpuMs);put("allocatedBytes",allocated)}
            }
        }
        val report=buildJsonObject{put("scope","production organizationFolders batch-list metadata only; excludes disk, UI, native RSS and human acceptance");put("providerCalls",0);put("java",System.getProperty("java.version"));put("heapLimitBytes",Runtime.getRuntime().maxMemory());put("measurements",JsonArray(records))}
        val path=Path.of("build/performance-20261025/batch-listing.json");Files.createDirectories(path.parent);Files.writeString(path,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),report))
    }
}
