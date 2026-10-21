package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32

/** TypeSet source text-chunk/alpha-stream preservation, with bounded lengths and CRC validation.
 * Payloads remain opaque: this adapter never decompresses or executes source metadata. */
object TypesetPngMetadata {
    data class Source(val chunks:List<ByteArray>,val alpha:ByteArray?,val alphaBits:Int)
    private val signature=byteArrayOf(137.toByte(),80,78,71,13,10,26,10)
    private fun walk(bytes:ByteArray,action:(String,Int,Int)->Unit):Boolean {
        if(bytes.size<8 || !bytes.copyOfRange(0,8).contentEquals(signature))return false
        var offset=8;var ended=false
        while(offset<bytes.size){require(offset.toLong()+12<=bytes.size){"Truncated PNG chunk"};val length=ByteBuffer.wrap(bytes,offset,4).int
            require(length>=0 && offset.toLong()+length+12<=bytes.size){"Invalid PNG chunk length"}
            val type=bytes.copyOfRange(offset+4,offset+8).toString(Charsets.US_ASCII);val end=offset+length+12
            val crc=CRC32().apply{update(bytes,offset+4,length+4)}.value;require(crc==(ByteBuffer.wrap(bytes,end-4,4).int.toLong() and 0xffffffffL)){"PNG CRC mismatch"}
            action(type,offset,end-offset);offset=end;if(type=="IEND"){ended=true;break}
        }
        require(ended){"PNG missing IEND"};return true
    }
    fun textChunks(original:ByteArray):List<ByteArray> {
        if(ImageFormats.format(original)!="png")return emptyList()
        val text=mutableListOf<ByteArray>();var totalBytes=0L;walk(original){type,start,size->if(type in setOf("tEXt","zTXt","iTXt")){totalBytes+=size;require(totalBytes<=8*1024*1024){"PNG source metadata exceeds 8 MiB"};text+=original.copyOfRange(start,start+size)}}
        return text
    }
    fun extract(original:ByteArray):Source {
        if(ImageFormats.format(original)!="png")return Source(emptyList(),null,0)
        val text=textChunks(original)
        val image=EditorRenderer.decode(original)
        try {
            fun byte(index:Int):Byte {var value=0;repeat(8){val bit=index*8+it;value=(value shl 1) or ((image.getRGB(bit/image.height,bit%image.height) ushr 24) and 1)};return value.toByte()}
            if(image.width.toLong()*image.height<152)return Source(text,null,0)
            val header=ByteArray(19){byte(it)};val sig=header.copyOfRange(0,15).toString(Charsets.US_ASCII)
            if(sig !in setOf("stealth_pnginfo","stealth_pngcomp"))return Source(text,null,0)
            val bits=ByteBuffer.wrap(header,15,4).int.toLong() and 0xffffffffL;val total=152+bits
            require(bits>0 && bits%8==0L && total<=image.width.toLong()*image.height && bits/8<=8*1024*1024){"Invalid alpha metadata length; choose privacy output to remove it"}
            return Source(text,ByteArray((total/8).toInt()){byte(it)},total.toInt())
        }finally{image.flush()}
    }
    fun injectAlpha(image:BufferedImage,source:Source) {
        val alpha=source.alpha?:return;require(source.alphaBits<=image.width.toLong()*image.height && image.colorModel.hasAlpha())
        repeat(source.alphaBits){bit->val x=bit/image.height;val y=bit%image.height;val value=image.getRGB(x,y);val lsb=(alpha[bit/8].toInt() ushr (7-bit%8)) and 1
            image.setRGB(x,y,(value and 0xfeffffff.toInt()) or (lsb shl 24))}
    }
    fun injectChunks(encoded:ByteArray,source:Source):ByteArray {
        if(source.chunks.isEmpty())return encoded
        return ByteArrayOutputStream().use{out->out.write(signature);require(walk(encoded){type,start,size->if(type=="IEND")source.chunks.forEach(out::write);out.write(encoded,start,size)});out.toByteArray()}
    }
}
