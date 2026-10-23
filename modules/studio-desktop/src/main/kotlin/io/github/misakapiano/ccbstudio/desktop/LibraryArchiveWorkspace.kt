@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.foundation.ExperimentalFoundationApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*

@Composable internal fun LibraryArchiveWorkspace(c:WorkbenchController) {
    val l=c.library;val g=l.archiveView
    val focus=remember{FocusRequester()};var focused by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){focus.requestFocus()}
    var current by remember{mutableStateOf<String?>(null)}
    var restore by remember{mutableStateOf<Set<String>?>(null)}
    val contents by produceState<Result<List<io.github.misakapiano.ccbstudio.assets.AssetStore.Asset>>?>(null,l.archived){
        value=withContext(Dispatchers.IO){runCatching{l.archive.manageable().filter{it.request().id().toString() in l.archived}}}
    }
    val assets=remember(contents,l.archived){contents?.getOrNull().orEmpty().filter{it.request().id().toString() in l.archived}};val labels=l.state?.labels.orEmpty()
    val byId=remember(assets){assets.associateBy{it.request().id().toString()}}
    val images=remember(assets){SharedAssets.images(assets).map{it.request().id().toString()}.toSet()}
    val times=remember(assets,c.state.jobs){libraryRecordedTimes(assets,c.state.jobs)}
    val generated=remember(c.state.jobs){c.state.jobs.flatMap{it.results+it.composedResults}.toSet()}
    val sources=remember(assets,c.state.jobs){librarySources(assets,c.state.jobs)}
    val index=remember(assets,labels){LibraryTagIndex(assets.map{it.request().id().toString()},labels)}
    val matchesDate=remember(g.date){libraryDatePredicate(g.date)}
    val visible=remember(assets,g.query,g.filterTags,g.anyTag,g.date,g.source,g.scope,g.sort,g.assetType,labels,times,sources){
        val filtered=assets.filter{a->
            val id=a.request().id().toString();val label=labels[id]?:AssetLabels()
            val type=when(a.request().project().decodeToString()){LibraryRepository.PROMPT->"prompt";SharedAssets.DOCUMENT->"document";else->"image"}
            val source=if(type=="image")sources[id]?:"unknown"else"other"
            (g.scope.ids?.contains(id)!=false)&&(g.source=="all"||g.source==source)&&(g.assetType=="all"||g.assetType==type)&&
                (a.request().name().contains(g.query,true)||label.alias.contains(g.query,true)||label.tags.any{it.contains(g.query,true)})&&
                matchesLibraryTags(label.tags,g.filterTags,g.anyTag)&&matchesDate(times[id])
        }
        sortGalleryAssets(filtered,g.sort,times,labels.mapValues{it.value.alias})
    }
    val ids=remember(visible){visible.map{it.request().id().toString()}}
    val visibleSet=remember(ids){ids.toSet()}
    val history by produceState<GalleryHistoryProjection?>(null,c.state.jobs,c.assets){value=withContext(Dispatchers.IO){runCatching{readGalleryHistory(c.workspace)}.getOrNull()}}
    val albums=remember(ids,assets,history,g.fold,g.directoryFold,g.tagFold,labels,l.state?.folders){
        when {
            g.fold.enabled&&g.tagFold->galleryTagAlbums(ids,labels)
            g.fold.enabled&&g.directoryFold->galleryDirectoryAlbums(ids,assets,l.state?.folders.orEmpty())
            else->galleryAlbums(ids,history?:GalleryHistoryProjection(emptyList(),emptyMap()),g.fold.type.takeIf{g.fold.enabled})
        }
    }
    fun select(id:String,control:Boolean=false,shift:Boolean=false){l.archiveSelection=selectLibraryRange(ids,l.archiveSelection,id,g.anchor,control,shift);if(!shift)g.anchor=id;current=id;focus.requestFocus()}
    fun undoKey(e:KeyEvent):Boolean {
        if(!focused||e.type!=KeyEventType.KeyDown)return false
        return when {
            e.isCtrlPressed&&e.key==Key.A->{l.archiveSelection=ids.toSet();true}
            e.isCtrlPressed&&e.key==Key.Z->{if(e.isShiftPressed)l.redo()else l.undo();true}
            e.isCtrlPressed&&e.key==Key.Y->{l.redo();true}
            e.key==Key.Escape->{l.archiveSelection=emptySet();true}
            e.key==Key.Enter&&l.archiveSelection.isNotEmpty()->{restore=l.archiveSelection.toSet();true}
            else->false
        }
    }
    g.viewing?.let{(ordered,at)->StudioAssetViewer(c,ordered,at){g.viewing=null}}
    g.tagEditor?.let{selected->BatchTagDialog(c,selected){g.tagEditor=null}}
    restore?.let{fixed->AlertDialog(onDismissRequest={restore=null},title={Text("恢复 ${fixed.size} 项资产？")},text={Text("恢复本次选定的资产。其他回收站项目保持原状。")},confirmButton={TextButton(enabled=!c.busy,onClick={l.changeArchive(fixed,false);restore=null}){Text("确认恢复")}},dismissButton={TextButton(onClick={restore=null}){Text("取消")}})}
    LibraryPresentation {
    Column(Modifier.fillMaxSize().focusRequester(focus).onFocusChanged{focused=it.isFocused}.onKeyEvent(::undoKey).focusable(),verticalArrangement=Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            LibraryAction("返回图库"){c.view="图库"}
            LibraryChoice("资产管理",listOf("Prompt 资产","多 Prompt 参照","目录入口","回收站","备份与恢复"),l.tab,{it}){l.tab=it}
            Text("${assets.size} 项")
            Spacer(Modifier.weight(1f))
            Box {
                var info by remember{mutableStateOf(false)}
                LibraryAction("说明",style=StudioActionStyle.TERTIARY){info=true}
                StudioMenu(info,{info=false}){Text("原件、工程引用与外部目录保留。\n此处不执行永久删除。\n日期为有记录的添加/生成日期；未知不补默认值。",Modifier.width(360.dp).padding(12.dp))}
            }
            StudioCompactField("查找回收站文件名、别名或标签",g.query,{g.query=it},Modifier.width(270.dp))
            LibraryChoice("类型",listOf("all","image","prompt","document"),g.assetType,{when(it){"image"->"图片";"prompt"->"Prompt";"document"->"文档";else->"全部"}}){g.assetType=it}
        }
        if(l.archiveError!=null){Text(l.archiveError!!,color=MaterialTheme.colors.error);LibraryAction("重读回收站"){l.refresh()};return@Column}
        if(contents?.isFailure==true){Text("回收站目录不可读取，原件保留",color=MaterialTheme.colors.error);return@Column}
        GalleryAlbumControls(c,g){l.archiveSelection=emptySet()}
        LibraryTagFilter(c,g,index,ids.size)
        if(contents==null)Text("正在读取回收站…")
        LibrarySplitPane(g,Modifier.weight(1f),main={
            val scroll=rememberLazyListState()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=10.dp),state=scroll,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    items(albums,key={it.key}){album->
                        val id=album.cover.image.path;val a=byId.getValue(id);val selected=album.images.all{it.image.path in l.archiveSelection}
                        val grouped=g.fold.enabled&&(album.images.size>1 || g.tagFold || (g.directoryFold&&album.label!="来源目录未记录"))
                        var ctrl by remember{mutableStateOf(false)};var shift by remember{mutableStateOf(false)}
                        fun choose(){
                            if(grouped&&!ctrl&&!shift&&!g.checkboxMode){g.enter(album,sha256((g.scope.identity+album.label).toByteArray()));l.archiveSelection=emptySet()}
                            else if(grouped){val groupIds=album.images.map{it.image.path}.toSet();l.archiveSelection=if(groupIds.all{it in l.archiveSelection})l.archiveSelection-groupIds else l.archiveSelection+groupIds;current=id;focus.requestFocus()}
                            else select(id,ctrl||g.checkboxMode,shift)
                        }
                        ContextMenuArea(items={
                            if(id !in l.archiveSelection)l.archiveSelection=album.images.map{it.image.path}.toSet()
                            listOf(ContextMenuItem("恢复所选资产"){restore=l.archiveSelection.toSet()},ContextMenuItem("编辑标签"){g.tagEditor=l.archiveSelection.toSet()})
                        }) {
                            Row(Modifier.fillMaxWidth().background(if(selected)LibrarySelected else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(5.dp)).border(1.dp,if(selected)Gold else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(5.dp)).padding(6.dp)
                                .onPointerEvent(PointerEventType.Press){ctrl=it.keyboardModifiers.isCtrlPressed;shift=it.keyboardModifiers.isShiftPressed}
                                .combinedClickable(onClick={choose()},onDoubleClick={if(grouped)choose()else if(id in images)g.preview(ids.filter{it in images},id)})) {
                                Box(Modifier.size(64.dp).semantics{contentDescription="回收站资产 $id"}) {
                                    if(id in images)GalleryStaticThumbnail(c,id,Modifier.fillMaxSize())else Text(if(a.request().project().decodeToString()==LibraryRepository.PROMPT)"Prompt"else"文档",Modifier.align(Alignment.Center))
                                    if(grouped)Text("${album.images.size} 项",Modifier.align(Alignment.TopEnd).background(Panel))
                                }
                                Column(Modifier.weight(1f).padding(start=10.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment=Alignment.CenterVertically){if(g.checkboxMode)LibrarySelectionCheckbox(selected,{choose()});Text(if(grouped)album.label.lineSequence().first()else labels[id]?.alias?.ifBlank{a.request().name()}?:a.request().name(),maxLines=2,overflow=TextOverflow.Ellipsis)}
                                Text(libraryRecordedDay(times[id]).ifBlank{"日期未记录"},style=MaterialTheme.typography.caption)
                                Text((if(id in images)"图片 · ${librarySourceLabel(sources[id]?:"unknown")}"else if(a.request().project().decodeToString()==LibraryRepository.PROMPT)"Prompt"else"文档")+" · "+labels[id]?.tags.orEmpty().joinToString(" · "),color=LibraryMuted,maxLines=1,overflow=TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(rememberScrollbarAdapter(scroll),Modifier.align(Alignment.CenterEnd).fillMaxHeight(),style=studioVisibleScrollbarStyle())
                if(contents!=null&&ids.isEmpty())Text(if(assets.isEmpty())"回收站为空"else"没有符合当前筛选的资产",Modifier.align(Alignment.Center))
            }
        },detail={
            val scroll=rememberScrollState()
            LaunchedEffect(ids){if(current !in ids)current=ids.firstOrNull()}
            Column(Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=14.dp).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                val a=byId[current]
                if(a==null)Text("选择资产查看详情",color=Quiet)else {
                    val id=a.request().id().toString()
                    if(id in images)AssetImage(c,id,Modifier.fillMaxWidth().height(180.dp))
                    Text(labels[id]?.alias?.ifBlank{a.request().name()}?:a.request().name())
                    Text("${a.size()} bytes",style=MaterialTheme.typography.caption)
                    Text(labels[id]?.tags.orEmpty().joinToString(" · "))
                    LibraryAction("恢复此项",!c.busy,accessibilityLabel="恢复资产 ${id.take(8)}"){l.changeArchive(setOf(id),false)}
                }
            }
        })
        LibraryBand {
            LibraryToggle("勾选模式",g.checkboxMode){g.checkboxMode=!g.checkboxMode}
            LibraryAction("全选",style=StudioActionStyle.TERTIARY){l.archiveSelection=ids.toSet()};LibraryAction("反选",style=StudioActionStyle.TERTIARY){l.archiveSelection=(l.archiveSelection-ids.toSet())+(ids.toSet()-l.archiveSelection)};LibraryAction("清空",accessibilityLabel="清空选择"){l.archiveSelection=emptySet()}
            Text("已选 ${l.archiveSelection.size} · 筛选外 ${l.archiveSelection.count{it !in visibleSet}}",style=MaterialTheme.typography.caption)
            LibraryAction("",l.canUndo,icon=DesktopAppIcons.Undo,style=StudioActionStyle.TERTIARY,accessibilityLabel="撤销"){l.undo()};LibraryAction("",l.canRedo,icon=DesktopAppIcons.Redo,style=StudioActionStyle.TERTIARY,accessibilityLabel="重做"){l.redo()}
            Spacer(Modifier.weight(1f))
            LibraryAction("恢复所选",l.archiveSelection.isNotEmpty()&&!c.busy){restore=l.archiveSelection.toSet()}
            LibraryAction("恢复当前筛选 ${ids.size} 项",ids.isNotEmpty()&&!c.busy){restore=ids.toSet()}
        }
    }
    }
}
