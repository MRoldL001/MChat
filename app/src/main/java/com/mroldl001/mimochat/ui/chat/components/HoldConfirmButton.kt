package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mroldl001.mimochat.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HoldConfirmButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdTimeMillis: Long = 1000,
    resetAfterMillis: Long = 1200
) {
    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary
    val surfaceContainerHigh = colorScheme.surfaceContainerHigh

    val progress = remember { Animatable(0f) }
    var holding by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val label = stringResource(R.string.hold_to_confirm)
    val doneLabel = stringResource(R.string.hold_to_confirm_done)

    val fillColor = lerp(primary, Color.White, 0.78f)
    val scale by animateFloatAsState(
        targetValue = if (holding && !done) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "hold_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceContainerHigh)
            .border(2.dp, primary, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (done) return@detectTapGestures
                    holding = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val fill = scope.launch {
                        progress.animateTo(1f, tween(holdTimeMillis.toInt(), easing = LinearEasing))
                        done = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                        delay(resetAfterMillis)
                        done = false
                        progress.snapTo(0f)
                        holding = false
                    }
                    awaitRelease()
                    if (!done) {
                        fill.cancel()
                        holding = false
                        progress.animateTo(0f, tween(200))
                    }
                })
            }
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                .background(fillColor)
        )
        Text(
            text = if (done) doneLabel else label,
            color = primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HoldDeleteLayout(
    message: String,
    onConfirm: () -> Unit
) {
    Column {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        HoldConfirmButton(onConfirm = onConfirm)
    }
}
