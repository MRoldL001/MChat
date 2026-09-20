package com.mroldl001.mimochat.data.preferences

import android.content.Context
import android.content.SharedPreferences
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
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_CUSTOM_SYSTEM_PROMPT = "custom_system_prompt"
        private const val KEY_CHAT_BACKGROUND_URI = "chat_background_uri"
        private const val KEY_CHAT_BACKGROUND_OPACITY = "chat_background_opacity"
        private const val KEY_SELECTED_MODEL_ID = "selected_model_id"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        private const val KEY_ACCEPT_PRERELEASE_UPDATES = "accept_prerelease_updates"
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
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
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
