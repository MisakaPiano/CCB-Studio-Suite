@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*

@Composable internal fun LibraryTagChips(tags:List<String>,counts:Map<String,Int>,remove:(String)->Unit,modifier:Modifier=Modifier) {
    val scroll=rememberLazyListState()
    LaunchedEffect(tags){if(tags.isNotEmpty())scroll.scrollToItem(tags.lastIndex)}
    Column(modifier) {
    LazyRow(Modifier.fillMaxWidth().heightIn(min=32.dp),state=scroll,horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
        items(tags,key={it}){tag->TooltipArea(tooltip={Surface{Text(tag,Modifier.padding(8.dp))}}) {
            Row(Modifier.background(Panel).border(1.dp,DesktopBootstrapColors.border).padding(start=7.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(tag,Modifier.widthIn(max=140.dp),maxLines=1,overflow=TextOverflow.Ellipsis)
                counts[tag]?.let{Text(" $it",style=MaterialTheme.typography.caption)}
                StudioAction("×",accessibilityLabel="移除标签 $tag"){remove(tag)}
            }
        }}
    }
    if(tags.isNotEmpty())HorizontalScrollbar(rememberScrollbarAdapter(scroll),Modifier.fillMaxWidth().height(6.dp).semantics{contentDescription="标签横向滚动条"},style=studioVisibleScrollbarStyle())
    }
}

@Composable internal fun LibraryTagInput(index:LibraryTagIndex,selected:List<String>,add:(List<String>)->Unit,modifier:Modifier=Modifier,allowNew:Boolean=false) {
    var text by remember{mutableStateOf("")};var browse by remember{mutableStateOf(false)}
    var prediction by remember{mutableStateOf(false)}
    fun submit(){val value=text.trim();if(value.isNotEmpty() && (allowNew||value in index.counts)){add(listOf(value));text="";prediction=false}}
    val candidates=remember(text,index,selected){index.suggestions(text).filter{it !in selected}.take(8)}
    Box(modifier) {
        StudioCompactField(if(allowNew)"输入或检索标签"else"添加筛选标签",text,{text=it.take(64);prediction=it.isNotBlank()},Modifier.fillMaxWidth().onKeyEvent{if(it.type==KeyEventType.KeyDown&&it.key==Key.Enter){submit();true}else false},trailing={
            StudioAction("＋",style=StudioActionStyle.TERTIARY,accessibilityLabel="添加输入标签"){submit()}
            StudioAction("⌕",style=StudioActionStyle.TERTIARY,accessibilityLabel="浏览已有标签"){prediction=false;browse=true}
        })
        DropdownMenu(prediction && candidates.isNotEmpty(),{prediction=false},properties=PopupProperties(focusable=false)) {
            candidates.forEach{tag->DropdownMenuItem(onClick={add(listOf(tag));text="";prediction=false}){Text("$tag   ${index.counts[tag]}")}}
        }
    }
    if(browse)DialogWindow(onCloseRequest={browse=false},title="已有标签",state=rememberDialogState(width=620.dp,height=600.dp)) {
        Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){LibraryTagPickerContent(index,selected,{browse=false}){values->add(values);browse=false}}
    }
}

@Composable internal fun LibraryTagPickerContent(index:LibraryTagIndex,already:List<String>,cancel:()->Unit,apply:(List<String>)->Unit) {
    var query by remember{mutableStateOf("")};var pending by remember{mutableStateOf<Set<String>>(emptySet())}
    val tags=remember(query,index){index.suggestions(query)}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        StudioCompactField("检索已有标签",query,{query=it},Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            StudioAction("选择检索结果"){pending=pending+tags.filter{it !in already}}
            StudioAction("清空待添加"){pending=emptySet()}
            Text("待添加 ${pending.size}")
        }
        val state=rememberLazyListState()
        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().padding(end=16.dp),state=state){items(tags,key={it}){tag->
                Row(Modifier.fillMaxWidth().clickable(enabled=tag !in already){pending=if(tag in pending)pending-tag else pending+tag},verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(tag in pending || tag in already,{pending=if(tag in pending)pending-tag else pending+tag},enabled=tag !in already)
                    Text(tag,Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text("${index.counts[tag]} 项"+(if(tag in already)" · 已加入"else""))
                }
            }}
            VerticalScrollbar(rememberScrollbarAdapter(state),Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
        if(tags.isEmpty())Text("没有匹配的标签")
        StudioActions{StudioAction("取消",onClick=cancel);StudioAction("添加所选标签",pending.isNotEmpty()){apply(index.suggestions("").filter{it in pending})}}
    }
}

