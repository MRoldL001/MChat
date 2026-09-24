package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.sin

/**
 * 同色系霓虹流光：始终保持主题色的色相，只在亮度 / 饱和度 / 极小的色相漂移上变化。
 * 能量场用解析正弦波而不是高斯亮带——波在任何位置、任何时刻都连续，
 * 不会出现亮带在文字两端"闪跳"或者叠加成一片平色的问题。
 */
private const val FLOW_DURATION_MILLIS = 4000
private const val WAVE_COUNT = 2
private const val CREST_SHARPNESS = 0.85f
private const val SHEEN_WEIGHT = 0.12f
private const val SAMPLE_COUNT = 64
private const val TWO_PI = 6.2831855f

private data class NeonPalette(
    val hue: Float,
    val saturation: Float,
    val hueDrift: Float,
    val lowLightness: Float,
    val highLightness: Float,
    val highSaturation: Float
)

@Composable
fun rememberNeonFlowBrush(
    baseColor: Color,
    waveCount: Int = WAVE_COUNT,
    durationMillis: Int = FLOW_DURATION_MILLIS
): Brush {
    val isLightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val palette = remember(baseColor, isLightBackground) {
        buildNeonPalette(baseColor, isLightBackground)
    }
    val transition = rememberInfiniteTransition(label = "neon_flow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neon_flow_progress"
    )
    return remember(palette, progress, waveCount) {
        val stops = mutableListOf<Pair<Float, Color>>()
        repeat(SAMPLE_COUNT + 1) { index ->
            val offset = index.toFloat() / SAMPLE_COUNT
            val energy = flowEnergy(offset, progress * waveCount, waveCount)
            stops.add(offset to neonColorAt(palette, offset, energy))
        }
        Brush.horizontalGradient(*stops.sortedBy { it.first }.toTypedArray())
    }
}

private fun buildNeonPalette(baseColor: Color, isLightBackground: Boolean): NeonPalette {
    val (hue, saturation, lightness) = rgbToHsl(baseColor.red, baseColor.green, baseColor.blue)
    val lowLightness = (lightness - 0.16f).coerceIn(
        minimumValue = 0.14f,
        maximumValue = if (isLightBackground) 0.62f else 0.50f
    )
    val highLightness = (lightness + 0.22f).coerceIn(
        minimumValue = lowLightness + 0.10f,
        maximumValue = if (isLightBackground) 0.74f else 0.93f
    )
    return NeonPalette(
        hue = hue,
        saturation = saturation,
        hueDrift = if (isLightBackground) 5f else 9f,
        lowLightness = lowLightness,
        highLightness = highLightness,
        highSaturation = (saturation * 1.35f + 0.05f).coerceIn(0f, 1f)
    )
}

private fun flowEnergy(offset: Float, phase: Float, waveCount: Int): Float {
    val crest = 0.5f + 0.5f * sin(TWO_PI * (offset * waveCount - phase))
    val shaped = crest * crest
    val sheen = 0.5f + 0.5f * sin(TWO_PI * (offset - phase / waveCount))
    val energy = shaped * CREST_SHARPNESS + sheen * SHEEN_WEIGHT
    return energy.coerceIn(0f, 1f)
}

private fun neonColorAt(palette: NeonPalette, offset: Float, energy: Float): Color {
    val blend = (0.3f * offset + 0.7f * energy).coerceIn(0f, 1f)
    val lightness = (palette.lowLightness + (palette.highLightness - palette.lowLightness) * blend)
        .coerceIn(0f, 1f)
    val saturation = (palette.saturation + (palette.highSaturation - palette.saturation) * energy)
        .coerceIn(0f, 1f)
    val hue = palette.hue + palette.hueDrift * (energy - 0.5f) * 2f
    return hslToColor(hue, saturation, lightness)
}

private fun rgbToHsl(red: Float, green: Float, blue: Float): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val lightness = (max + min) / 2f
    val delta = max - min
    if (delta < 1e-4f) return Triple(0f, 0f, lightness)
    val saturation = delta / (1f - kotlin.math.abs(2f * lightness - 1f)).coerceAtLeast(1e-4f)
    val hue = when {
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return Triple(if (hue < 0f) hue + 360f else hue, saturation, lightness)
}

private fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val normalizedHue = ((hue % 360f) + 360f) % 360f
    val chroma = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val secondary = chroma * (1f - kotlin.math.abs((normalizedHue / 60f) % 2f - 1f))
    val match = lightness - chroma / 2f
    return when {
        normalizedHue < 60f -> Color(match + chroma, match + secondary, match)
        normalizedHue < 120f -> Color(match + secondary, match + chroma, match)
        normalizedHue < 180f -> Color(match, match + chroma, match + secondary)
        normalizedHue < 240f -> Color(match, match + secondary, match + chroma)
        normalizedHue < 300f -> Color(match + secondary, match, match + chroma)
        else -> Color(match + chroma, match, match + secondary)
    }
}
