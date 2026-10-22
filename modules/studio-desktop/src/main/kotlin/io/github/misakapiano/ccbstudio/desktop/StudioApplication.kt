package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.*

internal class WorkbenchController(val workspace:StudioWorkspace,val scope:CoroutineScope,val infrastructure:DesktopNovelAiInfrastructure,budgetRoot:Path,
    val http:okhttp3.OkHttpClient=studioHttpClient(),admissionOverride:RequestAdmission?=null,val pauseCollectionsOnStart:Boolean=false) {
    val session=CreationSession(workspace)
    val runtimeRoot:Path=budgetRoot.parent
    val backups by lazy{LibraryBackupController(this)}
    val gallery = GalleryViewState()
    val library by lazy{LibraryController(this)}
    val updater=StudioUpdateController(scope,budgetRoot.parent.resolve("updates"))
    var maintenance by mutableStateOf(false);private set
    var installUpdate:(()->Unit)?=null
    var requestClose:(()->Unit)?=null
    val animation=GalleryAnimationController(this,budgetRoot.parent.resolve("apng-exports"))
    val editor by lazy {TypesetEditorController(this)}
    val vault=SecretVault(workspace.root.resolve("secrets"))
    val modelAccounts by lazy{ModelAccountsController(this)}
    val naiSettings by lazy{NaiSettingsController(this)}
    var state by mutableStateOf(workspace.state);private set
    var draft by mutableStateOf(state.creation!!.draft);private set
    var composer by mutableStateOf(state.creation!!.composer);private set
    var attach by mutableStateOf(state.creation!!.attachCurrentPrompt);private set
    var llmId by mutableStateOf(state.creation!!.llmAccountId);private set
    var naiId by mutableStateOf(state.creation!!.naiAccountId);private set
    var view by mutableStateOf("创作")
    var section by mutableStateOf("Prompt")
    var message by mutableStateOf("草稿已读取")
    var problem by mutableStateOf<String?>(null)
    internal var operationNotice:String?=null
    var busy by mutableStateOf(false);private set
    var saved by mutableStateOf(true);private set
    var stream by mutableStateOf("");var reasoning by mutableStateOf("")
    var intermediate by mutableStateOf<ByteArray?>(null);private set
    var usage by mutableStateOf<NovelAiAccountUsage?>(null);private set
    var usageAt by mutableStateOf<Long?>(null);private set
    var current by mutableStateOf(state.jobs.lastOrNull()?.let{it.composedResults.lastOrNull()?:it.results.lastOrNull()})
    var selection by mutableStateOf(setOf<String>())
    var documentAssets by mutableStateOf(emptyList<AssetStore.Asset>());private set
    var assets by mutableStateOf(visibleAssets());private set
    fun refreshLibraryAssets(){assets=visibleAssets()}
    val picker=DesktopFilePicker()
    var active:Job?=null;private set
    var activeOperation by mutableStateOf("");private set
    val cancelLabel get()=when(activeOperation){"AI 设计"->"停止 AI 设计";"NovelAI 生成"->"停止生图";"刷新账户额度"->"取消刷新额度";else->"停止${activeOperation.ifBlank{"当前任务"}}"}
    var designDetached by mutableStateOf(false);private set
    fun detachDesign(){if(!maintenance){designDetached=true;view="创作"}}
    fun reattachDesign(){designDetached=false;view="AI 设计"}
    fun dismissDesign(){designDetached=false}
    private var pending:Job?=null
    private val admission=admissionOverride?:RequestAdmission{}
    private val guidanceRoot=budgetRoot.parent.resolve("guidance")
    private val metadataRoot=budgetRoot.parent.resolve("metadata-snapshots")
    private val modelRoot=budgetRoot.parent.resolve("model-snapshots")
    private fun guidanceFor(account:AccountProfile?)=DesktopNovelAiGuidance(guidanceRoot,::readAsset,{
        vault.use(requireNotNull(account?.secretRef)){it.decodeToString()}
    },admission,http)
    fun visibleAssets():List<AssetStore.Asset> {
        val catalog=workspace.catalog()
        documentAssets=catalog.filter{it.request().project().decodeToString()==SharedAssets.DOCUMENT}
        return SharedAssets.images(catalog)
    }
    fun readAsset(path:String):ByteArray {require(path.startsWith("asset:"));return workspace.bytes(path.removePrefix("asset:"))}
    fun assetRef(id:String):NovelAiStudioAssetRef {
        val bytes=workspace.bytes(id);val image=DesktopImageEditing.decode(bytes)
        return try{NovelAiStudioAssetRef(id=id,path="asset:$id",sha256=sha256(bytes),width=image.width,height=image.height)}finally{image.flush()}
    }
    fun useImage(id:String,target:NovelAiImageUseTarget)=run("应用图像引导") {
        val bytes=workspace.bytes(id);DesktopImageEditing.requireStatic(bytes)
        val raster=DesktopImageEditing.decode(bytes)
        val normalized=try{DesktopImageEditing.png(raster)}finally{raster.flush()}
        val normalizedId=if(normalized.contentEquals(bytes))id else workspace.importDerived("Guidance-source.png",normalized,id)
        val ref=assetRef(normalizedId)
        session.editDraft {d->d.copy(imageGuidance=when(target){
            NovelAiImageUseTarget.IMAGE_TO_IMAGE->d.imageGuidance.copy(action=NovelAiGenerationAction.IMAGE_TO_IMAGE,baseImage=ref,maskImage=null,focusedInpaintRegion=null)
            NovelAiImageUseTarget.INPAINT->d.imageGuidance.copy(action=NovelAiGenerationAction.INPAINT,baseImage=ref,maskImage=null,focusedInpaintRegion=NovelAiFocusedInpaintRegion(0f,0f,1f,1f))
            NovelAiImageUseTarget.PRECISE_REFERENCE->d.imageGuidance.copy(referenceMode=NovelAiReferenceMode.PRECISE,preciseReference=d.imageGuidance.preciseReference.copy(asset=ref))
            NovelAiImageUseTarget.VIBE_REFERENCE->{require(d.imageGuidance.vibes.size<NovelAiImageGuidanceDraft.MAX_VIBES);d.imageGuidance.copy(referenceMode=NovelAiReferenceMode.VIBE,vibes=d.imageGuidance.vibes+NovelAiVibeReferenceDraft(asset=ref))}
        })}
        draft=workspace.state.creation!!.draft;section="图像引导";view="创作"
    }
    suspend fun applyGuidanceTools(parent:String,bytes:ByteArray,mask:ByteArray?,region:NovelAiFocusedInpaintRegion):Boolean {
        if(busy)return false
        busy=true
        try {pending?.cancelAndJoin();flush()
        workspace.beginTask().use {withContext(Dispatchers.IO){
            val base=if(bytes.contentEquals(workspace.bytes(parent)))parent else workspace.importDerived("Guidance-edit.png",bytes,parent)
            val baseRef=assetRef(base)
            val maskRef=mask?.let{assetRef(workspace.importDerived("Guidance-mask.png",it,base))}
            val next=draft.imageGuidance.copy(baseImage=baseRef,maskImage=maskRef,focusedInpaintRegion=region)
            next.validationError(draft.selectedModel)?.let{throw DataProblem("GUIDANCE",it)}
            session.editDraft{it.copy(imageGuidance=next)}
        }};draft=workspace.state.creation!!.draft;state=workspace.state;return true
        }finally{busy=false}
    }
    suspend fun saveToolCopy(parent:String,bytes:ByteArray) {
        check(!busy);busy=true
        try {workspace.beginTask().use {withContext(Dispatchers.IO){
            val id=workspace.importDerived("Edited-copy.png",bytes,parent)
            workspace.operation{workspace.save(workspace.state.copy(savedCopies=workspace.state.savedCopies+id))};current=id
        }};state=workspace.state;assets=visibleAssets();message="已保存工具副本；原件保留"
        }finally{busy=false}
    }
    fun vibeMisses()=guidanceFor(null).vibeCacheMisses(draft)
    fun <T> metadataFile(id:String,read:(Path)->T):T {
        Files.createDirectories(metadataRoot)
        val path=metadataRoot.resolve("${newId()}.png")
        writeNewForced(path,workspace.bytes(id))
        try{return read(path)}finally{Files.deleteIfExists(path)}
    }
    fun postProcess(parent:String,bytes:ByteArray,tab:NovelAiPostProcessTab,source:NovelAiEnhanceSource?,options:NovelAiEnhanceOptions,
        onComplete:(ByteArray)->Unit,onStatus:(String)->Unit)=run(tab.label) {
        val account=state.accounts.single{it.id==naiId && it.service=="NAI"}
        workspace.beginTask().use {
            var job=JobRecord(account=account,recipe="",state="QUEUED",detail="POSTPROCESS:${tab.name}")
            fun save()=workspace.operation{workspace.save(workspace.state.copy(jobs=workspace.state.jobs.filterNot{it.id==job.id}+job))}
            save()
            val captured=http.newBuilder().addInterceptor{chain->
                val body=okio.Buffer().also{chain.request().body!!.writeTo(it)}.readUtf8()
                val retained=RequestEvidence(workspace,parent).retainRequest(Json.parseToJsonElement(body))
                val receipt=buildJsonObject{put("version",3);put("kind",tab.name);put("parentAsset",parent);put("authoritySha",AUTHORITY_SHA);put("wireSha256",sha256(body.toByteArray()));put("request",retained);put("account",documentJson.encodeToJsonElement(account))}.toString()
                require(receipt.toByteArray().size<=1024*1024)
                job=job.copy(state="RUNNING",recipe=receipt);save();chain.proceed(chain.request())
            }.build()
            try {
                val processor=DesktopNovelAiPostProcessor({vault.use(requireNotNull(account.secretRef)){it.decodeToString()}},admission,captured){final->
                    val id=workspace.importImage("${tab.name}-${job.id}.png",final,job.recipe)
                    job=job.copy(results=job.results+id);save();current=id;assets=visibleAssets()
                }
                val output=processor.process(bytes,tab,source,options,usage,onStatus)
                job=job.copy(state="COMPLETED");save();onComplete(output)
            } catch(e:Exception) {
                job=job.copy(state=if(e is CancellationException)"CANCELLED"else"FAILED",detail="${tab.name} 未完成；已提交原件保留；未自动重发")
                withContext(NonCancellable){save()};throw e
            }
        }
    }
    fun reverseImage(id:String)=run("图片反推") {
        val account=state.accounts.single{it.id==llmId && it.service=="LLM"};val launch=draft
        val bytes=workspace.bytes(id);DesktopImageEditing.requireStatic(bytes);DesktopImageEditing.decode(bytes).flush()
        workspace.beginTask().use {
            var job=JobRecord(account=account,recipe=buildJsonObject{put("kind","REVERSE");put("sourceAsset",id);put("targetModel",launch.selectedModel.apiId)}.toString(),state="RUNNING")
            fun save()=workspace.operation{workspace.save(workspace.state.copy(jobs=workspace.state.jobs.filterNot{it.id==job.id}+job))}
            save();stream="";reasoning="";session.update{it.copy(reverseCandidate=null)};state=workspace.state
            try {
                val understanding=studioVision(modelRoot.resolve(newId()),state.accounts,vault,admission,http)
                val designer=infrastructure.designer(StudioTextTransport(account,vault,admission,http),understanding)
                val plan=designer.designForPromptTool(imageDescription="",characterPrompt="",finalPromptRequirement=launch.extraRequirement,
                    imageBase64s=listOf(java.util.Base64.getEncoder().encodeToString(bytes)),model=account.modelConfig(),targetImageModel=launch.selectedModel,
                    referenceImageInstruction=com.example.chatbar.domain.prompt.NovelAiPromptAuthority.novelAiImageReversePromptUser(launch.selectedModel.displayName),
                    excludeStyle=false,onContentDelta={stream=it},onReasoningDelta={reasoning=it})
                session.update{it.copy(reverseCandidate=ReverseCandidate(id,account.id,launch.selectedModel,plan))}
                job=job.copy(state="COMPLETED");save()
            }catch(e:Exception){job=job.copy(state=if(e is CancellationException)"CANCELLED"else"FAILED");withContext(NonCancellable){save()};throw e}
        }
    }
    fun applyReverse()=run("应用反推候选") {
        session.editDraft{it.applyReversePromptPlan(requireNotNull(workspace.state.creation!!.reverseCandidate).plan)}
        draft=workspace.state.creation!!.draft;section="Prompt";view="创作"
    }
    fun importMetadata(id:String,selection:NovelAiStudioMetadataSelection)=run("导入元数据") {
        workspace.beginTask().use {
            val metadata=metadataFile(id){DesktopImageMetadata.readStudio(it.toString())} ?: throw DataProblem("METADATA_UNAVAILABLE","无可导入的元数据")
            var next=draft.applyImportedMetadata(metadata,selection)
            if(selection.imageGuidance && metadata.imageGuidance.hasAny) {
                fun materialize(encoded:String?):NovelAiStudioAssetRef?=encoded?.let {
                    require(it.length<=DesktopImageEditing.MAX_BYTES*2)
                    val bytes=java.util.Base64.getDecoder().decode(it)
                    DesktopImageEditing.requireStatic(bytes)
                    val raster=DesktopImageEditing.decode(bytes)
                    val normalized=try{DesktopImageEditing.png(raster)}finally{raster.flush()}
                    // Preserve embedded source bytes as well as its normalized consumer representation.
                    val source=workspace.importDerived("Metadata-source.png",bytes,id)
                    assetRef(if(bytes.contentEquals(normalized))source else workspace.importDerived("Metadata-guidance.png",normalized,source))
                }
                val g=metadata.imageGuidance
                next=next.copy(imageGuidance=next.imageGuidance.copy(baseImage=materialize(g.baseImageBase64),maskImage=materialize(g.maskBase64),
                    preciseReference=next.imageGuidance.preciseReference.copy(asset=materialize(g.preciseImageBase64)),focusedInpaintRegion=null))
            }
            session.editDraft{next};draft=workspace.state.creation!!.draft;view="创作";section="Prompt"
        }
    }
    fun reuseHistory(id:String,mode:NovelAiHistoryApplyMode,confirmed:Boolean=false)=run("复用历史参数") {
        workspace.beginTask().use {
            val history=historyRecipe(id) ?: throw DataProblem("MISSING_RECIPE","没有可复用的生成记录")
            val recipe=history.first
            require(mode!=NovelAiHistoryApplyMode.FULL || !recipe.imageGuidance.hasMissingHistorySource()) {"缺少原始引导，不能完整复现"}
            require(confirmed || !recipe.requiresImageGuidanceReuseWarning(mode)) {"需确认缺失引导警告"}
            session.editDraft{it.applyHistoryRecipe(recipe,history.second,mode)};draft=workspace.state.creation!!.draft;view="创作"
        }
    }
    fun applyRecipeFields(id:String,fields:Set<RecipeDraftField>,expected:NovelAiStudioDraft)=run("应用所选 Recipe 字段") {
        workspace.beginTask().use {
            val history=requireNotNull(historyRecipe(id)){"生成记录不存在"}
            val asset=workspace.assets.read(java.util.UUID.fromString(id)).require()
            session.update {old->
                require(old.draft==expected){"草稿已改变，请重新预览字段差异"}
                val next=RecipeDraftSelection.apply(old.draft,history.first,history.second,fields)
                val record=RecipeApplicationRecord(id,asset.sha256(),fields.sortedBy{it.ordinal},RecipeDraftSelection.fingerprint(old.draft),RecipeDraftSelection.fingerprint(next))
                old.change(next).copy(recipeApplications=(old.recipeApplications+record).takeLast(100))
            }
            draft=workspace.state.creation!!.draft;view="创作"
        }
    }
    fun historyRecipe(id:String):Pair<NovelAiGenerationRecipe,Long>? {
        val origin=imageOrigin(workspace,id)
        val receipt=origin.receipt?:return null
        return receipt.recipeForReuse(workspace) to requireNotNull(origin.seed)
    }
    init {if(current==null)current=assets.lastOrNull()?.request()?.id()?.toString()}
    private fun changed(){saved=false;pending?.cancel();pending=scope.launch {delay(350);try{flush()}catch(e:CancellationException){throw e}catch(e:Exception){problem="草稿未能保存，保留当前输入；请先处理数据错误"}}}
    fun edit(change:(NovelAiStudioDraft)->NovelAiStudioDraft){if(maintenance)return;draft=state.creation!!.resolvedDraft(change(draft));changed()}
    fun selectImageCard(card:ImageCardSource)=run("应用角色卡图像字段") {
        session.update{old->
            val cards=old.imageCards.filterNot{it.id==card.id}+card
            val next=old.copy(imageCards=cards)
            next.change(next.resolvedDraft(old.draft.importCharacterCardPromptSources(card.id,card.stylePrompt,card.negativePrompt,card.characters)))
        };draft=workspace.state.creation!!.draft
    }
    fun defaults(model:NovelAiImageModel?=null,translated:Boolean?=null)=run("保存创作偏好") {
        session.update{old->val next=old.copy(defaultImageModel=model?:old.defaultImageModel,translatedTags=translated?:old.translatedTags);next.copy(draft=next.resolvedDraft(next.draft))}
        draft=workspace.state.creation!!.draft
    }
    fun clearPromptDraft()=edit{it.clearPrompts(state.creation!!.imageCards.firstOrNull{card->card.id==it.importedCharacterCardId}?.negativePrompt)}
    fun pastePrompt(text:String,origin:String="clipboard-or-asset")=run("粘贴正向 Prompt") {
        require(text.length<=100000)
        library.repository.savePrompt(text,"粘贴的 Prompt",origin);library.refresh()
        session.editDraft{NovelAiStudioPromptClipboard.apply(text,it)};draft=workspace.state.creation!!.draft
    }
    fun input(value:String){if(maintenance)return;composer=value;changed()}
    fun attach(value:Boolean){if(maintenance)return;attach=value;changed()}
    fun chooseAccount(service:String,id:String){if(maintenance)return;if(service=="LLM")llmId=id else {naiId=id;usage=null;usageAt=null};changed()}
    suspend fun flush() {
        val d=draft;val input=composer;val attached=attach;val l=llmId;val n=naiId
        withContext(Dispatchers.IO){session.update(DesignLineageOperation.EDIT) {old -> (if(old.draft==d)old else old.change(d)).copy(composer=input,attachCurrentPrompt=attached,llmAccountId=l,naiAccountId=n)}}
        state=withContext(Dispatchers.IO){workspace.state};saved=draft==d && composer==input && attach==attached && llmId==l && naiId==n
    }
    fun run(label:String,body:suspend ()->Unit) {
        if(busy||maintenance)return
        busy=true;activeOperation=label;message=label;problem=null;operationNotice=null
        active=scope.launch(start=CoroutineStart.UNDISPATCHED) {
            try {pending?.cancelAndJoin();flush();withContext(Dispatchers.IO){body()};message=operationNotice?:if(problem==null)"$label · 已完成" else "$label · 有未完成项，请查看回执"}
            catch(e:CancellationException){message="已停止；已提交结果保留";throw e}
            catch(e:Exception){problem="${if(e is DataProblem)e.status else e.javaClass.simpleName}：操作未完成，未自动重发。已有资料保留。";message="操作未完成"}
            finally {try{withContext(NonCancellable+Dispatchers.IO){state=workspace.state;assets=visibleAssets()};modelAccounts.syncSavedProfile()}catch(e:Exception){problem="工作区需要恢复，已保留现有文件；请停止写入并检查"};activeOperation="";busy=false}
        }
    }
    fun design()=run("AI 设计") {
        val account=state.accounts.single{it.id==llmId && it.service=="LLM"}
        stream="";reasoning=""
        val transport=StudioTextTransport(account,vault,admission,http)
        // Clear only the submitted composer. New edits made during the request remain in the UI.
        val submitted=composer
        session.design(account,infrastructure.designer(transport),{stream=it},{reasoning=it},onSubmitted={
            state=workspace.state;if(composer==submitted){composer="";attach=false}
        })
    }
    fun newConversation()=run("新建设计对话"){session.newConversation();composer="";state=workspace.state}
    fun selectConversation(id:String)=run("读取设计历史"){session.update{it.copy(currentConversationId=id)};state=workspace.state}
    fun branch(c:String,t:String,text:String)=run("建立设计分支"){session.branch(c,t,text);composer=text;state=workspace.state}
    fun apply(c:String,t:String)=run("应用设计候选"){session.applyReply(c,t);draft=workspace.state.creation!!.draft;view="创作"}
    fun retry(c:String,t:String)=run("准备重试分支") {
        val turn=workspace.state.creation!!.conversations.single{it.id==c}.turns.single{it.id==t}
        session.branch(c,t,turn.userText);composer=turn.userText;state=workspace.state
        message="已保留原记录；检查后发送重试分支"
    }
    fun undo(redo:Boolean=false)=run(if(redo)"重做草稿" else "撤销草稿") {session.undo(redo);draft=workspace.state.creation!!.draft}
    fun generate()=run("NovelAI 生成") {
        val account=state.accounts.single{it.id==naiId && it.service=="NAI"}.copy(model=draft.selectedModel.apiId)
        require(draft.basePrompt.isNotBlank())
        val requested=draft
        val designApplication=DesignProvenance(workspace).current()
        val prepared=workspace.beginTask().use{guidanceFor(account).prepare(requested)}
        val launch=requested.copy(imageGuidance=prepared.retainedGuidance)
        val total=if(launch.continuousModeEnabled)launch.continuousTargetCount else launch.activeSettings.count
        intermediate=null
        var completed=0
        while(completed<total) {
            currentCoroutineContext().ensureActive()
            val batch=launch.withActiveSettings(launch.activeSettings.copy(count=minOf(launch.activeSettings.count,total-completed)))
            val result=StudioGenerator(workspace,vault,admission,http).generate(account,batch,prepared.guidance,prepared.requestSize,if(launch.imageGuidance.action==NovelAiGenerationAction.INPAINT)prepared.compose else null,onIntermediate={intermediate=it},onResult={id->
                intermediate=null;current=id;assets=visibleAssets();message="已保存 ${++completed}/$total 张"
            },designApplication=designApplication)
            check(result.state=="COMPLETED")
        }
    }
    fun refreshUsage()=run("刷新账户额度") {
        val account=state.accounts.single{it.id==naiId && it.service=="NAI"}
        workspace.beginTask().use {
            val token=vault.use(requireNotNull(account.secretRef)){it.decodeToString()}
            admission.await("NAI");val result=NovelAiAccountService(http).fetchCancellable(token)
            if(naiId==account.id){usage=result;usageAt=System.currentTimeMillis()}
        }
    }
    private fun activateVision(id:String?) {
        val primary=workspace.state.accounts.singleOrNull{it.id==llmId&&it.service=="LLM"}?:error("请先设置默认语言模型")
        val target=id?.let{ref->workspace.state.accounts.single{it.id==ref&&it.service=="LLM"&&it.isMultimodal}.also{requireChatModelPurpose(it.model)}}
        val updated=primary.copy(isMultimodal=target?.id==primary.id,visionAccountId=target?.id?.takeUnless{it==primary.id})
        workspace.save(workspace.state.copy(accounts=workspace.state.accounts.map{if(it.id==primary.id)updated else it}))
    }
    fun chooseVision(id:String?)=run("设置视觉模型"){workspace.operation{activateVision(id)}}
    fun saveAccount(existing:AccountProfile?,alias:String,service:String,endpoint:String,model:String,key:String,isMultimodal:Boolean=false,visionAccountId:String?=null,useGlobalKey:Boolean=false,selectAfterSave:Boolean=true,activateAsVision:Boolean=false,onSaved:(AccountProfile)->Unit={})=run("保存账户") {
        if(activateAsVision)require(workspace.state.accounts.any{it.id==llmId&&it.service=="LLM"}){"请先设置默认语言模型"}
        if(service=="LLM")requireChatModelPurpose(model)
        AccountProfile(id=existing?.id?:newId(),alias=alias.trim(),service=service,provider=if(service=="NAI")"NOVELAI" else "OPENAI_COMPATIBLE",endpoint=endpoint.trim().trimEnd('/'),model=model.trim(),isMultimodal=isMultimodal,visionAccountId=visionAccountId).validate()
        require(!useGlobalKey || service=="LLM")
        val ref=if(useGlobalKey) SecretVault.GLOBAL_LLM_REF else if(key.isNotBlank()) {val bytes=key.trim().toByteArray();try{vault.put(bytes)}finally{bytes.fill(0)}} else existing?.secretRef?.takeUnless{it==SecretVault.GLOBAL_LLM_REF}
        require(ref!=null){"请填写独立凭据，或选择使用共用默认 API Key"}
        val a=AccountProfile(id=existing?.id?:newId(),alias=alias.trim(),service=service,provider=if(service=="NAI")"NOVELAI" else "OPENAI_COMPATIBLE",endpoint=endpoint.trim().trimEnd('/'),model=model.trim(),secretRef=ref,isMultimodal=service=="LLM"&&isMultimodal,visionAccountId=visionAccountId.takeIf{service=="LLM"&&!isMultimodal})
        a.validate();workspace.operation{workspace.save(workspace.state.copy(accounts=workspace.state.accounts.filterNot{it.id==a.id}+a))}
        if(activateAsVision)workspace.operation{activateVision(a.id)}
        if(selectAfterSave) {
            if(service=="LLM")llmId=a.id else {naiId=a.id;usage=null;usageAt=null}
            session.update{it.copy(llmAccountId=llmId,naiAccountId=naiId)}
        }
        withContext(scope.coroutineContext.minusKey(Job)){onSaved(a)}
    }
    fun importImages() {
        val files=StudioFileDialogs.openMany("导入图片",listOf("png","jpg","jpeg","webp","gif"))
        if(files.isNotEmpty())importIngress(GalleryIngress(files=files))
    }
    fun pasteImages():Boolean=try{if(busy)false else gallery.pasteContents()?.let{receiveTransfer(it,true)}?:false}catch(e:Exception){problem="剪贴板读取失败；现有资料未改变";true}
    fun receiveTransfer(transfer:java.awt.datatransfer.Transferable,paste:Boolean):Boolean=try {
        if(busy)false else galleryTransfer(transfer,paste)?.let{importIngress(it);true}?:false
    }catch(e:Exception){problem="图片传入失败：${e.javaClass.simpleName}；现有资料保留";true}
    fun importIngress(input:GalleryIngress,expandDirectories:Boolean=false,maxImages:Int=1000,onAccepted:(List<String>)->Unit={})=run("导入图片") {
        require(input.files.size<=1000 && (input.files.isNotEmpty() xor (input.raster!=null)))
        val accepted=mutableListOf<String>()
        val files=if(expandDirectories)animationIngressFiles(input.files)else input.files
        require(files.size.coerceAtLeast(if(input.raster!=null)1 else 0)<=maxImages){"当前批次最多1000张，请先移除部分项目"}
        if(files.isEmpty()&&input.raster==null){problem="所选目录本层没有支持的图片，未添加；不包含子目录";return@run}
        val entries=files.map{it.fileName.toString() to it}.ifEmpty{listOf("Clipboard-raster.png" to null)}
        gallery.imports=entries.map{GalleryImportResult(it.first,"WAITING")}
        try {
            for((index,entry) in entries.withIndex()){
                currentCoroutineContext().ensureActive()
                val prepared=try {
                    val bytes=entry.second?.let{readBounded(it,EditorRenderer.MAX_BYTES)}?:requireNotNull(input.raster)
                    EditorRenderer.decode(bytes).flush()
                    Result.success(bytes)
                }catch(e:CancellationException){throw e}catch(e:Exception){Result.failure<ByteArray>(e)}
                val result=if(prepared.isSuccess){
                    val receipt=buildJsonObject{put("kind","image-ingress");put("version",1);put("source",if(entry.second==null)"clipboard-raster" else "file");put("channel",input.channel);entry.second?.let{put("sourcePath",it.toAbsolutePath().normalize().toString())};put("createdAt",System.currentTimeMillis())}.toString()
                    // A storage/commit failure stops the batch; never keep writing after uncertain persistence.
                    val id=try{workspace.importImage(entry.first,prepared.getOrThrow(),receipt)}catch(e:Exception){
                        gallery.imports=gallery.imports.toMutableList().also{it[index]=GalleryImportResult(entry.first,"STORAGE_ERROR · 状态待检查，批次停止")};throw e
                    };accepted+=id;current=id;selection=accepted.toSet()
                    try {library.repository.retainIngress(id);library.refresh()}catch(e:Exception){
                        gallery.imports=gallery.imports.toMutableList().also{it[index]=GalleryImportResult(entry.first,"ORIGINAL_RETAINED · 导入目录副本未完成，批次停止",id)};throw e
                    }
                    GalleryImportResult(entry.first,if(entry.second==null)"IMPORTED_RASTER（新编码PNG，无原始文件字节）" else "IMPORTED_ORIGINAL",id)
                }else{val e=prepared.exceptionOrNull()!!;GalleryImportResult(entry.first,"FAILED · ${if(e is DataProblem)e.status else e.javaClass.simpleName}")}
                gallery.imports=gallery.imports.toMutableList().also{it[index]=result}
            }
        }finally{
            gallery.imports=gallery.imports.map{if(it.status=="WAITING")it.copy(status="NOT_IMPORTED · 已停止")else it}
            if(gallery.imports.any{it.assetId==null})problem="部分文件未导入；查看逐项结果。已成功原件保留。"
            if(accepted.isNotEmpty()){onAccepted(accepted.toList());view="图库"}
        }
    }
    suspend fun enterMaintenance() {
        if(modelAccounts.dirty)throw DataProblem("ACCOUNT_DRAFT_UNSAVED","账户档案尚未保存，请在账户页保存或放弃修改后再关闭/更新")
        maintenance=true;message="正在停止接收新操作并保存、排水…"
        modelAccounts.cancel()
        naiSettings.cancelAndDrain()
        animation.cancelAndDrain()
        library.cancelAndDrain()
        active?.cancelAndJoin();pending?.cancelAndJoin();editor.drain();flush()
    }
    fun resumeAfterPreflightFailure() {workspace.state;check(active?.isActive!=true);maintenance=false;message="更新未开始安装；工作区仍可继续使用"}
    suspend fun close() {updater.closeAndDrain();enterMaintenance();infrastructure.closeAndDrain();withContext(Dispatchers.IO){workspace.close()};http.dispatcher.executorService.shutdown();http.connectionPool.evictAll()}
}

