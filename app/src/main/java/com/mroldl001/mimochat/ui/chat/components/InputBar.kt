package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.net.Uri

@Composable
fun InputBar(
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    isGenerating: Boolean = false,
    onAttachmentSelected: (Uri) -> Unit = {},
    onAttachmentCleared: () -> Unit = {},
    attachmentLabel: String? = null,
    isAttachmentEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val canSend = messageText.isNotBlank() && !isGenerating

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (attachmentLabel != null) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attachmentLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onAttachmentCleared, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "移除附件")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            FilledIconButton(
                onClick = { onAttachmentSelected(Uri.EMPTY) },
                enabled = !isGenerating && isAttachmentEnabled,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加图片、音频或视频")
            }
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                enabled = !isGenerating,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )

            if (isGenerating) {
                FilledIconButton(
                    onClick = onStopGenerating,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "停止生成"
                    )
                }
            } else {
                val primary = MaterialTheme.colorScheme.primary
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                
                val containerColor by animateColorAsState(
                    targetValue = if (canSend) primary else primary.copy(alpha = 0.3f),
                    label = "containerColor"
                )
                
                val contentColor by animateColorAsState(
                    targetValue = if (canSend) onPrimary else primary.copy(alpha = 0.5f),
                    label = "contentColor"
                )
                
                val alpha by animateFloatAsState(
                    targetValue = if (canSend) 1f else 0.7f,
                    label = "alpha"
                )

                FilledIconButton(
                    onClick = {
                        if (canSend) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                        disabledContainerColor = containerColor,
                        disabledContentColor = contentColor
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(alpha)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送"
                    )
                }
            }
            }
        }
    }
}
