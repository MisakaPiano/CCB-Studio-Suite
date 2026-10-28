package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*

/** The opening snapshots asset identity/order; later gallery arrivals cannot retarget a page. */
internal class StudioViewerSelection(ids:List<String>,initial:Int) {
    val ids=ids.toList()
    var index by mutableStateOf(initial.coerceIn(0,ids.lastIndex.coerceAtLeast(0)));private set
    val id get()=ids.getOrNull(index)
    val menu=ContextMenuState()
    fun previous(){index=(index-1).coerceAtLeast(0)}
    fun next(){index=(index+1).coerceAtMost(ids.lastIndex.coerceAtLeast(0))}
}
internal fun viewerEdit(c:WorkbenchController,id:String,close:()->Unit) {
    if(c.busy||c.maintenance)return
    c.current=id;c.selection=setOf(id)
    c.editor.create("图片编辑 · ${id.take(8)}",listOf(id));close()
}
internal fun viewerAnimation(c:WorkbenchController,id:String,mode:String,close:()->Unit) {
    if(c.busy||c.maintenance)return
    c.current=id;c.selection=setOf(id);c.animation.open(listOf(id));c.animation.selectMode(mode);c.view="图库";close()
}
internal fun viewerMenu(c:WorkbenchController,id:String,copy:()->Unit,save:()->Unit,close:()->Unit):List<ContextMenuItem> = buildList {
    add(ContextMenuItem("复制图片",copy));add(ContextMenuItem("另存原始文件",save))
    if(!c.busy&&!c.maintenance) {
        add(ContextMenuItem("进入编辑"){viewerEdit(c,id,close)})
        add(ContextMenuItem("APNG 伪装"){viewerAnimation(c,id,"disguise",close)})
        add(ContextMenuItem("APNG 还原"){viewerAnimation(c,id,"restore",close)})
    }
}
/** Display adaptation of pinned Desktop 5ef9542 DesktopViewerImageRegion; no image/request changes. */
@Composable internal fun StudioViewerImageRegion(selection:StudioViewerSelection,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
    val interaction=remember{MutableInteractionSource()};val hovered by interaction.collectIsHoveredAsState()
    Box(modifier.hoverable(interaction).semantics{contentDescription="浏览图片区域"}) {
        content()
        if(hovered&&selection.ids.size>1) {
            if(selection.index>0)Box(Modifier.align(Alignment.CenterStart).padding(8.dp).background(Panel.copy(alpha=.9f))) {
                StudioAction("上一张",accessibilityLabel="悬浮上一张",icon=DesktopAppIcons.Previous,onClick=selection::previous)
            }
            if(selection.index<selection.ids.lastIndex)Box(Modifier.align(Alignment.CenterEnd).padding(8.dp).background(Panel.copy(alpha=.9f))) {
                StudioAction("下一张",accessibilityLabel="悬浮下一张",icon=DesktopAppIcons.Next,onClick=selection::next)
            }
        }
    }
}
@Composable internal fun StudioAssetViewer(c:WorkbenchController,ids:List<String>,initial:Int,onClose:()->Unit) {
    val selection=remember{StudioViewerSelection(ids,initial)}
    if(selection.id==null)return
    StudioImageWindow(close=onClose,title="图片 ${selection.index+1} / ${selection.ids.size}",width=1000.dp,height=800.dp,onKeyEvent={event->
        if(event.type!=KeyEventType.KeyDown)false else when(event.key){
            Key.DirectionLeft->{selection.previous();true}
            Key.DirectionRight->{selection.next();true}
            Key.Escape->{onClose();true}
            else->false
        }
    }) {Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){StudioAssetViewerContent(c,selection,onClose)}}
}
@Composable internal fun StudioAssetViewerContent(c:WorkbenchController,selection:StudioViewerSelection,onClose:()->Unit) {
    val id=selection.id?:return
    var reset by remember{mutableStateOf(0)};var status by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    val copy:()->Unit={scope.launch{runCatching{withContext(Dispatchers.IO){copyDesktopImageResult(c.workspace.bytes(id))}}.onSuccess{status="已复制图片／原始文件"}.onFailure{status="复制失败；可以另存原件"}};Unit}
    val save:()->Unit={scope.launch{runCatching{
        val asset=c.assets.single{it.request().id().toString()==id}
        c.picker.pickSaveFile(DesktopFileType("原始文件",listOf("png","jpg","webp","gif","bmp")),asset.request().name())?.let{target->withContext(Dispatchers.IO){DesktopExternalFileWriter.writeBytes(target,c.workspace.bytes(id))};status="已另存：$target"}
    }.onFailure{status="另存失败；原件保留"}};Unit}
    Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth()){ContextMenuArea(items={viewerMenu(c,id,copy,save,onClose)},state=selection.menu) {
                StudioViewerImageRegion(selection,Modifier.fillMaxSize()) {
                    DesktopImageZoomSurface("asset:$id",c::readAsset,Modifier.fillMaxSize(),reset)
                }
            }}
            StatusText("${selection.index+1} / ${selection.ids.size} · 滚轮缩放 · 拖动平移 · 双击原始尺寸 · 右键图片操作 · Esc 返回")
            StudioActions {
                StudioAction("上一张",selection.index>0,onClick=selection::previous);StudioAction("下一张",selection.index<selection.ids.lastIndex,onClick=selection::next)
                StudioAction("适应窗口"){reset++}
                StudioAction("进入编辑",!c.busy&&!c.maintenance){viewerEdit(c,id,onClose)}
                StudioAction("APNG 伪装",!c.busy&&!c.maintenance){viewerAnimation(c,id,"disguise",onClose)}
                StudioAction("APNG 还原",!c.busy&&!c.maintenance){viewerAnimation(c,id,"restore",onClose)}
                StudioAction("复制图片",onClick=copy)
                StudioAction("另存原始文件",onClick=save)
                StudioAction("返回",onClick=onClose)
            }
            if(status.isNotEmpty())StatusText(status)
    }
}
