package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

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
            val expected=List(images,::image);assertEquals(expected,VirtualAlbums.members(albums,album(0)).toList())
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                val actual=VirtualAlbums.members(albums,album(0))
                val elapsed=(System.nanoTime()-start)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6;val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                assertEquals(expected,actual.toList());assertContentEquals(bytes,documentJson.encodeToString(state).toByteArray())
                rows+=buildJsonObject{put("albums",count);put("referencesPerAlbum",images);put("totalReferences",count*images);put("uniqueMembers",actual.size);put("serializedBytes",bytes.size);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("VIRTUAL_MEMBERS_ALLOCATION="+JsonArray(rows))
    }
}
