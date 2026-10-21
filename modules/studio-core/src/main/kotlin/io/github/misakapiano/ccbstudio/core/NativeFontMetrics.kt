package io.github.misakapiano.ccbstudio.core

import java.awt.Font
import java.io.RandomAccessFile
import java.nio.file.*
import java.util.Locale
import kotlin.math.floor

/** Canvas top baseline uses normalized OpenType typo ascent, not Java's Windows ascent.
 * Measured against the pinned source browser; font files remain installed OS resources.
 * Chromium: third_party/blink/renderer/core/html/canvas/text_metrics.cc GetFontBaseline. */
object NativeFontMetrics {
    private val ratios:Map<String,Double> by lazy {
        val result=mutableMapOf<String,Double>()
        val root=Path.of(System.getenv("WINDIR")?:"C:/Windows","Fonts")
        if(Files.isDirectory(root))Files.list(root).use{files->files.filter{it.fileName.toString().substringAfterLast('.').lowercase() in setOf("ttf","otf","ttc")}.forEach{path->
            runCatching{inspect(path).forEach{(name,ratio)->result.putIfAbsent(name.lowercase(Locale.ROOT),ratio)}}
        }}
        result
    }
    fun topBaseline(font:Font,overrideRatio:Double?=null):Double {
        val ratio=overrideRatio?:ratios[font.getFontName(Locale.ENGLISH).lowercase(Locale.ROOT)]?:ratios[font.getFamily(Locale.ENGLISH).lowercase(Locale.ROOT)]
        val raw=if(ratio!=null)font.size2D*ratio else {
            val metrics=font.getLineMetrics("国",java.awt.font.FontRenderContext(null,true,true))
            font.size2D*metrics.ascent/(metrics.ascent+metrics.descent).toDouble()
        }
        return floor(raw*64+.5)/64
    }
    internal fun inspect(path:Path):Map<String,Double> = RandomAccessFile(path.toFile(),"r").use{f->
        val size=f.length();require(size in 12..100_000_000)
        fun uint()=f.readInt().toLong() and 0xffffffffL
        fun at(offset:Long){require(offset in 0 until size);f.seek(offset)}
        val signature=f.readInt()
        val offsets=if(signature==0x74746366){f.readInt();val count=f.readInt();require(count in 1..64);List(count){uint()}}else listOf(0L)
        val result=mutableMapOf<String,Double>()
        for(offset in offsets){at(offset+4);val count=f.readUnsignedShort();require(count in 1..512);at(offset+12)
            val tables=mutableMapOf<String,Pair<Long,Long>>()
            repeat(count){val tag=ByteArray(4);f.readFully(tag);f.readInt();val start=uint();val length=uint();require(start+length<=size);tables[tag.toString(Charsets.US_ASCII)]=start to length}
            val os=tables["OS/2"]?:continue;val names=tables["name"]?:continue
            if(os.second<74 || names.second<6)continue
            at(os.first+68);val ascent=f.readShort().toInt();val descent=f.readShort().toInt();if(ascent<=0 || descent>0)continue
            val ratio=ascent.toDouble()/(ascent-descent)
            at(names.first+2);val records=f.readUnsignedShort();val strings=f.readUnsignedShort();require(records<=4096 && 6L+records*12<=names.second)
            repeat(records){i->at(names.first+6+i*12);val platform=f.readUnsignedShort();f.readUnsignedShort();f.readUnsignedShort();val name=f.readUnsignedShort();val length=f.readUnsignedShort();val start=f.readUnsignedShort()
                if(platform==3 && name in setOf(1,4,6,16) && length<=2048 && strings.toLong()+start+length<=names.second){at(names.first+strings+start);val bytes=ByteArray(length);f.readFully(bytes);val value=bytes.toString(Charsets.UTF_16BE);if(value.isNotBlank())result[value]=ratio}
            }
        };result
    }
}
