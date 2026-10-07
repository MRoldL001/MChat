package com.mroldl001.mimochat.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.domain.model.MessageAttachment
import com.mroldl001.mimochat.ui.chat.ChatScreen
import com.mroldl001.mimochat.ui.chat.components.AttachmentViewerScreen
import com.mroldl001.mimochat.ui.chat.components.ChatScrollPosition
import com.mroldl001.mimochat.ui.search.SearchScreen
import com.mroldl001.mimochat.ui.settings.DisclaimerScreen
import com.mroldl001.mimochat.ui.settings.EasterEggHistoryScreen
import com.mroldl001.mimochat.ui.settings.ExperimentalFeaturesScreen
import com.mroldl001.mimochat.ui.settings.SettingsScreen
import com.mroldl001.mimochat.ui.settings.AppLocale
import com.mroldl001.mimochat.ui.theme.CodeBlockColorMode
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode

sealed class Screen {
    object Chat : Screen()
    object Search : Screen()
    object Settings : Screen()
    object ExperimentalFeatures : Screen()
    object EasterEggHistory : Screen()
    object Disclaimer : Screen()
    data class AttachmentViewer(val attachments: List<MessageAttachment>, val index: Int) : Screen()
}

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
    var selectedChatId by rememberSaveable { mutableStateOf(initialChatId) }
    var suppressInitialChatScroll by remember { mutableStateOf(false) }
    // 从二级页返回聊天页时 +1，触发 ChatScreen 恢复滚动位置（不跳到底部）
    var chatScrollRestoreSignal by remember { mutableStateOf(0) }
    val chatScrollPositions = remember { mutableMapOf<Long, ChatScrollPosition>() }
    // recreate 后停在原处
    var settingsScrollOffset by rememberSaveable { mutableStateOf(0) }
    val settingsScrollState = rememberScrollState(initial = settingsScrollOffset)
    var attachmentViewer by remember { mutableStateOf<Screen.AttachmentViewer?>(null) }
    val pendingAttachmentsState = remember { mutableStateOf<List<MessageAttachment>>(emptyList()) }
    val pendingOwnedPathsState = remember { mutableStateOf<List<String>>(emptyList()) }

    val navController = rememberNavController()

    LaunchedEffect(initialChatId) {
        if (initialChatId != null) selectedChatId = initialChatId
    }
    LaunchedEffect(settingsScrollState) {
        snapshotFlow { settingsScrollState.value }.collect { settingsScrollOffset = it }
    }

    var previousRoute by remember { mutableStateOf("chat") }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "chat"
    LaunchedEffect(currentRoute) {
        // 从任意二级页回到聊天页：触发滚动位置恢复，不跳到底部
        if (currentRoute == "chat" && previousRoute != "chat") {
            chatScrollRestoreSignal++
        }
        // 二级页返回不回顶部
        if (currentRoute == "settings" &&
            (previousRoute == "chat" || previousRoute == "search")
        ) {
            settingsScrollState.scrollTo(0)
        }
        previousRoute = currentRoute
    }

    LaunchedEffect(Unit) {
        onBackToChat?.invoke {
            navController.navigate("chat") {
                popUpTo("chat") { inclusive = true }
            }
        }
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

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(300)
        )
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(300)
        )
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(300)
        )
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(300)
        )
    }

    NavHost(
        navController = navController,
        startDestination = "chat",
        modifier = Modifier.fillMaxSize()
    ) {
        composable(
            route = "chat",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ChatScreen(
                    isExpandedScreen = isExpandedScreen,
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToSettings = { navController.navigate("settings") },
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
                    scrollRestoreSignal = chatScrollRestoreSignal,
                    onInitialChatNavigationHandled = {
                        suppressInitialChatScroll = false
                    },
                    onAttachmentOpen = { attachments, index ->
                        attachmentViewer = Screen.AttachmentViewer(attachments, index)
                        navController.navigate("attachmentViewer")
                    },
                    pendingAttachments = pendingAttachmentsState,
                    pendingOwnedPaths = pendingOwnedPathsState
                )
            }
        }

        composable(
            route = "search",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { chatId ->
                        selectedChatId = chatId
                        suppressInitialChatScroll = true
                        navController.navigate("chat") {
                            popUpTo("chat") { inclusive = true }
                        }
                    },
                    onNavigateFromSearch = onNavigateFromSearch
                )
            }
        }

        composable(
            route = "settings",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onThemeChanged = onThemeChanged,
                    onCodeBlockColorModeChanged = onCodeBlockColorModeChanged,
                    onNavigateToDisclaimer = { navController.navigate("disclaimer") },
                    onNavigateToExperimentalFeatures = { navController.navigate("experimental") },
                    onNavigateToEasterEggHistory = { navController.navigate("egg") },
                    isExpandedScreen = isExpandedScreen,
                    scrollState = settingsScrollState,
                    appLanguage = appLanguage,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }

        composable(
            route = "experimental",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ExperimentalFeaturesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    isExpandedScreen = isExpandedScreen
                )
            }
        }

        composable(
            route = "egg",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                EasterEggHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    isExpandedScreen = isExpandedScreen
                )
            }
        }

        composable(
            route = "disclaimer",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                DisclaimerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "attachmentViewer",
            enterTransition = enter,
            exitTransition = exit,
            popEnterTransition = popEnter,
            popExitTransition = popExit
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AttachmentViewerScreen(
                    attachments = attachmentViewer?.attachments ?: emptyList(),
                    initialIndex = attachmentViewer?.index ?: 0,
                    onNavigateBack = {
                        attachmentViewer = null
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
