package io.github.misakapiano.ccbstudio.core

import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import kotlin.math.*

/** Port of TypeSet 1.1 2583–3306. No browser renderer or network font dependency. */
object TypesetLayout {
    data class Token(val ch:String,val style:TypesetStyle,val display:String=ch,val rotate:Boolean=false,val forced:Boolean=false,val alnum:Boolean=false)
    data class Cell(val token:Token,val advance:Double,val cls:String="normal")
    data class Line(val cells:List<Cell>,val width:Double)
    data class Layout(val lines:List<Line>,val width:Double,val height:Double,val vertical:Boolean)
    private val frc=FontRenderContext(AffineTransform(),true,true)
    fun font(s:TypesetStyle,ch:String=""):Font {
        val name=when(s.fontFamily){"CCBRef"->"Microsoft YaHei";"CCBRefSerif"->"SimSun";else->s.fontFamily.split(',').first().trim().trim('"','\'')}
        val style=(if(s.fontWeight>=600)Font.BOLD else Font.PLAIN) or (if(s.italic)Font.ITALIC else Font.PLAIN)
        var f=(TypesetFonts.font(s.fontFamily)?.deriveFont(style,s.fontSize.toFloat())?:Font(name,style,1).deriveFont(s.fontSize.toFloat()))
        if(ch.isNotEmpty() && f.canDisplayUpTo(ch)>=0) {
            for(family in listOf("SimSun","Microsoft YaHei","Yu Gothic","Segoe UI Symbol","Dialog")) {
                val alternate=Font(family,style,1).deriveFont(s.fontSize.toFloat())
                if(alternate.canDisplayUpTo(ch)<0){f=alternate;break}
            }
        }
        return f
    }
    fun measure(text:String,s:TypesetStyle)=font(s,text).getStringBounds(text,frc).width
    private fun chars(text:String)=text.codePoints().toArray().map{String(Character.toChars(it))}
    fun tokens(text:String,base:TypesetStyle):List<Token> {
        require(text.length<=100000)
        val out=mutableListOf<Token>();val stack=mutableListOf(base);var i=0
        while(i<text.length) {
            if(text[i]=='[') {
                if(text.getOrNull(i+1)=='['){out+=Token("[",stack.last());i+=2;continue}
                val end=text.indexOf(']',i+1)
                if(end>i){val tag=text.substring(i+1,end).trim()
                    if(tag=="/"){if(stack.size>1)stack.removeAt(stack.lastIndex);i=end+1;continue}
                    patch(tag,stack.last())?.let{require(stack.size<1000){"Rich text nesting too deep"};stack+=it;i=end+1}
                    if(i==end+1)continue
                }
            }
            val cp=text.codePointAt(i);out+=Token(String(Character.toChars(cp)),stack.last());i+=Character.charCount(cp)
        }
        return out
    }
    private val richTagSyntax by lazy { Regex("^([a-zA-Z_]+)\\s*=\\s*(.+)$") }
    private fun patch(tag:String,s:TypesetStyle):TypesetStyle? {
        when(tag.lowercase()) {
            "b","bold"->return s.copy(fontWeight=700);"i","italic"->return s.copy(italic=true)
            "u","underline"->return s.copy(underline=true);"strike","del"->return s.copy(strike=true)
            "plain"->return s.copy(fontWeight=400,italic=false,underline=false,strike=false)
        }
        val m=richTagSyntax.matchEntire(tag)?:return null
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
    private val verticalPunctuation by lazy { Regex("\\p{P}") }
    private val rotationSymbols by lazy { chars("/\\／＼~～〜=＝+＋*＊&＆@＠#＃%％^＾<>＜＞|｜").toSet() }
    private fun presentation(ch:String)=ch.codePointAt(0).let{it in 0xfe10..0xfe19 || it in 0xfe30..0xfe4f}
    fun upright(ch:String)=ch.codePointAt(0).let{it in 0x3000..0x30ff || it in 0x31f0..0x31ff || it in 0x3400..0x9fff || it in 0xf900..0xfaff || it in 0xff01..0xff60 || it in 0xac00..0xd7af || it in 0xfe10..0xfe4f}
    fun verticalTokens(input:List<Token>):List<Token> {
        val out=mutableListOf<Token>();var doubleOpen=true;var singleOpen=true;var i=0
        while(i<input.size){val t=input[i];val ch=t.ch;val cp=ch.codePointAt(0)
            val next=when {
                ch=="\"" || ch=="＂"->t.copy(display=if(doubleOpen)"﹃"else"﹄",forced=true).also{doubleOpen=!doubleOpen}
                ch=="'" || ch=="＇"->if(input.getOrNull(i-1)?.ch?.codePointAt(0)?.let(Character::isLetterOrDigit)==true && input.getOrNull(i+1)?.ch?.codePointAt(0)?.let(Character::isLetterOrDigit)==true)t.copy(rotate=true)
                    else t.copy(display=if(singleOpen)"﹁"else"﹂",forced=true).also{singleOpen=!singleOpen}
                ch=="." && input.getOrNull(i+1)?.ch=="."->if(input.getOrNull(i+2)?.ch=="."){i+=2;t.copy(ch="…",display="︙",forced=true)}else{i++;t.copy(ch="‥",display="︰",forced=true)}
                presentation(ch)->t.copy(forced=true)
                force[ch]!=null->t.copy(display=force.getValue(ch),forced=true)
                cp in 48..57 || cp in 65..90 || cp in 97..122->t.copy(display=String(Character.toChars(cp+0xfee0)),forced=true,alnum=true)
                else->t.copy(rotate=ch !in uprightFallback && (verticalPunctuation.matches(ch)||ch in rotationSymbols))
            };out+=next;i++
        };return out
    }
    private fun cls(ch:String)=when(ch) {
        in chars("，,﹐、﹑。．.：:﹕；;﹔")->"small"
        in chars(")）｠}｝〕〙】》»〉›」』]］〛”‟’〞〟﹚﹜﹞")->"close"
        in chars("(（｟{｛〔〘【《«〈‹「『[［〚“„‘‚‛〝﹙﹛﹝\"＂'＇")->"open"
        in chars("…‥—―–")->"middle"
        in chars("！!﹗？?﹖")->"emph"
        else->"normal"
    }
    private fun reduction(prev:String?,next:String,t:TypesetText,step:Double):Double {
        if(prev==null || !t.punctuationOptimization)return 0.0
        val p=when(t.punctuationTightness){"compact"->doubleArrayOf(.20,.18,.28,.10,.10,.28,.22,.08);"loose"->doubleArrayOf(.08,.06,.12,.04,.04,.12,.10,.02);else->doubleArrayOf(.14,.12,.20,.07,.07,.20,.16,.05)}
        fun part(c:String,before:Boolean)=when(c){"small"->p[if(before)0 else 1];"close"->p[if(before)2 else 3];"open"->p[if(before)4 else 5];"middle"->p[6];"emph"->p[7];else->0.0}
        return min((part(prev,false)+part(next,true))*t.style.fontSize,step*.36)
    }
    fun layout(t:TypesetText):Layout {
        val paragraphs=mutableListOf(mutableListOf<Token>())
        tokens(t.text,t.style).forEach{if(it.ch=="\n")paragraphs+=mutableListOf<Token>() else paragraphs.last()+=it}
        val lines=mutableListOf<Line>();val step=max(1.0,t.style.fontSize*t.lineHeight+t.letterSpacing)
        for(para in paragraphs){var current=mutableListOf<Cell>();var used=0.0;var prev:String?=null
            for(token in if(t.vertical)verticalTokens(para)else para){val c=if(t.vertical && t.punctuationOptimization)cls(token.ch)else"normal"
                var advance=if(t.vertical)max(step*.64,step-reduction(prev,c,t,step))else measure(token.ch,token.style)
                val spacing=if(!t.vertical && current.isNotEmpty())t.letterSpacing else 0.0
                if(current.isNotEmpty() && used+spacing+advance>t.width){lines+=Line(current,used);current=mutableListOf();used=0.0;prev=null;if(t.vertical)advance=step}
                if(!t.vertical && current.isNotEmpty())used+=t.letterSpacing
                current+=Cell(token,advance,c);used+=advance;prev=c
            };lines+=Line(current,used)
        }
        val pitch=t.style.fontSize*t.lineHeight
        return if(t.vertical)Layout(lines,max(pitch,lines.size*pitch),t.width,true)else Layout(lines,t.width,max(pitch,lines.size*pitch),false)
    }
    data class StoryLayout(val width:Int,val height:Int,val imageWidth:Int,val imageHeight:Int,val lines:List<String>,val bodyHeight:Int,val numberGap:Int,val numberHeight:Int)
    fun story(imageWidth:Int,imageHeight:Int,caption:String,s:StoryStyle):StoryLayout {
        require(imageWidth>0 && imageHeight>0)
        val w=s.pageWidth;val content=max(100,w-2*s.pagePadding);val ih=max(1,floor(imageHeight.toDouble()*content/imageWidth+.5).toInt())
        val textWidth=max(60,content-2*s.textPadding);val style=TypesetStyle(fontFamily=s.fontFamily,fontSize=s.fontSize)
        val lines=mutableListOf<String>()
        caption.replace("\r","").split('\n').forEach{p->if(p.isEmpty())lines+=""else {
            var line="";for(ch in chars((if(s.firstIndent)"　　"else"")+p)){if(line.isNotEmpty() && measure(line+ch,style)>textWidth){lines+=line;line=ch}else line+=ch};lines+=line
        }}
        val body=max(s.textMinHeight,ceil(lines.size*s.fontSize*s.lineHeight+2*s.textPadding).toInt())
        val ng=if(s.showPageNumber)ceil(s.fontSize*.4).toInt()else 0;val nh=if(s.showPageNumber)ceil(s.fontSize*.95).toInt()else 0
        val h=2*s.pagePadding+ih+s.gap+body+ng+nh
        require(h in 1..100000 && w.toLong()*h<=40_000_000){"Story page exceeds safe raster size; split the caption or reduce page width"}
        return StoryLayout(w,h,content,ih,lines,body,ng,nh)
    }
}
