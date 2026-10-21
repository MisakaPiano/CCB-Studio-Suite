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
import javax.swing.JFileChooser

internal class WorkbenchController(val workspace:StudioWorkspace,val scope:CoroutineScope,val infrastructure:DesktopNovelAiInfrastructure,budgetRoot:Path,
    val http:okhttp3.OkHttpClient=studioHttpClient(),admissionOverride:RequestAdmission?=null) {
    val session=CreationSession(workspace)
    val gallery = GalleryViewState()
    val editor by lazy {TypesetEditorController(this)}
    val vault=SecretVault(workspace.root.resolve("secrets"))
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
    var busy by mutableStateOf(false);private set
    var saved by mutableStateOf(true);private set
    var stream by mutableStateOf("");var reasoning by mutableStateOf("")
    var intermediate by mutableStateOf<ByteArray?>(null);private set
    var usage by mutableStateOf<NovelAiAccountUsage?>(null);private set
    var usageAt by mutableStateOf<Long?>(null);private set
    var current by mutableStateOf(state.jobs.lastOrNull()?.let{it.composedResults.lastOrNull()?:it.results.lastOrNull()})
    var selection by mutableStateOf(setOf<String>())
    var assets by mutableStateOf(visibleAssets());private set
    val picker=DesktopFilePicker()
    var active:Job?=null;private set
    private var pending:Job?=null
    private val admission=admissionOverride?:BudgetAdmission(LiveRequestBudget(budgetRoot)){wait -> if(wait>0)message="请求预算等待 ${(wait+999)/1000} 秒 · 可停止"}
    private val guidanceRoot=budgetRoot.parent.resolve("guidance")
    private val metadataRoot=budgetRoot.parent.resolve("metadata-snapshots")
    private val modelRoot=budgetRoot.parent.resolve("model-snapshots")
    private fun guidanceFor(account:AccountProfile?)=DesktopNovelAiGuidance(guidanceRoot,::readAsset,{
        vault.use(requireNotNull(account?.secretRef)){it.decodeToString()}
    },admission,http)
    fun visibleAssets():List<AssetStore.Asset> {
        val s=workspace.state;val copies=(s.savedCopies+s.jobs.flatMap{it.composedResults}).toSet()
        return workspace.catalog().filter{it.request().project().decodeToString() !in setOf("CCB_REQUEST_PAYLOAD_V1",TypesetRepository.TAG,TypesetFonts.TAG,TypesetSourceExchange.TAG) && (it.request().kind()==AssetStore.Kind.ORIGINAL || it.request().id().toString() in copies)}
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
        val job=workspace.state.jobs.lastOrNull{id in it.results || id in it.composedResults} ?: return null
        val json=Json.parseToJsonElement(job.recipe).jsonObject
        if(json["kind"]!=null)return null
        val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
        receipt.expandedRequest(workspace) // Fail closed when referenced request evidence is unavailable or corrupt.
        val recipe=receipt.recipeForReuse(workspace)
        val index=job.results.indexOf(id).takeIf{it>=0} ?: workspace.catalog().single{it.request().id().toString()==id}.request().parentId()?.let{job.results.indexOf(it.toString())} ?: -1
        require(index>=0)
        return recipe to recipe.settings.seed+index
    }
    init {if(current==null)current=assets.lastOrNull()?.request()?.id()?.toString()}
    private fun changed(){saved=false;pending?.cancel();pending=scope.launch {delay(350);try{flush()}catch(e:CancellationException){throw e}catch(e:Exception){problem="草稿未能保存，保留当前输入；请先处理数据错误"}}}
    fun edit(change:(NovelAiStudioDraft)->NovelAiStudioDraft){draft=workspace.state.creation!!.resolvedDraft(change(draft));changed()}
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
    fun clearPromptDraft()=edit{it.clearPrompts(workspace.state.creation!!.imageCards.firstOrNull{card->card.id==it.importedCharacterCardId}?.negativePrompt)}
    fun pastePrompt(text:String)=run("粘贴正向 Prompt") {
        require(text.length<=100000)
        session.editDraft{NovelAiStudioPromptClipboard.apply(text,it)};draft=workspace.state.creation!!.draft
    }
    fun input(value:String){composer=value;changed()}
    fun attach(value:Boolean){attach=value;changed()}
    fun chooseAccount(service:String,id:String){if(service=="LLM")llmId=id else {naiId=id;usage=null;usageAt=null};changed()}
    suspend fun flush() {
        val d=draft;val input=composer;val attached=attach;val l=llmId;val n=naiId
        withContext(Dispatchers.IO){session.update {old -> (if(old.draft==d)old else old.change(d)).copy(composer=input,attachCurrentPrompt=attached,llmAccountId=l,naiAccountId=n)}}
        state=workspace.state;saved=draft==d && composer==input && attach==attached && llmId==l && naiId==n
    }
    fun run(label:String,body:suspend ()->Unit) {
        if(busy)return
        busy=true;message=label;problem=null
        active=scope.launch {
            try {pending?.cancelAndJoin();flush();withContext(Dispatchers.IO){body()};message="$label · 已完成"}
            catch(e:CancellationException){message="已停止；已提交结果保留";throw e}
            catch(e:Exception){problem="${if(e is DataProblem)e.status else e.javaClass.simpleName}：操作未完成，未自动重发。已有资料保留。";message="操作未完成"}
            finally {try{state=workspace.state;assets=visibleAssets()}catch(e:Exception){problem="工作区需要恢复，已保留现有文件；请停止写入并检查"};busy=false}
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
    fun undo(redo:Boolean=false)=run(if(redo)"重做草稿" else "撤销草稿") {session.update{if(redo)it.redoDraft() else it.undoDraft()};draft=workspace.state.creation!!.draft}
    fun generate()=run("NovelAI 生成") {
        val account=state.accounts.single{it.id==naiId && it.service=="NAI"}.copy(model=draft.selectedModel.apiId)
        require(draft.basePrompt.isNotBlank())
        val requested=draft
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
            })
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
    fun saveAccount(existing:AccountProfile?,alias:String,service:String,endpoint:String,model:String,key:String,isMultimodal:Boolean=false,visionAccountId:String?=null)=run("保存账户") {
        val ref=if(key.isNotBlank()) {val bytes=key.trim().toByteArray();try{vault.put(bytes)}finally{bytes.fill(0)}} else existing?.secretRef
        val a=AccountProfile(id=existing?.id?:newId(),alias=alias,service=service,provider=if(service=="NAI")"NOVELAI" else "OPENAI_COMPATIBLE",endpoint=endpoint.trim().trimEnd('/'),model=model,secretRef=ref,isMultimodal=service=="LLM"&&isMultimodal,visionAccountId=visionAccountId.takeIf{service=="LLM"&&!isMultimodal})
        a.validate();workspace.operation{workspace.save(workspace.state.copy(accounts=workspace.state.accounts.filterNot{it.id==a.id}+a))}
        if(service=="LLM")llmId=a.id else naiId=a.id
        session.update{it.copy(llmAccountId=llmId,naiAccountId=naiId)}
    }
    fun importImages() {
        val chooser=JFileChooser();chooser.isMultiSelectionEnabled=true
        if(chooser.showOpenDialog(null)!=JFileChooser.APPROVE_OPTION)return
        val files=chooser.selectedFiles.toList()
        run("导入原件") {val accepted=mutableListOf<String>();val failed=mutableListOf<String>()
            for(f in files)try{accepted+=workspace.importImage(f.name,readBounded(f.toPath(),EditorRenderer.MAX_BYTES))}catch(e:Exception){failed+="${f.name}: ${if(e is DataProblem)e.status else e.javaClass.simpleName}"}
            current=accepted.lastOrNull()?:current;selection=accepted.toSet();if(failed.isNotEmpty())problem="部分导入未完成：\n"+failed.joinToString("\n")
        }
    }
    suspend fun close() {active?.cancelAndJoin();pending?.cancelAndJoin();editor.drain();flush();infrastructure.closeAndDrain();withContext(Dispatchers.IO){workspace.close()};http.dispatcher.executorService.shutdown();http.connectionPool.evictAll()}
}

internal fun launchStudio(args:Array<String>) {
    fun option(name:String)=args.indexOf(name).takeIf{it>=0}?.let{args.getOrNull(it+1)}
    StudioBrand.configureDefaultWindows()
    val home=Paths.get(System.getenv("LOCALAPPDATA"),"CCBStudioSuite")
    val root=option("--studio-root")?.let(Paths::get)?:home.resolve("workspace-v2")
    val runtime=option("--runtime-root")?.let(Paths::get)?:home.resolve("runtime")
    val workspace=try{StudioWorkspace.openStudio(root)}catch(e:Exception){javax.swing.JOptionPane.showMessageDialog(null,"工作区无法安全打开；现有文件保留。\n${if(e is DataProblem)e.status else e.javaClass.simpleName}","CCB Studio Suite",javax.swing.JOptionPane.ERROR_MESSAGE);return}
    application {
        val scope=rememberCoroutineScope();val c=remember{WorkbenchController(workspace,scope,DesktopNovelAiInfrastructure(runtime.resolve("catalog")),runtime.resolve("request-budget"))}
        var closing by remember{mutableStateOf(false)}
        fun close(){if(!closing){closing=true;scope.launch{try{c.close();exitApplication()}catch(e:Exception){closing=false;c.problem="保存或排水尚未完成，窗口保持打开"}}}}
        Window(onCloseRequest=::close,title="CCB Studio Suite · 工作台",undecorated=DesktopWindowChrome.isWindows,icon=StudioBrand.painter,state=rememberWindowState(width=1320.dp,height=880.dp)) {
            val chrome=rememberDesktopWindowChrome(window,::close);SideEffect{chrome.appearance(true)}
            MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=Color(0xffe6edf3))) {Workbench(c,chrome,closing)}
        }
    }
}

