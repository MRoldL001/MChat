package com.mroldl001.mimochat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class WebSearchResult(
    val url: String,
    val title: String,
    val summary: String? = null,
    val siteName: String? = null,
    val publishTime: String? = null,
    val logoUrl: String? = null
)