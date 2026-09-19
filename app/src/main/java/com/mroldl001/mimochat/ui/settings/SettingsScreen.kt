package com.mroldl001.mimochat.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.chat.ApiKeyDialog
import com.mroldl001.mimochat.ui.chat.CustomSystemPromptDialog
import com.mroldl001.mimochat.ui.chat.ParameterSettingsDialog
import com.mroldl001.mimochat.ui.chat.components.*
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatViewModel
import com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.theme.supportsDynamicColor
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit,
    onNavigateToDisclaimer: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val transition = rememberSettingsTransition()
    var showApiKey by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var showParameters by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var showApiUrl by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var pendingCropUri by rememberSaveable { mutableStateOf<String?>(null) }

    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingCropUri = uri.toString()
    }
    val pickBackground = {
        backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val clearBackground = {
        deleteStoredChatBackground(context, uiState.chatBackgroundUri)
        viewModel.setChatBackgroundUri(null)
        viewModel.setChatBackgroundOpacity(PreferencesManager.DEFAULT_CHAT_BACKGROUND_OPACITY)
    }

    LaunchedEffect(uiState.updateState) {
        val updateState = uiState.updateState
        if (updateState is UpdateUiState.Latest) {
            Toast.makeText(context, updateState.message, Toast.LENGTH_SHORT).show()
            viewModel.clearUpdateState()
        }
    }

    SettingsPageContent(
        themeColor = uiState.themeColor,
        themeMode = uiState.themeMode,
        onThemeChanged = { color, mode ->
            viewModel.setThemeColor(color)
            viewModel.setThemeMode(mode)
            onThemeChanged(color, mode)
        },
        onApiKeyClick = { showApiKey = true },
        onBackgroundImageClick = { showBackground = true },
        updateState = uiState.updateState,
        onCheckForUpdate = viewModel::checkForUpdate,
        onParameterSettingsClick = { showParameters = true },
        onCustomPromptClick = { showPrompt = true },
        onApiBaseUrlClick = { showApiUrl = true },
        acceptPrereleaseUpdates = uiState.acceptPrereleaseUpdates,
        onAcceptPrereleaseUpdatesChanged = viewModel::setAcceptPrereleaseUpdates,
        onAboutClick = { showAbout = true },
        onNavigateBack = onNavigateBack
    )

    if (showApiKey) {
        ApiKeyDialog(
            currentKey = uiState.apiKey,
            anchorBounds = Rect.Zero,
            transition = transition,
            onDismiss = { showApiKey = false },
            onConfirm = { viewModel.setApiKey(it); showApiKey = false }
        )
    }
    if (showBackground) {
        BackgroundImageSettingsDialog(
            hasBackgroundImage = !uiState.chatBackgroundUri.isNullOrBlank(),
            opacity = uiState.chatBackgroundOpacity,
            onSelectImage = pickBackground,
            onOpacityChanged = viewModel::setChatBackgroundOpacity,
            onRestoreDefault = clearBackground,
            onDismiss = { showBackground = false }
        )
    }
    pendingCropUri?.let { uriString ->
        BackgroundCropDialog(
            sourceUri = Uri.parse(uriString),
            onCropped = { croppedUri ->
                val previousUri = uiState.chatBackgroundUri
                viewModel.setChatBackgroundUri(croppedUri)
                if (previousUri != croppedUri) deleteStoredChatBackground(context, previousUri)
                pruneStoredChatBackgrounds(context, croppedUri)
                pendingCropUri = null
            },
            onDismiss = { pendingCropUri = null }
        )
    }
    if (showParameters) {
        ParameterSettingsDialog(
            initialTemperature = uiState.temperature,
            initialTopP = uiState.topP,
            initialFrequencyPenalty = uiState.frequencyPenalty,
            initialPresencePenalty = uiState.presencePenalty,
            anchorBounds = Rect.Zero,
            transition = transition,
            onDismiss = { showParameters = false },
            onConfirm = { temperature, topP, frequencyPenalty, presencePenalty ->
                viewModel.setTemperature(temperature)
                viewModel.setTopP(topP)
                viewModel.setFrequencyPenalty(frequencyPenalty)
                viewModel.setPresencePenalty(presencePenalty)
                showParameters = false
            },
            onReset = { viewModel.resetParameters(); showParameters = false }
        )
    }
    if (showPrompt) {
        CustomSystemPromptDialog(
            currentPrompt = uiState.customSystemPrompt,
            anchorBounds = Rect.Zero,
            transition = transition,
            onDismiss = { showPrompt = false },
            onConfirm = { viewModel.setCustomSystemPrompt(it); showPrompt = false }
        )
    }
    if (showApiUrl) {
        ApiBaseUrlDialog(
            currentUrl = uiState.apiBaseUrl,
            anchorBounds = Rect.Zero,
            transition = transition,
            onDismiss = { showApiUrl = false },
            onConfirm = { viewModel.setApiBaseUrl(it); showApiUrl = false }
        )
    }
    if (showAbout) {
        AboutDialog(
            transition = transition,
            onDismiss = { showAbout = false },
            onDisclaimerClick = {
                showAbout = false
                onNavigateToDisclaimer()
            }
        )
    }
    (uiState.updateState as? UpdateUiState.Available)?.let { update ->
        UpdateReleaseDialog(
            release = update.release,
            onDismiss = viewModel::clearUpdateState,
            onDownload = { viewModel.downloadUpdate(update.release) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPageContent(
    themeColor: ThemeColor,
    themeMode: ThemeMode,
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
    onAboutClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val pageColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 450),
        label = "settings_page_color"
    )
    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = backInteractionSource,
                                indication = null,
                                onClick = onNavigateBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                ShowMeTheCastleBanner()

                SettingSectionHeader(Icons.Default.Brightness7, "显示模式")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PageThemeModeOption(themeMode == ThemeMode.LIGHT, "白天", Color.White) { onThemeChanged(themeColor, ThemeMode.LIGHT) }
                    PageThemeModeOption(themeMode == ThemeMode.DARK, "夜间", Color.Black) { onThemeChanged(themeColor, ThemeMode.DARK) }
                    PageThemeModeOption(themeMode == ThemeMode.FOLLOW_SYSTEM, "跟随系统", Color.Gray) { onThemeChanged(themeColor, ThemeMode.FOLLOW_SYSTEM) }
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
                                    onClick = { onThemeChanged(option, themeMode) }
                                )
                            }
                            repeat(3 - rowColors.size) { Spacer(Modifier.width(80.dp)) }
                        }
                    }
                }

                SettingAction(Icons.Default.Image, "聊天背景图", "选择聊天中使用的背景图片", onBackgroundImageClick)
                SettingAction(Icons.Default.Key, "API Key", "配置您的 API 密钥以使用服务", onApiKeyClick)
                SettingAction(Icons.Default.Link, "API Base URL", "配置 API 服务器地址", onApiBaseUrlClick)
                SettingAction(Icons.Default.ChatBubble, "自定义系统提示词", "设置个性化的系统提示词", onCustomPromptClick)
                SettingAction(Icons.Default.Tune, "参数设置", "调整模型参数", onParameterSettingsClick)
                UpdateSettingsItem(updateState, onCheckForUpdate)
                PrereleaseUpdateSetting(acceptPrereleaseUpdates, onAcceptPrereleaseUpdatesChanged)
                SettingAction(Icons.Default.Info, "关于 MIMO Chat", "应用信息与声明", onAboutClick)
            }
        }
    }
}

