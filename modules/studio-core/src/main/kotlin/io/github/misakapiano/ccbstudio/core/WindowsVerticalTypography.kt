package io.github.misakapiano.ccbstudio.core

import com.sun.jna.*
import com.sun.jna.Function
import com.sun.jna.platform.win32.Guid
import com.sun.jna.ptr.*
import com.sun.jna.win32.StdCallLibrary
import java.awt.Shape
import java.awt.geom.*
import java.nio.file.Path
import java.util.Locale

/** DirectWrite vertical shaping, OpenType glyph selection and font metrics.
 * SDK ABI references are frozen in the local audit; no native binary/font installation.
 * Callback outlines are returned to the same Java2D preview/export painter. */
internal object WindowsVerticalTypography {
    private interface DWrite:StdCallLibrary {fun DWriteCreateFactory(type:Int,iid:Guid.GUID,result:PointerByReference):Int}
    @Structure.FieldOrder("start","length") class Range:Structure(),Structure.ByValue {@JvmField var start=0;@JvmField var length=0}
    data class Part(val outline:Shape,val style:TypesetStyle,val decoration:Boolean=false)
    data class Result(val width:Double,val height:Double,val parts:List<Part>,val orientations:Set<Int>,val glyphs:Int)
    private val cache=LinkedHashMap<TypesetText,Result>(16,.75f,true)
    private var cachedSegments=0
    private fun weight(result:Result):Int=result.parts.sumOf { part->
        val path=part.outline.getPathIterator(null);var count=0
        while(!path.isDone){count++;path.next()};count
    }
    fun layout(text:TypesetText):Result {
        val key=text.copy(id="",x=0.0,y=0.0,opacity=1.0,backgroundOpacity=0.0,borderOpacity=0.0)
        if(text.text.length<=4096)synchronized(cache){cache[key]?.let{return it}}
        val result=shape(text)
        if(text.text.length<=4096&&result.glyphs<=4096){val count=weight(result)
            if(count<=65536)synchronized(cache){
                cache.remove(key)?.let{cachedSegments-=weight(it)}
                while(cache.isNotEmpty()&&(cache.size>=12||cachedSegments+count>131072)){
                    val oldest=cache.entries.iterator();val entry=oldest.next();cachedSegments-=weight(entry.value);oldest.remove()
                };cache[key]=result;cachedSegments+=count
            }
        }
        return result
    }
    // One-way display mapping only; never reverse or rewrite existing corner quotes.
    internal fun verticalQuote(ch:String)=when(ch){"“"->"﹃";"”"->"﹄";"‘"->"﹁";"’"->"﹂";else->ch}
    private fun function(p:Pointer,slot:Int)=Function.getFunction(p.getPointer(0).getPointer(slot.toLong()*Native.POINTER_SIZE),Function.ALT_CONVENTION)
    private fun invoke(p:Pointer,slot:Int,vararg args:Any?):Int=function(p,slot).invokeInt(arrayOf(p,*args))
    private fun check(hr:Int){if(hr<0)throw DataProblem("VERTICAL_LAYOUT_FAILED","字体竖排未完成（HRESULT ${hr.toUInt().toString(16)}）；原工程保留")}
    private fun guid(value:String)=Guid.GUID("{$value}").also{it.write()}
    private val pointer=Pointer::class.java;private val integer=Int::class.javaPrimitiveType!!;private val float=Float::class.javaPrimitiveType!!
    /** Strong callback retention until every native consumer has been released. */
    private class Com(iids:Set<String>,methods:List<Pair<Array<Class<*>>, (Array<Any?>)->Any?>>):AutoCloseable {
        private val table=Memory((methods.size+3L)*Native.POINTER_SIZE);val value=Memory(Native.POINTER_SIZE.toLong())
        private val ids=(iids+"00000000-0000-0000-C000-000000000046").map{guid(it).pointer.getByteArray(0,16)}
        private val callbacks=mutableListOf<Callback>()
        init {
            val all=listOf<Pair<Array<Class<*>>,(Array<Any?>)->Any?>>(
                arrayOf<Class<*>>(pointer,pointer,pointer) to {a:Array<Any?>->val out=a[2] as Pointer;val match=ids.any{it.contentEquals((a[1] as Pointer).getByteArray(0,16))};out.setPointer(0,if(match)value else null);if(match)0 else 0x80004002.toInt()},
                arrayOf<Class<*>>(pointer) to {_:Array<Any?>->1},arrayOf<Class<*>>(pointer) to {_:Array<Any?>->1})+methods
            all.forEachIndexed{i,(types,action)->
                val callback=object:CallbackProxy,StdCallLibrary.StdCallCallback {
                    override fun getParameterTypes()=types
                    override fun getReturnType():Class<*> = integer
                    override fun callback(args:Array<Any?>):Any?=action(args)
                };callbacks+=callback;table.setPointer(i.toLong()*Native.POINTER_SIZE,CallbackReference.getFunctionPointer(callback))
            };value.setPointer(0,table)
        }
        override fun close(){value.close();table.close()}
    }
    private fun shape(t:TypesetText):Result {
        if(!System.getProperty("os.name").startsWith("Windows"))throw DataProblem("UNSUPPORTED_PLATFORM","字体竖排需要Windows DirectWrite")
        require(Native.POINTER_SIZE==8){"Font outline callback ABI requires Windows x64"}
        val library=Native.load(Path.of(System.getenv("WINDIR"),"System32","dwrite.dll").toString(),DWrite::class.java)
        val ref=PointerByReference();check(library.DWriteCreateFactory(1,guid("958DB99A-BE2A-4F09-AF7D-65189803D1D3"),ref));val factory=ref.value
        val owned=mutableListOf<Pointer>();val loaders=mutableListOf<Pointer>();val memory=mutableListOf<Memory>();val coms=mutableListOf<Com>()
        var callbackFailure:Throwable?=null
        fun obtain(p:Pointer,slot:Int,vararg args:Any?):Pointer {val out=PointerByReference();check(invoke(p,slot,*args,out));return requireNotNull(out.value).also{owned+=it}}
        fun range(start:Int,count:Int)=Range().also{it.start=start;it.length=count;it.write()}
        val families=mutableMapOf<String,Pair<String,Pointer>>()
        try {
            fun family(style:TypesetStyle):Pair<String,Pointer> = families.getOrPut(style.fontFamily){
                val f=TypesetLayout.font(style);val name=when(f.getFamily(Locale.ENGLISH)){"Dialog","DialogInput","SansSerif"->"Segoe UI";"Serif"->"Times New Roman";"Monospaced"->"Consolas";else->f.getFamily(Locale.ENGLISH)}
                val bytes=TypesetFonts.nativeBytes(style.fontFamily)
                val collection=if(bytes==null){val system=PointerByReference();check(invoke(factory,3,system,0));requireNotNull(system.value).also{owned+=it}}else{
                    val loaderRef=PointerByReference();check(invoke(factory,44,loaderRef));val loader=loaderRef.value;check(invoke(factory,13,loader));loaders+=loader
                    val buffer=Memory(bytes.size.toLong()).also{it.write(0,bytes,0,bytes.size);memory+=it}
                    val file=obtain(loader,4,factory,buffer,bytes.size,null)
                    val builder=obtain(factory,43);check(invoke(builder,7,file));val set=obtain(builder,6);obtain(factory,37,set)
                }
                val index=IntByReference();val exists=IntByReference();check(invoke(collection,5,WString(name),index,exists))
                if(exists.value==0)throw DataProblem("FONT_MISSING","字体竖排找不到所选字体 $name；没有静默替换")
                name to collection
            }
            val tokens=TypesetLayout.tokens(t.text,t.style).map{it.copy(ch=verticalQuote(it.ch))};val plain=tokens.joinToString(""){it.ch}
            val base=family(t.style)
            val format=obtain(factory,15,WString(base.first),base.second,t.style.fontWeight,if(t.style.italic)2 else 0,5,t.style.fontSize.toFloat(),WString("zh-CN"))
            check(invoke(format,6,2));check(invoke(format,7,3));check(invoke(format,3,when(t.align){"left"->0;"right"->1;else->2}))
            val layout=obtain(factory,18,WString(plain),plain.length,format,100000f,t.width.toFloat())
            val layout2=obtain(layout,0,guid("1093C18F-8D5E-43F0-B064-0917311B525E"));check(invoke(layout2,72,0))
            val effects=mutableMapOf<Long,TypesetStyle>();var position=0
            tokens.groupAdjacent().forEach{run->
                val style=run.first().style;val length=run.sumOf{it.ch.length};val area=range(position,length);val fam=family(style)
                check(invoke(layout,30,fam.second,area));check(invoke(layout,31,WString(fam.first),area))
                check(invoke(layout,32,style.fontWeight,area));check(invoke(layout,33,if(style.italic)2 else 0,area));check(invoke(layout,35,style.fontSize.toFloat(),area))
                check(invoke(layout,36,if(style.underline)1 else 0,area));check(invoke(layout,37,if(style.strike)1 else 0,area))
                val effect=Com(emptySet(),emptyList()).also{coms+=it};effects[Pointer.nativeValue(effect.value)]=style;check(invoke(layout,38,effect.value,area));position+=length
            }
            if(t.letterSpacing!=0.0)check(invoke(layout2,69,0f,t.letterSpacing.toFloat(),0f,range(0,plain.length)))
            // Preserve requested column pitch, using the engine's measured baseline.
            val count=IntByReference();invoke(layout,59,null,0,count);require(count.value in 0..100000)
            if(count.value>0)Memory(count.value*24L).use{lines->check(invoke(layout,59,lines,count.value,count));val natural=lines.getFloat(12).coerceAtLeast(1f);val pitch=(t.style.fontSize*t.lineHeight).toFloat();check(invoke(layout,10,1,pitch,lines.getFloat(16)*pitch/natural))}
            val metrics=Memory(36);memory+=metrics;check(invoke(layout,60,metrics));val left=metrics.getFloat(0);val width=metrics.getFloat(8).toDouble().coerceAtLeast(1.0)
            val analyzer=obtain(factory,21);val analyzer1=obtain(analyzer,0,guid("80DAD800-E21F-4E83-96CE-BFCCE500DB7C"))
            val parts=mutableListOf<Part>();val angles=mutableSetOf<Int>();var glyphs=0;var segments=0
            fun safely(body:()->Unit):Int=try{body();0}catch(e:Throwable){if(callbackFailure==null)callbackFailure=e;0x80004005.toInt()}
            fun transform(x:Float,y:Float,angle:Int,sideways:Int=0)=AffineTransform().also{result->
                result.translate((x-left).toDouble(),y.toDouble())
                Memory(24).use{matrix->check(invoke(analyzer1,13,angle,sideways,matrix));result.concatenate(AffineTransform(matrix.getFloat(0).toDouble(),matrix.getFloat(4).toDouble(),matrix.getFloat(8).toDouble(),matrix.getFloat(12).toDouble(),matrix.getFloat(16).toDouble(),matrix.getFloat(20).toDouble()))}
            }
            fun glyph(x:Float,y:Float,angle:Int,run:Pointer,effect:Pointer?) {
                val n=run.getInt(12);require(n in 0..100000&&glyphs+n<=100000);glyphs+=n;angles+=angle
                val path=Path2D.Double()
                val sink=Com(setOf("2cd9069e-12e2-11dc-9fed-001143a055f9"),listOf(
                    arrayOf<Class<*>>(pointer,integer) to {a:Array<Any?>->path.windingRule=if(a[1] as Int==0)Path2D.WIND_EVEN_ODD else Path2D.WIND_NON_ZERO;0},
                    arrayOf<Class<*>>(pointer,integer) to {_:Array<Any?>->0},
                    // Win64 passes this 8-byte POINT_2F aggregate in one integer register.
                    arrayOf<Class<*>>(pointer,Long::class.javaPrimitiveType!!,integer) to {a:Array<Any?>->safely{val bits=a[1] as Long;path.moveTo(Float.fromBits(bits.toInt()).toDouble(),Float.fromBits((bits ushr 32).toInt()).toDouble())}},
                    arrayOf<Class<*>>(pointer,pointer,integer) to {a:Array<Any?>->safely{val data=a[1] as Pointer;val count=a[2] as Int;require(count in 0..100000);segments+=count;require(segments<=524288);repeat(count){i->path.lineTo(data.getFloat(i*8L).toDouble(),data.getFloat(i*8L+4).toDouble())}}},
                    arrayOf<Class<*>>(pointer,pointer,integer) to {a:Array<Any?>->safely{val data=a[1] as Pointer;val count=a[2] as Int;require(count in 0..100000);segments+=count;require(segments<=524288);repeat(count){i->val o=i*24L;path.curveTo(data.getFloat(o).toDouble(),data.getFloat(o+4).toDouble(),data.getFloat(o+8).toDouble(),data.getFloat(o+12).toDouble(),data.getFloat(o+16).toDouble(),data.getFloat(o+20).toDouble())}}},
                    arrayOf<Class<*>>(pointer,integer) to {a:Array<Any?>->safely{if(a[1] as Int==1)path.closePath()}},
                    arrayOf<Class<*>>(pointer) to {_:Array<Any?>->if(callbackFailure==null)0 else 0x80004005.toInt()}
                ))
                try{check(invoke(run.getPointer(0),14,run.getFloat(8),run.getPointer(16),run.getPointer(24),run.getPointer(32),n,run.getInt(40),run.getInt(44)and 1,sink.value))
                    parts+=Part(transform(x,y,angle,run.getInt(40)).createTransformedShape(path),effects[Pointer.nativeValue(effect)]?:t.style)
                }finally{sink.close()}
            }
            fun decoration(x:Float,y:Float,angle:Int,info:Pointer,effect:Pointer?){val style=effects[Pointer.nativeValue(effect)]?:t.style
                parts+=Part(transform(x,y,angle).createTransformedShape(Rectangle2D.Float(0f,info.getFloat(8),info.getFloat(0),info.getFloat(4))),style,true)}
            val renderer=Com(setOf("EF8A8135-5CC6-45FE-8825-C5A0724EB819","D3E0E934-22A0-427E-AAE4-7D9574B59DB1","eaf3a2da-ecf4-4d24-b644-b34f6842024b"),listOf(
                arrayOf<Class<*>>(pointer,pointer,pointer) to {a:Array<Any?>->(a[2] as Pointer).setInt(0,1);0},
                arrayOf<Class<*>>(pointer,pointer,pointer) to {a:Array<Any?>->val m=a[2] as Pointer;for(i in 0..5)m.setFloat(i*4L,if(i==0||i==3)1f else 0f);0},
                arrayOf<Class<*>>(pointer,pointer,pointer) to {a:Array<Any?>->(a[2] as Pointer).setFloat(0,1f);0},
                arrayOf<Class<*>>(pointer,pointer,float,float,integer,pointer,pointer,pointer) to {a:Array<Any?>->safely{glyph(a[2] as Float,a[3] as Float,0,a[5] as Pointer,a[7] as Pointer?)}},
                arrayOf<Class<*>>(pointer,pointer,float,float,pointer,pointer) to {a:Array<Any?>->safely{decoration(a[2] as Float,a[3] as Float,0,a[4] as Pointer,a[5] as Pointer?)}},
                arrayOf<Class<*>>(pointer,pointer,float,float,pointer,pointer) to {a:Array<Any?>->safely{decoration(a[2] as Float,a[3] as Float,0,a[4] as Pointer,a[5] as Pointer?)}},
                arrayOf<Class<*>>(pointer,pointer,float,float,pointer,integer,integer,pointer) to {_:Array<Any?>->0x80004001.toInt()},
                arrayOf<Class<*>>(pointer,pointer,float,float,integer,integer,pointer,pointer,pointer) to {a:Array<Any?>->safely{glyph(a[2] as Float,a[3] as Float,a[4] as Int,a[6] as Pointer,a[8] as Pointer?)}},
                arrayOf<Class<*>>(pointer,pointer,float,float,integer,pointer,pointer) to {a:Array<Any?>->safely{decoration(a[2] as Float,a[3] as Float,a[4] as Int,a[5] as Pointer,a[6] as Pointer?)}},
                arrayOf<Class<*>>(pointer,pointer,float,float,integer,pointer,pointer) to {a:Array<Any?>->safely{decoration(a[2] as Float,a[3] as Float,a[4] as Int,a[5] as Pointer,a[6] as Pointer?)}},
                arrayOf<Class<*>>(pointer,pointer,float,float,integer,pointer,integer,integer,pointer) to {_:Array<Any?>->0x80004001.toInt()}
            )).also{coms+=it}
            check(invoke(layout,58,null,renderer.value,0f,0f));callbackFailure?.let{throw DataProblem("VERTICAL_LAYOUT_FAILED","字体竖排轮廓未完成",it)}
            return Result(width,t.width,parts,angles,glyphs)
        }finally{
            owned.asReversed().forEach{invoke(it,2)}
            loaders.asReversed().forEach{invoke(factory,14,it);invoke(it,2)}
            coms.asReversed().forEach{it.close()};memory.asReversed().forEach{it.close()};invoke(factory,2)
        }
    }
    private fun List<TypesetLayout.Token>.groupAdjacent():List<List<TypesetLayout.Token>> {
        val out=mutableListOf<MutableList<TypesetLayout.Token>>()
        for(t in this){if(out.lastOrNull()?.lastOrNull()?.style!=t.style)out+=mutableListOf<TypesetLayout.Token>();out.last()+=t};return out
    }
}
