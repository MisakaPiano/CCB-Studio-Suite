@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

/** Offscreen production composition: timing baseline, never native-window or human acceptance. */
class GallerySustainedResourceTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun repeatedPagesDrainTasksAndRecordColdWarmResources():Unit=runBlocking(awt) {
        val root=Path.of(System.getProperty("preview.test.root"),newId());val data=root.resolve("workspace")
        StudioWorkspace.openStudio(data).use{w->val bytes=EditorRenderer.synthetic(128,160);repeat(48){w.importImage("synthetic-$it.png",bytes)};LibraryRepository(w).pauseCollections(true)}
        val measurements=mutableListOf<JsonObject>();val process=ProcessHandle.current()
        fun cpu()=process.info().totalCpuDuration().orElse(java.time.Duration.ZERO).toNanos()
        suspend fun measure(name:String,body:suspend ()->Unit){val start=System.nanoTime();val before=cpu();body();measurements+=buildJsonObject{put("phase",name);put("wallMs",(System.nanoTime()-start)/1e6);put("processCpuMs",(cpu()-before)/1e6);put("heapUsedBytes",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used);put("liveThreads",ManagementFactory.getThreadMXBean().threadCount)}}
        val scope=CoroutineScope(SupervisorJob()+awt);lateinit var c:WorkbenchController
        measure("workspace-controller-open"){val w=withContext(Dispatchers.IO){StudioWorkspace.openStudio(data)};c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("budget"),admissionOverride=RequestAdmission{error("No provider calls")},pauseCollectionsOnStart=true)}
        c.view="图库"
        val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
        suspend fun frame(){scene.render(System.nanoTime()).close();yield()}
        suspend fun settled(){withTimeout(20000){do{frame();delay(20)}while(c.gallery.historyCache==null||c.gallery.foldPreferences==null||c.thumbnails.pendingCount>0)};frame()}
        try {
            measure("first-gallery-ready"){settled()}
            measure("idle-gallery-3-seconds"){delay(3000)}
            repeat(60){cycle->measure("page-cycle-$cycle"){c.view="创作";frame();c.view="图库";settled();delay(1000)}}
            assertTrue(c.thumbnails.retainedBytes<=8L*1024*1024);assertEquals(0,c.thumbnails.pendingCount)
            measure("maintenance-drain"){c.enterMaintenance()};assertEquals(0,c.thumbnails.pendingCount);assertEquals(0,c.thumbnails.retainedBytes)
            c.resumeAfterPreflightFailure();measure("resume-gallery"){settled()}
            measure("post-cycles-explicit-gc"){System.gc();delay(500)}
            assertNull(c.problem,c.problem)
        }finally{scene.close();measure("close-drain"){c.close()};scope.cancel()}
        assertEquals(0,c.thumbnails.pendingCount);assertEquals(0,c.thumbnails.retainedBytes)
        val report=buildJsonObject{put("syntheticOnly",true);put("images",48);put("pageCycles",60);put("providerCalls",0);put("window","offscreen ImageComposeScene 1320x880");put("nativeStartupAndRss","NOT MEASURED; heap/process CPU are of isolated Gradle test JVM");put("measurements",JsonArray(measurements));put("remainingThumbnailTasks",c.thumbnails.pendingCount);put("retainedPixelsAfterClose",c.thumbnails.retainedBytes)}
        val target=Path.of("build/performance-108/sustained-pages.json");Files.createDirectories(target.parent);Files.writeString(target,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),report))
    }
}