private class WorkbenchChrome(private val chrome:DesktopWindowChrome) {
    var width=0f;var height=0f;var title:ChromeRect?=null;var icon:ChromeRect?=null
    val buttons=linkedMapOf<String,ChromeRect>();val captions=linkedMapOf<ChromeHit,ChromeRect>()
    fun rect(c:LayoutCoordinates)=c.boundsInWindow().let{ChromeRect(it.left,it.top,it.right,it.bottom)}
    fun publish(){chrome.updateLayout(DesktopChromeLayout(width,height,title,buttons.values.toList(),captions.toMap(),icon))}
}
@Composable internal fun Workbench(c:WorkbenchController,chrome:DesktopWindowChrome,closing:Boolean) {
    Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground) {
    val r=remember{WorkbenchChrome(chrome)};val native by chrome.state.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact=maxWidth<760.dp
    DisposableEffect(compact){r.buttons.clear();r.publish();onDispose{r.buttons.clear();r.publish()}}
    Column(Modifier.fillMaxSize().background(Ink).onGloballyPositioned{r.width=it.size.width.toFloat();r.height=it.size.height.toFloat();r.publish()}) {
        Row(Modifier.fillMaxWidth().height(44.dp).background(Panel).onGloballyPositioned{r.title=r.rect(it);r.publish()},verticalAlignment=Alignment.CenterVertically) {
            Image(StudioBrand.painter,"CCB 蓝色 logo",Modifier.padding(9.dp).size(26.dp).onGloballyPositioned{r.icon=r.rect(it);r.publish()})
            if(compact)Box(Modifier.onGloballyPositioned{r.buttons["导航"]=r.rect(it);r.publish()}){CompactChoice("",listOf("创作","AI 设计","图库","编辑","账户"),c.view,{it}){c.view=it}}
            else listOf("创作","AI 设计","图库","编辑","账户").forEach {label -> Box(Modifier.onGloballyPositioned{r.buttons[label]=r.rect(it);r.publish()}){StudioAction(label,selected=c.view==label){c.view=label}} }
            Spacer(Modifier.weight(1f))
            listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE).forEach {hit ->
                val label=when(hit){ChromeHit.MINIMIZE->"—";ChromeHit.MAXIMIZE->if(native.maximized)"❐"else"□";else->"×"}
                Box(Modifier.width(44.dp).fillMaxHeight().onGloballyPositioned{r.captions[hit]=r.rect(it);r.publish()}.clickable{desktopCaptionCommand(hit,native.maximized)?.let(chrome::command)},contentAlignment=Alignment.Center){Text(label)}
            }
        }
        Divider()
        Box(Modifier.weight(1f).fillMaxWidth()) {when(c.view){"AI 设计"->DesignWorkspace(c);"图库"->GalleryWorkspace(c);"编辑"->TypesetWorkspace(c);"账户"->AccountsWorkspace(c);else->CreationWorkspace(c)}}
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(closing)"正在保存并排水…"else c.message,Modifier.weight(1f),fontSize=12.sp,color=Quiet)
            Text(if(c.saved)"已保存" else "保存中…",fontSize=12.sp)
            if(c.busy)TextButton(onClick={c.active?.cancel()}){Text("停止")}
        }
    }
    }
    c.problem?.let {AlertDialog(onDismissRequest={c.problem=null},title={Text("操作回执")},text={SelectionContainer{Text(it)}},confirmButton={TextButton(onClick={c.problem=null}){Text("关闭")}})}
    }
}

