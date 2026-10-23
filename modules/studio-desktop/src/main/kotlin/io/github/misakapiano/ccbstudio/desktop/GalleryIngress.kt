package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.input.key.*
import io.github.misakapiano.ccbstudio.core.EditorRenderer
import java.awt.datatransfer.*
import java.nio.file.Path
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Explicit APNG intake only: one level, bounded, cancellable, and no directory registration/watch. */
internal suspend fun animationIngressFiles(paths:List<Path>):List<Path> {
    val result=linkedSetOf<Path>();var examined=0
    for(path in paths) {
        currentCoroutineContext().ensureActive()
        io.github.misakapiano.ccbstudio.core.safePath(path)
        if(Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS))Files.newDirectoryStream(path).use{entries->
            for(child in entries) {
                currentCoroutineContext().ensureActive();require(++examined<=10000){"目录项目过多，请选择较小目录"}
                if(Files.isRegularFile(child,LinkOption.NOFOLLOW_LINKS)&&child.fileName.toString().substringAfterLast('.',"").lowercase() in setOf("png","apng","jpg","jpeg","webp","gif","bmp")) {
                    io.github.misakapiano.ccbstudio.core.safePath(child);result.add(child.toAbsolutePath().normalize());require(result.size<=1000){"一次最多导入1000张图片"}
                }
            }
        }else{result.add(path.toAbsolutePath().normalize());require(result.size<=1000)}
    }
    return result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER){it.toString()})
}

internal data class GalleryIngress(val files:List<Path> = emptyList(),val raster:ByteArray?=null,val channel:String="file-dialog")
internal data class GalleryImportResult(val name:String,val status:String,val assetId:String?=null)

/** Desktop 5ef9542 DesktopImageIngress behavior: file flavor wins; text paste is never stolen.
 * Suite retains unsupported file entries for per-file diagnostics instead of silently filtering. */
internal fun galleryTransfer(transfer:Transferable,paste:Boolean):GalleryIngress? {
    if(transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
        val values=transfer.getTransferData(DataFlavor.javaFileListFlavor) as? List<*> ?: error("Invalid file list")
        require(values.size<=1000 && values.all{it is java.io.File}){"一次最多导入1000个文件"}
        return values.takeIf{it.isNotEmpty()}?.let{GalleryIngress(files=it.map{f->(f as java.io.File).toPath()},channel=if(paste)"clipboard-files"else"drag-files")}
    }
    if(paste && transfer.isDataFlavorSupported(DataFlavor.stringFlavor))return null
    if(!transfer.isDataFlavorSupported(DataFlavor.imageFlavor))return null
    val image=transfer.getTransferData(DataFlavor.imageFlavor) as? java.awt.Image ?: error("Invalid image")
    val width=image.getWidth(null);val height=image.getHeight(null)
    require(width>0 && height>0 && width.toLong()*height<=EditorRenderer.MAX_PIXELS){"剪贴板像素尺寸超限"}
    val raster=BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB)
    try {
        raster.createGraphics().let{g->try{check(g.drawImage(image,0,0,null)){"剪贴板图像尚未载入"}}finally{g.dispose()}}
        val bytes=ByteArrayOutputStream().also{check(ImageIO.write(raster,"png",it))}.toByteArray()
        require(bytes.size<=EditorRenderer.MAX_BYTES)
        return GalleryIngress(raster=bytes,channel=if(paste)"clipboard-raster"else"drag-raster")
    }finally{raster.flush()}
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable internal fun Modifier.galleryIngress(c:WorkbenchController,animation:Boolean=false,handlePaste:Boolean=true):Modifier {
    val enabled=!c.busy
    val target=remember(c,enabled,animation){object:DragAndDropTarget{
        override fun onDrop(event:DragAndDropEvent):Boolean=if(!enabled)false else if(animation)c.animation.receive(event.awtTransferable,false)else c.receiveTransfer(event.awtTransferable,false)
    }}
    return dragAndDropTarget(shouldStartDragAndDrop={enabled},target=target).onPreviewKeyEvent{event->
        if(handlePaste && enabled && event.type==KeyEventType.KeyDown && event.isCtrlPressed && event.key==Key.V){if(animation)c.animation.paste()else c.pasteImages()}else false
    }
}
