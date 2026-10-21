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

@Composable internal fun ImageCardChoice(c:WorkbenchController) {
    var candidate by remember{mutableStateOf<ImageCardSource?>(null)}
    val scope=rememberCoroutineScope();val cards=c.state.creation!!.imageCards
    StudioActions {
        StudioAction("导入 CCB 角色卡 Prompt",!c.busy){
            StudioFileDialogs.open("选择 CCB 角色卡 PNG / JSON",listOf("png","json"))?.let{path->scope.launch{
                runCatching{withContext(Dispatchers.IO){readImageCardSource(readBounded(path,16*1024*1024))}}.onSuccess{candidate=it}.onFailure{c.problem="角色卡未导入：格式不支持、内容不完整或超过限制；现有草稿保留"}
            }}
        }
    }
    if(cards.isNotEmpty())CompactChoice("图像来源卡",cards,cards.firstOrNull{it.id==c.draft.importedCharacterCardId},{it.name}){candidate=it}
    StatusText("卡片画风与基础负面词可回填；角色图像词仅供 AI 设计参考。")
    candidate?.let{card->AlertDialog(onDismissRequest={candidate=null},title={Text("应用 ${card.name} 的图像字段")},text={SelectionContainer{Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("画风：${card.stylePrompt}");Text("负向：${card.negativePrompt}")
        Text("默认模型：${card.model?.displayName?:"跟随全局"}")
        card.characters.forEach{Text("${it.name}：${it.prompt}")}
        Text("来源 SHA-256：${card.sourceSha256}")
    }}},confirmButton={TextButton(onClick={candidate=null;c.selectImageCard(card)}){Text("确认应用图像字段")}},dismissButton={TextButton(onClick={candidate=null}){Text("取消")}})}
}
