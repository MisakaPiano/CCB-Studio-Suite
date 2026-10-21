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
import java.nio.file.*
import javax.swing.JFileChooser

internal class WorkbenchController(val workspace:StudioWorkspace,val scope:CoroutineScope,val infrastructure:DesktopNovelAiInfrastructure,budgetRoot:Path,
    val http:okhttp3.OkHttpClient=studioHttpClient(),admissionOverride:RequestAdmission?=null) {
    val session=CreationSession(workspace)
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
    private fun guidanceFor(account:AccountProfile?)=DesktopNovelAiGuidance(guidanceRoot,::readAsset,{
        vault.use(requireNotNull(account?.secretRef)){it.decodeToString()}
    },admission,http)
    fun visibleAssets():List<AssetStore.Asset> {
        val s=workspace.state;val copies=(s.savedCopies+s.jobs.flatMap{it.composedResults}).toSet()
        return workspace.catalog().filter{!it.request().project().contentEquals("CCB_REQUEST_PAYLOAD_V1".toByteArray()) && (it.request().kind()==AssetStore.Kind.ORIGINAL || it.request().id().toString() in copies)}
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
    init {if(current==null)current=assets.lastOrNull()?.request()?.id()?.toString()}
    private fun changed(){saved=false;pending?.cancel();pending=scope.launch {delay(350);try{flush()}catch(e:Exception){problem="草稿未能保存，保留当前输入；请先处理数据错误"}}}
    fun edit(change:(NovelAiStudioDraft)->NovelAiStudioDraft){draft=change(draft);changed()}
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
    fun saveAccount(existing:AccountProfile?,alias:String,service:String,endpoint:String,model:String,key:String)=run("保存账户") {
        val ref=if(key.isNotBlank()) {val bytes=key.trim().toByteArray();try{vault.put(bytes)}finally{bytes.fill(0)}} else existing?.secretRef
        val a=AccountProfile(id=existing?.id?:newId(),alias=alias,service=service,provider=if(service=="NAI")"NOVELAI" else "OPENAI_COMPATIBLE",endpoint=endpoint.trim().trimEnd('/'),model=model,secretRef=ref)
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
    suspend fun close() {active?.cancelAndJoin();pending?.cancelAndJoin();flush();infrastructure.closeAndDrain();withContext(Dispatchers.IO){workspace.close()};http.dispatcher.executorService.shutdown();http.connectionPool.evictAll()}
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
    val r=remember{WorkbenchChrome(chrome)};val native by chrome.state.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact=maxWidth<760.dp
    DisposableEffect(compact){r.buttons.clear();r.publish();onDispose{r.buttons.clear();r.publish()}}
    Column(Modifier.fillMaxSize().background(Ink).onGloballyPositioned{r.width=it.size.width.toFloat();r.height=it.size.height.toFloat();r.publish()}) {
        Row(Modifier.fillMaxWidth().height(44.dp).background(Panel).onGloballyPositioned{r.title=r.rect(it);r.publish()},verticalAlignment=Alignment.CenterVertically) {
            Image(StudioBrand.painter,"CCB 蓝色 logo",Modifier.padding(9.dp).size(26.dp).onGloballyPositioned{r.icon=r.rect(it);r.publish()})
            if(compact)Box(Modifier.onGloballyPositioned{r.buttons["导航"]=r.rect(it);r.publish()}){CompactChoice("",listOf("创作","AI 设计","图库","账户"),c.view,{it}){c.view=it}}
            else listOf("创作","AI 设计","图库","账户").forEach {label -> Box(Modifier.onGloballyPositioned{r.buttons[label]=r.rect(it);r.publish()}){StudioAction(label,selected=c.view==label){c.view=label}} }
            Spacer(Modifier.weight(1f))
            listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE).forEach {hit ->
                val label=when(hit){ChromeHit.MINIMIZE->"—";ChromeHit.MAXIMIZE->if(native.maximized)"❐"else"□";else->"×"}
                Box(Modifier.width(44.dp).fillMaxHeight().onGloballyPositioned{r.captions[hit]=r.rect(it);r.publish()}.clickable{desktopCaptionCommand(hit,native.maximized)?.let(chrome::command)},contentAlignment=Alignment.Center){Text(label)}
            }
        }
        Divider()
        Box(Modifier.weight(1f).fillMaxWidth()) {when(c.view){"AI 设计"->DesignWorkspace(c);"图库"->GalleryWorkspace(c);"账户"->AccountsWorkspace(c);else->CreationWorkspace(c)}}
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(closing)"正在保存并排水…"else c.message,Modifier.weight(1f),fontSize=12.sp,color=Quiet)
            Text(if(c.saved)"已保存" else "保存中…",fontSize=12.sp)
            if(c.busy)TextButton(onClick={c.active?.cancel()}){Text("停止")}
        }
    }
    }
    c.problem?.let {AlertDialog(onDismissRequest={c.problem=null},title={Text("操作回执")},text={SelectionContainer{Text(it)}},confirmButton={TextButton(onClick={c.problem=null}){Text("关闭")}})}
}

