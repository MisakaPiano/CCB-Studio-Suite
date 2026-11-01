package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import java.nio.file.Path

private data class CollectionDraft(val path:Path,val category:String)

@Composable internal fun CollectionWorkspace(c:WorkbenchController) {
    val library=c.library;val state=library.state?:return
    val folders=state.folders.filterNot{library.repository.isReferenceFolder(it.id,state)}
    val globallyPaused=library.sessionPaused || state.preferences["collectionsPaused"]=="true"
    val eligible=remember(state,library.monitorEpoch){runCatching{library.repository.automaticFolders(state).map{it.id}.toSet()}}
    var draft by remember{mutableStateOf<CollectionDraft?>(null)}
    var stop by remember{mutableStateOf<LibraryFolder?>(null)}
    Text("自动收集文件夹",style=MaterialTheme.typography.h6)
    Text("这里新增的图片会自动加入图库，源文件保留。只导入一次，请到图库选择“导入图片”。")
    StudioAction("添加文件夹",!c.busy){library.chooseCollectionDirectory()?.let{draft=CollectionDraft(it,"images")}}
    if(folders.isEmpty()) {
        Text("还没有自动收集的文件夹。已导入图片和相册不需要在这里设置。",color=Quiet)
        StudioAction("去图库导入"){c.navigate("图库")}
    } else {
        if(globallyPaused)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("全部自动收集已暂停",Modifier.weight(1f))
            StudioAction("继续自动收集",!c.busy && eligible.isSuccess){library.pauseCollections(false)}
        }
        if(library.collectionBlocked || eligible.isFailure) {
            Text("自动收集暂不可用，图库已有图片保留。请检查文件夹后重试。",color=MaterialTheme.colors.error)
            StudioAction("检查并重试",!c.busy){c.run("重新检查自动收集"){library.retryCollections();library.refresh()}}
        }
        if(library.collectionBusy)Text(library.automaticNotice.ifBlank{"正在收集新图片…"})
        folders.forEach{folder->
            val paused=library.repository.isFolderPaused(folder.id,state)
            val connected=folder.id in eligible.getOrDefault(emptySet())
            val count=state.observations.count{it.key.startsWith(folder.id+":")}
            val missing=library.scanResults.any{it.path==folder.path && it.status.startsWith("DIRECTORY_MISSING")}
            Card(Modifier.fillMaxWidth(),backgroundColor=Panel) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(Path.of(folder.path).fileName?.toString()?:folder.path,style=MaterialTheme.typography.subtitle1)
                    Text(folder.path,style=MaterialTheme.typography.caption,color=Quiet,maxLines=2,overflow=TextOverflow.Ellipsis)
                    val status=when {
                        library.collectionBlocked || eligible.isFailure->"暂不可用"
                        missing->"文件夹暂不可用，图库图片保留"
                        globallyPaused->"已全部暂停"
                        paused->"已暂停"
                        !connected->"需要重新连接"
                        else->"已开启自动收集"
                    }
                    Text("$status · 已收集 $count ${if(folder.category=="prompts")"项Prompt"else"张图片"} · ${if(folder.recursive)"包含子文件夹"else"仅此文件夹"}")
                    StudioActions {
                        if(!globallyPaused && eligible.isSuccess && !library.collectionBlocked) {
                            if(paused || !connected)StudioAction(if(paused)"继续收集"else"重新连接",!c.busy){library.setFolderPaused(folder.id,false)}
                            else StudioAction("暂停收集",!c.busy){library.setFolderPaused(folder.id,true)}
                        }
                        StudioAction("立即检查",!c.busy && !library.collectionBusy && eligible.isSuccess && !library.collectionBlocked){library.scan(folder.id)}
                        StudioAction("停止收集",!c.busy){stop=folder}
                    }
                }
            }
        }
    }
    StudioDisclosure("更多选项") {
        StudioAction("添加Prompt文件夹",!c.busy){library.chooseCollectionDirectory()?.let{draft=CollectionDraft(it,"prompts")}}
        if(folders.isNotEmpty() && !globallyPaused)StudioAction("暂停全部收集",!c.busy){library.pauseCollections(true)}
        if(library.scanResults.isNotEmpty())StudioDisclosure("最近收集记录") {library.scanResults.forEach{Text("${it.path} · ${it.status}",style=MaterialTheme.typography.caption)}}
        Text("备份恢复或移动资料后，外部文件夹需要明确重新连接；图库图片不会因此丢失。",style=MaterialTheme.typography.caption)
    }
    draft?.let{request->
        var recursive by remember(request){mutableStateOf(true)}
        AlertDialog(onDismissRequest={draft=null},title={Text(if(request.category=="prompts")"自动收集Prompt"else"自动收集图片")},text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(request.path.toString())
                StudioSwitch("包含子文件夹",recursive){recursive=!recursive}
                Text(if(request.category=="prompts")"新Prompt会加入资产管理，源文件不会被修改或删除。"else"新图片会加入图库，源文件不会被修改或删除。")
                if(globallyPaused)Text("自动收集目前已暂停，添加后可在列表上方继续。")
            }
        },confirmButton={TextButton(enabled=!c.busy,onClick={draft=null;library.registerCollection(request.path,recursive,request.category)}){Text("添加")}},dismissButton={TextButton(onClick={draft=null}){Text("取消")}})
    }
    stop?.let{folder->AlertDialog(onDismissRequest={stop=null},title={Text("停止收集这个文件夹？")},text={Text("以后不再自动加入这里的新文件。图库里已有图片、相册和源文件都保留。")},confirmButton={TextButton(enabled=!c.busy,onClick={stop=null;library.removeCollection(folder.id)}){Text("确认停止")}},dismissButton={TextButton(onClick={stop=null}){Text("取消")}})}
}
