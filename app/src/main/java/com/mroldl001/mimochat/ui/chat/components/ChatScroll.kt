package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

data class ChatScrollPosition(
    val index: Int,
    val offset: Int
)

/**
 * 生成结束后等这么久再滚到底。
 * Markdown 异步渲染、代码块换行、图片解码都会在最后一帧之后继续撑高内容，
 * 立刻滚只会滚到一半；等布局基本稳定再一次性滚到位。
 */
const val BOTTOM_SETTLE_DELAY_MILLIS = 180L

@Composable
internal fun PersistChatScrollPosition(
    chatId: Long?,
    listState: LazyListState,
    onPositionChanged: (Long, Int, Int) -> Unit
) {
    DisposableEffect(chatId, listState) {
        onDispose {
            if (chatId != null) {
                onPositionChanged(
                    chatId,
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset
                )
            }
        }
    }

    LaunchedEffect(chatId, listState) {
        if (chatId == null) return@LaunchedEffect
        var hasUserScrolled = false
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress
            )
        }
            .distinctUntilChanged()
            .collectLatest { (index, offset, isScrolling) ->
                if (isScrolling) {
                    hasUserScrolled = true
                    return@collectLatest
                }
                if (hasUserScrolled) {
                    delay(150)
                    onPositionChanged(chatId, index, offset)
                }
            }
    }
}

suspend fun LazyListState.animateScrollToBottomContent() {
    ensureLastItemVisible(animate = true)
    val hidden = measureHiddenBottom() ?: return
    if (hidden > 0) {
        animateScrollBy(hidden.toFloat())
    }
}

suspend fun LazyListState.scrollToBottomContent() {
    ensureLastItemVisible(animate = false)
    val hidden = measureHiddenBottom() ?: return
    if (hidden > 0) {
        scrollBy(hidden.toFloat())
    }
}

/** 最后一项底部超出视口底部的距离；列表还没布局好、或最后一项不在可见区时返回 null */
private fun LazyListState.measureHiddenBottom(): Int? {
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return null

    if (layoutInfo.visibleItemsInfo.none { it.index == lastIndex }) return null

    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex } ?: return null
    return lastItem.offset + lastItem.size - layoutInfo.viewportEndOffset
}

/** 最后一项不在可视区时先跳过去，之后才能算出真实隐藏距离 */
private suspend fun LazyListState.ensureLastItemVisible(animate: Boolean) {
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return
    if (layoutInfo.visibleItemsInfo.any { it.index == lastIndex }) return

    if (animate) animateScrollToItem(lastIndex) else scrollToItem(lastIndex)
    withFrameNanos { }
}

/**
 * 等内容布局稳定后再一次性滚到底。
 * 单次滚动不会来回抖；[BOTTOM_SETTLE_DELAY_MILLIS] 用来盖住渲染期的持续撑高。
 * 撤回编辑复用同一个时序：消息淡入、Markdown/图片异步渲染，180ms 后高度基本到位。
 */
suspend fun LazyListState.awaitStableScrollToBottom() {
    delay(BOTTOM_SETTLE_DELAY_MILLIS)
    withFrameNanos { }
    scrollToBottomContent()
}
