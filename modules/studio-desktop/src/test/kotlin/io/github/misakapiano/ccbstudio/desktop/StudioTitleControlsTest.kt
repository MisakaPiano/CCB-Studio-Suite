@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.example.chatbar.desktop.*
import kotlinx.coroutines.*
import java.nio.file.*
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.*

class StudioTitleControlsTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return scene.semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    @Test fun captionGeometryAndCommandsAtFourDisplayScales()=runBlocking {withContext(awt) {
        for(scale in listOf(1f,1.25f,1.5f,2f))for(maximized in listOf(false,true)) {
            val commands=mutableListOf<DesktopWindowCommand>()
            // Compose rounds each button independently; the fixture must allow all three widths.
            val scene=ImageComposeScene((46*scale).roundToInt()*3,(44*scale).roundToInt(),coroutineContext=coroutineContext)
            try {
                scene.setContent {CompositionLocalProvider(LocalDensity provides Density(scale)) {
                    Row(Modifier.height(44.dp).background(Panel)) {
                        listOf(ChromeHit.MINIMIZE,ChromeHit.MAXIMIZE,ChromeHit.CLOSE).forEach {StudioCaptionButton(it,DesktopChromeState(maximized=maximized),false,onCommand=commands::add)}
                    }
                }}
                repeat(3){scene.render().close();delay(20)}
                val buttons=nodes(scene).filter{it.config.getOrNull(SemanticsProperties.Role)==Role.Button}
                assertEquals(3,buttons.size)
                buttons.forEach {assertEquals(46*scale,it.boundsInRoot.width,1f);it.config[SemanticsActions.OnClick].action!!.invoke()}
                assertEquals(listOf(DesktopWindowCommand.MINIMIZE,if(maximized)DesktopWindowCommand.RESTORE else DesktopWindowCommand.MAXIMIZE,DesktopWindowCommand.CLOSE),commands)
                val output=Path.of("build/ux-update-feedback/captions-${scale}-${maximized}.png");Files.createDirectories(output.parent)
                scene.render().use{image->image.encodeToData()!!.use{Files.write(output,it.bytes)}}
                val pixels=ImageIO.read(output.toFile())
                val widths=buttons.map {button->
                    val bounds=button.boundsInRoot
                    val points=(bounds.left.toInt() until bounds.right.toInt()).flatMap{x->(0 until pixels.height).filter{y->
                        // A 1 dp stroke on an integer pixel edge has half-coverage antialiasing.
                        // Count visible coverage above the dark background, not only near-white pixels.
                        val rgb=pixels.getRGB(x,y);(rgb shr 16 and 255)>80 && (rgb shr 8 and 255)>80 && (rgb and 255)>80
                    }.map{y->x to y}}
                    assertTrue(points.isNotEmpty(),"No visible glyph at $scale in $bounds")
                    val left=points.minOf{it.first};val right=points.maxOf{it.first}
                    assertTrue(abs((left+right)/2f-bounds.center.x)<=1.5f*scale)
                    right-left+1
                }
                assertTrue(widths.max()-widths.min()<=2*scale,"Unequal optical width: $widths at $scale")
            } finally{scene.close()}
        }
    }}
    @Test fun narrowNavigationKeepsEveryRouteAccessibleWithoutTextOverflow()=runBlocking {withContext(awt) {
        for(compact in listOf(false,true)) {
            val clicks=mutableListOf<String>();val width=if(compact)252 else 616
            val scene=ImageComposeScene(width,44,coroutineContext=coroutineContext)
            try {
                scene.setContent {MaterialTheme {Row {studioRoutes.forEach{label->StudioRouteButton(label,label=="更新",compact){clicks+=label}}}}}
                repeat(3){scene.render().close();delay(20)}
                val tabs=nodes(scene).filter{it.config.getOrNull(SemanticsProperties.Role)==Role.Tab}
                assertEquals(7,tabs.size)
                tabs.forEach {assertTrue(it.boundsInRoot.right<=width);it.config[SemanticsActions.OnClick].action!!.invoke()}
                assertEquals(studioRoutes,clicks)
                assertEquals(1,tabs.count{it.config.getOrNull(SemanticsProperties.Selected)==true})
                val output=Path.of("build/ux-update-feedback/navigation-$compact.png");Files.createDirectories(output.parent)
                scene.render().use{image->image.encodeToData()!!.use{Files.write(output,it.bytes)}}
            } finally{scene.close()}
        }
    }}
}
