package com.mroldl001.mimochat.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
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
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.chat.ApiKeyDialog
import com.mroldl001.mimochat.ui.chat.CustomSystemPromptDialog
import com.mroldl001.mimochat.ui.chat.ParameterSettingsDialog
import com.mroldl001.mimochat.ui.chat.components.*
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatViewModel
import com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState
import com.mroldl001.mimochat.ui.theme.CodeBlockColorMode
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.theme.supportsDynamicColor
import com.mroldl001.mimochat.ui.theme.themePreviewColorScheme
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.settings.AppLocale
import com.mroldl001.mimochat.ui.settings.LanguageSettingsDialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit,
    onCodeBlockColorModeChanged: (CodeBlockColorMode) -> Unit = {},
    onNavigateToDisclaimer: () -> Unit,
    onNavigateToExperimentalFeatures: () -> Unit,
    onNavigateToEasterEggHistory: () -> Unit,
    isExpandedScreen: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    appLanguage: String = AppLocale.SYSTEM,
    onLanguageSelected: (String) -> Unit = {},
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
    var showCustomColor by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf(viewModel.getCustomThemeColorHex()) }
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
        onAboutClick = { showAbout = true },
        onEasterEggHistoryClick = onNavigateToEasterEggHistory,
        onNavigateBack = onNavigateBack,
        onExperimentalFeaturesClick = onNavigateToExperimentalFeatures,
        customColorHex = customColorHex,
        onCustomColorClicked = { showCustomColor = true },
        onLanguageClick = { showLanguageDialog = true },
        codeBlockColorMode = uiState.codeBlockColorMode,
        onCodeBlockColorModeSelected = { mode ->
            viewModel.setCodeBlockColorMode(mode)
            onCodeBlockColorModeChanged(mode)
        },
        isExpandedScreen = isExpandedScreen,
        scrollState = scrollState,
        appLanguage = appLanguage,
        onLanguageSelected = onLanguageSelected,
        showUsage = uiState.showUsage,
        onShowUsageChanged = viewModel::setShowUsage
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
    if (showCustomColor) {
        CustomColorDialog(
            initialHex = viewModel.getCustomThemeColorHex(),
            onCancel = { showCustomColor = false },
            onSave = { hex ->
                viewModel.setCustomThemeColorHex(hex)
                customColorHex = hex
                onThemeChanged(ThemeColor.CUSTOM, uiState.themeMode)
                showCustomColor = false
            }
        )
    }
    if (showLanguageDialog) {
        LanguageSettingsDialog(
            currentLanguage = appLanguage,
            onLanguageSelected = { lang ->
                onLanguageSelected(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
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
    onAboutClick: () -> Unit,
    onEasterEggHistoryClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onExperimentalFeaturesClick: () -> Unit,
        customColorHex: String,
        onCustomColorClicked: () -> Unit,
        onLanguageClick: () -> Unit,
    codeBlockColorMode: CodeBlockColorMode,
    onCodeBlockColorModeSelected: (CodeBlockColorMode) -> Unit,
    isExpandedScreen: Boolean,
    scrollState: ScrollState,
    appLanguage: String,
    onLanguageSelected: (String) -> Unit,
    showUsage: Boolean = false,
    onShowUsageChanged: (Boolean) -> Unit = {}
) {
    val pageColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 450),
        label = "settings_page_color"
    )
    val previewDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageColor,
                    titleContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "settings_title"),
                    navigationIconContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "settings_back_icon")
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopStart
        ) {
            val columnModifier = if (isExpandedScreen) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.widthIn(max = 560.dp).fillMaxWidth()
            }
            Column(
                modifier = columnModifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SteelBallRunBanner()

                SettingsGroupTitle(stringResource(R.string.group_appearance))

                SettingSectionHeader(Icons.Outlined.Brightness7, stringResource(R.string.display_mode))
                // 当前主题为自定义色彩时，显示模式预览也要用用户设置的自定义色（否则回退成默认黑色）
                val displayModeCustomHex = if (themeColor == ThemeColor.CUSTOM) customColorHex else null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isExpandedScreen) {
                        Arrangement.spacedBy(12.dp, Alignment.Start)
                    } else {
                        Arrangement.SpaceEvenly
                    }
                ) {
                    ThemePreviewCard(
                        label = stringResource(R.string.preview_day),
                        scheme = themePreviewColorScheme(themeColor, dark = false, customColorHex = displayModeCustomHex),
                        selected = themeMode == ThemeMode.LIGHT
                    ) { onThemeChanged(themeColor, ThemeMode.LIGHT) }
                    ThemePreviewCard(
                        label = stringResource(R.string.preview_night),
                        scheme = themePreviewColorScheme(themeColor, dark = true, customColorHex = displayModeCustomHex),
                        selected = themeMode == ThemeMode.DARK
                    ) { onThemeChanged(themeColor, ThemeMode.DARK) }
                    ThemePreviewCard(
                        label = stringResource(R.string.preview_follow_system),
                        scheme = themePreviewColorScheme(themeColor, dark = false, customColorHex = displayModeCustomHex),
                        bottomScheme = themePreviewColorScheme(themeColor, dark = true, customColorHex = displayModeCustomHex),
                        selected = themeMode == ThemeMode.FOLLOW_SYSTEM
                    ) { onThemeChanged(themeColor, ThemeMode.FOLLOW_SYSTEM) }
                }

                SettingSectionHeader(Icons.Outlined.Palette, stringResource(R.string.theme_color))
                val colors = buildList {
                    add(ThemeColor.WHITE)
                    add(ThemeColor.CUSTOM)
                    if (supportsDynamicColor()) add(ThemeColor.AUTO_COLOR)
                    add(ThemeColor.HATSUNE_MIKU)
                    add(ThemeColor.TETO_RED)
                    add(ThemeColor.MI_ORANGE)
                    add(ThemeColor.GREEN)
                    add(ThemeColor.PURPLE)
                    add(ThemeColor.DEEP_BLUE)
                }
                if (isExpandedScreen) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colors, key = { it.name }) { option ->
                            ThemePreviewCard(
                                label = themeColorLabel(option),
                                scheme = themePreviewColorScheme(option, dark = previewDark, customColorHex = if (option == ThemeColor.CUSTOM) customColorHex else null),
                                selected = themeColor == option,
                                width = 104.dp
                            ) { if (option == ThemeColor.CUSTOM) onCustomColorClicked() else onThemeChanged(option, themeMode) }
                        }
                    }
                } else {
                    // 与显示模式行的首卡左对齐：SpaceEvenly 的首卡左侧留白 = 剩余空间 / 4
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val themeRowStartPadding = ((maxWidth - 100.dp * 3) / 4).coerceAtLeast(0.dp)
                        LazyRow(
                            contentPadding = PaddingValues(start = themeRowStartPadding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(colors, key = { it.name }) { option ->
                                ThemePreviewCard(
                                    label = themeColorLabel(option),
                                    scheme = themePreviewColorScheme(option, dark = previewDark, customColorHex = if (option == ThemeColor.CUSTOM) customColorHex else null),
                                    selected = themeColor == option,
                                    width = 104.dp
                                ) { if (option == ThemeColor.CUSTOM) onCustomColorClicked() else onThemeChanged(option, themeMode) }
                            }
                        }
                    }
                }

                SettingSectionHeader(Icons.Outlined.Code, stringResource(R.string.code_block_color))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isExpandedScreen) {
                        Arrangement.spacedBy(12.dp, Alignment.Start)
                    } else {
                        Arrangement.SpaceEvenly
                    }
                ) {
                    CodeBlockPreviewCard(
                        label = stringResource(R.string.code_block_dark),
                        dark = true,
                        selected = codeBlockColorMode == CodeBlockColorMode.DARK
                    ) { onCodeBlockColorModeSelected(CodeBlockColorMode.DARK) }
                    CodeBlockPreviewCard(
                        label = stringResource(R.string.code_block_light),
                        dark = false,
                        selected = codeBlockColorMode == CodeBlockColorMode.LIGHT
                    ) { onCodeBlockColorModeSelected(CodeBlockColorMode.LIGHT) }
                    CodeBlockPreviewCard(
                        label = stringResource(R.string.code_block_follow),
                        dark = previewDark,
                        // 上浅下深硬分割，直观表达「跟随切换」
                        split = true,
                        selected = codeBlockColorMode == CodeBlockColorMode.FOLLOW
                    ) { onCodeBlockColorModeSelected(CodeBlockColorMode.FOLLOW) }
                }

                SettingAction(Icons.Outlined.Image, stringResource(R.string.chat_background), stringResource(R.string.chat_background_desc), onBackgroundImageClick)

                SettingsGroupTitle(stringResource(R.string.group_api))

                SettingAction(Icons.Outlined.VpnKey, stringResource(R.string.api_key), stringResource(R.string.api_key_desc), onApiKeyClick)
                SettingAction(Icons.Outlined.Link, stringResource(R.string.api_base_url), stringResource(R.string.api_base_url_desc), onApiBaseUrlClick)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowUsageChanged(!showUsage) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingPageIcon(Icons.Outlined.AccountBalanceWallet)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.usage_setting_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.usage_setting_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = showUsage, onCheckedChange = onShowUsageChanged)
                }

                SettingsGroupTitle(stringResource(R.string.group_experience))

                SettingAction(
                    Icons.Outlined.Translate,
                    stringResource(R.string.language),
                    AppLocale.label(appLanguage),
                    onClick = onLanguageClick
                )
                SettingAction(Icons.Outlined.Chat, stringResource(R.string.custom_system_prompt), stringResource(R.string.custom_system_prompt_desc), onCustomPromptClick)
                SettingAction(Icons.Outlined.Tune, stringResource(R.string.parameter_settings), stringResource(R.string.parameter_settings_desc), onParameterSettingsClick)

                SettingsGroupTitle(stringResource(R.string.group_other))

                SettingAction(Icons.Outlined.Science, stringResource(R.string.experimental_features), stringResource(R.string.experimental_features_desc), onExperimentalFeaturesClick)
                UpdateSettingsItem(updateState, onCheckForUpdate)
                SettingAction(Icons.Outlined.Egg, stringResource(R.string.history_easter_egg), stringResource(R.string.history_easter_egg_desc), onEasterEggHistoryClick)
                SettingAction(Icons.Outlined.Info, stringResource(R.string.about_mchat), stringResource(R.string.about_mchat_desc), onAboutClick)
            }
        }
    }
}

private suspend fun fetchAniListCover(title: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val query = "query (\$search: String) { Media (search: \$search, type: ANIME) { coverImage { extraLarge large } } }"
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", JSONObject().apply { put("search", title) })
        }.toString()
        val connection = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return@runCatching null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val cover = JSONObject(body)
                .optJSONObject("data")?.optJSONObject("Media")
                ?.optJSONObject("coverImage")
            cover?.optString("extraLarge")?.takeIf { it.isNotBlank() }
                ?: cover?.optString("large")?.takeIf { it.isNotBlank() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

@Composable
private fun SteelBallRunBanner() {
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
        value = fetchAniListCover("JoJo's Bizarre Adventure: Steel Ball Run")
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
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://zh.moegirl.org.cn/" + URLEncoder.encode("飙马野郎", "UTF-8"))
                    )
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
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Egg,
                    contentDescription = null,
                    tint = accentColor
                )
                coverUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = stringResource(R.string.banner_cover_desc),
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
                    text = stringResource(R.string.easter_egg_sbr_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardContentColor.copy(alpha = 0.82f)
                )
            }
        }
    }
}
