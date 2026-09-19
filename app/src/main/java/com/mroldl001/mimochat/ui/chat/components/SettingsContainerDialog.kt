package com.mroldl001.mimochat.ui.chat.components

import android.view.View
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    // 与其它二级设置菜单保持一致：使用 Material3 AlertDialog，
    // 由系统负责居中、状态栏区域的 dim 以及超高内容的滚动，
    // 避免自定义全屏 Dialog 造成的页面不居中与状态栏高亮问题。
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.settingsDialogWidth().widthIn(max = maxWidth),
        containerColor = containerColor,
        shape = RoundedCornerShape(28.dp),
        title = title,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
