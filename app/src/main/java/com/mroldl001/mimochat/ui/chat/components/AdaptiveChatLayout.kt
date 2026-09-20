package com.mroldl001.mimochat.ui.chat.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.domain.model.Message
import com.mroldl001.mimochat.domain.model.AIModel
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatUiState
import com.mroldl001.mimochat.ui.chat.viewmodel.SkillType
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.theme.supportsDynamicColor
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveChatLayout(
    uiState: ChatUiState,
    messages: List<Message>,
    streamingContent: String,
    streamingReasoning: String,
    isStreaming: Boolean,
    isThinkingMode: Boolean,
    isWebSearchEnabled: Boolean,
    onThinkingModeChanged: (Boolean) -> Unit,
    onWebSearchChanged: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    onSelectChat: (Chat) -> Unit,
    onSelectModel: (AIModel) -> Unit,
    onSetActiveSkill: (SkillType?) -> Unit,
    onThemeColorChanged: (ThemeColor) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onApiKeySaved: (String) -> Unit,
    onApiBaseUrlSaved: (String) -> Unit,
    onCustomPromptSaved: (String) -> Unit,
    chatBackgroundUri: String?,
    chatBackgroundOpacity: Float,
    onSelectBackgroundImage: () -> Unit,
    onBackgroundOpacityChanged: (Float) -> Unit,
    onRestoreBackgroundDefault: () -> Unit,
    onTemperatureSaved: (Float) -> Unit,
    onTopPSaved: (Float) -> Unit,
    onFrequencyPenaltySaved: (Float) -> Unit,
    onPresencePenaltySaved: (Float) -> Unit,
    onResetParameters: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onAcceptPrereleaseUpdatesChanged: (Boolean) -> Unit,
    onDownloadUpdate: (com.mroldl001.mimochat.data.update.GitHubRelease) -> Unit,
    onClearUpdateState: () -> Unit,
    onClearError: () -> Unit,
    onTakePhoto: () -> Unit = {},
    onSelectFile: () -> Unit = {},
    onAttachmentCleared: () -> Unit = {},
    attachmentLabel: String? = null,
    attachmentUri: String? = null,
    attachmentMimeType: String? = null,
    isAttachmentEnabled: Boolean = true,
    initialChatId: Long? = null,
    chatScrollPositions: MutableMap<Long, ChatScrollPosition>,
    loadChatScrollPosition: (Long) -> ChatScrollPosition? = { null },
    onChatScrollPositionChanged: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onCurrentChatChanged: (Long?) -> Unit = {},
    suppressInitialScroll: Boolean = false,
    onInitialChatNavigationHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val bottomProximityPx = with(LocalDensity.current) { 120.dp.roundToPx() }
    val scrollButtonTravelPx = with(LocalDensity.current) { 76.dp.roundToPx() }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var settingsAnchorBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val settingsPageTransition = rememberSettingsTransition()
    var showAdvancedSettingsDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showApiBaseUrlDialog by remember { mutableStateOf(false) }
    var showCustomPromptDialog by remember { mutableStateOf(false) }
    var showParameterSettingsDialog by remember { mutableStateOf(false) }
    var showBackgroundSettingsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showApiKeyWarningDialog by remember { mutableStateOf(false) }
    
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    
    var currentApiKey by remember { mutableStateOf("") }
    var currentApiBaseUrl by remember { mutableStateOf("") }
    var currentCustomPrompt by remember { mutableStateOf("") }
    
    var pendingRestoreChatId by remember { mutableStateOf(uiState.currentChat?.id) }
    var pendingInitialTopChatId by remember(initialChatId, suppressInitialScroll) {
        mutableStateOf(initialChatId.takeIf { suppressInitialScroll })
    }
    var pendingSendMessageCount by remember { mutableStateOf<Int?>(null) }
    var followStreaming by remember { mutableStateOf(false) }
    var automaticStreamScroll by remember { mutableStateOf(false) }

    val isNearBottom by remember(listState, bottomProximityPx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastIndex = layoutInfo.totalItemsCount - 1
            if (lastIndex < 0) {
                true
            } else {
                val lastItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex }
                lastItem != null &&
                    lastItem.offset + lastItem.size - layoutInfo.viewportEndOffset <= bottomProximityPx
            }
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > 0 &&
                pendingRestoreChatId == null &&
                !isNearBottom
        }
    }

    PersistChatScrollPosition(
        chatId = uiState.currentChat?.id,
        listState = listState,
        onPositionChanged = onChatScrollPositionChanged
    )

    LaunchedEffect(uiState.currentChat?.id) {
        val currentChatId = uiState.currentChat?.id
        followStreaming = false
        if (currentChatId != null) onCurrentChatChanged(currentChatId)
        pendingRestoreChatId = when {
            currentChatId == null -> null
            pendingInitialTopChatId == currentChatId -> null
            pendingSendMessageCount != null -> null
            else -> currentChatId
        }
    }

    LaunchedEffect(messages.size, pendingSendMessageCount) {
        val previousCount = pendingSendMessageCount
        if (previousCount != null && messages.size > previousCount) {
            val itemCount = snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it >= messages.size }
            if (itemCount > 0) {
                automaticStreamScroll = true
                try {
                    listState.scrollToBottomContent()
                } finally {
                    automaticStreamScroll = false
                }
            }
            pendingSendMessageCount = null
            pendingRestoreChatId = null
            followStreaming = isStreaming
        }
    }

    LaunchedEffect(isStreaming) {
        if (!isStreaming) {
            followStreaming = false
        } else if (
            pendingInitialTopChatId == null &&
            pendingRestoreChatId == null &&
            isNearBottom
        ) {
            followStreaming = true
        }
    }

    LaunchedEffect(listState, isStreaming, bottomProximityPx) {
        snapshotFlow {
            listState.isScrollInProgress to isNearBottom
        }.collect { (isScrolling, nearBottom) ->
            if (isStreaming && !automaticStreamScroll) {
                if (isScrolling && !nearBottom) {
                    followStreaming = false
                } else if (!isScrolling && nearBottom) {
                    followStreaming = true
                }
            }
        }
    }

    LaunchedEffect(streamingContent.length, streamingReasoning.length, isStreaming, followStreaming) {
        if (isStreaming && followStreaming) {
            automaticStreamScroll = true
            try {
                withFrameNanos { }
                listState.scrollToBottomContent()
            } finally {
                automaticStreamScroll = false
            }
        }
    }

    LaunchedEffect(
        uiState.currentChat?.id,
        messages.size,
        messages.lastOrNull()?.chatId,
        pendingInitialTopChatId
    ) {
        val targetChatId = pendingInitialTopChatId
        if (
            targetChatId != null &&
            uiState.currentChat?.id == targetChatId &&
            messages.lastOrNull()?.chatId == targetChatId
        ) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it >= messages.size && it > 0 }
            listState.scrollToItem(0)
            chatScrollPositions[targetChatId] = ChatScrollPosition(0, 0)
            onChatScrollPositionChanged(targetChatId, 0, 0)
            pendingRestoreChatId = null
            pendingInitialTopChatId = null
            onInitialChatNavigationHandled()
        }
    }

    LaunchedEffect(
        uiState.currentChat?.id,
        messages.size,
        messages.lastOrNull()?.chatId,
        pendingRestoreChatId,
        isStreaming
    ) {
        val targetChatId = pendingRestoreChatId ?: return@LaunchedEffect
        if (uiState.currentChat?.id != targetChatId) return@LaunchedEffect
        if (messages.isEmpty() || messages.lastOrNull()?.chatId != targetChatId) {
            return@LaunchedEffect
        }

        val totalItems = messages.size + if (isStreaming) 1 else 0
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it >= totalItems }

        val savedPosition = chatScrollPositions[targetChatId]
            ?: loadChatScrollPosition(targetChatId)?.also {
                chatScrollPositions[targetChatId] = it
            }
        automaticStreamScroll = true
        try {
            if (savedPosition != null) {
                listState.scrollToItem(
                    index = savedPosition.index.coerceIn(0, totalItems - 1),
                    scrollOffset = savedPosition.offset.coerceAtLeast(0)
                )
            } else {
                listState.scrollToBottomContent()
            }
        } finally {
            automaticStreamScroll = false
        }
        pendingRestoreChatId = null
    }

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier.width(ChatSidebarWidth),
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ChatHistoryHeader(
                        onSettingsBoundsChanged = { settingsAnchorBounds = it },
                        onSearchClick = {
                            focusManager.clearFocus()
                            onNavigateToSearch()
                        },
                        onSettingsClick = {
                            focusManager.clearFocus()
                            onNavigateToSettings()
                        },
                        onGitHubClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MRoldL001/MIMO-Chat"))
                            context.startActivity(intent)
                        }
                    )

                    if (uiState.chats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无对话记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val historyListState = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                ) {
                            ChatSelectionHighlight(
                                listState = historyListState,
                                selectedIndex = uiState.chats.indexOfFirst { it.id == uiState.currentChat?.id }
                            )
                            LazyColumn(
                                state = historyListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.chats, key = { it.id }) { chat ->
                                    TabletChatListItem(
                                        chat = chat,
                                        isSelected = chat.id == uiState.currentChat?.id,
                                        onClick = {
                                            focusManager.clearFocus()
                                            onSelectChat(chat)
                                        },
                                        onDelete = { chatItem ->
                                            chatToDelete = chatItem
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onCreateNewChat()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新建对话")
                    }
                }
            }
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        ModelSelector(
                            currentModel = uiState.selectedModel,
                            models = uiState.availableModels,
                            onModelSelected = onSelectModel
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .imePadding() // 修复输入法弹出时输入框不被顶起的 bug
                        .navigationBarsPadding()
                ) {
                    SkillToggleBar(
                        isThinkingMode = isThinkingMode,
                        isWebSearchEnabled = isWebSearchEnabled,
                        activeSkill = uiState.activeSkill,
                        isGenerating = isStreaming,
                        onThinkingModeToggle = { newValue ->
                            onThinkingModeChanged(newValue)
                        },
                        onWebSearchToggle = onWebSearchChanged,
                        onSkillToggle = { skill ->
                            if (skill != null) {
                                onThinkingModeChanged(false)
                            }
                            onSetActiveSkill(skill)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    InputBar(
                        onSendMessage = {
                            if (uiState.apiKey.isBlank()) {
                                showApiKeyWarningDialog = true
                            } else {
                                pendingRestoreChatId = null
                                pendingSendMessageCount = messages.size
                                onSendMessage(it)
                            }
                        },
                        onStopGenerating = onStopGenerating,
                        isGenerating = isStreaming,
                        onTakePhoto = onTakePhoto,
                        onSelectFile = onSelectFile,
                        onAttachmentCleared = onAttachmentCleared,
                        attachmentLabel = attachmentLabel,
                        attachmentUri = attachmentUri,
                        attachmentMimeType = attachmentMimeType,
                        isAttachmentEnabled = isAttachmentEnabled
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { 
                        focusManager.clearFocus()
                    }
            ) {
                chatBackgroundUri?.takeIf { it.isNotBlank() }?.let { backgroundUri ->
                    AsyncImage(
                        model = backgroundUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(chatBackgroundOpacity)
                    )
                }

                if (messages.isEmpty() && !isStreaming) {
                    TabletEmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages.size) { index ->
                            MessageBubble(message = messages[index])
                        }
                        if (isStreaming) {
                            item {
                                StreamingMessageBubble(
                                    content = streamingContent,
                                    reasoningContent = streamingReasoning
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showScrollToBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 16.dp),
                    enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                        initialOffsetX = { scrollButtonTravelPx }
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        targetOffsetX = { scrollButtonTravelPx }
                    ) + fadeOut(animationSpec = tween(durationMillis = 160))
                ) {
                    FilledIconButton(
                        onClick = {
                            scrollScope.launch {
                                automaticStreamScroll = true
                                try {
                                    listState.animateScrollToBottomContent()
                                    followStreaming = isStreaming
                                } finally {
                                    automaticStreamScroll = false
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "跳转到底部"
                        )
                    }
                }

                LaunchedEffect(uiState.error) {
                    uiState.error?.let { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        onClearError()
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsPage(
            initialThemeColor = uiState.themeColor,
            initialThemeMode = uiState.themeMode,
            onThemeChanged = { color, mode ->
                onThemeColorChanged(color)
                onThemeModeChanged(mode)
                onThemeChanged(color, mode)
            },
            onApiKeyClick = {
                showSettingsDialog = false
                showApiKeyDialog = true
            },
            onBackgroundImageClick = {
                showSettingsDialog = false
                showBackgroundSettingsDialog = true
            },
            updateState = uiState.updateState,
            onCheckForUpdate = onCheckForUpdate,
            onParameterSettingsClick = {
                showSettingsDialog = false
                showParameterSettingsDialog = true
            },
            onCustomPromptClick = {
                showSettingsDialog = false
                showCustomPromptDialog = true
            },
            onApiBaseUrlClick = {
                showSettingsDialog = false
                showApiBaseUrlDialog = true
            },
            acceptPrereleaseUpdates = uiState.acceptPrereleaseUpdates,
            onAcceptPrereleaseUpdatesChanged = onAcceptPrereleaseUpdatesChanged,
            onDismiss = { showSettingsDialog = false }
        )
    }

    (uiState.updateState as? com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState.Available)?.let { update ->
        UpdateReleaseDialog(
            release = update.release,
            onDismiss = onClearUpdateState,
            onDownload = { onDownloadUpdate(update.release) }
        )
    }

    if (showBackgroundSettingsDialog) {
        BackgroundImageSettingsDialog(
            hasBackgroundImage = !chatBackgroundUri.isNullOrBlank(),
            opacity = chatBackgroundOpacity,
            onSelectImage = onSelectBackgroundImage,
            onOpacityChanged = onBackgroundOpacityChanged,
            onRestoreDefault = onRestoreBackgroundDefault,
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showBackgroundSettingsDialog = false } }
        )
    }

    if (showParameterSettingsDialog) {
        ParameterSettingsDialog(
            initialTemperature = uiState.temperature,
            initialTopP = uiState.topP,
            initialFrequencyPenalty = uiState.frequencyPenalty,
            initialPresencePenalty = uiState.presencePenalty,
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showParameterSettingsDialog = false } },
            onConfirm = { temp, topP, freqPenalty, presPenalty ->
                settingsPageTransition.close {
                    onTemperatureSaved(temp)
                    onTopPSaved(topP)
                    onFrequencyPenaltySaved(freqPenalty)
                    onPresencePenaltySaved(presPenalty)
                    showParameterSettingsDialog = false
                }
            },
            onReset = {
                settingsPageTransition.close {
                    onResetParameters()
                    showParameterSettingsDialog = false
                }
            }
        )
    }
    
    if (showApiKeyWarningDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyWarningDialog = false },
            title = { Text("提示") },
            text = { Text("未设置API Key") },
            confirmButton = {
                TextButton(onClick = { showApiKeyWarningDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = currentApiKey.ifEmpty { uiState.apiKey },
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showApiKeyDialog = false } },
            onConfirm = { key ->
                settingsPageTransition.close {
                    currentApiKey = key
                    onApiKeySaved(key)
                    showApiKeyDialog = false
                }
            }
        )
    }
    
    if (showApiBaseUrlDialog) {
        ApiBaseUrlDialog(
            currentUrl = currentApiBaseUrl.ifEmpty { uiState.apiBaseUrl },
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showApiBaseUrlDialog = false } },
            onConfirm = { url ->
                settingsPageTransition.close {
                    currentApiBaseUrl = url
                    onApiBaseUrlSaved(url)
                    showApiBaseUrlDialog = false
                }
            }
        )
    }
    
    if (showCustomPromptDialog) {
        CustomSystemPromptDialog(
            currentPrompt = currentCustomPrompt.ifEmpty { uiState.customSystemPrompt },
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showCustomPromptDialog = false } },
            onConfirm = { prompt ->
                settingsPageTransition.close {
                    currentCustomPrompt = prompt
                    onCustomPromptSaved(prompt)
                    showCustomPromptDialog = false
                }
            }
        )
    }
    
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "确认删除",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "你真的要删除吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatToDelete?.let { onDeleteChat(it) }
                        showDeleteConfirmDialog = false
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TabletChatListItem(
    chat: Chat,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (Chat) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentOffset by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "tabletChatItemContentOffset"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(72.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { translationX = contentOffset.toPx() }
        ) {
            Text(
                text = chat.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatTimestamp(chat.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onDelete(chat) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun TabletEmptyState(modifier: Modifier = Modifier) {
    val welcomeTexts = listOf(
        "MiMo在这里，今天你要做什么？",
        "MiMo在这里，有什么好主意？",
        "MiMo在这里，一起完成任务吧！",
        "MiMo在这里，シタイだけ探した冒険TONGUE",
        "MiMo在这里，有什么可以帮你的？"
    )
    
    val randomText = remember {
        welcomeTexts.random()
    }
    
    val firstLine = "MiMo在这里，"
    val secondLine = randomText.removePrefix("MiMo在这里，")
    
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = firstLine,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = secondLine,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun TabletSkillSwitchRow(
    isThinkingMode: Boolean,
    onThinkingModeChanged: (Boolean) -> Unit,
    activeSkill: SkillType?,
    isGenerating: Boolean,
    onSkillSelected: (SkillType?) -> Unit
) {
    val isThinkingActive = activeSkill == null && isThinkingMode
    val isThinkingDisabled = isGenerating
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thinkingInteractionSource = remember { MutableInteractionSource() }
        val thinkingBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

        val thinkingBorderColor by animateColorAsState(
            targetValue = if (isThinkingActive) MaterialTheme.colorScheme.primary else Color.Transparent,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "thinking_border_color"
        )

        val thinkingIconTint by animateColorAsState(
            targetValue = if (isThinkingActive) MaterialTheme.colorScheme.primary else if (isThinkingDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "thinking_icon_tint"
        )

        val thinkingTextColor by animateColorAsState(
            targetValue = if (isThinkingActive) MaterialTheme.colorScheme.primary else if (isThinkingDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "thinking_text_color"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = thinkingBackgroundColor,
            border = BorderStroke(width = 2.dp, color = thinkingBorderColor),
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    indication = null,
                    interactionSource = thinkingInteractionSource,
                    enabled = !isGenerating
                ) {
                    if (isThinkingMode) {
                        onThinkingModeChanged(false)
                    } else {
                        onThinkingModeChanged(true)
                        onSkillSelected(null)
                    }
                }
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "思考模式",
                    tint = thinkingIconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "思考",
                    style = MaterialTheme.typography.labelMedium,
                    color = thinkingTextColor
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val poetInteractionSource = remember { MutableInteractionSource() }
        val isPoetActive = activeSkill == SkillType.POET
        val poetBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

        val poetBorderColor by animateColorAsState(
            targetValue = if (isPoetActive) MaterialTheme.colorScheme.primary else Color.Transparent,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "poet_border_color"
        )

        val poetIconTint by animateColorAsState(
            targetValue = if (isPoetActive) MaterialTheme.colorScheme.primary else if (isGenerating) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "poet_icon_tint"
        )

        val poetTextColor by animateColorAsState(
            targetValue = if (isPoetActive) MaterialTheme.colorScheme.primary else if (isGenerating) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "poet_text_color"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = poetBackgroundColor,
            border = BorderStroke(width = 2.dp, color = poetBorderColor),
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    indication = null,
                    interactionSource = poetInteractionSource,
                    enabled = !isGenerating
                ) {
                    if (isPoetActive) {
                        onSkillSelected(null)
                    } else {
                        onSkillSelected(SkillType.POET)
                        onThinkingModeChanged(false)
                    }
                }
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "诗人模式",
                    tint = poetIconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "诗人",
                    style = MaterialTheme.typography.labelMedium,
                    color = poetTextColor
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val learningInteractionSource = remember { MutableInteractionSource() }
        val isLearningActive = activeSkill == SkillType.LEARNING
        val learningBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

        val learningBorderColor by animateColorAsState(
            targetValue = if (isLearningActive) MaterialTheme.colorScheme.primary else Color.Transparent,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "learning_border_color"
        )

        val learningIconTint by animateColorAsState(
            targetValue = if (isLearningActive) MaterialTheme.colorScheme.primary else if (isGenerating) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "learning_icon_tint"
        )

        val learningTextColor by animateColorAsState(
            targetValue = if (isLearningActive) MaterialTheme.colorScheme.primary else if (isGenerating) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "learning_text_color"
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = learningBackgroundColor,
            border = BorderStroke(width = 2.dp, color = learningBorderColor),
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    indication = null,
                    interactionSource = learningInteractionSource,
                    enabled = !isGenerating
                ) {
                    if (isLearningActive) {
                        onSkillSelected(null)
                    } else {
                        onSkillSelected(SkillType.LEARNING)
                        onThinkingModeChanged(false)
                    }
                }
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "学习模式",
                    tint = learningIconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "学习",
                    style = MaterialTheme.typography.labelMedium,
                    color = learningTextColor
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsDialog(
    anchorBounds: androidx.compose.ui.geometry.Rect,
    transition: SettingsTransition,
    onApiBaseUrlClick: () -> Unit,
    onParameterSettingsClick: () -> Unit,
    onCustomPromptClick: () -> Unit,
    acceptPrereleaseUpdates: Boolean,
    onAcceptPrereleaseUpdatesChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("高级选项") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 参数设置
                val paramInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = paramInteractionSource,
                            indication = null,
                            onClick = onParameterSettingsClick
                        )
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "参数设置",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "调整模型参数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 自定义系统提示词
                val customPromptInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = customPromptInteractionSource,
                            indication = null,
                            onClick = onCustomPromptClick
                        )
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "自定义系统提示词",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "设置个性化的系统提示词",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // API Base URL
                val apiUrlInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = apiUrlInteractionSource,
                            indication = null,
                            onClick = onApiBaseUrlClick
                        )
                        .padding(vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "API Base URL",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "配置 API 服务器地址",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PrereleaseUpdateSetting(
                    checked = acceptPrereleaseUpdates,
                    onCheckedChange = onAcceptPrereleaseUpdatesChanged
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    anchorBounds: androidx.compose.ui.geometry.Rect,
    initialThemeColor: ThemeColor,
    initialThemeMode: ThemeMode,
    onApply: (ThemeColor, ThemeMode) -> Unit,
    onApiKeyClick: () -> Unit,
    hasBackgroundImage: Boolean,
    onBackgroundImageClick: () -> Unit,
    onAdvancedSettingsClick: () -> Unit,
    updateState: com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val transition = rememberSettingsTransition()
    var tempThemeColor by remember { mutableStateOf(initialThemeColor) }
    var tempThemeMode by remember { mutableStateOf(initialThemeMode) }
    
    LaunchedEffect(initialThemeColor) {
        tempThemeColor = initialThemeColor
    }
    LaunchedEffect(initialThemeMode) {
        tempThemeMode = initialThemeMode
    }
    
    val themeColorNames = mapOf(
        ThemeColor.WHITE to "默认",
        ThemeColor.AUTO_COLOR to "莫奈取色",
        ThemeColor.HATSUNE_MIKU to "初音绿",
        ThemeColor.MI_ORANGE to "小米橙",
        ThemeColor.GREEN to "盎然绿",
        ThemeColor.PURPLE to "罗兰紫"
    )
    
    val themeColorValues = mapOf(
        ThemeColor.WHITE to Color(0xFFFFFFFF),
        ThemeColor.HATSUNE_MIKU to Color(0xFF39C5BB),
        ThemeColor.MI_ORANGE to Color(0xFFFF7E00),
        ThemeColor.GREEN to Color(0xFF006E2A),
        ThemeColor.PURPLE to Color(0xFF6650A4)
    )
    
    val availableColors = buildList {
        add(ThemeColor.WHITE)
        if (supportsDynamicColor()) {
            add(ThemeColor.AUTO_COLOR)
        }
        add(ThemeColor.HATSUNE_MIKU)
        add(ThemeColor.MI_ORANGE)
        add(ThemeColor.GREEN)
        add(ThemeColor.PURPLE)
    }
    
    SettingsContainerDialog(
        anchorBounds = anchorBounds,
        transition = transition,
        onDismissRequest = { transition.close(onDismiss) },
        title = {
            Text("设置")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness7,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "显示模式",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeModeOption(
                            selected = tempThemeMode == ThemeMode.LIGHT,
                            onClick = { tempThemeMode = ThemeMode.LIGHT },
                            label = "白天",
                            color = Color.White
                        )
                        ThemeModeOption(
                            selected = tempThemeMode == ThemeMode.DARK,
                            onClick = { tempThemeMode = ThemeMode.DARK },
                            label = "夜间",
                            color = Color.Black
                        )
                        ThemeModeOption(
                            selected = tempThemeMode == ThemeMode.FOLLOW_SYSTEM,
                            onClick = { tempThemeMode = ThemeMode.FOLLOW_SYSTEM },
                            label = "跟随系统",
                            color = Color.Gray,
                            isDiagonal = false
                        )
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "主题颜色",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        availableColors.forEach { colorOption ->
                            ThemeColorOption(
                                selected = tempThemeColor == colorOption,
                                onClick = { tempThemeColor = colorOption },
                                label = themeColorNames[colorOption] ?: "",
                                color = themeColorValues[colorOption] ?: Color.Gray,
                                isAutoColor = colorOption == ThemeColor.AUTO_COLOR
                            )
                        }
                    }
                }

                val apiKeyInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable(
                            interactionSource = apiKeyInteractionSource,
                            indication = null,
                            onClick = onApiKeyClick
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "API Key",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "配置您的 API 密钥以使用服务",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val backgroundImageInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable(
                            interactionSource = backgroundImageInteractionSource,
                            indication = null,
                            onClick = onBackgroundImageClick
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "聊天背景图",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "选择聊天中使用的背景图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                UpdateSettingsItem(
                    state = updateState,
                    onCheck = onCheckForUpdate
                )

                val advancedSettingsInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable(
                            interactionSource = advancedSettingsInteractionSource,
                            indication = null,
                            onClick = onAdvancedSettingsClick
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "高级选项",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "更多可供修改的选项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { transition.close { onApply(tempThemeColor, tempThemeMode) } }) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { transition.close(onDismiss) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ThemeModeOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    color: Color,
    isDiagonal: Boolean = false
) {
    val isWhiteColor = color == Color.White
    val checkmarkColor = if (isWhiteColor) Color.Black else Color.White
    val interactionSource = remember { MutableInteractionSource() }
    
    val borderColor by animateColorAsState(
        targetValue = themeOptionBorderColor(selected),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "border_color"
    )
    
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(
                    if (isDiagonal) {
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.White,
                                0.5f to Color.White,
                                0.5f to Color.Black,
                                1f to Color.Black
                            ),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(color, color))
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checkScale > 0f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkmarkColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    anchorBounds: androidx.compose.ui.geometry.Rect,
    transition: SettingsTransition,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }

    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        icon = { SettingsDialogIcon(Icons.Default.Key) },
        title = { Text("API Key") },
        text = {
            Column {
                Text(
                    text = "月度套餐用户请在高级选项内将 API Base URL 改为订阅接口",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CustomSystemPromptDialog(
    currentPrompt: String,
    anchorBounds: androidx.compose.ui.geometry.Rect,
    transition: SettingsTransition,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var customPrompt by remember { mutableStateOf(currentPrompt) }

    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        icon = { SettingsDialogIcon(Icons.Default.ChatBubble) },
        title = { Text("自定义系统提示词") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "请输入自定义的系统提示词",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("系统提示词") },
                    placeholder = { Text("在此输入您的自定义提示词...") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(customPrompt) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ParameterSettingsDialog(
    initialTemperature: Float,
    initialTopP: Float,
    initialFrequencyPenalty: Float,
    initialPresencePenalty: Float,
    anchorBounds: androidx.compose.ui.geometry.Rect,
    transition: SettingsTransition,
    onDismiss: () -> Unit,
    onConfirm: (Float, Float, Float, Float) -> Unit,
    onReset: () -> Unit
) {
    var temperature by remember { mutableStateOf(initialTemperature) }
    var topP by remember { mutableStateOf(initialTopP) }
    var frequencyPenalty by remember { mutableStateOf(initialFrequencyPenalty) }
    var presencePenalty by remember { mutableStateOf(initialPresencePenalty) }
    var temperatureText by remember { mutableStateOf(initialTemperature.toString()) }
    var topPText by remember { mutableStateOf(initialTopP.toString()) }
    var frequencyPenaltyText by remember { mutableStateOf(initialFrequencyPenalty.toString()) }
    var presencePenaltyText by remember { mutableStateOf(initialPresencePenalty.toString()) }
    var temperatureError by remember { mutableStateOf(false) }
    var topPError by remember { mutableStateOf(false) }
    var frequencyPenaltyError by remember { mutableStateOf(false) }
    var presencePenaltyError by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    fun validateTemperature(value: String): Float? {
        return try {
            val num = value.toFloat()
            if (num in 0f..2f) {
                temperatureError = false
                num
            } else {
                temperatureError = true
                null
            }
        } catch (e: NumberFormatException) {
            temperatureError = true
            null
        }
    }

    fun validateTopP(value: String): Float? {
        return try {
            val num = value.toFloat()
            if (num in 0f..1f) {
                topPError = false
                num
            } else {
                topPError = true
                null
            }
        } catch (e: NumberFormatException) {
            topPError = true
            null
        }
    }

    fun validateFrequencyPenalty(value: String): Float? {
        return try {
            val num = value.toFloat()
            if (num in -2f..2f) {
                frequencyPenaltyError = false
                num
            } else {
                frequencyPenaltyError = true
                null
            }
        } catch (e: NumberFormatException) {
            frequencyPenaltyError = true
            null
        }
    }

    fun validatePresencePenalty(value: String): Float? {
        return try {
            val num = value.toFloat()
            if (num in -2f..2f) {
                presencePenaltyError = false
                num
            } else {
                presencePenaltyError = true
                null
            }
        } catch (e: NumberFormatException) {
            presencePenaltyError = true
            null
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "恢复默认",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "你真的要恢复默认参数吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        onReset()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = { SettingsDialogIcon(Icons.Default.Tune) },
        title = { Text("参数设置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Temperature
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Temperature",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val tempInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(
                                    interactionSource = tempInteractionSource,
                                    indication = null,
                                    onClick = { }
                                )
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                                value = temperatureText,
                                onValueChange = { newValue ->
                                    temperatureText = newValue
                                    validateTemperature(newValue)?.let {
                                        temperature = it
                                    }
                                },
                                singleLine = true,
                                isError = temperatureError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                    if (temperatureError) {
                        Text(
                            text = "请输入 0 到 2.0 之间的数值",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { 
                            temperature = it
                            temperatureText = String.format("%.2f", it)
                            temperatureError = false
                        },
                        valueRange = 0f..2f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "控制模型输出的随机性。值为 0 时输出接近确定性结果，值越高则输出越具创意和多样性",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Top P
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top P",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val topPInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(
                                    interactionSource = topPInteractionSource,
                                    indication = null,
                                    onClick = { }
                                )
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                                value = topPText,
                                onValueChange = { newValue ->
                                    topPText = newValue
                                    validateTopP(newValue)?.let {
                                        topP = it
                                    }
                                },
                                singleLine = true,
                                isError = topPError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                    if (topPError) {
                        Text(
                            text = "请输入 0.0 到 1.0 之间的数值",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Slider(
                        value = topP,
                        onValueChange = { 
                            topP = it
                            topPText = String.format("%.2f", it)
                            topPError = false
                        },
                        valueRange = 0f..1f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "也称为核采样。模型会从累积概率达到 top_p 的最小 Token 集合中进行采样。一般建议只调整 temperature 或 top_p 其中之一",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Frequency Penalty
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Frequency Penalty",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val freqPenaltyInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(
                                    interactionSource = freqPenaltyInteractionSource,
                                    indication = null,
                                    onClick = { }
                                )
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                                value = frequencyPenaltyText,
                                onValueChange = { newValue ->
                                    frequencyPenaltyText = newValue
                                    validateFrequencyPenalty(newValue)?.let {
                                        frequencyPenalty = it
                                    }
                                },
                                singleLine = true,
                                isError = frequencyPenaltyError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                    if (frequencyPenaltyError) {
                        Text(
                            text = "请输入 -2.0 到 2.0 之间的数值",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Slider(
                        value = frequencyPenalty,
                        onValueChange = { 
                            frequencyPenalty = it
                            frequencyPenaltyText = String.format("%.2f", it)
                            frequencyPenaltyError = false
                        },
                        valueRange = -2f..2f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "根据 Token 在已生成文本中出现的频率进行惩罚。正值可以减少重复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Presence Penalty
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Presence Penalty",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val presPenaltyInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(
                                    interactionSource = presPenaltyInteractionSource,
                                    indication = null,
                                    onClick = { }
                                )
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                                value = presencePenaltyText,
                                onValueChange = { newValue ->
                                    presencePenaltyText = newValue
                                    validatePresencePenalty(newValue)?.let {
                                        presencePenalty = it
                                    }
                                },
                                singleLine = true,
                                isError = presencePenaltyError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                    if (presencePenaltyError) {
                        Text(
                            text = "请输入 -2.0 到 2.0 之间的数值",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Slider(
                        value = presencePenalty,
                        onValueChange = { 
                            presencePenalty = it
                            presencePenaltyText = String.format("%.2f", it)
                            presencePenaltyError = false
                        },
                        valueRange = -2f..2f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            activeTickColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "根据 Token 是否已在生成的文本中出现过进行惩罚，不考虑出现频率。正值鼓励模型引入新话题",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val validatedTemp = validateTemperature(temperatureText)
                    val validatedTopP = validateTopP(topPText)
                    val validatedFreqPenalty = validateFrequencyPenalty(frequencyPenaltyText)
                    val validatedPresPenalty = validatePresencePenalty(presencePenaltyText)
                    if (validatedTemp != null && validatedTopP != null && validatedFreqPenalty != null && validatedPresPenalty != null) {
                        onConfirm(validatedTemp, validatedTopP, validatedFreqPenalty, validatedPresPenalty)
                    }
                },
                enabled = !temperatureError && !topPError && !frequencyPenaltyError && !presencePenaltyError
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("取消")
                }
                TextButton(
                    onClick = { showResetConfirmDialog = true }
                ) {
                    Text("恢复默认")
                }
            }
        }
    )
}

@Composable
private fun ThemeColorOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    color: Color,
    isAutoColor: Boolean = false
) {
    val isWhiteColor = color == Color.White
    val checkmarkColor = if (isWhiteColor) Color.Black else Color.White
    val interactionSource = remember { MutableInteractionSource() }
    
    val borderColor by animateColorAsState(
        targetValue = themeOptionBorderColor(selected),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "border_color"
    )
    
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(
                    if (isAutoColor) {
                        Brush.sweepGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF9BC4E2),
                                0.125f to Color(0xFFB4C7E7),
                                0.25f to Color(0xFFD4A373),
                                0.375f to Color(0xFFE6A57E),
                                0.5f to Color(0xFFE7D8C9),
                                0.625f to Color(0xFFC9D4BF),
                                0.75f to Color(0xFF8FA6CB),
                                0.875f to Color(0xFF9BC4E2),
                                1.0f to Color(0xFF9BC4E2)
                            )
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(color, color))
                    }
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checkScale > 0f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = checkmarkColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = checkScale
                            scaleY = checkScale
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
