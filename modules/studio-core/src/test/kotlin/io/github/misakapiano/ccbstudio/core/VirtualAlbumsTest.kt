package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import java.nio.file.Path
import kotlin.test.*

class VirtualAlbumsTest {
    @Test fun nestedMembershipMoveRemoveReopenKeepsOriginalBytes() {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val bytes=EditorRenderer.synthetic(20,20)
        var image="";var album=""
        StudioWorkspace.openStudio(root).use{w->
            image=w.importImage("synthetic.png",bytes);val repo=LibraryRepository(w)
            val a=VirtualAlbum(name="精选",images=listOf(image));val b=VirtualAlbum(name="子相册",parent=a.id);val other=VirtualAlbum(name="其他")
            album=a.id;repo.update{it.copy(virtualAlbums=listOf(a,b,other))}
            val associated=VirtualAlbums.associate(repo.load().virtualAlbums,setOf(image),setOf(b.id,other.id),a.id)
            repo.update{it.copy(virtualAlbums=associated)}
            assertEquals(setOf(image),VirtualAlbums.members(associated,a.id));assertTrue(associated.single{it.id==a.id}.images.isEmpty())
            assertFails{VirtualAlbums.save(associated,a.copy(parent=b.id))}
            assertFails{VirtualAlbums.save(associated,b.copy(parent=newId()))}
            assertContentEquals(bytes,w.bytes(image))
        }
        StudioWorkspace.openStudio(root).use{w->val repo=LibraryRepository(w);val old=repo.load().virtualAlbums
            assertEquals(setOf(image),VirtualAlbums.members(old,album));repo.update{it.copy(virtualAlbums=VirtualAlbums.remove(old,album))}
            assertEquals(1,repo.load().virtualAlbums.size);assertContentEquals(bytes,w.bytes(image))
        }
    }
    @Test fun sourceProjectionUsesReceiptAfterQueueDisappears() {
        StudioWorkspace.openStudio(Path.of(System.getProperty("preview.test.root"),newId())).use{w->
            val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="nai-diffusion-4-5-full")
            val origin=BatchResultOrigin(newId(),newId(),"来源分组",newId(),"横",1,123)
            val receipt=GenerationReceipt(recipe=com.example.chatbar.domain.image.NovelAiStudioDraft(basePrompt="synthetic").toRecipe(),account=account,request=kotlinx.serialization.json.buildJsonObject{},batchOrigin=origin)
            val raw=documentJson.encodeToString(GenerationReceipt.serializer(),receipt);val id=w.importImage("synthetic.png",EditorRenderer.synthetic(20,20),raw)
            w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe=raw,results=listOf(id),state="COMPLETED")),batch=BatchState()))
            assertEquals(origin,readGalleryHistory(w).batchOrigins[id])
        }
    }
}
