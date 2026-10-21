@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
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
        val scope=CoroutineScope(SupervisorJob()+awt);val calls=mutableListOf<Boolean>()
        val controller=StudioUpdateController(scope,root()){download->calls+=download;buildJsonObject {
            put("status",if(download)"DOWNLOADED_VERIFIED" else "AVAILABLE");put("displayVersion","0.2.0-internal");put("channel","internal");put("notes","合成回执：不执行安装");put("sourceSha","a".repeat(40));put("bytes",1234)
        }}
        withContext(awt) {
            val scene=ImageComposeScene(900,700,coroutineContext=coroutineContext)
            try {
                scene.setContent {MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)) {
                    Surface(color=Ink,contentColor=com.example.chatbar.desktop.DesktopBootstrapColors.foreground){UpdateWorkspace(controller)}
                }}
                suspend fun frames(){repeat(4){scene.render().close();delay(30)}}
                fun nodes():List<SemanticsNode>{
                    fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                    return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
                }
                suspend fun click(text:String){nodes().first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==text}==true && it.config.getOrNull(SemanticsActions.OnClick)!=null}.config[SemanticsActions.OnClick].action!!.invoke();frames();controller.job?.join();frames()}
                frames();assertTrue(calls.isEmpty());click("检查更新");assertEquals(listOf(false),calls)
                assertTrue(controller.message.contains("尚未下载或安装"));click("确认下载并验证");assertEquals(listOf(false,true),calls)
                assertTrue(controller.message.contains("尚未安装或重启"))
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
}
