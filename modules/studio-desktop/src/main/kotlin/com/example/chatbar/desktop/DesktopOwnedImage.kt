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
internal fun DesktopOwnedImage(reference: String, read: (String) -> ByteArray,
    modifier: Modifier = Modifier, crop: Boolean = false, onDimensions: ((IntSize) -> Unit)? = null,
    minimumDisplayWidthPx: Int? = null,
    displayTargetForSource: ((IntSize) -> IntSize)? = null) {
    // Each source owns its display state. A slow/failed next image must never retain the old image.
    key(reference, minimumDisplayWidthPx, displayTargetForSource) {
        DesktopOwnedImageContent(reference,read,modifier,crop,onDimensions,minimumDisplayWidthPx,displayTargetForSource)
    }
}

@Composable private fun DesktopOwnedImageContent(reference:String,read:(String)->ByteArray,
    modifier:Modifier,crop:Boolean,onDimensions:((IntSize)->Unit)?,minimumDisplayWidthPx:Int?,
    displayTargetForSource:((IntSize)->IntSize)?) {
    var error by remember(reference) { mutableStateOf<String?>(null) }
    val displayed by produceState<DesktopDisplayedImage?>(null, reference, minimumDisplayWidthPx, displayTargetForSource) {
        try {
            withContext(Dispatchers.IO) {
                val bytes = read(reference)
                require(bytes.size <= com.example.chatbar.domain.image.ApngDisguiseCodec.MAX_OUTPUT_BYTES)
                if (playDesktopApng(bytes) { frame, duration ->
                    value = DesktopDisplayedImage(desktopDisplayBitmap(frame, minimumDisplayWidthPx),
                        IntSize(frame.width, frame.height)); delay(duration)
                }) return@withContext
                var animated=false
                org.jetbrains.skia.Data.makeFromBytes(bytes).use { data ->
                    org.jetbrains.skia.Codec.makeFromData(data).use { codec ->
                        require(codec.width.toLong() * codec.height <= DesktopImageEditing.MAX_PIXELS)
                        if (codec.frameCount > 1) org.jetbrains.skia.Bitmap().use { frame ->
                            animated=true
                            check(frame.allocPixels(codec.imageInfo))
                            var loops = 0
                            do {
                                for (index in 0 until codec.frameCount) {
                                    currentCoroutineContext().ensureActive()
                                    codec.readPixels(frame, index)
                                    value = DesktopDisplayedImage(org.jetbrains.skia.Image.makeFromBitmap(frame).toComposeImageBitmap(),
                                        IntSize(codec.width, codec.height))
                                    delay(codec.getFrameInfo(index).duration.coerceAtLeast(10).toLong())
                                }
                                loops++
                            } while (codec.repetitionCount < 0 || loops <= codec.repetitionCount)
                        }
                    }
                }
                if(!animated) {
                    currentCoroutineContext().ensureActive()
                    val result=desktopDisplayBitmap(bytes,minimumDisplayWidthPx,displayTargetForSource)
                    currentCoroutineContext().ensureActive()
                    value=result
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

internal data class DesktopDisplayedImage(val bitmap: ImageBitmap, val sourceDimensions: IntSize)

internal fun desktopDisplayBitmap(bytes: ByteArray, minimumDisplayWidthPx: Int?,
    displayTargetForSource: ((IntSize) -> IntSize)?): DesktopDisplayedImage {
    var source: IntSize? = null
    val decoded = DesktopImageEditing.decode(bytes, longestSide = 1600,
        minimumDisplayWidth = minimumDisplayWidthPx,
        onSourceDimensions = { width, height -> source = IntSize(width, height) },
        displayTargetForSource = displayTargetForSource?.let { target ->
            { width, height -> target(IntSize(width, height)).let { it.width to it.height } }
        })
    return try {
        val orientedSource = requireNotNull(source)
        val target = displayTargetForSource?.invoke(orientedSource)
        DesktopDisplayedImage(if (target == null) desktopDisplayBitmap(decoded, minimumDisplayWidthPx)
            else desktopDisplayBitmap(decoded, target), orientedSource)
    } finally { decoded.flush() }
}

private fun desktopDisplayBitmap(original: java.awt.image.BufferedImage, target: IntSize): ImageBitmap {
    val ratio = minOf(1.0, target.width.toDouble() / original.width,
        target.height.toDouble() / original.height)
    val width = (original.width * ratio).roundToInt().coerceAtLeast(1)
    val height = (original.height * ratio).roundToInt().coerceAtLeast(1)
    if(width==original.width && height==original.height)return desktopRasterBitmap(original)
    val small = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    try {
    small.createGraphics().let { graphics -> try {
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.drawImage(original, 0, 0, width, height, null)
    } finally { graphics.dispose() } }
    return desktopRasterBitmap(small)
    } finally { small.flush() }
}

// Bulk row conversion avoids BufferedImage's per-pixel temporary objects on byte-backed rasters.
// getRGB normalizes color space and unpremultiplies alpha without modifying the source pixels.
private fun desktopRasterBitmap(source: java.awt.image.BufferedImage): ImageBitmap {
    val pixels = ByteArray(Math.multiplyExact(Math.multiplyExact(source.width, source.height), 4))
    val row = IntArray(source.width)
    var offset = 0
    for (y in 0 until source.height) {
        source.getRGB(0, y, source.width, 1, row, 0, source.width)
        for (argb in row) {
            pixels[offset++] = (argb ushr 16).toByte()
            pixels[offset++] = (argb ushr 8).toByte()
            pixels[offset++] = argb.toByte()
            pixels[offset++] = (argb ushr 24).toByte()
        }
    }
    return org.jetbrains.skia.Image.makeRaster(
        org.jetbrains.skia.ImageInfo(source.width, source.height,
            org.jetbrains.skia.ColorType.RGBA_8888, org.jetbrains.skia.ColorAlphaType.UNPREMUL),
        pixels, source.width * 4).toComposeImageBitmap()
}

private fun desktopDisplayBitmap(original: java.awt.image.BufferedImage, minimumDisplayWidthPx: Int?): ImageBitmap {
    val ratio = minOf(1.0, maxOf(1600.0 / maxOf(original.width, original.height),
        (minimumDisplayWidthPx ?: 0).toDouble() / original.width))
    return desktopDisplayBitmap(original,IntSize((original.width*ratio).toInt().coerceAtLeast(1),
        (original.height*ratio).toInt().coerceAtLeast(1)))
}

