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
@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun TypesetProjectToolbar(c:WorkbenchController,e:TypesetEditorController,history:Boolean,action:(String)->Unit){
    var expanded by remember{mutableStateOf(false)}
    var exportMenu by remember{mutableStateOf(false)}
    val current=e.project!=null&&!history
    val editable=current&&!c.busy
    val title=if(history)"归档历史"else e.project?.let(e::projectTitle)?:"未打开工程"
    BoxWithConstraints(Modifier.fillMaxWidth().background(Panel)){
    val narrow=maxWidth<1000.dp
    Row(Modifier.fillMaxWidth().then(if(narrow)Modifier.horizontalScroll(rememberScrollState())else Modifier).padding(8.dp)
        .semantics{contentDescription="工程主工具栏"},horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){
        Box{
            OutlinedButton(onClick={expanded=true},modifier=Modifier.height(32.dp).semantics{contentDescription="工程菜单"},contentPadding=PaddingValues(horizontal=8.dp)){
                Icon(TypesetIcons.FolderOpen,null,Modifier.size(15.dp));Spacer(Modifier.width(6.dp))
                Text("工程",fontSize=12.sp)
                Text("▾",fontSize=12.sp)
            }
            DropdownMenu(expanded,{expanded=false},modifier=Modifier.width(340.dp).heightIn(max=580.dp)){
                @Composable fun item(label:String,enabled:Boolean=!c.busy,key:String){
                    DropdownMenuItem(onClick={expanded=false;action(key)},enabled=enabled,modifier=Modifier.height(32.dp)){Text(label,fontSize=12.sp)}
                }
                item("打开本机工程",key="local")
                item("导入…",key="import-menu")
                Divider()
                item("当前工程设置…",editable,"settings")
                item("收起当前工程",editable,"collapse")
                item("保存工程",editable&&e.dirty,"save")
            }
        }
        EditorCommand("新建编辑工程",!c.busy,DesktopAppIcons.Add){action("new")}
        TooltipArea(tooltip={Surface(color=Panel,elevation=4.dp){Text(title,Modifier.widthIn(max=600.dp).padding(8.dp))}},modifier=(if(narrow)Modifier.width(260.dp)else Modifier.weight(1f)).semantics{contentDescription="当前工程信息"}){
          Column(Modifier.fillMaxWidth().padding(horizontal=4.dp)){
            Text(title,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(if(e.project==null)"本机工程与归档历史分开管理"else c.state.generationOrganization.path(e.editorExtras.projectRecords.firstOrNull{it.projectId==e.project!!.id}?.parent).ifBlank{"未归类"}+" · ${e.document?.pages?.size?:0} 页 · "+if(e.dirty)"待保存"else"已自动保存",style=MaterialTheme.typography.caption,maxLines=1,overflow=TextOverflow.Ellipsis)
          }
        }
        EditorCommand("命名并归档",editable,TypesetIcons.Archive){action("archive")}
        EditorCommand("废弃当前工程",editable,TypesetIcons.Delete,destructive=true){action("discard")}
        Box{
            EditorCommand("导出",editable,TypesetIcons.Export){exportMenu=true}
            DropdownMenu(exportMenu,{exportMenu=false}){
                DropdownMenuItem({exportMenu=false;action("output")}){Text("图片／PDF 等成品",fontSize=12.sp)}
                DropdownMenuItem({exportMenu=false;action("export")}){Text("完整工程包（继续编辑）",fontSize=12.sp)}
            }
        }
    }
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
    }
}
