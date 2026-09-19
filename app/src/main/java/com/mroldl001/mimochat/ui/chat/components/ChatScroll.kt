package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay

data class ChatScrollPosition(
    val index: Int,
    val offset: Int
)

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

suspend fun LazyListState.scrollToBottomContentStable(
    maxFrames: Int = 20
) {
    var stableFrames = 0
    repeat(maxFrames) {
        scrollToBottomContent()
        withFrameNanos { }
        val lastIndex = layoutInfo.totalItemsCount - 1
        val lastItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex }
        val hiddenBottom = lastItem?.let {
            it.offset + it.size - layoutInfo.viewportEndOffset
        } ?: 0
        if (hiddenBottom <= 1) {
            stableFrames++
            if (stableFrames >= 2) return
        } else {
            stableFrames = 0
        }
        delay(16)
    }
    scrollToBottomContent()
}
