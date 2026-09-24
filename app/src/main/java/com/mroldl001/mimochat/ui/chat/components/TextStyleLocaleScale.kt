package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale

@Composable
internal fun TextStyle.localeScaled(scale: Float = 0.9f): TextStyle {
    return if (Locale.current.language in setOf("en", "ja")) {
        copy(fontSize = fontSize * scale)
    } else {
        this
    }
}
