package com.mroldl001.mimochat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Chat(
    val id: Long = 0,
    val title: String,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
