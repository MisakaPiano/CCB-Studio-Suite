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

class OfflineMaintenanceUpdateTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    @Test fun maintenanceStatusClearsOldNoticeAndShowsNoOnlineOrInstallActions()=runBlocking(awt) {
        val cache=Path.of(System.getProperty("preview.test.root"),java.util.UUID.randomUUID().toString())
        Files.createDirectories(cache);Files.writeString(cache.resolve("pending-notice.txt"),"synthetic-old-notice")
        val scope=CoroutineScope(SupervisorJob()+awt);var calls=0;var installs=0
        val c=StudioUpdateController(scope,cache){calls++;buildJsonObject{put("status","LOCAL_MAINTENANCE")}}
        c.checkOnStartup();c.job!!.join();assertEquals(1,calls);assertNull(c.pendingVersion);assertFalse(c.showStartupNotice)
        assertFalse(Files.exists(cache.resolve("pending-notice.txt")));assertFalse(c.message.contains("失败"));assertFalse(c.message.contains("最新"))
        val scene=ImageComposeScene(1050,700,coroutineContext=coroutineContext)
        try {
            scene.setContent{MaterialTheme(colors=darkColors(primary=Gold,background=Ink,surface=Panel)){
                Surface(color=Ink,contentColor=com.example.chatbar.desktop.DesktopBootstrapColors.foreground){UpdateWorkspace(c,"synthetic-workspace",onInstall={installs++})}
            }}
            suspend fun frames(){repeat(4){scene.render().close();delay(30)}}
            fun nodes():List<SemanticsNode>{
                fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
            }
            frames()
            val texts=nodes().flatMap{it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.map{it.text}
            assertTrue(texts.any{it.contains("本机维护版")});assertTrue(texts.any{it.contains("离线安装包")})
            for(label in listOf("检查更新","确认下载并验证","安装并重启"))assertFalse(label in texts)
            scene.sendKeyEvent(KeyEvent(Key.F5,KeyEventType.KeyDown));scene.sendKeyEvent(KeyEvent(Key.F5,KeyEventType.KeyUp));frames()
            assertEquals(1,calls);assertEquals(0,installs);c.checkOnStartup();assertEquals(1,calls)
            val output=Path.of("build/offline-maintenance-update.png");Files.createDirectories(output.parent)
            scene.render().use{image->image.encodeToData()!!.use{Files.write(output,it.bytes)}}
            Unit
        }finally{scene.close();c.closeAndDrain();scope.cancel()}
    }
}
