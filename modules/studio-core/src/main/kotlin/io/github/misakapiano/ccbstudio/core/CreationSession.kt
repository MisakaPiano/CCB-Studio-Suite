package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.data.local.entity.ModelConfig
import com.example.chatbar.domain.image.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient

/** One creation-state owner used by the normal GUI and integration tests. No alternate demo path. */
class CreationSession(private val workspace: StudioWorkspace) {
    init { require(workspace.studioMode) }
    fun update(operation:DesignLineageOperation=DesignLineageOperation.PRESERVE,change: (CreationState) -> CreationState) = workspace.operation {
        DesignProvenance(workspace).update(change(requireNotNull(workspace.state.creation)),operation)
    }
    fun newConversation(): String {
        val c = NovelAiDesignConversation()
        update { it.copy(conversations=it.conversations+c,currentConversationId=c.id,composer="") }; return c.id
    }
    fun editDraft(change: (NovelAiStudioDraft) -> NovelAiStudioDraft) = update(DesignLineageOperation.EDIT) { it.change(change(it.draft)) }
    fun undo(redo:Boolean=false)=update(if(redo)DesignLineageOperation.REDO else DesignLineageOperation.UNDO){if(redo)it.redoDraft()else it.undoDraft()}
    fun applyReply(conversationId: String, turnId: String) = workspace.operation {
        val state=workspace.state.creation!!;val conversation=state.conversations.single{it.id==conversationId}
        val reply = requireNotNull(conversation.turns.single {it.id==turnId}.reply)
        val applied=state.draft.applyDesignedPromptPlan(reply.plan, reply.targetImageModel)
        val provenance=DesignProvenance(workspace);val reference=provenance.retain(conversation,turnId,state.draft,applied)
        provenance.update(state.change(applied),DesignLineageOperation.APPLY,reference)
    }
    /** Edit creates a new branch. The old completed history and its descendants remain intact. */
    fun branch(conversationId: String, turnId: String, replacement: String): String {
        val id = newId()
        update { state ->
            val c = state.conversations.single {it.id == conversationId}
            val index = c.turns.indexOfFirst {it.id == turnId}; require(index >= 0)
            require(c.turns.none {it.status == NovelAiDesignTurnStatus.PENDING})
            val fork = c.copy(id=id,title=c.title+" · 分支",turns=c.turns.take(index),updatedAt=System.currentTimeMillis())
            state.copy(conversations=state.conversations+fork,currentConversationId=id,composer=replacement)
        }; return id
    }
    suspend fun design(account: AccountProfile, designer: NovelAiPromptDesigner,
        onContent: (String)->Unit={}, onReasoning: (String)->Unit={}, onSubmitted:()->Unit={}): NovelAiDesignReply {
        account.validate(); require(account.service == "LLM" && account.provider != "SYNTHETIC")
        workspace.beginTask().use {
            lateinit var conversation: NovelAiDesignConversation; lateinit var turn: NovelAiDesignTurn
            update { state ->
                require(state.composer.isNotBlank()) { "请输入设计要求" }
                val existing = state.conversations.firstOrNull {it.id == state.currentConversationId} ?: NovelAiDesignConversation()
                require(!existing.hasBlockingTurn) { "上一轮未完成，请保留记录并新建或编辑分支" }
                val target=if(state.draft.aiDesignNaturalLanguageMode) NovelAiImageModel.V5_FULL else state.draft.selectedModel
                if(state.attachCurrentPrompt) require(state.draft.basePrompt.isNotBlank() && state.draft.activeCharacters.all {it.prompt.isNotBlank()} && state.draft.activeCharacters.size<=target.maxCharacters)
                turn = NovelAiDesignTurn(userText=state.composer,designModelId=account.id,targetImageModel=target,
                    naturalLanguageMode=state.draft.aiDesignNaturalLanguageMode,
                    attachedStudioPrompt=if(state.attachCurrentPrompt) NovelAiPositivePromptSnapshot(state.draft.basePrompt,
                        state.draft.activeCharacters.map {it.prompt}) else null)
                conversation = existing.copy(title=if(existing.turns.isEmpty()) NovelAiDesignConversation.titleFrom(turn.userText) else existing.title,
                    designContext=if(existing.turns.isEmpty()) NovelAiDesignContextSnapshot(characterImagePrompts=state.draft.importedCharacterPromptSources,finalPromptRequirement=state.draft.extraRequirement) else existing.designContext,
                    turns=existing.turns+turn,updatedAt=System.currentTimeMillis())
                state.copy(conversations=state.conversations.filterNot {it.id==conversation.id}+conversation,
                    currentConversationId=conversation.id,composer="",attachCurrentPrompt=false,llmAccountId=account.id)
            }
            try {
                onSubmitted()
                // Mirrors pinned NovelAiDesignTurnRunner's orchestration; repository is the Suite journal.
                val index=conversation.turns.lastIndex; val baseline=conversation.revisionBaselineFor(index)
                val context=conversation.designContext
                val model=account.modelConfig()
                val result=if(baseline==null) designer.designForPromptToolDetailed(
                    imageDescription=turn.userText,characterPrompt=context.characterPrompt,
                    characterImagePrompts=context.characterImagePrompts.map {it.name to it.prompt},
                    finalPromptRequirement=context.finalPromptRequirement,model=model,targetImageModel=turn.targetImageModel,
                    naturalLanguageMode=turn.naturalLanguageMode,onContentDelta=onContent,onReasoningDelta=onReasoning)
                else NovelAiPromptToolDesignResult(designer.reviseForPromptTool(previousPlan=baseline,modificationRequest=turn.userText,
                    characterPrompt=context.characterPrompt,characterImagePrompts=context.characterImagePrompts.map {it.name to it.prompt},
                    initialResearch=conversation.revisionResearchFor(index),finalPromptRequirement=context.finalPromptRequirement,
                    model=model,targetImageModel=turn.targetImageModel,naturalLanguageMode=turn.naturalLanguageMode,
                    onContentDelta=onContent,onReasoningDelta=onReasoning),conversation.revisionResearchFor(index))
                val reply=NovelAiDesignReply(result.plan,turn.targetImageModel,account.id,turn.naturalLanguageMode)
                update { state -> state.copy(conversations=state.conversations.map {c -> if(c.id != conversation.id)c else
                    c.copy(turns=c.turns.map {t -> if(t.id!=turn.id)t else t.copy(reply=reply,status=NovelAiDesignTurnStatus.COMPLETED,updatedAt=System.currentTimeMillis())},
                        initialResearch=if(baseline==null) result.research else c.initialResearch,updatedAt=System.currentTimeMillis())}) }
                return reply // Deliberately not applied to draft; the user reviews a candidate first.
            } catch(e: Exception) {
                withContext(NonCancellable) { update { state -> state.copy(conversations=state.conversations.map {c -> if(c.id!=conversation.id)c else
                    c.copy(turns=c.turns.map {t -> if(t.id!=turn.id)t else t.copy(status=if(e is CancellationException) NovelAiDesignTurnStatus.CANCELLED else NovelAiDesignTurnStatus.FAILED,
                        error=if(e is CancellationException) "已取消；未自动重发" else "设计未完成；请检查账户或响应后显式重试",updatedAt=System.currentTimeMillis())})}) } }
                throw e
            }
        }
    }
}