@Composable private fun CreationWorkspace(c:WorkbenchController) {
    var translated by remember{mutableStateOf(false)}
    DesktopStudioWorkspace(compact={expand -> StudioAction("展开图片预览",onClick=expand)},editor={
        StudioActions{StudioAction("AI 设计"){c.view="AI 设计"};StudioAction("导入图片",!c.busy){c.importImages()};StudioAction("历史图库"){c.view="图库"}}
        Spacer(Modifier.height(12.dp));StudioChips("",listOf("Prompt","角色","参数","图像引导"),c.section,{it}){c.section=it}
        Spacer(Modifier.height(12.dp))
        when(c.section) {
            "角色"->DesktopStudioCharacters(c.draft,c.infrastructure,translated,c::edit)
            "参数"->StudioGenerationSettings(c.draft,c::edit)
            "图像引导"->GuidanceWorkspace(c)
            else->{
                StudioSwitch("中文注释",translated){translated=!translated}
                StudioPromptField("画风",c.draft.stylePrompt,c.infrastructure,translated){v->c.edit{it.copy(stylePrompt=v)}}
                Spacer(Modifier.height(10.dp));StudioPromptField("基础 Prompt",c.draft.basePrompt,c.infrastructure,translated){v->c.edit{it.copy(basePrompt=v)}}
                Spacer(Modifier.height(10.dp));StudioPromptField("补充 Prompt",c.draft.extraPrompt,c.infrastructure,translated){v->c.edit{it.copy(extraPrompt=v)}}
                Spacer(Modifier.height(10.dp));StudioPromptField("负向 Prompt",c.draft.negativePrompt,c.infrastructure,translated){v->c.edit{it.copy(negativePrompt=v)}}
                Spacer(Modifier.height(12.dp));StudioActions{StudioAction("撤销",!c.busy && c.state.creation!!.undo.isNotEmpty()){c.undo()};StudioAction("重做",!c.busy && c.state.creation!!.redo.isNotEmpty()){c.undo(true)};StudioAction("复制正向"){desktopCopyText(c.draft.copyPositivePrompt())}}
            }
        }
    },footer={Column {
        AccountChoice(c,"NAI")
        val cost=runCatching{NovelAiImageCostEstimator.estimate(c.draft.activeSettings,c.usage,c.draft.imageGuidance,c.vibeMisses())}.getOrNull()
        StatusText(cost?.let{"预计 ${it.anlas} Anlas · ${it.kind}"}?:"参数无效，暂无法估算")
        StudioActions{StatusText(c.usage?.let{"额度 ${it.anlas} Anlas · 刷新于 ${java.time.Instant.ofEpochMilli(c.usageAt!!)}"}?:"账户额度未知，未自动请求")
            StudioAction("刷新额度",!c.busy && c.naiId!=null){c.refreshUsage()}}
        StatusText("${c.draft.selectedModel.displayName} · ${c.draft.activeSettings.count} 张 · 实际账户请求；遵守全局调用预算")
        StudioAction(if(c.busy)"停止" else "生成图片",enabled=c.busy||c.naiId!=null,style=StudioActionStyle.PRIMARY){if(c.busy)c.active?.cancel() else c.generate()}
    }},preview={rail -> ResultPreview(c,rail)})
}

