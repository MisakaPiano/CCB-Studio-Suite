package io.github.misakapiano.ccbstudio.core

import io.github.misakapiano.ccbstudio.assets.AssetStore
import kotlinx.serialization.json.*
import java.io.DataOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.CRC32
import kotlin.concurrent.thread
import kotlin.test.*

class TypesetOriginalPerformanceTest {
    private fun root()=Files.createDirectories(Path.of(System.getProperty("preview.test.root"),newId()))
    private fun digest(path:Path):String {
        val md=MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use{input->val block=ByteArray(65536);while(true){val n=input.read(block);if(n<0)break;md.update(block,0,n)}}
        return java.util.HexFormat.of().formatHex(md.digest())
    }
    private fun original(parent:Path,size:Int):Path {
        val png=EditorRenderer.synthetic(16,16);val path=parent.resolve("typeset-$size.png")
        DataOutputStream(Files.newOutputStream(path,StandardOpenOption.CREATE_NEW)).use{out->
            out.write(png,0,png.size-12);out.writeInt(size)
            val type="paDd".toByteArray();val crc=CRC32();crc.update(type);out.write(type)
            val block=ByteArray(65536){(it*37+11).toByte()};var remaining=size
            while(remaining>0){val n=minOf(remaining,block.size);out.write(block,0,n);crc.update(block,0,n);remaining-=n}
            out.writeInt(crc.value.toInt());out.write(png,png.size-12,12)
        }
        return path
    }
    @Test fun measuredOriginalBatchAllocation() {
        val base=root();val rows=mutableListOf<JsonObject>();val bean=ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        check(bean.isThreadAllocatedMemorySupported);bean.isThreadAllocatedMemoryEnabled=true
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(3*1024*1024,12*1024*1024)) {
                val input=original(base,size);val expected=digest(input);val id=UUID.randomUUID()
                Files.newInputStream(input).use{w.assets.put(AssetStore.Request(id,"source.png",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
                val repository=TypesetRepository(w);val project=repository.create("Synthetic originals",listOf(id.toString()));val document=repository.load(project)
                val parent=Files.createDirectory(base.resolve("exports-$size"));val request=TypesetExportRequest(document.pages.map{it.id},variant="original",includeRecipe=true)
                TypesetExport.create(w,project,document,parent,request)
                repeat(3){round->
                    val thread=Thread.currentThread().id;val allocated=bean.getThreadAllocatedBytes(thread);val cpu=bean.currentThreadCpuTime;val start=System.nanoTime()
                    val output=TypesetExport.create(w,project,document,parent,request)
                    val row=buildJsonObject{put("payloadBytes",size);put("round",round);put("wallMs",(System.nanoTime()-start)/1e6);put("cpuMs",(bean.currentThreadCpuTime-cpu)/1e6);put("allocatedBytes",bean.getThreadAllocatedBytes(thread)-allocated)}
                    val receipt=TypesetExport.verify(output);assertEquals(2,receipt.files.size)
                    assertEquals(expected,digest(output.resolve("001.png")));assertEquals(id.toString(),receipt.assetIds["001.png"])
                    assertEquals("original-byte-identical",receipt.metadataStatuses["001.png"])
                    assertEquals(document,repository.load(project));assertEquals(expected,w.assets.read(id).require().sha256())
                    assertTrue(row.getValue("allocatedBytes").jsonPrimitive.long<3L*1024*1024,"Original delivery must not buffer the 3/12MiB encoded input")
                    rows+=row
                }
            }
        }
        println("TYPESET_ORIGINAL_ALLOCATION="+JsonArray(rows))
    }

    @Test fun originalSignatureDispatchPreservesOpaqueBytesAndIgnoresMisleadingNames() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            // Format-signature fixtures exercise opaque original delivery, not raster decoding.
            val signatures=linkedMapOf("png" to byteArrayOf(-119,80,78,71,13,10,26,10),"jpg" to byteArrayOf(-1,-40,-1),"gif" to "GIF89a".toByteArray(),"bmp" to byteArrayOf(66,77),"webp" to ("RIFF".toByteArray()+byteArrayOf(0,0,0,0)+"WEBP".toByteArray()))
            for((extension,signature) in signatures) {
                val bytes=signature+ByteArray(512){(it*17).toByte()};val id=UUID.randomUUID()
                bytes.inputStream().use{w.assets.put(AssetStore.Request(id,"misleading.bin",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
                val repo=TypesetRepository(w);val p=repo.create("Signature $extension",listOf(id.toString()));val d=repo.load(p)
                val parent=Files.createDirectory(base.resolve(extension));val out=TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},variant="original",format="source"))
                val receipt=TypesetExport.verify(out);assertEquals("001.$extension",receipt.files.single().name)
                assertContentEquals(bytes,Files.readAllBytes(out.resolve("001.$extension")))
                assertEquals(id.toString(),receipt.assetIds["001.$extension"]);assertEquals(d,repo.load(p))
                assertFalse(Files.list(out).use{it.anyMatch{p->p.fileName.toString().endsWith(".pending")}})
            }
            val bad=UUID.randomUUID();"unsupported original".byteInputStream().use{w.assets.put(AssetStore.Request(bad,"fake.png",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
            val repo=TypesetRepository(w);val p=repo.create("Unsupported",listOf(bad.toString()));val d=repo.load(p);val parent=Files.createDirectory(base.resolve("unsupported"))
            assertEquals("EXPORT_INCOMPLETE",assertFailsWith<DataProblem>{TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},variant="original"))}.status)
            val stage=Files.list(parent).use{it.toList().single()};assertTrue(stage.fileName.toString().endsWith(".incomplete"));assertFalse(Files.exists(stage.resolve("COMPLETE.json")))
            assertEquals(sha256("unsupported original".toByteArray()),w.assets.read(bad).require().sha256())
        }
    }

    @Test fun existingOriginalFilenameCannotBeOverwrittenDuringStaging() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val bytes=EditorRenderer.synthetic(16,16);val id=w.importImage("source.png",bytes);val repo=TypesetRepository(w)
            val p=repo.create("Collision",listOf(id));val d=repo.load(p);val parent=Files.createDirectory(base.resolve("exports"))
            val failure=assertFailsWith<DataProblem>{TypesetExport.create(w,p,d,parent,TypesetExportRequest(d.pages.map{it.id},variant="original"),hook={stage->
                if(stage=="BEFORE_PAGE") {val root=Files.list(parent).use{it.toList().single()};Files.writeString(root.resolve("001.png"),"unknown-keep",StandardOpenOption.CREATE_NEW)}
            })}
            assertEquals("EXPORT_INCOMPLETE",failure.status)
            val stage=Files.list(parent).use{it.toList().single()};assertEquals("unknown-keep",Files.readString(stage.resolve("001.png")))
            assertFalse(Files.exists(stage.resolve("COMPLETE.json")));assertContentEquals(bytes,w.bytes(id));assertEquals(d,repo.load(p))
        }
    }

    @Test fun sharedCopyKeepsPrefixAndTailAcrossBufferBoundariesAndRefusesExistingTarget() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            for(size in listOf(0,1,8,12,13,262143,262144,262145)) {
                val bytes=ByteArray(size){(it*31).toByte()};val id=UUID.randomUUID()
                bytes.inputStream().use{w.assets.put(AssetStore.Request(id,"opaque.bin",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
                val target=base.resolve("copy-$size.bin");val result=copyVerifiedOriginal(w,id.toString(),target)
                assertEquals(size.toLong(),result.size);assertEquals(sha256(bytes),result.sha256);assertContentEquals(bytes.take(12).toByteArray(),result.prefix)
                assertContentEquals(bytes,Files.readAllBytes(target))
                assertFailsWith<FileAlreadyExistsException>{copyVerifiedOriginal(w,id.toString(),target)}
                assertContentEquals(bytes,Files.readAllBytes(target));assertEquals(sha256(bytes),w.assets.read(id).require().sha256())
            }
        }
    }

    @Test fun sharedCopyDoesNotHoldWorkspaceMonitorDuringBlockedOutput() {
        val base=root()
        StudioWorkspace.openStudio(base.resolve("workspace")).use{w->
            val bytes=ByteArray(256*1024){(it*13).toByte()};val id=UUID.randomUUID()
            bytes.inputStream().use{w.assets.put(AssetStore.Request(id,"opaque.bin",AssetStore.Kind.ORIGINAL,null,byteArrayOf(),byteArrayOf()),it)}
            val entered=CountDownLatch(1);val release=CountDownLatch(1);val stateRead=CountDownLatch(1)
            val calls=AtomicInteger();val problem=AtomicReference<Throwable?>();val target=base.resolve("blocked.bin")
            val writer=thread(name="Synthetic-shared-copy"){
                try{copyVerifiedOriginal(w,id.toString(),target){
                    if(calls.incrementAndGet()==2){entered.countDown();check(release.await(10,TimeUnit.SECONDS))}
                }}catch(error:Throwable){problem.set(error)}
            }
            var reader:Thread?=null
            try {
                assertTrue(entered.await(10,TimeUnit.SECONDS))
                reader=thread(name="Synthetic-workspace-read"){try{w.state}finally{stateRead.countDown()}}
                assertTrue(stateRead.await(2,TimeUnit.SECONDS),"A blocked output must not monopolize the workspace monitor")
            }finally{release.countDown();writer.join(10000);reader?.join(10000)}
            assertFalse(writer.isAlive);assertFalse(reader?.isAlive==true);assertNull(problem.get())
            assertEquals(sha256(bytes),digest(target));assertEquals(sha256(bytes),w.assets.read(id).require().sha256())
        }
    }
}
