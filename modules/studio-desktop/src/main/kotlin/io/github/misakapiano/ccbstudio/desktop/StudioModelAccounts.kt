package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.runtime.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Desktop ModelDiscoveryService behavior, adapted to Suite's injected transport and vault.
 * Source: ChatChatBar-Desktop 5ef95428421f7c693b010ac88f51d470804ab07a.
 * No generation request, automatic retry, redirect, provider body or credential logging.
 */
internal suspend fun discoverModelIds(http:OkHttpClient,endpoint:String,key:String):List<String> {
    val base=endpoint.trim().trimEnd('/').toHttpUrlOrNull()
        ?:throw IOException("API 地址无效")
    require(base.isHttps && base.username.isEmpty() && base.password.isEmpty() && base.query==null && base.fragment==null){"请填写不含账号、查询参数的 HTTPS API 地址"}
    require(key.trim().all{it in '!'..'~'}){"凭据含空格、换行或无效字符"}
    val request=Request.Builder().url(base.newBuilder().addPathSegment("models").build()).get()
        .apply{if(key.isNotBlank())header("Authorization","Bearer ${key.trim()}")}.build()
    val client=http.newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).callTimeout(30,TimeUnit.SECONDS).build()
    return suspendCancellableCoroutine { continuation ->
        val call=client.newCall(request)
        continuation.invokeOnCancellation{call.cancel()}
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException){continuation.resumeWith(Result.failure(IOException("获取失败，请检查网络和 API 地址；可手动填写模型 ID")))}
            override fun onResponse(call:Call,response:Response) {
                continuation.resumeWith(runCatching {response.use {
                    if(!it.isSuccessful)throw IOException(when(it.code){
                        401,403->"模型列表访问被拒绝（HTTP ${it.code}），请检查凭据与权限"
                        404,405->"服务商未提供模型列表（HTTP ${it.code}），可手动填写模型 ID"
                        else->"获取模型列表失败（HTTP ${it.code}），可稍后重试"
                    })
                    val source=requireNotNull(it.body).source()
                    if(source.request(1024L*1024+1))throw IOException("模型列表过大，请手动填写模型 ID")
                    val root=runCatching{Json.parseToJsonElement(source.readUtf8()) as? JsonObject}.getOrNull()
                    val data=root?.get("data") as? JsonArray?:throw IOException("模型列表格式不兼容，需要 OpenAI 兼容接口的 data 列表")
                    data.mapNotNull {entry->((entry as? JsonObject)?.get("id") as? JsonPrimitive)?.takeIf{it.isString}?.content?.trim()?.takeIf{it.isNotEmpty()}}
                        .distinct().sortedWith(String.CASE_INSENSITIVE_ORDER).also{ids->if(ids.isEmpty())throw IOException("服务商未返回模型，请检查权限或手动填写")}
                }})
            }
        })
    }
}

internal data class ModelAccountDraft(
    val existing:AccountProfile?=null,val alias:String="",val service:String="LLM",
    val endpoint:String="https://api.siliconflow.cn/v1",val model:String="",val key:String="",
    val multimodal:Boolean=false,val visionId:String?=null,val useGlobalKey:Boolean=false,
) {
    override fun toString()="ModelAccountDraft([REDACTED])"
    companion object {fun from(a:AccountProfile)=ModelAccountDraft(a,a.alias,a.service,a.endpoint,a.model,"",a.isMultimodal,a.visionAccountId,a.secretRef==SecretVault.GLOBAL_LLM_REF)}
}

internal class ModelAccountsController(private val c:WorkbenchController) {
    var draft by mutableStateOf(c.state.accounts.firstOrNull{it.id==c.llmId}?.let{ModelAccountDraft.from(it)}?:ModelAccountDraft());private set
    private var baseline=draft
    var globalKey by mutableStateOf("")
    var globalConfigured by mutableStateOf(c.vault.hasGlobalKey());private set
    val dirty get()=draft!=baseline || globalKey.isNotEmpty()
    val canSave get()=draft.alias.isNotBlank() && draft.model.isNotBlank() && (draft.useGlobalKey || draft.key.isNotBlank() || draft.existing?.secretRef?.takeUnless{it==SecretVault.GLOBAL_LLM_REF}!=null)
    fun saveGlobal() {
        if(c.busy||c.maintenance||globalKey.isBlank())return
        val value=globalKey;invalidate()
        c.run("保存全局 API Key") {
            val bytes=value.trim().toByteArray()
            try{c.vault.putGlobal(bytes)}finally{bytes.fill(0)}
            withContext(c.scope.coroutineContext.minusKey(Job)){globalKey="";globalConfigured=true}
        }
    }
    fun newVision() {
        if(c.busy||c.maintenance)return
        val action={load(null);draft=draft.copy(multimodal=true,useGlobalKey=true);baseline=draft}
        if(dirty)pending=action else action()
    }
    var ids by mutableStateOf(emptyList<String>());private set
    var searching by mutableStateOf(false);private set
    var error by mutableStateOf<String?>(null);private set
    var query by mutableStateOf("")
    var pending by mutableStateOf<(() -> Unit)?>(null);private set
    private var generation=0L
    private var job:Job?=null
    fun edit(change:(ModelAccountDraft)->ModelAccountDraft) {
        if(c.busy||c.maintenance)return
        val next=change(draft)
        if(next.endpoint!=draft.endpoint||next.key!=draft.key||next.service!=draft.service||next.useGlobalKey!=draft.useGlobalKey)invalidate()
        draft=next
    }
    private fun invalidate(){generation++;job?.cancel();job=null;ids=emptyList();searching=false;error=null;query=""}
    private fun load(a:AccountProfile?){invalidate();draft=a?.let{ModelAccountDraft.from(it)}?:ModelAccountDraft();baseline=draft}
    fun open(a:AccountProfile?){if(c.busy||c.maintenance)return;if(dirty)pending={load(a)} else load(a)}
    fun discardAndContinue(){val action=pending;pending=null;if(action!=null){globalKey="";action()}}
    fun continueEditing(){pending=null}
    fun discard(){if(!c.busy&&!c.maintenance){globalKey="";load(draft.existing)}}
    fun select(id:String){if(id in ids)edit{it.copy(model=id)}}
    fun cancel(){invalidate()}
    fun discover() {
        if(c.busy||c.maintenance||draft.service!="LLM")return
        invalidate();val epoch=generation;val input=draft;searching=true
        job=c.scope.launch {
            try {
                val ref=if(input.useGlobalKey)SecretVault.GLOBAL_LLM_REF else input.existing?.secretRef?.takeUnless{it==SecretVault.GLOBAL_LLM_REF}
                val key=if(!input.useGlobalKey&&input.key.isNotBlank())input.key else ref?.let{withContext(Dispatchers.IO){c.vault.use(it){bytes->bytes.decodeToString()}}}.orEmpty()
                val result=discoverModelIds(c.http,input.endpoint,key)
                if(epoch==generation)ids=result
            } catch(e:CancellationException){throw e}
            catch(e:Exception){if(epoch==generation)error=when(e){is IOException,is IllegalArgumentException->e.message;else->"无法读取已存凭据，请重新填写"}}
            finally{if(epoch==generation){searching=false;job=null}}
        }
    }
    fun save(selectAfterSave:Boolean=true) {
        if(c.busy||c.maintenance)return
        val input=draft;invalidate()
        c.saveAccount(input.existing,input.alias,input.service,input.endpoint,input.model,input.key,input.multimodal,input.visionId,input.useGlobalKey,selectAfterSave){saved->
            draft=ModelAccountDraft.from(saved);baseline=draft
        }
    }
}
