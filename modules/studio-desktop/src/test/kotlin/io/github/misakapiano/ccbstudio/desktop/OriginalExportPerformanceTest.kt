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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import kotlin.concurrent.thread
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

    @Test fun pairedOriginalExportPreservesBytesWithBoundedAllocation():Unit=runBlocking {
        val base=root();val rows=mutableListOf<JsonObject>()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val input=original(base,size);val id=admit(w,input);val parent=Files.createDirectory(base.resolve("paired-$size"))
                legacyOriginalsExport(w,listOf(id),parent);exportLibraryOriginals(w,listOf(id),parent)
                val allocations=mutableMapOf("legacy" to mutableListOf<Long>(),"streamed" to mutableListOf<Long>())
                repeat(4){round->
                    for(kind in if(round%2==0)listOf("legacy","streamed")else listOf("streamed","legacy")) {
                        val sample=measure{if(kind=="legacy")legacyOriginalsExport(w,listOf(id),parent)else exportLibraryOriginals(w,listOf(id),parent)}
                        verify(sample.output,id,input);allocations.getValue(kind)+=sample.allocatedBytes
                        rows+=buildJsonObject{put("payloadBytes",size);put("round",round);put("kind",kind);put("wallMs",sample.wallMs);put("cpuMs",sample.cpuMs);put("allocatedBytes",sample.allocatedBytes)}
                    }
                }
                // This guards removal of whole-file buffering, not a wall-clock speed target.
                assertTrue(allocations.getValue("streamed").max()<allocations.getValue("legacy").min()/4)
                assertEquals(digest(input),w.assets.read(UUID.fromString(id)).require().sha256())
            }
        }
        println("ORIGINAL_EXPORT_PAIRED="+JsonArray(rows))
    }

    @Test fun duplicateSelectionAndNamesProduceIdenticalReceiptsWithoutMovingOriginals():Unit=runBlocking {
        val base=root();val parent=Files.createDirectory(base.resolve("exports"));Files.writeString(parent.resolve("keep.txt"),"untouched")
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val input=original(base,2048);val a=admit(w,input,"one:?/bad.png");val b=admit(w,input,"second.png")
            val ids=listOf(b,a,b);val old=legacyOriginalsExport(w,ids,parent);val fresh=exportLibraryOriginals(w,ids,parent)
            assertEquals(Files.readString(old.resolve("COMPLETE.json")),Files.readString(fresh.resolve("COMPLETE.json")))
            val manifest=Json.parseToJsonElement(Files.readString(fresh.resolve("COMPLETE.json"))).jsonObject.getValue("files").jsonArray
            assertEquals(listOf(b,a),manifest.map{it.jsonObject.getValue("sourceId").jsonPrimitive.content})
            for(item in manifest){val name=item.jsonObject.getValue("file").jsonPrimitive.content;assertEquals(digest(input),digest(fresh.resolve(name)))}
            assertEquals("untouched",Files.readString(parent.resolve("keep.txt")))
            for(id in listOf(a,b))assertEquals(digest(input),w.assets.read(UUID.fromString(id)).require().sha256())
        }
    }

    @Test fun corruptLaterOriginalRetainsPartialBatchWithoutCompleteMarker():Unit=runBlocking {
        val base=root();val parent=Files.createDirectory(base.resolve("exports"))
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val first=original(base,1024);val second=original(base,2048);val a=admit(w,first);val b=admit(w,second)
            val record=w.assets.read(UUID.fromString(b)).require();val blob=w.root.resolve("assets/objects/${record.sha256()}.blob")
            java.nio.channels.FileChannel.open(blob,StandardOpenOption.WRITE).use{it.position(100);it.write(java.nio.ByteBuffer.wrap(byteArrayOf(42)))}
            val corrupt=digest(blob)
            assertFails{exportLibraryOriginals(w,listOf(a,b),parent)}
            val stage=Files.list(parent).use{it.toList().single()};assertTrue(stage.fileName.toString().endsWith(".incomplete"))
            assertFalse(Files.exists(stage.resolve("COMPLETE.json")))
            assertEquals(digest(first),digest(stage.resolve("0001-${first.fileName}")))
            assertEquals(corrupt,digest(blob));assertEquals(digest(first),w.assets.read(UUID.fromString(a)).require().sha256())
        }
    }

    @Test fun cancellationAfterOpeningOutputDoesNotPublishPartialBatch():Unit=runBlocking {
        val base=root();val parent=Files.createDirectory(base.resolve("exports"))
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val input=original(base,1024*1024);val id=admit(w,input)
            val locked=CountDownLatch(1);val release=CountDownLatch(1)
            val owner=thread(name="Synthetic-export-workspace-owner"){w.operation{locked.countDown();check(release.await(15,TimeUnit.SECONDS))}}
            assertTrue(locked.await(10,TimeUnit.SECONDS))
            val job=launch(Dispatchers.IO){exportLibraryOriginals(w,listOf(id),parent)}
            try {
                withTimeout(10000){while(Files.walk(parent).use{files->files.noneMatch{Files.isRegularFile(it)}})delay(5)}
                job.cancel();release.countDown();job.join();owner.join(10000);assertFalse(owner.isAlive)
                assertTrue(job.isCancelled)
                val stage=Files.list(parent).use{it.toList().single()};assertTrue(stage.fileName.toString().endsWith(".incomplete"))
                assertFalse(Files.exists(stage.resolve("COMPLETE.json")))
                val partial=Files.list(stage).use{it.toList().single()};assertEquals(0L,Files.size(partial))
                assertEquals(digest(input),w.assets.read(UUID.fromString(id)).require().sha256())
            }finally{release.countDown();job.cancelAndJoin();owner.join(10000)}
        }
    }

    @Test fun invalidDestinationAndAlreadyCancelledJobLeaveOriginalsUntouched():Unit=runBlocking {
        val base=root();val parent=Files.createDirectory(base.resolve("exports"))
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val input=original(base,1024);val id=admit(w,input)
            assertFailsWith<IllegalArgumentException>{exportLibraryOriginals(w,listOf(id),w.root)}
            val cancelled=Job().apply{cancel()}
            assertFailsWith<CancellationException>{withContext(cancelled){exportLibraryOriginals(w,listOf(id),parent)}}
            assertTrue(Files.list(parent).use{it.toList().isEmpty()})
            assertEquals(digest(input),w.assets.read(UUID.fromString(id)).require().sha256())
        }
    }
}
