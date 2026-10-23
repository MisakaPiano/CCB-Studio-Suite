@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.foundation.ExperimentalFoundationApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import kotlinx.coroutines.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.json.*

internal data class GalleryHistoryIdentity(val assets:List<Pair<String,String>>,val jobs:List<JobRecord>)
internal class GalleryViewState(val namespace:String="",rootLabel:String="全部资产") {
    var importProgress by mutableStateOf("")
    var importStartedNanos:Long=0
    var importDurationMs by mutableStateOf<Long?>(null)
    var importTimings by mutableStateOf<Map<String,Long>>(emptyMap())
    var historyCache:Pair<GalleryHistoryIdentity,GalleryHistoryProjection>?=null
    var historyReadOverride:(suspend ()->GalleryHistoryProjection)?=null
    var pasteContents:()->java.awt.datatransfer.Transferable?={java.awt.Toolkit.getDefaultToolkit().systemClipboard.getContents(null)}
    var imports by mutableStateOf<List<GalleryImportResult>>(emptyList())
    var copyText:(String)->Unit={java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(it),null)}
    var query by mutableStateOf("");var source by mutableStateOf("all");var date by mutableStateOf("")
    var compare by mutableStateOf<List<String>?>(null);var metadata by mutableStateOf<String?>(null)
    var anchor by mutableStateOf<String?>(null)
    var levels by mutableStateOf(listOf(GalleryScope(label=rootLabel,identity=if(namespace.isBlank())"root"else sha256(namespace.toByteArray()))))
    var filterTags by mutableStateOf<List<String>>(emptyList())
    var anyTag by mutableStateOf(false)
    var tagFoldDepths by mutableStateOf<Set<Int>>(emptySet())
    val tagFold get()=depth in tagFoldDepths
    var checkboxMode by mutableStateOf(false)
    var detailWidth by mutableStateOf(340f)
    val scrollPositions=mutableMapOf<String,Pair<Int,Int>>()
    var assetType by mutableStateOf("all")
    var tagEditor by mutableStateOf<Set<String>?>(null)
    var clipboard:LibraryClipboard?=null
    var copyFiles:(List<java.nio.file.Path>)->Unit=ExportClipboard::copy
    var writeTransferable:(java.awt.datatransfer.Transferable)->Unit={java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(it,null)}
    var guidanceSelection by mutableStateOf<List<String>?>(null)
    private data class Visit(val levels:List<GalleryScope>,val query:String,val date:String,val source:String,val tags:List<String>,val anyTag:Boolean,val type:String)
    private var previous by mutableStateOf<List<Visit>>(emptyList())
    private var next by mutableStateOf<List<Visit>>(emptyList())
    val canGoBack get()=previous.isNotEmpty()
    val canGoForward get()=next.isNotEmpty()
    private fun snapshot()=Visit(levels,query,date,source,filterTags,anyTag,assetType)
    private fun restore(v:Visit){levels=v.levels;query=v.query;date=v.date;source=v.source;filterTags=v.tags;anyTag=v.anyTag;assetType=v.type;anchor=null}
    private fun record(){previous=(previous+snapshot()).takeLast(100);next=emptyList()}
    fun goBack(){previous.lastOrNull()?.let{v->next=next+snapshot();previous=previous.dropLast(1);restore(v)}}
    fun goForward(){next.lastOrNull()?.let{v->previous=previous+snapshot();next=next.dropLast(1);restore(v)}}
    val depth get()=levels.lastIndex
    val scope get()=levels.last()
    var foldPreferences by mutableStateOf<Map<Int,NovelAiHistoryFoldPreference>?>(null)
    var directoryFoldDepths by mutableStateOf<Set<Int>?>(null)
    var sortPreferences by mutableStateOf<Map<String,GallerySort>?>(null)
    val sort get()=sortPreferences?.get(scope.identity)?:sortPreferences?.get("root")?:GallerySort()
    val directoryFold get()=depth in directoryFoldDepths.orEmpty()
    val fold get()=foldPreferences?.get(depth)?:NovelAiHistoryFoldPreference(depth)
    var historyProblem by mutableStateOf<String?>(null)
    var viewing by mutableStateOf<Pair<List<String>,Int>?>(null)
    var previewOverride:((List<String>,Int)->Unit)?=null
    fun preview(ids:List<String>,id:String){val index=ids.indexOf(id);if(index<0)return;previewOverride?.invoke(ids,index)?:run{viewing=ids to index}}
    fun enter(album:NovelAiHistoryAlbum,identity:String=sha256((scope.identity+album.images.map{it.image.path}.sorted().joinToString()).toByteArray())){
        if(depth>=31){historyProblem="相册层级已达32层，请返回上层调整分组";return}
        record()
        levels=levels.dropLast(1)+scope.copy(query=query,date=date,source=source)+GalleryScope(album.images.map{it.image.path}.toSet(),album.label,identity=identity,tag=album.label.takeIf{tagFold&&album.cover.entry.id.startsWith("tag:${sha256(it.toByteArray())}:")})
        query="";date="";source="all";filterTags=emptyList()
    }
    fun back(){if(depth>0){record();levels=levels.dropLast(1);query=scope.query;date=scope.date;source=scope.source;filterTags=emptyList();anchor=null}}
    fun goLevel(index:Int){if(index in 0 until depth){record();levels=levels.take(index+1);query=scope.query;date=scope.date;source=scope.source;filterTags=emptyList();anchor=null}}
}
@Composable internal fun GalleryWorkspace(c:WorkbenchController) {
    val g=c.gallery;val labels=c.library.state?.labels.orEmpty()
    if(c.animation.ids!=null){GalleryAnimationWorkspace(c);return}
    g.metadata?.let{id->MetadataWorkspace(c,id){g.metadata=null};return}
    g.compare?.let{ids->ComparisonWorkspace(c,ids){g.compare=null};return}
    g.viewing?.let{(ids,index)->StudioAssetViewer(c,ids,index){g.viewing=null}}
    g.tagEditor?.let{ids->BatchTagDialog(c,ids){g.tagEditor=null}}
    g.guidanceSelection?.let{ids->GalleryGuidanceDialog(c,ids){g.guidanceSelection=null}}
    val focus=remember{FocusRequester()};var focused by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){focus.requestFocus()}
    val drag=remember{LibraryDragState()};var rootOffset by remember{mutableStateOf(Offset.Zero)};var rootSize by remember{mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)}
    val historyIdentity=GalleryHistoryIdentity(c.assets.map{it.request().id().toString() to it.sha256()},c.state.jobs)
    val cached=g.historyCache?.takeIf{it.first==historyIdentity}?.second
    val history by produceState<Result<GalleryHistoryProjection>?>(cached?.let{Result.success(it)},historyIdentity) {
        if(cached!=null){value=Result.success(cached);return@produceState};value=null
        val result=withContext(Dispatchers.IO){try{Result.success(g.historyReadOverride?.invoke()?:readGalleryHistory(c.workspace))}catch(e:CancellationException){throw e}catch(e:Exception){Result.failure(e)}}
        currentCoroutineContext().ensureActive();result.getOrNull()?.let{g.historyCache=historyIdentity to it};value=result
    }
    val projection=history?.getOrNull()?:GalleryHistoryProjection(emptyList(),emptyMap())
    val times=remember(c.assets,c.state.jobs){libraryRecordedTimes(c.assets,c.state.jobs)}
    val generated=remember(c.state.jobs){c.state.jobs.flatMap{it.results+it.composedResults}.toSet()}
    val activeIds=remember(c.assets,c.library.archived){c.assets.map{it.request().id().toString()}.filter{it !in c.library.archived}}
    val tagIndex=remember(activeIds,labels){LibraryTagIndex(activeIds,labels)}
    val promptMatches=remember(projection,g.query){NovelAiHistoryFilterPolicy.filter(projection.entries,g.query,null).map{it.image.path}.toSet()}
    val visible=remember(c.assets,c.library.archived,g.query,g.filterTags,g.anyTag,g.date,g.source,g.scope,g.sort,labels,times,promptMatches){
        sortGalleryAssets(c.assets,g.sort,times,labels.mapValues{it.value.alias}).filter{a->
            val id=a.request().id().toString();val label=labels[id]?:AssetLabels()
            id !in c.library.archived&&(g.source=="all"||(g.source=="generated")== (id in generated))&&g.scope.ids?.contains(id)!=false&&
                runCatching{libraryDateMatches(times[id],g.date)}.getOrDefault(false)&&matchesLibraryTags(label.tags,g.filterTags,g.anyTag)&&
                (a.request().name().contains(g.query,true)||label.alias.contains(g.query,true)||label.tags.any{it.contains(g.query,true)}||id in promptMatches)
        }
    }
    val visibleIds=visible.map{it.request().id().toString()}
    val orderedSelection=(visibleIds+c.assets.map{it.request().id().toString()}).distinct().filter{it in c.selection&&it in activeIds}
    val albums=remember(visibleIds,projection,g.fold,g.directoryFold,g.tagFold,g.sort,c.assets,c.library.state?.folders,labels){
        val grouped=when {
            g.fold.enabled&&g.tagFold->galleryTagAlbums(visibleIds,labels)
            g.fold.enabled&&g.directoryFold->galleryDirectoryAlbums(visibleIds,c.assets,c.library.state?.folders.orEmpty())
            else->galleryAlbums(visibleIds,projection,g.fold.type.takeIf{g.fold.enabled})
        }
        val order=visibleIds.withIndex().associate{it.value to it.index}
        val arranged=grouped.map{NovelAiHistoryAlbum(it.images.sortedBy{image->order.getValue(image.image.path)},it.label)}
        if(g.sort.field==GallerySortField.NAME&&g.fold.enabled)arranged.sortedWith { a,b->
            fun name(album:NovelAiHistoryAlbum)=album.label.takeIf{it.isNotBlank()}?:visible.first{it.request().id().toString()==album.cover.image.path}.request().name()
            val compared=galleryNaturalCompare(name(a),name(b));(if(g.sort.descending)-compared else compared).takeIf{it!=0}?:a.key.compareTo(b.key)
        }else arranged
    }
    fun remove(){if(c.selection.isNotEmpty()&&!c.busy&&c.library.archiveError==null)c.library.archiveRequest=c.selection.toSet()}
    fun select(id:String,ctrl:Boolean=false,shift:Boolean=false){c.selection=selectLibraryRange(visibleIds,c.selection,id,g.anchor,ctrl,shift);if(!shift)g.anchor=id;c.current=id;focus.requestFocus()}
    fun cut(){val tag=g.scope.tag;if(tag==null)c.problem="请先进入标签相册；剪切用于移动标签归属，外部目录保持只读"else copyLibrarySelection(c,orderedSelection,tag)}
    fun commands(e:KeyEvent):Boolean {
        if(!focused||e.type!=KeyEventType.KeyDown)return false
        if(e.key==Key.Escape){if(drag.dragging.isNotEmpty()||drag.marquee!=null)drag.cancel()else c.selection=emptySet();return true}
        if(c.busy)return false
        return when {
            e.isCtrlPressed&&e.key==Key.A->{c.selection=visibleIds.toSet();true}
            e.isCtrlPressed&&e.key==Key.Z->{if(e.isShiftPressed)c.library.redo()else c.library.undo();true}
            e.isCtrlPressed&&e.key==Key.Y->{c.library.redo();true}
            e.isCtrlPressed&&e.key==Key.C->{copyLibrarySelection(c,orderedSelection);true}
            e.isCtrlPressed&&e.key==Key.X->{cut();true}
            e.isCtrlPressed&&e.key==Key.V->{pasteLibrarySelection(c);true}
            e.isAltPressed&&e.key==Key.DirectionLeft->{g.goBack();c.selection=emptySet();true}
            e.isAltPressed&&e.key==Key.DirectionRight->{g.goForward();c.selection=emptySet();true}
            e.isAltPressed&&e.key==Key.DirectionUp->{g.back();c.selection=emptySet();true}
            e.key==Key.Delete->{remove();true}
            e.key==Key.Enter->{c.current?.takeIf{it in visibleIds}?.let{g.preview(visibleIds,it)};true}
            e.key==Key.Spacebar->{c.current?.let{select(it,true)};true}
            e.key in listOf(Key.DirectionLeft,Key.DirectionRight,Key.DirectionUp,Key.DirectionDown)->{
                val delta=if(e.key==Key.DirectionLeft||e.key==Key.DirectionUp)-1 else 1
                val at=visibleIds.indexOf(c.current);visibleIds.getOrNull((at+delta).coerceIn(0,visibleIds.lastIndex.coerceAtLeast(0)))?.let{select(it,e.isCtrlPressed,e.isShiftPressed)};true
            }
            else->false
        }
    }
    val byId=remember(visible){visible.associateBy{it.request().id().toString()}}
    Box(Modifier.fillMaxSize().onGloballyPositioned{rootOffset=it.positionInRoot()}.onSizeChanged{rootSize=it}.libraryDrag(drag,{c.selection},{c.selection=it},{visibleIds},{focus.requestFocus()})
        .focusRequester(focus).onFocusChanged{focused=it.isFocused}.onKeyEvent(::commands).focusable()) {
        Column(Modifier.fillMaxSize().galleryIngress(c,handlePaste=false).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                StudioAction("返回创作"){c.view="创作"}
                StudioAction("导入图片",!c.busy){c.importImages()}
                StudioAction("粘贴图片",!c.busy){pasteLibrarySelection(c)}
                GalleryImportReceipt(c)
                Spacer(Modifier.weight(1f))
                GalleryImportHelp(c)
                GallerySearch(c,Modifier.width(350.dp))
            }
            c.library.archiveError?.let{Text(it,color=MaterialTheme.colors.error)}
            if(c.busy&&c.activeOperation=="导入图片")StudioActions{StatusText(g.importProgress);StudioAction("停止导入图片"){c.active?.cancel()}}
            if(c.busy&&c.activeOperation=="复制所选原始文件")StudioAction("停止复制所选原始文件"){c.active?.cancel()}
            SharedDocumentAssets(c)
            GalleryAlbumControls(c)
            LibraryTagFilter(c,g,tagIndex,visibleIds.size)
            if(history==null)StatusText("正在核验生成记录，图片原件可查看…")
            else if(history!!.isFailure)StatusText("生成记录不可读取，分组暂不可用；原件保留")
            else if(projection.unavailable.isNotEmpty())StatusText("${projection.unavailable.size} 张图片的生成记录不可核验，单独显示；原件保留")
            LibrarySplitPane(g,Modifier.weight(1f),main={
                val scroll=rememberLazyGridState()
                val scopeIdentity=g.scope.identity
                DisposableEffect(scopeIdentity){onDispose{g.scrollPositions[scopeIdentity]=scroll.firstVisibleItemIndex to scroll.firstVisibleItemScrollOffset}}
                LaunchedEffect(scopeIdentity){val position=g.scrollPositions[scopeIdentity];scroll.scrollToItem(position?.first?:0,position?.second?:0)}
                LaunchedEffect(drag.position,drag.dragging.isNotEmpty(),drag.marquee!=null){
                    if(drag.dragging.isNotEmpty()||drag.marquee!=null){
                        val y=drag.position.y;val delta=when{y<drag.grid.top+28->-18f;y>drag.grid.bottom-28->18f;else->0f}
                        if(delta!=0f)while(isActive){scroll.scrollBy(delta);delay(40)}
                    }
                }
                Box(Modifier.fillMaxSize().onGloballyPositioned{drag.grid=it.boundsInRoot().translate(-rootOffset)}) {
                    LazyVerticalGrid(GridCells.Adaptive(150.dp),Modifier.fillMaxSize().padding(end=16.dp),state=scroll,horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        items(albums,key={it.key}){album->
                            val id=album.cover.image.path;val a=byId.getValue(id);val albumIds=album.images.map{it.image.path}.distinct()
                            val selected=albumIds.all{it in c.selection}
                            val grouped=g.fold.enabled&&(album.images.size>1||g.tagFold||(g.directoryFold&&album.label!="来源目录未记录"))
                            var ctrl by remember{mutableStateOf(false)};var shift by remember{mutableStateOf(false)}
                            fun enter(){g.enter(album,if(g.tagFold)sha256((g.scope.identity+"tag:"+album.label).toByteArray())else galleryAlbumIdentity(g.scope.identity,album,g.fold.type,g.directoryFold,c.assets));c.selection=emptySet();g.anchor=null}
                            fun choose(){
                                if(grouped&&!ctrl&&!shift&&!g.checkboxMode)enter()
                                else if(grouped){if(shift)select(id,ctrl,true)else c.selection=if(selected)c.selection-albumIds.toSet()else c.selection+albumIds;c.current=id;focus.requestFocus()}
                                else select(id,ctrl||g.checkboxMode,shift)
                            }
                            DisposableEffect(album.key){onDispose{drag.cards.remove(album.key)}}
                            ContextMenuArea(items={buildList {
                                if(!c.busy){
                                    add(ContextMenuItem("进入编辑"){runGalleryTool(c,"edit",c.assets.map{it.request().id().toString()}.filter{it in c.selection})})
                                    add(ContextMenuItem("编辑标签"){g.tagEditor=c.selection.toSet()})
                                    add(ContextMenuItem("复制所选原始文件"){copyLibrarySelection(c,c.assets.map{it.request().id().toString()}.filter{it in c.selection})})
                                    g.scope.tag?.let{tag->add(ContextMenuItem("剪切标签归属"){copyLibrarySelection(c,c.assets.map{it.request().id().toString()}.filter{it in c.selection},tag)})}
                                    add(ContextMenuItem("粘贴"){pasteLibrarySelection(c)})
                                    add(ContextMenuItem("APNG伪装 / 还原"){runGalleryTool(c,"apng",c.assets.map{it.request().id().toString()}.filter{it in c.selection})})
                                    add(ContextMenuItem("所选资产移入回收站"){remove()})
                                    if(c.library.canUndo)add(ContextMenuItem("撤销"){c.library.undo()})
                                    if(c.library.canRedo)add(ContextMenuItem("重做"){c.library.redo()})
                                }
                            }}) {
                                Column(Modifier.libraryCardBounds(drag,album.key,albumIds){rootOffset}.border(if(selected)2.dp else 1.dp,if(selected)Gold else Quiet).padding(6.dp)
                                    .onPointerEvent(PointerEventType.Press){event->ctrl=event.keyboardModifiers.isCtrlPressed;shift=event.keyboardModifiers.isShiftPressed;if(event.buttons.isSecondaryPressed){if(!selected)c.selection=albumIds.toSet();c.current=id;focus.requestFocus()}}
                                    .combinedClickable(onClick={choose()},onDoubleClick={if(grouped)enter()else{c.current=id;g.preview(visibleIds,id)}})) {
                                    Box(Modifier.fillMaxWidth().height(135.dp).semantics{contentDescription=if(grouped)"图库相册 $id"else"图库图片 $id"}) {
                                        AssetImage(c,id,Modifier.fillMaxSize())
                                        if(grouped)Text("${album.images.size} 张",Modifier.align(Alignment.TopEnd).background(Panel).padding(4.dp))
                                        if(selected&&!g.checkboxMode)Text("✓",Modifier.align(Alignment.TopStart).background(Gold).padding(3.dp),color=Ink)
                                    }
                                    Row(verticalAlignment=Alignment.CenterVertically) {
                                        if(g.checkboxMode)Checkbox(selected,{val old=g.checkboxMode;g.checkboxMode=true;choose();g.checkboxMode=old},Modifier.size(28.dp).semantics{contentDescription="选择图库图片 $id"})
                                        Text(if(grouped)album.label.lineSequence().first()else labels[id]?.alias?.takeIf{it.isNotBlank()}?:a.request().name(),maxLines=2,overflow=TextOverflow.Ellipsis)
                                    }
                                    labels[id]?.tags?.takeIf{it.isNotEmpty()}?.let{Text(it.joinToString(" · "),maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)}
                                    Text(libraryRecordedDay(times[id]).ifBlank{"日期未记录"},style=MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                    VerticalScrollbar(rememberScrollbarAdapter(scroll),Modifier.align(Alignment.CenterEnd).fillMaxHeight().semantics{contentDescription="图库图片滚动条"},style=studioVisibleScrollbarStyle())
                    if(visibleIds.isEmpty())Text("没有符合当前筛选的图片",Modifier.align(Alignment.Center),color=Quiet)
                }
            },detail={
                val scroll=rememberScrollState()
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(end=16.dp).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        AssetImage(c,c.current,Modifier.fillMaxWidth().height(240.dp))
                        c.assets.firstOrNull{it.request().id().toString()==c.current}?.let{a->Text(labels[c.current]?.alias?.ifBlank{a.request().name()}?:a.request().name());StudioDisclosure("文件详情"){Text("${a.size()} bytes · SHA-256 ${a.sha256()}",style=MaterialTheme.typography.caption)}}
                        c.current?.let{ImagePromptPreview(c,it);AssetLabelEditor(c,it)}
                    }
                    VerticalScrollbar(rememberScrollbarAdapter(scroll),Modifier.align(Alignment.CenterEnd).fillMaxHeight().semantics{contentDescription="图片详情滚动条"},style=studioVisibleScrollbarStyle())
                }
            })
            StudioActions {
                StudioSwitch("勾选模式",g.checkboxMode){g.checkboxMode=!g.checkboxMode}
                StudioAction("全选"){c.selection=visibleIds.toSet()};StudioAction("反选"){c.selection=(c.selection-visibleIds.toSet())+(visibleIds.toSet()-c.selection)};StudioAction("清空选择"){c.selection=emptySet()}
                Text("已选 ${c.selection.size} · 筛选外 ${c.selection.count{it !in visibleIds}}",style=MaterialTheme.typography.caption)
                StudioAction("撤销",c.library.canUndo){c.library.undo()};StudioAction("重做",c.library.canRedo){c.library.redo()}
                StudioAction("所选资产移入回收站",c.selection.isNotEmpty()&&!c.busy&&c.library.error==null&&c.library.archiveError==null,icon=DesktopAppIcons.Delete){remove()}
                Box {
                    var menu by remember{mutableStateOf(false)}
                    StudioAction("操作",icon=DesktopAppIcons.More){menu=true}
                    DropdownMenu(menu,{menu=false}) {
                        DropdownMenuItem(enabled=orderedSelection.isNotEmpty(),onClick={menu=false;copyLibrarySelection(c,orderedSelection)}){Text("复制所选原始文件  Ctrl+C")}
                        DropdownMenuItem(enabled=orderedSelection.isNotEmpty()&&g.scope.tag!=null,onClick={menu=false;cut()}){Text("剪切标签归属  Ctrl+X")}
                        DropdownMenuItem(onClick={menu=false;pasteLibrarySelection(c)}){Text("粘贴  Ctrl+V")}
                        DropdownMenuItem(onClick={menu=false;c.library.tab="目录入口";c.view="资产管理"}){Text("管理收集与导入目录")}
                    }
                }
            }
            GalleryTools(c,drag,{rootOffset},orderedSelection)
        }
        val density=androidx.compose.ui.platform.LocalDensity.current
        drag.marquee?.let{rect->Box(Modifier.offset(with(density){rect.left.toDp()},with(density){rect.top.toDp()}).size(with(density){rect.width.toDp()},with(density){rect.height.toDp()}).background(Gold.copy(alpha=.15f)).border(1.dp,Gold))}
        if(drag.dragging.isNotEmpty())Surface(Modifier.offset(with(density){(drag.position.x+14).coerceIn(0f,(rootSize.width-240*density.density).coerceAtLeast(0f)).toDp()},with(density){(drag.position.y+16).coerceIn(0f,(rootSize.height-90*density.density).coerceAtLeast(0f)).toDp()}).width(220.dp),color=Panel,elevation=8.dp) {
            Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                drag.dragging.take(3).forEach{QueueStaticThumbnail(c,it,Modifier.size(34.dp))}
                Text("${drag.dragging.size} 项\n${drag.activeTarget?:"拖到下方工具"}",style=MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable private fun GalleryImportReceipt(c:WorkbenchController) {
    val g=c.gallery;if(g.imports.isEmpty())return
    var open by remember{mutableStateOf(false)}
    Box {
        StudioAction("导入结果 ${g.imports.count{it.assetId!=null}} / ${g.imports.size}",style=StudioActionStyle.TERTIARY){open=true}
        DropdownMenu(open,{open=false}) {
            Column(Modifier.width(420.dp).heightIn(max=280.dp).verticalScroll(rememberScrollState()).padding(8.dp)) {
                g.importDurationMs?.let{Text("用时 ${"%.1f".format(it/1000.0)} 秒")}
                g.imports.forEach{Text("${it.name} · ${it.status}",style=MaterialTheme.typography.caption)}
                StudioDisclosure("详细用时"){Text("分阶段用时(ms)：${g.importTimings}",style=MaterialTheme.typography.caption)}
            }
        }
    }
}
@Composable private fun GalleryImportHelp(c:WorkbenchController) {
    var open by remember{mutableStateOf(false)}
    Box {StudioAction("导入说明",style=StudioActionStyle.TERTIARY){open=true}
        DropdownMenu(open,{open=false}){Text("拖入文件或 Ctrl+V 粘贴；PNG/JPEG/WebP/GIF/BMP。\n每文件最多32MiB、12582912像素。\n动图预览首帧，原文件保留。\n目录入口同步会入库保留原件，外部文件不变。",Modifier.width(350.dp).padding(12.dp))}
    }
}
@Composable private fun GalleryGuidanceDialog(c:WorkbenchController,ids:List<String>,close:()->Unit) {
    var current by remember(ids){mutableStateOf(ids.first())}
    AlertDialog(onDismissRequest=close,title={Text("图像引导 · ${ids.size} 张")},text={Column {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){ids.forEach{id->Box(Modifier.size(72.dp).border(if(id==current)2.dp else 1.dp,if(id==current)Gold else Quiet).clickable{current=id}){QueueStaticThumbnail(c,id,Modifier.fillMaxSize())}}}
        Text("选择其中一张并指定引导用途；其余所选图片继续保留。")
    }},confirmButton={Column {
        com.example.chatbar.domain.image.NovelAiImageUseTarget.entries.forEach{target->TextButton(enabled=!c.busy,onClick={c.useImage(current,target);close()}){Text(target.displayName)}}
    }},dismissButton={TextButton(onClick=close){Text("取消")}})
}
