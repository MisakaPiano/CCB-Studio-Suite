package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.misakapiano.ccbstudio.core.*

/** One staged tree for creation and archive. Cancel never publishes organization edits. */
@Composable internal fun TypesetProjectPlacementDialog(c:WorkbenchController,e:TypesetEditorController,archive:Boolean,dismiss:()->Unit){
    val p=e.project;if(archive&&p==null)return
    val before=remember{c.state.generationOrganization}
    var org by remember{mutableStateOf(before)}
    var name by remember{mutableStateOf(if(archive)p!!.name else "")}
    var parent by remember{mutableStateOf(e.editorExtras.projectRecords.firstOrNull{it.projectId==p?.id}?.parent)}
    val at=remember{System.currentTimeMillis()}
    var expanded by remember{mutableStateOf(before.folders.filter{f->parent!=null&&parent in before.descendants(f.id)}.map{it.id}.toSet())}
    var editing by remember{mutableStateOf<String?>(null)};var nodeName by remember{mutableStateOf("")}
    var nodeProblem by remember{mutableStateOf<String?>(null)}
    var ids by remember{mutableStateOf(c.assets.filter{it.request().id().toString() in c.selection}.map{it.request().id().toString()})}
    var picking by remember{mutableStateOf(false)}
    if(picking){SharedAssetPicker(c,true,{picking=false}){ids=it;picking=false};return}
    fun applyNode(){try{
        val n=org.folders.firstOrNull{it.id==parent}
        when(editing){
            "create"->{val folder=GenerationFolder(name=nodeName.trim(),kind=if(parent==null)"project"else"folder",parent=parent);val next=org.copy(folders=org.folders+folder);next.validate();org=next;parent?.let{expanded=expanded+it};parent=folder.id}
            "rename"->{require(n!=null);val next=org.copy(folders=org.folders.map{if(it.id==parent)it.copy(name=nodeName.trim())else it});next.validate();org=next}
            "remove"->{require(n!=null);org=TypesetOrganizationChanges.remove(org,n.id);parent=n.parent}
        };editing=null;nodeProblem=null
    }catch(ex:Exception){nodeProblem="名称无效或超过32层，原组织未改变"}}
    @Composable fun treePanel(modifier:Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("所属组织 · 可多层嵌套",style=MaterialTheme.typography.subtitle2)
            Column(Modifier.fillMaxWidth().heightIn(max=235.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(3.dp)){
                EditorCommand("未归类 / 根层",!c.busy,TypesetIcons.FolderOpen,selected=parent==null){parent=null;editing=null}
                @Composable fun nodes(id:String?,depth:Int){org.folders.filter{it.parent==id}.forEach{n->
                    Row(Modifier.fillMaxWidth().padding(start=minOf(depth*14,84).dp),verticalAlignment=Alignment.CenterVertically){
                        val children=org.folders.any{it.parent==n.id}
                        TextButton(onClick={expanded=if(n.id in expanded)expanded-n.id else expanded+n.id},enabled=children&&!c.busy,modifier=Modifier.width(28.dp).semantics{contentDescription="展开或收起组织 · ${org.path(n.id)}"},contentPadding=PaddingValues(0.dp)){Text(if(!children)"·"else if(n.id in expanded)"⌄"else"›")}
                        OutlinedButton(onClick={parent=n.id;editing=null},enabled=!c.busy,modifier=Modifier.weight(1f).height(32.dp).semantics{contentDescription="组织节点 · ${org.path(n.id)}"},contentPadding=PaddingValues(horizontal=8.dp,vertical=3.dp),colors=ButtonDefaults.outlinedButtonColors(backgroundColor=if(parent==n.id)Gold.copy(alpha=.22f)else Panel)){
                            Icon(TypesetIcons.FolderOpen,null,Modifier.size(15.dp));Spacer(Modifier.width(6.dp));Text(n.name,Modifier.weight(1f),maxLines=1)
                        }
                    }
                    if(n.id in expanded)nodes(n.id,depth+1)
                }};nodes(null,0)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                EditorCommand("新建下级",!c.busy,TypesetIcons.AddFolder){editing="create";nodeName="";nodeProblem=null}
                EditorCommand("重命名节点",!c.busy&&parent!=null){editing="rename";nodeName=org.folders.single{it.id==parent}.name;nodeProblem=null}
                EditorCommand("移除节点",!c.busy&&parent!=null,TypesetIcons.RemoveFolder){editing="remove";nodeProblem=null}
            }
            editing?.let{mode->Column(Modifier.fillMaxWidth().border(1.dp,Quiet).padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                if(mode=="remove")Text("仅移除所选节点，下级节点与归属工程上移到父节点；根层内容进入未归类。不删除工程、原图或生成文件。")else EditorInput(if(mode=="create")"新节点名称"else"节点新名称",nodeName){nodeName=it.take(100)}
                nodeProblem?.let{Text(it,color=MaterialTheme.colors.error)}
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){EditorCommand("取消节点修改",!c.busy){editing=null};EditorCommand(if(mode=="remove")"确认移除节点"else"保存节点",!c.busy&&(mode=="remove"||nodeName.isNotBlank())){applyNode()}}
            }}
    }}
    @Composable fun detailsPanel(modifier:Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)){
            EditorInput(if(archive)"归档工程名称"else"工程名称",name){if(!c.busy)name=it.take(100)}
            Text("归属位置：${org.path(parent).ifBlank{"未归类"}}")
            if(!archive){EditorCommand("选择图库图片 · ${ids.size} 张",!c.busy,TypesetIcons.Image){picking=true};Text("引用所选图库原图，不另复制；也可先通过图库导入图片。",style=MaterialTheme.typography.caption)}
            if(archive){Divider();Text(typesetProjectTitle(p!!.copy(name=name.trim().ifBlank{"未命名工程"}),TypesetProjectRecord(p.id,"ARCHIVED",at,parent)));Text("同一份工程与原图，保留编辑历史；归档后清空编辑区。")}
            Text("组织调整随本次保存生效，取消不会改变已有组织。",style=MaterialTheme.typography.caption)
            c.problem?.let{Text(it,color=MaterialTheme.colors.error)}
    }}
    Dialog(onDismissRequest={if(!c.busy)dismiss()},properties=DialogProperties(usePlatformDefaultWidth=false)){
      Surface(Modifier.widthIn(max=860.dp).fillMaxWidth(.94f),shape=MaterialTheme.shapes.medium,color=Panel){Column(Modifier.padding(20.dp).heightIn(max=650.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text(if(archive)"命名并归档"else"新建编辑工程",style=MaterialTheme.typography.h6);Divider()
        BoxWithConstraints(Modifier.fillMaxWidth()){
            if(maxWidth>=620.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(20.dp)){treePanel(Modifier.weight(1f));detailsPanel(Modifier.weight(1f))}
            else Column(verticalArrangement=Arrangement.spacedBy(16.dp)){detailsPanel(Modifier.fillMaxWidth());treePanel(Modifier.fillMaxWidth())}
        }
        Divider();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(enabled=!c.busy,onClick=dismiss){Text(if(archive)"取消归档"else"取消新建")};Spacer(Modifier.width(8.dp));TextButton(enabled=!c.busy&&name.isNotBlank()&&editing==null&&(archive||ids.isNotEmpty()),onClick={
        val change=TypesetOrganizationChange(before,org)
        if(archive)e.archiveCurrent(name,parent,at,dismiss,change)else {e.create(name,ids,organization=change,parent=parent);dismiss()}
        }){Text(if(archive)"保存并归档"else"建立工程")}}
      }}
    }
}

