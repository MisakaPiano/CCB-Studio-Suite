package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

/** Organization node metadata only; complete StudioState validation is outside the timed traversal. */
class OrganizationDescendantsPerformanceTest {
    private fun folder(index:Int)=UUID(6L,index.toLong()+1).toString()
    private fun fixture(count:Int):GenerationOrganization {
        val folders=List(count){i->GenerationFolder(id=folder(i),name="Synthetic node $i",parent=when{ i==0->null;i<=31->folder(i-1);else->folder(i%31)})}
        return GenerationOrganization(folders=folders.shuffled(Random(91+count)))
    }
    @Test fun measuredActualFolderTraversalPreservesValidatedMetadataAndRootMembership() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(32,256,1000)) {
            val org=fixture(count);val state=StudioState(version=3,generationOrganization=org);state.validate()
            val before=documentJson.encodeToString(state).toByteArray();assertTrue(before.size<=4*1024*1024)
            val expectedMembers=List(count,::folder).toSet();val expectedOrder=org.descendants(folder(0)).toList()
            fun verify(actual:Set<String>) {assertEquals(expectedMembers,actual);assertEquals(folder(0),actual.first());assertEquals(expectedOrder,actual.toList());assertContentEquals(before,documentJson.encodeToString(state).toByteArray())}
            repeat(2){verify(org.descendants(folder(0)))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=org.descendants(folder(0))
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("folders",count);put("serializedStateBytes",before.size);put("members",actual.size);put("orderSha256",sha256(actual.joinToString("|").toByteArray()));put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("FOLDER_DESCENDANTS_ALLOCATION="+JsonArray(rows))
    }
}
