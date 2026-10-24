package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import kotlinx.coroutines.*
import java.nio.file.*
import kotlin.test.*

class ReviewBoundaryTest {
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun group()=BatchGroup.create("fixture",NovelAiStudioDraft(basePrompt="fixture"),listOf(BatchDirection("横",1216,832,2)))
    @Test fun pauseOrEndWinsBeforeAtomicReservationAndBeforeTransportAdmission() {
        for(end in listOf(false,true))StudioWorkspace.openStudio(root()).use{w->
            val q=BatchQueue(w);q.replaceGroups(listOf(group()));q.configure("fixture",BatchPolicy());val t=1_000_000L
            q.resume(t);val slot=q.next(t)!!.second
            if(end)q.end()else q.pause()
            assertFailsWith<GenerationNotSent>{q.begin(slot.id,t,0)};assertTrue(q.state.groups.flatMap{it.slots}.all{it.attempts.isEmpty()})
            if(!end){q.resume(t);q.begin(slot.id,t,0);q.pause();assertFailsWith<GenerationNotSent>{q.submitted(t)};q.withdrawUnsent(slot.id,0);assertEquals("PAUSED",q.state.mode);assertEquals("READY",q.state.groups.first().slots.first().status)}
        }
    }
    @Test fun queuedAccountSnapshotSurvivesEditingAndReopening() {
        val r=root();lateinit var original:AccountProfile
        StudioWorkspace.openStudio(r).use{w->
            original=AccountProfile(alias="frozen",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="fixture",secretRef=newId())
            w.save(w.state.copy(accounts=listOf(original)));val q=BatchQueue(w);q.replaceGroups(listOf(group()));q.configure(original.id,BatchPolicy())
            w.save(w.state.copy(accounts=listOf(original.copy(alias="edited",secretRef=newId()))));assertEquals(original,q.dispatchAccount())
        }
        StudioWorkspace.openStudio(r).use{assertEquals(original,BatchQueue(it).dispatchAccount())}
    }
    @Test fun closeCancelsOnlyPendingAdmissionAndReopenRequiresExplicitAction()=runBlocking {
        val gate=GenerationSendGate();val entered=CompletableDeferred<Unit>();var sends=0
        val waiting=async{assertFailsWith<GenerationNotSent>{gate.await(RequestAdmission{entered.complete(Unit);awaitCancellation()})}}
        entered.await();gate.close();withTimeout(3000){waiting.await()}
        assertFailsWith<GenerationNotSent>{gate.submit{sends++}};assertEquals(0,sends)
        gate.reopen();gate.submit{sends++};assertEquals(1,sends)
    }
    @Test fun promptInterruptionsReuseAssetAndRestoreLabelsAndObservationAcrossReopen() {
        for(point in listOf("PROMPT_ASSET_COMMITTED","COLLECTION_ASSET_COMMITTED")) {
            val r=root();val folder=Files.createDirectories(root());val file=Files.writeString(folder.resolve("fixture.prompt"),"first synthetic prompt\r\n")
            var registration="";var first=""
            StudioWorkspace.openStudio(r).use{w->val repo=LibraryRepository(w);registration=repo.addFolder(folder,"prompts",false).folders.single().id;first=repo.scan(registration).single().assetId!!;repo.label(first,"named",listOf("kept"))}
            Files.writeString(file,"second synthetic prompt\r\n")
            StudioWorkspace.openStudio(r).use{w->
                var armed=true;val repo=LibraryRepository(w){if(it==point&&armed){armed=false;throw java.io.IOException("synthetic interruption")}}
                assertFails{repo.scan(registration)};assertEquals(2,repo.prompts().size);assertEquals(first,repo.load().observations.values.single().assetId)
            }
            StudioWorkspace.openStudio(r).use{w->val repo=LibraryRepository(w);val recovered=repo.scan(registration).single().assetId!!
                assertNotEquals(first,recovered);assertEquals(2,repo.prompts().size);assertEquals(AssetLabels("named",listOf("kept")),repo.load().labels[recovered]);assertEquals("first synthetic prompt\r\n",repo.prompt(first).text)
                assertEquals("UNCHANGED",repo.scan(registration).single().status);assertEquals(2,repo.prompts().size)
            }
        }
    }
    @Test fun manualCapacityRejectsNewWorkBeforeSendAndLegacyOverflowStillArchivesWithoutLoss() {
        StudioWorkspace.openStudio(root()).use{w->
            val repo=GenerationOrganizations(w);val b=repo.ensureManual();val account=AccountProfile(alias="fixture",service="NAI",provider="SYNTHETIC",model="fixture")
            // Metadata-only cardinality fixture; separate transport tests verify actual PNG bytes.
            val ids=List(1001){newId()}
            w.save(w.state.copy(jobs=listOf(JobRecord(account=account,recipe="",state="COMPLETED",manualBatchId=b.id,results=ids.take(999)))))
            repo.requireManualCapacity(b.id,1);assertFails{repo.requireManualCapacity(b.id,2)}
            w.save(w.state.copy(jobs=listOf(w.state.jobs.single().copy(results=ids))))
            assertFails{repo.requireManualCapacity(b.id,1)};repo.archiveManual(b.id,"legacy retained",null)
            assertNull(repo.state.currentManual);assertEquals(ids,repo.state.batches.single().groups.flatMap{it.images});assertEquals(listOf(1000,1),repo.state.batches.single().groups.map{it.images.size})
        }
    }
}
