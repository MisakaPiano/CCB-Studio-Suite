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
        val start=System.nanoTime();val events=out.resolve("events.jsonl");val frames=AtomicLong();val drawCallbacks=AtomicLong();val firstDraw=AtomicLong();val lastDraw=AtomicLong();val drawnEpoch=AtomicLong(-1)
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
        application(exitProcessOnExit=false) {
            val scope=rememberCoroutineScope()
            val http=remember{studioHttpClient().newBuilder().addInterceptor{error("Native resource probe forbids all HTTP")}.build()}
            val c=remember {WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime/catalog")),root.resolve("runtime/budget"),http,
                admissionOverride=RequestAdmission{error("No provider requests")},pauseCollectionsOnStart=true,allowDefaultEntrypoint=false).also{note("controller-opened")}}
            var closing by remember{mutableStateOf(false)}
            var renderEpoch by remember{mutableLongStateOf(0)}
            fun close(){if(!closing){closing=true;scope.launch{try{c.close();note("controller-closed",buildJsonObject{put("pendingThumbnails",c.thumbnails.pendingCount);put("retainedThumbnailBytes",c.thumbnails.retainedBytes)})}catch(e:Throwable){failure=e;note("close-failed",buildJsonObject{put("error",e.toString())})}finally{exitApplication()}}}}
            c.requestClose=::close
            Window(onCloseRequest=::close,title="Studio PERF-002/007 · ${out.fileName}",undecorated=DesktopWindowChrome.isWindows,
                icon=StudioBrand.painter,state=rememberWindowState(width=1320.dp,height=880.dp)) {
                val chrome=rememberDesktopWindowChrome(window,::close);SideEffect{chrome.appearance(true)}
                val requestedEpoch=renderEpoch
                MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=DesktopBootstrapColors.foreground)) {
                    Box(Modifier.fillMaxSize().drawWithContent {
                        drawCallbacks.incrementAndGet()
                        drawContent()
                        // Compose prepares its first native surface before showing the Window.
                        // Verify both completed draw and showing state; don't require simultaneous callbacks.
                        val now=System.nanoTime();frames.incrementAndGet();firstDraw.compareAndSet(0,now);lastDraw.set(now);drawnEpoch.set(requestedEpoch)
                    }) {Workbench(c,chrome,closing)}
                }
                LaunchedEffect(Unit) {
                    note("native-window-state",buildJsonObject{put("showing",window.isShowing);put("visible",window.isVisible);put("displayable",window.isDisplayable);put("width",window.width);put("height",window.height);put("drawCallbacks",drawCallbacks.get())})
                    try {
                        withTimeout(30000){while(!window.isShowing||frames.get()==0L)delay(20)}
                        note("first-native-draw",buildJsonObject{put("showing",window.isShowing);put("firstContentDrawMs",(firstDraw.get()-start)/1e6);put("predicate","completed native draw AND Window showing; Windows compositor presentation not measured");put("width",window.width);put("height",window.height);put("screenScaleX",window.graphicsConfiguration.defaultTransform.scaleX)})
                        delay(5000);note("creation-idle-5s")
                        suspend fun confirmFrame() {
                            // Native rendering may retain the parent display list while children redraw.
                            // Change a test-only draw input once per barrier, not an idle animation loop.
                            val wanted=++renderEpoch
                            withTimeout(30000){while(!window.isShowing||drawnEpoch.get()<wanted)delay(20)}
                        }
                        suspend fun gallery(phase:String) {
                            val begin=System.nanoTime();c.view="图库";confirmFrame()
                            withTimeout(30000){while(c.gallery.historyCache==null||c.gallery.foldPreferences==null||c.thumbnails.pendingCount!=0||c.thumbnails.retainedBytes==0L)delay(20)}
                            confirmFrame()
                            note(phase,buildJsonObject{put("readyMs",(lastDraw.get()-begin)/1e6);put("confirmedDrawEpoch",drawnEpoch.get());put("pendingThumbnails",c.thumbnails.pendingCount);put("retainedThumbnailBytes",c.thumbnails.retainedBytes);put("historyLoaded",c.gallery.historyCache!=null);put("predicate","view frame, metadata+thumbnail readiness, explicit confirmation frame; not compositor timestamp")})
                        }
                        gallery("first-gallery-ready")
                        delay(10000);note("gallery-idle-10s")
                        repeat(6){cycle->c.view="创作";confirmFrame();delay(150);gallery("gallery-cycle-$cycle");delay(150)}
                        check(c.problem==null){c.problem.orEmpty()};check(c.thumbnails.retainedBytes<=8L*1024*1024)
                        expected.forEach{(id,hash)->check(sha256(w.bytes(id))==hash.jsonPrimitive.content)}
                        note("originals-verified",buildJsonObject{put("images",expected.size);put("providerCalls",0)})
                    }catch(e:Throwable){failure=e;note("FAILED",buildJsonObject{put("error",e.toString());put("showing",window.isShowing);put("visible",window.isVisible);put("displayable",window.isDisplayable);put("width",window.width);put("height",window.height);put("drawCallbacks",drawCallbacks.get());put("causeAttribution","UNKNOWN unless error itself identifies a cause")})}
                    finally{note("close-requested");close()}
                }
            }
        }
        note(if(failure==null)"PASS"else"FAILED_EXIT")
        failure?.let{throw it}
    }
}
