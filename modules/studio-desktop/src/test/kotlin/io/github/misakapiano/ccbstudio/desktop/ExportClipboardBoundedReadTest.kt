package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.core.*
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.*
import java.util.zip.CRC32
import kotlin.test.*

/** Transferables only; never reads or writes the OS clipboard or starts a window/service. */
class ExportClipboardBoundedReadTest {
    private fun chunk(type:String,payload:ByteArray):ByteArray=ByteArrayOutputStream().also{buffer->DataOutputStream(buffer).use{out->
        val name=type.toByteArray();val crc=CRC32();crc.update(name);crc.update(payload)
        out.writeInt(payload.size);out.write(name);out.write(payload);out.writeInt(crc.value.toInt())
    }}.toByteArray()
    @Test fun staticAnimationCorruptAndMultipleFilesKeepExistingFlavorsAndSourceBytes() {
        val base=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()));val png=EditorRenderer.synthetic(16,16)
        val bytes=png.copyOfRange(0,png.size-12)+chunk("paDd",ByteArray(3*1024*1024){(it*37+11).toByte()})+png.copyOfRange(png.size-12,png.size)
        val static=Files.write(base.resolve("static.png"),bytes,StandardOpenOption.CREATE_NEW)
        val transfer=ExportClipboard.contents(listOf(static));assertEquals(listOf(DataFlavor.javaFileListFlavor,DataFlavor.imageFlavor),transfer.transferDataFlavors.toList())
        assertEquals(listOf(static.toAbsolutePath().toFile()),transfer.getTransferData(DataFlavor.javaFileListFlavor))
        val raster=transfer.getTransferData(DataFlavor.imageFlavor) as BufferedImage
        val expected=EditorRenderer.decode(png)
        try {assertEquals(16,raster.width);assertEquals(16,raster.height);assertEquals(expected.getRGB(0,0),raster.getRGB(0,0))}finally{expected.flush();raster.flush()}
        val animation=png.copyOfRange(0,png.size-12)+chunk("acTL",byteArrayOf(0,0,0,1,0,0,0,0))+png.copyOfRange(png.size-12,png.size)
        val animated=Files.write(base.resolve("animation.png"),animation,StandardOpenOption.CREATE_NEW)
        val corrupt=Files.write(base.resolve("corrupt.png"),byteArrayOf(1,2,3,4),StandardOpenOption.CREATE_NEW)
        for(paths in listOf(listOf(animated),listOf(corrupt),listOf(static,animated))) {
            val files=ExportClipboard.contents(paths);assertEquals(listOf(DataFlavor.javaFileListFlavor),files.transferDataFlavors.toList())
            assertEquals(paths.map{it.toAbsolutePath().toFile()},files.getTransferData(DataFlavor.javaFileListFlavor))
        }
        assertContentEquals(bytes,Files.readAllBytes(static));assertContentEquals(animation,Files.readAllBytes(animated));assertContentEquals(byteArrayOf(1,2,3,4),Files.readAllBytes(corrupt))
    }
}
