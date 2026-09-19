package com.mroldl001.mimochat.ui.theme

import android.app.Activity
import android.os.Build

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

fun supportsDynamicColor(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

enum class ThemeColor {
    WHITE,
    HATSUNE_MIKU,
    AUTO_COLOR,
    MI_ORANGE,
    GREEN,
    PURPLE
}

enum class ThemeMode {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM
}

private fun calculateLightContainerColor(primary: Color): Color {
    return primary.copy(alpha = 0.12f)
}

private fun calculateDarkContainerColor(primary: Color): Color {
    return primary.copy(alpha = 0.24f)
}

private fun calculateSurfaceVariantColor(primary: Color, light: Boolean): Color {
    return primary.copy(alpha = if (light) 0.08f else 0.16f)
}

// Neutral text/surfaces must not inherit Material's default purple tint, including in Monet mode.
private fun ColorScheme.withNeutralSurfaces(dark: Boolean): ColorScheme = copy(
    background = if (dark) Color(0xFF1C1C1C) else Color(0xFFFFFBFE),
    onBackground = if (dark) Color(0xFFE6E6E6) else Color(0xFF1C1C1C),
    surface = if (dark) Color(0xFF1C1C1C) else Color.White,
    onSurface = if (dark) Color(0xFFE6E6E6) else Color(0xFF1C1C1C),
    onSurfaceVariant = if (dark) Color(0xFFCACACA) else Color(0xFF494949),
    outline = if (dark) Color(0xFF939393) else Color(0xFF797979),
    outlineVariant = if (dark) Color(0xFF494949) else Color(0xFFCACACA),
    inverseSurface = if (dark) Color(0xFFE6E6E6) else Color(0xFF313131),
    inverseOnSurface = if (dark) Color(0xFF313131) else Color(0xFFF4F4F4),
    surfaceTint = primary,
    surfaceDim = if (dark) Color(0xFF141414) else Color(0xFFDEDEDE),
    surfaceBright = if (dark) Color(0xFF3B3B3B) else Color(0xFFFAFAFA),
    surfaceContainerLowest = if (dark) Color(0xFF0F0F0F) else Color.White,
    surfaceContainerLow = if (dark) Color(0xFF1C1C1C) else Color(0xFFF7F7F7),
    surfaceContainer = if (dark) Color(0xFF202020) else Color(0xFFF3F3F3),
    surfaceContainerHigh = if (dark) Color(0xFF2B2B2B) else Color(0xFFEDEDED),
    surfaceContainerHighest = if (dark) Color(0xFF363636) else Color(0xFFE7E7E7)
)

private fun brightenColor(color: Color): Color {
    val r = color.red
    val g = color.green
    val b = color.blue
    
    val avg = (r + g + b) / 3f
    val targetBrightness = 0.7f
    
    if (avg >= targetBrightness) return color
    
    val adjustment = (targetBrightness - avg) / (1f - avg)
    return Color(
        red = r + (1f - r) * adjustment,
        green = g + (1f - g) * adjustment,
        blue = b + (1f - b) * adjustment,
        alpha = color.alpha
    )
}

private fun lightColorSchemeWithPrimary(primary: Color, onPrimary: Color): androidx.compose.material3.ColorScheme {
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = calculateLightContainerColor(primary),
        onPrimaryContainer = Color(0xFF000000),
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = calculateLightContainerColor(primary),
        onSecondaryContainer = Color.Black,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = calculateLightContainerColor(primary),
        onTertiaryContainer = Color.Black,
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFFFFBFE),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = calculateSurfaceVariantColor(primary, true),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = androidx.compose.ui.graphics.lerp(primary, Color.White, 0.6f)
    )
}

private fun darkColorSchemeWithPrimary(primary: Color, onPrimary: Color): androidx.compose.material3.ColorScheme {
    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = calculateDarkContainerColor(primary),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = calculateDarkContainerColor(primary),
        onSecondaryContainer = Color.White,
        tertiary = primary,
        onTertiary = onPrimary,
        tertiaryContainer = calculateDarkContainerColor(primary),
        onTertiaryContainer = Color.White,
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
        background = Color(0xFF1C1B1F),
        onBackground = Color(0xFFE6E1E5),
        surface = Color(0xFF1C1B1F),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = calculateSurfaceVariantColor(primary, false),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF1C1B1F),
        inversePrimary = androidx.compose.ui.graphics.lerp(primary, Color.Black, 0.6f)
    )
}

private val WhiteLightColors = lightColorSchemeWithPrimary(
    primary = WhiteLightPrimary,
    onPrimary = WhiteLightOnPrimary
)

