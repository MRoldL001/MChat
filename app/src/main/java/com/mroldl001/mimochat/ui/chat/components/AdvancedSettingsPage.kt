package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.theme.ThemeColor

@Composable
internal fun animateThemeColor(
    target: Color,
    label: String
): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 450),
        label = label
    )
    return color
}

@Composable
internal fun animatePreviewColors(scheme: ColorScheme, label: String): ColorScheme {
    val spec = tween<Color>(durationMillis = 450)
    val surface by animateColorAsState(scheme.surface, animationSpec = spec, label = "${label}_surface")
    val inverseSurface by animateColorAsState(scheme.inverseSurface, animationSpec = spec, label = "${label}_inverse_surface")
    val primary by animateColorAsState(scheme.primary, animationSpec = spec, label = "${label}_primary")
    val primaryContainer by animateColorAsState(scheme.primaryContainer, animationSpec = spec, label = "${label}_primary_container")
    val surfaceContainerHighest by animateColorAsState(
        scheme.surfaceContainerHighest,
        animationSpec = spec,
        label = "${label}_surface_container_highest"
    )
    return scheme.copy(
        surface = surface,
        inverseSurface = inverseSurface,
        primary = primary,
        primaryContainer = primaryContainer,
        surfaceContainerHighest = surfaceContainerHighest
    )
}

@Composable
internal fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = animateThemeColor(MaterialTheme.colorScheme.primary, "settings_group_title"),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
internal fun SettingSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingPageIcon(icon)
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = animateThemeColor(MaterialTheme.colorScheme.onSurface, "settings_section_header")
        )
    }
}

@Composable
internal fun SettingAction(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingPageIcon(icon)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = animateThemeColor(MaterialTheme.colorScheme.onSurface, "settings_action_title")
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = animateThemeColor(MaterialTheme.colorScheme.onSurfaceVariant, "settings_action_desc")
            )
        }
    }
}

@Composable
internal fun SettingPageIcon(icon: ImageVector) {
    val iconColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 450),
        label = "settings_icon_color"
    )
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 450),
        label = "settings_icon_container_color"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun ThemePreviewCard(
    label: String,
    scheme: ColorScheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    bottomScheme: ColorScheme? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 450),
        label = "theme_preview_border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 1.dp,
        animationSpec = tween(durationMillis = 450),
        label = "theme_preview_border_width"
    )
    val labelColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "theme_preview_label")
    val topColors = animatePreviewColors(scheme, "theme_preview_top")
    val lowerColors = bottomScheme?.let { animatePreviewColors(it, "theme_preview_bottom") } ?: topColors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            modifier = Modifier
                .width(width)
                .height(172.dp)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            color = topColors.surface,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 背景层：bottomScheme 不为空时下半部分硬分割覆盖
                if (bottomScheme != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(lowerColors.surface)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    // 顶栏：标题胶囊
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(topColors.inverseSurface)
                    )
                    Spacer(Modifier.height(12.dp))
                    // 用户气泡（右）
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(15.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(topColors.primary)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // AI 气泡（左）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(topColors.surfaceContainerHighest)
                    )
                    Spacer(Modifier.height(8.dp))
                    // 技能按钮（只画一个）——位于分割线下半部分，取 lowerColors
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(lowerColors.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(lowerColors.primary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(lowerColors.primary.copy(alpha = 0.55f))
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // 输入栏：加号 + 输入胶囊 + 发送钮——同样取 lowerColors
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(lowerColors.primary)
                        )
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(13.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(lowerColors.surfaceContainerHighest)
                        )
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(lowerColors.primary.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(width)
        )
    }
}

/**
 * 代码块配色预览卡：代码块顶满整张卡（无内边距、无空白框），
 * 行条用 SpaceBetween 均匀铺满卡高，超出部分被卡圆角裁剪。
 * split = true 时为「随显示模式切换」：上浅下深硬分割（同一张卡内无缝拼接）。
 */
@Composable
internal fun CodeBlockPreviewCard(
    label: String,
    dark: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 100.dp,
    split: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 450),
        label = "code_preview_border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 3.dp else 1.dp,
        animationSpec = tween(durationMillis = 450),
        label = "code_preview_border_width"
    )
    val labelColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "code_preview_label")
    val surfaceColor = animateThemeColor(MaterialTheme.colorScheme.surface, "code_preview_surface")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            modifier = Modifier
                .width(width)
                .height(80.dp)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            if (split) {
                // 与其它卡同一套行条布局，只是底色上浅下深硬分割（分割线穿过某行条也无妨）
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(codeBlockPalette(false).background)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(codeBlockPalette(true).background)
                        )
                    }
                    MiniCodeBlock(
                        dark = false,
                        sharp = true,
                        lines = 4,
                        transparent = true,
                        darkFromLine = 3,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                MiniCodeBlock(dark = dark, sharp = true, lines = 4, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(width)
        )
    }
}

/**
 * 迷你代码块：左侧行号条 + 顶栏语言条 + lines 行代码色条，
 * 行条 SpaceBetween 均匀铺满给定高度；上下 18dp 边距让代码行向中间收拢。
 * - sharp = true：不带圆角（顶满卡片）
 * - transparent = true：不画底色（由外层画硬分割背景）
 * - darkFromLine：从第 index 条（顶栏为 0）起改用深色配色，用于分割卡
 */
@Composable
private fun MiniCodeBlock(
    dark: Boolean,
    modifier: Modifier = Modifier,
    sharp: Boolean = false,
    lines: Int = 4,
    transparent: Boolean = false,
    darkFromLine: Int? = null
) {
    val base = codeBlockPalette(dark)
    val lightPalette = if (darkFromLine != null) codeBlockPalette(false) else base
    val darkPalette = if (darkFromLine != null) codeBlockPalette(true) else base
    val paletteAt: (Int) -> CodeBlockPalette = { index ->
        if (darkFromLine != null && index >= darkFromLine) darkPalette else lightPalette
    }
    val shape = if (sharp) RectangleShape else RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .then(if (transparent) Modifier else Modifier.background(base.background))
            .clip(shape)
            // 上下大边距：行条整体向中间收拢，不贴卡的上下边缘
            .padding(horizontal = 8.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.width(8.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(lines) { index ->
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(paletteAt(index + 1).muted.copy(alpha = 0.6f))
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(paletteAt(0).muted)
            )
            repeat(lines) { index ->
                val palette = paletteAt(index + 1)
                val color = when (index % 5) {
                    0 -> palette.keyword
                    1 -> palette.string
                    2 -> palette.number
                    3 -> palette.text
                    else -> palette.function
                }
                val widthFraction = when (index % 5) {
                    0 -> 0.85f
                    1 -> 0.55f
                    2 -> 0.7f
                    3 -> 0.4f
                    else -> 0.6f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
internal fun themeColorLabel(color: ThemeColor): String {
    return when (color) {
        ThemeColor.WHITE -> stringResource(R.string.theme_default)
        ThemeColor.CUSTOM -> stringResource(R.string.theme_custom)
        ThemeColor.AUTO_COLOR -> stringResource(R.string.theme_auto_color)
        ThemeColor.HATSUNE_MIKU -> stringResource(R.string.theme_hatsune_miku)
        ThemeColor.TETO_RED -> stringResource(R.string.theme_teto_red)
        ThemeColor.MI_ORANGE -> stringResource(R.string.theme_mi_orange)
        ThemeColor.GREEN -> stringResource(R.string.theme_green)
        ThemeColor.PURPLE -> stringResource(R.string.theme_purple)
        ThemeColor.DEEP_BLUE -> stringResource(R.string.theme_deep_blue)
    }
}