@Composable private fun CreationWorkspace(c:WorkbenchController) {
    val translated=c.state.creation!!.translatedTags
    var paste by remember{mutableStateOf<String?>(null)}
    var clear by remember{mutableStateOf(false)}
    val tokens by produceState<NovelAiPromptTokenUsage?>(null,c.draft.toPromptPlan(),c.draft.selectedModel){value=runCatching{c.infrastructure.countTokens(c.draft)}.getOrNull()}
    DesktopStudioWorkspace(compact={expand -> StudioAction("展开图片预览",onClick=expand)},editor={
        StudioActions{StudioAction("AI 设计"){c.view="AI 设计"};StudioAction("导入图片",!c.busy){c.importImages()};StudioAction("历史图库"){c.view="图库"}}
        Spacer(Modifier.height(12.dp));StudioChips("",listOf("Prompt","角色","参数","图像引导"),c.section,{it}){c.section=it}
        Spacer(Modifier.height(12.dp))
        when(c.section) {
            "角色"->DesktopStudioCharacters(c.draft,c.infrastructure,translated,c::edit)
            "参数"->{
                CompactChoice("全局默认生图模型",NovelAiImageModel.entries,c.state.creation!!.defaultImageModel,{it.displayName}){c.defaults(model=it)}
                StudioGenerationSettings(c.draft,c::edit)
            }
            "图像引导"->GuidanceWorkspace(c)
            else->{
                StudioSwitch("中文注释",translated){c.defaults(translated=!translated)}
                ImageCardChoice(c)
                StudioPromptField("画风",c.draft.stylePrompt,c.infrastructure,translated){v->c.edit{it.copy(stylePrompt=v)}}
                Spacer(Modifier.height(10.dp));StudioPromptField("基础 Prompt",c.draft.basePrompt,c.infrastructure,translated){v->c.edit{it.copy(basePrompt=v)}}
                Spacer(Modifier.height(10.dp));StudioPromptField("补充 Prompt",c.draft.extraPrompt,c.infrastructure,translated){v->c.edit{it.copy(extraPrompt=v)}}
                Spacer(Modifier.height(10.dp));StudioPromptField("负向 Prompt",c.draft.negativePrompt,c.infrastructure,translated){v->c.edit{it.copy(negativePrompt=v)}}
                Spacer(Modifier.height(12.dp));StudioActions{StudioAction("撤销",!c.busy && c.state.creation!!.undo.isNotEmpty()){c.undo()};StudioAction("重做",!c.busy && c.state.creation!!.redo.isNotEmpty()){c.undo(true)};StudioAction("复制正向"){desktopCopyText(c.draft.copyPositivePrompt())}}
                StudioActions {StudioAction("粘贴并预览",!c.busy){runCatching{java.awt.Toolkit.getDefaultToolkit().systemClipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String}.onSuccess{paste=it}.onFailure{c.problem="剪贴板文字暂不可读取"}};StudioAction("清空 Prompt",!c.busy){clear=true}}
            }
        }
    },footer={Column {
        tokens?.let{StatusText("正向 ${it.positive}/${it.limit} · 负向 ${it.negative}/${it.limit}",if(it.positive>it.limit||it.negative>it.limit)DesktopBootstrapColors.warning else Quiet)}
        AccountChoice(c,"NAI")
        val cost=runCatching{NovelAiImageCostEstimator.estimate(c.draft.activeSettings,c.usage,c.draft.imageGuidance,c.vibeMisses())}.getOrNull()
        StatusText(cost?.let{"预计 ${it.anlas} Anlas · ${it.kind}"}?:"参数无效，暂无法估算")
        StudioActions{StatusText(c.usage?.let{"额度 ${it.anlas} Anlas · 刷新于 ${java.time.Instant.ofEpochMilli(c.usageAt!!)}"}?:"账户额度未知，未自动请求")
            StudioAction("刷新额度",!c.busy && c.naiId!=null){c.refreshUsage()}}
        StatusText("${c.draft.selectedModel.displayName} · ${c.draft.activeSettings.count} 张 · 实际账户请求；遵守全局调用预算")
        StudioAction(if(c.busy)"停止" else "生成图片",enabled=c.busy||c.naiId!=null,style=StudioActionStyle.PRIMARY){if(c.busy)c.active?.cancel() else c.generate()}
    }},preview={rail -> ResultPreview(c,rail)})
    paste?.let{text->AlertDialog(onDismissRequest={paste=null},title={Text("确认粘贴覆盖")},text={SelectionContainer{Text(text,Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()))}},confirmButton={TextButton(onClick={c.pastePrompt(text);paste=null}){Text("确认覆盖，可撤销")}},dismissButton={TextButton(onClick={paste=null}){Text("取消")}})}
    if(clear)AlertDialog(onDismissRequest={clear=false},title={Text("清空 Prompt？")},text={Text("保留图像引导和生成设置。可撤销恢复。")},confirmButton={TextButton(onClick={c.clearPromptDraft();clear=false}){Text("确认清空")}},dismissButton={TextButton(onClick={clear=false}){Text("取消")}})
}

