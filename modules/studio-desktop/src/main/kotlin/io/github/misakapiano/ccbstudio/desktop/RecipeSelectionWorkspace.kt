package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString

@Composable internal fun RecipeSelectionWorkspace(c:WorkbenchController,id:String,dismiss:()->Unit) {
    val history by produceState<Result<Pair<com.example.chatbar.domain.image.NovelAiGenerationRecipe,Long>?>?>(null,id){
        value=withContext(Dispatchers.IO){runCatching{c.historyRecipe(id)}}
    }
    var chosen by remember(id){mutableStateOf(emptySet<RecipeDraftField>())}
    val before=c.draft
    val source=history?.getOrNull()
    val result=remember(before,source,chosen){source?.let{runCatching{RecipeDraftSelection.apply(before,it.first,it.second,chosen)}}}
    val next=result?.getOrNull()
    val rows=remember(before,next){if(next==null)emptyList() else RecipeComparison.rows(
        RecipeComparison.inspect(documentJson.encodeToString(before)),RecipeComparison.inspect(documentJson.encodeToString(next))).filter{it.different}}
    Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){
        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("选择历史字段应用到草稿",style=MaterialTheme.typography.h6)
            Text("来源 $id · 未选字段保留。角色与位置、尺寸分别作为完整组应用；可以撤销。",style=MaterialTheme.typography.caption)
            StudioActions{StudioAction("返回比较",!c.busy,onClick=dismiss);StudioAction("取消全部字段"){chosen=emptySet()}}
            if(history==null)Text("正在验证原记录…")
            else if(source==null)Text(if(history?.isFailure==true)"原记录无法验证，禁止应用" else "没有 Studio 生成记录；请在参数页检查 PNG 元数据")
            if(source!=null){
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
                    RecipeDraftField.entries.forEach{field->StudioSwitch("应用 ${field.label}",field in chosen){chosen=if(field in chosen)chosen-field else chosen+field}}
                    Divider();Text("实际草稿差异（包含权威规则产生的关联变化）")
                    if(result?.isFailure==true)Text(result.exceptionOrNull()?.message.orEmpty(),color=MaterialTheme.colors.error)
                    rows.forEach{row->Text(row.path,color=Gold);Text("当前：${row.a?.value?:"<missing>"}\n应用后：${row.b?.value?:"<missing>"}")}
                    if(next!=null&&rows.isEmpty())Text("所选字段与当前草稿一致")
                }
                StudioAction("确认应用 ${chosen.size} 个字段",!c.busy && next!=null && rows.isNotEmpty()){c.applyRecipeFields(id,chosen,before)}
            }
            Text("最近字段应用记录（历史操作，不代表当前草稿；最多保留100条）",style=MaterialTheme.typography.caption)
            c.state.creation?.recipeApplications?.takeLast(3)?.reversed()?.forEach{r->Text("${r.sourceId.take(8)} · ${r.fields.joinToString{it.label}} · ${r.afterSha256.take(12)}",style=MaterialTheme.typography.caption)}
            c.problem?.let{Text(it,color=MaterialTheme.colors.error)}
        }
    }
}
