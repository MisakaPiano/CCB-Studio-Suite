package io.github.misakapiano.ccbstudio.core

import com.example.chatbar.domain.image.PrivacyPngEncoder
import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

object EditorRenderer {
    const val MAX_BYTES=32*1024*1024
    const val MAX_PIXELS=12_582_912L
    /** Bounds are checked from the header before allocating decoded pixels. */
    fun decode(bytes:ByteArray):BufferedImage = ImageFormats.decode(bytes)
    data class Result(val image:BufferedImage,val warnings:List<String>)
    fun render(bytes:ByteArray,layers:List<Layer>):Result {
        val original=decode(bytes)
        val image=BufferedImage(original.width,original.height,BufferedImage.TYPE_INT_ARGB)
        val g=image.createGraphics();val warnings=mutableListOf<String>()
        try {
            g.drawImage(original,0,0,null)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
            val fonts=GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
            for(layer in layers) {
                layer.validate();if(!layer.visible)continue
                g.color=Color(layer.color,true)
                if(layer.kind=="TEXT") {
                    val available=layer.font in fonts || layer.font in setOf("Dialog","SansSerif","Serif","Monospaced")
                    if(!available) warnings+="Missing font: ${layer.font}; preview uses Dialog. Export is blocked until a font is selected."
                    g.font=Font(if(available)layer.font else "Dialog",Font.PLAIN,layer.fontSize)
                    if(layer.vertical) {
                        var cx=layer.x;var cy=layer.y+layer.fontSize
                        for(cp in layer.text.codePoints().toArray()) {
                            if(cp==10 || cy>layer.y+layer.height) { cx-=layer.fontSize*1.25f;cy=layer.y+layer.fontSize }
                            if(cp!=10) { g.drawString(String(Character.toChars(cp)),cx,cy);cy+=layer.fontSize*1.2f }
                        }
                    } else {
                        var yy=layer.y+layer.fontSize
                        layer.text.lines().forEach { g.drawString(it,layer.x,yy);yy+=layer.fontSize*1.3f }
                    }
                } else {
                    val shape:Shape=when(layer.kind) {
                        "RECT"->Rectangle2D.Float(layer.x,layer.y,layer.width,layer.height)
                        "BRUSH"->BasicStroke(layer.width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(path(layer.points,false))
                        "POLYGON"->path(layer.points,true)
                        else->error("Unknown layer")
                    }
                    if(layer.mosaic) {
                        val old=g.clip;g.clip=shape
                        val step=layer.blockSize
                        for(y in 0 until image.height step step) for(x in 0 until image.width step step) {
                            val xx=(x+step/2).coerceAtMost(image.width-1);val yy=(y+step/2).coerceAtMost(image.height-1)
                            g.color=Color(image.getRGB(xx,yy),true);g.fillRect(x,y,step,step)
                        }
                        g.clip=old
                    } else g.fill(shape)
                }
            }
        } finally { g.dispose();original.flush() }
        return Result(image,warnings.distinct())
    }
    private fun path(points:List<Point>,closed:Boolean)=Path2D.Float().apply {
        moveTo(points.first().x.toDouble(),points.first().y.toDouble())
        points.drop(1).forEach { lineTo(it.x.toDouble(),it.y.toDouble()) };if(closed)closePath()
    }
    fun encode(result:Result,privacy:Boolean,recipe:String=""):ByteArray {
        require(result.warnings.isEmpty()) { result.warnings.joinToString("\n") }
        val image=result.image
        val out=ByteArrayOutputStream()
        if(privacy) PrivacyPngEncoder.write(out,image.width,image.height) { y,row -> image.getRGB(0,y,image.width,1,row,0,image.width) }
        else {
            val writer=ImageIO.getImageWritersByFormatName("png").next()
            ImageIO.createImageOutputStream(out).use { target ->
                try {
                    writer.output=target
                    val metadata=writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier(image),writer.defaultWriteParam)
                    if(recipe.isNotBlank()) {
                        val tree=IIOMetadataNode("javax_imageio_png_1.0")
                        val text=IIOMetadataNode("iTXt"); val entry=IIOMetadataNode("iTXtEntry")
                        mapOf("keyword" to "CCBStudioRecipe","compressionFlag" to "FALSE","compressionMethod" to "0","languageTag" to "","translatedKeyword" to "","text" to recipe).forEach { (k,v)->entry.setAttribute(k,v) }
                        text.appendChild(entry);tree.appendChild(text);metadata.mergeTree("javax_imageio_png_1.0",tree)
                    }
                    writer.write(null,javax.imageio.IIOImage(image,null,metadata),writer.defaultWriteParam)
                } finally { writer.dispose() }
            }
        }
        return out.toByteArray()
    }
    fun synthetic(width:Int=512,height:Int=512):ByteArray {
        val img=BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);val g=img.createGraphics()
        try {
            g.paint=GradientPaint(0f,0f,Color(44,66,87),width.toFloat(),height.toFloat(),Color(224,169,99));g.fillRect(0,0,width,height)
            g.color=Color(244,237,218);g.fillOval(width/4,height/4,width/2,height/2)
            g.color=Color(44,66,87);g.font=Font("Dialog",Font.BOLD,24);g.drawString("SYNTHETIC PREVIEW",24,height-30)
        } finally {g.dispose()}
        return encode(Result(img,emptyList()),false)
    }
}
