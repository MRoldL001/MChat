package com.mroldl001.mimochat.domain.model

data class Message(
    val id: Long = 0,
    val chatId: Long,
    val role: String,
    val content: String,
    val reasoningContent: String? = null,
    val searchResults: List<WebSearchResult>? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isAborted: Boolean = false,
    val isFailed: Boolean = false
    , val attachmentUri: String? = null
    , val attachmentMimeType: String? = null
)
