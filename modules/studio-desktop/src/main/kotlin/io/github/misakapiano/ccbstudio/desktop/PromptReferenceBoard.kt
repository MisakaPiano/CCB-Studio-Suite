package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import com.example.chatbar.domain.image.*
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PromptBoardRead(val state:PromptReferenceState,val sources:Map<String,PromptAsset>,
    val rows:List<PromptReferenceRow>,val roleOrders:Map<String,List<String>>)

@Composable internal fun PromptReferenceBoard(c:WorkbenchController) = BoxWithConstraints(Modifier.fillMaxWidth()) {
    val availableWidth=maxWidth
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    val repo=remember(c){PromptReferences(c.library.repository)}
    val loaded by produceState<Result<PromptBoardRead>?>(null,c.library.state){value=withContext(Dispatchers.IO){runCatching{
        val state=repo.load()
        PromptBoardRead(state,state.ids.associateWith{c.library.repository.prompt(it)},repo.rows(),state.ids.associateWith{repo.roles(it)})
    }}}
    if(loaded==null){Text("正在读取 Prompt 参照…");return@Column}
    if(loaded!!.isFailure){Text("参照记录无法读取，原始资料保留",color=MaterialTheme.colors.error);return@Column}
    val snapshot=loaded!!.getOrThrow();val state=snapshot.state
    val columnWidth=((availableWidth-10.dp*(state.ids.size-1).coerceAtLeast(0))/state.ids.size.coerceAtLeast(1)).coerceIn(320.dp,640.dp)
    val sources=snapshot.sources;val rows=snapshot.rows;val roleOrders=snapshot.roleOrders
    var excerpt by remember{mutableStateOf("")};var origins by remember{mutableStateOf(listOf<String>())}
    var differences by remember{mutableStateOf(false)};var panel by remember{mutableStateOf("Prompt 对比")};var advanced by remember{mutableStateOf(false)};var confirm by remember{mutableStateOf(false)}
    var detail by remember{mutableStateOf<Pair<String,String>?>(null)}
    var designImage by remember{mutableStateOf<String?>(null)}
    val bounds=remember{mutableMapOf<Pair<String,String>,Rect>()}
    val boardScroll=rememberScrollState()
    fun take(id:String,s:PromptSection){excerpt+=if(excerpt.isBlank())s.text else "\n"+s.text;origins=(origins+"$id#${s.id}").distinct()}
    fun origin()="prompt-excerpts:"+origins.joinToString(",")
    @Composable fun matrixRow(content:@Composable (String)->Unit) {
        Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            state.ids.forEach{id->Column(Modifier.width(columnWidth).fillMaxHeight().background(Panel).border(1.dp,DesktopBootstrapColors.border).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){content(id)}}
        }
    }
    @Composable fun promptRow(row:PromptReferenceRow) {
        val values=row.cells.values.map{it.text}.distinct()
        if(!differences || values.size>1) {
            val common=row.cells.values.map{it.text.split(Regex(",|\\r?\\n")).map(String::trim).filter(String::isNotEmpty).toSet()}.reduceOrNull{a,b->a.intersect(b)}.orEmpty()
            matrixRow{id->
                Text(row.label,style=MaterialTheme.typography.subtitle2)
                row.cells[id]?.let{s->
                    val rich=buildAnnotatedString {Regex("[^,\\r\\n]+|,|\\r?\\n").findAll(s.text).forEach{match->
                        val unique=row.cells.size>1 && match.value.trim().isNotEmpty() && match.value.trim()!="," && match.value.trim() !in common
                        if(unique)withStyle(SpanStyle(color=Gold)){append(match.value)}else append(match.value)
                    }}
                    SelectionContainer{Text(rich,Modifier.fillMaxWidth().heightIn(max=220.dp).verticalScroll(rememberScrollState()))}
                    StudioAction("摘取",accessibilityLabel="摘取 ${row.label} ${id.take(8)}"){take(id,s)}
                }?:Text("—（此来源无非空内容，不参与比较）",style=MaterialTheme.typography.caption)
            }
        }
    }
    StudioActions {
        StudioChips("",listOf("Prompt 对比","生成参数"),panel,{it}){panel=it}
        StudioSwitch("仅看不同内容",differences){differences=!differences}
    }
    StudioDisclosure("比较说明") {Text("只比较非空 Prompt；亮色表示不共有的原文片段。拖动角色标题对齐，不改变原 Recipe 或请求顺序。窄窗可横向滚动，摘取不自动发送给 AI。")}
    if(state.ids.isEmpty())Text("从图库选择多张图片，或将独立 Prompt 资产加入参照。")
    if(boardScroll.maxValue>0)HorizontalScrollbar(rememberScrollbarAdapter(boardScroll),Modifier.fillMaxWidth().height(12.dp).semantics{contentDescription="横向查看参照"})
    Column(Modifier.fillMaxWidth().horizontalScroll(boardScroll),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        matrixRow{id->val p=sources.getValue(id)
            p.image?.let{image->AssetImage(c,image.imageId,Modifier.fillMaxWidth().height(180.dp))}
                ?:Box(Modifier.fillMaxWidth().height(180.dp),contentAlignment=androidx.compose.ui.Alignment.Center){Text("独立 Prompt 资产")}
            Text(c.library.labels(id).alias.ifBlank{"Prompt ${id.take(8)}"},style=MaterialTheme.typography.subtitle1)
            StudioDisclosure("来源详情"){SelectionContainer{Text(p.origin,style=MaterialTheme.typography.caption)};p.image?.let{Text("SHA-256 ${it.sha256}",style=MaterialTheme.typography.caption);Text("AI 设计关联：${it.parameters["AI 设计关联"]?:"未记录"}",style=MaterialTheme.typography.caption)}}
            p.image?.let{image->
                Text("Seed ${image.parameters["实际 Seed"]?:"未记录"} · ${promptReadingLabel(image.status)}",style=MaterialTheme.typography.caption)
                image.parameters["来源图片"]?.let{Text("参数来自源图 ${it.take(8)}；当前图包含后续处理。",color=Gold,style=MaterialTheme.typography.caption)}
                StudioActions {
                if(image.parameters["AI 设计关联"]?.let{it!="未记录"}==true)StudioAction("AI 设计过程",accessibilityLabel="设计来源 ${id.take(8)}"){designImage=image.imageId}
                StudioAction("完整 Recipe",image.recipeJson!=null,accessibilityLabel="完整 Recipe ${id.take(8)}"){detail="保存的 Recipe" to image.recipeJson!!}
                StudioAction("实际请求",image.requestSha256!=null&&!c.busy,accessibilityLabel="发送请求快照 ${id.take(8)}"){c.run("读取已验证请求"){
                    val receipt=imageOrigin(c.workspace,image.imageId).receipt ?: error("历史请求不存在")
                    require(receipt.wireSha256==image.requestSha256)
                    detail="实际请求 SHA-256 ${receipt.wireSha256}" to receipt.expandedRequest(c.workspace).toString()
                }}
                }
            }
            StudioAction("移出参照",!c.busy,accessibilityLabel="移出参照 ${id.take(8)}"){c.run("调整参照板"){repo.remove(id);c.library.refresh()}}
        }
        if(panel=="Prompt 对比") {
        rows.filter{it.key.startsWith("global:")}.forEach{promptRow(it)}
        repeat(roleOrders.values.maxOfOrNull{it.size}?:0){index->
            matrixRow{id->val roles=roleOrders.getValue(id);val role=roles.getOrNull(index)
                if(role==null)Text("—（没有对应角色）")else {
                    var dragY by remember(id,role){mutableStateOf(0f)}
                    val first=repo.sections(id).first{it.roleId==role}
                    Row(Modifier.fillMaxWidth().onGloballyPositioned{bounds[id to role]=it.boundsInRoot()}
                        .semantics{contentDescription="拖动参照角色 $id $role"}
                        .pointerInput(id,role,roles,c.busy){detectDragGestures(onDragStart={pos->dragY=(bounds[id to role]?.top?:0f)+pos.y},onDragEnd={
                            if(!c.busy){val before=roles.firstOrNull{it!=role && dragY<(bounds[id to it]?.center?.y?:Float.MAX_VALUE)};c.run("对齐参照角色"){repo.moveRole(id,role,before);c.library.refresh()}}
                        }){change,amount->if(!c.busy){change.consume();dragY+=amount.y}}}){Text("⠿ 对齐位置 ${index+1} · 原始角色 ${first.attributes["原始角色位置"]?:role}",color=Gold)}
                    Text("原始位置：${first.attributes["位置"]?.takeUnless{it=="null"}?:"自动"} · ${if(first.attributes["启用"]=="false")"未启用"else "已启用"}",style=MaterialTheme.typography.caption)
                    StudioActions {
                        StudioAction("上移",!c.busy&&index>0,accessibilityLabel="角色上移 ${id.take(8)} ${index+1}"){c.run("对齐参照角色"){repo.moveRole(id,role,roles[index-1]);c.library.refresh()}}
                        StudioAction("下移",!c.busy&&index<roles.lastIndex,accessibilityLabel="角色下移 ${id.take(8)} ${index+1}"){c.run("对齐参照角色"){repo.moveRole(id,role,roles.getOrNull(index+2));c.library.refresh()}}
                    }
                }
            }
            rows.filter{it.key.startsWith("role:$index:")}.forEach{promptRow(it)}
        }
        }
        if(panel=="生成参数") {
            StudioSwitch("展开完整参数字段",advanced){advanced=!advanced}
            val allKeys=sources.values.flatMap{it.image?.parameters?.keys.orEmpty()}.distinct()
            val keys=promptParameterLabels.keys.filter{it in allKeys}+(allKeys-promptParameterLabels.keys).sorted()
            keys.filter{advanced || it in promptParameterLabels}.forEach{key->val values=state.ids.map{sources.getValue(it).image?.parameters?.get(key)}
                if(!differences || values.distinct().size>1)matrixRow{id->
                    Text(promptParameterLabels[key]?:key,color=if(values.distinct().size>1)Gold else Quiet)
                    SelectionContainer{Text(sources.getValue(id).image?.parameters?.get(key)?.let{promptParameterDisplay(key,it)}?:"未记录（未知，不等于默认值）",Modifier.heightIn(max=160.dp).verticalScroll(rememberScrollState()))}
                }
            }
        }
    }
    OutlinedTextField(excerpt,{excerpt=it},label={Text("摘取草稿（可编辑）")},modifier=Modifier.fillMaxWidth().heightIn(min=100.dp,max=240.dp))
    Text("摘取来源：${origins.joinToString(", ").ifBlank{"尚未摘取"}}",style=MaterialTheme.typography.caption)
    StudioActions {
        StudioAction("复制摘取",excerpt.isNotBlank()){c.gallery.copyText(excerpt)}
        StudioAction("摘取保存为 Prompt 资产",!c.busy&&excerpt.isNotBlank()){c.run("保存参照摘取"){c.library.repository.savePrompt(excerpt,"参照摘取",origin());c.library.refresh()}}
        StudioAction("清空摘取"){excerpt="";origins=emptyList()}
        StudioAction("预览应用摘取",!c.busy&&excerpt.isNotBlank()){confirm=true}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("应用摘取到创作？")},text={SelectionContainer{Text(excerpt,Modifier.heightIn(max=300.dp).verticalScroll(rememberScrollState()))}},confirmButton={TextButton(onClick={c.pastePrompt(excerpt,origin());confirm=false;c.view="创作"}){Text("确认应用，可撤销")}},dismissButton={TextButton(onClick={confirm=false}){Text("取消")}})
    detail?.let{(title,text)->AlertDialog(onDismissRequest={detail=null},title={Text(title)},text={Column{if(text.length>20000)Text("内容较长，预览前 20000 字；复制获取完整快照。");SelectionContainer{Text(text.take(20000),Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState()))}}},confirmButton={TextButton(onClick={c.gallery.copyText(text)}){Text("复制完整内容")}},dismissButton={TextButton(onClick={detail=null}){Text("关闭")}})}
    designImage?.let{DesignProvenanceDialog(c,it){designImage=null}}
}
}

