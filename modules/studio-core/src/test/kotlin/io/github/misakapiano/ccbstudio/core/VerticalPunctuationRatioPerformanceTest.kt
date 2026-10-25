package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.*
import kotlin.test.*

private object RecordedVerticalRatioLayout {
    private fun chars(text:String)=text.codePoints().toArray().map{String(Character.toChars(it))}
    private val smallPunctuation by lazy { chars("，,﹐、﹑。．.：:﹕；;﹔").toSet() }
    private val closePunctuation by lazy { chars(")）｠}｝〕〙】》»〉›」』]］〛”‟’〞〟﹚﹜﹞").toSet() }
    private val openPunctuation by lazy { chars("(（｟{｛〔〘【《«〈‹「『[［〚“„‘‚‛〝﹙﹛﹝\"＂'＇").toSet() }
    private val middlePunctuation by lazy { chars("…‥—―–").toSet() }
    private val emphPunctuation by lazy { chars("！!﹗？?﹖").toSet() }
    private fun cls(ch:String)=when(ch) {
        in smallPunctuation->"small"
        in closePunctuation->"close"
        in openPunctuation->"open"
        in middlePunctuation->"middle"
        in emphPunctuation->"emph"
        else->"normal"
    }
    private fun reduction(prev:String?,next:String,t:TypesetText,step:Double):Double {
        if(prev==null || !t.punctuationOptimization)return 0.0
        val p=when(t.punctuationTightness){"compact"->doubleArrayOf(.20,.18,.28,.10,.10,.28,.22,.08);"loose"->doubleArrayOf(.08,.06,.12,.04,.04,.12,.10,.02);else->doubleArrayOf(.14,.12,.20,.07,.07,.20,.16,.05)}
        fun part(c:String,before:Boolean)=when(c){"small"->p[if(before)0 else 1];"close"->p[if(before)2 else 3];"open"->p[if(before)4 else 5];"middle"->p[6];"emph"->p[7];else->0.0}
        return min((part(prev,false)+part(next,true))*t.style.fontSize,step*.36)
    }
    fun layout(t:TypesetText):TypesetLayout.Layout {
        val paragraphs=mutableListOf(mutableListOf<TypesetLayout.Token>())
        TypesetLayout.tokens(t.text,t.style).forEach{if(it.ch=="\n")paragraphs+=mutableListOf<TypesetLayout.Token>() else paragraphs.last()+=it}
        val lines=mutableListOf<TypesetLayout.Line>();val step=max(1.0,t.style.fontSize*t.lineHeight+t.letterSpacing)
        for(para in paragraphs){var current=mutableListOf<TypesetLayout.Cell>();var used=0.0;var prev:String?=null
            for(token in if(t.vertical)TypesetLayout.verticalTokens(para)else para){val c=if(t.vertical && t.punctuationOptimization)cls(token.ch)else"normal"
                var advance=if(t.vertical)max(step*.64,step-reduction(prev,c,t,step))else TypesetLayout.measure(token.ch,token.style)
                val spacing=if(!t.vertical && current.isNotEmpty())t.letterSpacing else 0.0
                if(current.isNotEmpty() && used+spacing+advance>t.width){lines+=TypesetLayout.Line(current,used);current=mutableListOf();used=0.0;prev=null;if(t.vertical)advance=step}
                if(!t.vertical && current.isNotEmpty())used+=t.letterSpacing
                current+=TypesetLayout.Cell(token,advance,c);used+=advance;prev=c
            };lines+=TypesetLayout.Line(current,used)
        }
        val pitch=t.style.fontSize*t.lineHeight
        return if(t.vertical)TypesetLayout.Layout(lines,max(pitch,lines.size*pitch),t.width,true)else TypesetLayout.Layout(lines,t.width,max(pitch,lines.size*pitch),false)
    }
}

