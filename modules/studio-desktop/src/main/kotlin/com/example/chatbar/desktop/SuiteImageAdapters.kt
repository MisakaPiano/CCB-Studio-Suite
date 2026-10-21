package com.example.chatbar.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.misakapiano.ccbstudio.core.*
import io.github.misakapiano.ccbstudio.desktop.StudioBrand
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.nio.file.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.*

internal object DesktopBrandResources {
    fun logoBytes()=requireNotNull(StudioBrand::class.java.getResourceAsStream("/brand/ccb-blue.png")).use{it.readBytes()}
}
@Composable internal fun BootstrapButton(text:String,enabled:Boolean=true,secondary:Boolean=false,onClick:()->Unit) =
    StudioAction(text,enabled=enabled,style=if(secondary)StudioActionStyle.SECONDARY else StudioActionStyle.PRIMARY,onClick=onClick)
@Composable internal fun StudioToggle(label:String,selected:Boolean,action:()->Unit)=StudioSwitch(label,selected,action)
@Composable internal fun DesktopImageToolControls(maxHeight:Dp,content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().heightIn(max=maxHeight).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
internal data class DesktopFileType(val label:String,val extensions:List<String>)
internal class DesktopFilePicker {
    suspend fun pickSaveFile(type:DesktopFileType,name:String):Path?=withContext(Dispatchers.IO){
        var chosen:Path?=null
        javax.swing.SwingUtilities.invokeAndWait {
            val chooser=JFileChooser();chooser.fileFilter=FileNameExtensionFilter(type.label,*type.extensions.toTypedArray());chooser.selectedFile=java.io.File(name)
            if(chooser.showSaveDialog(null)==JFileChooser.APPROVE_OPTION) chosen=chooser.selectedFile.toPath()
        };chosen
    }
}
internal object DesktopExternalFileWriter {
    /** User-selected destination, no implicit overwrite, publish only complete bytes. */
    fun writeBytes(path:Path,bytes:ByteArray) {
        require(!Files.exists(path,LinkOption.NOFOLLOW_LINKS)){"目标已存在；请选择新文件名"}
        val parent=path.toAbsolutePath().parent;require(Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS))
        val temporary=parent.resolve(".${path.fileName}.${newId()}.pending")
        writeNewForced(temporary,bytes)
        // Do not remove the pending file on failure: it is an inspectable incomplete delivery.
        Files.move(temporary,path)
    }
}
internal fun copyDesktopImageResult(bytes:ByteArray) {
    val animated=runCatching{DesktopImageEditing.requireStatic(bytes)}.isFailure
    val root=Path.of(System.getProperty("java.io.tmpdir"),"CCBStudioSuite","clipboard")
    val extension=if(bytes.take(3).toByteArray().decodeToString()=="GIF")"gif"else if(bytes.firstOrNull()==0xff.toByte())"jpg"else "png"
    Files.createDirectories(root);val file=root.resolve("${newId()}.$extension");writeNewForced(file,bytes)
    val raster=if(animated)null else DesktopImageEditing.decode(bytes)
    val formats=if(raster==null)arrayOf(DataFlavor.javaFileListFlavor)else arrayOf(DataFlavor.javaFileListFlavor,DataFlavor.imageFlavor)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(object:Transferable {
        override fun getTransferDataFlavors()=formats
        override fun isDataFlavorSupported(flavor:DataFlavor)=flavor in formats
        override fun getTransferData(flavor:DataFlavor):Any=when(flavor){DataFlavor.javaFileListFlavor->listOf(file.toFile());DataFlavor.imageFlavor->raster?:throw UnsupportedFlavorException(flavor);else->throw UnsupportedFlavorException(flavor)}
    },null)
}
