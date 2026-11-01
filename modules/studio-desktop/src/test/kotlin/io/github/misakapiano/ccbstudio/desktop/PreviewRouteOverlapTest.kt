@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

/** Route readiness and overlap gap with saved generation references; never native/DWM acceptance. */
class PreviewRouteOverlapTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun generatedHistoryAndFourPreviewConsumersReportActualReadiness():Unit=runBlocking(awt) {
        val root=Path.of(System.getProperty("preview.test.root"),newId())
        val out=Path.of("build/preview-route-overlap-20261028");Files.createDirectories(out)
        val w=StudioWorkspace.openStudio(root.resolve("workspace"))
        val ids=mutableListOf<String>();val hashes=mutableListOf<String>()
        withContext(Dispatchers.IO) {
            repeat(12){n->
                val image=BufferedImage(1024,1536,BufferedImage.TYPE_INT_RGB)
                val bytes=try{
                    val line=IntArray(1024)
                    for(y in 0 until 1536){for(x in line.indices)line[x]=(((x+n*19)%256) shl 16) or (((y+n*11)%256) shl 8) or ((x/8+y/8+n*7)%256);image.setRGB(0,y,1024,1,line,0,1024)}
                    ByteArrayOutputStream().also{check(ImageIO.write(image,"png",it))}.toByteArray()
                }finally{image.flush()}
                hashes+=sha256(bytes);ids+=w.importImage("route-$n.png",bytes)
            }
            assertEquals(12,hashes.toSet().size)
            val account=AccountProfile(alias="synthetic-no-network",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
            val jobs=(0 until 4).map{n->
                val prompt="fixture-group-$n";val recipe=NovelAiStudioDraft(basePrompt=prompt).toRecipe()
                val raw=documentJson.encodeToString(GenerationReceipt(recipe=recipe,account=account,request=buildJsonObject{put("input",prompt)}))
                JobRecord(account=account,recipe=raw,results=ids.subList(n*2,n*2+2).toList(),state="COMPLETED",createdAt=1767225600000L+n)
            }
            w.save(w.state.copy(jobs=jobs))
            AssetArchive(w).change(ids.take(4).toSet(),true)
            GalleryFoldPreferences(LibraryRepository(w),"archive").save(NovelAiHistoryFoldPreference(0,true,NovelAiHistoryFoldType.BASE))
            LibraryRepository(w).pauseCollections(true)
        }
        val scope=CoroutineScope(SupervisorJob()+awt)
        val http=studioHttpClient().newBuilder().addInterceptor{error("All HTTP forbidden by route diagnostic")}.build()
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root.resolve("runtime")),root.resolve("budget"),http,admissionOverride=RequestAdmission{error("No provider calls")},pauseCollectionsOnStart=true,allowDefaultEntrypoint=false)
        c.editor.create("synthetic route probe",ids.drop(4),true);c.active?.join();assertNull(c.problem,c.problem)
        c.current=ids[4];c.view="图库"
        val historyReads=java.util.concurrent.atomic.AtomicInteger()
        c.gallery.historyReadOverride={historyReads.incrementAndGet();readGalleryHistory(w)}
        val phase=AtomicReference("setup");val rows=mutableListOf<JsonObject>()
        val samples=CopyOnWriteArrayList<JsonObject>();val executor=Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate({
            try{
                val category=mutableMapOf<String,Int>()
                ManagementFactory.getThreadMXBean().dumpAllThreads(false,false).forEach{t->
                    val names=t.stackTrace.map{it.className+"."+it.methodName}
                    fun count(label:String){category[label]=(category[label]?:0)+1}
                    if(names.any{it.contains("AssetStore.catalog")||it.contains("AssetStore.verifyFile")})count("catalog-or-payload-verification")
                    if(names.any{it.contains("StudioWorkspace.bytes")})count("original-read")
                    if(names.any{it.contains("DesktopImageEditing.decode")||it.startsWith("javax.imageio.")})count("decode")
                    if(names.any{it.contains("TypesetRenderer.")||it.contains("TypesetPreviewSession.")})count("editor-render")
                    if(t.threadState==Thread.State.BLOCKED&&names.any{it.contains("ccbstudio")})count("blocked-suite-thread")
                }
                samples+=buildJsonObject{put("phase",phase.get());put("nanoTime",System.nanoTime());put("categories",buildJsonObject{category.forEach{(k,v)->put(k,v)}})}
            }catch(e:Exception){samples+=buildJsonObject{put("phase",phase.get());put("samplingError",e.javaClass.simpleName)}}
        },0,10,TimeUnit.MILLISECONDS)
        var scene:ImageComposeScene?=null
        try{
            scene=ImageComposeScene(1320,880,coroutineContext=awt){MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){Workbench(c,DesktopWindowChrome{},false)}}
            suspend fun frame(){scene.render(System.nanoTime()).close();yield()}
            fun nodes():List<SemanticsNode>{
                fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
            }
            fun text(n:SemanticsNode)=n.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" "){it.text}
            fun desc(n:SemanticsNode)=n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(" ")
            suspend fun route(name:String,change:()->Unit,first:(List<SemanticsNode>)->Boolean,settled:(List<SemanticsNode>)->Boolean){
                phase.set(name);val begin=System.nanoTime();var firstMs:Double?=null;change()
                withTimeout(30000){
                    do{frame();val ns=nodes();if(firstMs==null&&first(ns))firstMs=(System.nanoTime()-begin)/1e6
                        if(firstMs!=null&&settled(ns))break
                        delay(10)
                    }while(true)
                }
                repeat(2){frame()}
                val ns=nodes()
                rows+=buildJsonObject{put("phase",name);put("firstContentMs",firstMs!!);put("confirmedSettledMs",(System.nanoTime()-begin)/1e6);put("imageNodes",ns.count{desc(it).contains("图片")||desc(it).contains("缩略图")});put("thumbnailPending",c.thumbnails.pendingCount);put("queueRasterCount",c.editor.queueRasters.size());put("heapUsedBytes",ManagementFactory.getMemoryMXBean().heapMemoryUsage.used)}
                Files.writeString(out.resolve("partial.json"),JsonArray(rows).toString())
            }
            fun thumbs(ns:List<SemanticsNode>)=ns.any{it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().contains("静态缩略图")}
            fun noPlaceholders(ns:List<SemanticsNode>)=ns.none{it.config.getOrNull(SemanticsProperties.Text).orEmpty().any{t->t.text=="…"}}
            route("gallery",{c.view="图库"},::thumbs){ns->thumbs(ns)&&noPlaceholders(ns)&&c.gallery.historyCache!=null&&c.thumbnails.pendingCount==0}
            assertEquals(4,c.gallery.historyCache!!.second.entries.size)
            val expectedArchive=galleryAlbums(ids.take(4),c.gallery.historyCache!!.second,NovelAiHistoryFoldType.BASE)
            assertTrue(expectedArchive.isNotEmpty());assertTrue(expectedArchive.first().label.isNotBlank())
            route("asset-archive",{c.library.tab="回收站";c.view="资产管理"},::thumbs){ns->
                thumbs(ns)&&noPlaceholders(ns)&&c.thumbnails.pendingCount==0&&
                    ns.count{desc(it).contains("回收站资产 ")}==expectedArchive.size&&
                    ns.any{text(it).contains(expectedArchive.first().label.lineSequence().first())}&&
                    ns.none{text(it).contains("正在读取图片")}
            }
            fun canvas(ns:List<SemanticsNode>)=ns.any{it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().contains("排版编辑画布")&&it.config.getOrNull(SemanticsProperties.StateDescription)=="已更新"}
            route("editor",{c.view="编辑"},::canvas){ns->canvas(ns)&&ns.any{desc(it).contains("编辑缩略图")}&&noPlaceholders(ns)}
            fun result(ns:List<SemanticsNode>)=ns.any{it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().contains("图片")&&it.boundsInRoot.width>200&&it.boundsInRoot.height>200}
            route("creation-right",{c.view="创作"},::result){ns->result(ns)&&ns.none{text(it).contains("正在读取图片")}}
            assertNull(c.problem,c.problem)
            assertEquals(1,historyReads.get(),"Unchanged gallery/archive routes must share the successful projection")
            phase.set("original-verification")
            withContext(Dispatchers.IO){ids.forEachIndexed{i,id->assertEquals(hashes[i],sha256(w.bytes(id)))}}
            Files.writeString(out.resolve("routes.json"),buildJsonObject{
                put("images",12);put("uniqueSha256",hashes.toSet().size);put("generationJobs",4);put("generatedResults",8);put("archivedImages",4);put("editorPages",8)
                put("providerCalls",0);put("historyProjectionCalls",historyReads.get());put("measurements",JsonArray(rows));put("samples",JsonArray(samples.toList()));put("originalHashes",JsonArray(hashes.map{JsonPrimitive(it)}))
                put("scope","Offscreen production Workbench 1320x880; actual content semantics plus two confirming renders. Shared JVM/order/cache state; no OS cache flush. Thread-stack presence samples are not exclusive CPU durations. No native/DWM/input/human/RSS acceptance.")
            }.toString())
        }catch(e:Throwable){
            Files.writeString(out.resolve("failure-ui.json"),buildJsonObject{
                put("view",c.view);put("problem",c.problem);put("historyProblem",c.gallery.historyProblem)
                put("historyLoaded",c.gallery.historyCache!=null);put("foldLoaded",c.gallery.foldPreferences!=null);put("pending",c.thumbnails.pendingCount);put("retained",c.thumbnails.retainedBytes)
                fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                put("nodes",JsonArray(scene?.semanticsOwners.orEmpty().flatMap{walk(it.rootSemanticsNode)}.take(250).map{n->buildJsonObject{
                    put("desc",n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString("|"));put("text",n.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("|"){it.text}.take(400));put("state",n.config.getOrNull(SemanticsProperties.StateDescription));put("width",n.boundsInRoot.width);put("height",n.boundsInRoot.height)
                }}))
            }.toString())
            Files.writeString(out.resolve("failure.json"),buildJsonObject{put("phase",phase.get());put("error",e.toString());put("partial",JsonArray(rows));put("samples",JsonArray(samples.toList()))}.toString());throw e
        }finally{executor.shutdownNow();executor.awaitTermination(5,TimeUnit.SECONDS);scene?.close();c.close();scope.cancel()}
    }
}
