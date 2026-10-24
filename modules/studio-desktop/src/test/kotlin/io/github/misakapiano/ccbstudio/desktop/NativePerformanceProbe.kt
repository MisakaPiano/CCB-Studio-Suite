package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/** Test entry point only. Native Window/Workbench, not an offscreen or shipped launcher. */
object NativePerformanceProbe {
    @JvmStatic fun main(args:Array<String>) {
        val root=Path.of(args[1]).toAbsolutePath();require(root.toString().contains(".local")&&root.toString().contains("disposable"))
        val data=root.resolve("workspace")
        if(args[0]=="prepare") {
            require(!Files.exists(data));Files.createDirectories(root.resolve("runtime"))
            StudioWorkspace.openStudio(data).use{w->
                val bytes=EditorRenderer.synthetic(832,1216)
                repeat(48){w.importImage("synthetic-native-$it.png",bytes)}
                LibraryRepository(w).pauseCollections(true)
                val images=SharedAssets.images(w)
                Files.writeString(root.resolve("originals.json"),buildJsonObject{images.forEach{a->put(a.request().id().toString(),sha256(w.bytes(a.request().id().toString())))}}.toString())
            }
            return
        }
        val out=Path.of(args[2]);Files.createDirectories(out)
        val start=System.nanoTime();val events=out.resolve("events.jsonl");val frames=AtomicLong();val lastDraw=AtomicLong();val galleryDraw=AtomicLong()
        val epoch=System.currentTimeMillis();val runtime=ManagementFactory.getRuntimeMXBean()
        fun note(phase:String,extra:JsonObject=buildJsonObject{}) {
            val value=buildJsonObject {
                put("phase",phase);put("utc",Instant.now().toString());put("elapsedMs",(System.nanoTime()-start)/1e6)
                put("epochMillis",System.currentTimeMillis());put("jvmUptimeMs",runtime.uptime);put("pid",ProcessHandle.current().pid())
                put("processCpuNs",ProcessHandle.current().info().totalCpuDuration().orElse(java.time.Duration.ZERO).toNanos())
                put("heapUsedBytes",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used)
                put("liveThreads",ManagementFactory.getThreadMXBean().threadCount);put("nativeDraws",frames.get())
                extra.forEach{(k,v)->put(k,v)}
            }
            Files.writeString(events,value.toString()+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND)
        }
        note("main-enter",buildJsonObject{put("java",System.getProperty("java.version"));put("mainEpochMillis",epoch);put("launchKind","test-only-native-workbench-host")})
        StudioBrand.configureDefaultWindows()
        val w=StudioWorkspace.openStudio(data);note("workspace-opened")
        val expected=Json.parseToJsonElement(Files.readString(root.resolve("originals.json"))).jsonObject
        var failure:Throwable?=null
        application {
            val scope=rememberCoroutineScope()
            val http=remember{studioHttpClient().newBuilder().addInterceptor{error("Native resource probe forbids all HTTP")}.build()}
            val c=remember {WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime/catalog")),root.resolve("runtime/budget"),http,
                admissionOverride=RequestAdmission{error("No provider requests")},pauseCollectionsOnStart=true,allowDefaultEntrypoint=false).also{note("controller-opened")}}
            var closing by remember{mutableStateOf(false)}
            fun close(){if(!closing){closing=true;scope.launch{try{c.close();note("controller-closed",buildJsonObject{put("pendingThumbnails",c.thumbnails.pendingCount);put("retainedThumbnailBytes",c.thumbnails.retainedBytes)})}catch(e:Throwable){failure=e;note("close-failed",buildJsonObject{put("error",e.toString())})}finally{exitApplication()}}}}
            c.requestClose=::close
            Window(onCloseRequest=::close,title="Studio PERF-002/007 · ${out.fileName}",undecorated=DesktopWindowChrome.isWindows,
                icon=StudioBrand.painter,state=rememberWindowState(width=1320.dp,height=880.dp)) {
                val chrome=rememberDesktopWindowChrome(window,::close);SideEffect{chrome.appearance(true)}
                MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=DesktopBootstrapColors.foreground)) {
                    Box(Modifier.fillMaxSize().drawWithContent {
                        drawContent()
                        if(window.isShowing){frames.incrementAndGet();lastDraw.set(System.nanoTime())
                            if(c.view=="图库"&&c.gallery.historyCache!=null&&c.gallery.foldPreferences!=null&&c.thumbnails.pendingCount==0&&c.thumbnails.retainedBytes>0)galleryDraw.set(System.nanoTime())}
                    }) {Workbench(c,chrome,closing)}
                }
                LaunchedEffect(Unit) {
                    try {
                        withTimeout(30000){while(frames.get()==0L)delay(20)}
                        note("first-native-draw",buildJsonObject{put("showing",window.isShowing);put("width",window.width);put("height",window.height);put("screenScaleX",window.graphicsConfiguration.defaultTransform.scaleX)})
                        delay(5000);note("creation-idle-5s")
                        suspend fun gallery(phase:String) {
                            val begin=System.nanoTime();galleryDraw.set(0);c.view="图库"
                            withTimeout(30000){while(galleryDraw.get()<begin)delay(20)}
                            note(phase,buildJsonObject{put("readyMs",(galleryDraw.get()-begin)/1e6);put("pendingThumbnails",c.thumbnails.pendingCount);put("retainedThumbnailBytes",c.thumbnails.retainedBytes);put("historyLoaded",c.gallery.historyCache!=null)})
                        }
                        gallery("first-gallery-ready")
                        delay(10000);note("gallery-idle-10s")
                        repeat(6){cycle->val begin=System.nanoTime();c.view="创作";withTimeout(30000){while(lastDraw.get()<begin)delay(20)};delay(150);gallery("gallery-cycle-$cycle");delay(150)}
                        check(c.problem==null){c.problem.orEmpty()};check(c.thumbnails.retainedBytes<=8L*1024*1024)
                        expected.forEach{(id,hash)->check(sha256(w.bytes(id))==hash.jsonPrimitive.content)}
                        note("originals-verified",buildJsonObject{put("images",expected.size);put("providerCalls",0)})
                    }catch(e:Throwable){failure=e;note("FAILED",buildJsonObject{put("error",e.toString());put("causeAttribution","UNKNOWN unless error itself identifies a cause")})}
                    finally{note("close-requested");close()}
                }
            }
        }
        note(if(failure==null)"PASS"else"FAILED_EXIT")
        failure?.let{throw it}
    }
}
