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
import java.time.Duration
import java.time.Instant
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.accessibility.Accessible
import jdk.jfr.Recording
import jdk.jfr.consumer.RecordingFile

/** Native test host only; uses an explicitly copied synthetic workspace and never the daily launcher. */
object NativeRouteVisibilityProbe {
    private data class Node(val name:String,val description:String,val width:Int,val height:Int) {
        fun json()=buildJsonObject{put("name",name.take(400));put("description",description.take(400));put("width",width);put("height",height)}
    }
    private fun accessibility(root:Accessible):List<Node> {
        val seen=IdentityHashMap<Accessible,Boolean>();val rows=mutableListOf<Node>()
        fun visit(value:Accessible,depth:Int) {
            if(depth>45||rows.size>=800||seen.put(value,true)!=null)return
            val a=value.accessibleContext?:return
            val size=a.accessibleComponent?.size
            rows+=Node(a.accessibleName.orEmpty(),a.accessibleDescription.orEmpty(),size?.width?:0,size?.height?:0)
            repeat(a.accessibleChildrenCount.coerceAtMost(800)){i->a.getAccessibleChild(i)?.let{visit(it,depth+1)}}
        }
        visit(root,0);return rows
    }
    @JvmStatic fun main(args:Array<String>) {
        val root=Path.of(args[0]).toAbsolutePath();val out=Path.of(args[1]).toAbsolutePath()
        val mode=args.getOrNull(2)?:"four-routes";require(mode in setOf("four-routes","gallery-only"))
        require(root.toString().contains(".local")&&root.toString().contains("disposable"))
        require(out.toString().startsWith("H:\\CCB-Studio-Suite\\.local\\"))
        Files.createDirectories(out)
        val expectedHashes=Json.parseToJsonElement(Files.readString(root.resolve("originals.json"))).jsonArray.map{it.jsonPrimitive.content}.toSet()
        require(expectedHashes.size==12)
        val started=System.nanoTime();var failure:Throwable?=null
        fun note(name:String,fields:JsonObject=buildJsonObject{}) {
            val row=buildJsonObject{put("phase",name);put("utc",Instant.now().toString());put("elapsedMs",(System.nanoTime()-started)/1e6)
                put("pid",ProcessHandle.current().pid());put("processCpuNs",ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos())
                put("heapUsedBytes",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used);fields.forEach{(k,v)->put(k,v)}}
            Files.writeString(out.resolve("events.jsonl"),row.toString()+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND)
        }
        val recording=Recording()
        recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10))
        recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(10))
        recording.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(1)).withStackTrace()
        listOf("jdk.InitialEnvironmentVariable","jdk.InitialSystemProperty","jdk.OSInformation","jdk.JVMInformation","jdk.SystemProcess").forEach{recording.disable(it)}
        val w=StudioWorkspace.openStudio(root.resolve("workspace"))
        val origin=w.catalog().filter{it.sha256() in expectedHashes}.associate{it.request().id().toString() to it.sha256()}
        require(origin.size==12 && origin.values.toSet()==expectedHashes)
        Files.writeString(out.resolve("ADMITTED_ORIGINALS.json"),buildJsonObject{origin.forEach{(id,hash)->put(id,hash)}}.toString())
        note("workspace-opened",buildJsonObject{put("jobs",w.state.jobs.size);put("originals",origin.size);put("java",System.getProperty("java.version"));put("mode",mode);put("host","native test Window; not production Main/installer; no DWM presentation measurement")})
        try {
            application(exitProcessOnExit=false) {
                val scope=rememberCoroutineScope()
                val c=remember{WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("runtime/budget"),
                    studioHttpClient().newBuilder().addInterceptor{error("Native test host forbids HTTP")}.build(),
                    admissionOverride=RequestAdmission{error("No provider sends")},pauseCollectionsOnStart=true,allowDefaultEntrypoint=false)}
                var closing by remember{mutableStateOf(false)};var epoch by remember{mutableLongStateOf(0)}
                var mountWorkbench by remember{mutableStateOf(false)}
                val drawn=remember{AtomicLong(-1)}
                fun close(){if(!closing){closing=true;scope.launch{try{c.close();note("controller-closed",buildJsonObject{put("pendingThumbnails",c.thumbnails.pendingCount);put("retainedThumbnailBytes",c.thumbnails.retainedBytes)})}
                    catch(e:Throwable){failure=e;note("close-failed",buildJsonObject{put("error",e.toString())})}finally{exitApplication()}}}}
                c.requestClose=::close
                Window(onCloseRequest=::close,title="Studio synthetic native preview timing",state=rememberWindowState(width=1320.dp,height=880.dp)) {
                    val chrome=rememberDesktopWindowChrome(window,::close);val requested=epoch
                    MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel,onPrimary=Ink,onSurface=DesktopBootstrapColors.foreground)) {
                        Box(Modifier.fillMaxSize().drawWithContent{drawContent();drawn.set(requested)}){if(mountWorkbench)Workbench(c,chrome,closing)else Text("Preparing synthetic preview fixture")}
                    }
                    LaunchedEffect(Unit) {
                        var activePhase="native-initial"
                        try {
                            suspend fun frame(){val target=++epoch;withTimeout(30000){while(!window.isShowing||drawn.get()<target)delay(20)}}
                            frame()
                            note("native-window-showing",buildJsonObject{put("showing",window.isShowing);put("width",window.width);put("height",window.height);put("scaleX",window.graphicsConfiguration.defaultTransform.scaleX)})
                            c.editor.resumeLastProject();c.active?.join();check(c.problem==null){c.problem.orEmpty()}
                            check(c.editor.document?.pages?.size==8)
                            c.current=c.editor.document!!.pages.first().assetId;c.view="图库"
                            recording.start();note("recording-start")
                            var lastNodes=emptyList<Node>()
                            suspend fun route(name:String,change:()->Unit,first:(List<Node>)->Boolean,ready:(List<Node>)->Boolean) {
                                activePhase=name;val start=System.nanoTime();val cpu=ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos()
                                note("$name-start");change();frame();var firstMs:Double?=null
                                try{
                                    withTimeout(30000){do{
                                        lastNodes=accessibility(window)
                                        if(firstMs==null&&first(lastNodes)){firstMs=(System.nanoTime()-start)/1e6;note("$name-first-component",buildJsonObject{put("firstMs",firstMs!!);put("boundary","native accessible component after completed draw; not DWM timestamp")})}
                                        if(firstMs!=null&&ready(lastNodes))break
                                        delay(20)
                                    }while(true)}
                                    frame()
                                    note("$name-ready",buildJsonObject{put("firstMs",firstMs!!);put("confirmedReadyMs",(System.nanoTime()-start)/1e6)
                                        put("processCpuMs",(ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos()-cpu)/1e6)
                                        put("nativeDrawEpoch",drawn.get());put("pendingThumbnails",c.thumbnails.pendingCount);put("queueRasters",c.editor.queueRasters.size());put("accessibleNodes",lastNodes.size)})
                                    Files.writeString(out.resolve("$name-accessibility.json"),JsonArray(lastNodes.map{it.json()}).toString())
                                }catch(e:Throwable){Files.writeString(out.resolve("$name-incomplete-accessibility.json"),JsonArray(lastNodes.map{it.json()}).toString());throw e}
                            }
                            fun thumbs(ns:List<Node>)=ns.any{it.name.contains("静态缩略图")||it.description.contains("静态缩略图")}
                            fun noPlaceholders(ns:List<Node>)=ns.none{it.name=="…"||it.name.contains("正在读取图片")}
                            route("gallery",{c.view="图库";mountWorkbench=true},::thumbs){ns->thumbs(ns)&&noPlaceholders(ns)&&c.gallery.historyCache?.second?.entries?.size==4&&c.thumbnails.pendingCount==0}
                            if(mode!="gallery-only"){
                            route("asset-archive",{c.library.tab="回收站";c.view="资产管理"},::thumbs){ns->thumbs(ns)&&noPlaceholders(ns)&&c.thumbnails.pendingCount==0&&ns.any{it.name.contains("基础提示词")}}
                            fun canvas(ns:List<Node>)=c.editor.canvasSource!=null&&ns.any{it.name.contains("排版编辑画布")||it.description.contains("排版编辑画布")}
                            route("editor",{c.view="编辑"},::canvas){ns->canvas(ns)&&ns.any{it.name.contains("编辑缩略图")||it.description.contains("编辑缩略图")}&&noPlaceholders(ns)}
                            fun result(ns:List<Node>)=ns.any{(it.name=="图片"||it.description=="图片")&&it.width>200&&it.height>200}
                            route("creation-right",{c.view="创作"},::result){ns->result(ns)&&noPlaceholders(ns)}
                            check(c.problem==null){c.problem.orEmpty()}
                            note("targeted-stage-cost-start")
                            withContext(Dispatchers.IO){
                                val id=c.current!!;val t=System.nanoTime();val bytes=w.bytes(id)
                                note("creation-source-read",buildJsonObject{put("wallMs",(System.nanoTime()-t)/1e6);put("sourceId",id)})
                                val decodeStart=System.nanoTime();DesktopImageEditing.decode(bytes,longestSide=1600).let{try{note("creation-display-decode",buildJsonObject{put("wallMs",(System.nanoTime()-decodeStart)/1e6);put("width",it.width);put("height",it.height)})}finally{it.flush()}}
                                val page=c.editor.document!!.pages.first();val renderStart=System.nanoTime()
                                typesetPreviewRaster(w,page,false,1).let{try{note("editor-queue-full-render",buildJsonObject{put("wallMs",(System.nanoTime()-renderStart)/1e6);put("width",it.width);put("height",it.height);put("nominalQueueDebounceMs",120);put("scope","one follow-up component cost, not original native route duration or main canvas engine")})}finally{it.flush()}}
                            }
                            }
                            withContext(Dispatchers.IO){origin.forEach{(id,hash)->check(sha256(w.bytes(id))==hash)}}
                            note("originals-verified",buildJsonObject{put("images",origin.size);put("providerCalls",0)})
                        }catch(e:Throwable){failure=e;note("FAILED_OR_UNKNOWN",buildJsonObject{put("activePhase",activePhase);put("error",e.toString());put("showing",window.isShowing);put("scope","failure may be readiness observation; no automatic product/protection attribution")})}
                        finally{if(recording.state==jdk.jfr.RecordingState.RUNNING)recording.stop();recording.dump(out.resolve("route.jfr"));note("close-requested");close()}
                    }
                }
            }
        }finally{
            recording.close();w.close()
            // Aggregate only allowlisted events/method signatures, never environment/property values.
            val samples=mutableListOf<JsonObject>()
            if(Files.isRegularFile(out.resolve("route.jfr")))RecordingFile(out.resolve("route.jfr")).use{file->
                while(file.hasMoreEvents()){val event=file.readEvent();val name=event.eventType.name
                    if(name in setOf("jdk.ExecutionSample","jdk.NativeMethodSample","jdk.JavaMonitorEnter")){
                        samples+=buildJsonObject{put("event",name);put("utc",event.startTime.toString());put("durationMs",event.duration.toNanos()/1e6)
                            put("frames",JsonArray(event.stackTrace?.frames.orEmpty().take(16).map{JsonPrimitive(it.method.type.name+"."+it.method.name+":"+it.lineNumber)}))}
                    }
                }
            }
            Files.writeString(out.resolve("jfr-allowlisted-events.json"),JsonArray(samples).toString())
        }
        note(if(failure==null)"PASS_WITH_HOST_SCOPE"else"FAILED_EXIT")
        failure?.let{throw it}
    }
}
