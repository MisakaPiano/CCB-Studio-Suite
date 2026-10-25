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
        val folders=List(count){i->GenerationFolder(id=folder(i),name="Synthetic node $i",parent=when{ i==0->null;count>32&&i==count-2->folder(0);count>32&&i==count-1->folder(count-2);i<=31->folder(i-1);else->folder(i%31)})}
        return GenerationOrganization(folders=folders.shuffled(Random(91+count)))
    }
    @Test fun measuredActualFolderTraversalPreservesValidatedMetadataAndRootMembership() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(32,256,1000)) {
            val org=fixture(count);val state=StudioState(version=3,generationOrganization=org);state.validate()
            val before=documentJson.encodeToString(state).toByteArray();assertTrue(before.size<=4*1024*1024)
          for((context,target) in listOf("root" to folder(0),"small" to folder(count-2),"leaf" to folder(count-1),"unknown" to folder(count+100))) {
            val expectedMembers=when(context){"root"->List(count,::folder).toSet();"small"->setOf(folder(count-2),folder(count-1));else->setOf(target)}
            val expectedOrder=org.descendants(target).toList()
            fun verify(actual:Set<String>) {assertEquals(expectedMembers,actual);assertEquals(target,actual.first());assertEquals(expectedOrder,actual.toList());assertContentEquals(before,documentJson.encodeToString(state).toByteArray())}
            repeat(2){verify(org.descendants(target))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=org.descendants(target)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("context",context);put("folders",count);put("serializedStateBytes",before.size);put("members",actual.size);put("orderSha256",sha256(actual.joinToString("|").toByteArray()));put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
          }
        }
        println("FOLDER_DESCENDANTS_ALLOCATION="+JsonArray(rows))
    }
}
