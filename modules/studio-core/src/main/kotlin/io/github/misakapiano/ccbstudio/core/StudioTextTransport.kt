package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.data.local.entity.ModelConfig
import com.example.chatbar.domain.chat.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.prompt.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A reservation is made for each wire attempt, including planner and repair stages. */
fun interface RequestAdmission { suspend fun await(service: String) }
class BudgetAdmission(private val budget: LiveRequestBudget, private val waiting: (Long) -> Unit = {}) : RequestAdmission {
    override suspend fun await(service: String) {
        while(true) {
            currentCoroutineContext().ensureActive()
            try { withContext(Dispatchers.IO) { budget.reserve(service) }; waiting(0); return }
            catch(e: BudgetWait) { waiting(e.waitMillis); delay(minOf(e.waitMillis, 1000)) }
            catch(e: DataProblem) { if(e.status != "IN_USE") throw e; delay(100) }
        }
    }
}

/** Platform adapter. Prompts, envelope, serialization and refusal policy are fixed authority. */
class StudioTextTransport(
    private val account: AccountProfile,
    private val vault: SecretVault,
    private val admission: RequestAdmission,
    private val client: OkHttpClient = studioHttpClient(),
) : NovelAiTextTransport {
    override fun streamText(stage: NovelAiTextStage, messages: List<ChatApiMessage>, modelConfig: ModelConfig): Flow<StreamEvent> = flow {
        account.validate(); require(account.service == "LLM" && account.provider == "OPENAI_COMPATIBLE")
        require(modelConfig.id == account.id && modelConfig.baseUrl == account.endpoint && modelConfig.modelName == account.model)
        val input = AuxiliaryMessageAssembler.assemble(messages, stage == NovelAiTextStage.GENERATE)
        val body = OpenAiChatRequestSerializer.serialize(input, modelConfig, stream=true)
        // Fail missing/undecryptable credentials BEFORE a reservation; never persist the plaintext.
        val request = withContext(Dispatchers.IO) {
            vault.use(requireNotNull(account.secretRef) { "账户尚未保存凭据" }) { bytes ->
                Request.Builder().url(account.endpoint.trimEnd('/') + "/chat/completions")
                    .header("Authorization", "Bearer ${bytes.decodeToString().trim()}")
                    .header("Accept", "text/event-stream").post(body.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
            }
        }
        admission.await("LLM")
        emitAll(readStream(request))
    }

    private fun readStream(request: Request): Flow<StreamEvent> = callbackFlow {
        val call = client.newCall(request)
        call.enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) { close(DataProblem("NETWORK", "设计连接中断；未自动重发")) }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if(!response.isSuccessful) { close(DataProblem("HTTP_${response.code}", "设计请求失败 HTTP ${response.code}；未自动重发")); return }
                    try {
                        val source = requireNotNull(response.body).source()
                        val text = StringBuilder(); var reasoning = false; var finish: String? = null; var refused = false
                        var total = 0L; var done = false; val data = StringBuilder()
                        fun publish(event: StreamEvent) { if(trySendBlocking(event).isFailure) throw IOException("Cancelled") }
                        fun dispatch() {
                            if(data.isEmpty()) return
                            val value = data.toString().trimEnd('\n'); data.setLength(0)
                            if(value.trim() == "[DONE]") { done=true; return }
                            // Parser errors may contain server text: never forward their messages/causes.
                            val chunk = try { OpenAiSseParser.parse(value) } catch(e: Exception) { throw DataProblem("RESPONSE", "设计响应格式无效") }
                            finish = chunk.finishReason ?: finish; refused = refused || chunk.refused
                            chunk.content?.let { text.append(it); publish(StreamEvent.Delta(it)) }
                            chunk.reasoningContent?.let { reasoning=true; publish(StreamEvent.ReasoningDelta(it)) }
                            OpenAiSseParser.parseUsage(value)?.let { publish(StreamEvent.Usage(it)) }
                        }
                        while(!done && !call.isCanceled()) {
                            if(source.exhausted()) break
                            val line = source.readUtf8LineStrict(1024L*1024)
                            total += line.length; require(total <= 4L*1024*1024) { "Response budget" }
                            if(line.isEmpty()) dispatch()
                            else if(line.startsWith("data:")) data.append(line.substring(5).removePrefix(" ")).append('\n')
                        }
                        if(data.isNotEmpty()) dispatch()
                        if(!done && finish == null) throw DataProblem("INCOMPLETE", "设计连接关闭但无完成标记")
                        AiTaskRefusalPolicy.failure(text.toString(), finish, refused)?.let { throw it }
                        if(finish == "length") throw ModelResponseTruncatedException()
                        if(text.isBlank()) throw AiTaskEmptyResponseException(reasoning)
                        publish(StreamEvent.Done); close()
                    } catch(e: Exception) {
                        close(when(e) {
                            is DataProblem, is AiTaskRefusalException, is AiTaskEmptyResponseException, is ModelResponseTruncatedException -> e
                            else -> DataProblem("RESPONSE", "设计响应中断或超出预算；未自动重发")
                        })
                    }
                }
            }
        })
        awaitClose { call.cancel() }
    }
}

fun studioHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
    .connectTimeout(30, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