@Composable
private fun ShowMeTheCastleBanner() {
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val versionSeparator = versionName.indexOf('-')
    val rawVersionNumber = if (versionSeparator >= 0) versionName.substring(0, versionSeparator) else versionName
    val versionNumber = "V" + rawVersionNumber.removePrefix("v").removePrefix("V")
    val versionSubtitle = if (versionSeparator >= 0) {
        versionName.substring(versionSeparator + 1).replace('-', ' ').trim()
    } else {
        ""
    }
    val cardColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 450),
        label = "castle_card_color"
    )
    val cardContentColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(durationMillis = 450),
        label = "castle_card_content_color"
    )
    val accentColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 450),
        label = "castle_card_accent_color"
    )
    val coverUrl by produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(
                    "https://music.163.com/api/song/detail/?id=2630817670&ids=%5B2630817670%5D"
                ).openConnection().apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Referer", "https://music.163.com/")
                }
                val body = connection.getInputStream().bufferedReader().use { it.readText() }
                val song = JSONObject(body).getJSONArray("songs").getJSONObject(0)
                (song.optJSONObject("al") ?: song.optJSONObject("album"))
                    ?.optString("picUrl")
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }
    }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://music.163.com/song?id=2630817670"))
                )
            },
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        contentColor = cardContentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(cardContentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = accentColor
                )
                coverUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Show me the castle 封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val availableWidth = maxWidth
                    val initialTitleFontSize = MaterialTheme.typography.titleLarge.fontSize
                    var titleFontSize by remember(versionName, availableWidth, initialTitleFontSize) {
                        mutableStateOf(initialTitleFontSize)
                    }
                    Text(
                        modifier = Modifier.widthIn(max = availableWidth),
                        text = buildAnnotatedString {
                            append(versionNumber)
                            if (versionSubtitle.isNotEmpty()) {
                                append(" ")
                                withStyle(
                                    SpanStyle(
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(versionSubtitle)
                                }
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = titleFontSize),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        onTextLayout = { result ->
                            if (result.didOverflowWidth && titleFontSize > 12.sp) {
                                titleFontSize = (titleFontSize.value - 1f).coerceAtLeast(12f).sp
                            }
                        }
                    )
                }
                Text(
                    text = "上传附件，打开相机，让 MiMo Chat 看到一座由全新 UI 与动效砌成的雄伟城堡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardContentColor.copy(alpha = 0.82f)
                )
            }
        }
    }
}
