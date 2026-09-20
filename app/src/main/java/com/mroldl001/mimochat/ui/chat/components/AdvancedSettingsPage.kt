package com.mroldl001.mimochat.ui.chat.components

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.theme.supportsDynamicColor

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsPage(
    initialThemeColor: ThemeColor,
    initialThemeMode: ThemeMode,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit,
    onApiKeyClick: () -> Unit,
    onBackgroundImageClick: () -> Unit,
    updateState: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onParameterSettingsClick: () -> Unit,
    onCustomPromptClick: () -> Unit,
    onApiBaseUrlClick: () -> Unit,
    acceptPrereleaseUpdates: Boolean,
    onAcceptPrereleaseUpdatesChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var themeColor by rememberSaveable { mutableStateOf(initialThemeColor) }
    var themeMode by rememberSaveable { mutableStateOf(initialThemeMode) }
    val pageColor = MaterialTheme.colorScheme.surfaceContainerHigh

    BackHandler(onBack = onDismiss)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setBackgroundDrawable(ColorDrawable(pageColor.toArgb()))
            window.statusBarColor = pageColor.toArgb()
            window.navigationBarColor = pageColor.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = pageColor.luminance() > 0.5f
                isAppearanceLightNavigationBars = pageColor.luminance() > 0.5f
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = pageColor) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopAppBar(
                    title = { Text("设置") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = pageColor)
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        SettingSectionHeader(Icons.Default.Brightness7, "显示模式")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PageThemeModeOption(themeMode == ThemeMode.LIGHT, "白天", Color.White) {
                                themeMode = ThemeMode.LIGHT
                                onThemeChanged(themeColor, themeMode)
                            }
                            PageThemeModeOption(themeMode == ThemeMode.DARK, "夜间", Color.Black) {
                                themeMode = ThemeMode.DARK
                                onThemeChanged(themeColor, themeMode)
                            }
                            PageThemeModeOption(themeMode == ThemeMode.FOLLOW_SYSTEM, "跟随系统", Color.Gray) {
                                themeMode = ThemeMode.FOLLOW_SYSTEM
                                onThemeChanged(themeColor, themeMode)
                            }
                        }

                        SettingSectionHeader(Icons.Default.Palette, "主题颜色")
                        val colors = buildList {
                            add(ThemeColor.WHITE)
                            if (supportsDynamicColor()) add(ThemeColor.AUTO_COLOR)
                            add(ThemeColor.HATSUNE_MIKU)
                            add(ThemeColor.MI_ORANGE)
                            add(ThemeColor.GREEN)
                            add(ThemeColor.PURPLE)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            colors.chunked(3).forEach { rowColors ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    rowColors.forEach { option ->
                                        PageThemeColorOption(
                                            selected = themeColor == option,
                                            label = themeColorLabel(option),
                                            color = themeColorValue(option),
                                            isAutoColor = option == ThemeColor.AUTO_COLOR,
                                            onClick = {
                                                themeColor = option
                                                onThemeChanged(themeColor, themeMode)
                                            }
                                        )
                                    }
                                    repeat(3 - rowColors.size) { Spacer(Modifier.width(80.dp)) }
                                }
                            }
                        }

                        SettingAction(Icons.Default.Key, "API Key", "配置您的 API 密钥以使用服务", onApiKeyClick)
                        SettingAction(Icons.Default.Image, "聊天背景图", "选择聊天中使用的背景图片", onBackgroundImageClick)
                        UpdateSettingsItem(state = updateState, onCheck = onCheckForUpdate)

                        SettingAction(Icons.Default.Tune, "参数设置", "调整模型参数", onParameterSettingsClick)
                        SettingAction(Icons.Default.ChatBubble, "自定义系统提示词", "设置个性化的系统提示词", onCustomPromptClick)
                        SettingAction(Icons.Default.Link, "API Base URL", "配置 API 服务器地址", onApiBaseUrlClick)
                        PrereleaseUpdateSetting(
                            checked = acceptPrereleaseUpdates,
                            onCheckedChange = onAcceptPrereleaseUpdatesChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
internal fun SettingSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SettingPageIcon(icon)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingPageIcon(icon: ImageVector) {
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
internal fun PageThemeModeOption(selected: Boolean, label: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        themeOptionBorderColor(selected),
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "page_mode_border"
    )
    val checkScale by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "page_mode_check"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, borderColor, CircleShape)
                .clip(CircleShape)
                .background(color)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (checkScale > 0f) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (color == Color.White) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun PageThemeColorOption(
    selected: Boolean,
    label: String,
    color: Color,
    isAutoColor: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        themeOptionBorderColor(selected),
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "page_color_border"
    )
    val checkScale by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "page_color_check"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(2.dp, borderColor, CircleShape)
                .clip(CircleShape)
                .background(
                    if (isAutoColor) Brush.sweepGradient(
                        0f to Color(0xFF9BC4E2), 0.25f to Color(0xFFD4A373),
                        0.5f to Color(0xFFE7D8C9), 0.75f to Color(0xFF8FA6CB), 1f to Color(0xFF9BC4E2)
                    ) else Brush.linearGradient(listOf(color, color), start = Offset.Zero, end = Offset.Infinite)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (checkScale > 0f) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = if (color == Color.White) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

internal fun themeColorLabel(color: ThemeColor): String = when (color) {
    ThemeColor.WHITE -> "默认"
    ThemeColor.AUTO_COLOR -> "莫奈取色"
    ThemeColor.HATSUNE_MIKU -> "初音绿"
    ThemeColor.MI_ORANGE -> "小米橙"
    ThemeColor.GREEN -> "盎然绿"
    ThemeColor.PURPLE -> "罗兰紫"
}

internal fun themeColorValue(color: ThemeColor): Color = when (color) {
    ThemeColor.WHITE -> Color.White
    ThemeColor.AUTO_COLOR -> Color.Transparent
    ThemeColor.HATSUNE_MIKU -> Color(0xFF39C5BB)
    ThemeColor.MI_ORANGE -> Color(0xFFFF7E00)
    ThemeColor.GREEN -> Color(0xFF006E2A)
    ThemeColor.PURPLE -> Color(0xFF6650A4)
}
