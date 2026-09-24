package com.mroldl001.mimochat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AIModel(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val capabilities: List<String>
)

data class ModelListResponse(
    val models: List<AIModel>
)
