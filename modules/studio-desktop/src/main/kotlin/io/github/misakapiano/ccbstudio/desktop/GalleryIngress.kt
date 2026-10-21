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
@Composable internal fun Modifier.galleryIngress(c:WorkbenchController):Modifier {
    val enabled=!c.busy
    val target=remember(c,enabled){object:DragAndDropTarget{
        override fun onDrop(event:DragAndDropEvent):Boolean=if(!enabled)false else c.receiveTransfer(event.awtTransferable,false)
    }}
    return dragAndDropTarget(shouldStartDragAndDrop={enabled},target=target).onPreviewKeyEvent{event->
        if(enabled && event.type==KeyEventType.KeyDown && event.isCtrlPressed && event.key==Key.V)c.pasteImages() else false
    }
}
