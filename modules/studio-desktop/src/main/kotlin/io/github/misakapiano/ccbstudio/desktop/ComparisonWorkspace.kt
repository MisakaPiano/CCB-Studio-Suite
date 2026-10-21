@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private data class ComparisonSource(val id:String,val sha:String,val name:String,val inspection:RecipeInspection)
private fun comparisonSource(c:WorkbenchController,id:String):ComparisonSource {
    val a=c.workspace.assets.read(java.util.UUID.fromString(id)).require()
    val raw=a.request().recipe().decodeToString(throwOnInvalidSequence=true).takeIf{it.isNotBlank()}
    val inspection=RecipeComparison.inspect(raw)
    val complete=if(inspection.status=="READABLE" && raw!=null)runCatching{
        val tree=Json.parseToJsonElement(raw).jsonObject
        if(tree["authoritySha"]!=null && tree["recipe"]!=null){val receipt=documentJson.decodeFromString<GenerationReceipt>(raw)
            RecipeComparison.inspect(JsonObject(tree+("expandedRequest" to receipt.expandedRequest(c.workspace))).toString())
        }else inspection
    }.getOrElse{RecipeInspection("READ_ERROR",inspection.fields,raw)}else inspection
    return ComparisonSource(id,a.sha256(),a.request().name(),complete)
}
@Composable internal fun ComparisonWorkspace(c:WorkbenchController,ids:List<String>,dismiss:()->Unit) {
    require(ids.size==2 && ids.distinct().size==2)
    var query by remember(ids){mutableStateOf("")};var differences by remember(ids){mutableStateOf(true)}
    var locked by remember(ids){mutableStateOf(true)};var left by remember(ids){mutableStateOf(DesktopViewerTransform())};var right by remember(ids){mutableStateOf(DesktopViewerTransform())}
    var metadata by remember(ids){mutableStateOf<String?>(null)};var expanded by remember(ids){mutableStateOf<RecipeDifference?>(null)}
    metadata?.let{id->MetadataWorkspace(c,id){metadata=null};return}
    val source by produceState<Result<List<ComparisonSource>>?>(null,ids){value=withContext(Dispatchers.IO){runCatching{ids.map{comparisonSource(c,it)}}}}
    val pair=source?.getOrNull();val rows=pair?.let{RecipeComparison.rows(it[0].inspection,it[1].inspection)}.orEmpty().filter{(!differences || it.different) && (it.path.contains(query,true) || it.a?.value?.contains(query,true)==true || it.b?.value?.contains(query,true)==true)}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        StudioActions{StudioAction("返回图库",!c.busy,onClick=dismiss);StudioSwitch("锁定两图缩放",locked){locked=!locked;if(locked)right=left};StudioAction("适应两图"){left=DesktopViewerTransform();right=left};StudioSwitch("仅显示差异",differences){differences=!differences}}
        if(source?.isFailure==true)Text("来源读取失败；没有把失败当成空Recipe。原件仍保留。",color=MaterialTheme.colors.error)
        Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            ids.forEachIndexed{i,id->Column(Modifier.weight(1f).fillMaxHeight()){
                val item=pair?.get(i);Text("${if(i==0)"A"else"B"} · ${item?.name?:id} · ${((if(i==0)left.zoom else right.zoom)*100).toInt()}%",maxLines=2)
                Text("Studio Recipe ${item?.inspection?.status?:"READING"} · ${item?.sha.orEmpty()}",style=MaterialTheme.typography.caption,maxLines=2)
                ComparisonImage(c,id,if(i==0)left else right,{next->if(locked){left=next;right=next}else if(i==0)left=next else right=next},Modifier.fillMaxWidth().weight(1f))
                StudioActions{StudioAction("${if(i==0)"A"else"B"} 参数选择与应用",!c.busy){metadata=id};StudioAction("复制 ${if(i==0)"A"else"B"} 完整 Recipe",item?.inspection?.raw!=null){c.gallery.copyText(item!!.inspection.raw!!)}}
            }}
        }
        Text("搜索完整字段或值",style=MaterialTheme.typography.caption)
        OutlinedTextField(query,{query=it},singleLine=true,modifier=Modifier.fillMaxWidth().semantics{contentDescription="搜索完整字段或值"})
        Text("${rows.size} 个字段 · A/B身份固定；<missing>、null和空字符串分别显示。",style=MaterialTheme.typography.caption)
        LazyColumn(Modifier.fillMaxWidth().weight(1f)){
            items(rows,key={it.path}){row->Column(Modifier.fillMaxWidth().padding(vertical=6.dp)){
                Text(row.path.ifBlank{"/（根）"},color=if(row.different)Gold else Quiet)
                SelectionContainer{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Text("A: "+fieldText(row.a).take(2000),Modifier.weight(1f));Text("B: "+fieldText(row.b).take(2000),Modifier.weight(1f))
                }}
                StudioActions{StudioAction("查看完整字段 ${row.path}"){expanded=row};StudioAction("复制 A 字段 ${row.path}",row.a!=null){c.gallery.copyText(row.a!!.value)};StudioAction("复制 B 字段 ${row.path}",row.b!=null){c.gallery.copyText(row.b!!.value)}}
                Divider()
            }}
        }
    }
    expanded?.let{row->TypesetPreviewDialog(row.path.ifBlank{"完整根字段"},{expanded=null},actions={TextButton(onClick={expanded=null}){Text("返回比较")}}){
        SelectionContainer{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Text("A · "+fieldText(row.a));Divider();Text("B · "+fieldText(row.b))}}
    }}
}
private fun fieldText(value:RecipeField?)=value?.let{"${it.kind} · ${it.value}"}?:"<missing>"
@Composable private fun ComparisonImage(c:WorkbenchController,id:String,transform:DesktopViewerTransform,change:(DesktopViewerTransform)->Unit,modifier:Modifier) {
    var size by remember{mutableStateOf(IntSize.Zero)};val current by rememberUpdatedState(transform);val update by rememberUpdatedState(change)
    Box(modifier.semantics{contentDescription="比较图片 $id";stateDescription=transform.zoom.toString()}.clipToBounds().onSizeChanged{size=it}.pointerInput(id){detectDragGestures{event,delta->if(current.zoom>1f){event.consume();update(current.copy(pan=current.pan+delta))}}}
        .onPointerEvent(PointerEventType.Scroll){event->event.changes.firstOrNull()?.let{p->update(current.zoomAt(current.zoom*kotlin.math.exp(-p.scrollDelta.y*.1f),p.position-Offset(size.width/2f,size.height/2f)));event.changes.forEach{it.consume()}}}){
        AssetImage(c,id,Modifier.fillMaxSize().graphicsLayer{scaleX=transform.zoom;scaleY=transform.zoom;translationX=transform.pan.x;translationY=transform.pan.y})
    }
}
