package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.*

/** Actual parser calls over admitted synthetic document colors; no fonts/rendering/window. */
class TypesetColorSyntaxPerformanceTest {
    @Test fun measuredActualColorSyntaxKeepsAdmittedDocumentValuesAndSnapshots() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>()
        for(count in listOf(32,128,500))for((kind,palette) in listOf(
            "hex-named" to listOf("#abc","#1234","#11223344","rebeccapurple","transparent"),
            "functional" to listOf("rgba(240, 12, 30, .5)","hsl(210 50% 40% / 70%)","hwb(2turn 10% 20% / .4)","rgb(5e1 1e2 2e2 / .8)","hsla(180, 30%, 50%, 40%)"))) {
            val texts=List(count){i->TypesetText(id=UUID(15L,i.toLong()+1).toString(),text="Synthetic $i",style=TypesetStyle(fill=palette[i%palette.size],stroke=palette[(i+1)%palette.size]),background=palette[(i+2)%palette.size],border=palette[(i+3)%palette.size])}
            val doc=TypesetDocument(pages=listOf(TypesetPage(id=UUID(16L,1L).toString(),assetId=UUID(17L,1L).toString(),name="Synthetic colors",texts=texts)))
            doc.validate();val before=documentJson.encodeToString(doc).toByteArray();assertTrue(before.size<=4*1024*1024)
            val colors=texts.flatMap{listOf(it.style.fill,it.style.stroke,it.background,it.border)}
            val expected=colors.map{TypesetColors.parse(it).rgb};val hash=sha256(expected.joinToString(",").toByteArray())
            fun verify(actual:List<java.awt.Color>){assertEquals(expected,actual.map{it.rgb});assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}
            repeat(2){verify(colors.map{TypesetColors.parse(it)})}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=colors.map{TypesetColors.parse(it)}
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("context",kind);put("texts",count);put("colorCalls",colors.size);put("serializedDocumentBytes",before.size);put("argbOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("TYPESET_COLOR_SYNTAX="+JsonArray(rows))
    }
}
