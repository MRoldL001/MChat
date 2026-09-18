package com.mroldl001.mimochat.ui.chat.components

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

internal fun LayoutCoordinates.screenBounds(view: View): Rect {
    val screen = IntArray(2)
    val window = IntArray(2)
    view.getLocationOnScreen(screen)
    view.getLocationInWindow(window)
    return boundsInWindow().translate(Offset((screen[0] - window[0]).toFloat(), (screen[1] - window[1]).toFloat()))
}

internal class SettingsTransition {
    var closing by mutableStateOf(false)
        private set

    fun close(action: () -> Unit) {
        if (closing) return
        closing = true
        action()
    }

    fun openWithoutAnimation(action: () -> Unit) {
        closing = false
        action()
    }
}

@Composable
internal fun rememberSettingsTransition(): SettingsTransition {
    return remember { SettingsTransition() }
}

@Composable
internal fun Modifier.settingsDialogWidth(): Modifier {
    val maxWidth = if (LocalConfiguration.current.screenWidthDp < 600) 320.dp else 560.dp
    return widthIn(max = maxWidth)
}

@Composable
internal fun SettingsContainerDialog(
    anchorBounds: Rect,
    transition: SettingsTransition,
    onDismissRequest: () -> Unit,
    maxWidth: Dp = 560.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false
    )) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
        val colors = MaterialTheme.colorScheme
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.32f))
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismissRequest))
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(containerColor)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { }
                        .padding(24.dp)
                ) {
                    Column {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall.copy(color = colors.onSurface), title)
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = colors.onSurfaceVariant), text)
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        dismissButton()
                        Spacer(Modifier.width(8.dp))
                        confirmButton()
                    }
                }
            }
        }
    }
}
