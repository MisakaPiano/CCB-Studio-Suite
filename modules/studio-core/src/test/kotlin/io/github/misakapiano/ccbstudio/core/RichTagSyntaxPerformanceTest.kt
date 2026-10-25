package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

/** Actual rich text token parsing only, no font/measure/layout/render/provider APIs. */
class RichTagSyntaxPerformanceTest {
    @Test fun measuredActualRichTokensKeepAdmittedDocumentStyleAndSnapshots() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>();val base=TypesetStyle(fontFamily="Dialog")
        val vector=TypesetLayout.tokens("X[size=48]中[/][b]B[/]",base)
        assertEquals(listOf(TypesetLayout.Token("X",base),TypesetLayout.Token("中",base.copy(fontSize=48.0)),TypesetLayout.Token("B",base.copy(fontWeight=700))),vector)
        for(count in listOf(32,128,512))for((context,unit) in listOf("plain" to "A中😀","simple-tags" to "[b]A[/][i]中[/][plain]B[/]","attribute-tags" to "[font=\"Dialog\"]A[/][size=48]中[/][color=\"#123456\"]B[/][outlinewidth=2]C[/][w=700]D[/]")) {
            val text=unit.repeat(count);assertTrue(text.length<=100000)
            val item=TypesetText(id=UUID(21L,1L).toString(),text=text,style=base)
            val doc=TypesetDocument(pages=listOf(TypesetPage(id=UUID(22L,1L).toString(),assetId=UUID(23L,1L).toString(),name="Synthetic rich tags",texts=listOf(item))))
            doc.validate();val before=documentJson.encodeToString(doc).toByteArray();assertTrue(before.size<=4*1024*1024)
            val expected=TypesetLayout.tokens(text,base);val hash=sha256(expected.joinToString("\n").toByteArray())
            assertEquals(count*(if(context=="attribute-tags")5 else 3),expected.size)
            fun verify(actual:List<TypesetLayout.Token>){assertEquals(expected,actual);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray());assertEquals(TypesetStyle(fontFamily="Dialog"),base)}
            repeat(2){verify(TypesetLayout.tokens(text,base))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=TypesetLayout.tokens(text,base)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("context",context);put("units",count);put("utf16Length",text.length);put("outputTokens",actual.size);put("serializedDocumentBytes",before.size);put("tokenOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("RICH_TAG_TOKENS="+JsonArray(rows))
    }
}
