package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val StandardApiUrl = "https://api.xiaomimimo.com"
private const val SubscriptionApiUrl = "https://token-plan-cn.xiaomimimo.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApiBaseUrlDialog(
    currentUrl: String,
    anchorBounds: Rect,
    transition: SettingsTransition,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiBaseUrl by rememberSaveable(currentUrl) { mutableStateOf(currentUrl) }
    val normalizedUrl = apiBaseUrl.trim().trimEnd('/')
    val colors = MaterialTheme.colorScheme
    val tonalColor = colors.primary.copy(alpha = 0.12f).compositeOver(colors.surfaceContainerHigh)
    val confirm = { if (apiBaseUrl.isNotBlank()) onConfirm(apiBaseUrl.trim()) }

    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = colors.surfaceContainerHigh,
        icon = {
            SettingsDialogIcon(Icons.Default.Link)
        },
        title = { Text("API Base URL") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("选择接口类型，或输入自定义地址", style = MaterialTheme.typography.bodyMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("按量付费使用标准接口", style = MaterialTheme.typography.bodySmall)
                    Text("月度套餐使用订阅接口", style = MaterialTheme.typography.bodySmall)
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("标准接口" to StandardApiUrl, "订阅接口" to SubscriptionApiUrl)
                        .forEachIndexed { index, (label, url) ->
                            SegmentedButton(
                                selected = normalizedUrl == url,
                                onClick = { apiBaseUrl = url },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = tonalColor,
                                    activeContentColor = colors.primary,
                                    activeBorderColor = colors.outline,
                                    inactiveContainerColor = Color.Transparent,
                                    inactiveContentColor = colors.onSurfaceVariant,
                                    inactiveBorderColor = colors.outline
                                )
                            ) { Text(label) }
                        }
                }
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = apiBaseUrl.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
