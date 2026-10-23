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

private data class DesignReading(val application:DesignApplicationSnapshot,val sent:DesignPromptSnapshot,val requestSha256:String,val sourceId:String?)

/** Read-only view of an explicit application and the generated image's exact request identity. */
@Composable internal fun DesignProvenanceDialog(c:WorkbenchController,imageId:String,dismiss:()->Unit) {
    val reading by produceState<Result<DesignReading?>?>(null,imageId){
        value=withContext(Dispatchers.IO){runCatching{c.workspace.operation {
            val origin=imageOrigin(c.workspace,imageId)
            val job=origin.job?:return@operation null
            val application=DesignProvenance(c.workspace).forGeneration(job)?:return@operation null
            val receipt=documentJson.decodeFromString<GenerationReceipt>(job.receipt(c.workspace))
            DesignReading(application,DesignPromptSnapshot.of(receipt.recipe),receipt.wireSha256,origin.sourceId.takeIf{origin.derived})
        }}}
    }
    var tab by remember{mutableStateOf("应用与生成")}
    AlertDialog(onDismissRequest=dismiss,title={Text("这张图片的 AI 设计来源")},text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(reading==null)Text("正在核验设计记录…")
        else if(reading!!.isFailure)Text("设计关联无法核验，原记录保留；不会用相近时间或文字猜测来源。",color=MaterialTheme.colors.error)
        else reading!!.getOrNull()?.let{r->
            r.sourceId?.let{Text("这是来源图片 ${it.take(8)} 的设计过程；当前图片还包含后续编辑。",color=Gold)}
            Text("已核验：明确应用的设计候选 → 该次生成请求。")
            Text("应用后仍可手动修改；下方记录区分候选、应用后的草稿和最终生成内容。")
            StudioChips("",listOf("应用与生成","设计过程"),tab,{it}){tab=it}
            if(tab=="设计过程"){
                Text(r.application.conversation.title,style=MaterialTheme.typography.subtitle1)
                r.application.conversation.turns.forEachIndexed{index,turn->
                    Text("第 ${index+1} 轮 · 你的要求",color=Gold);SelectionContainer{Text(turn.userText)}
                    turn.attachedStudioPrompt?.let{attached->StudioDisclosure("本轮附加的 Prompt"){
                        SelectionContainer{Text(attached.basePrompt)};attached.characterPrompts.forEach{SelectionContainer{Text(it)}}
                    }}
                    turn.reply?.let{reply->DesktopDesignResult(reply,onCopy=c.gallery.copyText)}
                    if(turn.id==r.application.turnId)Text("本轮候选被明确应用",color=Gold)
                }
            }else{
                Text(if(r.application.applied==r.sent)"应用后的 Prompt 与生成草稿一致。"else"应用后又有修改：以最终生成内容和请求快照为准。",color=Gold)
                StudioDisclosure("模型返回的候选",initiallyOpen=true){DesktopDesignResult(r.application.conversation.turns.last().reply!!,onCopy=c.gallery.copyText)}
                StudioDisclosure("应用后的草稿"){DesignPromptFields(r.application.applied,c,"应用后")}
                StudioDisclosure("最终生成内容",initiallyOpen=true){DesignPromptFields(r.sent,c,"生成")}
                Text("最终请求 SHA-256：${r.requestSha256}",style=MaterialTheme.typography.caption)
            }
        }?:Text("这张图片没有已记录的设计应用关联；仍可查看它的 Prompt、Recipe 和请求快照。")
    }},confirmButton={TextButton(onClick=dismiss){Text("关闭设计来源")}})
}

@Composable private fun DesignPromptFields(snapshot:DesignPromptSnapshot,c:WorkbenchController,source:String){
    Text("模型：${snapshot.model.displayName}")
    fun sections()=listOf("画风" to snapshot.style,"基础 Prompt" to snapshot.base,"附加 Prompt" to snapshot.extra,"负向 Prompt" to snapshot.negative)+
        snapshot.characters.flatMapIndexed{index,role->listOf("角色 ${index+1} 正向" to role.prompt,"角色 ${index+1} 负向" to role.negativePrompt)}
    sections().filter{it.second.isNotBlank()}.forEach{(label,text)->
        Text(label,color=Gold);SelectionContainer{Text(text)};StudioAction("复制$source $label"){c.gallery.copyText(text)}
    }
}
