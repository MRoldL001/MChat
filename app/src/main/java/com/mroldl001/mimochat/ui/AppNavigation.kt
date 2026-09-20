package com.mroldl001.mimochat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.chat.ChatScreen
import com.mroldl001.mimochat.ui.chat.components.ChatScrollPosition
import com.mroldl001.mimochat.ui.search.SearchScreen
import com.mroldl001.mimochat.ui.settings.DisclaimerScreen
import com.mroldl001.mimochat.ui.settings.SettingsScreen
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode

sealed class Screen {
    object Chat : Screen()
    object Search : Screen()
    object Settings : Screen()
    object Disclaimer : Screen()
}

@Composable
fun AppNavigation(
    isExpandedScreen: Boolean = false,
    initialChatId: Long? = null,
    preferencesManager: PreferencesManager,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit = { _, _ -> },
    onNavigateFromSearch: () -> Unit = {},
    onNavigateFromDrawer: (Boolean) -> Unit = {},
    onBackToChat: ((() -> Unit) -> Unit)? = null
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }
    var selectedChatId by remember { mutableStateOf(initialChatId) }
    var suppressInitialChatScroll by remember { mutableStateOf(false) }
    val chatScrollPositions = remember { mutableMapOf<Long, ChatScrollPosition>() }
    val settingsScrollState = rememberScrollState()

    val loadChatScrollPosition: (Long) -> ChatScrollPosition? = { chatId ->
        preferencesManager.getChatScrollPosition(chatId)?.let { (index, offset) ->
            ChatScrollPosition(index, offset)
        }
    }
    val saveChatScrollPosition: (Long, Int, Int) -> Unit = { chatId, index, offset ->
        chatScrollPositions[chatId] = ChatScrollPosition(index, offset)
        preferencesManager.saveChatScrollPosition(chatId, index, offset)
    }

    LaunchedEffect(initialChatId) {
        if (initialChatId != null) selectedChatId = initialChatId
    }
    LaunchedEffect(Unit) {
        onBackToChat?.invoke {
            currentScreen = Screen.Chat
        }
    }

    BackHandler(enabled = currentScreen != Screen.Chat) {
        suppressInitialChatScroll = false
        currentScreen = if (currentScreen == Screen.Disclaimer) Screen.Settings else Screen.Chat
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                targetState is Screen.Settings && initialState is Screen.Disclaimer -> {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetX = { -it / 3 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 300),
                                targetOffsetX = { it }
                            ) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
                targetState is Screen.Search || targetState is Screen.Settings || targetState is Screen.Disclaimer -> {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetX = { it }
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 300),
                                targetOffsetX = { -it / 3 }
                            ) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
                targetState is Screen.Chat -> {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetX = { -it / 3 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 300),
                                targetOffsetX = { it }
                            ) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
                else -> {
                    fadeIn() togetherWith fadeOut()
                }
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                is Screen.Chat -> {
                    ChatScreen(
                        isExpandedScreen = isExpandedScreen,
                        onNavigateToSearch = {
                            currentScreen = Screen.Search
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                        },
                        onNavigateToChat = { chatId ->
                            selectedChatId = chatId
                        },
                        onThemeChanged = onThemeChanged,
                        onNavigateFromDrawer = onNavigateFromDrawer,
                        initialChatId = selectedChatId,
                        chatScrollPositions = chatScrollPositions,
                        loadChatScrollPosition = loadChatScrollPosition,
                        onChatScrollPositionChanged = saveChatScrollPosition,
                        onCurrentChatChanged = { chatId ->
                            if (chatId != null) selectedChatId = chatId
                        },
                        suppressInitialScroll = suppressInitialChatScroll,
                        onInitialChatNavigationHandled = {
                            suppressInitialChatScroll = false
                        }
                    )
                }

                is Screen.Search -> {
                    SearchScreen(
                        onNavigateBack = {
                            suppressInitialChatScroll = false
                            currentScreen = Screen.Chat
                        },
                        onNavigateToChat = { chatId ->
                            selectedChatId = chatId
                            suppressInitialChatScroll = true
                            currentScreen = Screen.Chat
                        },
                        onNavigateFromSearch = onNavigateFromSearch
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        onNavigateBack = { currentScreen = Screen.Chat },
                        onThemeChanged = onThemeChanged,
                        onNavigateToDisclaimer = { currentScreen = Screen.Disclaimer },
                        scrollState = settingsScrollState
                    )
                }

                is Screen.Disclaimer -> {
                    DisclaimerScreen(
                        onNavigateBack = { currentScreen = Screen.Settings }
                    )
                }
            }
        }
    }
}
