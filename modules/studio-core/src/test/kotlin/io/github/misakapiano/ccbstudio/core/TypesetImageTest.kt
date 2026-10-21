package io.github.misakapiano.ccbstudio.core

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*

class TypesetImageTest {
    private fun jpeg(orientation:Int,little:Boolean=true):ByteArray {
        val image=BufferedImage(7,5,BufferedImage.TYPE_INT_RGB)
        for(y in 0 until 5)for(x in 0 until 7)image.setRGB(x,y,((x*33) shl 16) or ((y*49) shl 8) or (x+y)*17)
        val raw=ByteArrayOutputStream().also{check(ImageIO.write(image,"jpeg",it))}.toByteArray();image.flush()
        val tiff=ByteBuffer.allocate(26).order(if(little)ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        tiff.put(if(little)byteArrayOf(73,73)else byteArrayOf(77,77));tiff.putShort(42);tiff.putInt(8);tiff.putShort(1)
        tiff.putShort(0x112);tiff.putShort(3);tiff.putInt(1);tiff.putShort(orientation.toShort());tiff.putShort(0);tiff.putInt(0)
        val exif="Exif\u0000\u0000".toByteArray()+tiff.array();val size=exif.size+2
        return raw.copyOfRange(0,2)+byteArrayOf(0xff.toByte(),0xe1.toByte(),(size ushr 8).toByte(),size.toByte())+exif+raw.copyOfRange(2,raw.size)
    }
    @Test fun allEightOrientationsBothEndianOrdersMatchPixelCoordinates() {
        for(little in listOf(true,false))for(o in 1..8){val bytes=jpeg(o,little);val original=bytes.copyOf();val raw=EditorRenderer.decode(bytes);val image=TypesetImage.decode(bytes,TypesetImage.DISPLAY)
            assertEquals(if(o>=5)5 else 7,image.width);assertEquals(if(o>=5)7 else 5,image.height)
            for(y in 0 until 5)for(x in 0 until 7){val point=when(o){2->6-x to y;3->6-x to 4-y;4->x to 4-y;5->y to x;6->4-y to x;7->4-y to 6-x;8->y to 6-x;else->x to y}
                assertEquals(raw.getRGB(x,y),image.getRGB(point.first,point.second),"orientation $o at $x,$y")}
            assertContentEquals(original,bytes);image.flush();raw.flush()
        }
    }
    @Test fun oldSerializedPagesKeepRawCoordinatesAndNewPagesSurviveReopen() {
        val root=Path.of(System.getProperty("preview.test.root")).resolve(newId());val bytes=jpeg(6);var projectId=""
        StudioWorkspace.openStudio(root).use{w->val id=w.importImage("oriented.jpg",bytes);val repo=TypesetRepository(w);val p=repo.create("orientation",listOf(id));projectId=p.id
            val page=repo.load(p).pages.single();assertEquals(TypesetImage.DISPLAY,page.coordinates)
            val legacy=documentJson.decodeFromString<TypesetPage>("""{"assetId":"$id","name":"old"}""")
            assertEquals(TypesetImage.RAW,legacy.coordinates)
            val old=TypesetRenderer.render(bytes,legacy);assertEquals(7,old.width);assertEquals(5,old.height);old.flush()
            val updated=page.copy(masks=listOf(TypesetMask(effect="black",x=0.0,y=0.0,width=1.0,height=1.0)))
            repo.update(p.id,p.revision){it.copy(pages=listOf(updated))};assertContentEquals(bytes,w.bytes(id))
        }
        StudioWorkspace.openStudio(root).use{w->val p=w.state.typesetProjects.single{it.id==projectId};val page=TypesetRepository(w).load(p).pages.single()
            val image=TypesetRenderer.render(w.bytes(page.assetId),page);assertEquals(5,image.width);assertEquals(7,image.height);assertEquals(0xff000000.toInt(),image.getRGB(0,0));image.flush()
            assertContentEquals(bytes,w.bytes(page.assetId))
        }
    }
    @Test fun unknownCoordinatesAndMalformedExifFailWithoutChangingLegacyInterpretation() {
        val bytes=jpeg(9);assertFails{TypesetImage.decode(bytes,TypesetImage.DISPLAY)}
        val raw=TypesetImage.decode(bytes,TypesetImage.RAW);assertEquals(7,raw.width);raw.flush()
        assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{TypesetImage.decode(jpeg(1),"future")}.status)
        assertEquals("UNSUPPORTED_VERSION",assertFailsWith<DataProblem>{TypesetDocument(pages=listOf(TypesetPage(assetId=newId(),name="future",coordinates="future"))).validate()}.status)
    }
}
