package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.Path
import javax.imageio.ImageIO

internal data class MetadataInspection(val status:String,val metadata:NovelAiStudioPngMetadata?=null,val explanation:String="")

/** The authority reader decides parameter precedence. This diagnostic projection distinguishes
 * absent metadata from malformed/unsupported content without rewriting or stripping the original. */
internal fun inspectMetadata(path:Path):MetadataInspection = try {
    val parsed=DesktopImageMetadata.readStudio(path.toString())
    if(parsed!=null)MetadataInspection("READABLE",parsed,"使用 Desktop 原有文本／Alpha 参数优先级；未知字段保留在原文件中") else {
    val comments=mutableListOf<String>()
    ImageIO.createImageInputStream(path.toFile()).use { input ->
        val readers=ImageIO.getImageReaders(input)
        if(!readers.hasNext())return MetadataInspection("UNSUPPORTED",null,"当前格式不能读取参数；原始文件仍保留")
        val reader=readers.next()
        try {
            reader.input=input
            require(reader.getWidth(0).toLong()*reader.getHeight(0)<=PrivacyPngEncoder.MAX_PIXELS)
            val metadata=reader.getImageMetadata(0)
            metadata.metadataFormatNames?.forEach {format->
                fun walk(node:org.w3c.dom.Node) {
                    val attributes=node.attributes
                    if(attributes?.getNamedItem("keyword")?.nodeValue=="Comment") {
                        val value=attributes.getNamedItem("text")?.nodeValue ?: attributes.getNamedItem("value")?.nodeValue
                        if(value!=null){require(value.length<=2*1024*1024);comments+=value}
                    }
                    var child=node.firstChild
                    while(child!=null){walk(child);child=child.nextSibling}
                }
                walk(metadata.getAsTree(format))
            }
            val image=reader.read(0)
            try {StealthAlphaMetadata.decode(image.width,image.height){x,y->image.getRGB(x,y) ushr 24}?.let{payload->
                require(payload.length<=2*1024*1024)
                val root=Json.parseToJsonElement(payload).jsonObject
                root["Comment"]?.let{comments+=if(it is JsonPrimitive)it.content else it.toString()}
            }}finally{image.flush()}
        }finally{reader.dispose()}
    }
    when {
        comments.isEmpty()->MetadataInspection("MISSING",explanation="没有发现 NovelAI 参数；可继续查看或编辑原图")
        comments.any{runCatching{Json.parseToJsonElement(it)}.isFailure}->MetadataInspection("CORRUPT",explanation="发现无法解析的参数，未改动草稿或原图")
        else->MetadataInspection("UNSUPPORTED",explanation="参数结构不能由当前权威读取器识别；没有猜测参数")
    }
    }
} catch(e:Exception) {MetadataInspection("READ_ERROR",explanation="参数读取失败；原件保留，未替换为空参数")}

