package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.DesktopImageEditing
import io.github.misakapiano.ccbstudio.core.readBounded
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.nio.file.*

/** File-list flavor preserves original/APNG bytes; raster flavor is offered only for a single static image. */
internal object ExportClipboard {
    fun contents(paths:List<Path>):Transferable {
        require(paths.isNotEmpty() && paths.size<=8002 && paths.all{Files.isRegularFile(it,LinkOption.NOFOLLOW_LINKS)})
        val files=paths.map{it.toAbsolutePath().toFile()}
        val image=paths.singleOrNull()?.takeIf{it.fileName.toString().substringAfterLast('.').lowercase() in setOf("png","jpg","jpeg","webp","gif","bmp")}?.let{file->
            runCatching{val bytes=readBounded(file,32*1024*1024);DesktopImageEditing.requireStatic(bytes);DesktopImageEditing.decode(bytes)}.getOrNull()}
        val flavors=if(image==null)arrayOf(DataFlavor.javaFileListFlavor)else arrayOf(DataFlavor.javaFileListFlavor,DataFlavor.imageFlavor)
        return object:Transferable {
            override fun getTransferDataFlavors()=flavors.copyOf()
            override fun isDataFlavorSupported(flavor:DataFlavor)=flavor in flavors
            override fun getTransferData(flavor:DataFlavor):Any=when(flavor){DataFlavor.javaFileListFlavor->files.toList();DataFlavor.imageFlavor->image?:throw UnsupportedFlavorException(flavor);else->throw UnsupportedFlavorException(flavor)}
        }
    }
    fun copy(paths:List<Path>)=Toolkit.getDefaultToolkit().systemClipboard.setContents(contents(paths),null)
}
