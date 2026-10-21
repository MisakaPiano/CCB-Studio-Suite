package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.nio.file.*
import java.util.UUID
import kotlin.test.*

class AssetArchiveTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    @Test fun imagePromptAndBookRestoreWithReferencesLabelsAndOriginalBytes() {
        val data=root();val target=root();val pixels=EditorRenderer.synthetic(48,48)
        var image="";var prompt="";var doc="";var project="";var backup:Path?=null
        StudioWorkspace.openStudio(data).use{w->
            image=w.importImage("original.png",pixels);val repo=LibraryRepository(w);val archive=AssetArchive(w)
            prompt=repo.savePrompt("nonempty independent words","Prompt alias","manual");PromptReferences(repo).add(prompt)
            val projects=TypesetRepository(w);val p=projects.create("Existing project",listOf(image));project=p.id;val d=projects.load(p)
            TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(d.pages.map{it.id},zip=true))
            doc=SharedAssets.documents(w).single().request().id().toString()
            repo.label(image,"Image alias",listOf("tag"));repo.label(doc,"Book alias",listOf("book"))
            val all=setOf(image,prompt,doc);val prior=w.catalog().associate{it.request().id().toString() to it.sha256()}
            archive.change(all,true);assertEquals(all,archive.ids())
            val count=w.catalog().size;archive.change(all,true);assertEquals(count,w.catalog().size)
            assertEquals(listOf(prompt),PromptReferences(repo).load().ids);assertEquals("nonempty independent words",repo.prompt(prompt).text)
            assertEquals(image,projects.load(w.state.typesetProjects.single{it.id==project}).pages.single().assetId)
            prior.forEach{(id,hash)->assertEquals(hash,sha256(w.bytes(id)))}
            backup=w.backup()
        }
        StudioRestore.restore(backup!!,target)
        StudioWorkspace.openStudio(target).use{w->val archive=AssetArchive(w);val repo=LibraryRepository(w)
            assertEquals(setOf(image,prompt,doc),archive.ids());assertContentEquals(pixels,w.bytes(image))
            archive.change(setOf(image,prompt,doc),false);assertTrue(archive.ids().isEmpty())
            assertEquals("Image alias",repo.load().labels[image]!!.alias);assertEquals(listOf("book"),repo.load().labels[doc]!!.tags)
            assertEquals(listOf(prompt),PromptReferences(repo).load().ids)
        }
        StudioWorkspace.openStudio(target).use{assertTrue(AssetArchive(it).ids().isEmpty());assertContentEquals(pixels,it.bytes(image))}
    }
    @Test fun interruptedSnapshotIsNotPublishedAndRetryRestores() {
        val data=root();var image=""
        StudioWorkspace.openStudio(data).use{w->image=w.importImage("kept.png",EditorRenderer.synthetic(32,32));val archive=AssetArchive(w)
            assertFails{archive.change(setOf(image),true){error("snapshot committed, library pointer not committed")}}
            assertTrue(archive.ids().isEmpty());assertEquals(1,w.catalog().count{it.request().project().decodeToString()==AssetArchive.TAG})
        }
        StudioWorkspace.openStudio(data).use{w->val archive=AssetArchive(w);assertTrue(archive.ids().isEmpty());archive.change(setOf(image),true)
            assertFails{archive.change(setOf(image),false){error("restore snapshot interrupted")}};assertEquals(setOf(image),archive.ids())
            archive.change(setOf(image),false);assertTrue(archive.ids().isEmpty())
            val internal=w.requestPayload(byteArrayOf(1,2,3),null);assertFails{archive.change(setOf(internal),true)}
        }
    }
    @Test fun futureAndCorruptArchiveBlocksChangesButPreservesOriginals() {
        for(future in listOf(false,true))StudioWorkspace.openStudio(root()).use{w->
            val image=w.importImage("kept.png",EditorRenderer.synthetic(32,32));val hash=sha256(w.bytes(image));val repo=LibraryRepository(w);val archive=AssetArchive(w)
            archive.change(setOf(image),true)
            val current=repo.load().preferences.getValue(AssetArchive.KEY)
            val raw=if(future)"{\"version\":2,\"futureField\":true}"else current.replace(Regex("\"sha256\":\"[0-9a-f]+\""),"\"sha256\":\"${"0".repeat(64)}\"")
            repo.update{it.copy(preferences=it.preferences+(AssetArchive.KEY to raw))}
            val count=w.catalog().size
            val failure=assertFails{archive.ids()};if(future)assertEquals("UNSUPPORTED_VERSION",(failure as DataProblem).status)
            assertFails{archive.change(setOf(image),false)};assertEquals(count,w.catalog().size);assertEquals(hash,sha256(w.bytes(image)))
        }
    }
    @Test fun futureSnapshotIsRecognizedBeforeDecodingFields() {
        StudioWorkspace.openStudio(root()).use{w->val image=w.importImage("kept.png",EditorRenderer.synthetic(32,32));val id=UUID.randomUUID()
            val bytes="{\"version\":2,\"newShape\":{}}".toByteArray()
            w.assets.put(AssetStore.Request(id,"future.json",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),AssetArchive.TAG.toByteArray()),ByteArrayInputStream(bytes))
            val reference=buildJsonObject{put("version",1);put("id",id.toString());put("sha256",sha256(bytes))}.toString()
            LibraryRepository(w).update{it.copy(preferences=it.preferences+(AssetArchive.KEY to reference))}
            assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{AssetArchive(w).ids()}.status)
            assertTrue(w.bytes(image).isNotEmpty())
        }
    }
    @Test fun unchangedCollectionDoesNotResurrectArchivedAsset() {
        val folder=Files.createDirectories(root());val path=folder.resolve("collected.png");val original=EditorRenderer.synthetic(32,32);Files.write(path,original)
        StudioWorkspace.openStudio(root()).use{w->val repo=LibraryRepository(w);val archive=AssetArchive(w)
            val entry=repo.addFolder(folder,"images",false).folders.single();val first=repo.scan(entry.id).single().assetId!!
            archive.change(setOf(first),true);assertContentEquals(original,Files.readAllBytes(path))
            assertEquals("UNCHANGED",repo.scan(entry.id).single().status);assertEquals(setOf(first),archive.ids())
            Files.write(path,EditorRenderer.synthetic(40,32));val next=repo.scan(entry.id).single()
            assertEquals("NEW_VERSION",next.status);assertFalse(next.assetId in archive.ids());assertContentEquals(original,w.bytes(first))
        }
    }
}
