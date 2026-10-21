package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.*

class TypesetMaskTest {
    private val source=Path.of("../../docs/audits/evidence/UX_R2B_MASKS/source-output")
    private val output=Path.of(System.getProperty("preview.test.root")).resolve("typeset-masks-"+newId())
    @Test fun allShapesAndEffectsComparedWithOriginalTypeSetOutputs() {
        Files.createDirectories(output)
        val manifest=Json.parseToJsonElement(Files.readString(source.resolve("manifest.json"))).jsonObject
        val original=Files.readAllBytes(source.resolve("original.png"));val measurements=mutableListOf<String>()
        for(case in manifest["cases"]!!.jsonArray){val c=case.jsonObject;val name=c["name"]!!.jsonPrimitive.content;val m=c["mask"]!!.jsonObject
            val mask=TypesetMask(shape=m["shape"]!!.jsonPrimitive.content,effect=m["type"]!!.jsonPrimitive.content,x=30.0,y=30.0,width=100.0,height=60.0,size=34.0,strength=12,
                points=m["points"]!!.jsonArray.map{TypesetPoint(it.jsonObject["x"]!!.jsonPrimitive.double,it.jsonObject["y"]!!.jsonPrimitive.double)})
            val samples=TypesetMaskRenderer.samples(mask.points,100.0/mask.density);val expectedSamples=c["samples"]!!.jsonArray
            assertEquals(expectedSamples.size,samples.size)
            samples.zip(expectedSamples).forEach{(a,b)->val e=b.jsonObject;assertEquals(e["x"]!!.jsonPrimitive.double,a.x,1e-9);assertEquals(e["y"]!!.jsonPrimitive.double,a.y,1e-9);assertEquals(e["tx"]!!.jsonPrimitive.double,a.tx,1e-9);assertEquals(e["ty"]!!.jsonPrimitive.double,a.ty,1e-9)}
            val actual=TypesetRenderer.render(original,TypesetPage(assetId=newId(),name=name,masks=listOf(mask)))
            val expected=ImageIO.read(source.resolve("$name.png").toFile())
            try {ImageIO.write(actual,"png",output.resolve("$name.png").toFile());var difference=0L;var max=0
                for(y in 0 until expected.height)for(x in 0 until expected.width){val a=actual.getRGB(x,y);val b=expected.getRGB(x,y);for(k in 0..2){val d=abs((a ushr(k*8) and 255)-(b ushr(k*8) and 255));difference+=d;max=maxOf(max,d)}}
                val mean=difference.toDouble()/(expected.width*expected.height*3)
                measurements+="$name mean=$mean max=$max";Files.write(output.resolve("comparison.txt"),measurements)
                // Cross-engine antialiasing and three-box Gaussian may differ at boundaries.
                // Fixed pre-run mean budget (8/255 blur; 4/255 other effects), not pixel equivalence.
                assertTrue(mean<if(mask.effect=="blur")8.0 else 4.0,"$name mean=$mean max=$max")
            }finally{actual.flush();expected.flush()}
        }
        assertEquals(12,measurements.size)
    }
    @Test fun laterMosaicReadsOriginalAndNeverEarlierMaskPixels() {
        val original=Files.readAllBytes(source.resolve("original.png"));val mosaic=TypesetMask(x=20.0,y=20.0,width=100.0,height=70.0,effect="mosaic")
        val base=TypesetPage(assetId=newId(),name="overlap",masks=listOf(mosaic))
        val a=TypesetRenderer.render(original,base);val b=TypesetRenderer.render(original,base.copy(masks=listOf(mosaic.copy(effect="black"),mosaic)))
        try{for(y in 21..88)for(x in 21..118)assertEquals(a.getRGB(x,y),b.getRGB(x,y))}finally{a.flush();b.flush()}
        assertContentEquals(original,Files.readAllBytes(source.resolve("original.png")))
    }
}
