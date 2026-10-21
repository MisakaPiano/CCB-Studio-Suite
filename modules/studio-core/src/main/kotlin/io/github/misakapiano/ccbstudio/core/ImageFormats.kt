package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Bounded first-frame raster decoder. Encoded originals are never replaced. */
object ImageFormats {
    fun format(bytes:ByteArray):String = when {
        bytes.size>=8 && bytes.copyOfRange(0,8).contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10))->"png"
        bytes.size>=3 && bytes[0]==(-1).toByte() && bytes[1]==(-40).toByte() && bytes[2]==(-1).toByte()->"jpeg"
        bytes.size>=6 && String(bytes,0,6,Charsets.US_ASCII) in setOf("GIF87a","GIF89a")->"gif"
        bytes.size>=2 && bytes[0]==66.toByte() && bytes[1]==77.toByte()->"bmp"
        bytes.size>=12 && String(bytes,0,4,Charsets.US_ASCII)=="RIFF" && String(bytes,8,4,Charsets.US_ASCII)=="WEBP"->"webp"
        else->throw DataProblem("UNSUPPORTED_IMAGE","Unsupported image; original retained")
    }
    fun extension(bytes:ByteArray)=format(bytes).let{if(it=="jpeg")"jpg"else it}
    fun decode(bytes:ByteArray):BufferedImage {
        require(bytes.size in 1..EditorRenderer.MAX_BYTES)
        val format=format(bytes)
        if(format=="webp")return org.jetbrains.skia.Data.makeFromBytes(bytes).use{data->
            org.jetbrains.skia.Codec.makeFromData(data).use{codec->
                bounds(codec.width,codec.height)
                codec.readPixels().use{bitmap->org.jetbrains.skia.Image.makeFromBitmap(bitmap).use{image->
                    image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.use{encoded->requireNotNull(ImageIO.read(ByteArrayInputStream(encoded.bytes)))}
                }}
            }
        }
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use{input->
            val reader=ImageIO.getImageReaders(input).asSequence().firstOrNull()?:error("Image decoder unavailable")
            try {
                reader.input=input;val w=reader.getWidth(0);val h=reader.getHeight(0);bounds(w,h)
                if(format!="gif")return requireNotNull(reader.read(0))
                fun u16(at:Int)=(bytes[at].toInt() and 255) or ((bytes[at+1].toInt() and 255) shl 8)
                require(bytes.size>=13);val canvasW=u16(6);val canvasH=u16(8);bounds(canvasW,canvasH)
                val tree=reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0")
                val descriptor=(0 until tree.childNodes.length).map{tree.childNodes.item(it)}.single{it.nodeName=="ImageDescriptor"}
                val left=descriptor.attributes.getNamedItem("imageLeftPosition").nodeValue.toInt();val top=descriptor.attributes.getNamedItem("imageTopPosition").nodeValue.toInt()
                require(left>=0 && top>=0 && left.toLong()+w<=canvasW && top.toLong()+h<=canvasH){"GIF frame exceeds logical canvas"}
                val frame=requireNotNull(reader.read(0));val result=BufferedImage(canvasW,canvasH,BufferedImage.TYPE_INT_ARGB)
                try{result.createGraphics().let{g->try{g.drawImage(frame,left,top,null)}finally{g.dispose()}}}finally{frame.flush()}
                return result
            }finally{reader.dispose()}
        }
    }
    private fun bounds(w:Int,h:Int){require(w>0 && h>0 && w.toLong()*h<=EditorRenderer.MAX_PIXELS){"Image pixel budget exceeded"}}
    fun webp(image:BufferedImage):ByteArray {
        bounds(image.width,image.height)
        val png=ByteArrayOutputStream().also{check(ImageIO.write(image,"png",it))}.toByteArray()
        return org.jetbrains.skia.Image.makeFromEncoded(png).use{skia->requireNotNull(skia.encodeToData(org.jetbrains.skia.EncodedImageFormat.WEBP,95)).use{it.bytes}}
    }
}
