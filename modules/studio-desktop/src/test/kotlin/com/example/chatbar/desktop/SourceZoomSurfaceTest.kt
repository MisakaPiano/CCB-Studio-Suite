@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package com.example.chatbar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.chatbar.data.local.entity.ChatMessage
import com.example.chatbar.data.local.entity.MessageRole
import com.example.chatbar.domain.image.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class SourceZoomSurfaceTest {
    private val awt = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) = SwingUtilities.invokeLater(block)
    }
    private val bytes = DesktopImageEditing.png(BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB).apply {
        for (x in 0 until width) for (y in 0 until height) setRGB(x, y, java.awt.Color(x * 6, y * 8, 100).rgb)
    })
    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun walk(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap(::walk)
        return semanticsOwners.flatMap { walk(it.rootSemanticsNode) }
    }
    private fun SemanticsNode.label(s: String) = config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s) == true ||
        config.getOrNull(SemanticsProperties.Text)?.any { it.text == s } == true
    private suspend fun ImageComposeScene.frames() { repeat(4) { render().close(); yield() }; delay(20); render().close() }
    private suspend fun ImageComposeScene.until(check: () -> Boolean) = withTimeout(5_000) { while (!check()) frames() }
    private suspend fun ImageComposeScene.tap(p: Offset) {
        sendPointerEvent(PointerEventType.Press, p, button = PointerButton.Primary)
        sendPointerEvent(PointerEventType.Release, p, button = PointerButton.Primary)
        frames()
    }
    private suspend fun ImageComposeScene.double(p: Offset) { tap(p); delay(70); tap(p); frames() }
    private fun ImageComposeScene.point(label: String) = nodes().first { it.label(label) }.boundsInRoot.center
    private fun ImageComposeScene.pixels() = render().use { it.encodeToData()!!.use { data -> data.bytes.contentHashCode() } }

    @Test fun `studio inline double click opens once while default zoom surface retains double zoom`() = runBlocking(awt) {
        var opened = 0
        val scene = ImageComposeScene(400, 300) {
            DesktopImageZoomSurface("saved", { bytes }, Modifier.fillMaxSize(), onOpenViewer = { opened++ })
        }
        try {
            scene.until { scene.nodes().any { it.label("图片") } }
            scene.double(Offset(200f, 150f)); scene.until { opened == 1 }
            scene.sendPointerEvent(PointerEventType.Scroll, Offset(200f, 150f), scrollDelta = Offset(0f, -4f))
            scene.frames(); assertEquals(1, opened)
        } finally { scene.close() }
        // Without the opt-in callback the existing Viewer interaction remains in-place.
        val viewer = ImageComposeScene(400, 300) { DesktopImageZoomSurface("saved", { bytes }, Modifier.fillMaxSize()) }
        try {
            viewer.until { viewer.nodes().any { it.label("图片") } }
            val fit = viewer.pixels()
            viewer.double(Offset(200f, 150f)); assertNotEquals(fit, viewer.pixels()); assertEquals(1, opened)
            viewer.double(Offset(200f, 150f)); assertEquals(fit, viewer.pixels())
        }
        finally { viewer.close() }
    }

}
