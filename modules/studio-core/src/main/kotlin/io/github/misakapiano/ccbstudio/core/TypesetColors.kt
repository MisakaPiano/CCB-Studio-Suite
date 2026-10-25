package io.github.misakapiano.ccbstudio.core

import java.awt.Color
import java.util.Locale
import kotlin.math.*
import kotlinx.serialization.json.*

/** sRGB colors used by TypeSet's controls and inline tags. Unsupported syntax is
 * explicit; the original text is retained instead of silently using a prior color. */
object TypesetColors {
    private val functionSyntax by lazy { Regex("([a-z]+)\\((.*)\\)",RegexOption.DOT_MATCHES_ALL) }
    private val whitespaceSyntax by lazy { Regex("\\s+") }
    private val numberSyntax by lazy { Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?") }
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
        val match=functionSyntax.matchEntire(value)
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
            parts=sides[0].trim().split(whitespaceSyntax);require(parts.size==3)
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
        require(numberSyntax.matches(value)){"颜色包含无效数值"}
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
