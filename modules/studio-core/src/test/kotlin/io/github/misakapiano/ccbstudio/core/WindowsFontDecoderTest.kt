package io.github.misakapiano.ccbstudio.core

import java.awt.Font
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import java.nio.file.Path
import kotlin.test.*

class WindowsFontDecoderTest {
    private fun fixture():ByteArray=javaClass.getResourceAsStream("/fonts/MicrosoftPackedFont.hex")!!.use{it.readAllBytes().decodeToString().trim().chunked(2).map{it.toInt(16).toByte()}.toByteArray()}
    @Test fun nativeWoff2AndWoffDecodeToUsableFonts() {
        val original=fixture();val ttf=WindowsFontDecoder.decode(original)
        assertEquals(0x00010000,ByteBuffer.wrap(ttf).int);val font=Font.createFont(Font.TRUETYPE_FONT,ByteArrayInputStream(ttf));assertTrue(font.numGlyphs>0)
        val woff=woff(ttf);val decoded=WindowsFontDecoder.decode(woff);val second=Font.createFont(Font.TRUETYPE_FONT,ByteArrayInputStream(decoded))
        assertEquals(font.numGlyphs,second.numGlyphs);assertEquals(font.family,second.family)
        assertContentEquals(ttf,WindowsFontDecoder.decode(ttf))
    }
    @Test fun rejectsMalformedContainersAndOversizedHeaders() {
        val data=fixture();assertFails{WindowsFontDecoder.decode(data.copyOf(40))}
        val bad=data.copyOf();ByteBuffer.wrap(bad).putInt(16,Int.MAX_VALUE);assertFails{WindowsFontDecoder.decode(bad)}
        val invalid=data.copyOf();invalid.fill(0,70);assertFails{WindowsFontDecoder.decode(invalid)}
    }
    @Test fun installedOtfAndDecodedTtfImportRetainExactBytes() {
        val otf=java.nio.file.Files.readAllBytes(Path.of(System.getenv("WINDIR"),"Fonts","SourceSansPro-Regular.otf"))
        assertEquals(0x4f54544f,ByteBuffer.wrap(otf).int)
        val ttf=WindowsFontDecoder.decode(fixture())
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId())
        StudioWorkspace.openStudio(root).use{w->for((name,bytes) in listOf("fixture.otf" to otf,"fixture.ttf" to ttf)){
            val ref=TypesetFonts.import(w,name,bytes);assertContentEquals(bytes,w.bytes(ref.sourceId));assertEquals(ref.sourceId,ref.decodedId);assertNotNull(TypesetFonts.font(ref.alias))
        }}
        java.nio.file.Files.writeString(root.parent.resolve("otf-fixture-identity.json"),"{\"source\":\"Windows/Fonts/SourceSansPro-Regular.otf\",\"bytes\":${otf.size},\"sha256\":\"${sha256(otf)}\",\"redistribution\":\"NOT COPIED\"}")
    }
    @Test fun importedFontIsDurableAndBackupRestorableWithoutSystemInstallation() {
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());var alias="";var source=""
        val backup=StudioWorkspace.openStudio(root).use{w->val ref=TypesetFonts.import(w,"fixture.woff2",fixture());alias=ref.alias;source=ref.sourceId
            assertContentEquals(fixture(),w.bytes(source));assertNotNull(TypesetFonts.font(alias));w.backup()}
        TypesetFonts.forget(listOf(alias));assertFailsWith<DataProblem>{TypesetFonts.font(alias)}
        val dest=root.parent.resolve(newId());StudioRestore.restore(backup,dest)
        StudioWorkspace.openStudio(dest).use{w->TypesetFonts.activate(w,listOf(alias));assertNotNull(TypesetFonts.font(alias));assertContentEquals(fixture(),w.bytes(source))}
        TypesetFonts.forget(listOf(alias))
    }
    private fun woff(ttf:ByteArray):ByteArray {
        val b=ByteBuffer.wrap(ttf).order(ByteOrder.BIG_ENDIAN);val n=b.getShort(4).toInt() and 65535
        data class Table(val tag:Int,val checksum:Int,val raw:ByteArray,val compressed:ByteArray)
        val tables=(0 until n).map{i->val pos=12+i*16;val start=b.getInt(pos+8);val len=b.getInt(pos+12);val raw=ttf.copyOfRange(start,start+len)
            val d=Deflater();val compressed=try{d.setInput(raw);d.finish();val out=ByteArray(raw.size+100);out.copyOf(d.deflate(out))}finally{d.end()}
            Table(b.getInt(pos),b.getInt(pos+4),raw,if(compressed.size<raw.size)compressed else raw)}
        val size=44+n*20+tables.sumOf{(it.compressed.size+3)/4*4};val out=ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)
        out.putInt(0x774f4646);out.putInt(b.getInt(0));out.putInt(size);out.putShort(n.toShort());out.putShort(0);out.putInt(ttf.size);out.putShort(1);out.putShort(0);repeat(5){out.putInt(0)}
        var offset=44+n*20
        tables.forEach{t->out.putInt(t.tag);out.putInt(offset);out.putInt(t.compressed.size);out.putInt(t.raw.size);out.putInt(t.checksum);System.arraycopy(t.compressed,0,out.array(),offset,t.compressed.size);offset+=(t.compressed.size+3)/4*4}
        return out.array()
    }
}
