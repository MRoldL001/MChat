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
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return

    if (layoutInfo.visibleItemsInfo.none { it.index == lastIndex }) {
        animateScrollToItem(lastIndex)
        withFrameNanos { }
    }

    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex } ?: return
    val hiddenBottom = lastItem.offset + lastItem.size - layoutInfo.viewportEndOffset
    if (hiddenBottom > 0) {
        animateScrollBy(hiddenBottom.toFloat())
    }
}

suspend fun LazyListState.scrollToBottomContent() {
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return

    if (layoutInfo.visibleItemsInfo.none { it.index == lastIndex }) {
        scrollToItem(lastIndex)
        withFrameNanos { }
    }

    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex } ?: return
    val hiddenBottom = lastItem.offset + lastItem.size - layoutInfo.viewportEndOffset
    if (hiddenBottom > 0) {
        scrollBy(hiddenBottom.toFloat())
    }
}
