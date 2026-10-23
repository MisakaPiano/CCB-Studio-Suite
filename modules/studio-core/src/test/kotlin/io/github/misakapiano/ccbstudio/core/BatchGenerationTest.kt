package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import java.nio.file.Files
import kotlin.test.*
import kotlinx.serialization.encodeToString
import org.junit.Test

class BatchGenerationTest {
    private fun fixture():StudioWorkspace=StudioWorkspace.openStudio(Files.createTempDirectory("studio-batch-test-"))
    private fun group(n:Int=4)=BatchGroup.create("合成风景",NovelAiStudioDraft(basePrompt="landscape"),listOf(BatchDirection("横",1216,832,n),BatchDirection("纵",832,1216,n),BatchDirection("方",1024,1024,n)))
    private fun ready(w:StudioWorkspace,n:Int=4):BatchQueue=BatchQueue(w).also{it.replaceGroups(listOf(group(n)));it.configure("synthetic-account",BatchPolicy())}
    @Test fun rateLimitLocksWholeBatchAcrossRestartUntilExplicitResume() {
        val w=fixture();val root=w.root;val q=ready(w);val now=1_000_000L
        q.resume(now);val slot=q.next(now)!!.second;val dispatch=q.begin(slot.id,now,0)
        q.fail(BatchFailure("429","整批暂停",slot.id,now+120_000))
        assertNull(q.next(now+999_000));assertFails{q.resume(now+60_000,retry=true)};assertFails{q.resume(now+150_000,skip=true)}
        assertFails{q.replaceGroups(emptyList())};w.close()
        StudioWorkspace.openStudio(root).use{reopened->val restored=BatchQueue(reopened);restored.recover();assertEquals("429",restored.state.error!!.code)
            assertNull(restored.next(now+999_000));restored.resume(now+150_000,retry=true)
            val retry=restored.begin(slot.id,now+150_000,0);assertNotEquals(dispatch.jobId,retry.jobId)
            restored.fail(BatchFailure("429","再次暂停",slot.id));assertNull(restored.next(now+999_000))
        }
    }
    @Test fun serialIntervalSlowResponseRestAndSleepNeverCatchUp()=fixture().use{w->
        val q=ready(w);q.configure("a",BatchPolicy(intervalSeconds=60,runMinutes=1,restMinutes=1,intervalJitterSeconds=0,restJitterMinutes=0));val t=1_000_000L;q.resume(t)
        val a=q.begin(q.next(t)!!.second.id,t,0);assertNull(q.next(t+180_000));q.complete(a.slot.id,listOf(newId()))
        assertNull(q.next(t+180_000));assertEquals("RESTING",q.state.mode);assertNull(q.next(t+239_000))
        assertNull(q.next(t+240_000));val b=q.begin(q.next(t+240_000)!!.second.id,t+240_000,0);q.complete(b.slot.id,listOf(newId()))
        assertNull(q.next(t+241_000));q.pause();assertNull(q.next(t+999_000))
    }
    @Test fun restartReconcilesSavedResultWithoutResubmittingAndUnknownRemainsBlocked() {
        val w=fixture();val root=w.root;val q=ready(w);val t=1_000_000L;q.resume(t);val d=q.begin(q.next(t)!!.second.id,t,0)
        val account=AccountProfile(alias="test",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=d.group.draft.selectedModel.apiId)
        w.operation{w.save(w.state.copy(jobs=listOf(JobRecord(id=d.jobId,account=account,recipe="",state="FAILED",results=listOf(newId())))))};w.close()
        StudioWorkspace.openStudio(root).use{r->val recovered=BatchQueue(r);recovered.recover();assertEquals(1,recovered.state.completed);assertEquals("PAUSED",recovered.state.mode)
            recovered.resume(t+120_000);recovered.begin(recovered.next(t+120_000)!!.second.id,t+120_000,0)
        }
        StudioWorkspace.openStudio(root).use{r->val recovered=BatchQueue(r);recovered.recover();assertEquals("UNCERTAIN",recovered.state.error!!.code);assertNull(recovered.next(t+999_000))}
    }
    @Test fun snapshotsSeedsCountsBudgetAndStartedGroupsAreProtected()=fixture().use{w->
        val g=group();assertEquals(12,g.slots.size);assertTrue(g.slots.all{g.draft.withSlot(it).activeSettings.count==1})
        val shared=BatchGroup.create("shared",g.draft,listOf(BatchDirection("横",1216,832),BatchDirection("纵",832,1216)),true)
        assertEquals(shared.slots.take(4).map{it.seed},shared.slots.drop(4).map{it.seed})
        val q=ready(w);q.resume(1_000_000);val slot=q.next(1_000_000)!!.second;assertFails{q.begin(slot.id,1_000_000,1)}
        q.begin(slot.id,1_000_000,0);q.pause();q.complete(slot.id,listOf(newId()));assertEquals("PAUSED",q.state.mode)
        assertFails{q.replaceGroups(emptyList())};Unit
    }
    @Test fun malformedGuidanceAndSizesFailBeforeQueueing() {
        assertFails{BatchGroup.create("bad",NovelAiStudioDraft(basePrompt="x"),listOf(BatchDirection("横",1,2)))}
        assertEquals(130000L,retryAfterDeadline("120",10000));assertEquals(0L,retryAfterDeadline("invalid",10000))
        assertEquals(1445412480000L,retryAfterDeadline("Wed, 21 Oct 2015 07:28:00 GMT",10000))
    }
    @Test fun jitterIsSampledOncePersistedAndCannotShortenMinimum() {
        val w=fixture();val root=w.root;val q=BatchQueue(w){bound->bound-1}
        q.replaceGroups(listOf(group()));q.configure("a",BatchPolicy(runMinutes=1));val t=1_000_000L;q.resume(t)
        val first=q.begin(q.next(t)!!.second.id,t,0);assertEquals(t+80_000,q.state.nextRequestAt)
        q.complete(first.slot.id,listOf(newId()));assertNull(q.next(t+60_000));assertNull(q.next(t+80_000));assertEquals(t+80_000+7*60_000,q.state.restUntil)
        val deadline=q.state.restUntil;w.close()
        StudioWorkspace.openStudio(root).use{r->val recovered=BatchQueue(r){error("must not resample on reopen")};recovered.recover();assertEquals(deadline,recovered.state.restUntil);recovered.resume(t+90_000);assertNull(recovered.next(t+120_000));assertEquals(deadline,recovered.state.restUntil)}
    }
    @Test fun fullThirtyGroup360ImageScheduleIsFiniteAndUsesBoundedDescriptors()=fixture().use{w->
        var randomCalls=0;val q=BatchQueue(w){bound->(randomCalls++%bound)}
        q.replaceGroups(List(30){group().copy(name="合成组 $it",draft=NovelAiStudioDraft(basePrompt="landscape, ".repeat(350)))})
        q.configure("a",BatchPolicy(roundRobin=true));var now=1_000_000L;q.resume(now);var sent=0;var rests=0
        while(q.state.running){
            val next=q.next(now)
            if(next==null){if(q.state.mode=="RESTING"){rests++;now=q.state.restUntil}else now+=1000;continue}
            val dispatch=q.begin(next.second.id,now,0);assertNull(q.next(now));assertTrue(q.state.nextRequestAt-now in 60000..80000)
            q.complete(dispatch.slot.id,listOf(newId()));sent++;now=q.state.nextRequestAt
        }
        assertEquals(360,sent);assertEquals(360,q.state.completed);assertEquals("DONE",q.state.mode);assertTrue(rests>=5)
        assertTrue(documentJson.encodeToString(w.state).toByteArray().size<1024*1024)
        q.newBatch();assertEquals(0,q.state.total);assertEquals(360,q.state.archives.single().completed)
        assertEquals(0,SharedAssets.images(w).size)
    }
}
