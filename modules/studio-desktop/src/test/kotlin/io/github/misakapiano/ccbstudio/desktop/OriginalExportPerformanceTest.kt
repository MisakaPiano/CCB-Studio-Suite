package io.github.misakapiano.ccbstudio.desktop

import io.github.misakapiano.ccbstudio.assets.AssetStore
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.io.DataOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.CRC32
import kotlin.test.*

/** Frozen f5b008e LibraryDelivery algorithm, only c.workspace -> workspace and name changed. */
internal suspend fun legacyOriginalsExport(workspace:StudioWorkspace,ids:List<String>,parent:Path):Path {
    require(ids.isNotEmpty())
    val root=workspace.root.toAbsolutePath().normalize();val destination=parent.toAbsolutePath().normalize()
    require(!destination.startsWith(root)){"请选择资料库之外的导出目录"}
    val name="Studio-Originals-${newId()}";val stage=Files.createDirectory(destination.resolve(".$name.incomplete"))
    val files=ids.distinct().mapIndexed{i,id->
        currentCoroutineContext().ensureActive()
        val asset=workspace.assets.read(java.util.UUID.fromString(id)).require()
        val filename=asset.request().name().replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"),"_").takeLast(160).ifBlank{"image.png"}
        val file="${(i+1).toString().padStart(4,'0')}-$filename";val bytes=workspace.bytes(id)
        writeNewForced(stage.resolve(file),bytes)
        buildJsonObject{put("file",file);put("sourceId",id);put("sha256",sha256(bytes));put("bytes",bytes.size)}
    }
    currentCoroutineContext().ensureActive()
    writeNewForced(stage.resolve("COMPLETE.json"),buildJsonObject{put("version",1);put("kind","originals");put("files",JsonArray(files))}.toString().toByteArray())
    return Files.move(stage,destination.resolve(name),StandardCopyOption.ATOMIC_MOVE)
}

class OriginalExportPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(file:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    /** Valid PNG with a bounded private ancillary chunk, generated with 64KiB memory. */
    private fun original(parent:Path,payloadBytes:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("synthetic-$payloadBytes.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(payloadBytes)
            val type="paDd".toByteArray();out.write(type);val crc=CRC32();crc.update(type)
            val block=ByteArray(65536){(it*37+11).toByte()};var remaining=payloadBytes
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    private fun admit(w:StudioWorkspace,path:Path,name:String=path.fileName.toString()):String {
        val id=UUID.randomUUID()
        Files.newInputStream(path).use{w.assets.put(AssetStore.Request(id,name,AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
        return id.toString()
    }
    private data class Sample(val wallMs:Double,val cpuMs:Double,val allocatedBytes:Long,val output:Path)
    private suspend fun measure(action:suspend()->Path):Sample {
        val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        val id=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(id);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
        val output=action();check(Thread.currentThread().id==id){"Allocation observation must stay on the measured thread"}
        return Sample((System.nanoTime()-start)/1e6,(bean.currentThreadCpuTime-cpu)/1e6,bean.getThreadAllocatedBytes(id)-allocated,output)
    }
    private fun verify(output:Path,id:String,input:Path) {
        val receipt=Json.parseToJsonElement(Files.readString(output.resolve("COMPLETE.json"))).jsonObject
        val files=receipt.getValue("files").jsonArray;assertEquals(1,files.size)
        val file=files.single().jsonObject;assertEquals(id,file.getValue("sourceId").jsonPrimitive.content)
        val exported=output.resolve(file.getValue("file").jsonPrimitive.content)
        assertEquals(Files.size(input),file.getValue("bytes").jsonPrimitive.long)
        assertEquals(digest(input),file.getValue("sha256").jsonPrimitive.content);assertEquals(digest(input),digest(exported))
    }
    @Test fun baselineOriginalsExportAllocation():Unit=runBlocking {
        val base=root();val rows=mutableListOf<JsonObject>()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val input=original(base,size);val id=admit(w,input);val parent=Files.createDirectory(base.resolve("exports-$size"))
                legacyOriginalsExport(w,listOf(id),parent) // one unmeasured warm-up
                repeat(2){round->
                    val sample=measure{legacyOriginalsExport(w,listOf(id),parent)};verify(sample.output,id,input)
                    rows+=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",sample.wallMs);put("cpuMs",sample.cpuMs);put("allocatedBytes",sample.allocatedBytes)}
                }
                assertEquals(digest(input),w.assets.read(UUID.fromString(id)).require().sha256())
            }
        }
        println("ORIGINAL_EXPORT_BASELINE="+JsonArray(rows))
    }
}
