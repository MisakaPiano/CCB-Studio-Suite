package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

private object RecordedVerticalProjection {
    private fun chars(text:String)=text.codePoints().toArray().map{String(Character.toChars(it))}
    private val force=buildMap<String,String> {
        fun group(input:String,output:String){chars(input).forEach{put(it,output)}}
        group("，,﹐","︐");group("、﹑","︑");group("。．.","︒");group("：:﹕","︓");group("；;﹔","︔")
        group("！!﹗","︕");group("？?﹖","︖");group("〖","︗");group("〗","︘");group("…","︙");group("‥","︰")
        group("—―","︱");group("–","︲");group("_＿","︳");group("(（｟﹙","︵");group(")）｠﹚","︶")
        group("{｛﹛","︷");group("}｝﹜","︸");group("〔〘﹝","︹");group("〕〙﹞","︺")
        group("【","︻");group("】","︼");group("《«","︽");group("》»","︾");group("〈‹","︿");group("〉›","﹀")
        group("「‘‚‛","﹁");group("」’","﹂");group("『“„〝","﹃");group("』”‟〞〟","﹄")
        group("[［〚","﹇");group("]］〛","﹈")
    }
    private val uprightFallback=chars("・･·‧•※々〃ゝゞヽヾ﹅﹆").toSet()
    private fun presentation(ch:String)=ch.codePointAt(0).let{it in 0xfe10..0xfe19 || it in 0xfe30..0xfe4f}
    fun verticalTokens(input:List<TypesetLayout.Token>):List<TypesetLayout.Token> {
        val out=mutableListOf<TypesetLayout.Token>();var doubleOpen=true;var singleOpen=true;var i=0
        while(i<input.size){val t=input[i];val ch=t.ch;val cp=ch.codePointAt(0)
            val next=when {
                ch in setOf("\"","＂")->t.copy(display=if(doubleOpen)"﹃"else"﹄",forced=true).also{doubleOpen=!doubleOpen}
                ch in setOf("'","＇")->if(input.getOrNull(i-1)?.ch?.codePointAt(0)?.let(Character::isLetterOrDigit)==true && input.getOrNull(i+1)?.ch?.codePointAt(0)?.let(Character::isLetterOrDigit)==true)t.copy(rotate=true)
                    else t.copy(display=if(singleOpen)"﹁"else"﹂",forced=true).also{singleOpen=!singleOpen}
                ch=="." && input.getOrNull(i+1)?.ch=="."->if(input.getOrNull(i+2)?.ch=="."){i+=2;t.copy(ch="…",display="︙",forced=true)}else{i++;t.copy(ch="‥",display="︰",forced=true)}
                presentation(ch)->t.copy(forced=true)
                force[ch]!=null->t.copy(display=force.getValue(ch),forced=true)
                cp in 48..57 || cp in 65..90 || cp in 97..122->t.copy(display=String(Character.toChars(cp+0xfee0)),forced=true,alnum=true)
                else->t.copy(rotate=ch !in uprightFallback && (Regex("\\p{P}").matches(ch)||ch in chars("/\\／＼~～〜=＝+＋*＊&＆@＠#＃%％^＾<>＜＞|｜")))
            };out+=next;i++
        };return out
    }
}

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
            repeat(2){verify(RecordedVerticalProjection.verticalTokens(input));verify(TypesetLayout.verticalTokens(input))}
            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedVerticalProjection.verticalTokens(input)else TypesetLayout.verticalTokens(input)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("context",context);put("inputTokens",input.size);put("utf16Length",text.length);put("outputTokens",actual.size);put("serializedDocumentBytes",before.size);put("tokenOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
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

    @Test fun unicodeStylesQuoteStateAndInvalidHelpersKeepRecordedSemantics() {
        val style=TypesetStyle(fontFamily="Dialog",fontSize=31.0,fill="#123456",italic=true)
        val samples=listOf("", "\"＂\"＂ '＇'＇", "can't a＇b '中'", ". .. ... .... .....", "︐︵︶・･·‧•※々〃ゝゞヽヾ﹅﹆", "/\\／＼~～〜=＝+＋*＊&＆@＠#＃%％^＾<>＜＞|｜", "中文한글αβ😀e\u0301𠀀\n\t", "[b]A[/][size=42]中[/]...")
        for(text in samples) {
            val input=TypesetLayout.tokens(text,style);val snapshot=input.toList()
            val expected=RecordedVerticalProjection.verticalTokens(input)
            repeat(3){assertEquals(expected,TypesetLayout.verticalTokens(input));assertEquals(snapshot,input)}
        }
        for(quote in listOf("\"","＂","'","＇"))repeat(3) {
            assertEquals(listOf(if(quote=="\""||quote=="＂")"﹃"else"﹁"),TypesetLayout.verticalTokens(TypesetLayout.tokens(quote,style)).map{it.display})
        }
        val decorated=listOf(TypesetLayout.Token("中",style,"custom",rotate=true,forced=true,alnum=true),TypesetLayout.Token("%",style.copy(fontSize=47.0),"mark",forced=true))
        assertEquals(RecordedVerticalProjection.verticalTokens(decorated),TypesetLayout.verticalTokens(decorated))
        // Empty helper tokens are not admitted document input; preserve the old exception too.
        val invalid=listOf(TypesetLayout.Token("",style))
        val oldError=assertFails{RecordedVerticalProjection.verticalTokens(invalid)}
        val newError=assertFails{TypesetLayout.verticalTokens(invalid)}
        assertEquals(oldError.javaClass,newError.javaClass);assertEquals(oldError.message,newError.message)
    }
    @Test fun simultaneousProjectionKeepsPerCallQuoteStateAndFreshResults() {
        val input=TypesetLayout.tokens("\"中文😀...\" 'a' a'b α%・",TypesetStyle(fontFamily="Dialog"))
        val snapshot=input.toList();val expected=RecordedVerticalProjection.verticalTokens(input)
        val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(100){val actual=TypesetLayout.verticalTokens(input);assertEquals(expected,actual);assertNotSame(expected,actual);assertEquals(snapshot,input)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
}