internal fun launchStudio(args:Array<String>) {
    fun option(name:String)=args.indexOf(name).takeIf{it>=0}?.let{args.getOrNull(it+1)}
    StudioBrand.configureDefaultWindows()
    val home=Paths.get(System.getenv("LOCALAPPDATA"),"CCBStudioSuite")
    val root=option("--studio-root")?.let(Paths::get)?:home.resolve("workspace-v3").let{current->if(!Files.exists(current)&&Files.exists(home.resolve("workspace-v2")))home.resolve("workspace-v2")else current}
    val runtime=option("--runtime-root")?.let(Paths::get)?:home.resolve("runtime")
    val updatePlan=option("--update-handoff")?.let(Paths::get)
    val workspace=try{openStudioForLaunch(root,runtime,updatePlan,{source,target->javax.swing.JOptionPane.showConfirmDialog(null,"此工作区需要升级资料格式。将在新目录建立副本，原目录和原件保留。\n来源：$source\n副本：$target\n继续？","升级资料副本",javax.swing.JOptionPane.OK_CANCEL_OPTION)==javax.swing.JOptionPane.OK_OPTION})}catch(e:Exception){
        if(updatePlan!=null&&updatePlan.toAbsolutePath().normalize().startsWith(runtime.toAbsolutePath().normalize().resolve("updates/handoffs")))runCatching{writeUpdateJson(updatePlan.parent.resolve("B_FAILED.json"),buildJsonObject{put("status","FAILED");put("reason",if(e is DataProblem)e.status else e.javaClass.simpleName)})}
        javax.swing.JOptionPane.showMessageDialog(null,studioStartupFailureText(e),"CCB Studio Suite",javax.swing.JOptionPane.ERROR_MESSAGE);return
    }
    val updateVerified=try{updatePlan?.let{verifyUpdatedWorkspace(workspace,it)}}catch(e:Exception){
        if(updatePlan!=null && updatePlan.toAbsolutePath().normalize().startsWith(runtime.toAbsolutePath().normalize().resolve("updates/handoffs")))runCatching{writeUpdateJson(updatePlan.parent.resolve("B_FAILED.json"),buildJsonObject{put("status","FAILED");put("reason",if(e is DataProblem)e.status else e.javaClass.simpleName)})}
        workspace.close();javax.swing.JOptionPane.showMessageDialog(null,"更新后的程序或资料验证未通过；已有程序和资料保留。","CCB Studio Suite",javax.swing.JOptionPane.ERROR_MESSAGE);return
    }
    application {
        val scope=rememberCoroutineScope();val c=remember{WorkbenchController(workspace,scope,DesktopNovelAiInfrastructure(runtime.resolve("catalog")),runtime.resolve("request-budget"),pauseCollectionsOnStart="--pause-collections" in args)}
        var closing by remember{mutableStateOf(false)}
        fun close(){if(!closing){closing=true;scope.launch{try{c.close();exitApplication()}catch(e:Exception){closing=false;c.problem=if(e is DataProblem && e.status=="ACCOUNT_DRAFT_UNSAVED")"账户档案尚未保存，请在账户页保存或放弃修改后再关闭" else "保存或排水尚未完成，窗口保持打开"}}}}
        c.requestClose=::close
        c.installUpdate={if(!closing){closing=true;scope.launch{
            var handoff:Path?=null
            var teardownStarted=false
            try {c.enterMaintenance();handoff=prepareUpdateHandoff(c,runtime);teardownStarted=true;c.close();exitApplication()}
            catch(e:Exception){
                handoff?.let{withContext(Dispatchers.IO){Files.writeString(it.parent.resolve("CANCEL"),"Application did not complete close",StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING)}}
                closing=false;if(!teardownStarted)runCatching{c.resumeAfterPreflightFailure()};c.problem="更新未完成，未强制释放资料锁：${e.message?.take(200)}"
            }
        }}}
        if(updatePlan!=null && updateVerified!=null)LaunchedEffect(Unit) {
            withFrameNanos{}
            withContext(Dispatchers.IO){writeUpdateJson(updatePlan.parent.resolve("B_VERIFIED.json"),updateVerified)}
            val missingCredentials=updateVerified["snapshot"]!!.jsonObject["credentials"]!!.jsonArray.count{it.jsonObject["status"]?.jsonPrimitive?.content=="MISSING_BEFORE_UPDATE"}
            val credentialNotice=if(missingCredentials>0)"；$missingCredentials 项凭据在更新前已缺失，仍需在账户页重新配置" else ""
            c.message="新版已启动，资料和凭据状态验证一致；正在完成更新交接$credentialNotice"
            val completed=withTimeoutOrNull(120000) {
                while(true) {
                    val status=withContext(Dispatchers.IO){val p=updatePlan.parent.resolve("result.json");if(Files.exists(p))Json.parseToJsonElement(Files.readString(p)).jsonObject else null}
                    when(status?.get("status")?.jsonPrimitive?.content){
                        "COMPLETE"->{c.message="更新完成：新版程序、资料与凭据状态已验证$credentialNotice";return@withTimeoutOrNull true}
                        "FAILED_RETAINED"->{c.problem="更新交接未完成；现有资料和旧程序均保留";return@withTimeoutOrNull false}
                    };delay(200)
                }
            }
            if(completed==null)c.problem="更新交接超时，尚不能确认完成；现有资料和旧程序均保留"
        }
        Window(onCloseRequest=::close,title="CCB Studio Suite · 工作台",undecorated=DesktopWindowChrome.isWindows,icon=StudioBrand.painter,state=rememberWindowState(width=1320.dp,height=880.dp)) {
            DisposableEffect(window){StudioFileDialogs.attach(window){c.problem=it};onDispose{StudioFileDialogs.detach(window)}}
            val chrome=rememberDesktopWindowChrome(window,::close);SideEffect{chrome.appearance(true)}
            MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=Color(0xffe6edf3))) {Workbench(c,chrome,closing)}
        }
        if(c.designDetached)Window(onCloseRequest=c::dismissDesign,title="CCB Studio Suite · AI 设计",icon=StudioBrand.painter,
            resizable=true,state=rememberWindowState(width=1120.dp,height=820.dp)) {
            SideEffect{window.minimumSize=java.awt.Dimension(700,520)}
            MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=DesktopBootstrapColors.foreground)) {
                Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){DesignWorkspace(c,detached=true)}
            }
        }
        DisguiseCoverManagerWindow(c)
    }
}

