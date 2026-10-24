package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopNovelAiInfrastructure
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
import kotlin.system.exitProcess

/** TEST-ONLY native launcher main. Not included in the production application JAR.
 * Drives real Workbench/prepareUpdateHandoff/close and a real Windows parent handle.
 * Only the generation response is synthetic; the updater transport is literal loopback.
 */
object ReviewUpdateNativeA {
    @JvmStatic fun main(args:Array<String>) {
        val out=Path.of(args.single()).toAbsolutePath().normalize();safePath(out)
        require(out.any{it.toString()==".local"}&&out.any{it.toString()=="review-update-native"})
        require(Files.readString(out.resolve("SYNTHETIC_ONLY.txt"))=="CCB_REVIEW_UPDATE_LAB")
        var exit=1
        try {runBlocking{exercise(out)};exit=0}
        catch(e:Throwable){writeUpdateJson(out.resolve("A_FAILED.json"),buildJsonObject{put("status","FAILED");put("reason",e.javaClass.simpleName);put("message",e.message?.take(200).orEmpty())})}
        exitProcess(exit)
    }
    private suspend fun exercise(out:Path)=coroutineScope {
        val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
        val data=out.resolve("data");val runtime=out.resolve("runtime");val w=StudioWorkspace.openStudio(data)
        val entered=CompletableDeferred<Unit>();val release=CountDownLatch(1);val sends=AtomicInteger();val png=EditorRenderer.synthetic(44,46)
        val payload=java.io.ByteArrayOutputStream().also{stream->stream.write(0x82);listOf("event_type","final","image").forEach{stream.write(0xa0+it.length);stream.write(it.toByteArray())};stream.write(0xc6);stream.write(ByteBuffer.allocate(4).putInt(png.size).array());stream.write(png)}.toByteArray()
        val frame=ByteBuffer.allocate(4+payload.size).putInt(payload.size).put(payload).array()
        val http=studioHttpClient().newBuilder().addInterceptor{chain->
            check(chain.request().url.encodedPath.contains("generate")){"Unexpected generation transport"}
            sends.incrementAndGet();entered.complete(Unit);check(release.await(60,TimeUnit.SECONDS))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("synthetic slow response").body(frame.toResponseBody()).build()
        }.build()
        val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(runtime.resolve("catalog")),runtime.resolve("budget"),http,RequestAdmission{},true)
        try {
            val ref=c.vault.put("SYNTHETIC_NATIVE_UPDATE_CREDENTIAL_ONLY".toByteArray())
            val account=AccountProfile(alias="Synthetic update account",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model="fixture",secretRef=ref)
            val editedRef=c.vault.put("SYNTHETIC_EDITED_NATIVE_UPDATE_CREDENTIAL_ONLY".toByteArray())
            val edited=account.copy(alias="Synthetic edited account",secretRef=editedRef)
            val original=w.importImage("synthetic-original.png",EditorRenderer.synthetic(48,48),"synthetic retained recipe")
            w.save(w.state.copy(accounts=listOf(edited),batch=w.state.batch.copy(accountId=account.id,accountSnapshot=account)))
            withContext(awt){c.updater.check(true);c.updater.job!!.join()}
            check(c.updater.result?.get("status")?.jsonPrimitive?.content=="DOWNLOADED_VERIFIED"){c.updater.message}
            withContext(awt){c.chooseAccount("NAI",account.id);c.edit{it.copy(basePrompt="synthetic update fixture",continuousModeEnabled=true,continuousTargetCount=3)};c.generate()}
            withTimeout(15000){entered.await()}
            val drain=scope.async{c.enterMaintenance()}
            withTimeout(5000){while(!c.maintenance)delay(10)}
            check(!c.generationSends.open);check(withTimeoutOrNull(150){drain.await();true}==null)
            writeUpdateJson(out.resolve("A_DRAIN_WAITING.json"),buildJsonObject{put("pid",ProcessHandle.current().pid());put("command",ProcessHandle.current().info().command().orElse("unknown"));put("gateClosed",true);put("responseStillPending",true);put("generationSends",sends.get())})
            release.countDown();withTimeout(15000){drain.await()}
            check(sends.get()==1);check(w.state.jobs.single().state=="COMPLETED");check(w.bytes(w.state.jobs.single().results.single()).contentEquals(png))
            val before=updateSnapshot(w);val credentials=before["credentials"]!!.jsonArray
            check(credentials.size==2&&credentials.all{it.jsonObject["sha256"]!=null})
            val plan=prepareUpdateHandoff(c,runtime)
            writeUpdateJson(out.resolve("A_HANDOFF_READY.json"),buildJsonObject{put("plan",plan.toString());put("parentPid",ProcessHandle.current().pid());put("generationSends",sends.get());put("snapshot",before);put("originalId",original);put("drain", "PRODUCTION_WORKBENCH_COMPLETED")})
            withContext(awt){c.close()}
            writeUpdateJson(out.resolve("A_NORMAL_EXIT_READY.json"),buildJsonObject{put("status","CLOSED_AFTER_DRAIN");put("parentPid",ProcessHandle.current().pid());put("workspaceClosed",true)})
        }finally{release.countDown();scope.cancel()}
    }
}