@Composable internal fun TypesetLocalProjects(c:WorkbenchController,e:TypesetEditorController,dismiss:()->Unit){
    val projects=e.activeProjects(c)
    AlertDialog(onDismissRequest=dismiss,title={Text("打开本机工程")},text={Column(Modifier.width(600.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("这里只列出未归档工程，包括已收起的工程；归档工程请从归档历史打开。")
        LazyColumn(Modifier.heightIn(max=380.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            if(projects.isEmpty())item{Text("暂无未归档工程")}
            items(projects,key={it.id}){p->Row(Modifier.fillMaxWidth().border(1.dp,Quiet).padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name);Text(c.state.generationOrganization.path(e.editorExtras.projectRecords.firstOrNull{it.projectId==p.id}?.parent).ifBlank{"未归类"},style=MaterialTheme.typography.caption);Text(if(p.id==e.project?.id)"正在编辑"else"已保存，可继续",style=MaterialTheme.typography.caption)};EditorCommand("打开本机工程 · ${p.name}",!c.busy&&p.id!=e.project?.id,TypesetIcons.FolderOpen){e.open(p.id);dismiss()}}}
        }
    }},confirmButton={TextButton(onClick=dismiss){Text("关闭列表")}})
}

@Composable internal fun TypesetProjectFileHelp(c:WorkbenchController,kind:String,dismiss:()->Unit,proceed:()->Unit={}){
    val exporting=kind=="export";val importing=kind=="import"
    AlertDialog(onDismissRequest=dismiss,title={Text(if(exporting)"导出完整工程"else if(importing)"导入外部工程文件"else"工程保存与分享")},text={Column(Modifier.width(620.dp).heightIn(max=530.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(kind=="info"){
            Text("本机工程由应用自动管理，不需要自行寻找内部工程文件。收起或归档继续使用同一份工程和原图，不生成分享包。")
            Text("编辑结果自动保存；动画图片仅编辑首帧或默认图，原始文件保留。",style=MaterialTheme.typography.caption)
            SelectionContainer{Text("当前资料位置：${c.workspace.root}")}
            EditorCommand("打开资料所在位置"){runCatching{java.awt.Desktop.getDesktop().open(c.workspace.root.toFile())}.onFailure{c.problem="路径未能打开，资料仍保留"}}
        }
        Text(if(importing)"选择 Studio 或兼容 TypeSet 导出的 .typesetter-workspace.json 文件。确认摘要后建立独立工程，不覆盖当前工程。"else"完整工程是一个 .typesetter-workspace.json 文件：包含原图字节、当前文字／打码／排版、图片队列顺序和活动页。只有主动导出才生成它。")
        Divider();Text("怎样继续编辑",style=MaterialTheme.typography.subtitle2)
        Text("1. 导出时自行选择保存位置，将生成的工程文件保存或发给对方。\n2. 在 Studio 编辑页点击“导入工程文件”，选择文件并核对内容摘要，再点击“导入为新工程”。\n3. 可继续修改文字、打码、排版与图片顺序；缺失字体需自行安装、导入或替换。")
        Text("不包含：本机撤销／重做历史、字体文件、AI 模型、账户凭据、已导出成品和本机组织树。这不是完整资料备份。",style=MaterialTheme.typography.caption)
        Text("单个文件上限64 MiB。原图会完整嵌入分享文件，请确认愿意分享这些原图；本机归档不复制原图。",style=MaterialTheme.typography.caption)
        Text("原有“预览与导出”继续用于成品图片／PDF／ZIP；不是可继续编辑的工程。",style=MaterialTheme.typography.caption)
    }},confirmButton={TextButton(enabled=!c.busy,onClick={dismiss();proceed()}){Text(if(exporting)"选择位置并导出完整工程"else if(importing)"选择外部工程文件"else"知道了")}},dismissButton={if(kind!="info")TextButton(onClick=dismiss){Text("取消文件操作")}})
}

@Composable internal fun TypesetProjectDeleteDialog(c:WorkbenchController,e:TypesetEditorController){
    val plan=e.deletionPlan?:return
    var files by remember(plan){mutableStateOf(false)};var selected by remember(plan){mutableStateOf(emptySet<String>())};var understood by remember(plan){mutableStateOf(false)}
    AlertDialog(onDismissRequest={if(!c.busy)e.cancelDeletion()},title={Text(if(e.project?.id==plan.project.id)"废弃当前工程"else"删除归档工程")},text={Column(Modifier.width(650.dp).heightIn(max=550.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text(plan.project.name,style=MaterialTheme.typography.subtitle1)
        Text("删除这个工程的全部编辑进度及撤销／重做入口，不是只移出列表。依赖原图、其他工程与已有备份保留。",color=MaterialTheme.colors.error)
        Text("回收仅属于该工程的文字、打码、排版快照，包括已离开撤销列表的旧进度。原图、共享快照、图库内部媒体资产和已有备份保留；只留下最小删除审计记录。",style=MaterialTheme.typography.caption)
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(files,{files=it;selected=emptySet();understood=false},modifier=Modifier.semantics{contentDescription="选择删除生成文件"});Text("同时选择删除本工程生成／导出的文件")}
        if(files)plan.entries.forEach{entry->Column(Modifier.fillMaxWidth().border(1.dp,Quiet).padding(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(entry.output.id in selected,{checked->selected=if(checked)selected+entry.output.id else selected-entry.output.id;understood=false},enabled=entry.eligible&&!c.busy,modifier=Modifier.semantics{contentDescription="删除文件 · ${java.nio.file.Path.of(entry.output.path).fileName}"});Text(java.nio.file.Path.of(entry.output.path).fileName.toString())}
            SelectionContainer{Text(entry.output.path,style=MaterialTheme.typography.caption)};Text(entry.reason,style=MaterialTheme.typography.caption)
        }}
        if(plan.entries.isEmpty())Text("没有已记录的生成文件。旧版未登记的导出文件不会被猜测删除。",style=MaterialTheme.typography.caption)
        Text("本次另选 ${selected.size} 个生成文件永久删除。文件占用、改变或引用检查不通过会保留并说明原因；不会扫描删除整个目录。已有备份和此前分享的工程文件保留。",style=MaterialTheme.typography.caption)
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(understood,{understood=it},modifier=Modifier.semantics{contentDescription="确认不再需要工程进度"});Text("我已了解范围，确认不再需要此工程进度")}
        c.problem?.let{Text(it,color=MaterialTheme.colors.error)}
    }},confirmButton={TextButton(enabled=understood&&!c.busy,colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colors.error),onClick={e.deleteProject(selected,true)}){Text("确认废弃工程")}},dismissButton={TextButton(enabled=!c.busy,onClick=e::cancelDeletion){Text("取消删除")}})
}
