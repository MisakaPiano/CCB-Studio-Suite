package io.github.misakapiano.ccbstudio.core

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.*
import javax.imageio.ImageIO
import kotlin.test.*

class WindowsVerticalTypographyTest {
    @Test fun curlyQuotesConvertOnlyForwardForDisplayWithoutRewritingCornerQuotesOrDocument(){
        val source="“双引”‘单引’『原双角』「原单角」﹃既有﹄﹁既有﹂"
        val expected="﹃双引﹄﹁单引﹂『原双角』「原单角」﹃既有﹄﹁既有﹂"
        assertEquals(expected,source.map{WindowsVerticalTypography.verticalQuote(it.toString())}.joinToString(""))
        assertEquals(expected,expected.map{WindowsVerticalTypography.verticalQuote(it.toString())}.joinToString(""))
        val text=TypesetText(text=source,vertical=true,width=550.0,style=TypesetStyle(fontSize=30.0))
        val before=documentJson.encodeToString(TypesetText.serializer(),text)
        val a=BufferedImage(300,600,BufferedImage.TYPE_INT_ARGB);val b=BufferedImage(300,600,BufferedImage.TYPE_INT_ARGB)
        try{
            for((image,t)in listOf(a to text,b to text.copy(text=expected))){val g=image.createGraphics();try{TypesetRenderer.text(g,t)}finally{g.dispose()}}
            assertContentEquals(a.getRGB(0,0,300,600,null,0,300),b.getRGB(0,0,300,600,null,0,300))
            assertEquals(before,documentJson.encodeToString(TypesetText.serializer(),text))
        }finally{a.flush();b.flush()}
    }
    @Test fun richFontRunsDecorationsWrappingAndEmptyTextKeepBoundedNativeGeometry(){
        val source="「中文」[b]粗体[/][u]ABC[/][strike]123[/]，句号。\n第二列"
        val text=TypesetText(text=source,vertical=true,width=150.0,align="left",style=TypesetStyle(fontFamily="Microsoft YaHei",fontSize=26.0))
        val result=WindowsVerticalTypography.layout(text)
        assertTrue(result.width>text.style.fontSize*2,"Short vertical box must wrap into multiple columns")
        assertTrue(result.parts.any{it.style.fontWeight==700});assertTrue(result.parts.count{it.decoration}>=2)
        assertTrue(result.parts.filter{it.decoration}.all{it.outline.bounds2D.height>it.outline.bounds2D.width},"Vertical decorations must follow the column")
        assertEquals(source,text.text);assertEquals(150.0,result.height)
        val empty=WindowsVerticalTypography.layout(text.copy(text=""));assertEquals(0,empty.glyphs);assertTrue(empty.parts.isEmpty())
        assertTrue(WindowsVerticalTypography.layout(text.copy(align="right")).parts.filterNot{it.decoration}.all{it.outline.bounds2D.maxY<=152.0})
    }
    @Test fun chineseAndLatinInkBothAdvanceVerticallyWithEngineOrientationTransform(){
        for(source in listOf("我家没有电。","Example text")){
            val text=TypesetText(text=source,vertical=true,width=600.0,align="left",style=TypesetStyle(fontFamily="Microsoft YaHei",fontSize=38.0))
            val result=WindowsVerticalTypography.layout(text)
            val ink=result.parts.map{it.outline.bounds2D}.reduce{a,b->a.createUnion(b)}
            assertTrue(ink.height>ink.width*3,"Vertical run must be tall, not a horizontal or double-rotated outline: $source $ink")
            assertTrue(ink.minX>=-3&&ink.maxX<=result.width+3&&ink.minY>=-3&&ink.maxY<=result.height+3,"Ink must remain in its measured box: $source $ink / ${result.width}x${result.height}")
        }
    }
    @Test fun nativeMixedRunsKeepOriginalTextAndProduceVerticalFontOutlines(){
        val source="我家没有电。\nExample text\n1994年に至っては"
        val text=TypesetText(text=source,vertical=true,x=220.0,y=20.0,width=600.0,align="left",style=TypesetStyle(fontFamily="Microsoft YaHei",fontSize=38.0,fill="#101010",strokeWidth=0.0))
        val shaped=WindowsVerticalTypography.layout(text)
        assertEquals(source,text.text);assertTrue(shaped.glyphs>20);assertTrue(shaped.parts.isNotEmpty());assertTrue(shaped.width in 40.0..400.0)
        assertTrue(shaped.orientations.size>=2,"Mixed Chinese/Latin must use different native orientations: ${shaped.orientations}")
        val image=BufferedImage(460,660,BufferedImage.TYPE_INT_ARGB);val g=image.createGraphics()
        try{g.color=Color.WHITE;g.fillRect(0,0,image.width,image.height);TypesetRenderer.text(g,text)}finally{g.dispose()}
        val folder=Path.of(System.getProperty("preview.test.root")).resolve("vertical-native");Files.createDirectories(folder)
        ImageIO.write(image,"png",folder.resolve("native-mixed-font.png").toFile());image.flush()
        Files.writeString(folder.resolve("native-mixed-metrics.txt"),"engine=Windows DirectWrite\nglyphs=${shaped.glyphs}\norientations=${shaped.orientations}\nwidth=${shaped.width}\nsource=$source\n")
        assertSame(shaped,WindowsVerticalTypography.layout(text.copy(x=400.0,y=70.0)),"Moving an object reuses immutable shaped geometry")
    }
    @Test fun importedTypefaceShapesWithoutInstallingFontsOrChangingItsBytes(){
        val folder=Path.of(System.getProperty("preview.test.root")).resolve(newId())
        StudioWorkspace.openStudio(folder).use{w->
            val bytes=javaClass.getResourceAsStream("/fonts/MicrosoftPackedFont.hex")!!.use{it.readBytes().decodeToString().filterNot(Char::isWhitespace).chunked(2).map{it.toInt(16).toByte()}.toByteArray()};val font=TypesetFonts.import(w,"fixture.woff2",bytes)
            val text=TypesetText(text="ABC123",vertical=true,width=420.0,style=TypesetStyle(fontFamily=font.alias))
            val result=WindowsVerticalTypography.layout(text);assertTrue(result.glyphs>=6)
            assertContentEquals(bytes,w.bytes(font.sourceId));assertEquals(1,w.state.typesetFonts.size)
        }
    }
}
