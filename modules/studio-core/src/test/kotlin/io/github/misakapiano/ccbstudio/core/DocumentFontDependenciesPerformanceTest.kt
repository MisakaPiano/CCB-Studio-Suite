package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

/** Font family strings only; does not load, activate, resolve, measure or render any font. */
class DocumentFontDependenciesPerformanceTest {
    @Test fun measuredFontDependenciesKeepDuplicateOrderAndAdmittedDocument() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        val tiny=TypesetDocument(pages=listOf(TypesetPage(assetId=UUID(27L,1L).toString(),name="One",story=StoryStyle(fontFamily="StoryOne"),texts=listOf(TypesetText(text="[font=Inline]A[/]B",style=TypesetStyle(fontFamily="BaseOne")))),TypesetPage(assetId=UUID(27L,2L).toString(),name="Two",story=StoryStyle(fontFamily="StoryTwo"))),presets=listOf(TypesetPreset(name="Preset",text=TypesetText(style=TypesetStyle(fontFamily="PresetBase")))))
        assertEquals(listOf("StoryOne","BaseOne","Inline","BaseOne","StoryTwo","PresetBase"),TypesetFonts.documentFonts(tiny))
        val inlineAlias="ccb-font:"+UUID(28L,1L);val baseAlias="ccb-font:"+UUID(28L,2L)
        for(pages in listOf(1,4,16))for(context in listOf("plain","inline")) {
            val unit=if(context=="inline")"[font='$inlineAlias']中A[/]😀B"else"A中😀B"
            val doc=TypesetDocument(pages=List(pages){page->TypesetPage(id=UUID(29L,page+1L).toString(),assetId=UUID(30L,page+1L).toString(),name="Synthetic page $page",story=StoryStyle(fontFamily=if(page%2==0)"Dialog"else inlineAlias),texts=List(8){index->TypesetText(id=UUID(31L,page*8+index+1L).toString(),text=unit.repeat(128),style=TypesetStyle(fontFamily=if(index%2==0)"Dialog"else baseAlias))})},presets=List(8){index->TypesetPreset(id=UUID(32L,index+1L).toString(),name="Preset $index",text=TypesetText(id=UUID(33L,index+1L).toString(),style=TypesetStyle(fontFamily=if(index%2==0)"Dialog"else baseAlias)))})
            doc.validate();val before=documentJson.encodeToString(doc).toByteArray();assertTrue(before.size<=4*1024*1024)
            val expected=TypesetFonts.documentFonts(doc);val hash=sha256(expected.joinToString("\n").toByteArray())
            assertEquals(pages*(1+8*(1+512))+8,expected.size)
            fun verify(actual:List<String>){assertEquals(expected,actual);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}
            repeat(2){verify(TypesetFonts.documentFonts(doc))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=TypesetFonts.documentFonts(doc)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("pages",pages);put("texts",pages*8);put("context",context);put("fontReferences",actual.size);put("serializedDocumentBytes",before.size);put("fontOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("DOCUMENT_FONT_DEPENDENCIES="+JsonArray(rows))
    }
}
