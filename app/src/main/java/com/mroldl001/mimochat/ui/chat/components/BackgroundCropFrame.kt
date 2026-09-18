package com.mroldl001.mimochat.ui.chat.components

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

// Shared with the real layout so a split-screen phone layout still targets the full tablet pane.
val ExpandedChatMinWidth = 600.dp
val ChatSidebarWidth = 280.dp

internal data class BackgroundWindow(
    val width: Int,
    val height: Int,
    val leftInset: Int = 0,
    val topInset: Int = 0,
    val rightInset: Int = 0,
    val bottomInset: Int = 0
)

/** Measures the normal chat chrome at restored-window width; only the preview uses local bounds. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackgroundCropFrame(
    modifier: Modifier = Modifier,
    window: BackgroundWindow = rememberBackgroundWindow(),
    content: @Composable () -> Unit
) {
    SubcomposeLayout(modifier) { constraints ->
        val sidebarWidth = if (window.width.toDp() >= ExpandedChatMinWidth) ChatSidebarWidth.roundToPx() else 0
        val paneWidth = (window.width - sidebarWidth).coerceAtLeast(1)
        val chrome = subcompose("chrome") {
            Column {
                TopAppBar(
                    title = {},
                    windowInsets = WindowInsets(window.leftInset, window.topInset, window.rightInset, 0)
                )
                Column(Modifier.absolutePadding(
                    left = window.leftInset.toDp(),
                    right = window.rightInset.toDp(),
                    bottom = window.bottomInset.toDp()
                )) {
                    SkillToggleBar(
                        isThinkingMode = false,
                        isWebSearchEnabled = false,
                        activeSkill = null,
                        isGenerating = false,
                        onThinkingModeToggle = {},
                        onWebSearchToggle = {},
                        onSkillToggle = {},
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    // Canonical empty composer: keyboard, drafts and attachments must not change the saved crop.
                    InputBar(onSendMessage = {}, onStopGenerating = {})
                }
            }
        }.single().measure(Constraints(minWidth = paneWidth, maxWidth = paneWidth))
        val bodyWidth = (paneWidth - window.leftInset - window.rightInset).coerceAtLeast(1)
        val bodyHeight = (window.height - chrome.height).coerceAtLeast(1)
        val scale = min(constraints.maxWidth.toFloat() / bodyWidth, constraints.maxHeight.toFloat() / bodyHeight)
        val previewWidth = (bodyWidth * scale).roundToInt().coerceIn(0, constraints.maxWidth)
        val previewHeight = (bodyHeight * scale).roundToInt().coerceIn(0, constraints.maxHeight)
        val preview = subcompose("preview") {
            Box(contentAlignment = Alignment.Center) { content() }
        }.single().measure(Constraints.fixed(previewWidth, previewHeight))
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Chrome is measured, never placed or exposed as another interactive UI.
            preview.placeRelative(
                (constraints.maxWidth - preview.width) / 2,
                (constraints.maxHeight - preview.height) / 2
            )
        }
    }
}

@Composable
private fun rememberBackgroundWindow(): BackgroundWindow {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { maximumBackgroundWindow(context) }
}

@Suppress("DEPRECATION")
private fun maximumBackgroundWindow(context: Context): BackgroundWindow {
    val manager = context.getSystemService(WindowManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics = manager.maximumWindowMetrics
        val bounds = metrics.bounds
        // Match Scaffold/TopAppBar system bars, excluding the IME and current freeform caption bar.
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars()
        )
        return BackgroundWindow(bounds.width(), bounds.height(), insets.left, insets.top, insets.right, insets.bottom)
    }
    val display = manager.defaultDisplay
    val size = Point()
    display.getRealSize(size)
    // Display-context resources describe full-screen configuration even in a split-screen Activity.
    val resources = context.createDisplayContext(display).resources
    fun dimension(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id) else 0
    }
    val navigationId = resources.getIdentifier("config_showNavigationBar", "bool", "android")
    val hasNavigation = navigationId != 0 && resources.getBoolean(navigationId)
    val sideNavigation = size.x > size.y && resources.configuration.smallestScreenWidthDp < 600
    val navigationOnLeft = sideNavigation && display.rotation == android.view.Surface.ROTATION_270
    return BackgroundWindow(
        width = size.x,
        height = size.y,
        leftInset = if (hasNavigation && navigationOnLeft) dimension("navigation_bar_width") else 0,
        topInset = dimension("status_bar_height"),
        rightInset = if (hasNavigation && sideNavigation && !navigationOnLeft) dimension("navigation_bar_width") else 0,
        bottomInset = if (hasNavigation && !sideNavigation) {
            dimension(if (size.x > size.y) "navigation_bar_height_landscape" else "navigation_bar_height")
        } else 0
    )
}
