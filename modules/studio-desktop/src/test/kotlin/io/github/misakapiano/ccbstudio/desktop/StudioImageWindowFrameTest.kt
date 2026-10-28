@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.material.Text
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.toArgb
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class StudioImageWindowFrameTest {
    @Test fun titleBarFollowsThemeAndOnlyTheMaximizeButtonChangesToRestore()=runBlocking(object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}){
        val previous=StudioAppearanceRuntime.palette
        try{for(mode in listOf(StudioThemeMode.DARK,StudioThemeMode.LIGHT)){
            StudioAppearanceRuntime.palette=studioSemanticColors(StudioDisplaySettings(mode),false)
            val commands=mutableListOf<DesktopWindowCommand>();val chrome=DesktopWindowChrome{commands+=DesktopWindowCommand.CLOSE}
            val scene=ImageComposeScene(700,400){StudioTheme{StudioImageWindowFrame("Synthetic title",chrome){Text("Synthetic image content")}}}
            fun nodes():List<SemanticsNode>{
                fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
                return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
            }
            fun labels()=nodes().flatMap{it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()}
            suspend fun frames(){repeat(3){androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications();scene.render(System.nanoTime()).close();yield()};delay(60)}
            try{
                frames();assertTrue("最大化" in labels());assertFalse("还原" in labels())
                scene.render(System.nanoTime()).use{image->image.encodeToData()!!.use{data->assertEquals(Panel.toArgb(),ImageIO.read(ByteArrayInputStream(data.bytes)).getRGB(450,20))}}
                chrome.updateState{it.copy(maximized=true)};frames();assertTrue("还原" in labels());assertFalse("最大化" in labels())
                val close=nodes().single{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("关闭")==true&&it.config.getOrNull(SemanticsActions.OnClick)!=null}
                assertTrue(close.config[SemanticsActions.OnClick].action!!.invoke());assertEquals(listOf(DesktopWindowCommand.CLOSE),commands)
            }finally{scene.close()}
        }}finally{StudioAppearanceRuntime.palette=previous}
    }
}
