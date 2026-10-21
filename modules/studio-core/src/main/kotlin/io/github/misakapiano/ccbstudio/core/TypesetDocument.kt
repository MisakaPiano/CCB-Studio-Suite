package io.github.misakapiano.ccbstudio.core

import kotlinx.serialization.Serializable
import java.util.UUID

/** Native, versioned consumer of TypeSet 1.1 behavior. Coordinates are original-image pixels. */
@Serializable data class TypesetStyle(
    val fontFamily:String="Microsoft YaHei", val fontSize:Double=32.0, val fontWeight:Int=400,
    val italic:Boolean=false, val underline:Boolean=false, val strike:Boolean=false,
    val fill:String="#ffffff", val stroke:String="#000000", val strokeWidth:Double=0.0,
)
@Serializable data class TypesetText(
    val id:String=newId(), val text:String="", val x:Double=300.0, val y:Double=40.0,
    val width:Double=400.0, val style:TypesetStyle=TypesetStyle(), val vertical:Boolean=false,
    val align:String="center", val lineHeight:Double=1.2, val letterSpacing:Double=0.0,
    val punctuationOptimization:Boolean=true, val punctuationTightness:String="standard",
    val opacity:Double=1.0, val background:String="#000000", val backgroundOpacity:Double=0.0,
    val border:String="#ffffff", val borderWidth:Double=0.0, val borderOpacity:Double=1.0,
    val visible:Boolean=true,
)
@Serializable data class TypesetPoint(val x:Double,val y:Double)
@Serializable data class TypesetMask(
    val id:String=newId(), val shape:String="rect", val effect:String="solid",
    val x:Double=0.0,val y:Double=0.0,val width:Double=100.0,val height:Double=100.0,
    val points:List<TypesetPoint> = emptyList(), val size:Double=60.0,val strength:Int=16,
    val color:String="#000000",val opacity:Double=1.0,val visible:Boolean=true,
)
@Serializable data class StoryStyle(
    val pageWidth:Int=1200,val pagePadding:Int=48,val gap:Int=28,val textPadding:Int=30,
    val textMinHeight:Int=170,val fontFamily:String="SimSun",val fontSize:Double=36.0,
    val lineHeight:Double=1.55,val textAlign:String="left",val firstIndent:Boolean=true,
    val showPageNumber:Boolean=true,val pageBackground:String="#fffdf5",val textColor:String="#241f19",
    val borderColor:String="#655b4e",val pageBorder:Boolean=true,val imageBorder:Boolean=true,
)
@Serializable data class TypesetPage(
    val id:String=newId(),val assetId:String,val name:String,
    val texts:List<TypesetText> = emptyList(),val masks:List<TypesetMask> = emptyList(),
    val caption:String="",val story:StoryStyle=StoryStyle(),
)
@Serializable data class TypesetPreset(val id:String=newId(),val name:String,val text:TypesetText)
@Serializable data class TypesetDocument(
    val version:Int=1,val pages:List<TypesetPage> = emptyList(),val startNumber:Int=1,
    val presets:List<TypesetPreset> = emptyList(),val mode:String="editor",
) {
    fun validate() {
        if(version!=1)throw DataProblem("UNSUPPORTED_VERSION","Typeset document $version retained")
        require(pages.size<=1000 && pages.map{it.id}.distinct().size==pages.size)
        require(startNumber in 0..1000000 && mode in setOf("editor","storybook") && presets.size<=500)
        fun finite(vararg values:Double)=require(values.all{it.isFinite() && it in -100000.0..100000.0})
        fun text(t:TypesetText) {
            UUID.fromString(t.id);finite(t.x,t.y,t.width,t.lineHeight,t.letterSpacing,t.style.fontSize,t.style.strokeWidth,t.borderWidth)
            require(t.width>0 && t.style.fontSize in 6.0..1000.0 && t.lineHeight in .2..10.0 && t.style.strokeWidth in 0.0..100.0 && t.borderWidth in 0.0..100.0)
            require(t.text.length<=100000 && t.style.fontFamily.length in 1..256 && t.style.fontWeight in 100..900)
            require(t.align in setOf("left","center","right") && t.punctuationTightness in setOf("standard","compact","loose"))
            require(listOf(t.opacity,t.backgroundOpacity,t.borderOpacity).all{it.isFinite() && it in 0.0..1.0})
            listOf(t.style.fill,t.style.stroke,t.background,t.border).forEach(::validateColor)
        }
        pages.forEach {p->
            UUID.fromString(p.id);UUID.fromString(p.assetId);require(p.name.length<=512 && p.caption.length<=100000)
            require(p.texts.size<=500 && p.masks.size<=500 && (p.texts.map{it.id}+p.masks.map{it.id}).distinct().size==p.texts.size+p.masks.size)
            p.texts.forEach(::text)
            p.masks.forEach{m->UUID.fromString(m.id);finite(m.x,m.y,m.width,m.height,m.size,m.opacity)
                require(m.shape in setOf("rect","brush","lasso") && m.effect in setOf("solid","mosaic","blur","hatch"))
                require(m.width>0 && m.height>0 && m.size in 1.0..2000.0 && m.strength in 1..256 && m.opacity in 0.0..1.0)
                require(m.points.size<=10000 && (m.shape!="lasso" || m.points.size>=3) && (m.shape!="brush" || m.points.isNotEmpty()))
                m.points.forEach{finite(it.x,it.y)};validateColor(m.color)
            }
            val s=p.story;finite(s.fontSize,s.lineHeight)
            require(s.pageWidth in 200..8000 && s.pagePadding in 0..2000 && s.gap in 0..2000 && s.textPadding in 0..2000 && s.textMinHeight in 0..8000)
            require(s.fontSize in 6.0..500.0 && s.lineHeight in .2..10.0 && s.fontFamily.length in 1..256 && s.textAlign in setOf("left","center","right"))
            listOf(s.pageBackground,s.textColor,s.borderColor).forEach(::validateColor)
        }
        require(presets.map{it.id}.distinct().size==presets.size)
        presets.forEach{UUID.fromString(it.id);require(it.name.length in 1..100);text(it.text)}
    }
}
internal fun validateColor(value:String) {require(Regex("#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?").matches(value)){"Use an RGB hex color"}}

@Serializable data class TypesetReference(val chunks:List<String>,val bytes:Int,val sha256:String) {
    fun validate(){require(chunks.size in 1..4 && bytes in 1..16*1024*1024 && sha256.matches(Regex("[0-9a-f]{64}")));chunks.forEach{UUID.fromString(it)}}
}
@Serializable data class TypesetProject(
    val id:String=newId(),val name:String,val revision:Long=0,val current:TypesetReference,
    val undo:List<TypesetReference> = emptyList(),val redo:List<TypesetReference> = emptyList(),
) {
    fun validate(){UUID.fromString(id);require(name.length in 1..100 && revision>=0 && undo.size<=30 && redo.size<=30);(listOf(current)+undo+redo).forEach{it.validate()}}
}
