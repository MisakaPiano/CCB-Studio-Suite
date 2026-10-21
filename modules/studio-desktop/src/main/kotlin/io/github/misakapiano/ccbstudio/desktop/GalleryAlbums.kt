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

internal data class GalleryScope(val ids:Set<String>?=null,val label:String="全部资产",val query:String="",val date:String="",val source:String="all")

@Composable internal fun GalleryAlbumControls(c:WorkbenchController) {
    val g=c.gallery
    LaunchedEffect(c) {
        if(g.foldPreferences==null)try{g.foldPreferences=withContext(Dispatchers.IO){GalleryFoldPreferences(c.library.repository).load()}}
        catch(e:Exception){g.historyProblem="相册偏好不可读取，原记录保留，折叠功能未启用"}
    }
    fun fold(enabled:Boolean,type:NovelAiHistoryFoldType) {
        val depth=g.depth;val preference=NovelAiHistoryFoldPreference(depth,enabled,type)
        c.run("保存相册折叠偏好") {
            GalleryFoldPreferences(c.library.repository).save(preference)
            g.foldPreferences=g.foldPreferences.orEmpty()+(depth to preference);c.library.refresh();c.selection=emptySet();g.anchor=null
        }
    }
    var dateInput by remember(g.depth,g.scope.ids,g.date){mutableStateOf(g.date)}
    StudioActions {
        if(g.depth>0)StudioAction("返回上层相册",!c.busy){g.back();c.selection=emptySet();g.anchor=null}
        StatusText("${g.scope.label} · 第 ${g.depth+1} 层")
        if(g.foldPreferences!=null&&!c.busy)StudioSwitch("折叠为相册",g.fold.enabled){fold(!g.fold.enabled,g.fold.type)}
        if(g.foldPreferences!=null&&!c.busy)CompactChoice("折叠方式",NovelAiHistoryFoldType.entries,g.fold.type,{it.label}){fold(g.fold.enabled,it)}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(dateInput,{dateInput=it},label={Text("日期（YYYY / YYYY-MM / YYYY-MM-DD）")},modifier=Modifier.weight(1f))
        StudioAction("应用日期"){runCatching{galleryDateFilter(dateInput)}.onSuccess{g.date=dateInput.trim();g.historyProblem=null;c.selection=emptySet();g.anchor=null}.onFailure{g.historyProblem="日期无效，请使用年、年月或年月日"}}
        StudioAction("清除日期"){g.date="";dateInput="";g.historyProblem=null;c.selection=emptySet();g.anchor=null}
    }
    if(g.fold.enabled)Text(g.fold.type.description,style=MaterialTheme.typography.caption)
    g.historyProblem?.let{Text(it,color=MaterialTheme.colors.error)}
}
