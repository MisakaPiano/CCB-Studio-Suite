package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage

/** EXIF handling follows the existing Suite DesktopImageEditing adapter. No original is rewritten.
 * Legacy documents explicitly retain their old raw-raster coordinate system. */
object TypesetImage {
    const val RAW = "raw-v1"
    const val DISPLAY = "exif-v1"
    fun decode(bytes:ByteArray,coordinates:String):BufferedImage {
        if(coordinates !in setOf(RAW,DISPLAY))throw DataProblem("UNSUPPORTED_VERSION","Unknown image coordinate system: $coordinates")
        val orientation=if(coordinates==DISPLAY)jpegOrientation(bytes)else 1
        return orient(EditorRenderer.decode(bytes),orientation)
    }
    private fun jpegOrientation(bytes: ByteArray): Int {
        if (bytes.size < 4 || bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte()) return 1
        fun u16(offset: Int) = ((bytes[offset].toInt() and 255) shl 8) or (bytes[offset + 1].toInt() and 255)
        var offset = 2
        while (offset + 4 <= bytes.size && bytes[offset] == 0xff.toByte()) {
            val marker = bytes[offset + 1].toInt() and 255
            if (marker == 0xda || marker == 0xd9) break
            val length = u16(offset + 2)
            if (length < 2 || offset.toLong() + length + 2 > bytes.size) break
            if (marker == 0xe1 && length >= 16 && String(bytes, offset + 4, 6, Charsets.US_ASCII) == "Exif\u0000\u0000") {
                val tiff = java.nio.ByteBuffer.wrap(bytes, offset + 10, length - 8).slice()
                val little = tiff.get(0) == 'I'.code.toByte() && tiff.get(1) == 'I'.code.toByte()
                val big = tiff.get(0) == 'M'.code.toByte() && tiff.get(1) == 'M'.code.toByte()
                require(little || big) { "EXIF 字节序无效" }
                tiff.order(if (little) java.nio.ByteOrder.LITTLE_ENDIAN else java.nio.ByteOrder.BIG_ENDIAN)
                require(tiff.getShort(2).toInt() == 42) { "EXIF 数据无效" }
                val directory = tiff.getInt(4)
                require(directory >= 8 && directory + 2L <= tiff.limit()) { "EXIF 目录无效" }
                val count = tiff.getShort(directory).toInt() and 65535
                require(directory + 2L + count * 12L <= tiff.limit()) { "EXIF 目录不完整" }
                repeat(count) { index ->
                    val entry = directory + 2 + index * 12
                    if (tiff.getShort(entry).toInt() and 65535 == 0x112) {
                        require(tiff.getShort(entry + 2).toInt() == 3 && tiff.getInt(entry + 4) == 1)
                        return (tiff.getShort(entry + 8).toInt() and 65535).also { require(it in 1..8) }
                    }
                }
            }
            offset += length + 2
        }
        return 1
    }

    private fun orient(source: BufferedImage, orientation: Int): BufferedImage {
        if (orientation == 1) return source
        val w = source.width; val h = source.height
        val result = BufferedImage(if (orientation >= 5) h else w, if (orientation >= 5) w else h,
            BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) for (x in 0 until w) {
            val (dx, dy) = when (orientation) {
                2 -> w - 1 - x to y
                3 -> w - 1 - x to h - 1 - y
                4 -> x to h - 1 - y
                5 -> y to x
                6 -> h - 1 - y to x
                7 -> h - 1 - y to w - 1 - x
                8 -> y to w - 1 - x
                else -> x to y
            }
            result.setRGB(dx, dy, source.getRGB(x, y))
        }
        source.flush()
        return result
    }

}
