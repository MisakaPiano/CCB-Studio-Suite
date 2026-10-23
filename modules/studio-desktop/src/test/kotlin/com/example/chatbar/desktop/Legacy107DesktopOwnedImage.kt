// Exact Suite107 display baseline, tests only. Original source91aa9be.
package com.example.chatbar.desktop

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun Legacy107DesktopOwnedImage(reference: String, read: (String) -> ByteArray,
    modifier: Modifier = Modifier, crop: Boolean = false, onDimensions: ((IntSize) -> Unit)? = null,
    minimumDisplayWidthPx: Int? = null,
    displayTargetForSource: ((IntSize) -> IntSize)? = null) {
    var error by remember(reference) { mutableStateOf<String?>(null) }
    val displayed by produceState<Legacy107DesktopDisplayedImage?>(null, reference, minimumDisplayWidthPx, displayTargetForSource) {
        try {
            withContext(Dispatchers.IO) {
                val bytes = read(reference)
                require(bytes.size <= com.example.chatbar.domain.image.ApngDisguiseCodec.MAX_OUTPUT_BYTES)
                if (playDesktopApng(bytes) { frame, duration ->
                    value = Legacy107DesktopDisplayedImage(Legacy107desktopDisplayBitmap(frame, minimumDisplayWidthPx),
                        IntSize(frame.width, frame.height)); delay(duration)
                }) return@withContext
                org.jetbrains.skia.Data.makeFromBytes(bytes).use { data ->
                    org.jetbrains.skia.Codec.makeFromData(data).use { codec ->
                        require(codec.width.toLong() * codec.height <= DesktopImageEditing.MAX_PIXELS)
                        if (codec.frameCount <= 1) value = Legacy107desktopDisplayBitmap(bytes, minimumDisplayWidthPx, displayTargetForSource)
                        else org.jetbrains.skia.Bitmap().use { frame ->
                            check(frame.allocPixels(codec.imageInfo))
                            var loops = 0
                            do {
                                for (index in 0 until codec.frameCount) {
                                    currentCoroutineContext().ensureActive()
                                    codec.readPixels(frame, index)
                                    value = Legacy107DesktopDisplayedImage(org.jetbrains.skia.Image.makeFromBitmap(frame).toComposeImageBitmap(),
                                        IntSize(codec.width, codec.height))
                                    delay(codec.getFrameInfo(index).duration.coerceAtLeast(10).toLong())
                                }
                                loops++
                            } while (codec.repetitionCount < 0 || loops <= codec.repetitionCount)
                        }
                    }
                }
            }
        }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "图片无法读取" }
    }
    val dimensionsChanged by rememberUpdatedState(onDimensions)
    LaunchedEffect(reference, displayed?.sourceDimensions) {
        displayed?.let { dimensionsChanged?.invoke(it.sourceDimensions) }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        displayed?.let { Image(it.bitmap, "图片", Modifier.fillMaxSize(), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit) }
            ?: StatusText(error ?: "正在读取图片…")
    }
}

internal data class Legacy107DesktopDisplayedImage(val bitmap: ImageBitmap, val sourceDimensions: IntSize)

internal fun Legacy107desktopDisplayBitmap(bytes: ByteArray, minimumDisplayWidthPx: Int?,
    displayTargetForSource: ((IntSize) -> IntSize)?): Legacy107DesktopDisplayedImage {
    var source: IntSize? = null
    val decoded = DesktopImageEditing.decode(bytes, longestSide = 1600,
        minimumDisplayWidth = minimumDisplayWidthPx,
        onSourceDimensions = { width, height -> source = IntSize(width, height) },
        displayTargetForSource = displayTargetForSource?.let { target ->
            { width, height -> target(IntSize(width, height)).let { it.width to it.height } }
        })
    val orientedSource = requireNotNull(source)
    val target = displayTargetForSource?.invoke(orientedSource)
    return Legacy107DesktopDisplayedImage(if (target == null) Legacy107desktopDisplayBitmap(decoded, minimumDisplayWidthPx)
        else Legacy107desktopDisplayBitmap(decoded, target), orientedSource)
}

private fun Legacy107desktopDisplayBitmap(original: java.awt.image.BufferedImage, target: IntSize): ImageBitmap {
    val ratio = minOf(1.0, target.width.toDouble() / original.width,
        target.height.toDouble() / original.height)
    val width = (original.width * ratio).roundToInt().coerceAtLeast(1)
    val height = (original.height * ratio).roundToInt().coerceAtLeast(1)
    val small = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    small.createGraphics().let { graphics -> try {
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.drawImage(original, 0, 0, width, height, null)
    } finally { graphics.dispose() } }
    return org.jetbrains.skia.Image.makeFromEncoded(DesktopImageEditing.png(small)).toComposeImageBitmap()
}

private fun Legacy107desktopDisplayBitmap(original: java.awt.image.BufferedImage, minimumDisplayWidthPx: Int?): ImageBitmap {
    val ratio = minOf(1.0, maxOf(1600.0 / maxOf(original.width, original.height),
        (minimumDisplayWidthPx ?: 0).toDouble() / original.width))
    val small = DesktopImageEditing.crop(original, DesktopImageTransform(),
        (original.width * ratio).toInt().coerceAtLeast(1), (original.height * ratio).toInt().coerceAtLeast(1))
    return org.jetbrains.skia.Image.makeFromEncoded(DesktopImageEditing.png(small)).toComposeImageBitmap()
}

