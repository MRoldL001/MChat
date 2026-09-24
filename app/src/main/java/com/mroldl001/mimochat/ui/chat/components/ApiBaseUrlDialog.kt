package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
// 此处必须用通配导入：layout 包里存在与 public fun Modifier.weight 同名的 internal
// val RowColumnParentData?.weight，精确导入会让编译器选中 internal 那个并报
// "Cannot access ... it is internal in file"。通配导入只取 public 声明。
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R

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
            SettingsDialogIcon(Icons.Outlined.Link)
        },
        title = { Text("API Base URL") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    stringResource(R.string.api_url_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium.localeScaled()
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.api_url_standard_desc),
                        style = MaterialTheme.typography.bodySmall.localeScaled()
                    )
                    Text(
                        stringResource(R.string.api_url_subscription_desc),
                        style = MaterialTheme.typography.bodySmall.localeScaled()
                    )
                }
                // 刻意不用 Material3 的 SegmentedButton：1.3.1 起它在选中项前会自动画一个勾
                // （内部走 SegmentedButtonDefaults.ActiveIcon 分支），既与高亮重复又压缩标签宽度。
                // 这里手写一个「滑动背景块」分段控件：选中态是一个从左到右平滑移动的 pill，
                // 文字颜色同步切换，没有额外的对勾图标。
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .border(BorderStroke(1.dp, colors.outline), RoundedCornerShape(28.dp))
                ) {
                    val segmentWidth = maxWidth / 2
                    val targetOffset = if (normalizedUrl == StandardApiUrl) 0.dp else segmentWidth
                    val indicatorOffset by animateDpAsState(
                        targetValue = targetOffset,
                        label = "api_url_indicator_offset"
                    )

                    // 底层滑动指示块
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .offset(x = indicatorOffset)
                            .background(tonalColor, RoundedCornerShape(28.dp))
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            stringResource(R.string.api_url_standard_label) to StandardApiUrl,
                            stringResource(R.string.api_url_subscription_label) to SubscriptionApiUrl
                        ).forEachIndexed { index, (label, url) ->
                            val selected = normalizedUrl == url
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { apiBaseUrl = url }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AutoFitText(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.localeScaled(),
                                    textAlign = TextAlign.Center,
                                    color = if (selected) colors.primary else colors.onSurfaceVariant
                                )
                            }
                            if (index == 0) {
                                Spacer(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(colors.outline)
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = apiBaseUrl,
                    onValueChange = { apiBaseUrl = it },
                    label = { Text(stringResource(R.string.api_url_server_address), style = MaterialTheme.typography.bodyMedium.localeScaled()) },
                    placeholder = { Text("https://", style = MaterialTheme.typography.bodyMedium.localeScaled()) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = apiBaseUrl.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
