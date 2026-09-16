package com.mroldl001.mimochat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.AppNavigation
import com.mroldl001.mimochat.ui.theme.MIMOChatTheme
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private lateinit var notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 修复输入法弹出时输入框不被顶起的 bug（Android 11+ 需要 edge-to-edge 配合 imePadding）
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 初始化权限请求 Launcher
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // 权限请求结果回调
            // 可以在这里添加统计或其他逻辑
        }
        
        // 检查并请求通知权限（仅初次启动）
        requestNotificationPermissionIfNeeded()
        
        val windowManager = windowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        
        val isPhone = widthDp < 600
        
        requestedOrientation = if (isPhone) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        
        setContent {
            MainContent(isExpandedScreen = !isPhone)
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        // Android 13+ (API 33+) 需要运行时请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 检查是否已经请求过通知权限
            if (!preferencesManager.hasRequestedNotificationPermission()) {
                // 检查当前权限状态
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // 标记为已请求（即使还没得到用户响应）
                    preferencesManager.setNotificationPermissionRequested(true)
                    // 请求通知权限
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // 已经有权限，也标记为已请求
                    preferencesManager.setNotificationPermissionRequested(true)
                }
            }
        }
    }
    
    private val preferencesManager: PreferencesManager by lazy {
        (application as MIMOChatApp).preferencesManager
    }
}

@Composable
private fun MainContent(
    viewModel: MainViewModel = hiltViewModel(),
    isExpandedScreen: Boolean
) {
    var themeColor by remember { mutableStateOf(viewModel.preferencesManager.getThemeColor()) }
    var themeMode by remember { mutableStateOf(viewModel.preferencesManager.getThemeMode()) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var onBackToChat: (() -> Unit)? by remember { mutableStateOf(null) }

    BackHandler(enabled = isDrawerOpen) {
        isDrawerOpen = false
        onBackToChat?.invoke()
    }
    
    val onNavigateFromSearch: () -> Unit = { }
    val onNavigateFromDrawer: (Boolean) -> Unit = { isDrawerOpen = it }
    
    MIMOChatTheme(
        themeColor = themeColor,
        themeMode = themeMode
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                isExpandedScreen = isExpandedScreen,
                onThemeChanged = { newColor, newMode ->
                    themeColor = newColor
                    themeMode = newMode
                },
                onNavigateFromSearch = onNavigateFromSearch,
                onNavigateFromDrawer = onNavigateFromDrawer,
                onBackToChat = { onBackToChat = it }
            )
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel()
