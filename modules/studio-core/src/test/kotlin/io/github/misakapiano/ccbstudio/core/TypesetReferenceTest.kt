package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.json.*
import org.junit.Test
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.test.*

class TypesetReferenceTest {
    @Test fun storyDrawsTheSameFallbackFontUsedToMeasureMixedChineseText() {
        val latin="Arial";val fallback="SimSun";val caption="中文ABC，混排旁白。"
        assertTrue(java.awt.Font(latin,0,28).canDisplayUpTo(caption)>=0)
        assertEquals(-1,java.awt.Font(fallback,0,28).canDisplayUpTo(caption))
        val source=EditorRenderer.synthetic(48,32)
        val page=TypesetPage(assetId=newId(),name="fallback",caption=caption,story=StoryStyle(pageWidth=320,pagePadding=16,textPadding=12,fontFamily=latin,fontSize=28.0,firstIndent=false,showPageNumber=false,pageBorder=false,imageBorder=false))
        val actual=TypesetRenderer.story(source,page,1)
        val expected=TypesetRenderer.story(source,page.copy(story=page.story.copy(fontFamily=fallback)),1)
        try {
            val out=java.nio.file.Path.of("build/typeset-glyph-fallback");java.nio.file.Files.createDirectories(out)
            javax.imageio.ImageIO.write(actual,"png",out.resolve("actual.png").toFile());javax.imageio.ImageIO.write(expected,"png",out.resolve("expected.png").toFile())
            assertEquals(expected.width,actual.width);assertEquals(expected.height,actual.height)
            assertEquals(sha256(java.nio.file.Files.readAllBytes(out.resolve("expected.png"))),sha256(java.nio.file.Files.readAllBytes(out.resolve("actual.png"))),"Drawn caption must use the same font as wrapping and alignment, not missing-glyph boxes")
        }finally{actual.flush();expected.flush()}
    }
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
    // Historical TypeSet fixtures remain unchanged. Their manual displayCh/fullwidth
    // substitutions are not the oracle for the user-requested native-font vertical mode.
    @Test fun sourceVerticalInputsKeepRawTextAndUseNativeShapingInsteadOfLegacySubstitution() {
        for(id in listOf("TS-V01","TS-V02","TS-R01-vertical")) {
            val fixture=source(id);val text=input(fixture.getValue("input").jsonObject)
            val original=documentJson.encodeToString(TypesetText.serializer(),text)
            val actual=TypesetLayout.layout(text);val native=WindowsVerticalTypography.layout(text)
            assertTrue(actual.vertical);assertEquals(native.width,actual.width);assertEquals(native.height,actual.height)
            assertTrue(native.glyphs>0);assertTrue(native.parts.isNotEmpty())
            val page=TypesetPage(assetId=newId(),name=id,texts=listOf(text))
            val image=TypesetRenderer.render(Files.readAllBytes(reference.resolve("synthetic-reference.png")),page)
            try{ImageIO.write(image,"png",output.resolve("$id-native.png").toFile())}finally{image.flush()}
            assertEquals(original,documentJson.encodeToString(TypesetText.serializer(),text),"Rendering never rewrites source text or source typography flags")
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
