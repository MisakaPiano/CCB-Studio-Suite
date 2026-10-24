package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import java.nio.ByteBuffer
import java.nio.file.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class ReviewDispatchTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun frame(png:ByteArray):ByteArray {
        val payload=java.io.ByteArrayOutputStream().also{out->
            out.write(0x82);for(text in listOf("event_type","final","image")){out.write(0xa0+text.length);out.write(text.toByteArray())}
            out.write(0xc6);out.write(ByteBuffer.allocate(4).putInt(png.size).array());out.write(png)
        }.toByteArray()
        return ByteBuffer.allocate(4+payload.size).putInt(payload.size).put(payload).array()
    }
    @Test fun restoredWorkspaceCannotChangeStableDefaultBytesOrShortcuts() {
        val base=Files.createDirectories(root());val app=Files.createDirectories(base.resolve("115-fixture"));val data=root();val runtime=root();val restored=root()
        val descriptor=buildJsonObject{put("format",1);put("version",115);put("installed",app.toString());put("data",data.toString());put("runtime",runtime.toString())}
        Files.writeString(base.resolve("launch-current.json"),descriptor.toString());Files.writeString(app.resolve("INSTALL_MANIFEST.json"),"{\"files\":[]}");Files.writeString(app.resolve("update-config.json"),"{\"version\":115}")
        Files.write(base.resolve("CCBStudioLauncher.exe"),byteArrayOf(1,2,3));Files.write(base.resolve("Launch-CCB-Studio.lnk"),byteArrayOf(4,5,6))
        val files=listOf("launch-current.json","CCBStudioLauncher.exe","Launch-CCB-Studio.lnk").associate{it to Files.readAllBytes(base.resolve(it))}
        val old=System.getProperty("jpackage.app-path")
        try {System.setProperty("jpackage.app-path",app.resolve("fixture.exe").toString())
            assertFalse(DesktopEntrypoints.ensure(restored,runtime));assertFalse(DesktopEntrypoints.ownsDefault(restored,runtime));assertTrue(DesktopEntrypoints.ownsDefault(data,runtime));assertFails{DesktopEntrypoints.create("desktop",restored,runtime)}
            files.forEach{(name,bytes)->assertContentEquals(bytes,Files.readAllBytes(base.resolve(name)))}
        }finally{if(old==null)System.clearProperty("jpackage.app-path")else System.setProperty("jpackage.app-path",old)}
    }
    @Test fun pauseAndEndDuringAdmissionSendZeroGenerationRequests()=runBlocking {
        for(end in listOf(false,true)) {
            val r=root();val w=StudioWorkspace.openStudio(r);val waiting=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();val calls=AtomicInteger()
            val http=studioHttpClient().newBuilder().addInterceptor{chain->
                if(chain.request().url.encodedPath.contains("generate"))calls.incrementAndGet()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("synthetic").body("{\"trainingStepsLeft\":1000,\"tier\":3,\"active\":true}".toResponseBody()).build()
            }.build()
            val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r),r.resolve("runtime/budget"),http,RequestAdmission{waiting.complete(Unit);release.await()},true)
            try {
                val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="fixture",secretRef=c.vault.put("synthetic queue credential".toByteArray()))
                w.save(w.state.copy(accounts=listOf(account)));val q=c.batch.queue;q.replaceGroups(listOf(BatchGroup.create("fixture",NovelAiStudioDraft(basePrompt="fixture"),listOf(BatchDirection("横",1216,832,2)))));q.configure(account.id,BatchPolicy());val now=System.currentTimeMillis();q.resume(now)
                val next=q.next(now)!!;val run=scope.async{c.executeBatch(q,next.first,next.second){}}
                withTimeout(10000){waiting.await()};if(end)q.end()else q.pause();release.complete(Unit);withTimeout(10000){run.await()}
                assertEquals(0,calls.get());assertEquals("NOT_SENT",w.state.jobs.single().state);assertEquals(if(end)"ENDED"else"PAUSED",q.state.mode);assertEquals(0,q.state.completed)
            }finally{release.complete(Unit);withContext(awt){c.close()};scope.cancel()}
        }
    }
    @Test fun batchMaintenanceDrainsRealRunnerAndKeepsOnlyInflightSlot()=runBlocking {
        val r=root();val w=StudioWorkspace.openStudio(r);val png=EditorRenderer.synthetic(42,42);val entered=CompletableDeferred<Unit>();val release=CountDownLatch(1);val calls=AtomicInteger()
        val http=studioHttpClient().newBuilder().addInterceptor{chain->
            val bytes=if(chain.request().url.encodedPath.contains("generate")){
                calls.incrementAndGet();entered.complete(Unit);check(release.await(15,TimeUnit.SECONDS));frame(png)
            }else "{\"trainingStepsLeft\":1000,\"tier\":3,\"active\":true}".toByteArray()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("synthetic").body(bytes.toResponseBody()).build()
        }.build()
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r),r.resolve("runtime/budget"),http,RequestAdmission{},true)
        try {
            val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="fixture",secretRef=c.vault.put("synthetic batch credential".toByteArray()))
            w.save(w.state.copy(accounts=listOf(account)));val q=c.batch.queue;q.replaceGroups(listOf(BatchGroup.create("fixture",NovelAiStudioDraft(basePrompt="fixture"),listOf(BatchDirection("横",1216,832,2)))));q.configure(account.id,BatchPolicy())
            withContext(awt){c.batch.refresh();c.batch.start()};withTimeout(10000){entered.await()}
            val drain=scope.async{c.enterMaintenance()};withTimeout(5000){while(!c.maintenance)delay(10)}
            assertNull(withTimeoutOrNull(100){drain.await();true});release.countDown();withTimeout(10000){drain.await()}
            assertEquals(1,calls.get());assertEquals(1,q.state.completed);assertEquals("PAUSED",q.state.mode);assertEquals("COMPLETED",w.state.jobs.single().state);assertContentEquals(png,w.bytes(w.state.jobs.single().results.single()))
        }finally{release.countDown();withContext(awt){c.close()};scope.cancel()}
    }
    @Test fun closeAndUpdateDrainKeepSlowInflightResultAndNeverSendTheNextManualRequest()=runBlocking {
        for(close in listOf(false,true)) {
            val r=root();val w=StudioWorkspace.openStudio(r);val png=EditorRenderer.synthetic(40,40);val entered=CompletableDeferred<Unit>();val release=CountDownLatch(1);val calls=AtomicInteger()
            val http=studioHttpClient().newBuilder().addInterceptor{chain->
                calls.incrementAndGet();entered.complete(Unit);check(release.await(15,TimeUnit.SECONDS))
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("synthetic").body(frame(png).toResponseBody()).build()
            }.build()
            val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r),r.resolve("runtime/budget"),http,RequestAdmission{},true)
            try {
                val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="fixture",secretRef=c.vault.put("synthetic drain credential".toByteArray()))
                w.save(w.state.copy(accounts=listOf(account)));withContext(awt){c.chooseAccount("NAI",account.id);c.edit{it.copy(basePrompt="fixture",continuousModeEnabled=true,continuousTargetCount=3)};c.generate()}
                withTimeout(10000){entered.await()};val drain=scope.async{if(close)c.close()else c.enterMaintenance()}
                withTimeout(5000){while(!c.maintenance)delay(10)};assertFalse(c.generationSends.open)
                assertNull(withTimeoutOrNull(100){drain.await();true});assertFails{StudioWorkspace.openStudio(r)}
                release.countDown();withTimeout(10000){drain.await()};assertEquals(1,calls.get())
                if(!close){assertEquals("COMPLETED",w.state.jobs.single().state);assertContentEquals(png,w.bytes(w.state.jobs.single().results.single()));withContext(awt){c.close()}}
                StudioWorkspace.openStudio(r).use{again->assertEquals("COMPLETED",again.state.jobs.single().state);assertContentEquals(png,again.bytes(again.state.jobs.single().results.single()));assertEquals(1,GenerationOrganizations(again).manualImages().size)}
            }finally{release.countDown();runCatching{withContext(awt){c.close()}};scope.cancel()}
        }
    }
}