private val promptParameterLabels=linkedMapOf("实际 Seed" to "实际 Seed","图片尺寸" to "图片尺寸", "/settings/model" to "模型", "/settings/steps" to "Steps", "/settings/guidance" to "Guidance", "/settings/cfgRescale" to "CFG Rescale", "/settings/sampler" to "采样器", "/settings/seedMode" to "种子模式", "/settings/count" to "生成数量", "/settings/useCharacterPositions" to "启用角色位置", "/imageGuidance/action" to "图像引导模式", "/naturalLanguageMode" to "自然语言模式")

private fun promptParameterDisplay(key:String,raw:String):String {
    val value=runCatching{(Json.parseToJsonElement(raw) as? JsonPrimitive)?.content}.getOrNull()?:raw
    return when(key) {
        "/settings/model"->NovelAiImageModel.entries.firstOrNull{it.name==value||it.apiId==value}?.displayName?:value
        "/settings/sampler"->NovelAiSampler.entries.firstOrNull{it.name==value||it.apiId==value}?.displayName?:value
        "/imageGuidance/action"->NovelAiGenerationAction.entries.firstOrNull{it.name==value||it.apiId==value}?.displayName?:value
        "/settings/seedMode"->when(value){"RANDOM"->"随机";"FIXED"->"固定";else->value}
        "/settings/useCharacterPositions","/naturalLanguageMode"->when(value){"true"->"开启";"false"->"关闭";else->value}
        else->value
    }
}
