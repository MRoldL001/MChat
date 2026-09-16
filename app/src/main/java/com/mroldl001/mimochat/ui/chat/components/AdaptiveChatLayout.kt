package com.mroldl001.mimochat.ui.chat.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.domain.model.Message
import com.mroldl001.mimochat.domain.model.AIModel
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatUiState
import com.mroldl001.mimochat.ui.chat.viewmodel.SkillType
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.theme.supportsDynamicColor
import kotlinx.coroutines.delay
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
    onThinkingModeChanged: (Boolean) -> Unit,
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
    onApiKeySaved: (String) -> Unit,
    onApiBaseUrlSaved: (String) -> Unit,
    onCustomPromptSaved: (String) -> Unit,
    onTemperatureSaved: (Float) -> Unit,
    onTopPSaved: (Float) -> Unit,
    onFrequencyPenaltySaved: (Float) -> Unit,
    onPresencePenaltySaved: (Float) -> Unit,
    onResetParameters: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAdvancedSettingsDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showApiBaseUrlDialog by remember { mutableStateOf(false) }
    var showCustomPromptDialog by remember { mutableStateOf(false) }
    var showParameterSettingsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showApiKeyWarningDialog by remember { mutableStateOf(false) }
    
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    
    var currentApiKey by remember { mutableStateOf("") }
    var currentApiBaseUrl by remember { mutableStateOf("") }
    var currentCustomPrompt by remember { mutableStateOf("") }
    
    var needScrollChatId by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(uiState.currentChat?.id) {
        if (uiState.currentChat?.id != null) {
            needScrollChatId = uiState.currentChat?.id
        }
    }

    LaunchedEffect(messages.size, needScrollChatId, isStreaming) {
        if (needScrollChatId != null && uiState.currentChat?.id == needScrollChatId) {
            val totalItems = messages.size + if (isStreaming) 1 else 0
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1, Int.MAX_VALUE)
                if (!isStreaming) {
                    needScrollChatId = null
                }
            }
        }
    }

    LaunchedEffect(isStreaming) {
        if (!isStreaming) {
            delay(50)
            val totalItems = messages.size
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1, Int.MAX_VALUE)
            }
        }
    }

    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ChatHistoryHeader(
                        onSearchClick = onNavigateToSearch,
                        onSettingsClick = { showSettingsDialog = true },
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
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.chats, key = { it.id }) { chat ->
                                TabletChatListItem(
                                    chat = chat,
                                    isSelected = chat.id == uiState.currentChat?.id,
                                    onClick = { onSelectChat(chat) },
                                    onDelete = { chatItem ->
                                        chatToDelete = chatItem
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onCreateNewChat,
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
                        .background(MaterialTheme.colorScheme.background)
                        .imePadding() // 修复输入法弹出时输入框不被顶起的 bug
                ) {
                    SkillToggleBar(
                        isThinkingMode = isThinkingMode,
                        activeSkill = uiState.activeSkill,
                        isGenerating = isStreaming,
                        onThinkingModeToggle = { newValue ->
                            onThinkingModeChanged(newValue)
                        },
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
                                onSendMessage(it)
                            }
                        },
                        onStopGenerating = onStopGenerating,
                        isGenerating = isStreaming
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

                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = onClearError) {
                                Text("关闭")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            initialThemeColor = uiState.themeColor,
            initialThemeMode = uiState.themeMode,
            onApply = { newColor, newMode ->
                onThemeColorChanged(newColor)
                onThemeModeChanged(newMode)
                onThemeChanged(newColor, newMode)
                showSettingsDialog = false
            },
            onApiKeyClick = {
                showSettingsDialog = false
                showApiKeyDialog = true
            },
            onAdvancedSettingsClick = {
                showSettingsDialog = false
                showAdvancedSettingsDialog = true
            },
            onCustomPromptClick = {
                showSettingsDialog = false
                showCustomPromptDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAdvancedSettingsDialog) {
        AdvancedSettingsDialog(
            onApiBaseUrlClick = {
                showAdvancedSettingsDialog = false
                showApiBaseUrlDialog = true
            },
            onParameterSettingsClick = {
                showAdvancedSettingsDialog = false
                showParameterSettingsDialog = true
            },
            onDismiss = { showAdvancedSettingsDialog = false }
        )
    }

    if (showParameterSettingsDialog) {
        ParameterSettingsDialog(
            initialTemperature = uiState.temperature,
            initialTopP = uiState.topP,
            initialFrequencyPenalty = uiState.frequencyPenalty,
            initialPresencePenalty = uiState.presencePenalty,
            onDismiss = { showParameterSettingsDialog = false },
            onConfirm = { temp, topP, freqPenalty, presPenalty ->
                onTemperatureSaved(temp)
                onTopPSaved(topP)
                onFrequencyPenaltySaved(freqPenalty)
                onPresencePenaltySaved(presPenalty)
                showParameterSettingsDialog = false
            },
            onReset = {
                onResetParameters()
                showParameterSettingsDialog = false
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
            onDismiss = { showApiKeyDialog = false },
            onConfirm = { key ->
                currentApiKey = key
                onApiKeySaved(key)
                showApiKeyDialog = false
            }
        )
    }
    
    if (showApiBaseUrlDialog) {
        ApiBaseUrlDialog(
            currentUrl = currentApiBaseUrl.ifEmpty { uiState.apiBaseUrl },
            onDismiss = { showApiBaseUrlDialog = false },
            onConfirm = { url ->
                currentApiBaseUrl = url
                onApiBaseUrlSaved(url)
                showApiBaseUrlDialog = false
            }
        )
    }
    
    if (showCustomPromptDialog) {
        CustomSystemPromptDialog(
            currentPrompt = currentCustomPrompt.ifEmpty { uiState.customSystemPrompt },
            onDismiss = { showCustomPromptDialog = false },
            onConfirm = { prompt ->
                currentCustomPrompt = prompt
                onCustomPromptSaved(prompt)
                showCustomPromptDialog = false
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
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
    onApiBaseUrlClick: () -> Unit,
    onParameterSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("高级设置") },
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
                            text = "调整 Temperature 和 Top P",
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
    initialThemeColor: ThemeColor,
    initialThemeMode: ThemeMode,
    onApply: (ThemeColor, ThemeMode) -> Unit,
    onApiKeyClick: () -> Unit,
    onAdvancedSettingsClick: () -> Unit,
    onCustomPromptClick: () -> Unit,
    onDismiss: () -> Unit
) {
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
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
                            color = Color.Transparent,
                            isDiagonal = true
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

                val customPromptInteractionSource = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clickable(
                            interactionSource = customPromptInteractionSource,
                            indication = null,
                            onClick = onCustomPromptClick
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
                            text = "更多设置项",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "更多高级选项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(tempThemeColor, tempThemeMode) }) {
                Text("应用")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 300),
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
                .clip(CircleShape)
                .background(
                    if (isDiagonal) {
                        Brush.linearGradient(
                            colors = listOf(Color.White, Color.Black),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset.Infinite
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(color, color))
                    }
                )
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDiagonal) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                start = androidx.compose.ui.geometry.Offset(36f, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, 36f)
                            )
                        )
                )
            }
            
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
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Key") },
        text = {
            Column {
                Text(
                    text = "月度套餐用户请在高级设置内将 API Base URL 改为订阅接口",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
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
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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
private fun ApiBaseUrlDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiBaseUrl by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Base URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "请输入API服务器地址或选择预设",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { apiBaseUrl = "https://api.xiaomimimo.com" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("标准接口", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = { apiBaseUrl = "https://token-plan-cn.xiaomimimo.com" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("订阅接口", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    label = { Text("API Base URL") },
                    placeholder = { Text("https://api.xiaomimimo.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(apiBaseUrl) },
                enabled = apiBaseUrl.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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
private fun CustomSystemPromptDialog(
    currentPrompt: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var customPrompt by remember { mutableStateOf(currentPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义系统提示词") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "请输入自定义的系统提示词",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
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
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
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
private fun ParameterSettingsDialog(
    initialTemperature: Float,
    initialTopP: Float,
    initialFrequencyPenalty: Float,
    initialPresencePenalty: Float,
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
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("参数设置") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
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
                Text("确认")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 300),
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
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
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