/** Actual vertical layout only; its vertical branch never measures fonts or renders images. */
class VerticalPunctuationRatioPerformanceTest {
    @Test fun measuredVerticalRatiosKeepCompleteLayoutAndAdmittedDocument() {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val rows=mutableListOf<JsonObject>();val style=TypesetStyle(fontFamily="Dialog")
        val known=TypesetLayout.layout(TypesetText(text="A，中",style=style.copy(fontSize=10.0),width=1000.0,vertical=true))
        assertEquals(listOf("normal","small","normal"),known.lines.single().cells.map{it.cls})
        listOf(12.0,10.6,10.8).zip(known.lines.single().cells).forEach{(expected,cell)->assertEquals(expected,cell.advance,1e-9)}
        assertEquals(33.4,known.lines.single().width,1e-9);assertEquals(12.0,known.width,1e-9);assertEquals(1000.0,known.height)
        val unit="A中，）(…！😀한%「".codePoints().toArray().map{String(Character.toChars(it))}
        for(count in listOf(128,1024,8192))for(profile in listOf("disabled","standard","compact","loose")) {
            val enabled=profile!="disabled";val tightness=if(enabled)profile else "standard"
            val text=List(count){unit[it%unit.size]}.joinToString("")
            val item=TypesetText(id=UUID(24L,1L).toString(),text=text,style=style,width=384.0,vertical=true,punctuationOptimization=enabled,punctuationTightness=tightness)
            val doc=TypesetDocument(pages=listOf(TypesetPage(id=UUID(25L,1L).toString(),assetId=UUID(26L,1L).toString(),name="Synthetic punctuation",texts=listOf(item))))
            assertTrue(item.vertical);doc.validate();val before=documentJson.encodeToString(doc).toByteArray();assertTrue(before.size<=4*1024*1024)
            val expected=TypesetLayout.layout(item);val hash=sha256(expected.toString().toByteArray())
            assertEquals(count,expected.lines.sumOf{it.cells.size})
            fun verify(actual:TypesetLayout.Layout){assertEquals(expected,actual);assertContentEquals(before,documentJson.encodeToString(doc).toByteArray())}
            repeat(2){verify(RecordedVerticalRatioLayout.layout(item));verify(TypesetLayout.layout(item))}
            repeat(4){round->
                for(kind in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                    val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                    val actual=if(kind=="recorded")RecordedVerticalRatioLayout.layout(item)else TypesetLayout.layout(item)
                    val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                    val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                    verify(actual)
                    rows+=buildJsonObject{put("kind",kind);put("profile",profile);put("enabled",enabled);put("inputCodepoints",count);put("utf16Length",text.length);put("lines",actual.lines.size);put("serializedDocumentBytes",before.size);put("layoutSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
                }
            }
        }
        println("VERTICAL_PUNCTUATION_RATIOS="+JsonArray(rows))
    }

    @Test fun ratioTablesStylesTightnessWrappingAndFallbackKeepCompleteLayout() {
        val style=TypesetStyle(fontFamily="Dialog")
        val samples=listOf("", "A，中", "，,﹐、﹑。．.：:﹕；;﹔", ")）｠}｝〕〙】》»〉›」』]］〛”‟’〞〟﹚﹜﹞", "(（｟{｛〔〘【《«〈‹「『[［〚“„‘‚‛〝﹙﹛﹝\"＂'＇", "…‥—―–！!﹗？?﹖", "中文한글αβe\u0301😀𠀀%・", "... .. . \"a\" a'b", "[size=48]中[/][b]A[/]，。\n한😀")
        for(text in samples)for(tightness in listOf("standard","compact","loose"))for(width in listOf(10.0,48.0,384.0)) {
            val item=TypesetText(text=text,style=style,width=width,vertical=true,punctuationTightness=tightness,letterSpacing=3.0)
            assertTrue(item.vertical);val before=item.copy()
            for(enabled in listOf(false,true)) {
                val input=item.copy(punctuationOptimization=enabled)
                assertEquals(RecordedVerticalRatioLayout.layout(input),TypesetLayout.layout(input))
            }
            assertEquals(before,item)
        }
        for(size in listOf(6.0,1000.0)) {
            val item=TypesetText(text="中，(A)…😀",style=style.copy(fontSize=size),width=48.0,vertical=true,lineHeight=2.0)
            assertEquals(RecordedVerticalRatioLayout.layout(item),TypesetLayout.layout(item))
        }

        for((tightness,advances) in listOf("standard" to listOf(12.0,10.6,10.8),"compact" to listOf(12.0,10.0,10.2),"loose" to listOf(12.0,11.2,11.4),"unknown-helper" to listOf(12.0,10.6,10.8))) {
            val item=TypesetText(text="A，中",style=style.copy(fontSize=10.0),width=1000.0,vertical=true,punctuationTightness=tightness)
            val actual=TypesetLayout.layout(item);assertEquals(RecordedVerticalRatioLayout.layout(item),actual)
            advances.zip(actual.lines.single().cells).forEach{(expected,cell)->assertEquals(expected,cell.advance,1e-9)}
        }
        val disabled=TypesetLayout.layout(TypesetText(text="A，中",style=style.copy(fontSize=10.0),width=1000.0,vertical=true,punctuationOptimization=false))
        assertEquals(listOf("normal","normal","normal"),disabled.lines.single().cells.map{it.cls})
        assertEquals(listOf(12.0,12.0,12.0),disabled.lines.single().cells.map{it.advance})
    }
    @Test fun simultaneousRatioProfilesKeepTablesAndLineStateIndependent() {
        val item=TypesetText(text="中，(A)…😀\n한。",style=TypesetStyle(fontFamily="Dialog"),width=48.0,vertical=true)
        assertTrue(item.vertical);val profiles=listOf("standard","compact","loose")
        val expected=profiles.associateWith{RecordedVerticalRatioLayout.layout(item.copy(punctuationTightness=it))};val pool=Executors.newFixedThreadPool(2)
        try {
            val futures=List(2){worker->pool.submit{repeat(50){round->val profile=profiles[(round+worker)%profiles.size];val actual=TypesetLayout.layout(item.copy(punctuationTightness=profile));assertEquals(expected.getValue(profile),actual);assertNotSame(expected.getValue(profile).lines,actual.lines)}}}
            futures.forEach{it.get(10,TimeUnit.SECONDS)}
        }finally{pool.shutdown();assertTrue(pool.awaitTermination(10,TimeUnit.SECONDS))}
    }
}
