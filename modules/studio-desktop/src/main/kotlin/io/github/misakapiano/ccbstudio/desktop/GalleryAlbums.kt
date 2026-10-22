package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*

internal data class GalleryScope(val ids:Set<String>?=null,val label:String="全部资产",val query:String="",val date:String="",val source:String="all",val identity:String="root")

@Composable internal fun GalleryAlbumControls(c:WorkbenchController) {
    val g=c.gallery
    LaunchedEffect(c,c.library.state) {
        val state=c.library.state?:return@LaunchedEffect
        if(g.foldPreferences==null)try{val loaded=withContext(Dispatchers.Default){GalleryFoldPreferences(c.library.repository).let{it.load(state) to it.directoryDepths(state)}};g.foldPreferences=loaded.first;g.directoryFoldDepths=loaded.second}
        catch(e:Exception){g.historyProblem="相册偏好不可读取，原记录保留，折叠功能未启用"}
        if(g.sortPreferences==null)try{g.sortPreferences=GallerySortPreferences(c.library.repository).load(state)}catch(e:Exception){g.historyProblem="排序偏好不可读取，原记录保留，暂不保存排序"}
    }
    fun fold(enabled:Boolean,type:NovelAiHistoryFoldType,directory:Boolean=g.directoryFold) {
        if(c.busy||g.foldPreferences==null)return
        val depth=g.depth;val preference=NovelAiHistoryFoldPreference(depth,enabled,type)
        c.run("保存相册折叠偏好") {
            GalleryFoldPreferences(c.library.repository).save(preference,directory)
            g.directoryFoldDepths=if(directory)g.directoryFoldDepths.orEmpty()+depth else g.directoryFoldDepths.orEmpty()-depth
            g.foldPreferences=g.foldPreferences.orEmpty()+(depth to preference);c.library.refresh();c.selection=emptySet();g.anchor=null
        }
    }
    fun sort(value:GallerySort) {
        if(c.busy||g.sortPreferences==null)return
        val identity=g.scope.identity
        c.run("保存相册默认排序") {GallerySortPreferences(c.library.repository).save(identity,value);g.sortPreferences=g.sortPreferences.orEmpty()+(identity to value);c.library.refresh();g.anchor=null}
    }
    var dateInput by remember(g.depth,g.scope.ids,g.date){mutableStateOf(g.date)}
        if(g.depth>0)StudioAction("返回上层相册",!c.busy){g.back();c.selection=emptySet();g.anchor=null}
    StudioDisclosure("筛选与分组") {
        CompactChoice("来源",listOf("all","generated","imported"),g.source,{when(it){"generated"->"生成历史";"imported"->"导入/副本";else->"全部来源"}}){g.source=it;c.selection=emptySet();g.anchor=null}
    StudioActions {
        StatusText("${g.scope.label} · 第 ${g.depth+1} 层")
        StudioSwitch("折叠为相册",g.fold.enabled){fold(!g.fold.enabled,g.fold.type)}
        CompactChoice("折叠方式",NovelAiHistoryFoldType.entries.map{it.label}+"目录",if(g.directoryFold)"目录"else g.fold.type.label,{it}){label->
            fold(g.fold.enabled,NovelAiHistoryFoldType.entries.firstOrNull{it.label==label}?:g.fold.type,label=="目录")
        }
        if(g.foldPreferences==null)StatusText("正在读取相册偏好…")
    }
    StudioActions {
        CompactChoice(if(g.depth==0)"默认排序"else"本相册默认排序",GallerySortField.entries,g.sort.field,{it.label}){sort(g.sort.copy(field=it))}
        StudioAction(if(g.sort.descending)"降序 ↓"else"升序 ↑",!c.busy&&g.sortPreferences!=null){sort(g.sort.copy(descending=!g.sort.descending))}
        if(g.depth>0)StatusText("仅记住当前相册")
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(dateInput,{dateInput=it},label={Text("日期（YYYY / YYYY-MM / YYYY-MM-DD）")},modifier=Modifier.weight(1f))
        StudioAction("应用日期"){runCatching{galleryDateFilter(dateInput)}.onSuccess{g.date=dateInput.trim();g.historyProblem=null;c.selection=emptySet();g.anchor=null}.onFailure{g.historyProblem="日期无效，请使用年、年月或年月日"}}
        StudioAction("清除日期"){g.date="";dateInput="";g.historyProblem=null;c.selection=emptySet();g.anchor=null}
    }
    }
    if(g.fold.enabled)Text(if(g.directoryFold)"按已记录的来源目录分组；未记录目录的图片单独显示，不扫描外部目录。"else g.fold.type.description,style=MaterialTheme.typography.caption)
    g.historyProblem?.let{Text(it,color=MaterialTheme.colors.error)}
}