@Composable internal fun MetadataWorkspace(c:WorkbenchController,id:String,onClose:()->Unit) {
    Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground) {
    var selection by remember(id){mutableStateOf(NovelAiStudioMetadataSelection())}
    var pendingMode by remember(id){mutableStateOf<NovelAiHistoryApplyMode?>(null)}
    val inspection by produceState<MetadataInspection?>(null,id){value=withContext(Dispatchers.IO){c.metadataFile(id,::inspectMetadata)}}
    val history by produceState<Result<Pair<NovelAiGenerationRecipe,Long>?>?>(null,id){value=withContext(Dispatchers.IO){runCatching{c.historyRecipe(id)}}}
    Column(Modifier.fillMaxSize().background(Ink).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("元数据与历史复用")
        StatusText("原件 ${id.take(8)} · 参数导入不会重编码原图")
        inspection?.let{read->
            StatusText("${read.status} · ${read.explanation}")
            read.metadata?.let{metadata->
                StudioSwitch("正面 Prompt",selection.positivePrompt){selection=selection.copy(positivePrompt=!selection.positivePrompt)}
                StudioSwitch("负面 Prompt",selection.negativePrompt){selection=selection.copy(negativePrompt=!selection.negativePrompt)}
                CompactChoice("角色 Prompt",NovelAiCharacterImportMode.entries,selection.characterPrompts,{it.displayName}){selection=selection.copy(characterPrompts=it)}
                StudioSwitch("生成参数",selection.generationSettings){selection=selection.copy(generationSettings=!selection.generationSettings)}
                StudioSwitch("Seed",selection.seed){selection=selection.copy(seed=!selection.seed)}
                StudioSwitch("图像引导",selection.imageGuidance){selection=selection.copy(imageGuidance=!selection.imageGuidance)}
                val next=c.draft.applyImportedMetadata(metadata,selection)
                SelectionContainer {Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text("当前基础 Prompt：${c.draft.basePrompt.ifBlank{"（空）"}}")
                    Text("导入后：${next.basePrompt.ifBlank{"（空）"}}")
                    Text("负向：${c.draft.negativePrompt} → ${next.negativePrompt}")
                    Text("角色 ${c.draft.characters.size} → ${next.characters.size}")
                    next.characters.forEachIndexed{i,ch->Text("${i+1}. ${ch.prompt} / ${ch.negativePrompt} / ${ch.center}")}
                    settingsComparison(c.draft.activeSettings,next.activeSettings).forEach{Text(it)}
                    Text("图像引导：${next.imageGuidance.action.displayName}；嵌入素材确认后保全")
                }}
                StudioAction("确认导入所选字段",!c.busy){c.importMetadata(id,selection)}
            }
        } ?: StatusText("正在读取…")
        Divider()
        val recipe=history?.getOrNull()
        when {
            history?.isFailure==true->StatusText("历史请求证据不可验证；不复用，原图仍可读",DesktopBootstrapColors.warning)
            history!=null && recipe==null->StatusText("没有关联的 Studio 生成 Recipe")
            recipe!=null->{
                Text("已保存的生成 Recipe · Seed ${recipe.second}")
                SelectionContainer{Text("${recipe.first.stylePrompt}\n${recipe.first.basePrompt}\n${recipe.first.extraPrompt}\n${recipe.first.settings.model.displayName} · ${recipe.first.settings.steps} Steps · ${recipe.first.settings.sampler.displayName}")}
                val missing=recipe.first.imageGuidance.hasMissingHistorySource()
                if(missing)StatusText("原记录没有可直接复现的图像引导；完整复现不可用。源请求证据仍保留。",DesktopBootstrapColors.warning)
                StudioActions {
                    StudioAction("完整复现",!c.busy&&!missing){pendingMode=NovelAiHistoryApplyMode.FULL}
                    StudioAction("新种子复用",!c.busy){pendingMode=NovelAiHistoryApplyMode.NEW_SEED}
                    StudioAction("仅复用 Seed",!c.busy){pendingMode=NovelAiHistoryApplyMode.SEED_ONLY}
                }
            }
        }
        StudioAction("返回",!c.busy,onClick=onClose)
    }
    pendingMode?.let{mode->AlertDialog(onDismissRequest={pendingMode=null},title={Text("确认复用 ${mode.name}")},text={Text(if(recipeWarning(history?.getOrNull()?.first,mode))"缺少原始图像引导，无法准确复现；按 Desktop 规则仅载入可复用设置。" else "应用将更新创作草稿；已保存历史不变，可以撤销。")},confirmButton={TextButton(onClick={pendingMode=null;c.reuseHistory(id,mode,true)}){Text("确认复用")}},dismissButton={TextButton(onClick={pendingMode=null}){Text("取消")}})}
    }
}
private fun recipeWarning(recipe:NovelAiGenerationRecipe?,mode:NovelAiHistoryApplyMode)=recipe?.requiresImageGuidanceReuseWarning(mode)==true
private fun settingsComparison(a:NovelAiGenerationSettings,b:NovelAiGenerationSettings):List<String> {
    fun size(s:NovelAiGenerationSettings)=runCatching{s.imageSize().let{"${it.width}×${it.height}"}}.getOrDefault("无效尺寸")
    return listOf("模型：${a.model.displayName} → ${b.model.displayName}","尺寸：${size(a)} → ${size(b)}",
        "数量：${a.count} → ${b.count}　Steps：${a.steps} → ${b.steps}","CFG：${a.guidance} → ${b.guidance}　Rescale：${a.cfgRescale} → ${b.cfgRescale}",
        "采样器：${a.sampler.displayName} → ${b.sampler.displayName}","Seed：${if(a.seedMode==NovelAiSeedMode.RANDOM)"随机"else a.seed} → ${if(b.seedMode==NovelAiSeedMode.RANDOM)"随机"else b.seed}")
}