private class WorkbenchChrome(private val chrome:DesktopWindowChrome) {
    var width=0f;var height=0f;var title:ChromeRect?=null;var icon:ChromeRect?=null
    val buttons=linkedMapOf<String,ChromeRect>();val captions=linkedMapOf<ChromeHit,ChromeRect>()
    fun rect(c:LayoutCoordinates)=c.boundsInWindow().let{ChromeRect(it.left,it.top,it.right,it.bottom)}
    fun publish(){chrome.updateLayout(DesktopChromeLayout(width,height,title,buttons.values.toList(),captions.toMap(),icon))}
}
@Composable internal fun Workbench(c:WorkbenchController,chrome:DesktopWindowChrome,closing:Boolean) {
    LibrarySynchronization(c)
    AssetArchiveConfirmation(c)
    Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground,modifier=Modifier.onPreviewKeyEvent{event->
        if(!c.maintenance && event.type==KeyEventType.KeyDown && event.isCtrlPressed && event.isShiftPressed && event.key==Key.U){c.view="更新";true}else false
    }) {
    val r=remember{WorkbenchChrome(chrome)};val native by chrome.state.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact=maxWidth<860.dp
    val showBrand=maxWidth>=1100.dp
    DisposableEffect(compact){r.buttons.clear();r.publish();onDispose{r.buttons.clear();r.publish()}}
    Column(Modifier.fillMaxSize().background(Ink).onGloballyPositioned{r.width=it.size.width.toFloat();r.height=it.size.height.toFloat();r.publish()}) {
        Row(Modifier.fillMaxWidth().height(44.dp).background(Panel).onGloballyPositioned{r.title=r.rect(it);r.publish()},verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.width(40.dp).fillMaxHeight().onGloballyPositioned{r.icon=r.rect(it);r.publish()},contentAlignment=Alignment.Center){Image(StudioBrand.painter,"CCB 蓝色 logo",Modifier.size(24.dp))}
            if(showBrand)Text("Studio Suite",Modifier.width(120.dp),fontSize=14.sp)
            studioRoutes.forEach {label -> StudioRouteButton(label,c.view==label,compact,Modifier.onGloballyPositioned{r.buttons[label]=r.rect(it);r.publish()}){c.view=label} }
            Spacer(Modifier.weight(1f))
            listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE).forEach {hit ->
                StudioCaptionButton(hit,native,compact,Modifier.onGloballyPositioned{r.captions[hit]=r.rect(it);r.publish()},chrome::command)
            }
        }
        Divider()
        if(c.library.collectionBusy)Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(c.library.automaticNotice,Modifier.weight(1f),fontSize=12.sp)
            TextButton(onClick={c.library.stopCollection()}){Text("停止目录收集")}
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {when(c.view){"AI 设计"->if(c.designDetached)Column(Modifier.padding(20.dp)){Text("AI 设计已在独立窗口打开");StudioAction("切回主窗口编辑",onClick=c::reattachDesign)}else DesignWorkspace(c);"图库"->GalleryWorkspace(c);"编辑"->TypesetWorkspace(c);"资产管理"->LibraryWorkspace(c);"账户"->AccountsWorkspace(c);"更新"->UpdateWorkspace(c.updater,c.workspace.root.toString(),c.installUpdate);else->CreationWorkspace(c)}}
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(closing)"正在保存并排水…"else c.message,Modifier.weight(1f),fontSize=12.sp,color=Quiet)
            Text(if(c.saved)"创作草稿已保存" else "创作草稿保存中…",fontSize=12.sp)
        }
    }
    }
    if(c.maintenance)AlertDialog(onDismissRequest={},title={Text("正在维护")},text={Text("正在保存、排水或交接更新。现有资料锁保持到操作完成；请等待，不会强制终止写入。")},confirmButton={if(!closing && c.requestClose!=null)TextButton(onClick={c.requestClose?.invoke()}){Text("再次尝试安全退出")}})
    c.problem?.let {AlertDialog(onDismissRequest={c.problem=null},title={Text("操作回执")},text={SelectionContainer{Text(it)}},confirmButton={TextButton(onClick={c.problem=null}){Text("关闭")}})}
    }
}

