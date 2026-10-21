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

/** Read-only projection. A broken linked receipt is not silently replaced with embedded PNG text. */
internal fun readImagePrompt(c:WorkbenchController,id:String):ImagePromptReading = c.workspace.operation {
    val asset=c.workspace.assets.read(UUID.fromString(id)).require()
    val history=c.historyRecipe(id)
    val sections=mutableListOf<PromptSection>()
    fun part(key:String,label:String,text:String?,role:String?=null,attributes:Map<String,String> = emptyMap()) {
        if(!text.isNullOrBlank())sections+=PromptSection(key,label,text,role,attributes)
    }
    if(history!=null) {
        val job=c.workspace.state.jobs.last{ id in it.results || id in it.composedResults }
        val receipt=documentJson.decodeFromString<GenerationReceipt>(job.recipe)
        // Stored recipe uses asset references rather than expanding large image payloads into the UI.
        val recipe=receipt.recipe;val raw=documentJson.encodeToString(recipe)
        part("style","风格 Prompt",recipe.stylePrompt);part("base","基础 Prompt",recipe.basePrompt)
        part("extra","额外 Prompt",recipe.extraPrompt);part("negative","负向 Prompt",recipe.negativePrompt)
        recipe.characters.forEachIndexed{i,ch->
            val role="role:$i";val attributes=mapOf("原始角色位置" to (i+1).toString(),"位置" to ch.center.toString(),"启用" to ch.enabled.toString())
            part("$role:positive","角色正向",ch.prompt,role,attributes);part("$role:negative","角色负向",ch.negativePrompt,role,attributes)
        }
        val parameters=RecipeComparison.inspect(raw).fields.filter{(key,value)->
            value.kind !in setOf("object","array") && (key.startsWith("/settings/") || key.startsWith("/imageGuidance/") || key=="/naturalLanguageMode")
        }.mapValues{it.value.value}+mapOf("实际 Seed" to history.second.toString(),"图片尺寸" to recipe.settings.imageSize().let{"${it.width}×${it.height}"})
        val status=if(receipt.wireSha256.isNotBlank())"VERIFIED_HISTORY"else"HISTORY_WITHOUT_REQUEST_HASH"
        return@operation ImagePromptReading(status,sections,PromptImageContext(id,asset.sha256(),status,parameters,raw,receipt.wireSha256.takeIf{it.isNotBlank()}),"读取保存的生成 Recipe；${if(receipt.wireSha256.isNotBlank())"请求摘要已核验"else"旧请求没有校验摘要"}。AI 设计记录与本图未建立可验证关联时，不推断它们属于同一次设计。")
    }
    val read=c.metadataFile(id,::inspectMetadata);val m=read.metadata
    val parameters=linkedMapOf<String,String>()
    if(m!=null) {
        part("base","基础 Prompt",m.positivePrompt);part("negative","负向 Prompt",m.negativePrompt)
        m.characters.forEachIndexed{i,ch->val role="role:$i";val attributes=mapOf("原始角色位置" to (i+1).toString(),"位置" to ch.center.toString())
            part("$role:positive","角色正向",ch.prompt,role,attributes);part("$role:negative","角色负向",ch.negativePrompt,role,attributes)
        }
        parameters["图片尺寸"]="${m.width}×${m.height}"
        m.seed?.let{parameters["实际 Seed"]=it.toString()}
        m.settings.let{s->listOf("/settings/model" to s.model?.apiId,"/settings/steps" to s.steps,"/settings/guidance" to s.guidance,"/settings/cfgRescale" to s.cfgRescale,"/settings/sampler" to s.sampler?.name,"/settings/useCharacterPositions" to s.useCharacterPositions).forEach{(key,value)->if(value!=null)parameters[key]=value.toString()}}
    }
    ImagePromptReading(read.status,sections,PromptImageContext(id,asset.sha256(),read.status,parameters),read.explanation+"；嵌入参数不是已核验的完整发送请求。")
}

internal fun saveImagePrompt(c:WorkbenchController,id:String,addToBoard:Boolean):String {
    val read=readImagePrompt(c,id);require(read.sections.isNotEmpty()){ "图片没有可比较的非空 Prompt：${read.status}" }
    val name=c.library.labels(id).alias.ifBlank{c.workspace.assets.read(UUID.fromString(id)).require().request().name()}
    val existing=c.library.repository.prompts().firstOrNull{a->val p=c.library.repository.prompt(a.request().id().toString());p.image==read.context && p.sections==read.sections}
    val prompt=existing?.request()?.id()?.toString()?:c.library.repository.savePrompt(read.text(),name,"image:$id:${read.context.sha256}",sections=read.sections,image=read.context)
    if(addToBoard)PromptReferences(c.library.repository).add(prompt)
    c.library.refresh();return prompt
}

@Composable internal fun ImagePromptPreview(c:WorkbenchController,id:String) {
    val result by produceState<Result<ImagePromptReading>?>(null,id,c.state.jobs){value=withContext(Dispatchers.IO){runCatching{readImagePrompt(c,id)}}}
    Text("图片 Prompt 预览",style=MaterialTheme.typography.subtitle1)
    val read=result?.getOrNull()
    when {
        result==null->Text("正在读取…")
        result!!.isFailure->Text("READ_ERROR · 历史或原件不能核验，未替换为空 Prompt。",color=MaterialTheme.colors.error)
        read!=null->{
            Text("${read.status} · ${read.explanation}",style=MaterialTheme.typography.caption)
            if(read.sections.isEmpty())Text("没有非空 Prompt；原图仍可查看和编辑。")
            read.sections.forEach{s->Text(s.label+(s.roleId?.let{" · 原始 ${s.attributes["原始角色位置"]}"}?:""),color=Gold);SelectionContainer{Text(s.text)}}
            StudioActions {
                StudioAction("保存图片 Prompt 资产",!c.busy&&read.sections.isNotEmpty()){c.run("保留图片 Prompt"){saveImagePrompt(c,id,false)}}
                StudioAction("图片加入多 Prompt 参照",!c.busy&&read.sections.isNotEmpty()){c.run("加入图片参照"){saveImagePrompt(c,id,true);c.library.tab="多 Prompt 参照";c.view="资产管理"}}
            }
        }
    }
}
