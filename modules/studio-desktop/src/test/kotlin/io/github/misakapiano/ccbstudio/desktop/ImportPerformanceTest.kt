package io.github.misakapiano.ccbstudio.desktop

import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class ImportPerformanceTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun twentyFourImportsIntoPopulatedLibraryRetainOriginalsAndReportElapsed()=runBlocking(awt) {
        val r=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"))
        val bytes=EditorRenderer.synthetic(480,640)
        repeat(80){w.importImage("existing-$it.png",bytes)}
        val inputs=Files.createDirectories(r.resolve("inputs"));val files=(1..24).map{Files.write(inputs.resolve("image-$it.png"),bytes)}
        val scope=CoroutineScope(SupervisorJob()+awt);val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        try {
            val start=System.nanoTime();c.importIngress(GalleryIngress(files=files));c.active!!.join();val ms=(System.nanoTime()-start)/1000000
            assertNull(c.problem,c.problem);assertEquals(24,c.gallery.imports.size);assertTrue(c.gallery.imports.all{it.assetId!=null})
            c.gallery.imports.forEach{assertContentEquals(bytes,w.bytes(it.assetId!!))}
            assertEquals(104,SharedAssets.images(w).size);assertEquals(24,c.library.repository.load().observations.size)
            assertEquals(3,w.catalog().count{it.request().project().decodeToString()==LibraryRepository.TAG})
            val out=Path.of("build/import-performance/measurement.json");Files.createDirectories(out.parent)
            Files.writeString(out,"""{"existingImages":80,"importedImages":24,"sourceBytesPerImage":${bytes.size},"elapsedMs":$ms,"phaseMs":${kotlinx.serialization.json.JsonObject(c.gallery.importTimings.mapValues{kotlinx.serialization.json.JsonPrimitive(it.value)})},"syntheticOnly":true,"realUserDirectoryRead":false}""")
            println("SYNTHETIC_IMPORT_ELAPSED_MS=$ms")
        }finally{c.close();scope.cancel()}
    }
}
