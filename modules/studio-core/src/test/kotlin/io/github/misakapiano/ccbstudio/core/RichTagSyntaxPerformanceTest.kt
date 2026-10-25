package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

private object RecordedRichTagTokens {
    fun tokens(text:String,base:TypesetStyle):List<TypesetLayout.Token> {
        require(text.length<=100000)
        val out=mutableListOf<TypesetLayout.Token>();val stack=mutableListOf(base);var i=0
        while(i<text.length) {
            if(text[i]=='[') {
                if(text.getOrNull(i+1)=='['){out+=TypesetLayout.Token("[",stack.last());i+=2;continue}
                val end=text.indexOf(']',i+1)
                if(end>i){val tag=text.substring(i+1,end).trim()
                    if(tag=="/"){if(stack.size>1)stack.removeAt(stack.lastIndex);i=end+1;continue}
                    patch(tag,stack.last())?.let{require(stack.size<1000){"Rich text nesting too deep"};stack+=it;i=end+1}
                    if(i==end+1)continue
                }
            }
            val cp=text.codePointAt(i);out+=TypesetLayout.Token(String(Character.toChars(cp)),stack.last());i+=Character.charCount(cp)
        }
        return out
    }
    private fun patch(tag:String,s:TypesetStyle):TypesetStyle? {
        when(tag.lowercase()) {
            "b","bold"->return s.copy(fontWeight=700);"i","italic"->return s.copy(italic=true)
            "u","underline"->return s.copy(underline=true);"strike","del"->return s.copy(strike=true)
            "plain"->return s.copy(fontWeight=400,italic=false,underline=false,strike=false)
        }
        val m=Regex("^([a-zA-Z_]+)\\s*=\\s*(.+)$").matchEntire(tag)?:return null
        val v=m.groupValues[2].trim().let{if(it.length>=2 && ((it.first()=='"' && it.last()=='"')||(it.first()=='\'' && it.last()=='\'')))it.substring(1,it.length-1)else it}
        fun number(default:Double)=v.toDoubleOrNull()?.takeIf{it.isFinite() && it!=0.0}?:default
        return when(m.groupValues[1].lowercase()) {
            "f","font","fontfamily"->s.copy(fontFamily=v.take(256))
            // Preserve source syntax for editing/layout; color validation belongs to
            // rendering so an invalid inline color cannot crash selection geometry.
            "c","color","fill"->s.copy(fill=v)
            "stroke","outline"->s.copy(stroke=v)
            "strokewidth","ow","outlinewidth"->s.copy(strokeWidth=number(0.0).coerceIn(0.0,100.0))
            "size","fs"->s.copy(fontSize=number(s.fontSize).coerceIn(6.0,1000.0))
            "weight","w"->s.copy(fontWeight=number(s.fontWeight.toDouble()).coerceIn(100.0,900.0).toInt())
            else->null
        }
    }
}

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
            repeat(2){verify(RecordedRichTagTokens.tokens(text,base));verify(TypesetLayout.tokens(text,base))}
            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedRichTagTokens.tokens(text,base)else TypesetLayout.tokens(text,base)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("context",context);put("units",count);put("utf16Length",text.length);put("outputTokens",actual.size);put("serializedDocumentBytes",before.size);put("tokenOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }
        }
        println("RICH_TAG_TOKENS="+JsonArray(rows))
    }

    @Test fun aliasesQuotedMalformedTagsNumericRulesAndLimitsKeepRecordedResults() {
        val base=TypesetStyle(fontFamily="Dialog",fontSize=31.0,fill="#123456",italic=true)
        val samples=listOf("", "[[b]中😀", "[unknown=7]X", "[size=48", "[/]X[/]", "[B][i][u][del]X[/][/][/][/]Y", "[plain]X[/]Y", "[ f = 'Example, serif' ]X[/][fontfamily=\"Dialog\"]Y[/]", "[size=0]X[/][size=NaN]Y[/][fs=-1]Z[/][fs=9999]Q[/]", "[weight=-5]A[/][w=9999]B[/][ow=Infinity]C[/][outlinewidth=200]D[/]", "[c=bad-color]A[/][fill=rgb(1,2,3)]B[/][outline=#abc]C[/][strokewidth=2]D[/]", "[size=\n48]X[/][size=]Y[/][sïze=48]Z[/]", "中文한글αβe\u0301😀𠀀\uD800")
        for(text in samples){val expected=RecordedRichTagTokens.tokens(text,base);repeat(3){assertEquals(expected,TypesetLayout.tokens(text,base))}}
        assertEquals("[b]中😀",TypesetLayout.tokens("[[b]中😀",base).joinToString(""){it.ch})
        assertEquals("[unknown=7]X",TypesetLayout.tokens("[unknown=7]X",base).joinToString(""){it.ch})
        assertEquals(listOf(31.0,31.0,6.0,1000.0),TypesetLayout.tokens("[size=0]X[/][size=NaN]Y[/][fs=-1]Z[/][fs=9999]Q[/]",base).map{it.style.fontSize})
        assertEquals("bad-color",TypesetLayout.tokens("[color=bad-color]X[/]",base).single().style.fill)
        assertEquals("z".repeat(256),TypesetLayout.tokens("[font=${"z".repeat(300)}]X[/]",base).single().style.fontFamily)
        val boundary="[b]".repeat(999)+"X"+"[/]".repeat(999)
        assertEquals(RecordedRichTagTokens.tokens(boundary,base),TypesetLayout.tokens(boundary,base))
        assertEquals(100000,TypesetLayout.tokens("X".repeat(100000),base).size)
        for(text in listOf("[b]".repeat(1000)+"X","X".repeat(100001))) {
            val oldError=assertFails{RecordedRichTagTokens.tokens(text,base)};val newError=assertFails{TypesetLayout.tokens(text,base)}
            assertEquals(oldError.javaClass,newError.javaClass);assertEquals(oldError.message,newError.message)
        }
        assertEquals(base,TypesetLayout.tokens("X",base).single().style)
        repeat(3){assertEquals(base,TypesetLayout.tokens("X",base).single().style)}
    }
    @Test fun simultaneousTagParsingKeepsIndependentStyleStacksAndResults() {
        val base=TypesetStyle(fontFamily="Dialog");val text="[size=48]中[/][color=#123456]A😀[/]B"
        val expected=RecordedRichTagTokens.tokens(text,base);val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){pool.submit{repeat(100){val actual=TypesetLayout.tokens(text,base);assertEquals(expected,actual);assertNotSame(expected,actual);assertEquals(base,TypesetLayout.tokens("X",base).single().style)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
}
