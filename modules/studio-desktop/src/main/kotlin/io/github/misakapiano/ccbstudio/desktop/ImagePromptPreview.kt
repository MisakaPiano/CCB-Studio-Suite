package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.UUID

internal data class ImagePromptReading(val status:String,val sections:List<PromptSection>,val context:PromptImageContext,val explanation:String) {
    fun text()=sections.joinToString("\n\n"){it.text}
}

internal fun promptReadingLabel(status:String)=when(status) {
    "VERIFIED_HISTORY"->"已核验生成记录"
    "VERIFIED_SOURCE_HISTORY"->"已核验来源图片的生成记录"
    "HISTORY_WITHOUT_REQUEST_HASH"->"生成记录（旧请求无摘要）"
    "READABLE"->"图片附带的生成参数"
    "MISSING"->"未找到生成参数"
    "CORRUPT"->"生成参数损坏"
    "UNSUPPORTED"->"尚不支持此参数格式"
    "READ_ERROR"->"生成参数读取失败"
    else->"来源状态未识别（$status）"
}

/** Read-only projection. A broken linked receipt is not silently replaced with embedded PNG text. */
internal fun readImagePrompt(c:WorkbenchController,id:String):ImagePromptReading = c.workspace.operation {
    val asset=c.workspace.assets.read(UUID.fromString(id)).require()
    val origin=imageOrigin(c.workspace,id)
    val sourceNotice=if(origin.derived)"当前图片经过后续处理；以下参数属于来源图片 ${origin.sourceId.take(8)}，不描述后续编辑。"else""
    val sections=mutableListOf<PromptSection>()
    fun part(key:String,label:String,text:String?,role:String?=null,attributes:Map<String,String> = emptyMap()) {
        if(!text.isNullOrBlank())sections+=PromptSection(key,label,text,role,attributes)
    }
    val receipt=origin.receipt
    if(receipt!=null) {
        val job=requireNotNull(origin.job)
        // Stored recipe uses asset references rather than expanding large image payloads into the UI.
        val recipe=receipt.recipe;val raw=documentJson.encodeToString(recipe)
        part("style","风格 Prompt",recipe.stylePrompt);part("base","基础 Prompt",recipe.basePrompt)
        part("extra","额外 Prompt",recipe.extraPrompt);part("negative","负向 Prompt",recipe.negativePrompt)
        recipe.characters.forEachIndexed{i,ch->
            val role="role:$i";val attributes=mapOf("原始角色位置" to (i+1).toString(),"位置" to ch.center.toString(),"启用" to ch.enabled.toString())
            part("$role:positive","角色正向",ch.prompt,role,attributes);part("$role:negative","角色负向",ch.negativePrompt,role,attributes)
        }
        val design=runCatching{DesignProvenance(c.workspace).forGeneration(job)}
        val designLabel=when{design.isFailure->"关联无法核验";design.getOrNull()==null->"未记录";design.getOrThrow()!!.applied==DesignPromptSnapshot.of(recipe)->"已核验 · 应用后未改 Prompt";else->"已核验 · 应用后有修改"}
        val parameters=RecipeComparison.inspect(raw).fields.filter{(key,value)->
            value.kind !in setOf("object","array") && (key.startsWith("/settings/") || key.startsWith("/imageGuidance/") || key=="/naturalLanguageMode")
        }.mapValues{it.value.value}+mapOf("实际 Seed" to origin.seed.toString(),"图片尺寸" to recipe.settings.imageSize().let{"${it.width}×${it.height}"},"AI 设计关联" to designLabel)+if(origin.derived)mapOf("来源图片" to origin.sourceId)else emptyMap()
        val batchParameters=receipt.batchOrigin?.let{mapOf("批次分组" to it.groupName,"批次方向 / 轮次" to "${it.direction} / ${it.round}","批次编号" to it.batchId)}.orEmpty()
        val status=if(receipt.wireSha256.isNotBlank())if(origin.derived)"VERIFIED_SOURCE_HISTORY"else"VERIFIED_HISTORY"else"HISTORY_WITHOUT_REQUEST_HASH"
        return@operation ImagePromptReading(status,sections,PromptImageContext(id,asset.sha256(),status,parameters+batchParameters,raw,receipt.wireSha256.takeIf{it.isNotBlank()}),sourceNotice+"读取保存的生成 Recipe；${if(receipt.wireSha256.isNotBlank())"请求摘要已核验"else"旧请求没有校验摘要"}。AI 设计：$designLabel；无明确记录时不推断关联。")
    }
    val read=c.metadataFile(origin.sourceId,::inspectMetadata);val m=read.metadata
    val parameters=linkedMapOf<String,String>()
    if(origin.derived)parameters["来源图片"]=origin.sourceId
    if(m!=null) {
        part("base","基础 Prompt",m.positivePrompt);part("negative","负向 Prompt",m.negativePrompt)
        m.characters.forEachIndexed{i,ch->val role="role:$i";val attributes=mapOf("原始角色位置" to (i+1).toString(),"位置" to ch.center.toString())
            part("$role:positive","角色正向",ch.prompt,role,attributes);part("$role:negative","角色负向",ch.negativePrompt,role,attributes)
        }
        parameters["图片尺寸"]="${m.width}×${m.height}"
        m.seed?.let{parameters["实际 Seed"]=it.toString()}
        m.settings.let{s->listOf("/settings/model" to s.model?.apiId,"/settings/steps" to s.steps,"/settings/guidance" to s.guidance,"/settings/cfgRescale" to s.cfgRescale,"/settings/sampler" to s.sampler?.name,"/settings/useCharacterPositions" to s.useCharacterPositions).forEach{(key,value)->if(value!=null)parameters[key]=value.toString()}}
    }
    ImagePromptReading(read.status,sections,PromptImageContext(id,asset.sha256(),read.status,parameters),sourceNotice+read.explanation+"；嵌入参数不是已核验的完整发送请求。")
}