private val WhiteDarkColors = darkColorSchemeWithPrimary(
    primary = WhiteDarkPrimary,
    onPrimary = WhiteDarkOnPrimary
)

private val MiOrangeLightColors = lightColorSchemeWithPrimary(
    primary = MiOrangeLightPrimary,
    onPrimary = MiOrangeLightOnPrimary
)

private val MiOrangeDarkColors = darkColorSchemeWithPrimary(
    primary = MiOrangeDarkPrimary,
    onPrimary = MiOrangeDarkOnPrimary
)

private val GreenLightColors = lightColorSchemeWithPrimary(
    primary = GreenLightPrimary,
    onPrimary = GreenLightOnPrimary
)

private val GreenDarkColors = darkColorSchemeWithPrimary(
    primary = GreenDarkPrimary,
    onPrimary = GreenDarkOnPrimary
)

private val PurpleLightColors = lightColorSchemeWithPrimary(
    primary = PurpleLightPrimary,
    onPrimary = PurpleLightOnPrimary
)

private val PurpleDarkColors = darkColorSchemeWithPrimary(
    primary = PurpleDarkPrimary,
    onPrimary = PurpleDarkOnPrimary
)

private val HatsuneMikuLightColors = lightColorSchemeWithPrimary(
    primary = HatsuneMikuLightPrimary,
    onPrimary = HatsuneMikuLightOnPrimary
)

private val HatsuneMikuDarkColors = darkColorSchemeWithPrimary(
    primary = HatsuneMikuDarkPrimary,
    onPrimary = HatsuneMikuDarkOnPrimary
)

@Suppress("DEPRECATION")
@Composable
fun MIMOChatTheme(
    themeColor: ThemeColor = ThemeColor.WHITE,
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeColor) {
        ThemeColor.AUTO_COLOR -> {
            val lightScheme = dynamicLightColorScheme(context)
            if (darkTheme) {
                val darkScheme = dynamicDarkColorScheme(context)
                val brightPrimary = brightenColor(lightScheme.primary)
                val brightSecondary = brightenColor(lightScheme.secondary)
                val brightTertiary = brightenColor(lightScheme.tertiary)
                darkScheme.copy(
                    primary = brightPrimary,
                    onPrimary = Color(0xFF1C1B1F),
                    primaryContainer = brightPrimary.copy(alpha = 0.24f),
                    onPrimaryContainer = Color(0xFFFFFFFF),
                    inversePrimary = lightScheme.primary,
                    secondary = brightSecondary,
                    onSecondary = Color(0xFF1C1B1F),
                    secondaryContainer = calculateDarkContainerColor(brightSecondary),
                    onSecondaryContainer = Color.White,
                    tertiary = brightTertiary,
                    onTertiary = Color(0xFF1C1B1F),
                    tertiaryContainer = calculateDarkContainerColor(brightTertiary),
                    onTertiaryContainer = Color.White,
                    background = Color(0xFF1C1B1F),
                    onBackground = Color(0xFFE6E1E5),
                    surface = Color(0xFF1C1B1F),
                    onSurface = Color(0xFFE6E1E5),
                    surfaceVariant = calculateSurfaceVariantColor(brightPrimary, false),
                    onSurfaceVariant = Color(0xFFCAC4D0)
                )
            } else {
                lightScheme.copy(
                    primaryContainer = calculateLightContainerColor(lightScheme.primary),
                    onPrimaryContainer = Color(0xFF000000),
                    surfaceVariant = calculateSurfaceVariantColor(lightScheme.primary, true)
                )
            }
        }
        else -> {
            when (themeColor) {
                ThemeColor.WHITE -> if (darkTheme) WhiteDarkColors else WhiteLightColors
                ThemeColor.HATSUNE_MIKU -> if (darkTheme) HatsuneMikuDarkColors else HatsuneMikuLightColors
                ThemeColor.MI_ORANGE -> if (darkTheme) MiOrangeDarkColors else MiOrangeLightColors
                ThemeColor.GREEN -> if (darkTheme) GreenDarkColors else GreenLightColors
                ThemeColor.PURPLE -> if (darkTheme) PurpleDarkColors else PurpleLightColors
                else -> WhiteLightColors
            }
        }
    }

    val resolvedColorScheme = colorScheme.withNeutralSurfaces(darkTheme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarColor = resolvedColorScheme.background.toArgb()
            window.statusBarColor = statusBarColor
            window.navigationBarColor = resolvedColorScheme.background.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = resolvedColorScheme,
        typography = Typography,
        content = content
    )
}
