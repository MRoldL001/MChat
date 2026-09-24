package com.mroldl001.mimochat.ui.chat
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.chat.components.MiMoLoginScreen

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import android.provider.OpenableColumns
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.mroldl001.mimochat.ui.chat.components.*
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatViewModel
import com.mroldl001.mimochat.data.api.*
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File

private fun LazyListState.isNearBottom(thresholdPx: Int): Boolean {
    val layoutInfo = layoutInfo
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return true

    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastIndex }
        ?: return false
    val distanceToBottom = lastItem.offset + lastItem.size - layoutInfo.viewportEndOffset
    return distanceToBottom <= thresholdPx
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    isExpandedScreen: Boolean = false,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: (Long) -> Unit = {},
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit = { _, _ -> },
    onNavigateFromDrawer: (Boolean) -> Unit = {},
    initialChatId: Long? = null,
    chatScrollPositions: MutableMap<Long, ChatScrollPosition>,
    loadChatScrollPosition: (Long) -> ChatScrollPosition? = { null },
    onChatScrollPositionChanged: (Long, Int, Int) -> Unit = { _, _, _ -> },
    onCurrentChatChanged: (Long?) -> Unit = {},
    suppressInitialScroll: Boolean = false,
    onInitialChatNavigationHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages = viewModel.messages
    val context = LocalContext.current
    val streamingContent by viewModel.streamingContent
    val streamingReasoning by viewModel.streamingReasoning
    val isStreaming by viewModel.isStreaming
    var attachment by remember { mutableStateOf<ContentPart?>(null) }
    var attachmentLabel by remember { mutableStateOf<String?>(null) }
    var attachmentUri by remember { mutableStateOf<String?>(null) }
    var attachmentMimeType by remember { mutableStateOf<String?>(null) }
    var attachmentOwnedPath by remember { mutableStateOf<String?>(null) }
    var showMiMoLogin by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var showBackgroundSettingsDialog by remember { mutableStateOf(false) }
    var pendingBackgroundCropUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingInitialChatId by remember(initialChatId) { mutableStateOf(initialChatId) }
    var pendingInitialTopChatId by remember(initialChatId, suppressInitialScroll) {
        mutableStateOf(initialChatId.takeIf { suppressInitialScroll })
    }
    fun clearAttachment(deleteOwnedFile: Boolean = false) {
        if (deleteOwnedFile) {
            attachmentOwnedPath?.let { path -> runCatching { File(path).delete() } }
        }
        attachment = null
        attachmentLabel = null
        attachmentUri = null
        attachmentMimeType = null
        attachmentOwnedPath = null
    }

    fun prepareAttachment(uri: Uri, mimeOverride: String? = null): Boolean {
        var displayName: String? = null
        var rawSize = -1L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        rawSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        }

        val mime = mimeOverride ?: context.contentResolver.getType(uri) ?: "application/octet-stream"
        if (!mime.startsWith("image/") && !mime.startsWith("audio/") && !mime.startsWith("video/")) {
            Toast.makeText(context, context.getString(R.string.toast_attachment_type), Toast.LENGTH_LONG).show()
            return false
        }

        val maxEncodedBytes = 50L * 1024L * 1024L
        if (rawSize > 0L) {
            val estimatedEncodedSize = ((rawSize + 2L) / 3L) * 4L
            if (estimatedEncodedSize > maxEncodedBytes) {
                Toast.makeText(context, context.getString(R.string.toast_file_too_large), Toast.LENGTH_LONG).show()
                return false
            }
        }

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.toast_file_unreadable), Toast.LENGTH_LONG).show()
            return false
        }

        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (encoded.length.toLong() > maxEncodedBytes) {
            Toast.makeText(context, context.getString(R.string.toast_file_too_large), Toast.LENGTH_LONG).show()
            return false
        }

        val data = "data:$mime;base64,$encoded"
        attachmentOwnedPath?.let { path -> runCatching { File(path).delete() } }
        attachmentOwnedPath = null
        attachment = when {
            mime.startsWith("image/") -> ContentPart(type = "image_url", imageUrl = ImageUrl(data))
            mime.startsWith("audio/") -> ContentPart(type = "input_audio", inputAudio = InputAudio(data))
            else -> ContentPart(
                type = "video_url",
                videoUrl = VideoUrl(data),
                fps = 2.0,
                mediaResolution = "default"
            )
        }
        attachmentLabel = displayName ?: uri.lastPathSegment?.substringAfterLast('/') ?: context.getString(R.string.attachment_selected)
        attachmentUri = uri.toString()
        attachmentMimeType = mime
        return true
    }

    val supportsMultimodal = uiState.selectedModel?.capabilities?.contains("multimodal") == true
    LaunchedEffect(supportsMultimodal) {
        if (!supportsMultimodal) {
            clearAttachment(deleteOwnedFile = true)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            prepareAttachment(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val cameraUri = pendingCameraUri?.let(Uri::parse)
        val prepared = captured && cameraUri != null && prepareAttachment(cameraUri, "image/jpeg")
        if (prepared) {
            attachmentOwnedPath = pendingCameraPath
        } else {
            pendingCameraPath?.let { path -> runCatching { File(path).delete() } }
        }
        pendingCameraUri = null
        pendingCameraPath = null
    }

    val pickAttachmentFile: () -> Unit = {
        filePicker.launch(arrayOf("image/*", "audio/*", "video/*"))
    }
    val takePhoto: () -> Unit = {
        val target = runCatching {
            val directory = File(context.filesDir, "attachments").apply { mkdirs() }
            val photoFile = File.createTempFile("camera_", ".jpg", directory)
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            photoFile to photoUri
        }.getOrNull()

        if (target == null) {
            Toast.makeText(context, context.getString(R.string.toast_photo_file_failed), Toast.LENGTH_LONG).show()
        } else {
            pendingCameraPath = target.first.absolutePath
            pendingCameraUri = target.second.toString()
            runCatching { cameraLauncher.launch(target.second) }
                .onFailure {
                    runCatching { target.first.delete() }
                    pendingCameraPath = null
                    pendingCameraUri = null
                    Toast.makeText(context, context.getString(R.string.toast_camera_failed), Toast.LENGTH_LONG).show()
                }
        }
    }

    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingBackgroundCropUri = uri.toString()
        }
    }
    val pickBackgroundImage = {
        backgroundPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val clearBackgroundImage = {
        deleteStoredChatBackground(context, uiState.chatBackgroundUri)
        viewModel.setChatBackgroundUri(null)
        viewModel.setChatBackgroundOpacity(com.mroldl001.mimochat.data.preferences.PreferencesManager.DEFAULT_CHAT_BACKGROUND_OPACITY)
    }

    // 手机和平板布局共用同一个会话跳转入口；必须在平板分支提前返回前处理。
    LaunchedEffect(pendingInitialChatId, uiState.chats) {
        if (pendingInitialChatId != null) {
            uiState.chats.find { it.id == pendingInitialChatId }?.let { chat ->
                pendingInitialChatId = null
                if (uiState.currentChat?.id != chat.id) {
                    viewModel.selectChat(chat)
                }
            }
        }
    }

    var isThinkingMode by remember { mutableStateOf(false) }
    var isWebSearchEnabled by remember { mutableStateOf(false) }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showApiBaseUrlDialog by remember { mutableStateOf(false) }
    var showCustomPromptDialog by remember { mutableStateOf(false) }
    var showParameterSettingsDialog by remember { mutableStateOf(false) }

    if (isExpandedScreen) {
        AdaptiveChatLayout(
            uiState = uiState,
            messages = messages,
            streamingContent = streamingContent,
            streamingReasoning = streamingReasoning,
            isStreaming = isStreaming,
            isThinkingMode = isThinkingMode,
            isWebSearchEnabled = isWebSearchEnabled,
            onThinkingModeChanged = { newValue ->
                isThinkingMode = newValue
            },
            onWebSearchChanged = { isWebSearchEnabled = it },
            onSendMessage = { content ->
                if (uiState.apiKey.isBlank()) {
                    return@AdaptiveChatLayout
                }
                viewModel.sendMessage(content, isThinkingMode, attachment, isWebSearchEnabled, attachmentUri, attachmentMimeType)
                clearAttachment()
            },
            onStopGenerating = { viewModel.stopGenerating() },
            onCreateNewChat = { viewModel.createNewChat() },
            onDeleteChat = { viewModel.deleteChat(it) },
            onSelectChat = { viewModel.selectChat(it) },
            onSelectModel = { viewModel.selectModel(it) },
            onSetActiveSkill = { skill ->
                if (skill != null) {
                    isThinkingMode = false
                }
                viewModel.setActiveSkill(skill)
            },
            onThemeColorChanged = { color ->
                viewModel.setThemeColor(color)
            },
            onThemeModeChanged = { mode ->
                viewModel.setThemeMode(mode)
            },
            onThemeChanged = { color, mode ->
                onThemeChanged(color, mode)
            },
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToSettings = onNavigateToSettings,
            onApiKeySaved = { key ->
                viewModel.setApiKey(key)
            },
            onApiBaseUrlSaved = { url ->
                viewModel.setApiBaseUrl(url)
            },
            onCustomPromptSaved = { prompt ->
                viewModel.setCustomSystemPrompt(prompt)
            },
            chatBackgroundUri = uiState.chatBackgroundUri,
            chatBackgroundOpacity = uiState.chatBackgroundOpacity,
            onSelectBackgroundImage = pickBackgroundImage,
            onBackgroundOpacityChanged = viewModel::setChatBackgroundOpacity,
            onRestoreBackgroundDefault = clearBackgroundImage,
            onTemperatureSaved = { temp ->
                viewModel.setTemperature(temp)
            },
            onTopPSaved = { topP ->
                viewModel.setTopP(topP)
            },
            onFrequencyPenaltySaved = { freqPenalty ->
                viewModel.setFrequencyPenalty(freqPenalty)
            },
            onPresencePenaltySaved = { presPenalty ->
                viewModel.setPresencePenalty(presPenalty)
            },
            onResetParameters = {
                viewModel.resetParameters()
            },
            onCheckForUpdate = viewModel::checkForUpdate,
            onAcceptPrereleaseUpdatesChanged = viewModel::setAcceptPrereleaseUpdates,
            onDownloadUpdate = viewModel::downloadUpdate,
            onClearUpdateState = viewModel::clearUpdateState,
            onClearError = { viewModel.clearError() }
            , onTakePhoto = takePhoto
            , onSelectFile = pickAttachmentFile
            , onAttachmentCleared = { clearAttachment(deleteOwnedFile = true) }
            , attachmentLabel = attachmentLabel
            , attachmentUri = attachmentUri
            , attachmentMimeType = attachmentMimeType
            , isAttachmentEnabled = supportsMultimodal
            , initialChatId = initialChatId
            , chatScrollPositions = chatScrollPositions
            , loadChatScrollPosition = loadChatScrollPosition
            , onChatScrollPositionChanged = onChatScrollPositionChanged
            , onCurrentChatChanged = onCurrentChatChanged
            , suppressInitialScroll = suppressInitialScroll
            , onInitialChatNavigationHandled = onInitialChatNavigationHandled
            , onRefreshUsage = { viewModel.refreshUsage() }
            , onLoginUsage = { showMiMoLogin = true }
        )
        if (showBackgroundSettingsDialog) {
            BackgroundImageSettingsDialog(
                hasBackgroundImage = !uiState.chatBackgroundUri.isNullOrBlank(),
                opacity = uiState.chatBackgroundOpacity,
                onSelectImage = pickBackgroundImage,
                onOpacityChanged = viewModel::setChatBackgroundOpacity,
                onRestoreDefault = clearBackgroundImage,
                onDismiss = { showBackgroundSettingsDialog = false }
            )
        }
        pendingBackgroundCropUri?.let { uriString ->
            BackgroundCropDialog(
                sourceUri = Uri.parse(uriString),
                onCropped = { croppedUri ->
                    val previousUri = uiState.chatBackgroundUri
                    viewModel.setChatBackgroundUri(croppedUri)
                    if (previousUri != croppedUri) deleteStoredChatBackground(context, previousUri)
                    pruneStoredChatBackgrounds(context, croppedUri)
                    pendingBackgroundCropUri = null
                },
                onDismiss = { pendingBackgroundCropUri = null }
            )
        }
        return
    }

    var settingsAnchorBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val settingsPageTransition = rememberSettingsTransition()
    var showApiKeyWarningDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var chatToDelete: com.mroldl001.mimochat.domain.model.Chat? by remember { mutableStateOf(null) }


    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val historyListState = rememberLazyListState()
    var pendingSelectedChatId by remember { mutableStateOf<Long?>(null) }
    var pendingSelectJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(uiState.currentChat?.id, pendingSelectedChatId) {
        if (pendingSelectedChatId == uiState.currentChat?.id) {
            pendingSelectedChatId = null
        }
    }
    val focusManager = LocalFocusManager.current
    val nearBottomThresholdPx = with(LocalDensity.current) { 120.dp.roundToPx() }
    val scrollButtonTravelPx = with(LocalDensity.current) { 72.dp.roundToPx() }
    var pendingRestoreChatId by remember { mutableStateOf<Long?>(null) }
    var pendingSendMessageCount by remember { mutableStateOf<Int?>(null) }
    var followStreaming by remember { mutableStateOf(false) }
    var automaticStreamScroll by remember { mutableStateOf(false) }
    val isNearBottom by remember(listState, nearBottomThresholdPx) {
        derivedStateOf { listState.isNearBottom(nearBottomThresholdPx) }
    }

    // 离开会话或用户停止滚动后记录精确阅读位置。
    PersistChatScrollPosition(
        chatId = uiState.currentChat?.id,
        listState = listState,
        onPositionChanged = onChatScrollPositionChanged
    )

    LaunchedEffect(uiState.currentChat?.id) {
        val chatId = uiState.currentChat?.id
        pendingRestoreChatId = chatId
        followStreaming = false
        if (chatId != null) onCurrentChatChanged(chatId)
    }

    // 发送后只等待新用户消息真正进入列表，然后立即滚到底部。
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
            followStreaming = true
        }
    }

    LaunchedEffect(isStreaming, uiState.currentChat?.id) {
        if (!isStreaming) {
            followStreaming = false
        } else if (pendingInitialTopChatId == null && pendingRestoreChatId == null) {
            withFrameNanos { }
            followStreaming = listState.isNearBottom(nearBottomThresholdPx)
        }
    }

    LaunchedEffect(listState, isStreaming, nearBottomThresholdPx) {
        snapshotFlow {
            listState.isScrollInProgress to listState.isNearBottom(nearBottomThresholdPx)
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

    // 搜索结果进入会话时固定从顶部显示，覆盖 LazyListState 的历史位置恢复。
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
            followStreaming = false
            pendingInitialTopChatId = null
            onInitialChatNavigationHandled()
        }
    }

    LaunchedEffect(
        uiState.currentChat?.id,
        messages.size,
        messages.lastOrNull()?.chatId,
        pendingRestoreChatId,
        pendingInitialTopChatId,
        isStreaming
    ) {
        val targetChatId = pendingRestoreChatId ?: return@LaunchedEffect
        if (uiState.currentChat?.id != targetChatId || pendingInitialTopChatId == targetChatId) {
            return@LaunchedEffect
        }

        val savedPosition = chatScrollPositions[targetChatId]
            ?: loadChatScrollPosition(targetChatId)?.also {
                chatScrollPositions[targetChatId] = it
            }
        if (messages.isEmpty()) return@LaunchedEffect
        if (messages.lastOrNull()?.chatId != targetChatId) return@LaunchedEffect

        val totalItems = messages.size + if (isStreaming) 1 else 0
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it >= totalItems }
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
        followStreaming = isStreaming && listState.isNearBottom(nearBottomThresholdPx)
    }

    LaunchedEffect(drawerState.currentValue) {
        onNavigateFromDrawer(drawerState.currentValue == DrawerValue.Open)
        // 每次打开侧边栏顺带刷新一次剩余用量
        if (drawerState.currentValue == DrawerValue.Open) viewModel.refreshUsage()
    }

    if (showMiMoLogin) {
        MiMoLoginScreen(
            onNavigateBack = { showMiMoLogin = false },
            onCookieObtained = { cookie ->
                viewModel.saveUsageCookie(cookie)
                showMiMoLogin = false
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f),
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ChatHistoryHeader(
                        onSettingsBoundsChanged = { settingsAnchorBounds = it },
                        onSearchClick = onNavigateToSearch,
                        onSettingsClick = onNavigateToSettings,
                        onGitHubClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MRoldL001/MChat"))
                            context.startActivity(intent)
                        }
                    )

                    val filteredChats = uiState.chats.filter {
                        it.title.contains(searchQuery, ignoreCase = true)
                    }
                    
                    if (filteredChats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clipToBounds()
                        ) {
                        val highlightedChatId = pendingSelectedChatId ?: uiState.currentChat?.id
                        val isAnimating = remember { mutableStateOf(false) }
                        val settledChatId = remember { mutableStateOf(highlightedChatId) }
                        val historyEntries = remember(filteredChats) { buildChatHistoryEntries(filteredChats) }
                        val selectedHistoryIndex = historyEntries.indexOfFirst {
                            it is ChatHistoryEntry.Item && it.chat.id == highlightedChatId
                        }
                        ChatSelectionHighlight(
                            listState = historyListState,
                            selectedIndex = selectedHistoryIndex,
                            selectedChatId = highlightedChatId,
                            settledChatId = settledChatId,
                            isAnimating = isAnimating,
                            itemHeight = 72.dp
                        )
                        LazyColumn(
                            state = historyListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                                items(historyEntries, key = { it.key }) { entry ->
                                    when (entry) {
                                        is ChatHistoryEntry.Header -> ChatListDateHeader(entry.title)
                                        is ChatHistoryEntry.Item -> ChatListItem(
                                            chat = entry.chat,
                                            isSelected = entry.chat.id == highlightedChatId,
                                            settledSelectedChatId = settledChatId.value,
                                            isAnimating = isAnimating.value,
                                            onClick = {
                                                pendingSelectJob?.cancel()
                                                if (entry.chat.id == uiState.currentChat?.id) {
                                                    pendingSelectedChatId = null
                                                    scope.launch { drawerState.close() }
                                                } else {
                                                    pendingSelectedChatId = entry.chat.id
                                                    pendingSelectJob = scope.launch {
                                                        delay(360L)
                                                        drawerState.close()
                                                        viewModel.selectChat(entry.chat)
                                                    }
                                                }
                                            },
                                            onDelete = {
                                                chatToDelete = entry.chat
                                                showDeleteConfirmDialog = true
                                            }
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
                            onRefresh = { viewModel.refreshUsage() },
                            onLogin = { showMiMoLogin = true },
                            onCreateNewChat = {
                                viewModel.createNewChat()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Button(
                            onClick = {
                                viewModel.createNewChat()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.new_chat))
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        ModelSelector(
                            currentModel = uiState.selectedModel,
                            models = uiState.availableModels,
                            onModelSelected = { viewModel.selectModel(it) }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                        }
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
                            isThinkingMode = newValue
                        },
                        onWebSearchToggle = { isWebSearchEnabled = it },
                        onSkillToggle = { skill ->
                            viewModel.setActiveSkill(skill)
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
                                viewModel.sendMessage(it, isThinkingMode, attachment, isWebSearchEnabled, attachmentUri, attachmentMimeType)
                                clearAttachment()
                            }
                        },
                        onStopGenerating = { viewModel.stopGenerating() },
                        isGenerating = isStreaming
                        , onTakePhoto = takePhoto
                        , onSelectFile = pickAttachmentFile
                        , onAttachmentCleared = { clearAttachment(deleteOwnedFile = true) }
                        , attachmentLabel = attachmentLabel
                        , attachmentUri = attachmentUri
                        , attachmentMimeType = attachmentMimeType
                        , isAttachmentEnabled = supportsMultimodal
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
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { focusManager.clearFocus() }
            ) {
                uiState.chatBackgroundUri?.let { backgroundUri ->
                    coil.compose.AsyncImage(
                        model = backgroundUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(uiState.chatBackgroundOpacity)
                    )
                }
                if (messages.isEmpty() && !isStreaming) {
                    EmptyState(
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
                    visible = (messages.isNotEmpty() || isStreaming) &&
                        pendingRestoreChatId == null &&
                        !isNearBottom,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    enter = fadeIn(animationSpec = tween(180)) + slideInHorizontally(
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        initialOffsetX = { scrollButtonTravelPx }
                    ),
                    exit = fadeOut(animationSpec = tween(160)) + slideOutHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        targetOffsetX = { scrollButtonTravelPx }
                    )
                ) {
                    FilledIconButton(
                        onClick = {
                            scope.launch {
                                automaticStreamScroll = true
                                try {
                                    listState.animateScrollToBottomContent()
                                } finally {
                                    automaticStreamScroll = false
                                }
                                followStreaming = isStreaming
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
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = uiState.apiKey,
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showApiKeyDialog = false } },
            onConfirm = {
                settingsPageTransition.close {
                    viewModel.setApiKey(it)
                    showApiKeyDialog = false
                }
            }
        )
    }

    if (showApiBaseUrlDialog) {
        ApiBaseUrlDialog(
            currentUrl = uiState.apiBaseUrl,
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showApiBaseUrlDialog = false } },
            onConfirm = {
                settingsPageTransition.close {
                    viewModel.setApiBaseUrl(it)
                    showApiBaseUrlDialog = false
                }
            }
        )
    }

    if (showCustomPromptDialog) {
        CustomSystemPromptDialog(
            currentPrompt = uiState.customSystemPrompt,
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showCustomPromptDialog = false } },
            onConfirm = {
                settingsPageTransition.close {
                    viewModel.setCustomSystemPrompt(it)
                    showCustomPromptDialog = false
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

    (uiState.updateState as? com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState.Available)?.let { update ->
        UpdateReleaseDialog(
            release = update.release,
            onDismiss = viewModel::clearUpdateState,
            onDownload = { viewModel.downloadUpdate(update.release) }
        )
    }

    if (showBackgroundSettingsDialog) {
        BackgroundImageSettingsDialog(
            hasBackgroundImage = !uiState.chatBackgroundUri.isNullOrBlank(),
            opacity = uiState.chatBackgroundOpacity,
            onSelectImage = pickBackgroundImage,
            onOpacityChanged = viewModel::setChatBackgroundOpacity,
            onRestoreDefault = clearBackgroundImage,
            anchorBounds = settingsAnchorBounds,
            transition = settingsPageTransition,
            onDismiss = { settingsPageTransition.close { showBackgroundSettingsDialog = false } }
        )
    }

    pendingBackgroundCropUri?.let { uriString ->
        BackgroundCropDialog(
            sourceUri = Uri.parse(uriString),
            onCropped = { croppedUri ->
                val previousUri = uiState.chatBackgroundUri
                viewModel.setChatBackgroundUri(croppedUri)
                if (previousUri != croppedUri) deleteStoredChatBackground(context, previousUri)
                pruneStoredChatBackgrounds(context, croppedUri)
                pendingBackgroundCropUri = null
            },
            onDismiss = { pendingBackgroundCropUri = null }
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
                    viewModel.setTemperature(temp)
                    viewModel.setTopP(topP)
                    viewModel.setFrequencyPenalty(freqPenalty)
                    viewModel.setPresencePenalty(presPenalty)
                    showParameterSettingsDialog = false
                }
            },
            onReset = {
                settingsPageTransition.close {
                    viewModel.resetParameters()
                    showParameterSettingsDialog = false
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
                        chatToDelete?.let { viewModel.deleteChat(it) }
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
            },
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatListItem(
    chat: com.mroldl001.mimochat.domain.model.Chat,
    isSelected: Boolean,
    settledSelectedChatId: Long?,
    isAnimating: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentOffset by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "chatItemContentOffset"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (chat.id == settledSelectedChatId && !isAnimating) 0.14f else 0f
                ),
                shape = RoundedCornerShape(20.dp)
            )
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
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val egg = "MChat在这里，シタイだけ探した冒険TONGUE"
    val tips = listOf(
        stringResource(R.string.welcome_tip_1),
        stringResource(R.string.welcome_tip_2),
        stringResource(R.string.welcome_tip_3),
        stringResource(R.string.welcome_tip_4),
        egg
    )

    val randomText = remember {
        tips.random()
    }

    val prefix = stringResource(R.string.welcome_prefix)
    val firstLine: String
    val secondLine: String
    if (randomText == egg) {
        firstLine = prefix
        secondLine = "シタイだけ探した冒険TONGUE"
    } else {
        firstLine = prefix
        secondLine = randomText
    }
    
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
internal fun ApiKeyDialog(
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
        icon = { SettingsDialogIcon(Icons.Outlined.VpnKey) },
        title = { Text("API Key") },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.api_key_monthly_tip),
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
internal fun CustomSystemPromptDialog(
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
        icon = { SettingsDialogIcon(Icons.Outlined.Chat) },
        title = { Text(stringResource(R.string.custom_system_prompt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.custom_prompt_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text(stringResource(R.string.custom_prompt_label)) },
                    placeholder = { Text(stringResource(R.string.custom_prompt_placeholder)) },
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
internal fun ParameterSettingsDialog(
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
                    text = stringResource(R.string.restore_default),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.restore_default_confirm),
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
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.common_cancel))
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        icon = { SettingsDialogIcon(Icons.Outlined.Tune) },
        title = { Text(stringResource(R.string.parameter_settings)) },
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
                            text = stringResource(R.string.param_temp_hint),
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
                        text = stringResource(R.string.param_temp_desc),
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
                            text = stringResource(R.string.param_topp_hint),
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
                        text = stringResource(R.string.param_topp_desc),
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
                            text = stringResource(R.string.param_freq_hint),
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
                        text = stringResource(R.string.param_freq_desc),
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
                            text = stringResource(R.string.param_freq_hint),
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
                        text = stringResource(R.string.param_rep_desc),
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
                Text(stringResource(R.string.save))
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
                    Text(stringResource(R.string.common_cancel))
                }
                TextButton(
                    onClick = { showResetConfirmDialog = true }
                ) {
                    Text(stringResource(R.string.restore_default))
                }
            }
        }
    )
}