internal fun saveImagePrompt(c:WorkbenchController,id:String,addToBoard:Boolean):String {
    val read=readImagePrompt(c,id);require(read.sections.isNotEmpty()){ "图片没有可比较的非空 Prompt：${read.status}" }
    val name=c.library.labels(id).alias.ifBlank{c.workspace.assets.read(UUID.fromString(id)).require().request().name()}
    val archived=AssetArchive(c.workspace).ids()
    val existing=c.library.repository.prompts().firstOrNull{a->val pid=a.request().id().toString();pid !in archived && c.library.repository.prompt(pid).let{p->p.image==read.context && p.sections==read.sections}}
    val prompt=existing?.request()?.id()?.toString()?:c.library.repository.savePrompt(read.text(),name,"image:$id:${read.context.sha256}",sections=read.sections,image=read.context)
    if(addToBoard)PromptReferences(c.library.repository).add(prompt)
    c.library.refresh();return prompt
}

@Composable internal fun ImagePromptPreview(c:WorkbenchController,id:String) {
    var showDesign by remember(id){mutableStateOf(false)}
    val result by produceState<Result<ImagePromptReading>?>(null,id,c.state.jobs){value=withContext(Dispatchers.IO){runCatching{readImagePrompt(c,id)}}}
    Text("图片 Prompt 预览",style=MaterialTheme.typography.subtitle1)
    val read=result?.getOrNull()
    when {
        result==null->Text("正在读取…")
        result!!.isFailure->Text("READ_ERROR · 历史或原件不能核验，未替换为空 Prompt。",color=MaterialTheme.colors.error)
        read!=null->{
            Text(promptReadingLabel(read.status),style=MaterialTheme.typography.caption)
            if(read.sections.isNotEmpty() || read.status!="MISSING")StudioDisclosure("参数来源说明"){Text(read.explanation,style=MaterialTheme.typography.caption)}
            read.sections.forEach{s->Text(s.label+(s.roleId?.let{" · 原始 ${s.attributes["原始角色位置"]}"}?:""),color=Gold);SelectionContainer{Text(s.text)}}
            StudioActions {
                if(read.context.parameters["AI 设计关联"]?.let{it!="未记录"}==true)StudioAction("查看 AI 设计过程"){showDesign=true}
                if(read.sections.isNotEmpty()) {
                StudioAction("保存图片 Prompt 资产",!c.busy&&read.sections.isNotEmpty()){c.run("保留图片 Prompt"){saveImagePrompt(c,id,false)}}
                StudioAction("图片加入多 Prompt 参照",!c.busy&&read.sections.isNotEmpty()){c.run("加入图片参照"){saveImagePrompt(c,id,true);c.library.tab="多 Prompt 参照";c.view="资产管理"}}
                }
            }
        }
    }
    if(showDesign)DesignProvenanceDialog(c,id){showDesign=false}
}
