package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

private fun legacyMembers(albums:List<VirtualAlbum>,id:String):Set<String> = VirtualAlbums.descendants(albums,id).flatMap{key->albums.single{it.id==key}.images}.toSet()

/** Pure metadata only, with serialized LibraryState below the existing4MiB storage limit. */
class VirtualMembersPerformanceTest {
    private fun image(index:Int)=UUID(2L,index.toLong()+1).toString()
    private fun album(index:Int)=UUID(1L,index.toLong()+1).toString()
    private fun fixture(count:Int,images:Int):List<VirtualAlbum> {
        val members=List(images,::image)
        return List(count){i->VirtualAlbum(id=album(i),name="Synthetic album $i",parent=if(i==0)null else album(0),images=members)}
    }
    @Test fun measuredActualVirtualMembershipKeepsOrderAndPersistableFixtureSize() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean;check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for((count,images) in listOf(32 to 128,128 to 512,500 to 128)) {
            val albums=fixture(count,images);val state=LibraryState(virtualAlbums=albums);val bytes=documentJson.encodeToString(state).toByteArray();assertTrue(bytes.size<=4*1024*1024)
            val expected=List(images,::image)
            repeat(2){assertEquals(expected,legacyMembers(albums,album(0)).toList());assertEquals(expected,VirtualAlbums.members(albums,album(0)).toList())}
            repeat(4){round->
              for(kind in if(round%2==0)listOf("legacy","current") else listOf("current","legacy")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                val actual=if(kind=="legacy")legacyMembers(albums,album(0)) else VirtualAlbums.members(albums,album(0))
                val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                assertEquals(expected,actual.toList());assertContentEquals(bytes,documentJson.encodeToString(state).toByteArray())
                rows+=buildJsonObject{put("kind",kind);put("albums",count);put("referencesPerAlbum",images);put("totalReferences",count*images);put("uniqueMembers",actual.size);put("serializedBytes",bytes.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("VIRTUAL_MEMBERS_ALLOCATION="+JsonArray(rows))
    }
    @Test fun membershipKeepsDescendantLayerOrderFirstOccurrenceAndDoesNotMutateMetadata() {
        val albums=listOf(
            VirtualAlbum(id=album(4),name="grandchild second branch",parent=album(2),images=listOf(image(7),image(1))),
            VirtualAlbum(id=album(2),name="second branch",parent=album(0),images=listOf(image(2),image(3))),
            VirtualAlbum(id=album(5),name="unrelated",images=listOf(image(99))),
            VirtualAlbum(id=album(3),name="grandchild first branch",parent=album(1),images=listOf(image(4),image(2))),
            VirtualAlbum(id=album(0),name="root",images=listOf(image(1))),
            VirtualAlbum(id=album(1),name="first branch",parent=album(0),images=listOf(image(5),image(2)))
        )
        val before=documentJson.encodeToString(LibraryState(virtualAlbums=albums))
        assertEquals(listOf(0,2,1,4,3).map(::album),VirtualAlbums.descendants(albums,album(0)).toList())
        assertEquals(listOf(1,2,3,5,7,4).map(::image),VirtualAlbums.members(albums,album(0)).toList())
        assertEquals(legacyMembers(albums,album(0)).toList(),VirtualAlbums.members(albums,album(0)).toList())
        assertEquals(before,documentJson.encodeToString(LibraryState(virtualAlbums=albums)))
        assertFailsWith<NoSuchElementException>{VirtualAlbums.members(albums,album(999))}
        assertFailsWith<NoSuchElementException>{VirtualAlbums.members(albums,"unknown")}
        assertFailsWith<NoSuchElementException>{VirtualAlbums.members(emptyList(),album(0))}
        assertTrue(VirtualAlbums.members(listOf(VirtualAlbum(id=album(0),name="empty")),album(0)).isEmpty())
    }
    @Test fun invalidAndUnrelatedMetadataStillRejectAndDepthBoundaryIsUnchanged() {
        val root=VirtualAlbum(id=album(0),name="root",images=listOf(image(1)))
        val invalid=listOf(
            listOf(root,root.copy(name="duplicate ID")),
            listOf(root.copy(images=listOf(image(1),image(1)))),
            listOf(root.copy(images=listOf("not-an-image-id"))),
            listOf(root.copy(parent=album(99))),
            listOf(root.copy(parent=album(1)),VirtualAlbum(id=album(1),name="cycle",parent=album(0))),
            listOf(root,VirtualAlbum(id=album(1),name="",images=emptyList())),
            List(33){i->VirtualAlbum(id=album(i),name="too deep $i",parent=if(i==0)null else album(i-1))}
        )
        invalid.forEach{assertFailsWith<IllegalArgumentException>{VirtualAlbums.members(it,root.id)}}
        val maximum=List(32){i->VirtualAlbum(id=album(i),name="allowed $i",parent=if(i==0)null else album(i-1),images=listOf(image(i)))}
        assertEquals(List(32,::image),VirtualAlbums.members(maximum,album(0)).toList())
    }
}