@Composable private fun CreationWorkspace(c:WorkbenchController) {
    val translated=c.state.creation!!.translatedTags
    var paste by remember{mutableStateOf<String?>(null)}
    var clear by remember{mutableStateOf(false)}
    var showSettings by remember{mutableStateOf(false)}
    val cost=runCatching{NovelAiImageCostEstimator.estimate(c.draft.activeSettings,c.usage,c.draft.imageGuidance,c.vibeMisses())}.getOrNull()
    val generating=c.busy&&c.activeOperation=="NovelAI 生成"
    val refreshing=c.busy&&c.activeOperation=="刷新账户额度"
    val tokens by produceState<NovelAiPromptTokenUsage?>(null,c.draft.toPromptPlan(),c.draft.selectedModel){value=runCatching{c.infrastructure.countTokens(c.draft)}.getOrNull()}
    DesktopStudioWorkspace(compact={expand ->
        Column(Modifier.fillMaxWidth().border(1.dp,DesktopBootstrapColors.border).padding(8.dp)
            .semantics{contentDescription="Studio 当前图片摘要"}) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(88.dp).clickable(onClick=expand)){AssetImage(c,c.current,Modifier.fillMaxSize())}
                Column(Modifier.weight(1f).padding(start=10.dp)) {
                    StatusText(if(c.busy)"生成中 · 先前图片保留" else "当前图片")
                    StudioAction("展开图片预览",onClick=expand)
                    if(c.current!=null)StudioAction("图像操作 / 用作",icon=DesktopAppIcons.Star){c.section="图像引导"}
                }
            }
        }
    },editor={
        StudioActions {
            StudioGenerationModelChoice(c.draft,c::edit)
            AccountChoice(c,"NAI")
            StatusText(c.usage?.let{"额度 ${it.anlas} Anlas"}?:"账户额度未知，未自动请求")
            StudioAction(if(refreshing)"取消刷新额度" else "刷新额度",refreshing||(!c.busy && c.naiId!=null),icon=DesktopAppIcons.Refresh){if(refreshing)c.active?.cancel()else c.refreshUsage()}
        }
        if(c.draft.selectedModel==NovelAiImageModel.V5_FULL)NaiAllowanceBar(c.usage)
        NaiCatalogBackgroundStatus(c)
        Spacer(Modifier.height(10.dp))
        StudioActions {
            StudioAction("AI 设计",icon=DesktopAppIcons.Chat){c.view="AI 设计"}
            StudioAction("图像引导",icon=DesktopAppIcons.Star){c.section="图像引导"}
            StudioAction(if(c.busy&&c.activeOperation=="导入图片")"停止导入图片" else "导入图片",!c.busy||c.activeOperation=="导入图片",icon=DesktopAppIcons.ImageAdd){if(c.busy)c.active?.cancel()else c.importImages()}
            StudioAction("历史图库",icon=DesktopAppIcons.History){c.view="图库"}
            StudioAction("创作设置"){showSettings=true}
        }
        Spacer(Modifier.height(12.dp))
        if(c.section=="图像引导") {
            StudioAction("返回 Prompt"){c.section="Prompt"}
            GuidanceWorkspace(c)
        } else {
            StudioSwitch("中文注释",translated){c.defaults(translated=!translated)}
            StudioActions {
                StudioAction("复制正向"){desktopCopyText(c.draft.copyPositivePrompt())}
                StudioAction("Prompt 资产"){c.view="资产管理"}
                StudioAction("粘贴并预览",!c.busy){runCatching{java.awt.Toolkit.getDefaultToolkit().systemClipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String}.onSuccess{paste=it}.onFailure{c.problem="剪贴板文字暂不可读取"}}
                StudioAction("清空 Prompt",!c.busy){clear=true}
            }
            ImageCardChoice(c)
            StudioDisclosure("画风",summary=c.draft.stylePrompt.take(60)) {
                StudioPromptField("画风",c.draft.stylePrompt,c.infrastructure,translated){v->c.edit{it.copy(stylePrompt=v)}}
            }
            StudioDisclosure("基础 Prompt",initiallyOpen=true) {
                StudioPromptField("基础 Prompt",c.draft.basePrompt,c.infrastructure,translated){v->c.edit{it.copy(basePrompt=v)}}
            }
            StudioDisclosure("补充 Prompt",summary=c.draft.extraPrompt.take(60),initiallyOpen=c.draft.extraPrompt.isNotBlank()) {
                StudioPromptField("补充 Prompt",c.draft.extraPrompt,c.infrastructure,translated){v->c.edit{it.copy(extraPrompt=v)}}
            }
            StudioDisclosure("负向 Prompt",summary=c.draft.negativePrompt.take(60)) {
                StudioPromptField("负向 Prompt",c.draft.negativePrompt,c.infrastructure,translated){v->c.edit{it.copy(negativePrompt=v)}}
            }
            DesktopStudioCharacters(c.draft,c.infrastructure,translated,c::edit)
            StudioDisclosure("生成设置",initiallyOpen=true) {
                StudioGenerationSettings(c.draft,c::edit)
            }
        }
    },footer={
        DesktopStudioFooter(tokens,!c.busy && c.state.creation!!.undo.isNotEmpty(),!c.busy && c.state.creation!!.redo.isNotEmpty(),
            if(generating)"停止生图" else naiGenerateLabel(false,c.naiId!=null,cost,c.usage!=null,c.draft.continuousModeEnabled),generating||(!c.busy&&c.naiId!=null&&cost!=null),
            {c.undo()},{c.undo(true)},{if(generating)c.active?.cancel() else if(!c.busy)c.generate()})
    },preview={rail -> ResultPreview(c,rail)})
    if(showSettings)NaiSettingsDialog(c){showSettings=false}
    paste?.let{text->AlertDialog(onDismissRequest={paste=null},title={Text("确认粘贴覆盖")},text={SelectionContainer{Text(text,Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()))}},confirmButton={TextButton(onClick={c.pastePrompt(text);paste=null}){Text("确认覆盖，可撤销")}},dismissButton={TextButton(onClick={paste=null}){Text("取消")}})}
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("清空 Prompt？")},text={Text("保留图像引导和生成设置。可撤销恢复。")},confirmButton={TextButton(onClick={c.clearPromptDraft();clear=false}){Text("确认清空")}},dismissButton={TextButton(onClick={clear=false}){Text("取消")}})
}

