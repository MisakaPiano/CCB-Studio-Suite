package io.github.misakapiano.ccbstudio.core

import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import kotlin.math.*

/** Preview and export share this renderer. Original bytes are never replaced. */
object TypesetRenderer {
    fun color(value:String):Color {
        validateColor(value);val hex=value.drop(1).let{if(it.length==3)it.flatMap{c->listOf(c,c)}.joinToString("")else it}
        return Color(hex.toInt(16))
    }
    private fun quality(g:Graphics2D){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC)}
    fun render(original:ByteArray,page:TypesetPage,variant:String="all"):BufferedImage {
        require(variant in setOf("all","text","mask","original"))
        val image=EditorRenderer.decode(original)
        val out=BufferedImage(image.width,image.height,BufferedImage.TYPE_INT_ARGB)
        val g=out.createGraphics();try{quality(g);g.drawImage(image,0,0,null)
            if(variant=="all" || variant=="mask")page.masks.filter{it.visible}.forEach{mask(g,out,it)}
            if(variant=="all" || variant=="text")page.texts.filter{it.visible}.forEach{text(g,it)}
        }catch(e:Exception){out.flush();throw e}finally{g.dispose();image.flush()};return out
    }
    fun text(given:Graphics2D,t:TypesetText) {
        val g=given.create() as Graphics2D;try {
            quality(g);val layout=TypesetLayout.layout(t);val left=t.x-layout.width/2
            val box=Rectangle2D.Double(left,t.y,layout.width,layout.height)
            if(t.backgroundOpacity>0){g.composite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,t.backgroundOpacity.toFloat());g.color=color(t.background);g.fill(box)}
            if(t.borderWidth>0 && t.borderOpacity>0){g.composite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,t.borderOpacity.toFloat());g.color=color(t.border);g.stroke=BasicStroke(t.borderWidth.toFloat());g.draw(Rectangle2D.Double(left+t.borderWidth/2,t.y+t.borderWidth/2,max(0.0,layout.width-t.borderWidth),max(0.0,layout.height-t.borderWidth)))}
            g.composite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,t.opacity.toFloat())
            val pitch=t.style.fontSize*t.lineHeight
            layout.lines.forEachIndexed{i,line->
                var x=if(t.vertical)t.x+layout.width/2-pitch/2-i*pitch else when(t.align){"left"->left;"right"->left+layout.width-line.width;else->t.x-line.width/2}
                var y=if(t.vertical)t.y else t.y+i*pitch
                line.cells.forEach {cell->val token=cell.token;val s=token.style;val f=TypesetLayout.font(s,token.display)
                    val glyph=g.create() as Graphics2D
                    try {
                        glyph.font=f;glyph.translate(x,y)
                        val rotate=t.vertical && !token.forced && (token.rotate || (!TypesetLayout.upright(token.display) && token.display.isNotBlank()))
                        if(rotate)glyph.rotate(Math.PI/2)
                        val bounds=f.getStringBounds(token.display,glyph.fontRenderContext)
                        val originX=if(t.vertical)-bounds.width/2 else 0.0
                        val baseline=TypesetFonts.baseline(s.fontFamily,f)
                        val shape=f.createGlyphVector(glyph.fontRenderContext,token.display).getOutline(originX.toFloat(),baseline.toFloat())
                        if(s.strokeWidth>0){glyph.color=color(s.stroke);glyph.stroke=BasicStroke((s.strokeWidth*2).toFloat(),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);glyph.draw(shape)}
                        glyph.color=color(s.fill);glyph.fill(shape)
                    }finally{glyph.dispose()}
                    g.color=color(s.fill);g.stroke=BasicStroke(max(1.0,s.fontSize*.055).toFloat())
                    if(t.vertical){if(s.underline)g.draw(Line2D.Double(x+s.fontSize*.5,y,x+s.fontSize*.5,y+s.fontSize));if(s.strike)g.draw(Line2D.Double(x,y,x,y+s.fontSize));y+=cell.advance}
                    else{if(s.underline)g.draw(Line2D.Double(x,y+s.fontSize*.98,x+cell.advance,y+s.fontSize*.98));if(s.strike)g.draw(Line2D.Double(x,y+s.fontSize*.52,x+cell.advance,y+s.fontSize*.52));x+=cell.advance+t.letterSpacing}
                }
            }
        }finally{g.dispose()}
    }
    fun maskShape(m:TypesetMask):Shape=when(m.shape) {
        "brush"->{val path=Path2D.Double();m.points.forEachIndexed{i,p->if(i==0)path.moveTo(p.x,p.y)else path.lineTo(p.x,p.y)}
            if(m.points.size==1)Ellipse2D.Double(m.points[0].x-m.size/2,m.points[0].y-m.size/2,m.size,m.size)else BasicStroke(m.size.toFloat(),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(path)}
        "lasso"->Path2D.Double().also{p->m.points.forEachIndexed{i,v->if(i==0)p.moveTo(v.x,v.y)else p.lineTo(v.x,v.y)};p.closePath()}
        else->Rectangle2D.Double(m.x,m.y,m.width,m.height)
    }
    private fun mask(given:Graphics2D,image:BufferedImage,m:TypesetMask) {
        val shape=maskShape(m);val bounds=shape.bounds.intersection(Rectangle(0,0,image.width,image.height));if(bounds.isEmpty)return
        val g=given.create() as Graphics2D;try {g.clip=shape;g.composite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,m.opacity.toFloat())
            when(m.effect) {
                "solid"->{g.color=color(m.color);g.fill(shape)}
                "hatch"->{g.color=color(m.color);g.stroke=BasicStroke(max(1,m.strength/4).toFloat());var x=bounds.x-bounds.height
                    while(x<bounds.x+bounds.width){g.drawLine(x,bounds.y,x+bounds.height,bounds.y+bounds.height);x+=max(3,m.strength)}}
                "mosaic"->{val step=max(2,m.strength);val sample=BufferedImage(bounds.width,bounds.height,BufferedImage.TYPE_INT_ARGB);val copy=sample.createGraphics();copy.drawImage(image,-bounds.x,-bounds.y,null);copy.dispose()
                    try{var y=0;while(y<bounds.height){var x=0;while(x<bounds.width){val w=min(step,bounds.width-x);val h=min(step,bounds.height-y);var rr=0L;var gg=0L;var bb=0L;var aa=0L
                        for(yy in y until y+h)for(xx in x until x+w){val c=sample.getRGB(xx,yy);aa+=(c ushr 24);rr+=(c ushr 16 and 255);gg+=(c ushr 8 and 255);bb+=(c and 255)}
                        val n=w*h;g.color=Color((rr/n).toInt(),(gg/n).toInt(),(bb/n).toInt(),(aa/n).toInt());g.fillRect(bounds.x+x,bounds.y+y,w,h);x+=step};y+=step}
                    }finally{sample.flush()}}
                "blur"->{val radius=min(128,max(1,m.strength));val padded=Rectangle(bounds.x-radius,bounds.y-radius,bounds.width+2*radius,bounds.height+2*radius).intersection(Rectangle(0,0,image.width,image.height))
                    val raster=BufferedImage(padded.width,padded.height,BufferedImage.TYPE_INT_ARGB);val copy=raster.createGraphics();copy.drawImage(image,-padded.x,-padded.y,null);copy.dispose()
                    try{boxBlur(raster,radius);g.drawImage(raster,padded.x,padded.y,null)}finally{raster.flush()}}
            }
        }finally{g.dispose()}
    }
    private fun boxBlur(image:BufferedImage,radius:Int) {
        val w=image.width;val h=image.height;val input=image.getRGB(0,0,w,h,null,0,w);val temp=IntArray(input.size);val output=IntArray(input.size)
        fun pass(src:IntArray,dst:IntArray,horizontal:Boolean) {
            val outer=if(horizontal)h else w;val inner=if(horizontal)w else h
            for(o in 0 until outer){val sum=LongArray(4);fun index(i:Int)=if(horizontal)o*w+i else i*w+o
                fun add(i:Int,sign:Int){val c=src[index(i.coerceIn(0,inner-1))];for(k in 0..3)sum[k]+=((c ushr(k*8))and 255)*sign}
                for(i in -radius..radius)add(i,1)
                for(i in 0 until inner){var c=0;for(k in 0..3)c=c or ((sum[k]/(2*radius+1)).toInt() shl(k*8));dst[index(i)]=c;add(i-radius,-1);add(i+radius+1,1)}
            }
        };pass(input,temp,true);pass(temp,output,false);image.setRGB(0,0,w,h,output,0,w)
    }
    fun story(original:ByteArray,page:TypesetPage,number:Int):BufferedImage {
        val edited=render(original,page);val s=page.story
        try{val m=TypesetLayout.story(edited.width,edited.height,page.caption,s);val out=BufferedImage(m.width,m.height,BufferedImage.TYPE_INT_RGB);val g=out.createGraphics()
            try {quality(g);g.color=color(s.pageBackground);g.fillRect(0,0,m.width,m.height)
                if(s.pageBorder){g.color=color(s.borderColor);g.stroke=BasicStroke(max(1,m.width/800).toFloat());g.drawRect(2,2,m.width-4,m.height-4)}
                g.drawImage(edited,s.pagePadding,s.pagePadding,m.imageWidth,m.imageHeight,null)
                if(s.imageBorder){g.color=color(s.borderColor);g.stroke=BasicStroke(max(1,m.width/1000).toFloat());g.drawRect(s.pagePadding,s.pagePadding,m.imageWidth,m.imageHeight)}
                val top=s.pagePadding+m.imageHeight+s.gap;val style=TypesetStyle(fontFamily=s.fontFamily,fontSize=s.fontSize,fill=s.textColor)
                g.font=TypesetLayout.font(style);g.color=color(s.textColor)
                val ascent=TypesetFonts.baseline(s.fontFamily,g.font)
                m.lines.forEachIndexed{i,line->val width=TypesetLayout.measure(line,style);val x=when(s.textAlign){"center"->m.width/2.0-width/2;"right"->s.pagePadding+m.imageWidth-s.textPadding-width;else->(s.pagePadding+s.textPadding).toDouble()}
                    g.drawString(line,x.toFloat(),(top+s.textPadding+i*s.fontSize*s.lineHeight+ascent).toFloat())}
                if(s.showPageNumber){g.font=TypesetLayout.font(style.copy(fontSize=floor(s.fontSize*.72+.5)));val text=number.toString();val width=g.font.getStringBounds(text,g.fontRenderContext).width;val asc=TypesetFonts.baseline(s.fontFamily,g.font)
                    g.drawString(text,(m.width/2.0-width/2).toFloat(),(top+m.bodyHeight+m.numberGap+asc).toFloat())}
            }catch(e:Exception){out.flush();throw e}finally{g.dispose()};return out
        }finally{edited.flush()}
    }
}
