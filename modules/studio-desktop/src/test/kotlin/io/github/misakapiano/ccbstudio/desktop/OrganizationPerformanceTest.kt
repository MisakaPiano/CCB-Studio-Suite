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
    @Test fun indexedScopesKeepLegacyMembershipNamesOrderAndGroupPrecedence() {
        val (initial,history)=fixture(100)
        val folder=GenerationFolder(id(900001),"Project")
        val chapter=GenerationFolder(id(900002),"Chapter","chapter",folder.id)
        val removed=initial.batches[1].id
        val empty=GenerationBatch(id(900003),"manual","empty",1)
        val org=initial.copy(folders=listOf(folder,chapter),batches=initial.batches.filter{it.id!=removed}.mapIndexed{i,b->
            b.copy(alias="renamed-$i",parent=if(i%2==0)chapter.id else folder.id,groups=if(i==0)b.groups.map{g->g.copy(images=listOf(id(999999))+g.images.take(2))}else b.groups)
        }+empty)
        val blank=history.batchOrigins.mapValues{(_,o)->if(o.batchId==removed)o.copy(batchName="")else o}
        val p=history.copy(batchOrigins=blank);val index=GalleryBatchIndex(org,p)
        val album=VirtualAlbum(id=id(900004),name="virtual",images=listOf(id(200000)))
        val nested=VirtualAlbum(id=id(900005),name="nested",parent=album.id,images=listOf(id(200001)))
        val scopes=listOf("all" to "","batches" to "","type" to "manual","type" to "auto","organizations" to "","folder" to folder.id,"folder" to chapter.id,
            "batch" to org.batches.first().id,"batch" to removed,"batch" to empty.id,"batch" to id(989898),"albums" to "","album" to album.id)
        scopes.forEach{(kind,id)->
            val g=GalleryViewState().apply{enterBatch(emptySet(),"scope","org:$kind:$id")}
            val old=legacyOrganizationFolders(g,listOf(album,nested),p,org)
            val new=organizationFolders(g,listOf(album,nested),p,org,index)
            assertEquals(old,new,"scope $kind:$id")
            assertEquals(old.map{it.ids.toList()},new.map{it.ids.toList()},"member order $kind:$id")
        }
        index.ids.forEach{assertEquals(generationBatchImages(org,it,p).toList(),index.images(it).toList())}
        assertEquals("auto",index.kind(removed));assertEquals("批次 ${removed.take(8)}",index.title(removed));assertTrue(index.images(empty.id).isEmpty())
    }
    @Test fun changedSnapshotsReflectRenameRemovalAndNewReceiptWithoutMutatingOldProjection() {
        val (org,p)=fixture(100);val old=GalleryBatchIndex(org,p);val batch=org.batches.first();val oldMembers=old.images(batch.id).toList()
        val newId=id(999999);val next=p.copy(batchOrigins=p.batchOrigins+(newId to p.batchOrigins.values.first()))
        val renamed=org.copy(batches=org.batches.map{if(it.id==batch.id)it.copy(alias="changed",groups=emptyList())else it})
        val fresh=GalleryBatchIndex(renamed,next)
        assertTrue(fresh.title(batch.id).startsWith("changed · "));assertEquals(oldMembers+newId,fresh.images(batch.id).toList())
        assertEquals(oldMembers,old.images(batch.id).toList());assertFalse(old.title(batch.id).startsWith("changed"))
        val deleted=GalleryBatchIndex(renamed.copy(batches=renamed.batches.filter{it.id!=batch.id}),next.copy(batchOrigins=next.batchOrigins.filterValues{it.batchId!=batch.id}))
        assertFalse(batch.id in deleted.ids);assertTrue(deleted.images(batch.id).isEmpty())
    }
    @Test fun repeatedQueriesTraverseReceiptSourceOnlyOncePerSnapshot() {
        val (org,p)=fixture(1000);var visits=0
        val counted=object:Map<String,BatchResultOrigin> by p.batchOrigins {
            override val entries:Set<Map.Entry<String,BatchResultOrigin>> get()=object:AbstractSet<Map.Entry<String,BatchResultOrigin>>() {
                override val size get()=p.batchOrigins.size
                override fun iterator():Iterator<Map.Entry<String,BatchResultOrigin>> {
                    val iterator=p.batchOrigins.entries.iterator()
                    return object:Iterator<Map.Entry<String,BatchResultOrigin>> {
                        override fun hasNext()=iterator.hasNext()
                        override fun next()=iterator.next().also{visits++}
                    }
                }
            }
        }
        val indexed=GalleryBatchIndex(org,p.copy(batchOrigins=counted));assertEquals(1000,visits)
        repeat(20){indexed.ids.forEach{id->assertEquals(10,indexed.images(id).size);assertEquals(10,indexed.origins(id).size)}}
        assertEquals(1000,visits,"Repaints/queries must not rescan admitted receipt source")
    }
    @Test fun pairedLegacyAndIndexedBatchProjectionMeasurements() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean;val records=mutableListOf<JsonObject>()
        val gallery=GalleryViewState().apply{enterBatch(emptySet(),"批次","org:batches:")}
        fun measure(phase:String,size:Int,round:Int,body:()->List<OrganizationFolder>):List<OrganizationFolder> {
            val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
            val result=body();val wall=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val bytes=bean.getThreadAllocatedBytes(thread)-allocated
            records+=buildJsonObject{put("phase",phase);put("images",size);put("batches",size/10);put("round",round);put("wallMs",wall);put("threadCpuMs",cpuMs);put("allocatedBytes",bytes)}
            return result
        }
        listOf(100,1000,10000).forEach{size->
            val (org,p)=fixture(size);val reused=GalleryBatchIndex(org,p)
            repeat(2){legacyOrganizationFolders(gallery,emptyList(),p,org);organizationFolders(gallery,emptyList(),p,org)}
            repeat(4){round->
                lateinit var old:List<OrganizationFolder>;lateinit var fresh:List<OrganizationFolder>
                fun legacy(){old=measure("legacy",size,round){legacyOrganizationFolders(gallery,emptyList(),p,org)}}
                fun indexed(){fresh=measure("indexed-including-build",size,round){organizationFolders(gallery,emptyList(),p,org)}}
                if(round%2==0){legacy();indexed()}else{indexed();legacy()}
                assertEquals(old,fresh);assertEquals(old.map{it.ids.toList()},fresh.map{it.ids.toList()})
                assertEquals(old,measure("indexed-reused-snapshot",size,round){organizationFolders(gallery,emptyList(),p,org,reused)})
            }
        }
        val report=buildJsonObject{put("scope","in-memory production batch-list calculations; fresh index construction included; reuse separate; no disk/UI/native RSS claim");put("legacySource","69f53d314abe0aa58730ac5ed4aadffd1ff7b940");put("syntheticOnly",true);put("providerCalls",0);put("java",System.getProperty("java.version"));put("measurements",JsonArray(records))}
        val path=Path.of("build/performance-20261025/paired-comparison.json");Files.createDirectories(path.parent);Files.writeString(path,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),report))
    }
}
