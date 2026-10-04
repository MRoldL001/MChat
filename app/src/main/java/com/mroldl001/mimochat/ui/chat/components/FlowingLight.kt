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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.sin

private const val SWEEP_DURATION_MILLIS = 2400
private const val PULSE_DURATION_MILLIS = 2900
private const val BEAM_MARGIN = 0.35f
private const val BEAM_SIGMA = 0.12f
private const val SAMPLE_COUNT = 48
private const val TWO_PI = 6.2831855f

private data class BeamPalette(
    val base: Color,
    val highlight: Color,
    val edge: Color
)

@Composable
fun rememberBeamingSpanStyle(
    baseColor: Color,
    sweepDurationMillis: Int = SWEEP_DURATION_MILLIS
): SpanStyle {
    val isLightBackground = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val palette = remember(baseColor, isLightBackground) {
        buildBeamPalette(baseColor, isLightBackground)
    }
    val transition = rememberInfiniteTransition(label = "beaming")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = sweepDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beaming_sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beaming_pulse"
    )
    val beam = 0.5f + 0.5f * sin(TWO_PI * pulse)
    val brush = remember(palette, sweep, beam, isLightBackground) {
        val center = -BEAM_MARGIN + sweep * (1f + 2f * BEAM_MARGIN)
        val restMix = if (isLightBackground) 0.05f + 0.13f * beam else 0.04f + 0.12f * beam
        val stops = mutableListOf<Pair<Float, Color>>()
        repeat(SAMPLE_COUNT + 1) { index ->
            val offset = index.toFloat() / SAMPLE_COUNT
            val flash = beamFlash(offset, center)
            val mix = (restMix + 0.9f * flash).coerceIn(0f, 1f)
            stops.add(offset to lerp(palette.base, palette.highlight, mix))
        }
        Brush.horizontalGradient(*stops.toTypedArray())
    }
    val density = LocalDensity.current
    val edgeRadius = with(density) { (1.5f + 2.5f * beam).dp.toPx() }
    val edgeAlpha = 0.16f + 0.34f * beam
    val shadow = remember(palette.edge, edgeRadius, edgeAlpha) {
        Shadow(
            color = palette.edge.copy(alpha = edgeAlpha),
            offset = Offset.Zero,
            blurRadius = edgeRadius
        )
    }
    return remember(brush, shadow) { SpanStyle(brush = brush, shadow = shadow) }
}

private fun beamFlash(offset: Float, center: Float): Float {
    val distance = (offset - center) / BEAM_SIGMA
    return exp(-distance * distance * 0.5f)
}

private fun buildBeamPalette(baseColor: Color, isLightBackground: Boolean): BeamPalette {
    val (hue, saturation, lightness) = rgbToHsl(baseColor.red, baseColor.green, baseColor.blue)
    val highlight = hslToColor(
        hue = hue,
        saturation = saturation.coerceAtLeast(0.7f),
        lightness = (lightness + 0.28f).coerceIn(0f, 0.9f)
    )
    val edge = hslToColor(
        hue = hue,
        saturation = saturation.coerceAtLeast(0.85f).coerceIn(0f, 1f),
        lightness = lightness.coerceIn(0.45f, 0.68f)
    )
    return BeamPalette(base = baseColor, highlight = highlight, edge = edge)
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
