@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class,androidx.compose.ui.InternalComposeUiApi::class)
package io.github.misakapiano.ccbstudio.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.*

/** Pixel regression for the white-on-white user screenshot; no workspace/provider use. */
class StudioAppearanceTextActionTest {
    private val awt=object:CoroutineDispatcher(){override fun dispatch(context:CoroutineContext,block:Runnable)=SwingUtilities.invokeLater(block)}
    private fun ImageComposeScene.nodes():List<SemanticsNode> {
        fun walk(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::walk)
        return semanticsOwners.flatMap{walk(it.rootSemanticsNode)}
    }
    @Test fun actualThemeActionsStayReadableAndClickableForBlackAndWhiteAccentsOnBothModes()=runBlocking {withContext(awt) {
        val previous=StudioAppearanceRuntime.palette
        val labels=listOf("重新读取","应用颜色","恢复 Studio 默认","关闭")
        try {for(mode in listOf(StudioThemeMode.LIGHT,StudioThemeMode.DARK))for(rgb in listOf(0,0xffffff)) {
            val palette=studioSemanticColors(StudioDisplaySettings(mode,StudioColorStyle.CUSTOM_ACCENT,rgb),false)
            StudioAppearanceRuntime.palette=palette
            val clicked=mutableListOf<String>();var actualPrimary=Color.Transparent
            val scene=ImageComposeScene(360,220,coroutineContext=coroutineContext)
            try {
                scene.setContent {StudioTheme {actualPrimary=MaterialTheme.colors.primary;Surface(color=palette.background) {
                    Column {labels.forEach {label->StudioAppearanceTextAction(label){clicked+=label}}}
                }}}
                repeat(4){scene.render(System.nanoTime()).close();yield()};delay(40)
                assertEquals(Color(0xff000000.toInt() or rgb),actualPrimary,"Original user RGB fill must remain unchanged")
                val nodes=scene.nodes()
                val pixels=scene.render(System.nanoTime()).use{image->image.encodeToData()!!.use{ImageIO.read(ByteArrayInputStream(it.bytes))}}
                for(label in labels) {
                    val text=nodes.first{it.config.getOrNull(SemanticsProperties.Text)?.any{t->t.text==label}==true}
                    val bounds=text.boundsInRoot;var readable=0
                    for(y in floor(bounds.top).toInt().coerceAtLeast(0) until ceil(bounds.bottom).toInt().coerceAtMost(pixels.height))
                        for(x in floor(bounds.left).toInt().coerceAtLeast(0) until ceil(bounds.right).toInt().coerceAtMost(pixels.width))
                            if(studioContrastRatio(Color(pixels.getRGB(x,y)),palette.background)>=4.5)readable++
                    assertTrue(readable>=3,"$label unreadable for $mode/#%06X: $readable foreground pixels".format(rgb))
                }
                val buttons=nodes.filter{it.config.getOrNull(SemanticsProperties.Role)==Role.Button}
                assertEquals(4,buttons.size)
                buttons.forEach{assertTrue(it.config[SemanticsActions.OnClick].action!!.invoke())}
                assertEquals(labels,clicked)
            } finally {scene.close()}
        }} finally {StudioAppearanceRuntime.palette=previous}
    }}
}
