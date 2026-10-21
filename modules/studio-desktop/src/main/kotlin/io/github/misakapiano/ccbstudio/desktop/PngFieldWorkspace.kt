package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*

@Composable internal fun PngFieldWorkspace(c:WorkbenchController,id:String,dismiss:()->Unit) {
    var query by remember(id){mutableStateOf("")};var expanded by remember(id){mutableStateOf<Pair<String,RecipeField>?>(null)}
    val inspection by produceState<RecipeInspection?>(null,id){value=withContext(Dispatchers.IO){try{PngFieldInspection.inspect(c.workspace.bytes(id))}catch(e:Exception){RecipeInspection("READ_ERROR",emptyMap(),null)}}}
    val fields=inspection?.fields?.entries?.filter{it.key.contains(query,true)||it.value.value.contains(query,true)}.orEmpty()
    Surface(Modifier.fillMaxSize(),color=Ink,contentColor=DesktopBootstrapColors.foreground) {
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        StudioActions{StudioAction("返回参数选择",onClick=dismiss);StudioAction("复制完整 PNG 字段",inspection?.raw!=null){c.gallery.copyText(inspection!!.raw!!)}}
        Text("完整 PNG 字段 · ${inspection?.status?:"READING"}")
        Text("只读查看原始文本块与 Alpha 参数，保留重复项及未知字段；应用参数仍使用 Desktop 权威优先级。",style=MaterialTheme.typography.caption)
        Text("查找 PNG 字段",style=MaterialTheme.typography.caption)
        OutlinedTextField(query,{query=it},singleLine=true,modifier=Modifier.fillMaxWidth().semantics{contentDescription="查找 PNG 字段"})
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(fields,key={it.key}){entry->Column(Modifier.fillMaxWidth().padding(vertical=6.dp)) {
                Text(entry.key.ifBlank{"/"},color=Gold)
                SelectionContainer{Text("${entry.value.kind} · ${entry.value.value.take(2000)}")}
                StudioActions{StudioAction("复制 PNG 字段 ${entry.key}"){c.gallery.copyText(entry.value.value)};StudioAction("展开 PNG 字段 ${entry.key}"){expanded=entry.key to entry.value}}
                Divider()
            }}
        }
    }
    expanded?.let{entry->TypesetPreviewDialog(entry.first,{expanded=null},actions={TextButton(onClick={expanded=null}){Text("返回字段列表")}}){SelectionContainer{Text(entry.second.value,Modifier.fillMaxSize().verticalScroll(rememberScrollState()))}}}
    }
}
