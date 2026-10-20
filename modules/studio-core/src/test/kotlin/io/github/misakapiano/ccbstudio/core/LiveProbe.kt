package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.chat.*
import com.example.chatbar.data.local.entity.ModelConfig
import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import java.nio.file.*
import java.util.concurrent.TimeUnit

/** Explicit, opt-in test-classpath runner. Never reads user libraries, never writes Desktop. */
object LiveProbe {
    @JvmStatic fun main(args:Array<String>)=runBlocking {
        require(args.size==2 && args[0] in setOf("LLM","NAI","CREDENTIAL_CHECK"))
        val root=Paths.get(args[1]).toAbsolutePath().normalize();Files.createDirectories(root)
        if(args[0]=="CREDENTIAL_CHECK") {
            for(service in listOf("LLM","NAI")) {
                val bytes=loadDesktopCredential(service)
                println("$service credentialRead=${bytes!=null}");bytes?.fill(0)
            }
            return@runBlocking
        }
        val service=args[0];val secret=loadDesktopCredential(service)?:error("Authorized stored credential unavailable")
        val budget=LiveRequestBudget(root.resolve("budget"))
        var dispatched=false;var httpStatus:Int?=null
        val client=OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .connectTimeout(30,TimeUnit.SECONDS).readTimeout(180,TimeUnit.SECONDS).addInterceptor {chain ->
                val request=chain.request()
                require(request.url.isHttps && request.url.host==if(service=="LLM")"api.siliconflow.cn" else "image.novelai.net")
                budget.reserve(service)
                dispatched=true
                chain.proceed(request).also {httpStatus=it.code}
            }.build()
        val receipt=linkedMapOf<String,JsonElement>("service" to JsonPrimitive(service),"sourceAuthority" to JsonPrimitive(AUTHORITY_SHA),"input" to JsonPrimitive("Synthetic ceramic cup on desk"))
        try {
            if(service=="LLM") {
                val model=ModelConfig("live-validation","SiliconFlow validation","https://api.siliconflow.cn/v1","","Qwen/Qwen3-8B",createdAt=1)
                val messages=listOf(ChatApiMessage.text("user","Describe a ceramic cup on a quiet desk in one short English sentence."))
                val body=OpenAiChatRequestSerializer.serialize(messages,model,stream=false,maxTokens=256,disableThinking=true)
                val request=Request.Builder().url(model.baseUrl+"/chat/completions").header("Authorization","Bearer "+secret.decodeToString()).post(body.toRequestBody("application/json".toMediaType())).build()
                client.newCall(request).execute().use {r->
                    receipt["httpStatus"]=JsonPrimitive(r.code)
                    check(r.isSuccessful) {"Provider HTTP ${r.code}"}
                    val source=r.body!!.source()
                    require(!source.request(1024L*1024+1)) {"Response exceeds limit"}
                    val raw=source.readByteArray()
                    val result=OpenAiCompletionResponsePolicy.requireCompletion(raw.decodeToString())
                    receipt["responseSha256"]=JsonPrimitive(sha256(raw));receipt["textLength"]=JsonPrimitive(result.length)
                    receipt["model"]=JsonPrimitive(model.modelName)
                }
            } else {
                val settings=NovelAiGenerationSettings(customWidth=512,customHeight=512,steps=28,seedMode=NovelAiSeedMode.FIXED,seed=20261021)
                val plan=NovelAiPromptPlan("ceramic cup, quiet desk, soft window light, still life",emptyList())
                val api=NovelAiImageService(client,safeErrorsOnly=true)
                val wire=api.buildRequestBody(plan,settings.imageSize(),settings)
                val events=api.generate(secret.decodeToString(),plan,settings.imageSize(),settings,maxRateLimitRetries=0).toList()
                val finals=events.filterIsInstance<NovelAiImageEvent.Final>()
                check(events.none {it is NovelAiImageEvent.Error} && finals.size==1) {"Provider returned no complete image; no automatic retry"}
                val image=finals.single().image;EditorRenderer.decode(image).flush()
                StudioWorkspace.open(root.resolve("synthetic-live-workspace")).use {w->
                    val id=w.importImage("live-synthetic-cup.png",image,wire)
                    check(w.bytes(id).contentEquals(image))
                    receipt["assetId"]=JsonPrimitive(id)
                }
                receipt["imageBytes"]=JsonPrimitive(image.size);receipt["imageSha256"]=JsonPrimitive(sha256(image))
                receipt["requestSha256"]=JsonPrimitive(sha256(wire.toByteArray()));receipt["intermediates"]=JsonPrimitive(events.filterIsInstance<NovelAiImageEvent.Intermediate>().size)
            }
            receipt["status"]=JsonPrimitive("PASS")
        } catch(e:Exception) {
            receipt["status"]=JsonPrimitive(when {e is BudgetWait->"NOT TESTED_RATE_LIMIT";dispatched && httpStatus==null->"UNKNOWN_REMOTE_OUTCOME";!dispatched->"ENVIRONMENT_UNAVAILABLE";else->"FAILED"})
            receipt["failureType"]=JsonPrimitive(e.javaClass.simpleName)
            // Provider bodies, exception messages and credentials never enter diagnostics.
        } finally {receipt["requestDispatched"]=JsonPrimitive(dispatched);httpStatus?.let {receipt["httpStatus"]=JsonPrimitive(it)};secret.fill(0);client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
        val p=root.resolve("${System.currentTimeMillis()}-$service.json")
        writeNewForced(p,JsonObject(receipt).toString().toByteArray());println(JsonObject(receipt))
    }
    private fun loadDesktopCredential(service:String):ByteArray? {
        val logical=if(service=="LLM")"silicon-flow-api-key" else "novelai-token"
        val name=sha256("ccb-desktop-credential/v1/global/$logical".toByteArray())+".ccbsecret"
        val path=Paths.get(System.getenv("LOCALAPPDATA"),"ChatChatBarDesktopCredentials",name)
        if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return null
        val bytes=readBounded(path,1024*1024)
        try {
            val b=ByteBuffer.wrap(bytes);val magic=ByteArray(6);b.get(magic)
            require(magic.contentEquals("CCBSEC".toByteArray()) && b.get().toInt()==1)
            val size=b.int;require(size>0 && size==b.remaining());val cipher=ByteArray(size);b.get(cipher)
            return try {Crypt32Util.cryptUnprotectData(cipher,"ChatChatBarDesktop/SecretStore/v1".toByteArray(),WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,null)} finally {cipher.fill(0)}
        } finally {bytes.fill(0)}
    }
}
