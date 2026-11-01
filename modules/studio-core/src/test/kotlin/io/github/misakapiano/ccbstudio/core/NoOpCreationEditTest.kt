package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import io.github.misakapiano.ccbstudio.assets.StoreException
import java.io.ByteArrayInputStream
import java.nio.file.*
import java.util.UUID
import kotlin.test.*

/** A no-change EDIT avoids only the duplicate durable append, after existing lineage validation. */
class NoOpCreationEditTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun documents(w:StudioWorkspace)=Files.list(w.root.resolve("documents")).use{fs->
        fs.toList().associate{it.fileName.toString() to sha256(Files.readAllBytes(it))}
    }
    private fun put(w:StudioWorkspace,tag:String,bytes:ByteArray,recipe:String=""):AssetStore.Asset {
        val id=UUID.randomUUID()
        w.assets.put(AssetStore.Request(id,"synthetic.bin",AssetStore.Kind.ORIGINAL,null,recipe.toByteArray(),tag.toByteArray()),ByteArrayInputStream(bytes))
        return w.assets.read(id).require()
    }
    @Test fun identicalEditKeepsDurableRevisionWhileActualEditPersistsAndReopens() {
        val r=root()
        StudioWorkspace.openStudio(r).use{w->
            val session=CreationSession(w);session.update(DesignLineageOperation.EDIT){it.copy(composer="synthetic initial")}
            val a=put(w,"OTHER",byteArrayOf(1,2,3))
            val docs=documents(w);val state=w.state;val body=w.committedStateBody;val revision=w.creationRevision
            session.update(DesignLineageOperation.EDIT){it.copy()}
            assertEquals(docs,documents(w));assertEquals(state,w.state)
            assertEquals(body,w.committedStateBody);assertEquals(revision,w.creationRevision)
            assertContentEquals(byteArrayOf(1,2,3),w.bytes(a.request().id().toString()))
            session.update(DesignLineageOperation.EDIT){it.copy(composer="synthetic actual edit")}
            assertEquals(docs.size+1,documents(w).size);assertNotEquals(revision,w.creationRevision)
            assertEquals("synthetic actual edit",w.state.creation!!.composer)
        }
        StudioWorkspace.openStudio(r).use{w->assertEquals("synthetic actual edit",w.state.creation!!.composer)}
    }
    @Test fun identicalEditStillRejectsFutureAndConflictingLineageWithoutWriting() {
        for(kind in listOf("future","conflict"))StudioWorkspace.openStudio(root()).use{w->
            val session=CreationSession(w);session.update(DesignLineageOperation.EDIT){it.copy(composer="synthetic")}
            val identity=w.creationRevision!!
            // Tag/version and duplicate identity rejection must precede body decoding.
            val raw="{}".toByteArray()
            val assets=if(kind=="future")listOf(put(w,"CCB_CREATION_LINEAGE_V99",raw,identity))
                else listOf(put(w,DesignProvenance.LINEAGE,raw,identity),put(w,DesignProvenance.LINEAGE,raw,identity))
            val docs=documents(w);val body=w.committedStateBody
            if(kind=="future")assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{session.update(DesignLineageOperation.EDIT){it.copy()}}.status)
            else assertFailsWith<IllegalArgumentException>{session.update(DesignLineageOperation.EDIT){it.copy()}}
            assertEquals(docs,documents(w));assertEquals(body,w.committedStateBody)
            assets.forEach{assertContentEquals(raw,w.bytes(it.request().id().toString()))}
        }
    }
    @Test fun identicalEditDoesNotSkipUnrelatedPayloadIntegrityFailure() {
        StudioWorkspace.openStudio(root()).use{w->
            val session=CreationSession(w);session.update(DesignLineageOperation.EDIT){it.copy(composer="synthetic")}
            val a=put(w,"OTHER",byteArrayOf(1,2,3));val blob=w.root.resolve("assets/objects/"+a.sha256()+".blob")
            Files.write(blob,byteArrayOf(9));val docs=documents(w);val body=w.committedStateBody
            assertEquals("CORRUPT",assertFailsWith<StoreException>{session.update(DesignLineageOperation.EDIT){it.copy()}}.code())
            assertEquals(docs,documents(w));assertEquals(body,w.committedStateBody)
            assertContentEquals(byteArrayOf(9),Files.readAllBytes(blob))
        }
    }
}
