package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.util.UUID
import java.util.Locale
import java.awt.Color
import kotlin.math.*
import kotlin.test.*

private object RecordedTypesetSyntaxColors {
    private val names by lazy {
        documentJson.parseToJsonElement(requireNotNull(javaClass.getResource("/typeset/css-named-colors.json")).readText()).jsonObject.mapValues{it.value.jsonPrimitive.content}
    }
    fun parse(input:String):Color {
        require(input.length<=256){"颜色值过长"}
        val value=input.trim().lowercase(Locale.ROOT)
        if(value=="transparent")return Color(0,0,0,0)
        val hex=names[value]?:value
        if(hex.startsWith('#')) {
            val raw=hex.drop(1);require(raw.length in listOf(3,4,6,8)&&raw.all{it in "0123456789abcdef"}){"无效的十六进制颜色"}
            val expanded=if(raw.length<5)raw.flatMap{listOf(it,it)}.joinToString("")else raw
            return Color(expanded.substring(0,2).toInt(16),expanded.substring(2,4).toInt(16),expanded.substring(4,6).toInt(16),if(expanded.length==8)expanded.substring(6,8).toInt(16)else 255)
        }
        val match=Regex("([a-z]+)\\((.*)\\)",RegexOption.DOT_MATCHES_ALL).matchEntire(value)
            ?:throw IllegalArgumentException("无法识别颜色；可使用色名、HEX、RGB、HSL 或 HWB")
        val function=match.groupValues[1];val body=match.groupValues[2].trim();val legacy=',' in body
        require(function in setOf("rgb","rgba","hsl","hsla","hwb")){"尚不支持此颜色空间；原值保留，请明确转换为 sRGB"}
        val parts:List<String>;val alpha:String?
        if(legacy) {
            require('/' !in body && function!="hwb")
            val fields=body.split(',').map(String::trim);require(fields.size in 3..4 && fields.none{it=="none"})
            parts=fields.take(3);alpha=fields.getOrNull(3)
        } else {
            val sides=body.split('/');require(sides.size<=2)
            parts=sides[0].trim().split(Regex("\\s+"));require(parts.size==3)
            alpha=sides.getOrNull(1)?.trim()
        }
        val a=alpha?.let{component(it,1.0)}?.coerceIn(0.0,1.0)?:1.0
        fun color(rgb:List<Double>)=Color(rgb[0].coerceIn(0.0,1.0).toFloat(),rgb[1].coerceIn(0.0,1.0).toFloat(),rgb[2].coerceIn(0.0,1.0).toFloat(),a.toFloat())
        if(function in setOf("rgb","rgba")) {
            if(legacy)require(parts.all{it.endsWith('%')}||parts.none{it.endsWith('%')})
            return color(parts.map{component(it,255.0)/255.0})
        }
        val hue=hue(parts[0])
        fun percent(s:String):Double {require(s=="none"||s.endsWith('%')){"饱和度、亮度或灰度必须为百分比"};return component(s,1.0).coerceIn(0.0,1.0)}
        val second=percent(parts[1]);val third=percent(parts[2])
        if(function=="hwb") {
            if(second+third>=1.0)return color(List(3){second/(second+third)})
            return color(hsl(hue,1.0,.5).map{it*(1-second-third)+second})
        }
        return color(hsl(hue,second,third))
    }
    private fun number(value:String):Double {
        require(Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?").matches(value)){"颜色包含无效数值"}
        return value.toDouble().also{require(it.isFinite())}
    }
    private fun component(value:String,percentScale:Double)=when {
        value=="none"->0.0
        value.endsWith('%')->number(value.dropLast(1))/100*percentScale
        else->number(value)
    }
    private fun hue(value:String):Double {
        if(value=="none")return 0.0
        val degrees=when {
            value.endsWith("grad")->number(value.dropLast(4))*.9
            value.endsWith("turn")->number(value.dropLast(4))*360
            value.endsWith("deg")->number(value.dropLast(3))
            value.endsWith("rad")->number(value.dropLast(3))*180/PI
            else->number(value)
        }
        require(degrees.isFinite());return ((degrees%360)+360)%360
    }
    private fun hsl(h:Double,s:Double,l:Double):List<Double> {
        val c=(1-abs(2*l-1))*s;val x=c*(1-abs((h/60)%2-1));val m=l-c/2
        val rgb=when(h){in 0.0..<60.0->listOf(c,x,0.0);in 60.0..<120.0->listOf(x,c,0.0);in 120.0..<180.0->listOf(0.0,c,x);in 180.0..<240.0->listOf(0.0,x,c);in 240.0..<300.0->listOf(x,0.0,c);else->listOf(c,0.0,x)}
        return rgb.map{it+m}
    }
}

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
            repeat(2){verify(colors.map{RecordedTypesetSyntaxColors.parse(it)});verify(colors.map{TypesetColors.parse(it)})}
            repeat(4){round->
              for(mode in if(round%2==0)listOf("recorded","current")else listOf("current","recorded")) {
                val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread)
                val cpu=bean.currentThreadCpuTime;val started=System.nanoTime()
                val actual=colors.map{if(mode=="recorded")RecordedTypesetSyntaxColors.parse(it)else TypesetColors.parse(it)}
                val elapsed=(System.nanoTime()-started)/1e6;val cpuMs=(bean.currentThreadCpuTime-cpu)/1e6
                val allocatedBytes=bean.getThreadAllocatedBytes(thread)-allocated
                verify(actual)
                rows+=buildJsonObject{put("kind",mode);put("context",kind);put("texts",count);put("colorCalls",colors.size);put("serializedDocumentBytes",before.size);put("argbOrderSha256",hash);put("round",round);put("wallMs",elapsed);put("cpuMs",cpuMs);put("allocatedBytes",allocatedBytes)}
              }
            }
        }
        println("TYPESET_COLOR_SYNTAX="+JsonArray(rows))
    }
    @Test fun exactArgbAndInvalidOutcomeMatchRecordedParserIncludingTrackedSourceCases() {
        val tracked=documentJson.parseToJsonElement(requireNotNull(javaClass.getResource("/typeset/source-colors.json")).readText()).jsonObject.getValue("cases").jsonArray
        assertEquals(162,tracked.size)
        val extra=listOf(" RED ","#1234","transparent","rgb(255 0 0 / .5)","hsl(120 100% 50%)","rgb(\n1 2\n3 / .5\n)","hwb(2turn 10% 20% / .4)","RGB(1e2 2e1 3e0 / 25%)")
        val invalid=listOf("#12","not-a-color","rgb(1,2)","rgb(NaN 0 0)","hsl(0 50 50)","rgba(1,2,3,4,5)","rgb(1e999 0 0)","rgb(1 2 3 4)","rgb(1,2,3 / .5)","hwb(0,10%,20%)","color(display-p3 1 0 0)","oklch(50% .2 30)","x".repeat(257))
        fun outcome(parser:(String)->Color,text:String):Pair<Int?,Pair<String,String?>?> = try{parser(text).rgb to null}catch(e:IllegalArgumentException){null to (e.javaClass.name to e.message)}
        for(value in tracked.map{it.jsonObject.getValue("value").jsonPrimitive.content}+extra+invalid)
            assertEquals(outcome(RecordedTypesetSyntaxColors::parse,value),outcome(TypesetColors::parse,value),value)
        invalid.forEach{assertFailsWith<IllegalArgumentException>{TypesetColors.parse(it)}}
        assertEquals(Color(17,34,51,68).rgb,TypesetColors.parse("#1234").rgb)
        assertEquals(0,TypesetColors.parse("transparent").rgb)
        assertEquals(Color(255,0,0).rgb,TypesetColors.parse(" RED ").rgb)
        assertEquals(Color(255,0,0,128).rgb,TypesetColors.parse("rgb(255 0 0 / .5)").rgb)
        assertEquals(Color(0,255,0).rgb,TypesetColors.parse("hsl(120 100% 50%)").rgb)
    }
    @Test fun concurrentParserReuseKeepsIndependentMatchAndColorResults() {
        val inputs=listOf("rgba(240,12,30,.5)","hsl(210 50% 40% / 70%)","hwb(2turn 10% 20% / .4)","rgb(5e1 1e2 2e2 / .8)","#1234","rebeccapurple","transparent")
        val expected=inputs.associateWith{RecordedTypesetSyntaxColors.parse(it).rgb}
        val pool=java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val futures=(0..1).map{worker->pool.submit(java.util.concurrent.Callable {
                repeat(100){round->for(offset in inputs.indices){val value=inputs[(offset+round+worker)%inputs.size];assertEquals(expected.getValue(value),TypesetColors.parse(value).rgb)}}
                true
            })}
            futures.forEach{assertTrue(it.get(20,java.util.concurrent.TimeUnit.SECONDS))}
        } finally {pool.shutdown();assertTrue(pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS))}
    }
}
