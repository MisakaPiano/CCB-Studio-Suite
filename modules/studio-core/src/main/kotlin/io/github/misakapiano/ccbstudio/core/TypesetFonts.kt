package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.Serializable
import java.awt.Font
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable data class TypesetFontResource(val id:String,val sourceId:String,val decodedId:String,val name:String,val sha256:String) {
    val alias get()="ccb-font:$id"
    fun validate(){UUID.fromString(id);UUID.fromString(sourceId);UUID.fromString(decodedId);require(name.length in 1..256 && sha256.matches(Regex("[0-9a-f]{64}")))}
}

/** Fonts stay in the Suite asset root, never installed into Windows or silently substituted. */
object TypesetFonts {
    const val TAG="CCB_FONT_RESOURCE_V1"
    private data class Loaded(val font:Font,val ratio:Double?)
    private val loaded=ConcurrentHashMap<String,Loaded>()
    fun font(alias:String):Font?=if(alias.startsWith("ccb-font:"))loaded[alias]?.font?:throw DataProblem("FONT_MISSING","Imported font is unavailable; retain the document and reimport its resource")else null
    fun baseline(alias:String,font:Font)=NativeFontMetrics.topBaseline(font,loaded[alias]?.ratio)
    fun import(workspace:StudioWorkspace,name:String,bytes:ByteArray):TypesetFontResource=workspace.operation {
        require(workspace.state.typesetFonts.size<64){"64 imported fonts already retained; use another workspace for additional fonts"}
        val decoded=WindowsFontDecoder.decode(bytes);val font=Font.createFont(Font.TRUETYPE_FONT,ByteArrayInputStream(decoded));require(font.numGlyphs>0)
        val hash=sha256(decoded);workspace.state.typesetFonts.firstOrNull{it.sha256==hash}?.let{activate(workspace,listOf(it.alias));return@operation it}
        fun put(data:ByteArray,parent:UUID?):String {val id=UUID.randomUUID();workspace.assets.put(AssetStore.Request(id,name.take(512),if(parent==null)AssetStore.Kind.ORIGINAL else AssetStore.Kind.DERIVED,parent,byteArrayOf(),TAG.toByteArray()),ByteArrayInputStream(data));workspace.assets.read(id).require();return id.toString()}
        val source=put(bytes,null);val target=if(decoded.contentEquals(bytes))source else put(decoded,UUID.fromString(source))
        val ref=TypesetFontResource(newId(),source,target,font.fontName,hash)
        workspace.save(workspace.state.copy(typesetFonts=workspace.state.typesetFonts+ref))
        loaded[ref.alias]=Loaded(font,ratio(decoded));ref
    }
    fun activate(workspace:StudioWorkspace,names:Collection<String>)=workspace.operation {
        names.filter{it.startsWith("ccb-font:")}.distinct().forEach{alias->
            val ref=workspace.state.typesetFonts.firstOrNull{it.alias==alias}?:throw DataProblem("FONT_MISSING","Font reference unavailable; document retained")
            // Revalidate the durable asset even if this process already rendered a cached font.
            val a=workspace.assets.read(UUID.fromString(ref.decodedId)).require()
            if(!a.request().project().contentEquals(TAG.toByteArray()) || a.sha256()!=ref.sha256)throw DataProblem("CORRUPT","Font resource identity mismatch; retained")
            if(loaded[alias]==null){val bytes=workspace.bytes(ref.decodedId);require(bytes.size<=64*1024*1024);loaded[alias]=Loaded(Font.createFont(Font.TRUETYPE_FONT,ByteArrayInputStream(bytes)),ratio(bytes))}
        }
    }
    fun documentFonts(document:TypesetDocument):List<String> = document.pages.flatMap{p->listOf(p.story.fontFamily)+p.texts.flatMap{t->listOf(t.style.fontFamily)+TypesetLayout.tokens(t.text,t.style).map{it.style.fontFamily}}}+document.presets.map{it.text.style.fontFamily}
    fun verifyRenderable(workspace:StudioWorkspace,document:TypesetDocument,variant:String="all") {
        val styles=mutableListOf<Pair<TypesetStyle,String>>()
        document.pages.forEach{p->
            if(document.mode=="storybook" || variant in setOf("all","text"))p.texts.filter{it.visible}.forEach{t->TypesetLayout.tokens(t.text,t.style).forEach{styles+=it.style to it.display}}
            if(document.mode=="storybook")styles+=TypesetStyle(fontFamily=p.story.fontFamily,fontSize=p.story.fontSize) to (p.caption+"0123456789")
        }
        activate(workspace,styles.map{it.first.fontFamily})
        val environment=java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
        val available=(environment.availableFontFamilyNames.toList()+environment.getAvailableFontFamilyNames(java.util.Locale.ENGLISH)).map{it.lowercase(java.util.Locale.ROOT)}.toSet()+setOf("dialog","dialoginput","serif","sansserif","monospaced")
        val missing=styles.map{it.first.fontFamily}.distinct().filter{family->
            !family.startsWith("ccb-font:") && when(family){"CCBRef"->"microsoft yahei";"CCBRefSerif"->"simsun";else->family.split(',').first().trim().trim('"','\'').lowercase()} !in available}
        if(missing.isNotEmpty())throw DataProblem("FONT_MISSING","请选择或导入可用字体：${missing.joinToString("、")}")
        if(styles.distinct().any{(style,value)->value.codePoints().toArray().any{cp->!Character.isWhitespace(cp) && TypesetLayout.font(style,String(Character.toChars(cp))).canDisplay(cp).not()}})
            throw DataProblem("GLYPH_MISSING","存在当前字体及回退字体无法显示的字符；请更换字体后重新预览")
    }
    internal fun forget(names:Collection<String>){names.forEach(loaded::remove)}
    private fun ratio(bytes:ByteArray):Double?=runCatching {
        val b=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);val n=b.getShort(4).toInt() and 65535;require(n in 1..512)
        for(i in 0 until n){val p=12+i*16;if(b.getInt(p)==0x4f532f32){val offset=b.getInt(p+8);val len=b.getInt(p+12);require(offset>=0 && len>=74 && offset.toLong()+len<=bytes.size)
            val a=b.getShort(offset+68).toInt();val d=b.getShort(offset+70).toInt();if(a>0 && d<=0)return@runCatching a.toDouble()/(a-d)}}
        null
    }.getOrNull()
}
