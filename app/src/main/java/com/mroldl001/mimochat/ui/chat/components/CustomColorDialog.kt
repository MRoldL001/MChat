package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.themePreviewColorScheme
import kotlin.math.roundToInt

private const val CUSTOM_COLOR_HEX_LENGTH = 6

@Composable
internal fun CustomColorDialog(
    initialHex: String = "#000000",
    onCancel: () -> Unit,
    onSave: (hex: String) -> Unit
) {
    val initial = remember(initialHex) { parseHexToRgb(initialHex) ?: Triple(0, 0, 0) }
    var red by remember(initial) { mutableIntStateOf(initial.first) }
    var green by remember(initial) { mutableIntStateOf(initial.second) }
    var blue by remember(initial) { mutableIntStateOf(initial.third) }
    var hexText by remember(initial) { mutableStateOf(toHex(initial.first, initial.second, initial.third)) }
    var hexError by remember { mutableStateOf(false) }

    fun syncFromChannel() {
        hexText = toHex(red, green, blue)
        hexError = false
    }

    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        icon = { SettingsDialogIcon(Icons.Outlined.Palette) },
        title = { Text("自定义色彩") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 主题预览：白天 / 黑夜两份配色
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    val previewHex = toHex(red, green, blue)
                    ThemePreviewCard(
                        label = "白天",
                        scheme = themePreviewColorScheme(
                            ThemeColor.CUSTOM,
                            dark = false,
                            customColorHex = previewHex
                        ),
                        selected = false,
                        onClick = {}
                    )
                    ThemePreviewCard(
                        label = "黑夜",
                        scheme = themePreviewColorScheme(
                            ThemeColor.CUSTOM,
                            dark = true,
                            customColorHex = previewHex
                        ),
                        selected = false,
                        onClick = {}
                    )
                }

                ChannelSlider(
                    label = "R",
                    value = red,
                    onValueChange = {
                        red = it
                        syncFromChannel()
                    }
                )
                ChannelSlider(
                    label = "G",
                    value = green,
                    onValueChange = {
                        green = it
                        syncFromChannel()
                    }
                )
                ChannelSlider(
                    label = "B",
                    value = blue,
                    onValueChange = {
                        blue = it
                        syncFromChannel()
                    }
                )

                Text(
                    text = "HEX色值",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    shape = RoundedCornerShape(16.dp),
                    value = hexText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { ch ->
                            ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F'
                        }.take(CUSTOM_COLOR_HEX_LENGTH)
                        hexText = filtered
                        if (filtered.length == CUSTOM_COLOR_HEX_LENGTH) {
                            val rgb = parseHexToRgb(filtered)
                            if (rgb != null) {
                                red = rgb.first
                                green = rgb.second
                                blue = rgb.third
                                hexError = false
                            } else {
                                hexError = true
                            }
                        } else {
                            hexError = true
                        }
                    },
                    singleLine = true,
                    isError = hexError,
                    prefix = { Text("#") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
                if (hexError) {
                    Text(
                        text = "请输入 6 位十六进制颜色值",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave("#${toHex(red, green, blue)}") },
                enabled = !hexError
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.20f)
                    .compositeOver(MaterialTheme.colorScheme.background),
                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            )
        )
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val filtered = raw.filter { it in '0'..'9' }.take(3)
                val parsed = filtered.toIntOrNull()
                when {
                    filtered.isEmpty() -> text = filtered
                    parsed == null -> Unit
                    parsed > 255 -> {
                        text = "255"
                        onValueChange(255)
                    }
                    else -> {
                        text = filtered
                        onValueChange(parsed)
                    }
                }
            },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp)
        )
    }
}

private fun parseHexToRgb(hex: String): Triple<Int, Int, Int>? {
    val h = hex.removePrefix("#").filter { ch ->
        ch in '0'..'9' || ch in 'a'..'f' || ch in 'A'..'F'
    }
    if (h.length != CUSTOM_COLOR_HEX_LENGTH) return null
    return runCatching {
        Triple(
            h.substring(0, 2).toInt(16),
            h.substring(2, 4).toInt(16),
            h.substring(4, 6).toInt(16)
        )
    }.getOrNull()
}

private fun toHex(r: Int, g: Int, b: Int): String {
    return "%02X%02X%02X".format(r, g, b)
}
