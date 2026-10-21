package io.github.misakapiano.ccbstudio.core

import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import kotlin.math.*

/** TypeSet 1.1 brush sampling, stripes and original-image sampling. */
object TypesetMaskRenderer {
    data class Sample(val x:Double,val y:Double,val tx:Double,val ty:Double)
    fun samples(points:List<TypesetPoint>,spacing:Double):List<Sample> {
        if(points.isEmpty())return emptyList()
        data class Segment(val a:TypesetPoint,val b:TypesetPoint,val length:Double,val start:Double)
        var total=0.0;val segments=points.zipWithNext().mapNotNull{(a,b)->val length=hypot(b.x-a.x,b.y-a.y);if(length<=.001)null else Segment(a,b,length,total).also{total+=length}}
        if(segments.isEmpty())return listOf(Sample(points[0].x,points[0].y,1.0,0.0))
        val out=mutableListOf<Sample>();var distance=0.0;var index=0
        // Validation permits long off-canvas paths. Bound work before allocating samples.
        require(total/max(1.0,spacing)<=200000){"Brush path exceeds sampling budget; split the stroke"}
        while(distance<=total+.001){while(index<segments.lastIndex && distance>segments[index].start+segments[index].length)index++
            val s=segments[index];val t=((distance-s.start)/s.length).coerceIn(0.0,1.0)
            out+=Sample(s.a.x+(s.b.x-s.a.x)*t,s.a.y+(s.b.y-s.a.y)*t,(s.b.x-s.a.x)/s.length,(s.b.y-s.a.y)/s.length);distance+=max(1.0,spacing)
        }
        val last=segments.last();val tail=out.last()
        if(hypot(tail.x-last.b.x,tail.y-last.b.y)>.5)out+=Sample(last.b.x,last.b.y,(last.b.x-last.a.x)/last.length,(last.b.y-last.a.y)/last.length)
        return out
    }
    fun draw(target:Graphics2D,original:BufferedImage,m:TypesetMask) {
        if(m.effect in setOf("solid","hatch")){throw DataProblem("LEGACY_MASK","Intermediate mask must be explicitly converted to a supported effect before export")}
        val shape=TypesetRenderer.maskShape(m)
        val solid=m.effect in setOf("black","white")
        val g=target.create() as Graphics2D
        try {
            g.composite=AlphaComposite.getInstance(AlphaComposite.SRC_OVER,m.opacity.toFloat())
            if(solid){g.color=if(m.effect=="white")Color.WHITE else Color.BLACK
                if(m.shape!="brush"){g.fill(shape);return}
                g.clip(shape)
                val length=max(8.0,m.size*.9)
                samples(m.points,100.0/m.density).forEach{s->val h=m.stripeThickness/2;val w=length/2
                    val p=Path2D.Double();p.moveTo(s.x-s.tx*h+s.ty*w,s.y-s.ty*h-s.tx*w);p.lineTo(s.x+s.tx*h+s.ty*w,s.y+s.ty*h-s.tx*w)
                    p.lineTo(s.x+s.tx*h-s.ty*w,s.y+s.ty*h+s.tx*w);p.lineTo(s.x-s.tx*h-s.ty*w,s.y-s.ty*h+s.tx*w);p.closePath();g.fill(p)}
                return
            }
            val raw=if(m.shape=="brush" && m.points.isNotEmpty())Rectangle2D.Double(m.points.minOf{it.x}-m.size/2,m.points.minOf{it.y}-m.size/2,m.points.maxOf{it.x}-m.points.minOf{it.x}+m.size,m.points.maxOf{it.y}-m.points.minOf{it.y}+m.size)else shape.bounds2D;val pad=if(m.shape=="rect") {if(m.effect=="blur")ceil(3*max(1.0,m.strength/2.0))else 0.0} else if(m.effect=="blur")max(4,m.strength).toDouble()else 2.0
            val x=max(0,floor(raw.x-pad).toInt());val y=max(0,floor(raw.y-pad).toInt())
            val right=min(original.width,ceil(raw.maxX+pad).toInt());val bottom=min(original.height,ceil(raw.maxY+pad).toInt())
            if(right<=x || bottom<=y)return
            val width=right-x;val height=bottom-y
            val processed=BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB)
            try {
                val pg=processed.createGraphics();try{if(m.shape=="rect")pg.clip=Rectangle2D.Double(m.x-x,m.y-y,m.width,m.height);pg.drawImage(original,-x,-y,null)}finally{pg.dispose()}
                if(m.effect=="mosaic") {
                    val sw=max(1,(width.toDouble()/max(2,m.strength)).roundToInt());val sh=max(1,(height.toDouble()/max(2,m.strength)).roundToInt())
                    val tiny=IntArray(sw*sh)
                    // Canvas/Skia nearest sampling resolves exact source-pixel boundaries downward.
                    for(yy in 0 until sh)for(xx in 0 until sw){val sx=((2L*xx+1)*width-1)/(2L*sw);val sy=((2L*yy+1)*height-1)/(2L*sh);tiny[yy*sw+xx]=processed.getRGB(sx.toInt(),sy.toInt())}
                    for(yy in 0 until height)for(xx in 0 until width){val sx=((2L*xx+1)*sw-1)/(2L*width);val sy=((2L*yy+1)*sh-1)/(2L*height);processed.setRGB(xx,yy,tiny[sy.toInt()*sw+sx.toInt()])}
                }else gaussian(processed,max(1.0,m.strength/2.0))
                if(m.shape=="brush") {
                    val clip=Path2D.Double(Path2D.WIND_NON_ZERO);samples(m.points,100.0/m.pointDensity).forEach{s->clip.append(Ellipse2D.Double(s.x-m.size/2,s.y-m.size/2,m.size,m.size),false)};g.clip(clip)
                }else if(m.shape!="rect" || m.effect!="blur")g.clip(shape)
                g.drawImage(processed,x,y,null)
            }finally{processed.flush()}
        }finally{g.dispose()}
    }
    /** Three separable box passes approximate CSS Gaussian sigma; transparent outside crop.
     * Engine-level edge/antialias differences remain subject to source visual comparison. */
    private fun gaussian(image:BufferedImage,sigma:Double) {
        val ideal=sqrt(4*sigma*sigma+1);var low=floor(ideal).toInt();if(low%2==0)low--;low=max(1,low)
        val high=low+2;val count=((12*sigma*sigma-3*low*low-12*low-9)/(-4.0*low-4)).roundToInt().coerceIn(0,3)
        val w=image.width;val h=image.height
        var input=image.getRGB(0,0,w,h,null,0,w);var output=IntArray(input.size)
        // Premultiplied channels prevent transparent edge colors contaminating the blur.
        for(i in input.indices){val c=input[i];val a=c ushr 24;input[i]=(a shl 24) or (((c ushr 16 and 255)*a/255) shl 16) or (((c ushr 8 and 255)*a/255) shl 8) or ((c and 255)*a/255)}
        repeat(3){pass->val radius=((if(pass<count)low else high)-1)/2
            for(horizontal in listOf(true,false)) {
                val outer=if(horizontal)h else w;val inner=if(horizontal)w else h
                for(o in 0 until outer){val sum=LongArray(4);fun index(i:Int)=if(horizontal)o*w+i else i*w+o
                    fun add(i:Int,sign:Int){if(i !in 0 until inner)return;val c=input[index(i)];for(k in 0..3)sum[k]+=((c ushr(k*8))and 255)*sign}
                    for(i in -radius..radius)add(i,1)
                    for(i in 0 until inner){var c=0;for(k in 0..3)c=c or ((sum[k]/(2*radius+1)).toInt() shl(k*8));output[index(i)]=c;add(i-radius,-1);add(i+radius+1,1)}
                };val swap=input;input=output;output=swap
            }
        }
        for(i in input.indices){val c=input[i];val a=c ushr 24;if(a>0)input[i]=(a shl 24) or (min(255,(c ushr 16 and 255)*255/a) shl 16) or (min(255,(c ushr 8 and 255)*255/a) shl 8) or min(255,(c and 255)*255/a)}
        image.setRGB(0,0,w,h,input,0,w)
    }
}
