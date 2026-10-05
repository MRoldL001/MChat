package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.mroldl001.mimochat.domain.model.Message
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
    isLatest: Boolean = false,
    onRetry: (Message) -> Unit = {},
    onEdit: (Message) -> Unit = {},
    onAttachmentClick: ((Int) -> Unit)? = null
) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val hasThinking = !isUser && !message.reasoningContent.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (isUser && message.isFailed) {
                if (isLatest) {
                    Icon(
                        imageVector = Icons.Outlined.Replay,
                        contentDescription = "点击重试",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onRetry(message) }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "发送失败",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (isUser) {
                val hasAttachment = message.attachments.isNotEmpty()
                val bubbleShape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 6.dp,
                    bottomStart = 20.dp
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Column(
                        modifier = if (hasAttachment) Modifier.padding(4.dp) else Modifier,
                        horizontalAlignment = Alignment.End
                    ) {
                        if (message.attachments.isNotEmpty()) {
                            AttachmentPreviewList(
                                attachments = message.attachments,
                                onClick = onAttachmentClick,
                                compactVisual = true,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .widthIn(max = 292.dp)
                            )
                        }
                        if (message.content.isNotBlank()) {
                            val textColor = MaterialTheme.colorScheme.onPrimaryContainer
                            val textSize = MaterialTheme.typography.bodyLarge.fontSize.value
                            AndroidView(
                                factory = { context ->
                                    TextView(context).apply {
                                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        setTextIsSelectable(true)
                                        includeFontPadding = false
                                        gravity = Gravity.START
                                        setLineSpacing(0f, 1.1f)
                                    }
                                },
                                update = { textView ->
                                    textView.text = message.content
                                    textView.setTextColor(textColor.toArgb())
                                    textView.setTextSize(
                                        TypedValue.COMPLEX_UNIT_SP,
                                        textSize
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(
                                        start = if (hasAttachment) 8.dp else 12.dp,
                                        top = if (hasAttachment) 7.dp else 9.dp,
                                        end = if (hasAttachment) 8.dp else 12.dp,
                                        bottom = if (hasAttachment) 7.dp else 9.dp
                                    )
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (hasThinking) {
                        ThinkingCard(
                            reasoningContent = message.reasoningContent!!,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    message.searchResults?.takeIf { it.isNotEmpty() }?.let { results ->
                        SearchResultsCard(searchResults = results)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (message.content.isNotBlank()) {
                        MixedMarkdownLatex(
                            text = message.content,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            isStreaming = message.isStreaming
                        )
                    }

                    if (message.isStreaming && message.content.isNotBlank()) {
                        StreamingIndicator(
                            startTime = message.timestamp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 4.dp).height(32.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = !message.isStreaming && isLatest,
                            enter = fadeIn(tween(200)) + expandHorizontally(tween(200)),
                            exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200), shrinkTowards = Alignment.Start)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                IconButton(
                                    onClick = { onRetry(message) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Replay,
                                        contentDescription = "重试",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp).offset(x = (-7).dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onEdit(message) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "编辑",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp).offset(x = (-7).dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now()
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    return when {
        date == today -> time
        date == today.minusDays(1) -> "昨天 $time"
        else -> SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
