package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.serialization.*
import java.nio.file.Files
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private val libraryCutFlavor=java.awt.datatransfer.DataFlavor("application/x-ccb-library-cut;class=java.lang.String")
internal data class LibraryClipboard(val ids:List<String>,val cutTag:String?,val token:String=newId(),val files:List<java.nio.file.Path>?=null) {
    fun matches(value:java.awt.datatransfer.Transferable?):Boolean=runCatching{
        if(cutTag!=null)value?.isDataFlavorSupported(libraryCutFlavor)==true&&value.getTransferData(libraryCutFlavor)==token
        else value?.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)==true&&
            (value.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as? List<*>)?.map{(it as java.io.File).toPath().toAbsolutePath().normalize()}==files?.map{it.toAbsolutePath().normalize()}
    }.getOrDefault(false)
}
internal fun copyLibrarySelection(c:WorkbenchController,ids:List<String>,cutTag:String?=null) {
    if(ids.isEmpty()||c.busy)return
    val fixed=ids.distinct();val clip=LibraryClipboard(fixed,cutTag)
    if(cutTag!=null){
        val transfer=object:java.awt.datatransfer.Transferable {
            override fun getTransferDataFlavors()=arrayOf(libraryCutFlavor)
            override fun isDataFlavorSupported(flavor:java.awt.datatransfer.DataFlavor)=flavor==libraryCutFlavor
            override fun getTransferData(flavor:java.awt.datatransfer.DataFlavor):Any {if(!isDataFlavorSupported(flavor))throw java.awt.datatransfer.UnsupportedFlavorException(flavor);return clip.token}
        }
        runCatching{c.gallery.writeTransferable(transfer)}.onSuccess{c.gallery.clipboard=clip;c.message="已剪切 ${fixed.size} 项的标签归属；到目标标签相册粘贴后生效"}.onFailure{c.problem="剪贴板暂不可用，标签归属尚未改变"};return
    }
    c.run("复制所选原始文件") {
        val dir=Files.createTempDirectory(Files.createDirectories(c.workspace.root.resolve("exports/clipboard")),"selection-")
        val paths=fixed.mapIndexed{i,id->
            currentCoroutineContext().ensureActive()
            val asset=c.workspace.assets.read(java.util.UUID.fromString(id)).require()
            val name=asset.request().name().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"),"_").takeLast(180).ifBlank{"image.png"}
            dir.resolve("${i+1}-$name").also{Files.write(it,c.workspace.bytes(id))}
        }
        c.gallery.copyFiles(paths)
        c.gallery.clipboard=clip.copy(files=paths)
    }
}
internal fun pasteLibrarySelection(c:WorkbenchController) {
    val clipboard=c.gallery.clipboard;val target=c.gallery.scope.tag
    if(clipboard!=null && target!=null && clipboard.matches(runCatching{c.gallery.pasteContents()}.getOrNull())) {
        c.library.changeLabels(clipboard.ids.toSet(),onSaved={if(clipboard.cutTag!=null)c.gallery.clipboard=null}){old->old.copy(tags=((old.tags-(clipboard.cutTag?.let{listOf(it)}?:emptyList()))+target).distinct())}
    }else if(!c.pasteImages())c.problem="剪贴板没有图片或文件列表；标签归属需进入目标标签相册后粘贴"
}

private data class GalleryTool(val id:String,val label:String,val icon:ImageVector)
private val galleryTools get()=listOf(
    GalleryTool("edit","所选图片建立编辑工程",DesktopAppIcons.Edit),
    GalleryTool("prompt","所选图片多 Prompt 参照",DesktopAppIcons.Chat),
    GalleryTool("apng","APNG伪装 / 还原",DesktopAppIcons.Tools),
    GalleryTool("tags","编辑标签",DesktopAppIcons.Star),
    GalleryTool("compare","比较 2 张",DesktopAppIcons.Square),
    GalleryTool("guide","选中图片用于引导",DesktopAppIcons.ImageAdd),
    GalleryTool("metadata","参数与来源",DesktopAppIcons.Data))
@Serializable private data class GalleryToolSettings(val version:Int=1,val order:List<String> = emptyList(),val hidden:Set<String> = emptySet(),val tiles:Boolean=false)

