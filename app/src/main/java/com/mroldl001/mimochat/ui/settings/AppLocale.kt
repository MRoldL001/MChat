package com.mroldl001.mimochat.ui.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mroldl001.mimochat.R
import java.util.Locale

/**
 * 应用显示语言管理。
 * - SYSTEM：跟随手机系统语言（不进行任何覆盖，直接使用宿主 Context 的配置）。
 * - 其它：在 Context 上叠加对应 Locale，使 Compose 的 stringResource 即时解析到目标语言。
 *
 * 切换语言由 MainActivity.attachBaseContext 在 Activity 自身 Context 上叠加目标 Locale，
 * 并通过 recreate() 重新走一遍；LocalContext.current 天然携带本地化资源，无需在 Compose 中
 * 向 LocalContext 注入配置上下文（那会令依赖真实 Activity Context 的对话框/Window 绑定崩溃）。
 */
object AppLocale {
    const val SYSTEM = "system"
    const val ZH_CN = "zh-CN"
    const val ZH_TW = "zh-TW"
    const val EN = "en"
    const val JA = "ja"

    val SUPPORTED = listOf(SYSTEM, ZH_CN, ZH_TW, EN, JA)

    /** 语言选项展示顺序（跟随系统永远排第一）。 */
    val ORDERED = listOf(SYSTEM, ZH_CN, ZH_TW, EN, JA)

    fun toLocale(languageCode: String): Locale = when (languageCode) {
        ZH_TW -> Locale.TRADITIONAL_CHINESE
        ZH_CN -> Locale.SIMPLIFIED_CHINESE
        EN -> Locale.ENGLISH
        JA -> Locale.JAPANESE
        else -> Locale.getDefault()
    }

    /**
     * 返回叠加了目标语言的 Context。
     * SYSTEM 解析为「与手机语言对齐」的实际语言；若手机系统语言不在支持列表内，
     * 则回退到英语（en），避免落到默认的中文字串。
     */
    fun wrap(context: Context, languageCode: String): Context {
        val effective = if (languageCode == SYSTEM) resolveSystemLanguage(context) else languageCode
        val locale = toLocale(effective)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    /**
     * 由手机系统语言解析为受支持的语言码；不支持的语言（如韩语、法语等）回退到英语。
     */
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

    /**
     * 给定语言码返回其本地化的展示名（跟随当前 Context 的 Locale 解析），
     * 因此「当前选中的语言」会以它自己的语言呈现（例如 English / 日本語）。
     */
    @Composable
    fun label(code: String): String = when (code) {
        SYSTEM -> stringResource(R.string.language_system)
        ZH_CN -> stringResource(R.string.language_zh_cn)
        ZH_TW -> stringResource(R.string.language_zh_tw)
        EN -> stringResource(R.string.language_en)
        JA -> stringResource(R.string.language_ja)
        else -> stringResource(R.string.language_system)
    }
}
