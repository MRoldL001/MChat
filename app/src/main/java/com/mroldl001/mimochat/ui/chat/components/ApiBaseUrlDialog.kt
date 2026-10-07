package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    val sliderColor = colors.primary.copy(alpha = 0.35f).compositeOver(colors.surfaceContainerHigh)
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
                // 不用 Material3 SegmentedButton：1.3.1 起它会在选中项前画勾，既重复又压标签宽度
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(BorderStroke(1.dp, colors.outline), RoundedCornerShape(28.dp))
                ) {
                    val segmentWidth = this.maxWidth / 2
                    val targetOffset = if (normalizedUrl == StandardApiUrl) 0.dp else segmentWidth
                    val indicatorOffset by animateDpAsState(
                        targetValue = targetOffset,
                        label = "api_url_indicator_offset"
                    )
                    val progress = if (segmentWidth > 0.dp) indicatorOffset / segmentWidth else 0f
                    val fullRadius = 28.dp
                    val innerRadius = fullRadius * (1f - kotlin.math.abs(progress - 0.5f) * 2f)
                    val indicatorShape = RoundedCornerShape(
                        topStart = if (progress <= 0.5f) fullRadius else innerRadius,
                        topEnd = if (progress >= 0.5f) fullRadius else innerRadius,
                        bottomEnd = if (progress >= 0.5f) fullRadius else innerRadius,
                        bottomStart = if (progress <= 0.5f) fullRadius else innerRadius
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .offset(x = indicatorOffset)
                            .background(sliderColor, indicatorShape)
                    )

                    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                        listOf(
                            stringResource(R.string.api_url_standard_label) to StandardApiUrl,
                            stringResource(R.string.api_url_subscription_label) to SubscriptionApiUrl
                        ).forEachIndexed { index, (label, url) ->
                            val selected = normalizedUrl == url
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { apiBaseUrl = url }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AutoFitText(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.localeScaled(),
                                    textAlign = TextAlign.Center,
                                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
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
