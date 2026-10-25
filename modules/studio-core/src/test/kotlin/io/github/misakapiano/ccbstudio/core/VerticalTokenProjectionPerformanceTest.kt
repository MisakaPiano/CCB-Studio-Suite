package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.UUID
import kotlin.test.*

/** Actual character projection only; no font/measure/layout/render/provider or user-file APIs. */
class VerticalTokenProjectionPerformanceTest {
    @Test fun measuredActualVerticalProjectionKeepsTokensStyleAndAdmittedDocument() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>();val style=TypesetStyle(fontFamily="Dialog")
        val known=TypesetLayout.verticalTokens(TypesetLayout.tokens("A1，...\"\"中",style))
        assertEquals(listOf("A","1","，","…","\"","\"","中"),known.map{it.ch})
        assertEquals(listOf("Ａ","１","︐","︙","﹃","﹄","中"),known.map{it.display})
        assertTrue(known.all{!it.rotate&&it.style==style})
        assertEquals(listOf(true,true,true,true,true,true,false),known.map{it.forced})
        assertEquals(listOf(true,true,false,false,false,false,false),known.map{it.alnum})
        for(count in listOf(128,1024,8192))for((context,unit) in listOf("mapped-control" to "Ab12，。!?...()\"'","fallback" to "中文한글αβ😀%&※・")) {
            val units=unit.codePoints().toArray().map{String(Character.toChars(it))}
            val text=List(count){units[it%units.size]}.joinToString("")
            val item=TypesetText(id=UUID(18L,1L).toString(),text=text,style=style,vertical=true)
            val doc=TypesetDocument(pages=listOf(TypesetPage(id=UUID(19L,1L).toString(),assetId=UUID(20L,1L).toString(),name="Synthetic vertical",texts=listOf(item))))
            doc.validate();val before=documentJson.encodeToString(doc).toByteArray();assertTrue(before.size<=4*1024*1024)
            val input=TypesetLayout.tokens(text,style);assertEquals(count,input.size);val inputBefore=input.toList()
            val expected=TypesetLayout.verticalTokens(input);val hash=sha256(expected.joinToString("\n").toByteArray())
            fun verify(actual:List<TypesetLayout.Token>){assertEquals(expected,actual);assertEquals(inputBefore,input);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}
            repeat(2){verify(TypesetLayout.verticalTokens(input))}
            repeat(4){round->
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=TypesetLayout.verticalTokens(input)
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("context",context);put("inputTokens",input.size);put("utf16Length",text.length);put("outputTokens",actual.size);put("serializedDocumentBytes",before.size);put("tokenOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
            }
        }
        println("VERTICAL_TOKEN_PROJECTION="+JsonArray(rows))
    }
    @Test fun fixedSourceVerticalMetadataMatchesWithoutFontMeasurementOrRendering() {
        val rel="docs/audits/evidence/TYPESET_REFERENCE/source-output"
        val repo=generateSequence(Path.of(System.getProperty("preview.test.root")).toAbsolutePath()){it.parent}.first{Files.isDirectory(it.resolve(rel),LinkOption.NOFOLLOW_LINKS)}
        val fixtures=listOf("TS-V01" to "f5e9ed66f711f95c197e0d1063c73c2f89ab526a4c4a7bc4b8fd1a83744ef7d8","TS-V02" to "7b593412c6cea5a5ba0483b9f615124bed509766631229c9c03c12c15bcede2e","TS-R01-vertical" to "953a8b62b5918a637f68121fedd221cf48c9dd1d2d6ae8a62e951bf371128636")
        val identities=mutableListOf<JsonObject>()
        for((name,expectedSha) in fixtures) {
            val path=repo.resolve(rel).resolve("$name.json");safePath(path);val raw=Files.readAllBytes(path);assertEquals(expectedSha,sha256(raw))
            val root=documentJson.parseToJsonElement(raw.decodeToString()).jsonObject;val o=root.getValue("input").jsonObject
            fun s(k:String)=o.getValue(k).jsonPrimitive.content
            fun d(k:String)=o.getValue(k).jsonPrimitive.double
            fun b(k:String)=o.getValue(k).jsonPrimitive.boolean
            val style=TypesetStyle(s("fontFamily"),d("fontSize"),d("fontWeight").toInt(),s("fontStyle")=="italic",b("underline"),b("strike"),s("fill"),s("stroke"),d("strokeWidth"))
            val paragraphs=mutableListOf(mutableListOf<TypesetLayout.Token>())
            TypesetLayout.tokens(s("text"),style).forEach{if(it.ch=="\n")paragraphs+=mutableListOf<TypesetLayout.Token>()else paragraphs.last()+=it}
            val actual=paragraphs.flatMap{TypesetLayout.verticalTokens(it)}
            val expected=root.getValue("metrics").jsonObject.getValue("cols").jsonArray.flatMap{it.jsonArray}
            assertEquals(expected.size,actual.size,name)
            expected.zip(actual).forEach{(value,token)->val e=value.jsonObject
                assertEquals(e.getValue("displayCh").jsonPrimitive.content,token.display,name)
                assertEquals(e.getValue("rotate").jsonPrimitive.boolean,token.rotate,name)
                assertEquals(e.getValue("style").jsonObject.getValue("fontSize").jsonPrimitive.double,token.style.fontSize,name)
            }
            identities+=buildJsonObject{put("path","$rel/$name.json");put("rawSha256",sha256(raw));put("bytes",raw.size);put("tokens",actual.size)}
        }
        println("VERTICAL_SOURCE_FIXTURES="+JsonArray(identities))
    }
}
