package io.github.misakapiano.ccbstudio.core

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import kotlin.test.*

class PngFieldInspectionTest {
    private fun chunk(type:String,data:ByteArray):ByteArray {val kind=type.toByteArray(Charsets.US_ASCII);val crc=CRC32().apply{update(kind);update(data)}
        return ByteBuffer.allocate(data.size+12).putInt(data.size).put(kind).put(data).putInt(crc.value.toInt()).array()}
    private fun png(vararg chunks:ByteArray)=TypesetPngMetadata.injectChunks(EditorRenderer.synthetic(100,80),TypesetPngMetadata.Source(chunks.toList(),null,0))
    private fun zipped(data:ByteArray)=ByteArrayOutputStream().use{out->DeflaterOutputStream(out).use{it.write(data)};out.toByteArray()}
    @Test fun textCompressedInternationalAndDuplicateUnknownFieldsRemainSeparate() {
        val bytes=png(chunk("tEXt","Comment\u0000{\"prompt\":\"first\",\"future\":{\"x\":9}}".toByteArray()),
            chunk("zTXt","Comment\u0000\u0000".toByteArray()+zipped("{\"prompt\":\"second\"}".toByteArray())),
            chunk("iTXt","Note\u0000\u0000\u0000zh\u0000注释\u0000中文字段".toByteArray()))
        val before=sha256(bytes);val read=PngFieldInspection.inspect(bytes)
        assertEquals("READABLE",read.status)
        assertEquals("9",read.fields.getValue("/textChunks/0/parsedJson/future/x").value)
        assertEquals("\"second\"",read.fields.getValue("/textChunks/1/parsedJson/prompt").value)
        assertEquals("\"中文字段\"",read.fields.getValue("/textChunks/2/text").value)
        assertEquals(before,sha256(bytes))
    }
    @Test fun alphaAndTextConflictsArePresentedWithoutChoosingPrecedence() {
        val value="{\"Comment\":{\"prompt\":\"alpha\",\"opaque\":true}}".toByteArray()
        val alpha="stealth_pnginfo".toByteArray()+ByteBuffer.allocate(4).putInt(value.size*8).array()+value
        val image=EditorRenderer.decode(EditorRenderer.synthetic(100,80))
        val bytes=try{TypesetPngMetadata.injectAlpha(image,TypesetPngMetadata.Source(emptyList(),alpha,alpha.size*8));TypesetPngMetadata.injectChunks(ByteArrayOutputStream().use{out->javax.imageio.ImageIO.write(image,"png",out);out.toByteArray()},TypesetPngMetadata.Source(listOf(chunk("tEXt","Comment\u0000{\"prompt\":\"text\"}".toByteArray())),null,0))}finally{image.flush()}
        val read=PngFieldInspection.inspect(bytes)
        assertEquals("\"alpha\"",read.fields.getValue("/alpha/parsedJson/Comment/prompt").value)
        assertEquals("\"text\"",read.fields.getValue("/textChunks/0/parsedJson/prompt").value)
    }
    @Test fun malformedUnknownCompressionMissingAndDecompressionLimitAreDistinct() {
        assertEquals("MISSING",PngFieldInspection.inspect(EditorRenderer.synthetic(100,80)).status)
        assertEquals("UNSUPPORTED_FORMAT",PngFieldInspection.inspect(byteArrayOf(1,2)).status)
        val invalid=png(chunk("tEXt","Comment\u0000{broken".toByteArray()),chunk("zTXt",byteArrayOf(65,0,5,1)))
        val read=PngFieldInspection.inspect(invalid)
        assertEquals("\"CORRUPT\"",read.fields.getValue("/textChunks/0/jsonStatus").value)
        assertEquals("\"UNSUPPORTED_COMPRESSION\"",read.fields.getValue("/textChunks/1/status").value)
        val huge=png(chunk("zTXt",byteArrayOf(65,0,0)+zipped(ByteArray(8*1024*1024+1){65})))
        assertEquals("LIMIT_EXCEEDED",PngFieldInspection.inspect(huge).status)
        val bad=invalid.copyOf();bad[bad.lastIndex-1]=(bad[bad.lastIndex-1].toInt() xor 1).toByte()
        assertEquals("CORRUPT",PngFieldInspection.inspect(bad).status)
    }
}
