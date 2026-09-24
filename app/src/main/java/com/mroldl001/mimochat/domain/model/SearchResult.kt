package com.mroldl001.mimochat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SearchResult(
    val message: Message,
    val chat: Chat,
    val highlightedContent: String
)
