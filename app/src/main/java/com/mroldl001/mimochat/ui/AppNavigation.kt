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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mroldl001.mimochat.ui.chat.ChatScreen
import com.mroldl001.mimochat.ui.search.SearchScreen
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode

sealed class Screen {
    object Chat : Screen()
    object Search : Screen()
}

@Composable
fun AppNavigation(
    isExpandedScreen: Boolean = false,
    initialChatId: Long? = null,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit = { _, _ -> },
    onNavigateFromSearch: () -> Unit = {},
    onNavigateFromDrawer: (Boolean) -> Unit = {},
    onBackToChat: ((() -> Unit) -> Unit)? = null
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }
    var selectedChatId by remember { mutableStateOf(initialChatId) }

    LaunchedEffect(initialChatId) {
        if (initialChatId != null) selectedChatId = initialChatId
    }

    LaunchedEffect(Unit) {
        onBackToChat?.invoke {
            currentScreen = Screen.Chat
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                targetState is Screen.Search -> {
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
                        onNavigateToChat = { chatId ->
                            selectedChatId = chatId
                        },
                        onThemeChanged = onThemeChanged,
                        onNavigateFromDrawer = onNavigateFromDrawer,
                        initialChatId = selectedChatId
                    )
                }

                is Screen.Search -> {
                    SearchScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Chat
                        },
                        onNavigateToChat = { chatId ->
                            selectedChatId = chatId
                            currentScreen = Screen.Chat
                        },
                        onNavigateFromSearch = onNavigateFromSearch
                    )
                }
            }
        }
    }
}
