package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.*
import com.example.chatbar.domain.chat.*
import com.example.chatbar.data.local.entity.ModelConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.msgpack.core.MessagePack
import java.nio.ByteBuffer
import java.util.UUID

const val AUTHORITY_SHA="5ef95428421f7c693b010ac88f51d470804ab07a"
@Serializable data class Recipe(val version:Int=1,val authoritySha:String=AUTHORITY_SHA,val plan:NovelAiPromptPlan,
    val settings:NovelAiGenerationSettings,val account:AccountProfile,val request:JsonElement,
    val guidance:JsonElement=JsonNull)

/** Fixed synthetic transport supplies responses only. All authoritative prompts/requests/parsing run unchanged. */
class SyntheticDesignerTransport(private val instruction:String,private val revise:Boolean):NovelAiTextTransport {
    val exchanges=mutableListOf<String>()
    override fun streamText(stage:NovelAiTextStage,messages:List<ChatApiMessage>,modelConfig:ModelConfig)=flow {
        exchanges += documentJson.encodeToString(messages)
        val answer=if(stage==NovelAiTextStage.PLAN) buildJsonObject {
            put("sceneDescription","A ceramic cup on a quiet desk, soft window light.");put("queries",buildJsonArray {add("cup")})
        }.toString() else buildJsonObject {
            put("baseCaption", if(revise) "ceramic cup, quiet desk, warm evening light, ${instruction.take(200)}" else "ceramic cup, quiet desk, soft window light, ${instruction.take(200)}")
            put("characters",buildJsonArray {})
        }.toString()
        answer.chunked(12).forEach {delay(8);emit(StreamEvent.Delta(it))};emit(StreamEvent.Done)
        exchanges += answer
    }
}

class GenerationEngine(private val workspace:StudioWorkspace) {
    suspend fun design(instruction:String,account:AccountProfile,previous:NovelAiPromptPlan?=null,onText:(String)->Unit={}):NovelAiPromptPlan {
        account.validate();require(account.service=="LLM")
        val transport=SyntheticDesignerTransport(instruction,previous!=null)
        val lookup=object:NovelAiTagSearchClient { override suspend fun search(query:String)=NovelAiTagSearchOutcome(query,listOf(NovelAiTagCandidate("cup","杯子",20,NovelAiTagCategory.GENERAL))) }
        val designer=NovelAiPromptDesigner(transport,NovelAiTagResearchService(LlmNovelAiTagSearchPlanner(transport),lookup))
        val model=ModelConfig(account.id,account.alias,account.endpoint,"",account.model,createdAt=1)
        val result=if(previous==null) designer.designForPromptToolDetailed(instruction,"",model=model,onContentDelta=onText).plan
            else designer.reviseForPromptTool(previous,instruction,"",initialResearch=NovelAiDesignResearchSnapshot(),model=model,onContentDelta=onText)
        workspace.operation {workspace.save(workspace.state.copy(designs=workspace.state.designs+DesignRecord(instruction=instruction,previousPlan=previous?.let {documentJson.encodeToString(it)},resultPlan=documentJson.encodeToString(result),account=account,exchanges=transport.exchanges.toList())))}
        return result
    }
    suspend fun generate(plan:NovelAiPromptPlan,settings:NovelAiGenerationSettings,account:AccountProfile,
        scenario:String="SUCCESS",onIntermediate:(ByteArray)->Unit={}):JobRecord {
        account.validate();require(account.service=="NAI");require(scenario in setOf("SUCCESS","HTTP_429","DISCONNECT"))
        val image=EditorRenderer.synthetic(settings.imageSize().width,settings.imageSize().height)
        var requestBody=""
        val client=OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).addInterceptor { chain ->
            // No chain.proceed(): no network or socket exists in synthetic execution.
            val buffer=okio.Buffer();chain.request().body!!.writeTo(buffer);requestBody=buffer.readUtf8()
            if(scenario=="DISCONNECT")throw java.io.IOException("Synthetic disconnect")
            val payload=if(scenario=="HTTP_429")byteArrayOf() else frame("intermediate",image)+(1..settings.count).fold(byteArrayOf()){a,_->a+frame("final",image)}
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(if(scenario=="HTTP_429")429 else 200).message("Synthetic").body(payload.toResponseBody()).build()
        }.build()
        val service=NovelAiImageService(client,safeErrorsOnly=true)
        val wire=service.buildRequestBody(plan,settings.imageSize(),settings)
        val recipe=documentJson.encodeToString(Recipe(plan=plan,settings=settings,account=account,request=Json.parseToJsonElement(wire)))
        var job=JobRecord(account=account,recipe=recipe,state="RUNNING")
        workspace.operation {workspace.save(workspace.state.copy(jobs=workspace.state.jobs+job))}
        try {
            service.generate("SYNTHETIC_NOT_A_CREDENTIAL",plan,settings.imageSize(),settings,maxRateLimitRetries=0).collect {event ->
                when(event) {
                    is NovelAiImageEvent.Intermediate -> onIntermediate(event.image)
                    is NovelAiImageEvent.Error -> throw java.io.IOException(event.message)
                    is NovelAiImageEvent.Final -> {
                        check(Json.parseToJsonElement(requestBody)==Json.parseToJsonElement(wire)) {"Recorded request differs from transport"}
                        val id=workspace.importImage("Synthetic-${job.id}.png",event.image,recipe)
                        job=job.copy(results=job.results+id)
                        updateJob(job)
                    }
                }
            }
            require(job.results.size==settings.count) {"Incomplete image batch retained"}
            job=job.copy(state="COMPLETED")
        } catch(e:CancellationException) {job=job.copy(state="CANCELLED",detail="Cancelled; received originals retained");throw e}
        catch(e:Exception) {job=job.copy(state="FAILED",detail="Synthetic transport failed; no automatic retry")}
        finally {updateJob(job);client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
        return job
    }
    private fun updateJob(job:JobRecord)=workspace.operation {workspace.save(workspace.state.copy(jobs=workspace.state.jobs.map {if(it.id==job.id)job else it}))}
    private fun frame(kind:String,image:ByteArray):ByteArray {
        val p=MessagePack.newDefaultBufferPacker()
        p.packMapHeader(if(kind=="intermediate")3 else 2);p.packString("event_type");p.packString(kind)
        p.packString("image");p.packBinaryHeader(image.size);p.writePayload(image)
        if(kind=="intermediate"){p.packString("step_ix");p.packInt(14)}
        p.close();val bytes=p.toByteArray();return ByteBuffer.allocate(bytes.size+4).putInt(bytes.size).put(bytes).array()
    }
}

fun compareRecipes(left:String,right:String):List<String> {
    fun flatten(element:JsonElement,path:String,out:MutableMap<String,String>) {
        when(element) {
            is JsonObject -> {if(element.isEmpty())out[path]="{}";element.forEach {(k,v)->flatten(v,"$path.$k",out)}}
            is JsonArray -> {out["$path.length"]=element.size.toString();element.forEachIndexed {i,v->flatten(v,"$path[$i]",out)}}
            else -> out[path]=element.toString()
        }
    }
    val a=sortedMapOf<String,String>();val b=sortedMapOf<String,String>()
    flatten(Json.parseToJsonElement(left),"recipe",a);flatten(Json.parseToJsonElement(right),"recipe",b)
    return (a.keys+b.keys).sorted().distinct().filter {a[it]!=b[it]}.map {"$it\n  A: ${a[it]?:"<missing>"}\n  B: ${b[it]?:"<missing>"}"}
}