@Composable private fun AccountChoice(c:WorkbenchController,service:String) {
    val accounts=c.state.accounts.filter{it.service==service}
    if(accounts.isEmpty())StudioAction("配置 $service 账户"){c.view="账户"}
    else CompactChoice(service,accounts,accounts.firstOrNull{it.id==if(service=="LLM")c.llmId else c.naiId},{"${it.alias} · ${it.model}"}){c.chooseAccount(service,it.id)}
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
    val interim by produceState<ImageBitmap?>(null,c.intermediate){value=withContext(Dispatchers.IO){c.intermediate?.let{bytes->runCatching{val decoded=EditorRenderer.decode(bytes);try{decoded.toComposeImageBitmap()}finally{decoded.flush()}}.getOrNull()}}}
    Column(Modifier.fillMaxSize()) {
        if(!rail)c.current?.let{id->StudioActions{
            StudioAction("图像工具",!c.busy){scope.launch{toolImage=id to withContext(Dispatchers.IO){c.workspace.bytes(id)}}}
            StudioAction("用于图像引导",!c.busy){c.section="图像引导"}
            StudioAction("增强 / 放大",!c.busy){processing=id}
            StudioAction("反推 Prompt",!c.busy && c.llmId!=null){reversing=true;c.reverseImage(id)}
            StudioAction("元数据与复用",!c.busy){metadata=id}
            StudioAction("全图查看"){viewing=c.assets.map{it.request().id().toString()};initial=viewing!!.indexOf(id).coerceAtLeast(0)}
        }}
        if(!rail) {if(interim!=null && c.busy) {StatusText("生成中预览 · 尚未保存为原件");Image(interim!!,"生成中预览",Modifier.weight(1f).fillMaxWidth(),contentScale=ContentScale.Fit)}else c.current?.let{id->DesktopImageZoomSurface("asset:$id",c::readAsset,Modifier.weight(1f).fillMaxWidth(),onOpenViewer={viewing=c.assets.map{it.request().id().toString()};initial=viewing!!.indexOf(id).coerceAtLeast(0)})}?:AssetImage(c,null,Modifier.weight(1f).fillMaxWidth())}
        val modifier=if(rail)Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(92.dp)
        if(rail)LazyColumn(modifier){items(c.assets.asReversed(),key={it.request().id().toString()}){a->val id=a.request().id().toString();AssetImage(c,id,Modifier.size(80.dp).clickable{c.current=id})}}
        else LazyRow(modifier){items(c.assets.asReversed(),key={it.request().id().toString()}){a->val id=a.request().id().toString();AssetImage(c,id,Modifier.padding(4.dp).size(84.dp).border(if(c.current==id)2.dp else 0.dp,Gold).clickable{c.current=id})}}
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

@Composable private fun DesignWorkspace(c:WorkbenchController) {
    val creation=c.state.creation!!;val conversation=creation.conversations.firstOrNull{it.id==creation.currentConversationId}
    var candidate by remember{mutableStateOf<Pair<String,String>?>(null)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        StudioActions {StudioAction("返回创作"){c.view="创作"};StudioAction("新对话",!c.busy){c.newConversation()};AccountChoice(c,"LLM")}
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
                    turn.reply?.let {reply->DesktopDesignResult(reply);StudioAction("应用候选到创作",!c.busy,style=StudioActionStyle.PRIMARY){candidate=conversation!!.id to turn.id}}
                    if(turn.status!=NovelAiDesignTurnStatus.COMPLETED)StatusText(turn.error.ifBlank{"正在设计…"},Gold)
                    if(turn.status in listOf(NovelAiDesignTurnStatus.FAILED,NovelAiDesignTurnStatus.CANCELLED))StudioAction("准备重试分支",!c.busy){c.retry(conversation!!.id,turn.id)}
                }
            }
            if(c.stream.isNotBlank())item{StudioDisclosure("本轮流式结果",initiallyOpen=true){SelectionContainer{Text(c.stream)}}}
            if(c.reasoning.isNotBlank())item{StudioDisclosure("思考过程"){SelectionContainer{Text(c.reasoning)}}}
        }
        StudioSwitch("附加当前基础 Prompt 与启用角色",c.attach){c.attach(!c.attach)}
        DesktopDesignField("画面需求 / 继续修改",c.composer,"composer",c::input)
        StudioAction(if(c.busy)"停止" else "发送",enabled=c.busy || (c.composer.isNotBlank() && c.llmId!=null),style=StudioActionStyle.PRIMARY){if(c.busy)c.active?.cancel()else c.design()}
    }
    candidate?.let{(conversationId,turnId)->
        val reply=creation.conversations.single{it.id==conversationId}.turns.single{it.id==turnId}.reply!!
        val proposed=c.draft.applyDesignedPromptPlan(reply.plan,reply.targetImageModel)
        AlertDialog(onDismissRequest={candidate=null},title={Text("确认应用设计候选")},text={
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("应用后可以撤销；画风、补充词与手动设置按原 Studio 规则保留。")
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
    var editing by remember{mutableStateOf<AccountProfile?>(null)}
    var alias by remember{mutableStateOf("")};var service by remember{mutableStateOf("LLM")}
    var endpoint by remember{mutableStateOf("https://api.siliconflow.cn/v1")};var model by remember{mutableStateOf("")};var secret by remember{mutableStateOf("")}
    var multimodal by remember{mutableStateOf(false)};var visionId by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("账户与模型",fontSize=22.sp);Text("凭据只保存在本机受保护存储。档案选择不会自动调用 API。")
        c.state.accounts.forEach {a->StudioAction("${a.alias} · ${a.service} · ${a.model}"){editing=a;alias=a.alias;service=a.service;endpoint=a.endpoint;model=a.model;secret="";multimodal=a.isMultimodal;visionId=a.visionAccountId}}
        StudioAction("新建档案"){editing=null;alias="";secret="";multimodal=false;visionId=null}
        StudioChips("服务",listOf("LLM","NAI"),service,{it}){service=it;endpoint=if(it=="NAI")"https://image.novelai.net" else "https://api.siliconflow.cn/v1";model=if(it=="NAI")NovelAiImageModel.V4_5_FULL.apiId else ""}
        OutlinedTextField(alias,{alias=it},label={Text("档案名称")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(endpoint,{endpoint=it},label={Text("HTTPS API 地址")},enabled=service=="LLM",modifier=Modifier.fillMaxWidth())
        OutlinedTextField(model,{model=it},label={Text("模型 ID")},modifier=Modifier.fillMaxWidth())
        if(service=="LLM") {
            StudioSwitch("此模型支持图片输入",multimodal){multimodal=!multimodal}
            if(!multimodal) {
                val vision=c.state.accounts.filter{it.service=="LLM"&&it.isMultimodal&&it.id!=editing?.id}
                CompactChoice("关联视觉模型",vision,vision.firstOrNull{it.id==visionId},{it.alias}){visionId=it.id}
                if(visionId!=null)StudioAction("清除视觉关联"){visionId=null}
                StatusText("图片反推需要可用视觉模型；未配置时明确失败，不以文字猜测图片。")
            }
        }
        OutlinedTextField(secret,{secret=it},label={Text(if(editing?.secretRef!=null)"新凭据（留空保留已有）"else"API 凭据")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        StudioAction("安全保存并选用",enabled=!c.busy && alias.isNotBlank() && model.isNotBlank() && (secret.isNotBlank()||editing?.secretRef!=null),style=StudioActionStyle.PRIMARY){c.saveAccount(editing,alias,service,endpoint,model,secret,multimodal,visionId);secret=""}
        StudioAction("返回创作"){c.view="创作"}
    }
}
