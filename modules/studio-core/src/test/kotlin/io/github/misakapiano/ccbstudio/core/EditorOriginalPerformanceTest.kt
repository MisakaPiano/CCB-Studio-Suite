package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.io.DataOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import kotlin.test.*

/** Calls the existing edit-project original export, not TypeSet or a replacement benchmark. */
class EditorOriginalPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(path:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun original(parent:Path,size:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("editor-$size.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(size)
            val type="paDd".toByteArray();val crc=CRC32();crc.update(type);out.write(type)
            val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    private fun admit(w:StudioWorkspace,input:Path):String {
        val id=UUID.randomUUID()
        Files.newInputStream(input).use{w.assets.put(AssetStore.Request(id,input.fileName.toString(),AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
        return id.toString()
    }
    @Test fun measuredOriginalEditorExportAllocation() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val input=original(base,size);val expected=digest(input);val id=admit(w,input);val project=w.newProject("Synthetic original $size",listOf(id));val state=w.state
                w.export(project,listOf(id),"ORIGINAL")
                repeat(3){round->
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val output=w.export(project,listOf(id),"ORIGINAL")
                    val row=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                    val name="001-$id.png";assertEquals(expected,digest(output.resolve(name)))
                    assertEquals("CCB Studio Suite ORIGINAL\n$name ${Files.size(input)} $expected",Files.readString(output.resolve("COMPLETE.txt")))
                    assertEquals(state,w.state);assertEquals(expected,w.assets.read(UUID.fromString(id)).require().sha256())
                    assertFalse(Files.list(output).use{it.anyMatch{p->p.fileName.toString().endsWith(".pending")}})
                    assertTrue(row.getValue("allocatedBytes").jsonPrimitive.long<3L*1024*1024,"Original export must not buffer the entire encoded source")
                    rows+=row
                }
            }
        }
        println("EDITOR_ORIGINAL_ALLOCATION="+JsonArray(rows))
    }
    @Test fun originalNamingOrderDuplicatesLayersAndReceiptRemainCompatible() {
        val base=root();val workspace=base.resolve("workspace");var saved:StudioState?=null
        StudioWorkspace.openStudio(workspace).use{w->
            // Opaque header samples preserve this older entry's first-byte naming rule;
            // they do not certify image decoding or add new export formats.
            val inputs=listOf(EditorRenderer.synthetic(16,16),byteArrayOf(-1,-40,-1)+ByteArray(127){it.toByte()},"RIFF____WEBP".toByteArray()+ByteArray(127){(it*3).toByte()})
            val ids=inputs.mapIndexed{index,bytes->val id=UUID.randomUUID();bytes.inputStream().use{w.assets.put(AssetStore.Request(id,"misleading-$index.bin",AssetStore.Kind.ORIGINAL,null,"synthetic-recipe".toByteArray(),byteArrayOf()),it)};id.toString()}
            val project=w.newProject("Synthetic originals",ids)
            w.applyLayers(project,ids.toSet(),listOf(Layer(kind="TEXT",font="__missing_fixture_font__",text="Original must not render layers")))
            val before=w.state;val count=w.catalog().size;val selected=listOf(ids[1],ids[0],ids[1],ids[2]);val output=w.export(project,selected,"ORIGINAL")
            val expected=selected.mapIndexed{index,id->
                val bytes=inputs[ids.indexOf(id)];val name="%03d-%s.%s".format(index+1,id,if(bytes[0].toInt() and 255==255)"jpg"else"png")
                assertContentEquals(bytes,Files.readAllBytes(output.resolve(name)));"$name ${bytes.size} ${sha256(bytes)}"
            }
            assertEquals("CCB Studio Suite ORIGINAL\n"+expected.joinToString("\n"),Files.readString(output.resolve("COMPLETE.txt")))
            assertEquals(expected.size+1,Files.list(output).use{it.count()}.toInt())
            assertEquals(count,w.catalog().size);assertEquals(before,w.state);saved=before
            ids.forEachIndexed{index,id->assertEquals(sha256(inputs[index]),w.assets.read(UUID.fromString(id)).require().sha256())}
        }
        StudioWorkspace.openStudio(workspace).use{assertEquals(saved,it.state)}
    }

    @Test fun laterCorruptSourceRetainsEarlierOutputWithoutCompletionOrSourceReplacement() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val first=original(base,1024);val second=original(base,2048);val a=admit(w,first);val b=admit(w,second);val project=w.newProject("Synthetic fault",listOf(a,b));val before=w.state
            val record=w.assets.read(UUID.fromString(b)).require();val blob=w.root.resolve("assets/objects/${record.sha256()}.blob")
            val changed=Files.readAllBytes(blob);changed[100]=(changed[100].toInt() xor 1).toByte();Files.write(blob,changed,StandardOpenOption.TRUNCATE_EXISTING);val corrupt=digest(blob)
            assertFails{w.export(project,listOf(a,b),"ORIGINAL")}
            val output=Files.list(w.root.resolve("exports")).use{it.toList().single()}
            assertFalse(Files.exists(output.resolve("COMPLETE.txt")));assertEquals(digest(first),digest(output.resolve("001-$a.png")))
            assertEquals(corrupt,digest(blob));assertEquals(digest(first),w.assets.read(UUID.fromString(a)).require().sha256());assertEquals(before,w.state)
        }
    }

    @Test fun invalidExportSelectionDoesNotTouchExistingUnknownContentOrProject() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val input=original(base,1024);val id=admit(w,input);val project=w.newProject("Synthetic invalid",listOf(id));val before=w.state
            val exports=Files.createDirectory(w.root.resolve("exports"));val sentinel=exports.resolve("keep.txt");Files.writeString(sentinel,"unknown content retained",StandardOpenOption.CREATE_NEW)
            assertFails{w.export(project,listOf(id),"FUTURE")};assertFails{w.export(project,emptyList(),"ORIGINAL")};assertFails{w.export(project,listOf(UUID.randomUUID().toString()),"ORIGINAL")}
            assertEquals(listOf(sentinel),Files.list(exports).use{it.toList()});assertEquals("unknown content retained",Files.readString(sentinel))
            assertEquals(before,w.state);assertEquals(digest(input),w.assets.read(UUID.fromString(id)).require().sha256())
        }
    }

}
