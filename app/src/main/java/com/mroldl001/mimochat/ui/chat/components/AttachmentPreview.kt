package com.mroldl001.mimochat.ui.chat.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AttachmentPreview(
    uri: String,
    mimeType: String?,
    label: String? = null,
    compact: Boolean = false,
    embedded: Boolean = false,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val normalizedMimeType = mimeType.orEmpty().lowercase()
    val context = LocalContext.current
    val resolvedLabel by produceState(initialValue = label, uri, label) {
        value = label ?: withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.query(
                    Uri.parse(uri),
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
        }
    }

    when {
        normalizedMimeType.startsWith("image/") -> VisualAttachmentPreview(
            compact = compact,
            embedded = embedded,
            onClear = onClear,
            modifier = modifier
        ) {
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = "图片附件",
                contentScale = if (compact) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        normalizedMimeType.startsWith("video/") -> VideoAttachmentPreview(
            uri = uri,
            compact = compact,
            embedded = embedded,
            onClear = onClear,
            modifier = modifier
        )

        normalizedMimeType.startsWith("audio/") -> FileAttachmentPreview(
            icon = { Icon(Icons.Outlined.AudioFile, contentDescription = null) },
            title = resolvedLabel ?: "音频附件",
            subtitle = "音频",
            embedded = embedded,
            onClear = onClear,
            modifier = modifier
        )

        else -> FileAttachmentPreview(
            icon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
            title = resolvedLabel ?: "文件附件",
            subtitle = fileTypeLabel(normalizedMimeType, resolvedLabel),
            embedded = embedded,
            onClear = onClear,
            modifier = modifier
        )
    }
}

@Composable
private fun VisualAttachmentPreview(
    compact: Boolean,
    embedded: Boolean,
    onClear: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 15.dp)
    val sizeModifier = if (compact) {
        Modifier.size(96.dp)
    } else if (embedded) {
        Modifier
            .width(236.dp)
            .height(164.dp)
    } else {
        Modifier
            .width(236.dp)
            .height(164.dp)
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
        ClearAttachmentButton(onClear)
    }
}

@Composable
private fun VideoAttachmentPreview(
    uri: String,
    compact: Boolean,
    embedded: Boolean,
    onClear: (() -> Unit)?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().let { retriever ->
                    try {
                        retriever.setDataSource(context, Uri.parse(uri))
                        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } finally {
                        retriever.release()
                    }
                }
            }.getOrNull()
        }
    }

    VisualAttachmentPreview(
        compact = compact,
        embedded = embedded,
        onClear = onClear,
        modifier = modifier
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "视频附件",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.VideoFile,
                contentDescription = "视频附件",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(50),
            tonalElevation = 1.dp,
            modifier = Modifier.size(if (compact) 36.dp else 44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放视频",
                    modifier = Modifier.size(if (compact) 23.dp else 28.dp)
                )
            }
        }
    }
}

@Composable
private fun FileAttachmentPreview(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    embedded: Boolean,
    onClear: (() -> Unit)?,
    modifier: Modifier
) {
    Box(modifier = modifier.widthIn(min = 204.dp, max = 244.dp)) {
        Surface(
            color = if (embedded) {
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.07f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 9.dp,
                    end = if (onClear == null) 12.dp else 42.dp,
                    bottom = 9.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(10.dp),
                    contentColor = if (embedded) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (embedded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (embedded) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        ClearAttachmentButton(onClear)
    }
}

private fun fileTypeLabel(mimeType: String, fileName: String?): String {
    return when {
        mimeType == "application/pdf" -> "PDF 文档"
        mimeType.contains("word") -> "Word 文档"
        mimeType.contains("sheet") || mimeType.contains("excel") -> "表格"
        mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "演示文稿"
        mimeType.startsWith("text/") -> "文本文件"
        else -> fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
            ?.let { "$it 文件" }
            ?: "文件"
    }
}

@Composable
private fun BoxScope.ClearAttachmentButton(onClear: (() -> Unit)?) {
    if (onClear == null) return
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(28.dp)
    ) {
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除附件",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
