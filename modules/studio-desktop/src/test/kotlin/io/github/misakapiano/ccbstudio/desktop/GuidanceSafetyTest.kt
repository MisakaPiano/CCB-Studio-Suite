@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import io.github.misakapiano.ccbstudio.core.*
import java.awt.image.BufferedImage
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*
import kotlinx.coroutines.*

class GuidanceSafetyTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),newId())
    private fun png(w:Int=320,h:Int=256)=DesktopImageEditing.png(BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB).also{im->for(y in 0 until h)for(x in 0 until w)im.setRGB(x,y,0xff000000.toInt() or ((x%256) shl 16) or ((y%256) shl 8) or ((x+y)%256))})
    private suspend fun ImageComposeScene.frames(){repeat(6){render().close();yield()};delay(100);render().close()}
    private fun ImageComposeScene.nodes():List<SemanticsNode>{
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    private suspend fun ImageComposeScene.click(text:String){val n=nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==text}==true && it.config.getOrNull(SemanticsActions.OnClick)!=null};assertTrue(n.config[SemanticsActions.OnClick].action!!.invoke());frames();delay(100);frames()}
    private fun ImageComposeScene.shot(name:String){val p=Path.of("build/ux-r1b-guidance/$name.png");Files.createDirectories(p.parent);render().use{im->im.encodeToData()!!.use{Files.write(p,it.bytes)}}}
    @Test fun q09CanvasRotateUndoMaskResetApplyAndReopen()=runBlocking(awt){
        val r=root();val bytes=png();val originalHash=sha256(bytes);val scope=CoroutineScope(SupervisorJob()+awt);val w=StudioWorkspace.openStudio(r)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(root()),root(),admissionOverride=RequestAdmission{})
        val id=w.importImage("Q09-original.png",bytes);c.useImage(id,NovelAiImageUseTarget.INPAINT);c.active!!.join();assertNull(c.problem)
        val initial=NovelAiFocusedInpaintRegion(.1f,.2f,.7f,.6f)
        var applied:Triple<ByteArray,ByteArray?,NovelAiFocusedInpaintRegion>?=null
        val scene=ImageComposeScene(920,900){MaterialTheme(colors=darkColors()){DesktopImageToolsDialog(bytes,c.picker,{},onGuidanceApply={b,m,region->applied=Triple(b,m,region);true},initialRegion=initial,container={_,_,body->body()})}}
        try{
            scene.frames();repeat(4){scene.click("旋转 90°")};scene.click("应用");assertContentEquals(DesktopImageEditing.decode(bytes).pixels(),DesktopImageEditing.decode(applied!!.first).pixels());assertEquals(initial.x,applied!!.third.x,.00001f)
            scene.click("区域打码");scene.click("应用");assertFalse(bytes.contentEquals(applied!!.first));scene.click("撤销");scene.click("重做");scene.click("重置");scene.click("应用");assertContentEquals(bytes,applied!!.first)
            scene.click("绘制蒙版")
            val bounds=scene.nodes().first{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("图像工具画布")==true}.boundsInRoot
            val from=bounds.topLeft+Offset(bounds.width*.3f,bounds.height*.4f);val to=bounds.topLeft+Offset(bounds.width*.6f,bounds.height*.6f)
            scene.sendPointerEvent(PointerEventType.Press,from,button=PointerButton.Primary)
            repeat(5){scene.sendPointerEvent(PointerEventType.Move,from+(to-from)*((it+1)/5f),button=PointerButton.Primary);scene.frames()}
            scene.sendPointerEvent(PointerEventType.Release,to,button=PointerButton.Primary);scene.frames();scene.click("应用");assertNotNull(applied!!.second)
            scene.shot("mask-and-focus");scene.click("清除蒙版");scene.click("应用");assertNull(applied!!.second);scene.click("撤销");scene.click("应用");assertNotNull(applied!!.second)
            // Use a valid full focused region for the actual journal operation; tiny focus remains a visible validation error.
            assertTrue(c.applyGuidanceTools(id,applied!!.first,applied!!.second,NovelAiFocusedInpaintRegion(0f,0f,1f,1f)))
            assertEquals(originalHash,sha256(w.bytes(id)));assertNotNull(w.state.creation!!.draft.imageGuidance.maskImage)
            c.saveToolCopy(id,DesktopImageEditing.png(DesktopImageTools.rotate(DesktopImageEditing.decode(bytes))));assertEquals(1,w.state.savedCopies.size)
        }finally{scene.close();c.close();scope.cancel()}
        StudioWorkspace.openStudio(r).use{again->assertEquals(originalHash,sha256(again.bytes(id)));assertNotNull(again.state.creation!!.draft.imageGuidance.maskImage);assertEquals(1,again.state.savedCopies.size);assertEquals(256,DesktopImageEditing.decode(again.bytes(again.state.savedCopies.single())).width)}
    }

    @Test fun focusedPreparationUsesAuthorityPixelsAndKeepsRawFinalSeparate()=runBlocking<Unit> {
        val original=png(512,512);val id=newId();val ref=NovelAiStudioAssetRef(id=id,path="asset:$id",sha256=sha256(original),width=512,height=512)
        val draft=NovelAiStudioDraft(basePrompt="fixture",imageGuidance=NovelAiImageGuidanceDraft(action=NovelAiGenerationAction.INPAINT,baseImage=ref,focusedInpaintRegion=NovelAiFocusedInpaintRegion(.1f,.1f,.8f,.8f)))
        var calls=0;val client=studioHttpClient()
        try {
            val adapter=DesktopNovelAiGuidance(root(),{path->assertEquals(ref.path,path);original},{error("No token needed")},RequestAdmission{calls++},client)
            val prepared=adapter.prepare(draft)
            val plan=NovelAiFocusedInpaintPlanner.plan(512,512,draft.imageGuidance.focusedInpaintRegion!!,96)
            assertEquals(plan.requestSize,prepared.requestSize);assertNotNull(prepared.guidance.maskBase64)
            val final=png(plan.requestSize.width,plan.requestSize.height);val result=prepared.compose(final)
            assertEquals(512,DesktopImageEditing.decode(result).width);assertEquals(0,calls)
            val source=DesktopImageEditing.decode(original);val output=DesktopImageEditing.decode(result)
            assertEquals(source.getRGB(0,0),output.getRGB(0,0));assertEquals(source.getRGB(511,511),output.getRGB(511,511));assertContentEquals(original,png(512,512))
            val broken=ref.copy(sha256="bad")
            assertFails{adapter.prepare(draft.copy(imageGuidance=draft.imageGuidance.copy(baseImage=broken)))}
        }finally{client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
    }
    @Test fun vibeMissUsesBudgetOnceAndCachedEncodingMakesNoRequest()=runBlocking {
        val bytes=png();val ref=NovelAiStudioAssetRef(path="asset:${newId()}",sha256=sha256(bytes),width=320,height=256)
        val requests=java.util.concurrent.atomic.AtomicInteger();var budget=0
        val server=com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/"){e->requests.incrementAndGet();val body=e.requestBody.readBytes().decodeToString();assertTrue(body.contains("information_extracted"));val response=byteArrayOf(11,22,33,44);e.sendResponseHeaders(200,4);e.responseBody.use{it.write(response)}};server.start()
        val client=studioHttpClient().newBuilder().addInterceptor{chain->chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.address.port}/").removeHeader("Authorization").build())}.build()
        try {
            val adapter=DesktopNovelAiGuidance(root(),{bytes},{"LOCAL_TEST_ONLY"},RequestAdmission{assertEquals("NAI",it);budget++},client)
            val draft=NovelAiStudioDraft(imageGuidance=NovelAiImageGuidanceDraft(referenceMode=NovelAiReferenceMode.VIBE,vibes=listOf(NovelAiVibeReferenceDraft(asset=ref))))
            assertEquals(1,adapter.vibeCacheMisses(draft));val first=adapter.prepare(draft);assertEquals(1,budget);assertEquals(1,requests.get());assertEquals(0,adapter.vibeCacheMisses(draft))
            val second=adapter.prepare(draft);assertEquals(first.guidance,second.guidance);assertEquals(1,budget);assertEquals(1,requests.get())
        }finally{server.stop(0);client.dispatcher.executorService.shutdown();client.connectionPool.evictAll()}
    }
}
