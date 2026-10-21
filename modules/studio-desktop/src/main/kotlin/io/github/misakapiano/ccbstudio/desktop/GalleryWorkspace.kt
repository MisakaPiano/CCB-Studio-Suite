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
import kotlinx.serialization.json.*

internal class GalleryViewState {
    var pasteContents:()->java.awt.datatransfer.Transferable?={java.awt.Toolkit.getDefaultToolkit().systemClipboard.getContents(null)}
    var imports by mutableStateOf<List<GalleryImportResult>>(emptyList())
    var copyText:(String)->Unit={java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(it),null)}
    var query by mutableStateOf("");var source by mutableStateOf("all");var date by mutableStateOf("")
    var compare by mutableStateOf<List<String>?>(null);var metadata by mutableStateOf<String?>(null)
    var anchor by mutableStateOf<String?>(null)
}
@Composable internal fun GalleryWorkspace(c:WorkbenchController) {
    val g=c.gallery
    val labels=c.library.state?.labels.orEmpty()
    if(c.animation.ids!=null){GalleryAnimationWorkspace(c);return}
    g.metadata?.let{id->MetadataWorkspace(c,id){g.metadata=null};return}
    g.compare?.let{ids->ComparisonWorkspace(c,ids){g.compare=null};return}
    val jobs=c.state.jobs.flatMap{job->(job.results+job.composedResults).map{it to job}}.toMap()
    val importedAt=remember(c.assets){c.assets.mapNotNull{a->runCatching{
        val bytes=a.request().recipe();if(bytes.size>4096)return@runCatching null
        val receipt=Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        if(receipt["kind"]?.jsonPrimitive?.content!="image-ingress" || receipt["version"]?.jsonPrimitive?.intOrNull!=1)return@runCatching null
        a.request().id().toString() to requireNotNull(receipt["createdAt"]?.jsonPrimitive?.longOrNull)
    }.getOrNull()}.toMap()}
    fun day(id:String)=runCatching{(jobs[id]?.createdAt?:importedAt[id])?.let{java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()}.orEmpty()}.getOrDefault("")
    val visible=c.assets.asReversed().filter{a->val id=a.request().id().toString();val job=jobs[id]
        (g.source=="all" || (g.source=="generated")== (job!=null)) && (g.date.isBlank() || day(id)==g.date) &&
        (a.request().name().contains(g.query,true) || labels[id]?.alias?.contains(g.query,true)==true || labels[id]?.tags?.any{it.contains(g.query,true)}==true || job?.recipe?.contains(g.query,true)==true)}
    fun select(id:String,toggle:Boolean,range:Boolean){val ids=visible.map{it.request().id().toString()};val anchor=ids.indexOf(g.anchor);val target=ids.indexOf(id)
        c.selection=when{range && anchor>=0->c.selection+ids.subList(minOf(anchor,target),maxOf(anchor,target)+1);toggle->if(id in c.selection)c.selection-id else c.selection+id;else->setOf(id)}
        c.current=id;if(!range)g.anchor=id
    }
    Column(Modifier.fillMaxSize().galleryIngress(c).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        StudioActions{StudioAction("导入图片",!c.busy){c.importImages()};StudioAction("粘贴图片",!c.busy){if(!c.pasteImages())c.problem="剪贴板没有图片或文件列表；文字请粘贴到输入框"};StudioAction("返回创作"){c.view="创作"}}
        Text("拖入文件 / Ctrl+V · PNG、JPEG、WebP、GIF、BMP · 每文件≤32MiB，≤12582912像素；动图预览首帧，原文件保留",style=MaterialTheme.typography.caption)
        SharedDocumentAssets(c)
        if(g.imports.isNotEmpty()){
            var show by remember{mutableStateOf(false)}
            StudioAction("导入结果 ${g.imports.count{it.assetId!=null}} / ${g.imports.size}"){show=!show}
            if(show)Column(Modifier.fillMaxWidth().heightIn(max=140.dp).verticalScroll(rememberScrollState())){g.imports.forEach{Text("${it.name} · ${it.status}",style=MaterialTheme.typography.caption)}}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(g.query,{g.query=it},label={Text("查找文件名或历史 Prompt")},modifier=Modifier.weight(1f))
            CompactChoice("来源",listOf("all","generated","imported"),g.source,{when(it){"generated"->"生成历史";"imported"->"导入/副本";else->"全部来源"}}){g.source=it}
            CompactChoice("日期",listOf("")+(jobs.keys+importedAt.keys).map(::day).filter{it.isNotBlank()}.distinct().sortedDescending(),g.date,{it.ifBlank{"全部日期"}}){g.date=it}
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
            val wide=maxWidth>=900.dp
            Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                LazyVerticalGrid(GridCells.Adaptive(150.dp),Modifier.weight(1f).fillMaxHeight(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    items(visible,key={it.request().id().toString()}){a->val id=a.request().id().toString()
                        Column(Modifier.border(1.dp,if(id in c.selection)Gold else Quiet).padding(6.dp)){
                            Box(Modifier.fillMaxWidth().height(140.dp).semantics{contentDescription="图库图片 $id";onClick{select(id,false,false);true}}
                                .onPointerEvent(PointerEventType.Press){event->if(event.button==PointerButton.Primary)select(id,event.keyboardModifiers.isCtrlPressed,event.keyboardModifiers.isShiftPressed)}){AssetImage(c,id,Modifier.fillMaxSize())}
                            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(id in c.selection,{c.selection=if(it)c.selection+id else c.selection-id},modifier=Modifier.semantics{contentDescription="选择图库图片 $id"});Text(labels[id]?.alias?.takeIf{it.isNotBlank()}?:a.request().name(),maxLines=2)}
                            labels[id]?.tags?.takeIf{it.isNotEmpty()}?.let{Text(it.joinToString(" · "),style=MaterialTheme.typography.caption)}
                            Text(day(id).ifBlank{"日期未记录"},style=MaterialTheme.typography.caption)
                        }
                    }
                }
                if(wide)Column(Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    AssetImage(c,c.current,Modifier.fillMaxWidth().height(240.dp))
                    c.assets.firstOrNull{it.request().id().toString()==c.current}?.let{a->Text(a.request().name());Text("${a.size()} bytes · SHA-256 ${a.sha256()}",style=MaterialTheme.typography.caption)}
                    c.current?.let{ImagePromptPreview(c,it)}
                }
            }
        }
        StudioActions{
            StudioAction("全选"){c.selection=visible.map{it.request().id().toString()}.toSet()};StudioAction("全不选"){c.selection=emptySet()};StudioAction("反选"){val ids=visible.map{it.request().id().toString()}.toSet();c.selection=(c.selection-ids)+(ids-c.selection)}
            StatusText("${c.selection.size} 项已选 · ${c.selection.count{id->visible.none{it.request().id().toString()==id}}} 项在筛选外")
            StudioAction("比较 2 张",c.selection.size==2){g.compare=c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()}}
            StudioAction("所选图片多 Prompt 参照",c.selection.isNotEmpty()&&!c.busy){c.run("建立图片参照"){
                val results=c.assets.filter{it.request().id().toString() in c.selection}.map{a->runCatching{saveImagePrompt(c,a.request().id().toString(),true)}}
                val failed=results.count{it.isFailure};if(failed>0)c.problem="$failed 张图片没有可读取的非空 Prompt，未加入；其他已保存的参照保留。"
                c.library.tab="多 Prompt 参照";c.view="资产管理"
            }}
            StudioAction("参数与来源",c.current!=null){g.metadata=c.current}
            StudioAction("选中图片用于引导",c.current!=null&&!c.busy){c.section="图像引导";c.view="创作"}
            StudioAction("APNG伪装 / 还原",c.selection.isNotEmpty()&&!c.busy){c.animation.open(c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()})}
            StudioAction("所选图片建立编辑工程",c.selection.isNotEmpty() && !c.busy){c.view="编辑"}
        }
    }
}