internal fun runGalleryTool(c:WorkbenchController,tool:String,ids:List<String>) {
    if(c.busy||c.maintenance)return
    val fixed=ids.distinct().filter{id->id !in c.library.archived && c.assets.any{it.request().id().toString()==id}}
    if(tool!="apng"&&fixed.isEmpty())return
    c.selection=fixed.toSet();c.current=fixed.firstOrNull()?:c.current
    when(tool) {
        "edit"->c.editor.create("图库编辑",fixed)
        "prompt"->c.run("建立图片参照") {
            val results=fixed.map{id->runCatching{saveImagePrompt(c,id,true)}}
            if(results.any{it.isFailure})c.problem="${results.count{it.isFailure}} 张图片没有可读取的非空 Prompt，未加入；其他已保存参照保留。"
            c.library.tab="多 Prompt 参照";c.view="资产管理"
        }
        "apng"->c.animation.open(fixed)
        "tags"->c.gallery.tagEditor=fixed.toSet()
        "compare"->if(fixed.size==2)c.gallery.compare=fixed
        "metadata"->c.gallery.metadata=fixed.first()
        "guide"->c.gallery.guidanceSelection=fixed
    }
}

@Composable internal fun GalleryTools(c:WorkbenchController,drag:LibraryDragState,rootOffset:()->Offset,selected:List<String>) {
    var custom by remember{mutableStateOf(false)}
    val raw=c.library.state?.preferences?.get("galleryTools")
    val loaded=remember(raw){runCatching{raw?.let{documentJson.decodeFromString<GalleryToolSettings>(it).also{value->require(value.version==1)}}?:GalleryToolSettings()}}
    val setting=loaded.getOrDefault(GalleryToolSettings())
    val tools=galleryTools;val order=(setting.order+tools.map{it.id}).distinct().mapNotNull{id->tools.firstOrNull{it.id==id}}
    fun save(next:GalleryToolSettings){if(loaded.isFailure){c.problem="工具布局记录不可读取，原配置保留";return};c.run("保存图库工具布局"){c.library.repository.update{it.copy(preferences=it.preferences+("galleryTools" to documentJson.encodeToString(next)))};c.library.refresh()}}
    StudioActions {
        order.filter{it.id !in setting.hidden}.forEach{tool->key(tool.id){
            val display=when(tool.id){"edit"->"进入编辑";"prompt"->"Prompt 参照";"apng"->"APNG 伪装 / 还原";"tags"->"编辑标签";"compare"->"比较";"guide"->"图像引导";else->"参数与来源"}
            val hovered=drag.activeTarget==tool.label
            val valid=(!c.busy&&(selected.isNotEmpty()||tool.id=="apng")&&(tool.id!="compare"||selected.size==2))
            Box(Modifier.libraryTarget(drag,tool.label,{ids->runGalleryTool(c,tool.id,ids)},rootOffset)
                .background(if(hovered)DesktopBootstrapColors.accent else androidx.compose.ui.graphics.Color.Transparent)) {
                if(setting.tiles)Column(Modifier.padding(4.dp).widthIn(min=95.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(tool.icon,null,Modifier.size(20.dp));StudioAction(display,valid,selected=hovered,icon=null,accessibilityLabel=tool.label){runGalleryTool(c,tool.id,selected)}
                }else StudioAction(display,valid,selected=hovered,icon=tool.icon,accessibilityLabel=tool.label){runGalleryTool(c,tool.id,selected)}
            }
            DisposableEffect(tool.id){onDispose{drag.targets.remove(tool.label)}}
        }}
        Spacer(Modifier.weight(1f))
        // Fixed entry cannot be hidden, reordered into a destructive action, or used as a delete drop target.
        StudioAction("资产回收站",icon=DesktopAppIcons.Delete){c.library.tab="回收站";c.view="资产管理"}
        Box {
            StudioAction("工具布局",icon=DesktopAppIcons.Settings){custom=true}
            DropdownMenu(custom,{custom=false}) {
                DropdownMenuItem(onClick={save(setting.copy(tiles=!setting.tiles))}){Text(if(setting.tiles)"✓ 磁贴样式"else"磁贴样式")}
                order.forEachIndexed{i,tool->Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(tool.id !in setting.hidden,{value->save(setting.copy(hidden=if(value)setting.hidden-tool.id else setting.hidden+tool.id))})
                    Text(tool.label,Modifier.width(180.dp))
                    TextButton(enabled=i>0&&!c.busy,onClick={val moved=order.map{it.id}.toMutableList();moved.add(i-1,moved.removeAt(i));save(setting.copy(order=moved))}){Text("上移")}
                }}
                DropdownMenuItem(onClick={save(GalleryToolSettings())}){Text("恢复默认布局")}
            }
        }
    }
}
