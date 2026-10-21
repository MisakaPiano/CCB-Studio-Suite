package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*

/** The opening snapshots asset identity/order; later gallery arrivals cannot retarget a page. */
@Composable internal fun StudioAssetViewer(c:WorkbenchController,ids:List<String>,initial:Int,onClose:()->Unit) {
    var index by remember{mutableStateOf(initial.coerceIn(ids.indices))}
    var reset by remember{mutableStateOf(0)};var status by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope();val id=ids[index]
    DialogWindow(onCloseRequest=onClose,title="图片 ${index+1} / ${ids.size}",icon=StudioBrand.painter,state=rememberDialogState(width=1000.dp,height=800.dp),onKeyEvent={event->
        if(event.type!=KeyEventType.KeyDown)false else when(event.key){
            Key.DirectionLeft->{index=(index-1).coerceAtLeast(0);true}
            Key.DirectionRight->{index=(index+1).coerceAtMost(ids.lastIndex);true}
            Key.Escape->{onClose();true}
            else->false
        }
    }) {
        Surface(color=Ink,contentColor=DesktopBootstrapColors.foreground){Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            DesktopImageZoomSurface("asset:$id",c::readAsset,Modifier.weight(1f).fillMaxWidth(),reset)
            StatusText("${index+1} / ${ids.size} · 滚轮缩放 · 拖动平移 · 双击原始尺寸 · Esc 返回")
            StudioActions {
                StudioAction("上一张",index>0){index--};StudioAction("下一张",index<ids.lastIndex){index++}
                StudioAction("适应窗口"){reset++}
                StudioAction("复制图片"){scope.launch{runCatching{withContext(Dispatchers.IO){copyDesktopImageResult(c.workspace.bytes(id))}}.onSuccess{status="已复制图片／原始文件"}.onFailure{status="复制失败；可以另存原件"}}}
                StudioAction("另存原始文件"){scope.launch{runCatching{
                    val asset=c.assets.single{it.request().id().toString()==id}
                    c.picker.pickSaveFile(DesktopFileType("原始文件",listOf("png","jpg","webp","gif","bmp")),asset.request().name())?.let{target->withContext(Dispatchers.IO){DesktopExternalFileWriter.writeBytes(target,c.workspace.bytes(id))};status="已另存：$target"}
                }.onFailure{status="另存失败；原件保留"}}}
                StudioAction("返回",onClick=onClose)
            }
            if(status.isNotEmpty())StatusText(status)
        }}
    }
}
