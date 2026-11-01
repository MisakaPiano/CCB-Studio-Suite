@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

/** New lock/real-menu gap; not the accepted 64-image diagnostic or native UI acceptance. */
class PreviewContentionWorkflowTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun seed(w:StudioWorkspace,r:Path):List<String> = (0 until 24).map{n->
        val image=BufferedImage(1024,1536,BufferedImage.TYPE_INT_RGB)
        val bytes=try{
            val line=IntArray(1024)
            for(y in 0 until 1536){for(x in line.indices)line[x]=(((x+n*9)%256) shl 16) or (((y+n*5)%256) shl 8) or ((x/8+y/8+n*17)%256);image.setRGB(0,y,1024,1,line,0,1024)}
            ByteArrayOutputStream().also{check(ImageIO.write(image,"png",it))}.toByteArray()
        }finally{image.flush()}
        w.importImage("contention-$n.png",bytes,buildJsonObject{put("kind","image-ingress");put("version",1);put("source","file");put("sourcePath",r.resolve("source-${n%4}/$n.png").toString())}.toString())
    }
    private fun report(name:String,value:JsonObject){val p=Path.of("build/preview-contention-20261028/$name.json");Files.createDirectories(p.parent);Files.writeString(p,Json{prettyPrint=true}.encodeToString(JsonObject.serializer(),value))}
    @Test fun historyProjectionAndPreviewReadCompeteForWorkspaceMonitor() {
        val r=root();StudioWorkspace.openStudio(r.resolve("workspace")).use{w->
            val ids=seed(w,r);val expected=sha256(w.bytes(ids.first()));val entered=CountDownLatch(1)
            val historyMs=AtomicLong();val previewMs=AtomicLong();val error=AtomicReference<Throwable?>();val observed=AtomicBoolean()
            val history=Thread({try{w.operation{entered.countDown();val t=System.nanoTime();assertTrue(readGalleryHistory(w).entries.isEmpty());historyMs.set(System.nanoTime()-t)}}catch(e:Throwable){error.set(e)}},"diagnostic-history")
            val preview=Thread({try{check(entered.await(10,TimeUnit.SECONDS));val t=System.nanoTime();assertEquals(expected,sha256(w.bytes(ids.first())));previewMs.set(System.nanoTime()-t)}catch(e:Throwable){error.set(e)}},"diagnostic-preview")
            history.start();preview.start()
            val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20)
            while((history.isAlive||preview.isAlive)&&System.nanoTime()<deadline){
                val info=ManagementFactory.getThreadMXBean().getThreadInfo(preview.id)
                if(info?.threadState==Thread.State.BLOCKED&&info.lockOwnerId==history.id)observed.set(true)
                Thread.sleep(1)
            }
            history.join(1000);preview.join(1000);assertFalse(history.isAlive);assertFalse(preview.isAlive);error.get()?.let{throw it}
            report("monitor",buildJsonObject{put("images",24);put("historyMs",historyMs.get()/1e6);put("previewReadMs",previewMs.get()/1e6);put("previewBlockedByHistoryThread",observed.get());put("scope","production methods, same reentrant monitor, no artificial hold inside it; imported images/no jobs; not native UI")})
        }
    }
    @Test fun actualGroupingMenuSeparatesStatePublicationFromCommandTail():Unit=runBlocking(awt) {
        val r=root();val w=StudioWorkspace.openStudio(r.resolve("workspace"));val ids=withContext(Dispatchers.IO){seed(w,r)}
        val original=withContext(Dispatchers.IO){ids.associateWith{sha256(w.bytes(it))}}
        val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        val scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){GalleryWorkspace(c)}}
        suspend fun frame(){scene.render(System.nanoTime()).close();yield()}
        fun nodes():List<SemanticsNode>{
            fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
            return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
        }
        fun click(text:String){nodes().first{n->n.config.getOrNull(SemanticsProperties.Text)?.any{it.text==text}==true&&n.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke()}
        try{
            withTimeout(30000){do{frame();delay(10)}while(c.gallery.foldPreferences==null||c.gallery.historyCache==null||c.thumbnails.pendingCount>0)}
            click("完整 ▾");repeat(3){frame()}
            val beforeAssets=c.assets;val beforeCount=w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size
            val t=System.nanoTime();click("目录")
            withTimeout(30000){while(!c.gallery.directoryFold){frame();delay(1)}}
            val published=(System.nanoTime()-t)/1e6
            c.active?.join();val done=(System.nanoTime()-t)/1e6;repeat(3){frame()}
            assertNull(c.problem,c.problem);assertTrue(c.gallery.directoryFold)
            val prefs=GalleryFoldPreferences(c.library.repository);assertEquals(setOf(0),prefs.directoryDepths());assertEquals(c.gallery.fold,prefs.load().getValue(0))
            val revisions=w.assets.catalogProject(LibraryRepository.TAG.toByteArray()).size-beforeCount
            assertSame(beforeAssets,c.assets,"View preferences must not refresh the entire image catalog");assertEquals(1,revisions)
            report("group-command",buildJsonObject{put("images",24);put("statePublishedMs",published);put("commandCompleteMs",done);put("tailAfterPublicationMs",done-published);put("visibleAssetListReused",beforeAssets===c.assets);put("newLibraryRevisions",revisions);put("scope","actual semantics click/production command; offscreen frame and polling overhead; not DWM/input latency")})
            // A real image-changing command retains the default full catalog refresh.
            var imported=""
            c.run("诊断新增合成图"){imported=w.importImage("new.png",EditorRenderer.synthetic(48,48))}
            c.active?.join();assertNull(c.problem,c.problem);assertTrue(c.assets.any{it.request().id().toString()==imported})
            withContext(Dispatchers.IO){ids.forEach{assertEquals(original[it],sha256(w.bytes(it)))}}
        }finally{scene.close();c.close();scope.cancel()}
    }
}
