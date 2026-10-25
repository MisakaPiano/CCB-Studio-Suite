package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

private object RecordedDocumentFontDependencies {
    fun documentFonts(document:TypesetDocument):List<String> = document.pages.flatMap{p->listOf(p.story.fontFamily)+p.texts.flatMap{t->listOf(t.style.fontFamily)+TypesetLayout.tokens(t.text,t.style).map{it.style.fontFamily}}}+document.presets.map{it.text.style.fontFamily}
}

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
            repeat(2){verify(RecordedDocumentFontDependencies.documentFonts(doc));verify(TypesetFonts.documentFonts(doc))}
            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedDocumentFontDependencies.documentFonts(doc)else TypesetFonts.documentFonts(doc)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("pages",pages);put("texts",pages*8);put("context",context);put("fontReferences",actual.size);put("serializedDocumentBytes",before.size);put("fontOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }
        }
        println("DOCUMENT_FONT_DEPENDENCIES="+JsonArray(rows))
    }

    @Test fun emptyHiddenTextPresetsDuplicatesAndParserErrorsKeepDependencySemantics() {
        val alias="ccb-font:"+UUID(34L,1L)
        val doc=TypesetDocument(pages=listOf(TypesetPage(assetId=UUID(35L,1L).toString(),name="All text included",story=StoryStyle(fontFamily=alias),texts=listOf(TypesetText(text="",style=TypesetStyle(fontFamily="EmptyBase")),TypesetText(text="[font='Inline']A[/]B",visible=false,locked=true,style=TypesetStyle(fontFamily=alias)),TypesetText(text="[unknown=9]中文😀[[",style=TypesetStyle(fontFamily="Dialog"))))),presets=listOf(TypesetPreset(name="Only base is used",text=TypesetText(text="[font=PresetInline]X[/]",style=TypesetStyle(fontFamily="PresetBase")))))
        doc.validate();val before=documentJson.encodeToString(doc).toByteArray()
        val expected=RecordedDocumentFontDependencies.documentFonts(doc);assertEquals(expected,TypesetFonts.documentFonts(doc))
        assertEquals(listOf(alias,"EmptyBase",alias,"Inline",alias),expected.take(5));assertEquals("PresetBase",expected.last());assertTrue("PresetInline" !in expected)
        val mutableCopy=TypesetFonts.documentFonts(doc).toMutableList();mutableCopy.clear();assertEquals(expected,TypesetFonts.documentFonts(doc))
        assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())
        assertEquals(emptyList(),TypesetFonts.documentFonts(TypesetDocument()))
        assertEquals(listOf("PresetBase"),TypesetFonts.documentFonts(TypesetDocument(presets=doc.presets)))
        for(text in listOf("[b]".repeat(1000)+"X","X".repeat(100001))) {
            val invalid=doc.copy(pages=listOf(doc.pages.single().copy(texts=listOf(TypesetText(text=text)))))
            val oldError=assertFails{RecordedDocumentFontDependencies.documentFonts(invalid)};val newError=assertFails{TypesetFonts.documentFonts(invalid)}
            assertEquals(oldError.javaClass,newError.javaClass);assertEquals(oldError.message,newError.message)
        }
        // Original helper reads preset base only, including malformed preset text; no admission claim.
        val invalidPreset=TypesetDocument(presets=listOf(doc.presets.single().copy(text=doc.presets.single().text.copy(text="X".repeat(100001)))))
        assertEquals(listOf("PresetBase"),TypesetFonts.documentFonts(invalidPreset));assertEquals(RecordedDocumentFontDependencies.documentFonts(invalidPreset),TypesetFonts.documentFonts(invalidPreset))
    }
    @Test fun simultaneousDependencyListsKeepOrderAndCallerDocumentIndependent() {
        val doc=TypesetDocument(pages=listOf(TypesetPage(assetId=UUID(36L,1L).toString(),name="Metadata only",texts=listOf(TypesetText(text="[font=Inline]中😀[/]A",style=TypesetStyle(fontFamily="Dialog"))))))
        val expected=RecordedDocumentFontDependencies.documentFonts(doc);val before=documentJson.encodeToString(doc).toByteArray();val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(50){val actual=TypesetFonts.documentFonts(doc);assertEquals(expected,actual);assertNotSame(expected,actual);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
}
