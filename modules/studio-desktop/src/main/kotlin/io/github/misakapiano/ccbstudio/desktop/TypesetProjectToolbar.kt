package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.chatbar.desktop.DesktopAppIcons
import kotlin.math.roundToInt

/** One project header. Secondary actions have exactly one menu entry, not another expanded row. */
@Composable internal fun TypesetProjectToolbar(c:WorkbenchController,e:TypesetEditorController,history:Boolean,action:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    val current=e.project!=null&&!history
    val editable=current&&!c.busy
    val archived=e.editorExtras.projectRecords.count{it.state=="ARCHIVED"&&c.state.typesetProjects.any{p->p.id==it.projectId}}
    Row(Modifier.fillMaxWidth().background(Panel).horizontalScroll(rememberScrollState()).padding(8.dp)
        .semantics{contentDescription="工程主工具栏"},horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
        Box{
            OutlinedButton(onClick={expanded=true},modifier=Modifier.width(270.dp).height(32.dp).semantics{contentDescription="工程菜单"},contentPadding=PaddingValues(horizontal=8.dp)){
                Icon(TypesetIcons.FolderOpen,null,Modifier.size(15.dp));Spacer(Modifier.width(6.dp))
                Text(if(history)"历史工程"else e.project?.name?:"工程 · 选择或新建",Modifier.weight(1f),fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("▾",fontSize=12.sp)
            }
            DropdownMenu(expanded,{expanded=false},modifier=Modifier.width(340.dp).heightIn(max=580.dp)){
                Column(Modifier.padding(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    Text(e.project?.let(e::projectTitle)?:"暂无打开的工程",style=MaterialTheme.typography.subtitle2)
                    Text(e.project?.let{p->c.state.generationOrganization.path(e.editorExtras.projectRecords.firstOrNull{it.projectId==p.id}?.parent).ifBlank{"未归类"}+" · ${e.document?.pages?.size?:0} 页"}?:"本机工程与归档历史分开管理",style=MaterialTheme.typography.caption)
                }
                Divider()
                @Composable fun item(label:String,enabled:Boolean=!c.busy,key:String){
                    DropdownMenuItem(onClick={expanded=false;action(key)},enabled=enabled,modifier=Modifier.height(32.dp)){Text(label,fontSize=12.sp)}
                }
                item("打开本机工程",key="local")
                item("历史工程 · $archived",key="history")
                Divider()
                item("收起当前工程",editable,"collapse")
                item("重命名工程",editable,"rename")
                item("另存工程",editable,"copy")
                item("移出工程列表",editable,"remove")
                item("保存工程",editable&&e.dirty,"save")
                Divider()
                item("导入工程文件",key="import")
                item("导入编辑 Recipe",editable&&e.page!=null,"recipe")
                item("另存导入原文件",editable&&e.project?.sourceExchangeId!=null,"source")
                Divider()
                item("工程与文件",key="info")
            }
        }
        EditorCommand("新建编辑工程",!c.busy,DesktopAppIcons.Add){action("new")}
        EditorCommand("从资产库追加",editable,DesktopAppIcons.ImageAdd){action("append")}
        Text(if(e.dirty)"待保存"else if(e.project!=null)"已保存"else"未打开工程",Modifier.padding(horizontal=4.dp),style=MaterialTheme.typography.caption)
        EditorCommand("命名并归档",editable,TypesetIcons.Archive){action("archive")}
        EditorCommand("导出完整工程",editable,TypesetIcons.Export){action("export")}
        EditorCommand("废弃当前工程…",editable,TypesetIcons.Delete){action("discard")}
    }
    Divider()
}

/** Editing history and viewing controls stay with the canvas, including at narrow widths. */
@Composable internal fun TypesetCanvasToolbar(c:WorkbenchController,e:TypesetEditorController,preview:()->Unit,export:()->Unit){
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp).semantics{contentDescription="画布底部工具栏"},
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        EditorCommand("适应画布",icon=DesktopAppIcons.ExpandComposer){e.fitCanvas()}
        EditorCommand("−"){e.zoom=(e.zoom/1.25f).coerceAtLeast(.1f)}
        Text("${(e.zoom*100).roundToInt()}%",style=MaterialTheme.typography.caption)
        EditorCommand("＋"){e.zoom=(e.zoom*1.25f).coerceAtMost(16f)}
        Divider(Modifier.height(22.dp).width(1.dp))
        EditorCommand("撤销编辑",!c.busy&&(e.dirty||e.project?.undo?.isNotEmpty()==true),TypesetIcons.Undo){e.history()}
        EditorCommand("重做编辑",!c.busy&&e.project?.redo?.isNotEmpty()==true,TypesetIcons.Redo){e.history(true)}
        EditorCommand("完整预览",!c.busy&&e.document?.pages?.isNotEmpty()==true,TypesetIcons.Preview,action=preview)
        EditorCommand("预览与导出",!c.busy&&e.document?.pages?.isNotEmpty()==true,TypesetIcons.Export,action=export)
    }
}
