package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

/** Actual vertical layout only; its vertical branch never measures fonts or renders images. */
class VerticalPunctuationClassPerformanceTest {
    @Test fun measuredVerticalLayoutKeepsClassesAdvancesWrappingAndDocument() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>();val style=TypesetStyle(fontFamily="Dialog")
        val known=TypesetLayout.layout(TypesetText(text="A，中",style=style.copy(fontSize=10.0),width=1000.0,vertical=true))
        assertEquals(listOf("normal","small","normal"),known.lines.single().cells.map{it.cls})
        listOf(12.0,10.6,10.8).zip(known.lines.single().cells).forEach{(expected,cell)->assertEquals(expected,cell.advance,1e-9)}
        assertEquals(33.4,known.lines.single().width,1e-9);assertEquals(12.0,known.width,1e-9);assertEquals(1000.0,known.height)
        val unit="A中，）(…！😀한%「".codePoints().toArray().map{String(Character.toChars(it))}
        for(count in listOf(128,1024,8192))for(enabled in listOf(false,true)) {
            val text=List(count){unit[it%unit.size]}.joinToString("")
            val item=TypesetText(id=UUID(24L,1L).toString(),text=text,style=style,width=384.0,vertical=true,punctuationOptimization=enabled)
            val doc=TypesetDocument(pages=listOf(TypesetPage(id=UUID(25L,1L).toString(),assetId=UUID(26L,1L).toString(),name="Synthetic punctuation",texts=listOf(item))))
            assertTrue(item.vertical);doc.validate();val before=documentJson.encodeToString(doc).toByteArray();assertTrue(before.size<=4*1024*1024)
            val expected=TypesetLayout.layout(item);val hash=sha256(expected.toString().toByteArray())
            assertEquals(count,expected.lines.sumOf{it.cells.size})
            fun verify(actual:TypesetLayout.Layout){assertEquals(expected,actual);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}
            repeat(2){verify(TypesetLayout.layout(item))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=TypesetLayout.layout(item)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("enabled",enabled);put("inputCodepoints",count);put("utf16Length",text.length);put("lines",actual.lines.size);put("serializedDocumentBytes",before.size);put("layoutSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("VERTICAL_PUNCTUATION_CLASSES="+JsonArray(rows))
    }
}
