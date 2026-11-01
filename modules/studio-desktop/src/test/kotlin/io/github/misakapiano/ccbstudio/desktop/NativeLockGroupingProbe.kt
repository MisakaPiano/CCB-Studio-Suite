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
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.management.ThreadInfo
import javax.accessibility.AccessibleContext
import javax.accessibility.Accessible
import jdk.jfr.Recording
import jdk.jfr.consumer.RecordingFile

/** Native test host only; uses an explicitly copied synthetic workspace and never the daily launcher. */
object NativeLockGroupingProbe {
    private data class Node(val name:String,val description:String,val width:Int,val height:Int)
    private fun accessibility(root:Accessible):List<Node> {
        val seen=IdentityHashMap<Accessible,Boolean>();val rows=mutableListOf<Node>()
        fun visit(value:Accessible,depth:Int) {
            if(depth>45||rows.size>=800||seen.put(value,true)!=null)return
            val a=value.accessibleContext?:return;val size=a.accessibleComponent?.size
            rows+=Node(a.accessibleName.orEmpty(),a.accessibleDescription.orEmpty(),size?.width?:0,size?.height?:0)
            repeat(a.accessibleChildrenCount.coerceAtMost(800)){i->a.getAccessibleChild(i)?.let{visit(it,depth+1)}}
        }
        visit(root,0);return rows
    }
    /** Only the owned synthetic Window and its owned popup windows. No arbitrary application traversal. */
    private fun action(root:java.awt.Window,label:String):Boolean {
        val seen=IdentityHashMap<Accessible,Boolean>()
        fun visit(value:Accessible,depth:Int):AccessibleContext? {
            if(depth>45||seen.size>=1500||seen.put(value,true)!=null)return null
            val a=value.accessibleContext?:return null
            if(a.accessibleName==label||a.accessibleDescription==label) {
                var candidate:AccessibleContext?=a
                repeat(4) {
                    val current=candidate
                    if(current?.accessibleAction?.accessibleActionCount?.let{it>0}==true)return current
                    candidate=current?.accessibleParent?.accessibleContext
                }
            }
            repeat(a.accessibleChildrenCount.coerceAtMost(800)){i->a.getAccessibleChild(i)?.let{visit(it,depth+1)?.let{return it}}}
            return null
        }
        fun find(w:java.awt.Window):AccessibleContext? {
            visit(w,0)?.let{return it}
            w.ownedWindows.filter{it.isShowing}.forEach{find(it)?.let{return it}}
            return null
        }
        return find(root)?.accessibleAction?.doAccessibleAction(0)==true
    }
    /** Samples method signatures and monitor metadata only, never locals/arguments/UI text. */
    private class MonitorTrace(val out:Path,val workspace:StudioWorkspace) {
        private val running=AtomicBoolean(false);private var worker:Thread?=null
        fun start() {
            check(running.compareAndSet(false,true))
            worker=Thread({
                val bean=ManagementFactory.getThreadMXBean()
                if(bean.isThreadCpuTimeSupported&&!bean.isThreadCpuTimeEnabled)bean.isThreadCpuTimeEnabled=true
                Files.newBufferedWriter(out.resolve("live-monitors.jsonl")).use{writer->
                    while(running.get()) {
                        val before=System.nanoTime();val utc=Instant.now().toString()
                        val all=bean.dumpAllThreads(true,true)
                        val selected=all.filter{t->t.lockedMonitors.any{it.identityHashCode==System.identityHashCode(workspace)}||
                            t.lockInfo?.identityHashCode==System.identityHashCode(workspace)||
                            t.stackTrace.any{it.className.startsWith("io.github.misakapiano.ccbstudio.")}||
                            t.threadName.startsWith("AWT-EventQueue")}
                        val row=buildJsonObject {
                            put("utc",utc);put("snapshotNs",before);put("inspectionMs",(System.nanoTime()-before)/1e6)
                            put("workspaceMonitorIdentity",System.identityHashCode(workspace))
                            put("threads",JsonArray(selected.map{t->buildJsonObject{
                                put("id",t.threadId);put("name",t.threadName);put("state",t.threadState.name)
                                put("cpuNs",bean.getThreadCpuTime(t.threadId));put("lockOwnerId",t.lockOwnerId)
                                t.lockInfo?.let{put("waitingClass",it.className);put("waitingIdentity",it.identityHashCode)}
                                put("frames",JsonArray(t.stackTrace.take(96).map{JsonPrimitive(it.className+"."+it.methodName+":"+it.lineNumber)}))
                                put("heldMonitors",JsonArray(t.lockedMonitors.map{m->buildJsonObject{
                                    put("class",m.className);put("identity",m.identityHashCode);put("depth",m.lockedStackDepth)
                                    m.lockedStackFrame?.let{put("acquiredAt",it.className+"."+it.methodName+":"+it.lineNumber)}
                                }}))
                            }}))
                        }
                        writer.write(row.toString());writer.newLine()
                        Thread.sleep(10)
                    }
                }
            },"synthetic-monitor-observer").apply{isDaemon=true;start()}
        }
        fun stop(){running.set(false);worker?.join(5000);check(worker?.isAlive!=true)}
    }
    @JvmStatic fun main(args:Array<String>) {
        val root=Path.of(args[0]).toAbsolutePath();val out=Path.of(args[1]).toAbsolutePath()
        val mode="gallery-lock-and-group"
        require(root.toString().contains(".local")&&root.toString().contains("disposable"))
        require(out.toString().startsWith("H:\\CCB-Studio-Suite\\.local\\"))
        Files.createDirectories(out)
        val expectedHashes=Json.parseToJsonElement(Files.readString(root.resolve("originals.json"))).jsonArray.map{it.jsonPrimitive.content}.toSet()
        require(expectedHashes.size==12)
        val started=System.nanoTime();var failure:Throwable?=null
        val eventGate=Any()
        fun note(name:String,fields:JsonObject=buildJsonObject{}) {
            val row=buildJsonObject{put("phase",name);put("utc",Instant.now().toString());put("elapsedMs",(System.nanoTime()-started)/1e6)
                put("threadId",Thread.currentThread().id);put("pid",ProcessHandle.current().pid());put("processCpuNs",ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos())
                put("heapUsedBytes",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used);fields.forEach{(k,v)->put(k,v)}}
            synchronized(eventGate){Files.writeString(out.resolve("events.jsonl"),row.toString()+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND)}
        }
        val recording=Recording()
        recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(10))
        recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(10))
        recording.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(1)).withStackTrace()
        listOf("jdk.InitialEnvironmentVariable","jdk.InitialSystemProperty","jdk.OSInformation","jdk.JVMInformation","jdk.SystemProcess").forEach{recording.disable(it)}
        val w=StudioWorkspace.openStudio(root.resolve("workspace"))
        val trace=MonitorTrace(out,w)
        val revisionCountBefore=w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size
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
                    catch(e:Throwable){failure=e;note("close-failed",buildJsonObject{put("errorClass",e.javaClass.name)})}finally{exitApplication()}}}}
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
                            var historyCalls=0
                            c.gallery.historyReadOverride={
                                historyCalls++
                                note("history-body-start",buildJsonObject{put("call",historyCalls)})
                                try{readGalleryHistory(w)}finally{note("history-body-end",buildJsonObject{put("call",historyCalls)})}
                            }
                            trace.start();recording.start();note("recording-start")
                            var lastNodes=emptyList<Node>()
                            suspend fun route(name:String,change:()->Unit,first:(List<Node>)->Boolean,ready:(List<Node>)->Boolean) {
                                activePhase=name;val start=System.nanoTime();val cpu=ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos()
                                note("$name-start");change();frame();var firstMs:Double?=null
                                var historySeen=false;var pendingSeen=false
                                try{
                                    withTimeout(30000){do{
                                        lastNodes=accessibility(window)
                                        if(!historySeen&&c.gallery.historyCache!=null){historySeen=true;note("$name-history-cache-observed")}
                                        if(!pendingSeen&&c.thumbnails.pendingCount==0&&historySeen){pendingSeen=true;note("$name-thumbnails-drained-observed")}
                                        if(firstMs==null&&first(lastNodes)){firstMs=(System.nanoTime()-start)/1e6;note("$name-first-component",buildJsonObject{put("firstMs",firstMs!!);put("boundary","native accessible component after completed draw; not DWM timestamp")})}
                                        if(firstMs!=null&&ready(lastNodes))break
                                        delay(20)
                                    }while(true)}
                                    frame()
                                    note("$name-ready",buildJsonObject{put("firstMs",firstMs!!);put("confirmedReadyMs",(System.nanoTime()-start)/1e6)
                                        put("processCpuMs",(ProcessHandle.current().info().totalCpuDuration().orElse(Duration.ZERO).toNanos()-cpu)/1e6)
                                        put("nativeDrawEpoch",drawn.get());put("pendingThumbnails",c.thumbnails.pendingCount);put("queueRasters",c.editor.queueRasters.size());put("accessibleNodes",lastNodes.size)})
                                    note("$name-observation-count",buildJsonObject{put("nodes",lastNodes.size)})
                                }catch(e:Throwable){note("$name-incomplete-observation",buildJsonObject{put("nodes",lastNodes.size)});throw e}
                            }
                            fun thumbs(ns:List<Node>)=ns.any{it.name.contains("静态缩略图")||it.description.contains("静态缩略图")}
                            fun noPlaceholders(ns:List<Node>)=ns.none{it.name=="…"||it.name.contains("正在读取图片")}
                            route("gallery",{c.view="图库";mountWorkbench=true},::thumbs){ns->thumbs(ns)&&noPlaceholders(ns)&&c.gallery.historyCache?.second?.entries?.size==4&&c.gallery.foldPreferences!=null&&c.thumbnails.pendingCount==0}
                            check(!c.gallery.directoryFold && !c.gallery.tagFold && c.gallery.fold.type.label=="完整")
                            val assetsBefore=c.assets;val historyBefore=c.gallery.historyCache
                            val libraryBefore=c.library.state
                            activePhase="group-menu"
                            note("group-menu-open-action");check(action(window,"完整 ▾")){"Grouping trigger accessibility action unavailable"}
                            frame();delay(40)
                            activePhase="group-directory"
                            note("group-action-start",buildJsonObject{put("historyCalls",historyCalls);put("historyAlreadyReady",historyBefore!=null)})
                            val groupStart=System.nanoTime()
                            check(action(window,"目录")){"Directory menu item accessibility action unavailable"}
                            note("group-action-returned",buildJsonObject{put("busy",c.busy)})
                            var preferenceSeen=false;var librarySeen=false;var commandSeen=false;var contentSeen=false
                            withTimeout(30000){while(true) {
                                if(!preferenceSeen&&c.gallery.directoryFold){preferenceSeen=true;note("group-preference-observed",buildJsonObject{put("fromActionMs",(System.nanoTime()-groupStart)/1e6)})}
                                if(!librarySeen&&c.library.state!==libraryBefore){librarySeen=true;note("group-library-revision-observed",buildJsonObject{put("fromActionMs",(System.nanoTime()-groupStart)/1e6);put("libraryStateChanged",c.library.state!==libraryBefore)})}
                                if(!commandSeen&&!c.busy&&c.active?.isCompleted==true){commandSeen=true;note("group-command-complete-observed",buildJsonObject{put("fromActionMs",(System.nanoTime()-groupStart)/1e6)})}
                                if(preferenceSeen){
                                    frame();lastNodes=accessibility(window)
                                    val actual=lastNodes.any{it.name=="目录 ▾"||it.description=="目录 ▾"}&&thumbs(lastNodes)
                                    if(!contentSeen&&actual){contentSeen=true;note("group-first-component",buildJsonObject{put("fromActionMs",(System.nanoTime()-groupStart)/1e6);put("drawEpoch",drawn.get())})}
                                    if(contentSeen&&commandSeen&&librarySeen&&noPlaceholders(lastNodes)&&c.thumbnails.pendingCount==0&&c.gallery.historyCache===historyBefore){
                                        frame();note("group-ready",buildJsonObject{
                                            put("fromActionMs",(System.nanoTime()-groupStart)/1e6);put("drawEpoch",drawn.get())
                                            put("historyCalls",historyCalls);put("historyReused",c.gallery.historyCache===historyBefore)
                                            put("assetsListReused",c.assets===assetsBefore);put("pendingThumbnails",c.thumbnails.pendingCount)
                                            put("libraryStateChanged",c.library.state!==libraryBefore);put("directoryFold",c.gallery.directoryFold)
                                        });break
                                    }
                                }
                                delay(5)
                            }}
                            check(historyCalls==1);check(c.assets===assetsBefore);check(c.problem==null)
                            trace.stop();recording.stop()
                            withContext(Dispatchers.IO){
                                origin.forEach{(id,hash)->check(sha256(w.bytes(id))==hash)}
                                val delta=w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size-revisionCountBefore
                                note("group-revisions-verified",buildJsonObject{put("newRevisions",delta)});check(delta==1)
                            }
                            note("originals-verified",buildJsonObject{put("images",origin.size);put("providerCalls",0)})
                        }catch(e:Throwable){failure=e;note("FAILED_OR_UNKNOWN",buildJsonObject{put("activePhase",activePhase);put("errorClass",e.javaClass.name);put("showing",window.isShowing);put("scope","failure may be readiness observation; no automatic product/protection attribution")})}
                        finally{trace.stop();if(recording.state==jdk.jfr.RecordingState.RUNNING)recording.stop();recording.dump(out.resolve("route.jfr"));note("close-requested");close()}
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
                            event.thread?.let{put("eventThreadId",it.javaThreadId)}
                            if(name=="jdk.JavaMonitorEnter"){
                                event.getThread("previousOwner")?.let{put("previousOwnerId",it.javaThreadId)}
                                put("monitorClass",event.getClass("monitorClass").name);put("address",event.getLong("address"))
                            }else event.getThread("sampledThread")?.let{put("sampledThreadId",it.javaThreadId)}
                            put("frames",JsonArray(event.stackTrace?.frames.orEmpty().take(96).map{JsonPrimitive(it.method.type.name+"."+it.method.name+":"+it.lineNumber)}))}
                    }
                }
            }
            Files.writeString(out.resolve("jfr-allowlisted-events.json"),JsonArray(samples).toString())
        }
        note(if(failure==null)"PASS_WITH_HOST_SCOPE"else"FAILED_EXIT")
        failure?.let{throw IllegalStateException("Synthetic native observation failed; see allowlisted events")}
    }
}
