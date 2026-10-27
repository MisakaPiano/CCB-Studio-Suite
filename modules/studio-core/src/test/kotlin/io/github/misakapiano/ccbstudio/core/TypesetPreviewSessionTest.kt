package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.system.measureNanoTime
import kotlin.test.*

class TypesetPreviewSessionTest {
    private fun source(width:Int,height:Int):ByteArray {
        val image=BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB)
        try{for(y in 0 until height)for(x in 0 until width)image.setRGB(x,y,
            ((128+(x+y)%128) shl 24) or ((x*255/width) shl 16) or ((y*255/height) shl 8) or ((x+y)%256))
            return ByteArrayOutputStream().use{ImageIO.write(image,"png",it);it.toByteArray()}
        }finally{image.flush()}
    }
    private fun equal(expected:BufferedImage,actual:BufferedImage){
        assertEquals(expected.width,actual.width);assertEquals(expected.height,actual.height)
        for(y in 0 until expected.height)assertContentEquals(expected.getRGB(0,y,expected.width,1,null,0,expected.width),actual.getRGB(0,y,actual.width,1,null,0,actual.width),"pixel row $y")
    }
    private fun compare(session:TypesetPreviewSession,bytes:ByteArray,page:TypesetPage,changing:String?){
        val expected=TypesetRenderer.render(bytes,page)
        try{val actual=session.render(page,changing);try{equal(expected,actual)}finally{actual.flush()}}finally{expected.flush()}
    }
    @Test fun immutableSourceSamplingAndInvalidationMatchFullRenderer(){
        val bytes=source(180,140);val original=bytes.copyOf()
        val a=TypesetText(text="AB，竖排",x=85.0,y=10.0,width=100.0,style=TypesetStyle(fontFamily="Dialog",fontSize=18.0))
        val b=a.copy(id=newId(),text="Text",x=90.0,y=85.0,vertical=true,width=45.0)
        val masks=listOf(TypesetMask(x=8.0,y=18.0,width=100.0,height=70.0,effect="black",opacity=.35),
            TypesetMask(x=20.0,y=30.0,width=100.0,height=80.0,effect="mosaic",strength=12),
            TypesetMask(shape="lasso",effect="blur",points=listOf(TypesetPoint(20.0,30.0),TypesetPoint(160.0,40.0),TypesetPoint(70.0,120.0)),strength=6),
            TypesetMask(shape="brush",effect="white",points=listOf(TypesetPoint(30.0,90.0),TypesetPoint(150.0,85.0)),size=20.0))
        val p=TypesetPage(assetId=newId(),name="synthetic",texts=listOf(a,b),masks=masks,layerOrder=listOf(a.id,masks[0].id,masks[1].id,b.id,masks[2].id,masks[3].id))
        TypesetPreviewSession(bytes,p.coordinates).use{session->
            compare(session,bytes,p,null)
            compare(session,bytes,p,b.id)
            compare(session,bytes,p.copy(texts=listOf(a,b.copy(width=90.0,text="More"))),b.id)
            compare(session,bytes,p.copy(masks=masks.mapIndexed{i,m->if(i==0)m.copy(opacity=.6)else m}),b.id)
            compare(session,bytes,p.copy(texts=listOf(a.copy(visible=false),b)),b.id)
            compare(session,bytes,p.copy(layerOrder=p.layerOrder.reversed()),masks[1].id)
            compare(session,bytes,p.copy(texts=emptyList(),masks=emptyList(),layerOrder=emptyList()),null)
            compare(session,bytes,p,masks.last().id)
            assertFailsWith<IllegalArgumentException>{session.render(p.copy(assetId=newId()),null)}
            compare(session,bytes,p,null)
        }
        assertContentEquals(original,bytes)
    }
    @Test fun partiallyDrawnFailureIsDiscardedAndCloseIsIdempotent(){
        val bytes=source(160,100)
        val first=TypesetMask(effect="black",opacity=.3,x=15.0,y=10.0,width=110.0,height=70.0)
        val bad=first.copy(id=newId(),effect="solid")
        val page=TypesetPage(assetId=newId(),name="failure",masks=listOf(first,bad))
        val session=TypesetPreviewSession(bytes,page.coordinates)
        try{
            assertFailsWith<DataProblem>{session.render(page,null)}
            val fixed=page.copy(masks=listOf(first,bad.copy(effect="mosaic")))
            compare(session,bytes,fixed,null)
            assertFailsWith<DataProblem>{session.render(page,bad.id)}
            compare(session,bytes,fixed,bad.id)
        }finally{session.close();session.close()}
        assertFailsWith<IllegalStateException>{session.render(page,null)}
    }
    @Test fun nineExistingMasksAndGrowingStrokeBoundRepeatedWork(){
        val bytes=source(1216,832);val original=bytes.copyOf()
        val masks=(0..8).map{i->TypesetMask(x=40.0+(i%3)*300,y=45.0+(i/3)*220,width=240.0,height=170.0,
            effect=if(i%3==0)"blur"else"mosaic",strength=12)}
        val stroke=TypesetMask(shape="brush",effect="mosaic",points=listOf(TypesetPoint(60.0,700.0)),size=60.0)
        val base=TypesetPage(assetId=newId(),name="1216x832-nine-masks",masks=masks)
        // Warm the unchanged export primitives once; measurements are engine samples, not UI FPS.
        TypesetRenderer.render(bytes,base).flush()
        val frames=(1..8).map{n->base.copy(masks=masks+stroke.copy(points=(0..n).map{i->TypesetPoint(60.0+i*95,700.0+i%2*15)}))}
        val timings=mutableListOf<String>()
        TypesetPreviewSession(bytes,base.coordinates).use{session->
            for((i,p) in frames.withIndex()){
                lateinit var expected:BufferedImage;lateinit var actual:BufferedImage
                val full=measureNanoTime{expected=TypesetRenderer.render(bytes,p)}
                val cached=measureNanoTime{actual=session.render(p,stroke.id)}
                try{equal(expected,actual)}finally{expected.flush();actual.flush()}
                timings+="${i+1},$full,$cached,${session.layersDrawn},${session.prefixRebuilds}"
            }
            assertEquals(17L,session.layersDrawn,"9 stable layers once + one growing stroke per frame")
            assertEquals(1L,session.prefixRebuilds,"unchanged prefix is not repeatedly rebuilt")
        }
        assertContentEquals(original,bytes)
        val output=Path.of(System.getProperty("preview.test.root")).resolve("editor-preview-session")
        Files.createDirectories(output)
        Files.write(output.resolve("frame-engine-nanoseconds.csv"),listOf("frame,full_ns,cached_ns,layers_drawn,prefix_rebuilds")+timings)
        println("EDITOR_ENGINE_SAMPLES="+timings.joinToString(";"))
    }
}