@Composable private fun AccountChoice(c:WorkbenchController,service:String) {
    val accounts=c.state.accounts.filter{it.service==service}
    if(accounts.isEmpty())StudioAction("配置 $service 账户"){c.view="账户"}
    else CompactChoice(service,accounts,accounts.firstOrNull{it.id==if(service=="LLM")c.llmId else c.naiId},{"${it.alias} · ${it.model}"}){c.chooseAccount(service,it.id)}
}
@Composable private fun AssetImage(c:WorkbenchController,id:String?,modifier:Modifier) {
    val bitmap by produceState<ImageBitmap?>(null,id){value=withContext(Dispatchers.IO){if(id==null)null else runCatching{val image=EditorRenderer.decode(c.workspace.bytes(id));try{image.toComposeImageBitmap()}finally{image.flush()}}.getOrNull()}}
    Box(modifier,contentAlignment=Alignment.Center){bitmap?.let{Image(it,"原始图片预览",Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}?:Text("生成或导入图片后在这里查看",color=Quiet)}
}
@Composable private fun ResultPreview(c:WorkbenchController,rail:Boolean) {
    var toolImage by remember{mutableStateOf<Pair<String,ByteArray>?>(null)}
    val scope=rememberCoroutineScope()
    val interim by produceState<ImageBitmap?>(null,c.intermediate){value=withContext(Dispatchers.IO){c.intermediate?.let{bytes->runCatching{val decoded=EditorRenderer.decode(bytes);try{decoded.toComposeImageBitmap()}finally{decoded.flush()}}.getOrNull()}}}
    Column(Modifier.fillMaxSize()) {
        if(!rail)c.current?.let{id->StudioActions{
            StudioAction("图像工具",!c.busy){scope.launch{toolImage=id to withContext(Dispatchers.IO){c.workspace.bytes(id)}}}
            StudioAction("用于图像引导",!c.busy){c.section="图像引导"}
        }}
        if(!rail) {if(interim!=null && c.busy) {StatusText("生成中预览 · 尚未保存为原件");Image(interim!!,"生成中预览",Modifier.weight(1f).fillMaxWidth(),contentScale=ContentScale.Fit)}else AssetImage(c,c.current,Modifier.weight(1f).fillMaxWidth())}
        val modifier=if(rail)Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(92.dp)
        if(rail)LazyColumn(modifier){items(c.assets.asReversed(),key={it.request().id().toString()}){a->val id=a.request().id().toString();AssetImage(c,id,Modifier.size(80.dp).clickable{c.current=id})}}
        else LazyRow(modifier){items(c.assets.asReversed(),key={it.request().id().toString()}){a->val id=a.request().id().toString();AssetImage(c,id,Modifier.padding(4.dp).size(84.dp).border(if(c.current==id)2.dp else 0.dp,Gold).clickable{c.current=id})}}
        StatusText("${c.assets.size} 份原件 · 原始字节保全")
    }
    toolImage?.let{(id,bytes)->DesktopImageToolsDialog(bytes,c.picker,{toolImage=null},onProcessedImage={c.saveToolCopy(id,it)})}
}

@Composable private fun DesignWorkspace(c:WorkbenchController) {
    val creation=c.state.creation!!;val conversation=creation.conversations.firstOrNull{it.id==creation.currentConversationId}
    var candidate by remember{mutableStateOf<Pair<String,String>?>(null)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        StudioActions {StudioAction("返回创作"){c.view="创作"};StudioAction("新对话",!c.busy){c.newConversation()};AccountChoice(c,"LLM")}
        Spacer(Modifier.height(8.dp));CompactChoice("设计历史",creation.conversations,conversation,{it.title}){c.selectConversation(it.id)}
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

@Composable private fun GalleryWorkspace(c:WorkbenchController) {
    var query by remember{mutableStateOf("")};val visible=c.assets.filter{it.request().name().contains(query,true)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        StudioActions{StudioAction("导入图片",!c.busy){c.importImages()};StudioAction("返回创作"){c.view="创作"}}
        OutlinedTextField(query,{query=it},label={Text("按文件名查找")},modifier=Modifier.fillMaxWidth())
        Row(Modifier.weight(1f)) {
            LazyColumn(Modifier.width(280.dp).fillMaxHeight()){items(visible.asReversed(),key={it.request().id().toString()}){a->val id=a.request().id().toString()
                Row(Modifier.fillMaxWidth().clickable{c.current=id}.padding(6.dp),verticalAlignment=Alignment.CenterVertically){Checkbox(id in c.selection,{c.selection=if(it)c.selection+id else c.selection-id});AssetImage(c,id,Modifier.size(64.dp));Text(a.request().name(),Modifier.weight(1f),fontSize=12.sp)}
            }}
            AssetImage(c,c.current,Modifier.weight(1f).fillMaxHeight())
        }
        StudioActions{StudioAction("全选"){c.selection=visible.map{it.request().id().toString()}.toSet()};StudioAction("全不选"){c.selection=emptySet()};StudioAction("反选"){c.selection=visible.map{it.request().id().toString()}.toSet()-c.selection};StatusText("${c.selection.size} 项已选")}
    }
}

@Composable private fun AccountsWorkspace(c:WorkbenchController) {
    var editing by remember{mutableStateOf<AccountProfile?>(null)}
    var alias by remember{mutableStateOf("")};var service by remember{mutableStateOf("LLM")}
    var endpoint by remember{mutableStateOf("https://api.siliconflow.cn/v1")};var model by remember{mutableStateOf("")};var secret by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("账户与模型",fontSize=22.sp);Text("凭据只保存在本机受保护存储。档案选择不会自动调用 API。")
        c.state.accounts.forEach {a->StudioAction("${a.alias} · ${a.service} · ${a.model}"){editing=a;alias=a.alias;service=a.service;endpoint=a.endpoint;model=a.model;secret=""}}
        StudioAction("新建档案"){editing=null;alias="";secret=""}
        StudioChips("服务",listOf("LLM","NAI"),service,{it}){service=it;endpoint=if(it=="NAI")"https://image.novelai.net" else "https://api.siliconflow.cn/v1";model=if(it=="NAI")NovelAiImageModel.V4_5_FULL.apiId else ""}
        OutlinedTextField(alias,{alias=it},label={Text("档案名称")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(endpoint,{endpoint=it},label={Text("HTTPS API 地址")},enabled=service=="LLM",modifier=Modifier.fillMaxWidth())
        OutlinedTextField(model,{model=it},label={Text("模型 ID")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(secret,{secret=it},label={Text(if(editing?.secretRef!=null)"新凭据（留空保留已有）"else"API 凭据")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        StudioAction("安全保存并选用",enabled=!c.busy && alias.isNotBlank() && model.isNotBlank() && (secret.isNotBlank()||editing?.secretRef!=null),style=StudioActionStyle.PRIMARY){c.saveAccount(editing,alias,service,endpoint,model,secret);secret=""}
        StudioAction("返回创作"){c.view="创作"}
    }
}
