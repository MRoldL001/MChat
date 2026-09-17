package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.withFrameNanos

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
