package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.*

class TypesetColorTest {
    private fun cases()=documentJson.parseToJsonElement(javaClass.getResource("/typeset/source-colors.json")!!.readText()).jsonObject.getValue("cases").jsonArray
    private fun pixel(color:Color,white:Boolean):List<Int> {
        val image=BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);val g=image.createGraphics()
        try{if(white){g.color=Color.WHITE;g.fillRect(0,0,1,1)};g.color=color;g.fillRect(0,0,1,1)
            val p=image.getRGB(0,0);return listOf(p shr 16 and 255,p shr 8 and 255,p and 255,p ushr 24)
        }finally{g.dispose();image.flush()}
    }
    @Test fun namedAndFunctionalRichColorsMatchIndependentSourceCanvasSamples() {
        assertEquals(162,cases().size)
        for(item in cases()) {
            val c=item.jsonObject;val text=c.getValue("value").jsonPrimitive.content
            val token=TypesetLayout.tokens("[color=\"$text\"]X[/]",TypesetStyle(fill="#112233")).single()
            assertEquals(text,token.style.fill,"Source rich-text value must not silently fall back: $text")
            val color=TypesetRenderer.color(token.style.fill)
            for((key,white) in listOf("rgba" to false,"overWhite" to true)) {
                val expected=c.getValue(key).jsonArray.map{it.jsonPrimitive.int};val actual=pixel(color,white)
                // Canvas and Java2D quantize premultiplied channels differently. A one-unit
                // premultiplied difference grows when divided by a small alpha; compare
                // the actual light contribution and separately verify compositing on white.
                fun channels(p:List<Int>)=if(white)p else p.take(3).map{(it*p[3]/255.0).roundToInt()}+p[3]
                assertTrue(channels(expected).zip(channels(actual)).all{(a,b)->abs(a-b)<=1},"$text $key expected=$expected actual=$actual")
            }
        }
    }
    @Test fun invalidColorsCannotSilentlyRenderAsThePreviousStyle() {
        for(value in listOf("#12","not-a-color","rgb(1,2)","rgb(NaN 0 0)","hsl(0 50 50)","rgba(1,2,3,4,5)")) {
            assertFails(value){TypesetRenderer.color(value)}
            assertEquals(value,TypesetLayout.tokens("[color=$value]X[/]",TypesetStyle()).single().style.fill)
        }
    }
}
