package com.mroldl001.mimochat.ui.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mroldl001.mimochat.R
import java.util.Locale

// 防绑定崩溃
object AppLocale {
    const val SYSTEM = "system"
    const val ZH_CN = "zh-CN"
    const val ZH_TW = "zh-TW"
    const val EN = "en"
    const val JA = "ja"

    val SUPPORTED = listOf(SYSTEM, ZH_CN, ZH_TW, EN, JA)

    val ORDERED = listOf(SYSTEM, ZH_CN, ZH_TW, EN, JA)

    fun toLocale(languageCode: String): Locale = when (languageCode) {
        ZH_TW -> Locale.TRADITIONAL_CHINESE
        ZH_CN -> Locale.SIMPLIFIED_CHINESE
        EN -> Locale.ENGLISH
        JA -> Locale.JAPANESE
        else -> Locale.getDefault()
    }

    fun wrap(context: Context, languageCode: String): Context {
        val effective = if (languageCode == SYSTEM) resolveSystemLanguage(context) else languageCode
        val locale = toLocale(effective)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    private fun resolveSystemLanguage(context: Context): String {
        val locales = context.resources.configuration.locales
        val sys = if (locales.isEmpty) Locale.getDefault() else locales[0]
        return when (sys.language.lowercase()) {
            "zh" -> if (sys.script.equals("Hant", ignoreCase = true) ||
                sys.country.equals("TW", ignoreCase = true) ||
                sys.country.equals("HK", ignoreCase = true) ||
                sys.country.equals("MO", ignoreCase = true)
            ) ZH_TW else ZH_CN
            "en" -> EN
            "ja" -> JA
            else -> EN
        }
    }

    @Composable
    fun label(code: String): String = when (code) {
        SYSTEM -> stringResource(R.string.language_system)
        ZH_CN -> "简体中文"
        ZH_TW -> "繁體中文"
        EN -> "English"
        JA -> "日本語"
        else -> stringResource(R.string.language_system)
    }
}