@Serializable data class GenerationReceipt(val version:Int=3,val authoritySha:String=AUTHORITY_SHA,
    val recipe:NovelAiGenerationRecipe,val account:AccountProfile,val request:JsonElement,
    val wireSha256:String="",val vibePayloads:Map<String,JsonElement> = emptyMap())

class StudioGenerator(private val workspace:StudioWorkspace,private val vault:SecretVault,
    private val admission:RequestAdmission,private val client:OkHttpClient=studioHttpClient()) {
    suspend fun generate(account:AccountProfile,draft:NovelAiStudioDraft,
        guidance:NovelAiPreparedImageGuidance=NovelAiPreparedImageGuidance.NONE,
        requestSize:NovelAiImageSize=draft.activeSettings.imageSize(),
        compose:((ByteArray)->ByteArray)?=null,
        onIntermediate:(ByteArray)->Unit={},onResult:(String)->Unit={},designApplication:DesignApplicationRef?=null):JobRecord {
        require(workspace.studioMode);account.validate();require(account.service=="NAI" && account.provider=="NOVELAI")
        require(guidance.action==draft.imageGuidance.action) {"Guidance must be prepared before generation"}
        require(draft.imageGuidance.effectiveReferenceMode(draft.selectedModel)==NovelAiReferenceMode.NONE || guidance!=NovelAiPreparedImageGuidance.NONE)
        val initial=draft.activeSettings;require(initial.validationError(draft.activeCharacters.size)==null)
        val settings=if(initial.seedMode==NovelAiSeedMode.RANDOM) initial.copy(seed=kotlin.random.Random.nextLong(initial.maxAllowedBaseSeed+1)) else initial
        val plan=draft.toPromptPlan(); val service=NovelAiImageService(client,safeErrorsOnly=true)
        val wire=service.buildRequestBody(plan,requestSize,settings,guidance)
        workspace.beginTask().use {
            val retained=RequestEvidence(workspace,draft.imageGuidance.baseImage?.id ?: draft.imageGuidance.preciseReference.asset?.id ?: draft.imageGuidance.vibes.firstOrNull()?.asset?.id)
            val savedRequest=retained.retainRequest(Json.parseToJsonElement(wire))
            val savedVibes=draft.imageGuidance.vibes.filter{it.encodedVibe!=null}.associate{it.id to retained.retainText(it.encodedVibe!!)}
            val snapshot=draft.toRecipe(settings).let{it.copy(imageGuidance=it.imageGuidance.copy(vibes=it.imageGuidance.vibes.map{v->v.copy(encodedVibe=null)}))}
            val recipe=documentJson.encodeToString(GenerationReceipt(recipe=snapshot,account=account,request=savedRequest,wireSha256=sha256(wire.toByteArray()),vibePayloads=savedVibes))
            require(recipe.toByteArray().size<=1024*1024){"Receipt exceeds descriptor budget; request not sent"}
            var job=JobRecord(account=account,recipe=recipe,state="QUEUED")
            designApplication?.let{DesignProvenance(workspace).linkGeneration(job,it)}
            workspace.operation {workspace.save(workspace.state.copy(jobs=workspace.state.jobs+job))}
            fun save()=workspace.operation {workspace.save(workspace.state.copy(jobs=workspace.state.jobs.map {if(it.id==job.id)job else it}))}
            try {
                val token=withContext(Dispatchers.IO){vault.use(requireNotNull(account.secretRef)){it.decodeToString()}}
                admission.await("NAI")
                currentCoroutineContext().ensureActive();job=job.copy(state="RUNNING");save()
                service.generate(token,plan,requestSize,settings,guidance,maxRateLimitRetries=0).collect { event ->
                    when(event) {
                        is NovelAiImageEvent.Intermediate -> onIntermediate(event.image)
                        is NovelAiImageEvent.Error -> throw DataProblem("PROVIDER", "生成未完成；已收到的原件保留，不自动重发")
                        is NovelAiImageEvent.Final -> {
                            val id=workspace.importImage("NovelAI-${job.id}-${job.results.size+1}.png",event.image,recipe)
                            job=job.copy(results=job.results+id);save()
                            val visible=if(compose==null)id else {
                                val composed=compose(event.image)
                                if(composed.contentEquals(event.image))id else workspace.importDerived("Inpaint-${job.id}-${job.results.size}.png",composed,id,recipe).also {derived->job=job.copy(composedResults=job.composedResults+derived);save()}
                            }
                            onResult(visible)
                        }
                    }
                }
                require(job.results.size==settings.count) { "Incomplete image batch retained" }
                job=job.copy(state="COMPLETED");save();return job
            } catch(e:Exception) {
                job=job.copy(state=if(e is CancellationException) "CANCELLED" else "FAILED",
                    detail="未自动重发；已收到的原件保留，可从图库查找")
                withContext(NonCancellable){save()};throw e
            }
        }
    }
}
