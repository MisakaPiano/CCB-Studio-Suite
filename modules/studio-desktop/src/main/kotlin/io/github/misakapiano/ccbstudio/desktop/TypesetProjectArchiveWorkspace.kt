package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import com.example.chatbar.desktop.StudioDisclosure

internal fun TypesetEditorController.projectTitle(p:TypesetProject)=typesetProjectTitle(p,editorExtras.projectRecords.firstOrNull{it.projectId==p.id})
internal fun TypesetEditorController.activeProjects(c:WorkbenchController):List<TypesetProject>{val hidden=editorExtras.projectRecords.filter{it.state!="ACTIVE"}.map{it.projectId}.toSet();return c.state.typesetProjects.filterNot{it.id in hidden}}
private fun organizationPath(c:WorkbenchController,id:String?)=if(id==null)"未归类"else c.state.generationOrganization.path(id).ifBlank{"原组织记录保留"}

@Composable internal fun TypesetProjectCleanupNotices(c:WorkbenchController,e:TypesetEditorController){
    e.editorExtras.retirements.filter{it.state!="COMPLETE"}.forEach{r->
        Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){Text("${r.name}：${r.detail.ifBlank{"独占进度回收待完成"}}",Modifier.weight(1f),style=MaterialTheme.typography.caption);EditorCommand("重试进度回收",!c.busy){e.retryRetirement(r.projectId)}}
    }
    val pendingFiles=e.editorExtras.outputs.filter{it.status in setOf("DELETE_PENDING","RETAINED")}
    if(pendingFiles.isNotEmpty())StudioDisclosure("文件删除记录 · ${pendingFiles.size} 项保留或待处理"){
        pendingFiles.forEach{f->Text("${f.path}\n${f.detail}",style=MaterialTheme.typography.caption)}
        EditorCommand("重试已确认的文件删除",!c.busy){e.retryOutputDeletion(pendingFiles.map{it.id}.toSet())}
    }
}

@Composable internal fun TypesetProjectArchiveDialog(c:WorkbenchController,e:TypesetEditorController,dismiss:()->Unit){
    TypesetProjectPlacementDialog(c,e,true,dismiss)
}

private data class TypesetArchivedPreview(val document:TypesetDocument,val bitmap:ImageBitmap?,val problem:String?)
@Composable internal fun TypesetProjectHistory(c:WorkbenchController,e:TypesetEditorController,modifier:Modifier,back:()->Unit){
    val records=e.editorExtras.projectRecords.filter{it.state=="ARCHIVED"}.associateBy{it.projectId}
    val projects=c.state.typesetProjects.filter{it.id in records}.sortedByDescending{records[it.id]!!.archivedAt}
    var selectedId by remember{mutableStateOf(projects.firstOrNull()?.id)}
    val selected=projects.firstOrNull{it.id==selectedId}?:projects.firstOrNull()
    var error by remember(selected?.id){mutableStateOf<String?>(null)}
    val preview by produceState<TypesetArchivedPreview?>(null,selected?.id,selected?.current){
        value=null;error=null
        if(selected!=null)try{value=withContext(Dispatchers.IO){
            val d=TypesetRepository(c.workspace).load(selected);val page=d.pages.firstOrNull()
            var problem:String?=null
            val image=if(page==null)null else try{val image=typesetPreviewRaster(c.workspace,page,d.mode=="storybook",d.startNumber);try{image.toComposeImageBitmap()}finally{image.flush()}}catch(ex:Exception){problem="封面预览暂不可用，工程保留";null}
            TypesetArchivedPreview(d,image,problem)
        }}catch(cancelled:CancellationException){throw cancelled}catch(ex:Exception){error="工程暂无法读取，原有文件保留；请检查恢复记录"}
    }
    Column(modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){EditorCommand("返回编辑",!c.busy,TypesetIcons.Previous,action=back);Text("历史工程",style=MaterialTheme.typography.subtitle1);Text("${projects.size} 份 · 重新打开后回到进行中",style=MaterialTheme.typography.caption)}
        Divider()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()){
            val compact=maxWidth<720.dp
            @Composable fun projectList(mod:Modifier){
                LazyColumn(mod.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    if(projects.isEmpty())item{Text("暂无归档工程",color=Quiet)}
                    items(projects,key={it.id}){p->
                        var assetId by remember(p.current){mutableStateOf<String?>(null)}
                        LaunchedEffect(p.current){assetId=withContext(Dispatchers.IO){runCatching{TypesetRepository(c.workspace).load(p).pages.firstOrNull()?.assetId}.getOrNull()}}
                        OutlinedButton(onClick={selectedId=p.id},enabled=!c.busy,modifier=Modifier.fillMaxWidth().heightIn(min=86.dp),colors=ButtonDefaults.outlinedButtonColors(backgroundColor=if(p.id==selected?.id)Gold.copy(alpha=.22f)else Panel)){
                            Box(Modifier.size(56.dp),contentAlignment=Alignment.Center){assetId?.let{QueueStaticThumbnail(c,it,Modifier.fillMaxSize())}?:Icon(TypesetIcons.Archive,null)}
                            Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                                Text("归档工程 · ${p.name}",maxLines=2,overflow=TextOverflow.Ellipsis)
                                Text(e.projectTitle(p),style=MaterialTheme.typography.caption)
                                Text(organizationPath(c,records[p.id]?.parent),style=MaterialTheme.typography.caption)
                            }
                        }
                    }
                }
            }
            @Composable fun details(mod:Modifier){Column(mod.padding(14.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
                if(selected==null)Text("归档后可在这里查看并继续编辑。",color=Quiet)else{
                    preview?.bitmap?.let{Image(it,"归档工程封面",Modifier.fillMaxWidth().heightIn(max=270.dp),contentScale=ContentScale.Fit)}
                    Text(e.projectTitle(selected),style=MaterialTheme.typography.subtitle1)
                    Text(organizationPath(c,records[selected.id]?.parent),style=MaterialTheme.typography.caption)
                    preview?.let{v->Text("${v.document.pages.size} 页 · 文字 ${v.document.pages.sumOf{it.texts.size}} 处 · 打码 ${v.document.pages.sumOf{it.masks.size}} 处");Text("上次编辑：第 ${(v.document.pages.indexOfFirst{it.id==selected.currentPageId}+1).coerceAtLeast(1)} 页 · 撤销 ${selected.undo.size} / 重做 ${selected.redo.size} 步",style=MaterialTheme.typography.caption);v.problem?.let{Text(it,color=Quiet)}}
                    error?.let{Text(it,color=MaterialTheme.colors.error)}
                    Text("沿用同一份工程，原图仍在原位置；不自动清理编辑历史。",style=MaterialTheme.typography.caption)
                    EditorCommand("重新打开工程",!c.busy&&preview!=null,TypesetIcons.FolderOpen){e.reopenArchived(selected.id,back)}
                    EditorCommand("删除归档工程…",!c.busy,TypesetIcons.Delete){e.prepareDeletion(selected.id)}
                }
            }}
            if(compact)Column(Modifier.fillMaxSize()){projectList(Modifier.fillMaxWidth().heightIn(max=210.dp));Divider();details(Modifier.weight(1f).fillMaxWidth())}
            else Row(Modifier.fillMaxSize()){projectList(Modifier.weight(.9f).fillMaxHeight());Divider(Modifier.fillMaxHeight().width(1.dp));details(Modifier.weight(1.1f).fillMaxHeight())}
        }
    }
}