@Composable private fun AccountChoice(c:WorkbenchController,service:String) {
    val accounts=c.state.accounts.filter{it.service==service}
    if(accounts.isEmpty())StudioAction("配置 $service 账户"){c.view="账户"}
    else CompactChoice(service,accounts,accounts.firstOrNull{it.id==if(service=="LLM")c.llmId else c.naiId},{if(service=="NAI")it.alias else "${it.alias} · ${it.model}"}){c.chooseAccount(service,it.id)}
}
@Composable internal fun AssetImage(c:WorkbenchController,id:String?,modifier:Modifier) {
    if(id!=null)DesktopOwnedImage("asset:$id",c::readAsset,modifier)
    else Box(modifier,contentAlignment=Alignment.Center){Text("生成或导入图片后在这里查看",color=Quiet)}
}
@Composable private fun ResultPreview(c:WorkbenchController,rail:Boolean) {
    var toolImage by remember{mutableStateOf<Pair<String,ByteArray>?>(null)}
    var processing by remember{mutableStateOf<String?>(null)}
    var reversing by remember{mutableStateOf(false)}
    var metadata by remember{mutableStateOf<String?>(null)}
    var viewing by remember{mutableStateOf<List<String>?>(null)}
    var initial by remember{mutableStateOf(0)}
    val scope=rememberCoroutineScope()
    val available=c.assets.filterNot{it.request().id().toString() in c.library.archived}
    fun viewerIds(id:String)=(available.map{it.request().id().toString()}+id).distinct()
    val interim by produceState<ImageBitmap?>(null,c.intermediate){value=withContext(Dispatchers.IO){c.intermediate?.let{bytes->runCatching{val decoded=EditorRenderer.decode(bytes);try{decoded.toComposeImageBitmap()}finally{decoded.flush()}}.getOrNull()}}}
    Column(Modifier.fillMaxSize()) {
        if(!rail)c.current?.let{id->StudioActions{
            StudioAction("图像工具",!c.busy){scope.launch{toolImage=id to withContext(Dispatchers.IO){c.workspace.bytes(id)}}}
            StudioAction("用于图像引导",!c.busy){c.section="图像引导"}
            StudioAction("增强 / 放大",!c.busy){processing=id}
            StudioAction("反推 Prompt",!c.busy && c.llmId!=null){reversing=true;c.reverseImage(id)}
            StudioAction("元数据与复用",!c.busy){metadata=id}
            StudioAction("全图查看"){viewing=viewerIds(id);initial=viewing!!.indexOf(id).coerceAtLeast(0)}
        }}
        if(!rail) {if(interim!=null && c.busy) {StatusText("生成中预览 · 尚未保存为原件");Image(interim!!,"生成中预览",Modifier.weight(1f).fillMaxWidth(),contentScale=ContentScale.Fit)}else c.current?.let{id->DesktopImageZoomSurface("asset:$id",c::readAsset,Modifier.weight(1f).fillMaxWidth(),onOpenViewer={viewing=viewerIds(id);initial=viewing!!.indexOf(id).coerceAtLeast(0)})}?:AssetImage(c,null,Modifier.weight(1f).fillMaxWidth())}
        val paths=available.asReversed().map{"asset:${it.request().id()}"}
        val selected=c.current?.let{"asset:$it"}
        val select:(String)->Unit={c.current=it.removePrefix("asset:")}
        val preview:(String)->Unit={path->viewing=viewerIds(path.removePrefix("asset:"));initial=viewing!!.indexOf(path.removePrefix("asset:")).coerceAtLeast(0)}
        if(rail)Box(Modifier.weight(1f)){StudioVerticalFilmstrip(paths,selected,c::readAsset,onPreview=preview,onSelect=select)}
        else StudioFilmstrip(paths,selected,c::readAsset,onPreview=preview,onSelect=select)
        StatusText("${c.assets.size} 份原件 · 原始字节保全")
    }
    toolImage?.let{(id,bytes)->DesktopImageToolsDialog(bytes,c.picker,{toolImage=null},onProcessedImage={c.saveToolCopy(id,it)})}
    viewing?.let{ids->StudioAssetViewer(c,ids,initial){viewing=null}}
    metadata?.let{id->DesktopImageToolWindow("元数据与历史复用",{if(!c.busy)metadata=null}){MetadataWorkspace(c,id){metadata=null}}}
    processing?.let{id->DesktopImageToolWindow("增强与放大",{if(!c.busy)processing=null}) {
        Column(Modifier.fillMaxSize().background(Ink).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            DesktopImagePostProcessPanel(id,c){processing=null}
            StudioAction("返回创作",!c.busy){processing=null}
        }
    }}
    if(reversing)DesktopImageToolWindow("图片反推候选",{if(!c.busy)reversing=false}) {
        Column(Modifier.fillMaxSize().background(Ink).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            StatusText(c.message);if(c.stream.isNotBlank())SelectionContainer{Text(c.stream)}
            if(c.reasoning.isNotBlank())StudioDisclosure("思考过程"){SelectionContainer{Text(c.reasoning)}}
            c.state.creation!!.reverseCandidate?.let{candidate->
                StatusText("来源 ${candidate.sourceId.take(8)} · ${candidate.targetModel.displayName}")
                DesktopDesignResult(NovelAiDesignReply(candidate.plan,candidate.targetModel,candidate.accountId,false))
                StudioAction("确认应用反推候选",!c.busy){c.applyReverse();reversing=false}
            }
            StudioAction(if(c.busy)"停止"else"返回创作"){if(c.busy)c.active?.cancel()else reversing=false}
        }
    }
}

