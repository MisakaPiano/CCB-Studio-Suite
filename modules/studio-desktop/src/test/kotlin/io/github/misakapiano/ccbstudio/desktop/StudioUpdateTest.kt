@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.nio.file.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class StudioUpdateTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun root()=Path.of(System.getProperty("preview.test.root"),java.util.UUID.randomUUID().toString())
    @Test fun actualUpdateScreenRequiresDownloadConfirmationAndDoesNotClaimInstall()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+awt);val calls=mutableListOf<Boolean>();var installs=0
        val controller=StudioUpdateController(scope,root()){download->calls+=download;buildJsonObject {
            put("status",if(download)"DOWNLOADED_VERIFIED" else "AVAILABLE");put("displayVersion","0.2.0-internal");put("channel","internal");put("notes","合成回执：不执行安装");put("sourceSha","a".repeat(40));put("bytes",1234)
        }}
        withContext(awt) {
            val scene=ImageComposeScene(900,700,coroutineContext=coroutineContext)
            try {
                scene.setContent {MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)) {
                    Surface(color=Ink,contentColor=com.example.chatbar.desktop.DesktopBootstrapColors.foreground){UpdateWorkspace(controller){installs++}}
                }}
                suspend fun frames(){repeat(4){scene.render().close();delay(30)}}
                fun nodes():List<SemanticsNode>{
                    fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                    return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
                }
                suspend fun click(text:String){nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==text}==true && it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames();controller.job?.join();frames()}
                frames();assertTrue(calls.isEmpty());scene.sendKeyEvent(KeyEvent(Key.F5,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(Key.F5,KeyEventType.KeyUp));frames();controller.job?.join();frames();assertEquals(listOf(false),calls)
                assertTrue(controller.message.contains("尚未下载或安装"));scene.sendKeyEvent(KeyEvent(Key.D,KeyEventType.KeyDown,isCtrlPressed=true));scene.sendKeyEvent(KeyEvent(Key.D,KeyEventType.KeyUp,isCtrlPressed=true));frames();controller.job?.join();frames();assertEquals(listOf(false,true),calls)
                assertEquals("下载完成，安全校验已通过",controller.message)
                assertTrue(nodes().any{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text.contains("目前尚未安装")}==true})
                click("安装并重启");assertEquals(0,installs);click("暂不安装");assertEquals(0,installs)
                click("安装并重启");click("保存后安装");assertEquals(1,installs)
                val out=Path.of("build/ux-r4-client/update-download.png");Files.createDirectories(out.parent)
                scene.render().use{image->image.encodeToData()!!.use{Files.write(out,it.bytes)}}
            } finally{scene.close();controller.closeAndDrain()}
        }
        scope.cancel()
    }
    @Test fun cancellationDrainsOwnedOperationAndRejectsNewWork()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);val entered=CompletableDeferred<Unit>();val done=CompletableDeferred<Unit>();var count=0
        val c=StudioUpdateController(scope,root()){count++;entered.complete(Unit);try{awaitCancellation()}finally{done.complete(Unit)}}
        c.check();entered.await();c.check();c.closeAndDrain();assertTrue(done.isCompleted);assertFalse(c.busy);c.check();assertEquals(1,count);assertTrue(c.message.contains("已取消"));scope.cancel()
    }
    @Test fun unconfiguredRuntimeFailsWithoutLaunchingProcess()=runBlocking {
        val root=root();assertFailsWith<IllegalArgumentException>{StudioUpdateClient(root,root.resolveSibling("unused-updates")).check()};assertFalse(Files.exists(root))
    }
    @Test fun currentVersionIsSuccessAndFailureClearsStaleInstallCandidate()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+awt);var status="DOWNLOADED_VERIFIED"
        val c=StudioUpdateController(scope,root()){buildJsonObject{put("status",status)}}
        withContext(awt) {
            c.check();c.job!!.join();assertEquals("DOWNLOADED_VERIFIED",c.result!!["status"]!!.jsonPrimitive.content)
            status="UP_TO_DATE";c.check();assertNull(c.result);c.job!!.join();assertEquals("已是最新版本",c.message)
            status="FAILED";c.check();assertNull(c.result);c.job!!.join();assertTrue(c.message.startsWith("更新未完成"))
            c.closeAndDrain()
        };scope.cancel()
    }
    @Test fun realByteProgressHasSeparateVerificationAndRejectsImpossibleCounts()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+awt);val c=StudioUpdateController(scope,root()){error("unused")}
        withContext(awt) {
            fun progress(phase:String,received:Long,total:Long)=buildJsonObject{put("phase",phase);put("received",received);put("total",total);put("bytesPerSecond",100)}
            c.receiveProgress(progress("DOWNLOADING",50,100));assertEquals("正在下载安装包…",c.message)
            assertFailsWith<IllegalArgumentException>{c.receiveProgress(progress("DOWNLOADING",40,100))}
            assertFailsWith<IllegalArgumentException>{c.receiveProgress(progress("DOWNLOADING",101,100))}
            c.receiveProgress(progress("VERIFYING",100,100));assertTrue(c.message.contains("正在校验"));assertNull(c.result)
            assertEquals("1.5 MiB",updateSize(1572864));c.closeAndDrain()
        };scope.cancel()
    }
}
