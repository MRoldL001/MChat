package com.mroldl001.mimochat.ui.chat.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mroldl001.mimochat.domain.model.MessageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AttachmentPreviewList(
    attachments: List<MessageAttachment>,
    onClear: ((Int) -> Unit)? = null,
    onClick: ((Int) -> Unit)? = null,
    compactVisual: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    val visualIndices = remember(attachments) {
        attachments.indices.filter {
            val mime = attachments[it].mimeType.orEmpty().lowercase()
            mime.startsWith("image/") || mime.startsWith("video/")
        }.toList()
    }
    val visualSet = remember(visualIndices) { visualIndices.toSet() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (visualIndices.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visualIndices.forEach { index ->
                    AttachmentPreview(
                        uri = attachments[index].uri,
                        mimeType = attachments[index].mimeType,
                        label = attachments[index].label,
                        compact = compactVisual,
                        onClear = onClear?.let { { it(index) } },
                        onClick = onClick?.let { { it(index) } }
                    )
                }
            }
        }

        attachments.indices.filter { it !in visualSet }.forEach { index ->
            AttachmentPreview(
                uri = attachments[index].uri,
                mimeType = attachments[index].mimeType,
                label = attachments[index].label,
                onClear = onClear?.let { { it(index) } },
                onClick = onClick?.let { { it(index) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun AttachmentPreview(
    uri: String,
    mimeType: String?,
    label: String? = null,
    compact: Boolean = false,
    onClear: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
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

    val audioDurationMs by produceState(initialValue = 0, uri, normalizedMimeType) {
        value = if (normalizedMimeType.startsWith("audio/")) {
            withContext(Dispatchers.IO) {
                runCatching {
                    MediaMetadataRetriever().let { retriever ->
                        try {
                            retriever.setDataSource(context, Uri.parse(uri))
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toIntOrNull() ?: 0
                        } finally {
                            retriever.release()
                        }
                    }
                }.getOrDefault(0)
            }
        } else {
            0
        }
    }

    when {
        normalizedMimeType.startsWith("image/") -> VisualAttachmentPreview(
            compact = compact,
            onClear = onClear,
            onClick = onClick,
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
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        )

        normalizedMimeType.startsWith("audio/") -> FileAttachmentPreview(
            icon = { Icon(Icons.Outlined.AudioFile, contentDescription = null) },
            title = resolvedLabel ?: "音频附件",
            subtitle = if (audioDurationMs > 0) formatDuration(audioDurationMs) else "音频",
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        )

        else -> FileAttachmentPreview(
            icon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
            title = resolvedLabel ?: "文件附件",
            subtitle = fileTypeLabel(normalizedMimeType, resolvedLabel),
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun VisualAttachmentPreview(
    compact: Boolean,
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 15.dp)
    val sizeModifier = if (compact) {
        Modifier.size(96.dp)
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
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
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
    val durationMs by produceState(initialValue = 0, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().let { retriever ->
                    try {
                        retriever.setDataSource(context, Uri.parse(uri))
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toIntOrNull() ?: 0
                    } finally {
                        retriever.release()
                    }
                }
            }.getOrDefault(0)
        }
    }

    VisualAttachmentPreview(
        compact = compact,
        onClear = onClear,
        onClick = onClick,
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
        if (durationMs > 0) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 204.dp, max = 244.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
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
                    contentColor = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
