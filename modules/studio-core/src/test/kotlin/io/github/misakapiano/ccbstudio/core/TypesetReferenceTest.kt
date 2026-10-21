package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import org.junit.Test
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.test.*

class TypesetReferenceTest {
    private val repo=Path.of(System.getProperty("preview.test.root")).toAbsolutePath().parents().first{Files.exists(it.resolve("docs/audits/evidence/TYPESET_REFERENCE"))}
    private val reference=repo.resolve("docs/audits/evidence/TYPESET_REFERENCE/source-output")
    private val output=Path.of(System.getProperty("preview.test.root")).resolve("typeset-layout-${newId()}").also{Files.createDirectories(it)}
    private fun source(id:String)=documentJson.parseToJsonElement(Files.readString(reference.resolve("$id.json"))).jsonObject
    @Test fun measuredCanvasTopBaselineUsesTypoMetrics() {
        assertEquals(24.390625,NativeFontMetrics.topBaseline(TypesetLayout.font(TypesetStyle(fontFamily="CCBRef",fontSize=32.0))),.000001)
    }
    private fun input(o:JsonObject):TypesetText {
        fun s(k:String)=o.getValue(k).jsonPrimitive.content
        fun d(k:String)=o.getValue(k).jsonPrimitive.double
        fun b(k:String)=o.getValue(k).jsonPrimitive.boolean
        return TypesetText(text=s("text"),x=d("x"),y=d("y"),width=d("width"),style=TypesetStyle(s("fontFamily"),d("fontSize"),d("fontWeight").toInt(),s("fontStyle")=="italic",b("underline"),b("strike"),s("fill"),s("stroke"),d("strokeWidth")),vertical=s("writingMode")=="vertical",align=s("align"),lineHeight=d("lineHeight"),letterSpacing=d("letterSpacing"))
    }
    @Test fun sourceVerticalGlyphsColumnsAndAdvances() {
        for(id in listOf("TS-V01","TS-V02","TS-R01-vertical")) {
            val fixture=source(id);val text=input(fixture.getValue("input").jsonObject);val actual=TypesetLayout.layout(text)
            val expected=fixture.getValue("metrics").jsonObject.getValue("cols").jsonArray
            assertEquals(expected.size,actual.lines.size,id)
            expected.forEachIndexed{i,line->val cells=line.jsonArray;assertEquals(cells.size,actual.lines[i].cells.size,id)
                cells.forEachIndexed{j,c->val e=c.jsonObject;val a=actual.lines[i].cells[j]
                    assertEquals(e.getValue("displayCh").jsonPrimitive.content,a.token.display,"$id $i/$j")
                    assertEquals(e.getValue("advance").jsonPrimitive.double,a.advance,.000001,"$id $i/$j")
                    assertEquals(e.getValue("rotate").jsonPrimitive.boolean,a.token.rotate)
                    assertEquals(e.getValue("style").jsonObject.getValue("fontSize").jsonPrimitive.double,a.token.style.fontSize)
                }
            }
            val page=TypesetPage(assetId=newId(),name=id,texts=listOf(text));val image=TypesetRenderer.render(Files.readAllBytes(reference.resolve("synthetic-reference.png")),page)
            try{ImageIO.write(image,"png",output.resolve("$id-native.png").toFile())}finally{image.flush()}
        }
    }
    @Test fun sourceHorizontalWrappingAndRichStyles() {
        for(id in listOf("TS-H01-96","TS-H01-192","TS-H01-384","TS-R01")) {
            val fixture=source(id);val text=input(fixture.getValue("input").jsonObject);val actual=TypesetLayout.layout(text)
            val expected=fixture.getValue("metrics").jsonObject.getValue("lines").jsonArray
            assertEquals(expected.map{it.jsonObject.getValue("tokens").jsonArray.joinToString(""){t->t.jsonObject.getValue("ch").jsonPrimitive.content}},actual.lines.map{it.cells.joinToString(""){it.token.ch}},id)
            expected.forEachIndexed{i,line->line.jsonObject.getValue("tokens").jsonArray.forEachIndexed{j,t->
                val s=t.jsonObject.getValue("style").jsonObject;val a=actual.lines[i].cells[j].token.style
                assertEquals(s.getValue("fontFamily").jsonPrimitive.content,a.fontFamily);assertEquals(s.getValue("fill").jsonPrimitive.content,a.fill)
                assertEquals(s.getValue("underline").jsonPrimitive.boolean,a.underline);assertEquals(s.getValue("strike").jsonPrimitive.boolean,a.strike)
            }}
        }
    }
    @Test fun sourceStoryGeometryAndBlankParagraphs() {
        for(i in 1..3)for(suffix in listOf("","-no-number")) {
            val fixture=source("TS-S01-$i$suffix");val m=fixture.getValue("metrics").jsonObject;val cfg=m.getValue("cfg").jsonObject
            val s=StoryStyle(fontFamily=cfg.getValue("fontFamily").jsonPrimitive.content,showPageNumber=cfg.getValue("showPageNumber").jsonPrimitive.boolean)
            val caption=cfg.getValue("caption").jsonPrimitive.content;val actual=TypesetLayout.story(600,400,caption,s)
            assertEquals(m.getValue("w").jsonPrimitive.int,actual.width);assertEquals(m.getValue("h").jsonPrimitive.int,actual.height)
            assertEquals(m.getValue("lines").jsonArray.map{it.jsonPrimitive.content},actual.lines)
            val image=TypesetRenderer.story(Files.readAllBytes(reference.resolve("synthetic-reference.png")),TypesetPage(assetId=newId(),name="page",caption=caption,story=s),i)
            try{ImageIO.write(image,"png",output.resolve("TS-S01-$i$suffix-native.png").toFile())}finally{image.flush()}
        }
    }
    private fun Path.parents():Sequence<Path> = generateSequence(this){it.parent}
}
