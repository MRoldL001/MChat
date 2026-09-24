package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
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
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "发送失败",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (isUser) {
                val hasAttachment = message.attachmentUri != null
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
                        message.attachmentUri?.let { uri ->
                            AttachmentPreview(
                                uri = uri,
                                mimeType = message.attachmentMimeType,
                                embedded = true,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
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
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (!isUser) {
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, start = 0.dp, end = 4.dp)
            )
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
