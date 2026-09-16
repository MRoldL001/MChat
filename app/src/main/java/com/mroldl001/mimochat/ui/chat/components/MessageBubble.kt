package com.mroldl001.mimochat.ui.chat.components

import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import android.widget.ImageView
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mroldl001.mimochat.domain.model.Message
import java.text.SimpleDateFormat
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
                val context = LocalContext.current
                val density = remember { context.resources.displayMetrics.density }
                val textColor = MaterialTheme.colorScheme.onPrimaryContainer
                val bgColor = MaterialTheme.colorScheme.primaryContainer

                key(textColor, bgColor, message.content, message.attachmentUri) {
                    Column(horizontalAlignment = Alignment.End) {
                        message.attachmentUri?.let { uri ->
                            AttachmentPreview(uri, message.attachmentMimeType)
                        }
                        SelectionContainer {
                        AndroidView(
                            factory = { ctx ->
                                val drawable = GradientDrawable().apply {
                                    cornerRadii = floatArrayOf(
                                        16 * density, 16 * density,
                                        4 * density, 4 * density,
                                        16 * density, 16 * density,
                                        16 * density, 16 * density
                                    )
                                    setColor(bgColor.toArgb())
                                }
                                TextView(ctx).apply {
                                    textSize = 15f
                                    setTextColor(textColor.toArgb())
                                    setTextIsSelectable(true)
                                    isClickable = false
                                    isLongClickable = true
                                    setPadding(
                                        (12 * density).toInt(),
                                        (8 * density).toInt(),
                                        (12 * density).toInt(),
                                        (8 * density).toInt()
                                    )
                                    background = drawable
                                    isFocusable = true
                                }
                            },
                            update = { textView ->
                                textView.text = message.content
                            },
                            modifier = Modifier
                                .widthIn(max = 320.dp)
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
                            textColor = MaterialTheme.colorScheme.onSurface
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

        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
private fun AttachmentPreview(uri: String, mimeType: String?) {
    val context = LocalContext.current
    when {
        mimeType?.startsWith("image/") == true -> {
            coil.compose.AsyncImage(
                model = Uri.parse(uri),
                contentDescription = "图片附件",
                modifier = Modifier.sizeIn(maxWidth = 280.dp, maxHeight = 220.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        mimeType?.startsWith("video/") == true -> {
            AndroidView(
                factory = {
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageBitmap(runCatching {
                            MediaMetadataRetriever().let { retriever ->
                                try {
                                retriever.setDataSource(context, Uri.parse(uri))
                                retriever.getFrameAtTime(0)
                                } finally {
                                    retriever.release()
                                }
                            }
                        }.getOrNull())
                    }
                },
                modifier = Modifier.sizeIn(maxWidth = 280.dp, maxHeight = 220.dp)
            )
        }
        else -> {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Outlined.AudioFile,
                    contentDescription = "音频附件",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(14.dp).size(28.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
