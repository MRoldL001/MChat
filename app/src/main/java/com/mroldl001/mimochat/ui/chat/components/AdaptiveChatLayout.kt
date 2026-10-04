package com.mroldl001.mimochat.ui.chat.components

import com.mroldl001.mimochat.R
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
    onRefreshUsage: () -> Unit = {},
    onLoginUsage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val bottomProximityPx = with(LocalDensity.current) { 120.dp.roundToPx() }
    val scrollButtonTravelPx = with(LocalDensity.current) { 76.dp.roundToPx() }

    var settingsAnchorBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val settingsPageTransition = rememberSettingsTransition()
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MRoldL001/MChat"))
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
                                text = stringResource(R.string.no_chat_history),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val historyListState = rememberLazyListState()
                        val isAnimating = remember { mutableStateOf(false) }
                        val settledChatId = remember { mutableStateOf(uiState.currentChat?.id) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                ) {
                            val historyEntries = remember(uiState.chats) { buildChatHistoryEntries(uiState.chats) }
                            val selectedHistoryIndex = historyEntries.indexOfFirst {
                                it is ChatHistoryEntry.Item && it.chat.id == uiState.currentChat?.id
                            }
                            ChatSelectionHighlight(
                                listState = historyListState,
                                selectedIndex = selectedHistoryIndex,
                                selectedChatId = uiState.currentChat?.id,
                                isAnimating = isAnimating,
                                settledChatId = settledChatId,
                                itemHeight = 72.dp
                            )
                            LazyColumn(
                                state = historyListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(historyEntries, key = { it.key }) { entry ->
                                    when (entry) {
                                        is ChatHistoryEntry.Header -> ChatListDateHeader(entry.title)
                                        is ChatHistoryEntry.Item -> TabletChatListItem(
                                            chat = entry.chat,
                                            isSelected = entry.chat.id == uiState.currentChat?.id,
                                            settledSelectedChatId = settledChatId.value,
                                            isAnimating = isAnimating.value,
                                            onClick = {
                                                focusManager.clearFocus()
                                                onSelectChat(entry.chat)
                                            },
                                            onDelete = { chatToDelete = entry.chat; showDeleteConfirmDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.showUsage) {
                        UsageWithNewChatCard(
                            loading = uiState.usageLoading,
                            loggedIn = uiState.usageLoggedIn,
                            usageText = uiState.usageText,
                            onRefresh = onRefreshUsage,
                            onLogin = onLoginUsage,
                            onCreateNewChat = {
                                focusManager.clearFocus()
                                onCreateNewChat()
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
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
                            Text(stringResource(R.string.new_chat))
                        }
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
                        items(
                            items = messages,
                            key = { message -> message.id },
                            contentType = { message -> message.role }
                        ) { message ->
                            MessageBubble(message = message)
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
                            contentDescription = stringResource(R.string.jump_to_bottom)
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
        TabletParameterSettingsDialog(
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
            title = { Text(stringResource(R.string.hint)) },
            text = { Text(stringResource(R.string.no_api_key)) },
            confirmButton = {
                TextButton(onClick = { showApiKeyWarningDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
    
    if (showApiKeyDialog) {
        TabletApiKeyDialog(
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
        TabletCustomSystemPromptDialog(
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
                    text = stringResource(R.string.confirm_delete),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_delete_message),
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
                    Text(stringResource(R.string.confirm))
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
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
