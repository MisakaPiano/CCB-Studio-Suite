package com.example.chatbar.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.example.chatbar.domain.image.*
import kotlinx.coroutines.launch

// Selected presentation functions from Desktop 37c67c7; no account/store ownership copied.
internal data class DesktopTokenProgress(val count: Int, val limit: Int) {
    val fraction get() = if (limit > 0) (count.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val exceeded get() = count > limit
    val warning get() = exceeded || (limit > 0 && count.toFloat() / limit >= .9f)
}

@Composable
internal fun StudioTokenBar(label: String, count: Int, limit: Int, modifier: Modifier = Modifier) {
    val progress = DesktopTokenProgress(count, limit)
    val color = if (progress.exceeded) DesktopBootstrapColors.destructive else if (progress.warning) DesktopBootstrapColors.warning else DesktopBootstrapColors.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        StatusText("$label $count/$limit" + if (progress.exceeded) " · 超出上限" else if (progress.warning) " · 接近上限" else "", color)
        Box(Modifier.fillMaxWidth().height(5.dp).background(DesktopBootstrapColors.muted)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.fraction, 0f..1f) }) {
            Box(Modifier.fillMaxWidth(progress.fraction).fillMaxHeight().background(color))
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun StudioFilmstrip(paths: List<String>, selected: String?, read: (String) -> ByteArray,
    scroll: LazyListState = rememberLazyListState(), onPreview: ((String) -> Unit)? = null, onSelect: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(selected, paths) {
        val index = paths.indexOf(selected)
        if (index >= 0 && scroll.layoutInfo.visibleItemsInfo.none { it.index == index }) scroll.scrollToItem(index)
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LazyRow(Modifier.fillMaxWidth().onPointerEvent(PointerEventType.Scroll) { event ->
            val delta = event.changes.sumOf { (it.scrollDelta.x + it.scrollDelta.y).toDouble() }.toFloat()
            if (delta != 0f) { scope.launch { scroll.scrollBy(delta * 48) }; event.changes.forEach { it.consume() } }
        }, state = scroll, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(paths, key = { it }) { path ->
                DesktopOwnedImage(path, read, Modifier.size(88.dp).border(if (path == selected) 3.dp else 1.dp,
                    if (path == selected) DesktopBootstrapColors.primary else DesktopBootstrapColors.border)
                    .semantics { this.selected = path == selected; contentDescription = "选择结果缩略图 ${paths.indexOf(path) + 1}" }
                    .then(if (onPreview == null) Modifier.clickable { onSelect(path) }
                        else Modifier.desktopViewerEntry(onClick = { onSelect(path) }, onOpen = { onPreview(path) })))
            }
        }
        HorizontalScrollbar(rememberScrollbarAdapter(scroll), Modifier.fillMaxWidth().height(8.dp)
            .semantics { contentDescription = "结果缩略图滚动条" }, style = ScrollbarStyle(
                minimalHeight = 24.dp, thickness = 8.dp, shape = RoundedCornerShape(4.dp),
                hoverDurationMillis = 0, unhoverColor = DesktopBootstrapColors.border,
                hoverColor = DesktopBootstrapColors.primary))
    }
}

/** Rail is the same path projection and callback as the horizontal strip, with its own viewport. */
@Composable
internal fun StudioVerticalFilmstrip(paths: List<String>, selected: String?, read: (String) -> ByteArray,
    scroll: LazyListState = rememberLazyListState(), onPreview: ((String) -> Unit)? = null, onSelect: (String) -> Unit) {
    LaunchedEffect(selected, paths) {
        val index = paths.indexOf(selected)
        if (index >= 0 && scroll.layoutInfo.visibleItemsInfo.none { it.index == index }) scroll.requestScrollToItem(index)
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(end = 10.dp).semantics { contentDescription = "竖向结果缩略图" },
            state = scroll, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(paths, key = { it }) { path ->
                DesktopOwnedImage(path, read, Modifier.size(88.dp).border(if (path == selected) 3.dp else 1.dp,
                    if (path == selected) DesktopBootstrapColors.primary else DesktopBootstrapColors.border)
                    .semantics { this.selected = path == selected; contentDescription = "选择结果缩略图 ${paths.indexOf(path) + 1}" }
                    .then(if (onPreview == null) Modifier.clickable { onSelect(path) }
                        else Modifier.desktopViewerEntry(onClick = { onSelect(path) }, onOpen = { onPreview(path) })))
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(8.dp)
            .semantics { contentDescription = "竖向结果缩略图滚动条" }, style = ScrollbarStyle(
                minimalHeight = 24.dp, thickness = 8.dp, shape = RoundedCornerShape(4.dp), hoverDurationMillis = 0,
                unhoverColor = DesktopBootstrapColors.border, hoverColor = DesktopBootstrapColors.primary))
    }
}

// DesktopViewerPresentation.kt: selected input adapter, identical behavior.
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.desktopViewerEntry(enabled: Boolean = true, onClick: () -> Unit,
    onOpen: () -> Unit = onClick): Modifier = combinedClickable(
    enabled = enabled, onClick = onClick, onDoubleClick = onOpen)
