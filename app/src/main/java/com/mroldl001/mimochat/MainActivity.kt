package com.mroldl001.mimochat

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.ui.settings.AppLocale
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.AppNavigation
import com.mroldl001.mimochat.ui.chat.components.ExpandedChatMinWidth
import com.mroldl001.mimochat.ui.theme.MIMOChatTheme
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var notificationChatId by mutableStateOf<Long?>(null)
    
    private lateinit var notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    /**
     * 在 Activity 自身 Context 上叠加应用语言，使 LocalContext.current（以及所有
     * stringResource）天然解析到目标语言。这是运行时切换语言最稳妥的做法，
     * 避免在 Compose 中向 LocalContext 注入「配置上下文」导致对话框/Window 绑定等
     * 依赖真实 Activity Context 的组件崩溃。
     * 切换语言只需 save + recreate() 即可重新走一遍此方法。
     */
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("mimochat_prefs", Context.MODE_PRIVATE)
            .getString("app_language", "system") ?: "system"
        super.attachBaseContext(AppLocale.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationChatId = intent.getLongExtra("com.mroldl001.mimochat.extra.CHAT_ID", 0L).takeIf { it > 0 }
        
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
            MainContent(initialChatId = notificationChatId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationChatId = intent.getLongExtra("com.mroldl001.mimochat.extra.CHAT_ID", 0L).takeIf { it > 0 }
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
    initialChatId: Long? = null,
) {
    var themeColor by remember { mutableStateOf(viewModel.preferencesManager.getThemeColor()) }
    var themeMode by remember { mutableStateOf(viewModel.preferencesManager.getThemeMode()) }
    var customThemeColorHex by remember { mutableStateOf(viewModel.preferencesManager.getCustomThemeColorHex()) }
    var codeBlockColorMode by remember { mutableStateOf(viewModel.preferencesManager.getCodeBlockColorMode()) }
    var appLanguage by remember { mutableStateOf(viewModel.preferencesManager.getAppLanguage()) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var onBackToChat: (() -> Unit)? by remember { mutableStateOf(null) }

    // 在组合作用域内读取 Activity（用于切语言后 recreate）；lambda 内不可读取 LocalContext。
    val activity = LocalContext.current as? ComponentActivity

    // 切换语言时先播放一段「向左滑出 + 淡出」的过渡动画（镜像于会话历史切换转场），
    // 动画结束后再 recreate()，让用户在视觉上明确感知到一次页面切换。
    val scope = rememberCoroutineScope()
    val langSwitchProgress = remember { Animatable(0f) }

    BackHandler(enabled = isDrawerOpen) {
        isDrawerOpen = false
        onBackToChat?.invoke()
    }
    
    val onNavigateFromSearch: () -> Unit = { }
    val onNavigateFromDrawer: (Boolean) -> Unit = { isDrawerOpen = it }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = this.maxWidth >= ExpandedChatMinWidth
        // 应用语言已在 MainActivity.attachBaseContext 中叠加到 Activity 自身 Context，
        // 因此 LocalContext.current 天然已是对应语言，这里无需再包裹。
        MIMOChatTheme(
            themeColor = themeColor,
            themeMode = themeMode,
            customColorHex = customThemeColorHex,
            codeBlockColorMode = codeBlockColorMode
        ) {
            // 底层同色背景：语言切换滑出动画期间防止露出黑边/窗口背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 1f - langSwitchProgress.value
                            translationX = -size.width * 0.33f * langSwitchProgress.value
                        },
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isExpandedScreen = isExpandedScreen,
                    initialChatId = initialChatId,
                    preferencesManager = viewModel.preferencesManager,
                    appLanguage = appLanguage,
                    onLanguageSelected = { code ->
                        viewModel.preferencesManager.saveAppLanguage(code)
                        scope.launch {
                            langSwitchProgress.animateTo(
                                1f,
                                animationSpec = tween(durationMillis = 300)
                            )
                            activity?.recreate()
                        }
                    },
                    onThemeChanged = { newColor, newMode ->
                        themeColor = newColor
                        themeMode = newMode
                        if (newColor == ThemeColor.CUSTOM) {
                            customThemeColorHex = viewModel.preferencesManager.getCustomThemeColorHex()
                        }
                    },
                    onCodeBlockColorModeChanged = { mode -> codeBlockColorMode = mode },
                    onNavigateFromSearch = onNavigateFromSearch,
                    onNavigateFromDrawer = onNavigateFromDrawer,
                    onBackToChat = { onBackToChat = it }
                )
                }
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel()
