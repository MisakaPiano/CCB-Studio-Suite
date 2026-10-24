package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*
import kotlin.test.*

class GenerationOrganizationTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private val account=AccountProfile(alias="Synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
    @Test fun manualSessionSurvivesReopenArchiveClearsOnlyCurrentAndKeepsReceipt(){
        val root=root();lateinit var id:String;lateinit var batch:String;lateinit var raw:String
        val bytes=EditorRenderer.synthetic(32,32)
        StudioWorkspace.openStudio(root).use{w->val repo=GenerationOrganizations(w);batch=repo.ensureManual().id
            raw=documentJson.encodeToString(GenerationReceipt(recipe=NovelAiStudioDraft(basePrompt="synthetic").toRecipe(),account=account,request=buildJsonObject{},manualBatchId=batch))
            id=w.importImage("manual.png",bytes,raw);val unrelated=w.importImage("import.png",bytes)
            w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe=raw,state="COMPLETED",results=listOf(id),manualBatchId=batch))))
            assertEquals(listOf(id),repo.manualImages());assertFalse(unrelated in repo.manualImages())
        }
        StudioWorkspace.openStudio(root).use{w->val repo=GenerationOrganizations(w);assertEquals(batch,repo.ensureManual().id);assertEquals(listOf(id),repo.manualImages())
            val project=GenerationFolder(name="插画集");repo.addFolder(project);val chapter=GenerationFolder(name="第一章",kind="chapter",parent=project.id);repo.addFolder(chapter)
            val created=repo.state.batches.single().createdAt;assertFailsWith<IllegalArgumentException>{repo.archiveManual(batch,"",chapter.id)};assertEquals(listOf(id),repo.manualImages())
            repo.archiveManual(batch,"角色光影",chapter.id);assertEquals(emptyList(),repo.manualImages());assertNull(repo.state.currentManual)
            assertEquals(created,repo.state.batches.single().createdAt);assertEquals("插画集 / 第一章",repo.state.path(chapter.id));assertTrue(repo.state.batches.single().title.startsWith("角色光影 · "))
            assertContentEquals(bytes,w.bytes(id));assertEquals(raw,w.state.jobs.single().recipe);assertEquals(batch,readGalleryHistory(w).batchOrigins.getValue(id).batchId)
            val prior=repo.state;repo.change(prior,prior.copy(batches=prior.batches.map{it.copy(parent=project.id)}));assertContentEquals(bytes,w.bytes(id));assertEquals(raw,w.state.jobs.single().recipe)
            assertNotEquals(batch,repo.ensureManual().id)
        }
        StudioWorkspace.openStudio(root).use{assertEquals(2,it.state.generationOrganization.batches.size);assertEquals(batch,readGalleryHistory(it).batchOrigins.getValue(id).groupId)}
    }
    @Test fun invalidHierarchyAndStaleEditsNeverReplaceCommittedOrganization(){StudioWorkspace.openStudio(root()).use{w->
        val repo=GenerationOrganizations(w);val a=GenerationFolder(name="Project");repo.addFolder(a);val b=GenerationFolder(name="Chapter",kind="chapter",parent=a.id);repo.addFolder(b);val saved=repo.state
        assertFailsWith<IllegalArgumentException>{repo.change(saved,saved.copy(folders=listOf(a.copy(parent=b.id),b)))}
        assertEquals(saved,repo.state);assertFailsWith<IllegalArgumentException>{repo.change(GenerationOrganization(),saved)};assertEquals(saved,repo.state)
    }}
    @Test fun automaticArchiveUsesSameNamingPlacementAndRetainsFullSnapshot(){StudioWorkspace.openStudio(root()).use{w->
        val repo=GenerationOrganizations(w);val p=GenerationFolder(name="风景集");repo.addFolder(p)
        val queue=BatchQueue(w);val group=BatchGroup.create("湖畔",NovelAiStudioDraft(basePrompt="synthetic landscape"),listOf(BatchDirection("横",832,1216,1)))
        queue.replaceGroups(listOf(group));val old=queue.state;assertTrue(old.createdAt>0)
        repo.archiveAutomatic("晨光",p.id);val b=repo.state.batches.single();assertEquals(old.id,b.id);assertEquals(old.createdAt,b.createdAt);assertEquals(p.id,b.parent);assertTrue(b.title.startsWith("晨光 · "));assertTrue(queue.state.groups.isEmpty())
        val ref=queue.state.archives.single().receipt;val bytes=w.bytes(ref.assetId);assertEquals(ref.sha256,sha256(bytes));assertEquals(old.copy(archives=emptyList()),documentJson.decodeFromString<BatchState>(bytes.decodeToString()))
    }}
    @Test fun archivingCannotBypassWholeBatch429Cooldown(){StudioWorkspace.openStudio(root()).use{w->
        val queue=BatchQueue(w);queue.replaceGroups(listOf(BatchGroup.create("test",NovelAiStudioDraft(basePrompt="synthetic"),listOf(BatchDirection("横",832,1216,1)))))
        queue.fail(BatchFailure("429","manual recovery required",notBefore=System.currentTimeMillis()+600000));val before=w.state
        assertFailsWith<IllegalArgumentException>{GenerationOrganizations(w).archiveAutomatic("no bypass",null)};assertEquals(before,w.state)
    }}
    @Test fun reopenedCopyReusesOriginalReferencesWithoutRewritingSource(){StudioWorkspace.openStudio(root()).use{w->
        val queue=BatchQueue(w);val group=BatchGroup.create("lake",NovelAiStudioDraft(basePrompt="synthetic"),listOf(BatchDirection("横",832,1216,1)));queue.replaceGroups(listOf(group));val batch=queue.state
        val origin=BatchResultOrigin(batch.id,group.id,group.name,group.slots.single().id,"横",1,group.slots.single().seed,"original")
        val raw=documentJson.encodeToString(GenerationReceipt(recipe=group.draft.toRecipe(),account=account,request=buildJsonObject{},batchOrigin=origin));val bytes=EditorRenderer.synthetic(32,32);val image=w.importImage("original.png",bytes,raw)
        val job=JobRecord(account=account,recipe=raw,state="COMPLETED",results=listOf(image))
        w.save(w.state.copy(jobs=listOf(job),batch=batch.copy(groups=listOf(group.copy(slots=group.slots.map{it.copy(status="DONE",results=listOf(image),attempts=listOf(job.id))})),mode="DONE")))
        val repo=GenerationOrganizations(w);repo.archiveAutomatic("original",null);repo.reopenAutomatic(batch.id);val copy=queue.state
        assertEquals("PAUSED",copy.mode);assertNotEquals(batch.id,copy.id);assertEquals(listOf(image),copy.groups.single().slots.single().results)
        repo.archiveAutomatic("copy",null);val projection=readGalleryHistory(w)
        assertEquals(setOf(image),generationBatchImages(repo.state,copy.id,projection));assertEquals(origin,projection.batchOrigins.getValue(image));assertContentEquals(bytes,w.bytes(image));assertEquals(raw,w.state.jobs.single().recipe)
    }}
}