@Composable internal fun LibraryTagFilter(c:WorkbenchController,g:GalleryViewState,index:LibraryTagIndex,resultCount:Int) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        LibraryTagChips(g.filterTags,index.counts,{g.filterTags=g.filterTags-it},Modifier.weight(1f))
        Box {
            var selected by remember{mutableStateOf(false)}
            StudioAction("已选 ${g.filterTags.size}",g.filterTags.isNotEmpty(),style=StudioActionStyle.TERTIARY){selected=true}
            DropdownMenu(selected,{selected=false}) {
                g.filterTags.forEach{tag->DropdownMenuItem(onClick={g.filterTags=g.filterTags-tag}){Text("$tag   ×",Modifier.widthIn(max=280.dp))}}
                DropdownMenuItem(onClick={g.filterTags=emptyList();selected=false}){Text("清空筛选标签")}
            }
        }
        LibraryTagInput(index,g.filterTags,{g.filterTags=(g.filterTags+it).distinct()},Modifier.width(280.dp))
        LibraryChoice("",listOf(false,true),g.anyTag,{if(it)"任一标签"else"全部标签"}){g.anyTag=it}
        Text("$resultCount 项",style=MaterialTheme.typography.caption)
    }
}

@Composable internal fun BatchTagDialog(c:WorkbenchController,ids:Set<String>,close:()->Unit) {
    val labels=c.library.state?.labels.orEmpty()
    val existing=remember(ids,labels){ids.flatMap{labels[it]?.tags.orEmpty()}.distinct()}
    val index=remember(c.assets,c.library.archived,labels){LibraryTagIndex(c.assets.map{it.request().id().toString()}.filter{it !in c.library.archived},labels)}
    var pending by remember(ids){mutableStateOf<List<String>>(emptyList())}
    var remove by remember(ids){mutableStateOf<List<String>>(emptyList())}
    val tooMany=ids.any{((labels[it]?.tags.orEmpty()-remove.toSet())+pending).distinct().size>32}
    DialogWindow(onCloseRequest=close,title="批量标签 · ${ids.size} 项",state=rememberDialogState(width=720.dp,height=420.dp)) {
        Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground) {
            Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("添加到所选项；未选择移除的原有标签继续保留。")
                LibraryTagChips(pending,index.counts,{pending=pending-it},Modifier.fillMaxWidth())
                LibraryTagInput(index,pending,{pending=(pending+it).distinct()},Modifier.fillMaxWidth(),allowNew=true)
                Text("原有标签 · 点击标记移除")
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    existing.forEach{tag->StudioAction((if(tag in remove)"− "else"")+tag,selected=tag in remove){remove=if(tag in remove)remove-tag else remove+tag}}
                }
                Text("每项最多 32 个标签，每个最多 64 字。",style=MaterialTheme.typography.caption)
                Spacer(Modifier.weight(1f))
                if(tooMany)Text("部分资产将超过 32 个标签，请先减少待添加标签。",color=MaterialTheme.colors.error)
                StudioActions{StudioAction("取消",onClick=close);StudioAction("保存标签",!c.busy&&!tooMany&&(pending.isNotEmpty()||remove.isNotEmpty())){
                    c.library.changeLabels(ids){it.copy(tags=((it.tags-remove.toSet())+pending).distinct())};close()
                }}
            }
        }
    }
}
