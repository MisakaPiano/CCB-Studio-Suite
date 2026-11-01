@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import com.example.chatbar.desktop.*
import com.example.chatbar.domain.image.*
import com.example.chatbar.ui.imageprompt.*
import io.github.misakapiano.ccbstudio.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.IOException
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class GalleryHistoryRouteCacheTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun routesReuseOnlyMatchingSuccessfulHistoryAndRetryAfterFailureOrIdentityChanges():Unit=runBlocking(awt) {
        val r=Path.of(System.getProperty("preview.test.root"),newId());val w=StudioWorkspace.openStudio(r.resolve("workspace"))
        val bytes=EditorRenderer.synthetic(32,32);val id=w.importImage("synthetic.png",bytes)
        val account=AccountProfile(alias="synthetic",service="NAI",provider="NOVELAI",endpoint="https://image.novelai.net",model=NovelAiImageModel.V4_5_FULL.apiId)
        val receipt=GenerationReceipt(recipe=NovelAiStudioDraft(basePrompt="synthetic").toRecipe(),account=account,request=buildJsonObject{put("input","synthetic")})
        val job=JobRecord(account=account,recipe=documentJson.encodeToString(receipt),results=listOf(id),state="COMPLETED")
        w.save(w.state.copy(jobs=listOf(job)))
        val scope=CoroutineScope(SupervisorJob()+awt)
        val c=WorkbenchController(w,scope,DesktopNovelAiInfrastructure(r.resolve("runtime")),r.resolve("budget"),admissionOverride=RequestAdmission{error("No HTTP")},pauseCollectionsOnStart=true)
        var reads=0
        c.gallery.historyReadOverride={reads++;readGalleryHistory(w)}
        var route by mutableStateOf(0);var observed:Result<GalleryHistoryProjection>?=null
        val scene=ImageComposeScene(640,480,coroutineContext=awt){
            key(route){val projection by rememberGalleryHistory(c);SideEffect{observed=projection};Text(if(projection?.isSuccess==true)"ready"else"waiting")}
        }
        suspend fun waitFor(done:()->Boolean){withTimeout(10000){do{scene.render(System.nanoTime()).close();yield();delay(1)}while(!done())}}
        try{
            waitFor{reads==1&&observed?.isSuccess==true}
            val first=observed!!.getOrThrow();assertEquals(1,first.entries.size)
            route++;observed=null;waitFor{observed?.isSuccess==true}
            assertEquals(1,reads);assertSame(first,observed!!.getOrThrow())
            c.gallery.historyReadOverride={reads++;throw IOException("synthetic failure")}
            c.run("synthetic job identity change"){w.save(w.state.copy(jobs=listOf(job,job.copy(id=newId(),results=emptyList()))))}
            c.active?.join();observed=null;waitFor{reads==2&&observed?.isFailure==true}
            assertNull(c.problem,c.problem)
            route++;observed=null;waitFor{reads==3&&observed?.isFailure==true}
            c.gallery.historyReadOverride={reads++;readGalleryHistory(w)}
            route++;observed=null;waitFor{reads==4&&observed?.isSuccess==true}
            assertEquals(1,observed!!.getOrThrow().entries.size)
            c.run("synthetic image identity change"){w.importImage("second.png",EditorRenderer.synthetic(40,40))}
            c.active?.join();observed=null;waitFor{reads==5&&observed?.isSuccess==true}
            assertContentEquals(bytes,w.bytes(id))
        }finally{scene.close();c.close();scope.cancel()}
    }
}