@Composable internal fun DesignWorkspace(c:WorkbenchController,detached:Boolean=false) {
    val designing=c.busy&&c.activeOperation=="AI 设计"
    val creation=c.state.creation!!;val conversation=creation.conversations.firstOrNull{it.id==creation.currentConversationId}
    var candidate by remember{mutableStateOf<Pair<String,String>?>(null)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        StudioActions {
            if(detached)StudioAction("切回主窗口",onClick=c::reattachDesign)
            else {StudioAction("返回创作"){c.view="创作"};StudioAction("独立窗口",onClick=c::detachDesign)}
            StudioAction("新对话",!c.busy){c.newConversation()};AccountChoice(c,"LLM")
        }
        Spacer(Modifier.height(8.dp));CompactChoice("设计历史",creation.conversations,conversation,{it.title}){c.selectConversation(it.id)}
        StudioDisclosure("设计设置") {
            StudioSwitch("V5 自然语言设计",c.draft.aiDesignNaturalLanguageMode){c.edit{it.copy(aiDesignNaturalLanguageMode=!it.aiDesignNaturalLanguageMode)}}
            DesktopDesignField("额外要求",c.draft.extraRequirement,"extraRequirement"){value->c.edit{it.copy(extraRequirement=value)}}
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(conversation?.turns.orEmpty(),key={it.id}) {turn ->
                var edit by remember{mutableStateOf(false)};var text by remember(turn.userText){mutableStateOf(turn.userText)}
                Column(Modifier.fillMaxWidth().background(Panel).padding(14.dp)) {
                    StatusText("你的要求");SelectionContainer{Text(turn.userText)}
                    turn.attachedStudioPrompt?.let{StatusText("附加当前基础 Prompt 与 ${it.characterPrompts.size} 个角色")}
                    if(edit)DesktopDesignField("编辑本轮要求",text,turn.id){text=it}
                    StudioActions{StudioAction(if(edit)"取消编辑" else "编辑并分支",!c.busy){edit=!edit};if(edit)StudioAction("建立分支",!c.busy && text.isNotBlank()){c.branch(conversation!!.id,turn.id,text);edit=false}}
                    turn.reply?.let {reply->
                        DesktopDesignResult(reply)
                        StudioActions {
                            StudioAction("应用候选到创作",!c.busy,style=StudioActionStyle.PRIMARY){candidate=conversation!!.id to turn.id}
                            StudioAction("候选加入多 Prompt 参照",!c.busy){c.run("加入设计参照"){
                                saveDesignPromptReference(c,conversation!!.id,turn.id);c.library.tab="多 Prompt 参照";c.view="资产管理"
                            }}
                        }
                    }
                    if(turn.status!=NovelAiDesignTurnStatus.COMPLETED)StatusText(turn.error.ifBlank{"正在设计…"},Gold)
                    if(turn.status in listOf(NovelAiDesignTurnStatus.FAILED,NovelAiDesignTurnStatus.CANCELLED))StudioAction("准备重试分支",!c.busy){c.retry(conversation!!.id,turn.id)}
                }
            }
            if(c.stream.isNotBlank())item{StudioDisclosure("本轮流式结果",initiallyOpen=true){SelectionContainer{Text(c.stream)}}}
            if(c.reasoning.isNotBlank())item{StudioDisclosure("思考过程"){SelectionContainer{Text(c.reasoning)}}}
        }
        StudioSwitch("附加当前基础 Prompt 与启用角色",c.attach){c.attach(!c.attach)}
        DesktopDesignField("画面需求 / 继续修改",c.composer,"composer",c::input)
        StudioAction(if(designing)"停止 AI 设计" else "发送",enabled=designing || (!c.busy&&c.composer.isNotBlank() && c.llmId!=null),style=StudioActionStyle.PRIMARY){if(designing)c.active?.cancel()else if(!c.busy)c.design()}
        if(detached){StatusText(c.message);c.problem?.let{Text(it,color=MaterialTheme.colors.error)}}
    }
    candidate?.let{(conversationId,turnId)->
        val reply=creation.conversations.single{it.id==conversationId}.turns.single{it.id==turnId}.reply!!
        val proposed=c.draft.applyDesignedPromptPlan(reply.plan,reply.targetImageModel)
        AlertDialog(onDismissRequest={candidate=null},title={Text("确认应用设计候选")},text={
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("应用后可以撤销。按原 Studio 规则替换基础 Prompt 和角色、清空附加 Prompt；保留画风及手动采样设置。")
                SelectionContainer {Column {
                    Text("当前基础 Prompt");Text(c.draft.basePrompt.ifBlank{"（空）"})
                    Spacer(Modifier.height(10.dp));Text("候选基础 Prompt");Text(proposed.basePrompt)
                    Spacer(Modifier.height(10.dp));Text("角色 ${c.draft.characters.size} → ${proposed.characters.size}")
                    proposed.characters.forEachIndexed {index,character->Text("角色 ${index+1}: ${character.prompt}")}
                    Text("模型 ${c.draft.selectedModel.displayName} → ${proposed.selectedModel.displayName}")
                }}
            }
        },confirmButton={TextButton(onClick={candidate=null;c.apply(conversationId,turnId)}){Text("确认应用")}},dismissButton={TextButton(onClick={candidate=null}){Text("保留当前草稿")}})
    }
}

