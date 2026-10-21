@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
package com.example.chatbar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext
import kotlin.test.*

class DesktopStudioWorkspaceTest {
    private val awt = object : CoroutineDispatcher() { override fun dispatch(context: CoroutineContext, block: Runnable) = SwingUtilities.invokeLater(block) }
    private suspend fun ImageComposeScene.frames() { repeat(8) { render().close(); yield() }; delay(60); render().close() }
    private fun ImageComposeScene.nodes(): List<SemanticsNode> {
        fun walk(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::walk)
        return semanticsOwners.flatMap { walk(it.rootSemanticsNode) }
    }
    private fun SemanticsNode.matches(s: String) = config.getOrNull(SemanticsProperties.ContentDescription)?.contains(s) == true ||
        config.getOrNull(SemanticsProperties.Text)?.any { it.text == s } == true
    private fun ImageComposeScene.bounds(s: String) = nodes().first { it.matches(s) }.boundsInRoot
    private suspend fun ImageComposeScene.click(s: String) {
        val target = nodes().first { it.matches(s) && it.config.getOrNull(SemanticsActions.OnClick) != null }.boundsInRoot.center
        sendPointerEvent(PointerEventType.Press, target, button = PointerButton.Primary)
        sendPointerEvent(PointerEventType.Release, target, button = PointerButton.Primary); frames()
    }
    private suspend fun ImageComposeScene.drag(from: Offset, to: Offset) {
        sendPointerEvent(PointerEventType.Press, from, button = PointerButton.Primary)
        for (step in 1..10) { sendPointerEvent(PointerEventType.Move, from + (to - from) * (step / 10f), button = PointerButton.Primary); frames() }
        sendPointerEvent(PointerEventType.Release, to, button = PointerButton.Primary); frames()
    }
    private fun ImageComposeScene.shot(name: String) = render().use { image ->
        val output = Path.of("build/phase7-slice-c-scheme-a-evidence/$name.png")
        Files.createDirectories(output.parent); image.encodeToData()!!.use { Files.write(output, it.bytes) }; Unit
    }

    @Test fun `tool navigation stays on one row with diagnostics at the right edge`() = runBlocking(awt) {
        for (width in listOf(320, 360, 720, 900, 1240, 1600)) {
            val state = DesktopStudioWorkspaceState()
            val scene = ImageComposeScene(width, 700) {
                DesktopStudioWorkspace(state,
                    diagnostics = { StatusText("Inspector fixture") },
                    compact = {}, editor = { StatusText("Editor fixture") },
                    footer = { StatusText("Footer fixture") }, preview = {})
            }
            try {
                scene.frames()
                val studio = scene.bounds("NovelAI Studio")
                val diagnostics = scene.bounds("高级 / 诊断")
                val editor = scene.bounds("Studio Prompt 编辑区")
                assertEquals(studio.center.y, diagnostics.center.y, 1f)
                assertEquals(editor.left, studio.left, 1f)
                assertEquals(editor.right, diagnostics.right, 1f)
                assertTrue(studio.right <= diagnostics.left)
                assertEquals(1, scene.nodes().count { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("高级 / 诊断") == true })
                scene.click("高级 / 诊断")
                assertTrue(state.diagnostics)
                assertTrue(scene.nodes().any { it.matches("Inspector fixture") })
                scene.click("NovelAI Studio")
                assertFalse(state.diagnostics)
                assertTrue(scene.nodes().any { it.matches("Editor fixture") })
            } finally { scene.close() }
        }
    }

    @Test fun `real divider drag snaps restores and clamps while editor composition survives mode and resize`() = runBlocking(awt) {
        val state = DesktopStudioWorkspaceState()
        var text = ""
        val scene = ImageComposeScene(1240, 800) {
            DesktopStudioWorkspace(state,
                compact = { expand -> StudioAction("展开预览", onClick = expand) },
                editor = { var input by remember { mutableStateOf("draft") }; StudioField("本地草稿", input) { input = it; text = it } },
                footer = { StatusText("Generate") }, preview = { StatusText("本地预览") })
        }
        try {
            scene.frames()
            val field = scene.nodes().first { it.config.getOrNull(SemanticsActions.SetText) != null }
            field.config[SemanticsActions.SetText].action!!.invoke(androidx.compose.ui.text.AnnotatedString("unsent")); scene.frames()
            val initial = scene.bounds("调整预览宽度")
            scene.drag(initial.center, initial.center - Offset(120f, 0f))
            val enlarged = scene.bounds("Studio 全高预览面板").width
            assertTrue(enlarged > 680f)
            scene.drag(scene.bounds("调整预览宽度").center, Offset(1130f, 400f))
            assertEquals("仅缩略图", state.mode); assertEquals(116f, scene.bounds("Studio 全高预览面板").width, 1f)
            scene.click("展开预览")
            assertTrue(scene.bounds("Studio 全高预览面板").width >= 300f)
            // Explicit modes keep the last expanded width; drag has intentionally changed it.
            val expandedWidth = scene.bounds("Studio 全高预览面板").width
            scene.click("预览 · 展开预览 ▾"); scene.click("预览专注"); scene.click("返回编辑")
            assertEquals(expandedWidth, scene.bounds("Studio 全高预览面板").width, 1f)
            assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "unsent" })
            scene.constraints = Constraints.fixed(360, 620); scene.frames(); scene.click("展开预览"); scene.click("收起预览")
            scene.constraints = Constraints.fixed(900, 700); scene.frames()
            assertTrue(scene.bounds("Studio 全高预览面板").width in 300f..568f)
            assertEquals("unsent", text)
            assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "unsent" })
        } finally { scene.close() }
    }
}
