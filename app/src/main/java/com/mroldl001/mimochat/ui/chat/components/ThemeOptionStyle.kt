package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
internal fun themeOptionBorderColor(selected: Boolean): Color {
    val colors = MaterialTheme.colorScheme
    // Some Monet palettes supply a very dark outlineVariant, obscuring the selected ring.
    // Keep the neutral outline consistent across phone/tablet and retain the active theme accent.
    return if (selected) {
        colors.primary
    } else if (colors.background.luminance() > 0.5f) {
        Color(0xFFCACACA)
    } else {
        Color(0xFF494949)
    }
}
