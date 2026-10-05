package com.mroldl001.mimochat.ui.chat.components

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface as AndroidSurface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.MessageAttachment
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PROGRESS_TICK_MILLIS = 200L
private const val SEEK_SETTLE_TOLERANCE_MS = 400
private val PROGRESS_THUMB_SIZE = 12.dp
private const val FALLBACK_VIDEO_ASPECT = 16f / 9f
private val PAGE_HORIZONTAL_PADDING = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachmentViewerScreen(
    attachments: List<MessageAttachment>,
    initialIndex: Int,
    onNavigateBack: () -> Unit
) {
    if (attachments.isEmpty()) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceIn(0, attachments.lastIndex)
    )
    val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val currentAttachment = attachments.getOrNull(currentIndex)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentAttachment?.label ?: stringResource(R.string.attachment_preview),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (attachments.size > 1) {
                        Text(
                            text = "${currentIndex + 1} / ${attachments.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = PAGE_HORIZONTAL_PADDING)
        ) {
            items(count = attachments.size) { page ->
                val attachment = attachments[page]
                val mime = attachment.mimeType.orEmpty().lowercase()
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .fillParentMaxHeight()
                ) {
                    when {
                        mime.startsWith("image/") -> ImagePreviewPage(attachment)
                        mime.startsWith("video/") -> VideoPreviewPage(attachment)
                        mime.startsWith("audio/") -> AudioPreviewPage(attachment)
                        else -> GenericFilePreviewPage(attachment)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewPage(attachment: MessageAttachment) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = Uri.parse(attachment.uri),
            contentDescription = attachment.label ?: "图片附件",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun GenericFilePreviewPage(attachment: MessageAttachment) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 300.dp)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = attachment.label ?: "文件附件",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun VideoPreviewPage(attachment: MessageAttachment) {
    val playback = rememberMediaPlayback(attachment.uri)
    val isPlaying = playback.isPlaying
    val isPrepared = playback.isPrepared
    val surfaceTextureState = remember(attachment.uri) {
        mutableStateOf<SurfaceTexture?>(null)
    }
    val surfaceTexture by surfaceTextureState

    LaunchedEffect(isPrepared, surfaceTexture) {
        val player = playback.player ?: return@LaunchedEffect
        val texture = surfaceTexture ?: return@LaunchedEffect
        if (isPrepared) {
            runCatching {
                player.setSurface(AndroidSurface(texture))
                player.start()
            }
        }
    }
    DisposableEffect(surfaceTexture) {
        onDispose { playback.player?.runCatching { setSurface(null) } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val aspect = if (playback.videoWidth > 0 && playback.videoHeight > 0) {
                    playback.videoWidth.toFloat() / playback.videoHeight
                } else {
                    FALLBACK_VIDEO_ASPECT
                }
                val frameWidth: Dp
                val frameHeight: Dp
                if (maxWidth > 0.dp && maxHeight > 0.dp && maxWidth / maxHeight > aspect) {
                    frameHeight = maxHeight
                    frameWidth = maxHeight * aspect
                } else {
                    frameWidth = maxWidth
                    frameHeight = if (maxWidth > 0.dp) maxWidth / aspect else maxHeight
                }

                Box(
                    modifier = Modifier
                        .width(frameWidth)
                        .height(frameHeight)
                        .background(color = Color.Black, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            TextureView(context).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(
                                        texture: SurfaceTexture,
                                        width: Int,
                                        height: Int
                                    ) {
                                        surfaceTextureState.value = texture
                                    }

                                    override fun onSurfaceTextureSizeChanged(
                                        texture: SurfaceTexture,
                                        width: Int,
                                        height: Int
                                    ) {
                                        surfaceTextureState.value = texture
                                    }

                                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                                        surfaceTextureState.value = null
                                        return true
                                    }

                                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    FilledIconButton(
                        onClick = { playback.toggle() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    if (playback.duration > 0) {
                        DurationBadge(
                            text = formatDuration(playback.duration),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        MediaProgressBar(
            position = playback.position,
            duration = playback.duration,
            onSeek = { target -> playback.seekTo(target) },
            modifier = Modifier.widthIn(max = 560.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AudioPreviewPage(attachment: MessageAttachment) {
    val playback = rememberMediaPlayback(attachment.uri)
    val isPlaying = playback.isPlaying
    val isPrepared = playback.isPrepared

    LaunchedEffect(isPrepared) {
        val player = playback.player ?: return@LaunchedEffect
        if (isPrepared) {
            runCatching { player.start() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                shape = RoundedCornerShape(26.dp),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = attachment.label ?: "音频附件",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
            FilledIconButton(
                onClick = { playback.toggle() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            MediaProgressBar(
                position = playback.position,
                duration = playback.duration,
                onSeek = { target -> playback.seekTo(target) },
                modifier = Modifier.fillMaxWidth()
            )
            if (!isPrepared) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "正在加载…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MediaProgressBar(
    position: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(1)
    val enabled = duration > 0
    var trackWidthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    var seekTarget by remember { mutableStateOf(-1) }
    val currentOnSeek by rememberUpdatedState(onSeek)

    LaunchedEffect(position) {
        if (seekTarget >= 0 && abs(position - seekTarget) <= SEEK_SETTLE_TOLERANCE_MS) {
            seekTarget = -1
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            dragFraction = -1f
            seekTarget = -1
        }
    }

    val fraction = when {
        dragFraction >= 0f -> dragFraction
        seekTarget >= 0 -> (seekTarget.toFloat() / safeDuration).coerceIn(0f, 1f)
        enabled -> (position.toFloat() / safeDuration).coerceIn(0f, 1f)
        else -> 0f
    }
    val displayPosition = (fraction * safeDuration).toInt()

    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(safeDuration, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val ratio = (offset.x / trackWidthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val target = (ratio * safeDuration).toInt()
                        seekTarget = target
                        currentOnSeek(target)
                    }
                }
                .pointerInput(safeDuration, enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            seekTarget = -1
                            dragFraction =
                                (offset.x / trackWidthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / trackWidthPx.coerceAtLeast(1))
                                .coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (dragFraction >= 0f) {
                                val target = (dragFraction * safeDuration).toInt()
                                seekTarget = target
                                currentOnSeek(target)
                            }
                            dragFraction = -1f
                        },
                        onDragCancel = { dragFraction = -1f }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
            if (enabled) {
                val density = LocalDensity.current
                val thumbOffsetPx = with(density) {
                    (trackWidthPx * fraction - PROGRESS_THUMB_SIZE.toPx() / 2f)
                        .coerceAtLeast(0f)
                        .roundToInt()
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetPx, 0) }
                        .size(PROGRESS_THUMB_SIZE)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private class MediaPlaybackState(var player: MediaPlayer?) {
    var isPrepared by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var position by mutableIntStateOf(0)
    var duration by mutableIntStateOf(0)
    var seekTarget by mutableStateOf<Int?>(null)
    var videoWidth by mutableIntStateOf(0)
    var videoHeight by mutableIntStateOf(0)

    fun toggle() {
        val current = player ?: return
        runCatching {
            if (current.isPlaying) current.pause() else current.start()
        }
    }

    fun seekTo(target: Int) {
        if (player == null) return
        seekTarget = target
    }
}

@Composable
private fun rememberMediaPlayback(uri: String): MediaPlaybackState {
    val context = LocalContext.current
    val state = remember(uri) { MediaPlaybackState(player = null) }

    DisposableEffect(uri) {
        val created = runCatching {
            MediaPlayer().apply {
                setDataSource(context, Uri.parse(uri))
                setOnPreparedListener { mp ->
                    state.isPrepared = true
                    state.duration = runCatching { mp.duration }.getOrDefault(0)
                    state.videoWidth = runCatching { mp.videoWidth }.getOrDefault(0)
                    state.videoHeight = runCatching { mp.videoHeight }.getOrDefault(0)
                }
                setOnCompletionListener { state.isPlaying = false }
                setOnErrorListener { _, _, _ ->
                    state.isPrepared = false
                    state.isPlaying = false
                    true
                }
                prepareAsync()
            }
        }.getOrNull()

        state.player = created
        state.isPrepared = false
        state.isPlaying = false
        state.position = 0
        state.duration = 0
        state.videoWidth = 0
        state.videoHeight = 0

        onDispose {
            runCatching { created?.release() }
            state.player = null
            state.isPrepared = false
            state.isPlaying = false
            state.position = 0
            state.duration = 0
            state.videoWidth = 0
            state.videoHeight = 0
        }
    }

    LaunchedEffect(uri) {
        while (true) {
            delay(PROGRESS_TICK_MILLIS)
            val player = state.player ?: continue
            if (!state.isPrepared) continue
            val target = state.seekTarget
            if (target != null) {
                state.seekTarget = null
                runCatching { player.seekTo(target) }
                state.position = target
            } else {
                state.position = runCatching { player.currentPosition }
                    .getOrDefault(state.position)
            }
            state.isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
        }
    }

    return state
}

internal fun formatDuration(millis: Int): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
