package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import com.composables.icons.lucide.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.*

internal data class GalleryScope(val ids:Set<String>?=null,val label:String="全部资产",val query:String="",val date:String="",val source:String="all",val identity:String="root",val tag:String?=null)

@Composable internal fun <T> LibraryChoice(label:String,options:List<T>,current:T,text:(T)->String,choose:(T)->Unit) {
    var open by remember{mutableStateOf(false)}
    Box {
        LibraryAction((if(label.isBlank())""else"$label · ")+text(current)+" ▾"){open=true}
        DropdownMenu(open,{open=false}) {options.forEach{item->DropdownMenuItem(onClick={open=false;choose(item)}){Text((if(item==current)"✓ "else"")+text(item))}}}
    }
}

/** Same navigation/filter controls for the live library and the archive, with separate preferences. */
@Composable internal fun GalleryAlbumControls(c:WorkbenchController,g:GalleryViewState=c.gallery,onNavigate:()->Unit={c.selection=emptySet()}) {
    val key=if(g.namespace.isBlank())"galleryTagFoldDepths"else g.namespace+".galleryTagFoldDepths"
    LaunchedEffect(c,c.library.state) {
        val state=c.library.state?:return@LaunchedEffect
        if(g.foldPreferences==null)try {
            val loaded=withContext(Dispatchers.Default){GalleryFoldPreferences(c.library.repository,g.namespace).let{it.load(state) to it.directoryDepths(state)}}
            g.foldPreferences=loaded.first;g.directoryFoldDepths=loaded.second
            val raw=state.preferences[key]
            g.tagFoldDepths=raw?.let{documentJson.decodeFromString<List<Int>>(it).also{v->require(v.size<=32 && v.all{d->d in 0..31})}.toSet()}.orEmpty()
        }catch(e:Exception){g.historyProblem="相册偏好不可读取，原记录保留，折叠功能未启用"}
        if(g.sortPreferences==null)try{g.sortPreferences=GallerySortPreferences(c.library.repository).load(state)}catch(e:Exception){g.historyProblem="排序偏好不可读取，原记录保留，暂不保存排序"}
    }
    fun fold(enabled:Boolean,type:NovelAiHistoryFoldType,directory:Boolean=g.directoryFold,tag:Boolean=g.tagFold) {
        if(c.busy||g.foldPreferences==null)return
        val depth=g.depth;val preference=NovelAiHistoryFoldPreference(depth,enabled,type)
        val tagDepths=if(tag)g.tagFoldDepths+depth else g.tagFoldDepths-depth
        c.run("保存相册折叠偏好") {
            GalleryFoldPreferences(c.library.repository,g.namespace).save(preference,directory)
            c.library.repository.update{it.copy(preferences=it.preferences+(key to documentJson.encodeToString(tagDepths.sorted())))}
            g.tagFoldDepths=tagDepths;g.directoryFoldDepths=if(directory)g.directoryFoldDepths.orEmpty()+depth else g.directoryFoldDepths.orEmpty()-depth
            g.foldPreferences=g.foldPreferences.orEmpty()+(depth to preference);c.library.refresh();onNavigate();g.anchor=null
        }
    }
    fun sort(value:GallerySort) {
        if(c.busy||g.sortPreferences==null)return
        val identity=g.scope.identity
        c.run("保存相册默认排序"){GallerySortPreferences(c.library.repository).save(identity,value);g.sortPreferences=g.sortPreferences.orEmpty()+(identity to value);c.library.refresh();g.anchor=null}
    }
    var dateInput by remember(g.scope.identity,g.date){mutableStateOf(g.date)}
    fun applyDate(){runCatching{galleryDateFilter(dateInput)}.onSuccess{g.date=dateInput.trim();g.historyProblem=null;g.anchor=null}.onFailure{g.historyProblem="日期无效，请使用年、年月或年月日"}}
    BoxWithConstraints(Modifier.fillMaxWidth().background(LibraryPanel)) {
    val rowWidth=maxWidth.coerceAtLeast(1000.dp)
    Row(Modifier.horizontalScroll(rememberScrollState()).width(rowWidth).padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        LibraryAction("",g.canGoBack,icon=Lucide.ArrowLeft,accessibilityLabel="后退"){g.goBack();onNavigate()}
        LibraryAction("",g.canGoForward,icon=Lucide.ArrowRight,accessibilityLabel="前进"){g.goForward();onNavigate()}
        LibraryAction("",g.depth>0,icon=Lucide.ArrowUp,accessibilityLabel="返回上层相册"){g.back();onNavigate()}
        Box {
            var path by remember{mutableStateOf(false)}
            TextButton(onClick={path=true},modifier=Modifier.widthIn(max=140.dp)){Text(g.scope.label.lineSequence().first()+" ▾",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.caption)}
            DropdownMenu(path,{path=false}){g.levels.forEachIndexed{index,level->DropdownMenuItem(onClick={path=false;g.goLevel(index);onNavigate()}){Text((if(index==g.depth)"✓ "else"")+level.label.lineSequence().first())}}}
        }
        LibraryChoice("",(listOf("all","directory","imported","generated","processed","unknown")+if(g.namespace=="archive")listOf("other")else emptyList()),g.source,::librarySourceLabel){g.source=it;g.anchor=null}
        LibraryToggle("相册",g.fold.enabled){fold(!g.fold.enabled,g.fold.type)}
        LibraryChoice("",NovelAiHistoryFoldType.entries.map{it.label}+listOf("目录","标签"),if(g.tagFold)"标签"else if(g.directoryFold)"目录"else g.fold.type.label,{it}){label->fold(g.fold.enabled,NovelAiHistoryFoldType.entries.firstOrNull{it.label==label}?:g.fold.type,label=="目录",label=="标签")}
        LibraryChoice("",GallerySortField.entries,g.sort.field,{it.label}){sort(g.sort.copy(field=it))}
        LibraryAction("",!c.busy&&g.sortPreferences!=null,icon=if(g.sort.descending)Lucide.ArrowDownWideNarrow else Lucide.ArrowUpNarrowWide,accessibilityLabel=if(g.sort.descending)"降序 ↓"else"升序 ↑"){sort(g.sort.copy(descending=!g.sort.descending))}
        Spacer(Modifier.weight(1f).widthIn(min=8.dp))
        StudioCompactField("日期（YYYY / YYYY-MM / YYYY-MM-DD）",dateInput,{dateInput=it},Modifier.width(256.dp).onKeyEvent{if(it.type==KeyEventType.KeyDown&&it.key==Key.Enter){applyDate();true}else false},trailing={
            GalleryCalendar(dateInput){picked->dateInput=picked;g.date=picked;g.historyProblem=null}
            LibraryAction("↵",style=StudioActionStyle.TERTIARY,accessibilityLabel="应用日期"){applyDate()}
            LibraryAction("×",style=StudioActionStyle.TERTIARY,accessibilityLabel="清除日期"){g.date="";dateInput="";g.historyProblem=null}
        })
    }
    }
    g.historyProblem?.let{Text(it,color=MaterialTheme.colors.error)}
}
