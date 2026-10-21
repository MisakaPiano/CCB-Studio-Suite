@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*

internal class GalleryViewState {
    var copyText:(String)->Unit={java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(it),null)}
    var query by mutableStateOf("");var source by mutableStateOf("all");var date by mutableStateOf("")
    var compare by mutableStateOf<List<String>?>(null);var metadata by mutableStateOf<String?>(null)
    var anchor by mutableStateOf<String?>(null)
}
@Composable internal fun GalleryWorkspace(c:WorkbenchController) {
    val g=c.gallery
    g.metadata?.let{id->MetadataWorkspace(c,id){g.metadata=null};return}
    g.compare?.let{ids->ComparisonWorkspace(c,ids){g.compare=null};return}
    val jobs=c.state.jobs.flatMap{job->(job.results+job.composedResults).map{it to job}}.toMap()
    fun day(id:String)=jobs[id]?.let{java.time.Instant.ofEpochMilli(it.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()}.orEmpty()
    val visible=c.assets.asReversed().filter{a->val id=a.request().id().toString();val job=jobs[id]
        (g.source=="all" || (g.source=="generated")== (job!=null)) && (g.date.isBlank() || day(id)==g.date) &&
        (a.request().name().contains(g.query,true) || job?.recipe?.contains(g.query,true)==true)}
    fun select(id:String,toggle:Boolean,range:Boolean){val ids=visible.map{it.request().id().toString()};val anchor=ids.indexOf(g.anchor);val target=ids.indexOf(id)
        c.selection=when{range && anchor>=0->c.selection+ids.subList(minOf(anchor,target),maxOf(anchor,target)+1);toggle->if(id in c.selection)c.selection-id else c.selection+id;else->setOf(id)}
        c.current=id;if(!range)g.anchor=id
    }
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        StudioActions{StudioAction("导入图片",!c.busy){c.importImages()};StudioAction("返回创作"){c.view="创作"}}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(g.query,{g.query=it},label={Text("查找文件名或历史 Prompt")},modifier=Modifier.weight(1f))
            CompactChoice("来源",listOf("all","generated","imported"),g.source,{when(it){"generated"->"生成历史";"imported"->"导入/副本";else->"全部来源"}}){g.source=it}
            CompactChoice("日期",listOf("")+jobs.keys.map(::day).distinct().sortedDescending(),g.date,{it.ifBlank{"全部日期"}}){g.date=it}
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
            val wide=maxWidth>=900.dp
            Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                LazyVerticalGrid(GridCells.Adaptive(150.dp),Modifier.weight(1f).fillMaxHeight(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    items(visible,key={it.request().id().toString()}){a->val id=a.request().id().toString()
                        Column(Modifier.border(1.dp,if(id in c.selection)Gold else Quiet).padding(6.dp)){
                            Box(Modifier.fillMaxWidth().height(140.dp).semantics{contentDescription="图库图片 $id";onClick{select(id,false,false);true}}
                                .onPointerEvent(PointerEventType.Press){event->if(event.button==PointerButton.Primary)select(id,event.keyboardModifiers.isCtrlPressed,event.keyboardModifiers.isShiftPressed)}){AssetImage(c,id,Modifier.fillMaxSize())}
                            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(id in c.selection,{c.selection=if(it)c.selection+id else c.selection-id},modifier=Modifier.semantics{contentDescription="选择图库图片 $id"});Text(a.request().name(),maxLines=2)}
                            Text(day(id).ifBlank{"日期未记录"},style=MaterialTheme.typography.caption)
                        }
                    }
                }
                if(wide)Column(Modifier.width(300.dp).fillMaxHeight()){
                    AssetImage(c,c.current,Modifier.fillMaxWidth().weight(1f))
                    c.assets.firstOrNull{it.request().id().toString()==c.current}?.let{a->Text(a.request().name());Text("${a.size()} bytes · SHA-256 ${a.sha256()}",style=MaterialTheme.typography.caption)}
                }
            }
        }
        StudioActions{
            StudioAction("全选"){c.selection=visible.map{it.request().id().toString()}.toSet()};StudioAction("全不选"){c.selection=emptySet()};StudioAction("反选"){val ids=visible.map{it.request().id().toString()}.toSet();c.selection=(c.selection-ids)+(ids-c.selection)}
            StatusText("${c.selection.size} 项已选 · ${c.selection.count{id->visible.none{it.request().id().toString()==id}}} 项在筛选外")
            StudioAction("比较 2 张",c.selection.size==2){g.compare=c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()}}
            StudioAction("参数与来源",c.current!=null){g.metadata=c.current}
            StudioAction("所选图片建立编辑工程",c.selection.isNotEmpty() && !c.busy){c.view="编辑"}
        }
    }
}
