package io.github.misakapiano.ccbstudio.core

import java.nio.file.*
import kotlin.test.*

class LibraryIngressBatchTest {
    @Test fun batchUsesBoundedRevisionsAndCancelledCopiesRemainRecoverable() {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val bytes=EditorRenderer.synthetic(40,50)
        var ids=emptyList<String>()
        StudioWorkspace.openStudio(root).use{w->
            ids=(1..24).map{w.importImage("image-$it.png",bytes)};val repository=LibraryRepository(w);var completed=0
            assertFailsWith<IllegalStateException>{repository.retainIngressBatch(ids,{if(completed==1)error("Synthetic cancellation")},{done,_->completed=done})}
            val observed=repository.load().observations.values
            assertEquals(1,observed.count{it.status=="PRESENT"});assertEquals(23,observed.count{it.status=="COPY_PENDING"})
            assertEquals(3,w.catalog().count{it.request().project().decodeToString()==LibraryRepository.TAG})
            val outputs=repository.retainIngressBatch(ids);assertEquals(24,outputs.size)
            assertTrue(repository.load().observations.values.all{it.status=="PRESENT"})
            outputs.forEach{assertContentEquals(bytes,Files.readAllBytes(it))};ids.forEach{assertContentEquals(bytes,w.bytes(it))}
        }
        StudioWorkspace.openStudio(root).use{w->assertEquals(24,LibraryRepository(w).load().observations.size);ids.forEach{assertContentEquals(bytes,w.bytes(it))}}
    }
}