@Composable private fun AccountsWorkspace(c:WorkbenchController) {
    val editor=c.modelAccounts;val d=editor.draft
    DisposableEffect(editor){onDispose{editor.cancel()}}
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("账户与模型",fontSize=22.sp)
        Text("默认语言模型：${editor.language?.let{"${it.alias} · ${it.model}"}?:"未设置"}")
        Text("当前视觉模型：${editor.vision?.let{"${it.alias} · ${it.model}"}?:"未启用"}${if(editor.language?.isMultimodal==true)"（语言模型直接识图）" else if(editor.vision!=null)"（独立识图，供当前语言模型使用）" else ""}")
        Text("NAI 生图：${editor.nai?.let{"${it.alias} · ${c.draft.selectedModel.displayName}"}?:"未设置账户"}")
        var showGlobal by remember{mutableStateOf(false)}
        StudioAction(if(showGlobal)"收起共用凭据" else "共用 API Key · ${if(editor.globalConfigured)"已配置" else "未配置"}"){showGlobal=!showGlobal}
        if(showGlobal) {
            Text("仅供选择“使用默认凭据”的文本和视觉模型使用；接口地址仍分别设置。")
            OutlinedTextField(editor.globalKey,{editor.globalKey=it},label={Text("设置或替换共用 API Key")},visualTransformation=PasswordVisualTransformation(),enabled=!c.busy,modifier=Modifier.fillMaxWidth())
            StudioAction("保存共用凭据",enabled=!c.busy&&editor.globalKey.isNotBlank()){editor.saveGlobal()}
        }
        StudioChips("配置分类",listOf("语言模型","视觉模型","NAI 生图"),editor.category,{it}){editor.category=it}
        val profiles=c.state.accounts.filter{a->when(editor.category){"NAI 生图"->a.service=="NAI";"视觉模型"->a.service=="LLM"&&a.isMultimodal;else->a.service=="LLM"}}
        profiles.forEach {a->
            val active=when(editor.category){"视觉模型"->a.id==editor.vision?.id;"NAI 生图"->a.id==c.naiId;else->a.id==c.llmId}
            Column(Modifier.fillMaxWidth().background(Panel).padding(10.dp)) {
                Text("${a.alias}${if(a.service=="NAI")" · NAI账户" else " · ${a.model}"}${if(active)" · ✓ 当前生效" else " · 未选用"}")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    StudioAction("编辑配置",enabled=!c.busy){editor.open(a,editor.category=="视觉模型")}
                    if(!active)StudioAction(if(editor.category=="视觉模型")"启用此视觉模型" else if(editor.category=="NAI 生图")"选用此 NAI 账户" else "设为默认语言模型",enabled=!c.busy&&(editor.category!="视觉模型"||editor.language!=null)&&(a.service=="NAI"||modelPurpose(a.model).supportsChatSelection)){if(editor.category=="视觉模型")c.chooseVision(a.id) else c.chooseAccount(a.service,a.id)}
                }
            }
        }
        if(profiles.isEmpty())Text("尚无${editor.category}配置")
        if(editor.category=="视觉模型") {
            Text("独立配置图片理解模型；启用后关联到默认语言模型。切换语言模型时，顶部显示该语言模型实际使用的视觉配置。",fontSize=12.sp,color=Quiet)
            StudioAction("新建视觉模型",enabled=!c.busy){editor.newVision()}
            if(editor.vision!=null)StudioAction("停用当前视觉模型",enabled=!c.busy){c.chooseVision(null)}
        } else StudioAction("新建档案",enabled=!c.busy){if(editor.category=="NAI 生图")editor.newNai() else editor.open(null)}
        val showDraft=when(editor.category){"视觉模型"->editor.visionEditing;"NAI 生图"->d.service=="NAI";else->d.service=="LLM"&&!editor.visionEditing}
        if(showDraft) {
        Text("${if(d.existing==null)"新建" else "编辑"}${if(editor.visionEditing)"视觉模型" else if(d.service=="NAI")"NAI账户" else "语言模型"}配置 · 编辑不改变当前选择")
        OutlinedTextField(d.alias,{value->editor.edit{it.copy(alias=value)}},label={Text("档案名称")},enabled=!c.busy,modifier=Modifier.fillMaxWidth())
        if(d.service=="LLM")OutlinedTextField(d.endpoint,{value->editor.edit{it.copy(endpoint=value)}},label={Text("HTTPS API 地址")},enabled=!c.busy,modifier=Modifier.fillMaxWidth())
        else Text("NovelAI 官方服务 · 固定接口")
        if(d.service=="LLM")StudioChips("API Key 来源",listOf(false,true),d.useGlobalKey,{if(it)"使用共用凭据" else "独立配置"}){value->editor.edit{it.copy(useGlobalKey=value,key="")}}
        if(!d.useGlobalKey)OutlinedTextField(d.key,{value->editor.edit{it.copy(key=value)}},label={Text(if(d.existing?.secretRef!=null&&d.existing.secretRef!=SecretVault.GLOBAL_LLM_REF)"新凭据（留空保留已有）"else"API 凭据")},visualTransformation=PasswordVisualTransformation(),enabled=!c.busy,modifier=Modifier.fillMaxWidth())
        else Text(if(editor.globalConfigured)"使用本工作区共用默认凭据" else "尚未配置默认凭据；可先保存档案，调用前需配置")
        if(d.service=="LLM") {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                StudioAction(if(editor.searching)"正在获取…" else "获取模型列表",enabled=!c.busy&&!editor.searching){editor.discover()}
                if(editor.searching)StudioAction("取消获取"){editor.cancel()}
            }
            editor.error?.let{StatusText(it)}
            if(editor.ids.isNotEmpty()) {
                OutlinedTextField(editor.query,{editor.query=it},label={Text("筛选模型")},modifier=Modifier.fillMaxWidth())
                val filtered=editor.ids.filter{it.contains(editor.query,ignoreCase=true)}
                Text("${filtered.size} 个匹配 · ${editor.ids.size} 个模型")
                LazyColumn(Modifier.fillMaxWidth().heightIn(max=220.dp)) {
                    items(filtered,key={it}){id->
                        val purpose=modelPurpose(id)
                        Column {
                            StudioAction(if(d.model==id)"✓ $id" else id,enabled=!c.busy&&purpose.supportsChatSelection){editor.select(id)}
                            Text(purpose.label,color=Quiet,fontSize=12.sp)
                        }
                    }
                }
            }
        }
        if(d.service=="LLM")OutlinedTextField(d.model,{value->editor.edit{it.copy(model=value)}},label={Text("模型 ID")},enabled=!c.busy,modifier=Modifier.fillMaxWidth())
        else Text("此处保存 NAI 账户与凭据。当前生图模型：${c.draft.selectedModel.displayName}，请在创作的生成设置中选择。")
        if(d.service=="LLM") {
            Text("模型用途按名称提示，未知型号需核对服务商；视觉模型须支持图片输入和对话。")
            if(!modelPurpose(d.model).supportsChatSelection)StatusText("${modelPurpose(d.model).label}；请更换模型，不能保存为设计或视觉模型。")
            if(!editor.visionEditing)StudioSwitch("此语言模型直接识图",d.multimodal){editor.edit{it.copy(multimodal=!it.multimodal,visionId=null)}}
            if(!d.multimodal) {
                val vision=c.state.accounts.filter{it.service=="LLM"&&it.isMultimodal&&it.id!=d.existing?.id&&modelPurpose(it.model).supportsChatSelection}
                CompactChoice("使用独立视觉模型",vision,vision.firstOrNull{it.id==d.visionId},{it.alias}){value->editor.edit{it.copy(visionId=value.id)}}
                if(vision.isEmpty())Text("先用“新建视觉模型”配置并保存配置，再返回此档案关联。")
                if(d.visionId!=null)StudioAction("清除视觉关联"){editor.edit{it.copy(visionId=null)}}
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            StudioAction("保存配置",enabled=!c.busy&&editor.canSave){editor.save(false)}
            StudioAction(if(editor.visionEditing)"保存并启用视觉模型" else if(d.service=="NAI")"保存并选用此账户" else "保存并设为当前模型",enabled=!c.busy&&editor.canSave&&(!editor.visionEditing||editor.language!=null),style=StudioActionStyle.PRIMARY){editor.save()}
        }
        Text(if(editor.dirty)"有未保存修改" else if(d.existing!=null)"已保存" else "凭据保存在本机受保护存储")
        if(editor.dirty)StudioAction("放弃修改",enabled=!c.busy){editor.discard()}
        if(!editor.dirty && d.existing!=null)StudioAction(if(editor.visionEditing)"启用为当前视觉模型" else if(d.service=="NAI")"选用此账户" else "设为当前模型",enabled=!c.busy&&(!editor.visionEditing||editor.language!=null)){if(editor.visionEditing)c.chooseVision(d.existing.id) else c.chooseAccount(d.service,d.existing.id)}
        }
        StudioAction("返回创作"){c.view="创作"}
    }
    if(editor.pending!=null) AlertDialog(onDismissRequest={editor.continueEditing()},title={Text("保留当前修改？")},text={Text("当前档案尚未保存。切换会放弃这些修改。")},confirmButton={TextButton(onClick={editor.discardAndContinue()}){Text("放弃修改并切换")}},dismissButton={TextButton(onClick={editor.continueEditing()}){Text("继续编辑")}})
}
