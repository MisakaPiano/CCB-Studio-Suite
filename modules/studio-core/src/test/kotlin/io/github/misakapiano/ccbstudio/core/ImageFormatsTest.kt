package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.test.*

class ImageFormatsTest {
    private fun root()=Path.of(System.getProperty("preview.test.root")).resolve(newId())
    private fun image()=BufferedImage(12,8,BufferedImage.TYPE_INT_RGB).apply{for(y in 0 until height)for(x in 0 until width)setRGB(x,y,if(x<6)0xff0000 else 0x0000ff)}
    private fun encoded(format:String):ByteArray {val image=image();return try{if(format=="webp")ImageFormats.webp(image)else ByteArrayOutputStream().also{check(ImageIO.write(image,format,it))}.toByteArray()}finally{image.flush()}}
    private fun offsetGif():ByteArray {
        val bytes=encoded("gif");bytes[6]=20;bytes[7]=0;bytes[8]=16;bytes[9]=0
        var at=13;if(bytes[10].toInt() and 128!=0)at+=3*(1 shl ((bytes[10].toInt() and 7)+1))
        while(bytes[at]==0x21.toByte()){at+=2;while(bytes[at].toInt()!=0){at+=1+(bytes[at].toInt() and 255)};at++}
        check(bytes[at]==0x2c.toByte());bytes[at+1]=3;bytes[at+2]=0;bytes[at+3]=4;bytes[at+4]=0
        return bytes
    }
    @Test fun animatedInputsUseFirstOrDefaultRasterAndRetainAllEncodedFrames() {
        StudioWorkspace.openStudio(root()).use{w->
            for(ext in listOf("gif","webp","png")){val bytes=javaClass.getResourceAsStream("/codecs/animated.$ext")!!.use{it.readBytes()}
                val image=EditorRenderer.decode(bytes);assertEquals(12,image.width);assertEquals(8,image.height);assertEquals(0xffff0000.toInt(),image.getRGB(5,4));image.flush()
                val id=w.importImage("animated.$ext",bytes);val repo=TypesetRepository(w);val p=repo.create("animation",listOf(id));val d=repo.load(p)
                val out=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(d.pages.map{it.id},variants=listOf("original","all")))
                assertContentEquals(bytes,Files.readAllBytes(out.resolve("original/001.$ext")));assertEquals("png",ImageFormats.format(Files.readAllBytes(out.resolve("all/001.png"))));assertContentEquals(bytes,w.bytes(id))
            }
        }
    }
    @Test fun gifLogicalCanvasAndFrameOffsetArePreservedAndOversizeRefused() {
        val bytes=offsetGif();val copy=bytes.copyOf();val image=EditorRenderer.decode(bytes)
        assertEquals(20,image.width);assertEquals(16,image.height);assertEquals(0,image.getRGB(0,0));assertEquals(0xffff0000.toInt(),image.getRGB(3,4));assertEquals(0xff0000ff.toInt(),image.getRGB(14,11));image.flush();assertContentEquals(copy,bytes)
        val oversized=bytes.copyOf();for(i in 6..9)oversized[i]=0xff.toByte();assertFails{EditorRenderer.decode(oversized)}
        val outside=bytes.copyOf();outside[6]=2;assertFails{EditorRenderer.decode(outside)}
    }
    @Test fun webpBmpGifImportExportAndSourceExchangeRetainOriginalBytes() {
        StudioWorkspace.openStudio(root()).use{w->val formats=listOf("webp","bmp","gif");val data=formats.map(::encoded)
            val ids=data.mapIndexed{i,b->w.importImage("source.${formats[i]}",b)};val r=TypesetRepository(w);val p=r.create("formats",ids);val d=r.load(p)
            for((i,b)in data.withIndex()){val decoded=EditorRenderer.decode(b);assertEquals(12,decoded.width);assertEquals(8,decoded.height);decoded.flush();assertContentEquals(b,w.bytes(ids[i]))}
            val out=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(d.pages.map{it.id},format="source",variants=listOf("original","all")))
            TypesetExport.verify(out)
            for((i,b)in data.withIndex())assertContentEquals(b,Files.readAllBytes(out.resolve("original/${(i+1).toString().padStart(3,'0')}.${formats[i]}")))
            assertEquals("webp",ImageFormats.format(Files.readAllBytes(out.resolve("all/001.webp"))));assertTrue(Files.exists(out.resolve("all/002.png")));assertTrue(Files.exists(out.resolve("all/003.png")))
            val exchange=TypesetSourceExchange.export(w,p,d);val imported=TypesetSourceExchange.inspect(exchange)
            data.indices.forEach{assertContentEquals(data[it],imported.originals[it])}
            val webp=TypesetExport.create(w,p,d,Files.createDirectories(root()),TypesetExportRequest(d.pages.map{it.id},format="webp",metadata="strip"))
            TypesetExport.verify(webp);assertEquals(3,Files.list(webp).use{stream->stream.filter{it.toString().endsWith(".webp")}.count()}.toInt())
        }
    }
    @Test fun unrecognizedOrCorruptCodecsDoNotCreateAssets() {
        StudioWorkspace.openStudio(root()).use{w->val before=w.catalog().size
            for(bytes in listOf("unknown".toByteArray(),"RIFF0000WEBPbad".toByteArray(),byteArrayOf(66,77,0,0)))assertFails{w.importImage("bad",bytes)}
            assertEquals(before,w.catalog().size)
        }
    }
}
