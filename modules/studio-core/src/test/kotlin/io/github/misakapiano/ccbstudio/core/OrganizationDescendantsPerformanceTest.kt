package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.random.Random
import kotlin.test.*

private fun GenerationOrganization.legacyDescendants(id:String):Set<String> = setOf(id)+folders.filter{it.parent==id}.flatMap{legacyDescendants(it.id)}

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
            val expectedOrder=org.legacyDescendants(target).toList()
            fun verify(actual:Set<String>) {assertEquals(expectedMembers,actual);assertEquals(target,actual.first());assertEquals(expectedOrder,actual.toList());assertContentEquals(before,documentJson.encodeToString(state).toByteArray())}
            repeat(2){verify(org.legacyDescendants(target));verify(org.descendants(target))}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("legacy","current")else listOf("current","legacy")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=if(kind=="legacy")org.legacyDescendants(target)else org.descendants(target)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("kind",kind);put("context",context);put("folders",count);put("serializedStateBytes",before.size);put("members",actual.size);put("orderSha256",sha256(actual.joinToString("|").toByteArray()));put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
          }
        }
        println("FOLDER_DESCENDANTS_ALLOCATION="+JsonArray(rows))
    }
    @Test fun depthFirstOrderUnknownRootsAndReparentedSnapshotsMatchLegacy() {
        val org=GenerationOrganization(folders=listOf(
            GenerationFolder(id=folder(4),name="B child",parent=folder(2)),
            GenerationFolder(id=folder(2),name="B",parent=folder(0)),
            GenerationFolder(id=folder(5),name="unrelated"),
            GenerationFolder(id=folder(3),name="A child",parent=folder(1)),
            GenerationFolder(id=folder(0),name="root"),
            GenerationFolder(id=folder(1),name="A",parent=folder(0))))
        org.validate();val before=documentJson.encodeToString(org)
        assertEquals(listOf(0,2,4,1,3).map(::folder),org.descendants(folder(0)).toList())
        assertEquals(listOf(2,4).map(::folder),org.descendants(folder(2)).toList())
        for(id in listOf(folder(0),folder(1),folder(2),folder(5),folder(999),"unknown"))assertEquals(org.legacyDescendants(id).toList(),org.descendants(id).toList())
        assertEquals(setOf("unknown"),GenerationOrganization().descendants("unknown"))
        val moved=org.copy(folders=org.folders.map{if(it.id==folder(4))it.copy(parent=folder(1))else it});moved.validate()
        assertEquals(listOf(0,2,1,4,3).map(::folder),moved.descendants(folder(0)).toList())
        assertEquals(listOf(0,2,4,1,3).map(::folder),org.descendants(folder(0)).toList())
        assertEquals(before,documentJson.encodeToString(org))
    }
    @Test fun originalOrganizationAdmissionStillRejectsInvalidGraphAndSizeAndDepth() {
        val root=GenerationFolder(id=folder(0),name="root")
        val invalid=listOf(
            GenerationOrganization(folders=listOf(root,root.copy(name="duplicate"))),
            GenerationOrganization(folders=listOf(root.copy(id="not-a-folder-id"))),
            GenerationOrganization(folders=listOf(root.copy(name=""))),
            GenerationOrganization(folders=listOf(root.copy(kind="unsupported"))),
            GenerationOrganization(folders=listOf(root.copy(parent=folder(99)))),
            GenerationOrganization(folders=listOf(root.copy(parent=folder(1)),GenerationFolder(id=folder(1),name="cycle",parent=folder(0)))),
            GenerationOrganization(folders=List(33){i->GenerationFolder(id=folder(i),name="deep$i",parent=if(i==0)null else folder(i-1))}),
            GenerationOrganization(folders=List(1001){i->GenerationFolder(id=folder(i),name="many$i")}))
        invalid.forEach{assertFails{it.validate()}}
        val maximum=GenerationOrganization(folders=List(32){i->GenerationFolder(id=folder(i),name="allowed$i",parent=if(i==0)null else folder(i-1))})
        maximum.validate();assertEquals(List(32,::folder),maximum.descendants(folder(0)).toList())
    }
}
