package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

/** Pure adapter of existing folder-filter predicates; no UI, queue, images or provider calls. */
private fun recordedFolderBatchFilter(org:GenerationOrganization,id:String?,archivedOnly:Boolean):List<GenerationBatch> =
    org.batches.filter{(!archivedOnly||it.archivedAt!=null)&&it.parent in org.descendants(id!!)}

class FolderBatchProjectionPerformanceTest {
    private fun folder(index:Int)=UUID(6L,index.toLong()+1).toString()
    private fun batch(index:Int)=UUID(7L,index.toLong()+1).toString()
    @Test fun measuredRecordedFolderPredicatesKeepOrderArchivePolicyAndAdmittedState() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for((folderCount,batchCount) in listOf(32 to 64,128 to 256,256 to 512)) {
            val folders=List(folderCount){i->GenerationFolder(id=folder(i),name="Synthetic node $i",parent=when{i==0->null;i<=31->folder(i-1);else->folder(i%31)})}.shuffled(Random(113+folderCount))
            val order=(0 until batchCount).shuffled(Random(137+folderCount+batchCount))
            val batches=order.map{i->GenerationBatch(id=batch(i),kind="auto",alias="Synthetic batch $i",createdAt=1_700_000_000_000L+i,archivedAt=if(i%2==0)1_700_000_100_000L+i else null,parent=if(i%5==0)null else folder(i%folderCount),groups=listOf(GenerationBatchGroup(UUID(9L,i.toLong()+1).toString(),"Synthetic group",listOf(UUID(8L,i.toLong()+1).toString()))))}
            val org=GenerationOrganization(folders=folders,batches=batches);val state=StudioState(version=3,generationOrganization=org);state.validate()
            val before=documentJson.encodeToString(state).toByteArray();assertTrue(before.size<=4*1024*1024)
          for((context,target) in listOf("root" to folder(0),"leaf" to folder(folderCount-1))) {
           for(archivedOnly in listOf(false,true)) {
            val expected=order.filter{i->i%5!=0&&(!archivedOnly||i%2==0)&&(context=="root"||i%folderCount==folderCount-1)}.map(::batch)
            fun verify(actual:List<GenerationBatch>) {assertEquals(expected,actual.map{it.id});assertContentEquals(before,documentJson.encodeToString(state).toByteArray())}
            repeat(2){verify(recordedFolderBatchFilter(org,target,archivedOnly))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=recordedFolderBatchFilter(org,target,archivedOnly)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("folders",folderCount);put("batches",batchCount);put("context",context);put("archivedOnly",archivedOnly);put("selected",actual.size);put("serializedStateBytes",before.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
           }
          }
        }
        println("FOLDER_BATCH_PREDICATES="+JsonArray(rows))
    }
}
