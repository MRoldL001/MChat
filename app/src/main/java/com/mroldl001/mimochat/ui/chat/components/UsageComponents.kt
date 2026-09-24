package com.mroldl001.mimochat.ui.chat.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.data.repository.UsageRepository

/**
 * 侧边栏剩余用量卡片（新对话按钮上方）。
 * 未登录时点击去登录，已登录时点击刷新。
 */
@Composable
fun UsageCard(
    loading: Boolean,
    loggedIn: Boolean,
    usageText: String?,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = when {
        loading -> stringResource(R.string.usage_loading)
        usageText != null -> usageText
        loggedIn -> stringResource(R.string.usage_error)
        else -> stringResource(R.string.usage_login_prompt)
    }
    Surface(
        onClick = { if (loggedIn) onRefresh() else onLogin() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                loggedIn -> Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                else -> Icon(
                    imageVector = Icons.Outlined.Login,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 侧边栏底部组合卡片：用量信息 + 新对话按钮。
 * 有用量显示时合并成一个圆角矩形，减少用量卡片与按钮之间的空白；
 * 没有用量显示时由调用方单独展示新对话按钮。
 */
@Composable
fun UsageWithNewChatCard(
    loading: Boolean,
    loggedIn: Boolean,
    usageText: String?,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
    onCreateNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val text = when {
        loading -> stringResource(R.string.usage_loading)
        usageText != null -> usageText
        loggedIn -> stringResource(R.string.usage_error)
        else -> stringResource(R.string.usage_login_prompt)
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (loggedIn) onRefresh() else onLogin() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    loggedIn -> Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Outlined.Login,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Button(
                onClick = onCreateNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.new_chat))
            }
        }
    }
}

/**
 * 内置控制台登录页。
 *
 * 小米的用量接口只认 `platform.xiaomimimo.com` 的登录 Cookie，没有别的办法拿到，
 * 所以这里让用户登录一次，再从 CookieManager 取出 Cookie 存起来。
 *
 * WebView 做了完整配置（DOM 存储、第三方 Cookie、桌面 UA、window.open 弹窗转发），
 * 否则小米账号 SSO 页会白屏或卡在授权跳转上。
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiMoLoginScreen(
    onNavigateBack: () -> Unit,
    onCookieObtained: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnCookieObtained by rememberUpdatedState(onCookieObtained)

    var isLoading by remember { mutableStateOf(true) }
    var pageFailed by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("") }

    /**
     * 从 CookieManager 取控制台登录态；成功则回调并标记完成（onCookieObtained 会关闭弹窗）。
     * 仅在当前 URL 处于控制台域内时尝试，避免在 SSO 登录页上误抓。
     */
    fun tryCaptureCookie(): Boolean {
        if (captured) return true
        if (!currentUrl.startsWith(UsageRepository.PLATFORM_ORIGIN)) return false
        val raw = runCatching {
            val cm = CookieManager.getInstance()
            cm.flush()
            cm.getCookie(UsageRepository.PLATFORM_ORIGIN).orEmpty()
        }.getOrNull().orEmpty()
        Log.d("MiMoLogin", "capture url=$currentUrl, hasToken=${UsageRepository.hasServiceToken(raw)}, len=${raw.length}")
        if (UsageRepository.hasServiceToken(raw)) {
            captured = true
            currentOnCookieObtained(raw)
            return true
        }
        return false
    }

    /**
     * 在控制台域内自动轮询抓 Cookie：首试 600ms，之后每 1s 重试一次，
     * 直到抓到 serviceToken（onCookieObtained 关闭弹窗）或离开控制台域。
     * 这样即便登录后 token 写盘较晚也不会漏抓，因此无需「完成」按钮手动兜底。
     */
    fun scheduleAutoCapture(view: WebView, delay: Long) {
        if (captured) return
        view.postDelayed({
            if (captured) return@postDelayed
            if (!currentUrl.startsWith(UsageRepository.PLATFORM_ORIGIN)) return@postDelayed
            if (tryCaptureCookie()) return@postDelayed
            scheduleAutoCapture(view, 1000L)
        }, delay)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.usage_login_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                // 用不确定进度条，避免不同 Material3 版本 progress 参数签名差异
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            val wv = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                // 控制台是 SPA，必须开 DOM 存储，否则登录后白屏
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(true)
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mediaPlaybackRequiresUserGesture = false
                                allowContentAccess = true
                                // 小米账号页对 WebView 默认 UA（带 "; wv"）兼容不好，伪装成普通 Chrome
                                userAgentString = DESKTOP_USER_AGENT
                            }
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(wv, true)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest
                                ): Boolean {
                                    val url = request.url.toString()
                                    // 微信 / 支付宝等第三方授权 scheme 交给系统处理
                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        runCatching {
                                            view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                        return true
                                    }
                                    return false
                                }

                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    currentUrl = url
                                    isLoading = true
                                    pageFailed = false
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    currentUrl = url
                                    isLoading = false
                                    if (url.startsWith(UsageRepository.PLATFORM_ORIGIN)) {
                                        // 回到控制台域：开始自动轮询抓取登录 Cookie
                                        scheduleAutoCapture(view, 600L)
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError
                                ) {
                                    if (request.isForMainFrame) pageFailed = true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    isLoading = newProgress < 100
                                }

                                // 小米 SSO 的第三方授权走 window.open，不处理会开一个空白窗口
                                override fun onCreateWindow(
                                    view: WebView,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val popup = WebView(view.context).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.userAgentString = DESKTOP_USER_AGENT
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                v: WebView,
                                                request: WebResourceRequest
                                            ): Boolean {
                                                view.loadUrl(request.url.toString())
                                                return true
                                            }
                                        }
                                    }
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                    transport.webView = popup
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }

                            loadUrl(UsageRepository.CONSOLE_URL)
                        }
                    },
                    update = { webViewRef = it }
                )

                if (pageFailed) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.usage_login_load_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { webViewRef?.reload() }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.usage_login_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/** 伪装成桌面 Chrome，避免小米账号页把 WebView UA 判成不支持的客户端。 */
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/126.0.0.0 Safari/537.36"
