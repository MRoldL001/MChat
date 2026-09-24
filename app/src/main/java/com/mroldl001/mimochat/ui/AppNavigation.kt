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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.chat.ChatScreen
import com.mroldl001.mimochat.ui.chat.components.ChatScrollPosition
import com.mroldl001.mimochat.ui.search.SearchScreen
import com.mroldl001.mimochat.ui.settings.DisclaimerScreen
import com.mroldl001.mimochat.ui.settings.EasterEggHistoryScreen
import com.mroldl001.mimochat.ui.settings.ExperimentalFeaturesScreen
import com.mroldl001.mimochat.ui.settings.SettingsScreen
import com.mroldl001.mimochat.ui.theme.CodeBlockColorMode
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.settings.AppLocale

sealed class Screen {
    object Chat : Screen()
    object Search : Screen()
    object Settings : Screen()
    object ExperimentalFeatures : Screen()
    object EasterEggHistory : Screen()
    object Disclaimer : Screen()
}

/** 让 Screen 在 recreate() 后通过 rememberSaveable 还原。 */
private val ScreenSaver = Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            is Screen.Chat -> "chat"
            is Screen.Search -> "search"
            is Screen.Settings -> "settings"
            is Screen.ExperimentalFeatures -> "exp"
            is Screen.EasterEggHistory -> "egg"
            is Screen.Disclaimer -> "disclaimer"
        }
    },
    restore = { name ->
        when (name) {
            "chat" -> Screen.Chat
            "search" -> Screen.Search
            "settings" -> Screen.Settings
            "exp" -> Screen.ExperimentalFeatures
            "egg" -> Screen.EasterEggHistory
            "disclaimer" -> Screen.Disclaimer
            else -> Screen.Chat
        }
    }
)

@Composable
fun AppNavigation(
    isExpandedScreen: Boolean = false,
    initialChatId: Long? = null,
    preferencesManager: PreferencesManager,
    appLanguage: String = AppLocale.SYSTEM,
    onLanguageSelected: (String) -> Unit = {},
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit = { _, _ -> },
    onCodeBlockColorModeChanged: (CodeBlockColorMode) -> Unit = {},
    onNavigateFromSearch: () -> Unit = {},
    onNavigateFromDrawer: (Boolean) -> Unit = {},
    onBackToChat: ((() -> Unit) -> Unit)? = null
) {
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Chat) }
    var selectedChatId by remember { mutableStateOf(initialChatId) }
    var suppressInitialChatScroll by remember { mutableStateOf(false) }
    val chatScrollPositions = remember { mutableMapOf<Long, ChatScrollPosition>() }
    // 设置页滚动位置：recreate()（切换语言）后仍需停留在原位置，故用 rememberSaveable 持久化偏移量
    var settingsScrollOffset by rememberSaveable { mutableStateOf(0) }
    val settingsScrollState = rememberScrollState(initial = settingsScrollOffset)
    var previousScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Chat) }
    LaunchedEffect(initialChatId) {
        if (initialChatId != null) selectedChatId = initialChatId
    }
    // 实时把当前滚动偏移写回 saveable，recreate 后据此还原
    LaunchedEffect(settingsScrollState) {
        snapshotFlow { settingsScrollState.value }.collect { settingsScrollOffset = it }
    }
    LaunchedEffect(currentScreen) {
        // 仅当从聊天页 / 搜索页「打开」设置页时回到顶部；
        // 从二级页（免责声明 / 实验性功能 / 历史彩蛋）返回，或 recreate（切换语言）后，保留原滚动位置。
        if (currentScreen is Screen.Settings &&
            (previousScreen is Screen.Chat || previousScreen is Screen.Search)
        ) {
            settingsScrollState.scrollTo(0)
        }
        previousScreen = currentScreen
    }

    val loadChatScrollPosition: (Long) -> ChatScrollPosition? = { chatId ->
        preferencesManager.getChatScrollPosition(chatId)?.let { (index, offset) ->
            ChatScrollPosition(index, offset)
        }
    }
    val saveChatScrollPosition: (Long, Int, Int) -> Unit = { chatId, index, offset ->
        chatScrollPositions[chatId] = ChatScrollPosition(index, offset)
        preferencesManager.saveChatScrollPosition(chatId, index, offset)
    }

    LaunchedEffect(Unit) {
        onBackToChat?.invoke {
            currentScreen = Screen.Chat
        }
    }

    BackHandler(enabled = currentScreen != Screen.Chat) {
        suppressInitialChatScroll = false
        currentScreen = when (currentScreen) {
            Screen.Disclaimer, Screen.ExperimentalFeatures, Screen.EasterEggHistory -> Screen.Settings
            else -> Screen.Chat
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                targetState is Screen.Settings &&
                    (initialState is Screen.Disclaimer ||
                        initialState is Screen.ExperimentalFeatures ||
                        initialState is Screen.EasterEggHistory) -> {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetX = { -it / 3 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 300),
                                targetOffsetX = { it }
                            ) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
                targetState is Screen.Search ||
                    targetState is Screen.Settings ||
                    targetState is Screen.Disclaimer ||
                    targetState is Screen.ExperimentalFeatures ||
                    targetState is Screen.EasterEggHistory -> {
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
                        onCodeBlockColorModeChanged = onCodeBlockColorModeChanged,
                        onNavigateToDisclaimer = { currentScreen = Screen.Disclaimer },
                        onNavigateToExperimentalFeatures = { currentScreen = Screen.ExperimentalFeatures },
                        onNavigateToEasterEggHistory = { currentScreen = Screen.EasterEggHistory },
                        isExpandedScreen = isExpandedScreen,
                        scrollState = settingsScrollState,
                        appLanguage = appLanguage,
                        onLanguageSelected = onLanguageSelected
                    )
                }

                is Screen.ExperimentalFeatures -> {
                    ExperimentalFeaturesScreen(
                        onNavigateBack = { currentScreen = Screen.Settings },
                        isExpandedScreen = isExpandedScreen
                    )
                }

                is Screen.EasterEggHistory -> {
                    EasterEggHistoryScreen(
                        onNavigateBack = { currentScreen = Screen.Settings },
                        isExpandedScreen = isExpandedScreen
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
