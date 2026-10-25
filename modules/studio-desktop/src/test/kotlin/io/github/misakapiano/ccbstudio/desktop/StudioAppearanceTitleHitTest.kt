@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.test.*

/** Uses the production control and native hit dispatcher; not a Windows/user acceptance claim. */
class StudioAppearanceTitleHitTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private suspend fun ImageComposeScene.frames(){repeat(4){render(System.nanoTime()).close();yield()};delay(40)}
    private fun ImageComposeScene.nodes():List<SemanticsNode> {
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }

    @Test fun productionGearHitStaysClientAndPointerClickReachesActionAtNarrowWideAndScaledSizes()=runBlocking {withContext(awt) {
        for(width in listOf(760,1320))for(scale in listOf(1f,1.5f,2f)) {
            val bridge=DesktopWindowChrome{};val measured=WorkbenchChrome(bridge);var opened=0
            val sceneWidth=(width*scale).roundToInt();val sceneHeight=(100*scale).roundToInt()
            val scene=ImageComposeScene(sceneWidth,sceneHeight,coroutineContext=coroutineContext)
            try {
                scene.setContent {CompositionLocalProvider(LocalDensity provides Density(scale)){MaterialTheme {
                    Row(Modifier.fillMaxWidth().height(44.dp).onGloballyPositioned {
                        measured.width=sceneWidth.toFloat();measured.height=sceneHeight.toFloat()
                        measured.title=measured.rect(it);measured.publish()
                    }) {
                        Spacer(Modifier.weight(1f))
                        StudioAppearanceTitleButton(measured){opened++}
                        listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE).forEach {hit->
                            StudioCaptionButton(hit,DesktopChromeState(),false,Modifier.onGloballyPositioned{
                                measured.captions[hit]=measured.rect(it);measured.publish()
                            }){}
                        }
                    }
                }}}
                scene.frames()
                val gear=scene.nodes().single{it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("显示主题")==true}
                val center=gear.boundsInRoot.center
                assertTrue(measured.buttons.containsKey("display-settings"),"Production gear must register native interactive bounds")
                for(maximized in listOf(false,true)) {
                    assertEquals(ChromeHit.CLIENT,DesktopChromeHitTest.hit(center.x,center.y,sceneWidth.toFloat(),sceneHeight.toFloat(),6f,maximized,bridge.layout))
                    assertEquals(ChromeHit.CAPTION,DesktopChromeHitTest.hit(center.x-60*scale,center.y,sceneWidth.toFloat(),sceneHeight.toFloat(),6f,maximized,bridge.layout))
                    val close=requireNotNull(measured.captions[ChromeHit.CLOSE])
                    assertEquals(ChromeHit.CLOSE,DesktopChromeHitTest.hit((close.left+close.right)/2,(close.top+close.bottom)/2,sceneWidth.toFloat(),sceneHeight.toFloat(),6f,maximized,bridge.layout))
                }
                scene.sendPointerEvent(PointerEventType.Press,center,button=PointerButton.Primary)
                scene.sendPointerEvent(PointerEventType.Release,center,button=PointerButton.Primary)
                scene.frames();assertEquals(1,opened,"Real pointer must reach gear action at $width/$scale")
            } finally {scene.close()}
        }
    }}

    @Test fun disposingGearRemovesItsInteractiveRectangleInsteadOfLeavingAStaleTitlebarTarget()=runBlocking {withContext(awt) {
        val bridge=DesktopWindowChrome{};val measured=WorkbenchChrome(bridge);var shown by mutableStateOf(true)
        val scene=ImageComposeScene(400,100,coroutineContext=coroutineContext)
        try {
            scene.setContent {MaterialTheme {Row(Modifier.fillMaxWidth().height(44.dp).onGloballyPositioned {
                measured.width=400f;measured.height=100f;measured.title=measured.rect(it);measured.publish()
            }) {Spacer(Modifier.weight(1f));if(shown)StudioAppearanceTitleButton(measured){} }}}
            scene.frames();val bounds=requireNotNull(measured.buttons["display-settings"])
            val p=Offset((bounds.left+bounds.right)/2,(bounds.top+bounds.bottom)/2)
            assertEquals(ChromeHit.CLIENT,DesktopChromeHitTest.hit(p.x,p.y,400f,100f,6f,true,bridge.layout))
            shown=false;scene.frames()
            assertFalse(measured.buttons.containsKey("display-settings"))
            assertEquals(ChromeHit.CAPTION,DesktopChromeHitTest.hit(p.x,p.y,400f,100f,6f,true,bridge.layout))
        } finally {scene.close()}
    }}
}
