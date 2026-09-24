package com.mroldl001.mimochat.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mroldl001.mimochat.ui.theme.CodeBlockColorMode
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "mimochat_prefs"
        /** API Key 单独存放：Keystore 主密钥 + AES256-GCM 加密。 */
        private const val SECURE_PREFS_NAME = "mimochat_secure_prefs"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CUSTOM_THEME_COLOR = "custom_theme_color"
        private const val KEY_CODE_BLOCK_COLOR_MODE = "code_block_color_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_CUSTOM_SYSTEM_PROMPT = "custom_system_prompt"
        private const val KEY_CHAT_BACKGROUND_URI = "chat_background_uri"
        private const val KEY_CHAT_BACKGROUND_OPACITY = "chat_background_opacity"
        private const val KEY_SELECTED_MODEL_ID = "selected_model_id"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val KEY_ACCEPT_PRERELEASE_UPDATES = "accept_prerelease_updates"
        private const val KEY_SHOW_USAGE = "show_usage"
        /** 控制台登录 Cookie（api-platform_serviceToken / userId），属敏感信息，优先加密存储。 */
        private const val KEY_USAGE_COOKIE = "usage_cookie"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_FREQUENCY_PENALTY = "frequency_penalty"
        private const val KEY_PRESENCE_PENALTY = "presence_penalty"
        private const val KEY_CHAT_SCROLL_PREFIX = "chat_scroll_"
        const val DEFAULT_API_BASE_URL = "https://api.xiaomimimo.com"
        const val DEFAULT_TEMPERATURE = 0.8f
        const val DEFAULT_TOP_P = 0.95f
        const val DEFAULT_FREQUENCY_PENALTY = 0.0f
        const val DEFAULT_PRESENCE_PENALTY = 0.0f
        const val DEFAULT_CHAT_BACKGROUND_OPACITY = 0.28f
        const val DEFAULT_CUSTOM_THEME_COLOR_HEX = "#000000"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 敏感项（API Key）专属存储：Keystore 主密钥 + AES256-GCM。
     * 首次创建要初始化 Keystore，有几十毫秒开销，因此保持懒加载；
     * 设备不支持或 Keystore 异常时返回 null，调用方降级到普通 prefs。
     */
    private val securePrefs: SharedPreferences? by lazy {
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    private fun readSecureApiKey(): String? {
        val secure = securePrefs ?: return null
        return runCatching { secure.getString(KEY_API_KEY, null) }.getOrNull()
    }

    private fun writeSecureApiKey(key: String): Boolean {
        val secure = securePrefs ?: return false
        return runCatching {
            secure.edit().putString(KEY_API_KEY, key).apply()
            true
        }.getOrDefault(false)
    }

    private fun readApiKey(): String {
        readSecureApiKey()?.let { return it }

        // 迁移：把升级前明文保存的 Key 搬进加密存储。
        val legacy = prefs.getString(KEY_API_KEY, null).orEmpty()
        if (legacy.isNotBlank() && writeSecureApiKey(legacy)) {
            prefs.edit().remove(KEY_API_KEY).apply()
        }
        return legacy
    }

    fun getThemeColor(): ThemeColor {
        val name = prefs.getString(KEY_THEME_COLOR, ThemeColor.WHITE.name)
        return try {
            ThemeColor.valueOf(name ?: ThemeColor.WHITE.name)
        } catch (e: IllegalArgumentException) {
            ThemeColor.WHITE
        }
    }

    fun saveThemeColor(color: ThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
    }

    fun getCustomThemeColorHex(): String {
        return prefs.getString(KEY_CUSTOM_THEME_COLOR, DEFAULT_CUSTOM_THEME_COLOR_HEX)
            ?: DEFAULT_CUSTOM_THEME_COLOR_HEX
    }

    fun saveCustomThemeColorHex(hex: String) {
        prefs.edit().putString(KEY_CUSTOM_THEME_COLOR, hex).apply()
    }

    fun getCodeBlockColorMode(): CodeBlockColorMode {
        val name = prefs.getString(KEY_CODE_BLOCK_COLOR_MODE, CodeBlockColorMode.DARK.name)
        return try {
            CodeBlockColorMode.valueOf(name ?: CodeBlockColorMode.DARK.name)
        } catch (e: IllegalArgumentException) {
            CodeBlockColorMode.DARK
        }
    }

    fun saveCodeBlockColorMode(mode: CodeBlockColorMode) {
        prefs.edit().putString(KEY_CODE_BLOCK_COLOR_MODE, mode.name).apply()
    }

    /**
     * 应用显示语言，默认 "system"（跟随手机系统语言）。
     * 取值见 ui.settings.AppLocale：system / zh-CN / zh-TW / en / ja。
     */
    fun getAppLanguage(): String {
        return prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
    }

    fun saveAppLanguage(languageCode: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, languageCode).apply()
    }

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.FOLLOW_SYSTEM.name)
        return try {
            ThemeMode.valueOf(name ?: ThemeMode.FOLLOW_SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.FOLLOW_SYSTEM
        }
    }

    fun saveThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getApiKey(): String {
        return readApiKey()
    }

    fun saveApiKey(key: String) {
        if (writeSecureApiKey(key)) {
            // 清掉可能残留的明文副本
            if (prefs.contains(KEY_API_KEY)) prefs.edit().remove(KEY_API_KEY).apply()
        } else {
            prefs.edit().putString(KEY_API_KEY, key).apply()
        }
    }

    fun getApiBaseUrl(): String {
        return prefs.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
    }

    fun saveApiBaseUrl(url: String) {
        prefs.edit().putString(KEY_API_BASE_URL, url).apply()
    }

    fun getCustomSystemPrompt(): String {
        return prefs.getString(KEY_CUSTOM_SYSTEM_PROMPT, "") ?: ""
    }

    fun saveCustomSystemPrompt(prompt: String) {
        prefs.edit().putString(KEY_CUSTOM_SYSTEM_PROMPT, prompt).apply()
    }

    fun getChatBackgroundUri(): String? {
        return prefs.getString(KEY_CHAT_BACKGROUND_URI, null)
    }

    fun saveChatBackgroundUri(uri: String?) {
        prefs.edit().apply {
            if (uri == null) {
                remove(KEY_CHAT_BACKGROUND_URI)
            } else {
                putString(KEY_CHAT_BACKGROUND_URI, uri)
            }
        }.apply()
    }

    fun getChatBackgroundOpacity(): Float {
        return prefs.getFloat(KEY_CHAT_BACKGROUND_OPACITY, DEFAULT_CHAT_BACKGROUND_OPACITY)
    }

    fun saveChatBackgroundOpacity(value: Float) {
        prefs.edit().putFloat(KEY_CHAT_BACKGROUND_OPACITY, value.coerceIn(0f, 1f)).apply()
    }

    fun getSelectedModelId(): String {
        return prefs.getString(KEY_SELECTED_MODEL_ID, "") ?: ""
    }

    fun saveSelectedModelId(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL_ID, modelId).apply()
    }

    fun hasRequestedNotificationPermission(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }

    fun setNotificationPermissionRequested(requested: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, requested).apply()
    }

    fun getAcceptPrereleaseUpdates(): Boolean {
        return prefs.getBoolean(KEY_ACCEPT_PRERELEASE_UPDATES, false)
    }

    fun saveAcceptPrereleaseUpdates(accept: Boolean) {
        prefs.edit().putBoolean(KEY_ACCEPT_PRERELEASE_UPDATES, accept).apply()
    }

    fun getShowUsage(): Boolean {
        return prefs.getBoolean(KEY_SHOW_USAGE, true)
    }

    fun saveShowUsage(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_USAGE, enabled).apply()
    }

    /**
     * 控制台登录 Cookie。小米的用量接口只认这个（API Key 打不通控制台接口），
     * 属凭证级数据，因此和 API Key 一样优先写入加密存储。
     */
    fun getUsageCookie(): String {
        val secure = securePrefs
        if (secure != null) {
            val value = runCatching { secure.getString(KEY_USAGE_COOKIE, null) }.getOrNull()
            if (!value.isNullOrBlank()) return value
        }
        return prefs.getString(KEY_USAGE_COOKIE, null).orEmpty()
    }

    fun saveUsageCookie(cookie: String) {
        val secure = securePrefs
        if (secure != null && cookie.isNotBlank()) {
            val written = runCatching {
                secure.edit().putString(KEY_USAGE_COOKIE, cookie).apply()
                true
            }.getOrDefault(false)
            if (written) {
                // 清掉可能残留的明文副本
                if (prefs.contains(KEY_USAGE_COOKIE)) prefs.edit().remove(KEY_USAGE_COOKIE).apply()
                return
            }
        }
        if (cookie.isBlank()) {
            secure?.let { runCatching { it.edit().remove(KEY_USAGE_COOKIE).apply() } }
            prefs.edit().remove(KEY_USAGE_COOKIE).apply()
        } else {
            prefs.edit().putString(KEY_USAGE_COOKIE, cookie).apply()
        }
    }

    fun clearUsageCookie() {
        saveUsageCookie("")
    }

    fun getTemperature(): Float {
        return prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
    }

    fun saveTemperature(value: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()
    }

    fun getTopP(): Float {
        return prefs.getFloat(KEY_TOP_P, DEFAULT_TOP_P)
    }

    fun saveTopP(value: Float) {
        prefs.edit().putFloat(KEY_TOP_P, value).apply()
    }

    fun getFrequencyPenalty(): Float {
        return prefs.getFloat(KEY_FREQUENCY_PENALTY, DEFAULT_FREQUENCY_PENALTY)
    }

    fun saveFrequencyPenalty(value: Float) {
        prefs.edit().putFloat(KEY_FREQUENCY_PENALTY, value).apply()
    }

    fun getPresencePenalty(): Float {
        return prefs.getFloat(KEY_PRESENCE_PENALTY, DEFAULT_PRESENCE_PENALTY)
    }

    fun savePresencePenalty(value: Float) {
        prefs.edit().putFloat(KEY_PRESENCE_PENALTY, value).apply()
    }

    fun resetParameters() {
        prefs.edit()
            .putFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
            .putFloat(KEY_TOP_P, DEFAULT_TOP_P)
            .putFloat(KEY_FREQUENCY_PENALTY, DEFAULT_FREQUENCY_PENALTY)
            .putFloat(KEY_PRESENCE_PENALTY, DEFAULT_PRESENCE_PENALTY)
            .apply()
    }

    fun getChatScrollPosition(chatId: Long): Pair<Int, Int>? {
        val indexKey = "${KEY_CHAT_SCROLL_PREFIX}${chatId}_index"
        val offsetKey = "${KEY_CHAT_SCROLL_PREFIX}${chatId}_offset"
        val index = prefs.getInt(indexKey, -1)
        val offset = prefs.getInt(offsetKey, -1)
        return if (index >= 0 && offset >= 0) index to offset else null
    }

    fun saveChatScrollPosition(chatId: Long, index: Int, offset: Int) {
        val indexKey = "${KEY_CHAT_SCROLL_PREFIX}${chatId}_index"
        val offsetKey = "${KEY_CHAT_SCROLL_PREFIX}${chatId}_offset"
        prefs.edit()
            .putInt(indexKey, index.coerceAtLeast(0))
            .putInt(offsetKey, offset.coerceAtLeast(0))
            .apply()
    }
}
