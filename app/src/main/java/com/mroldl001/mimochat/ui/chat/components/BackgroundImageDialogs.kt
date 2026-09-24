package com.mroldl001.mimochat.ui.chat.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun BackgroundImageSettingsDialog(
    hasBackgroundImage: Boolean,
    opacity: Float,
    onSelectImage: () -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onRestoreDefault: () -> Unit,
    anchorBounds: Rect = Rect.Zero,
    transition: SettingsTransition? = null,
    onDismiss: () -> Unit
) {
    var temporaryOpacity by remember(opacity) { mutableFloatStateOf(opacity) }
    var opacityText by remember(opacity) {
        mutableStateOf((opacity * 100).roundToInt().toString())
    }
    var opacityError by remember(opacity) { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = {
            onOpacityChanged(temporaryOpacity)
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        icon = { SettingsDialogIcon(Icons.Outlined.Image) },
        title = { AutoFitText(stringResource(R.string.chat_background_title), style = MaterialTheme.typography.headlineSmall.localeScaled()) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BackgroundSettingRow(
                    icon = Icons.Outlined.Crop,
                    title = stringResource(R.string.bg_select_image),
                    description = stringResource(R.string.bg_select_image_desc),
                    onClick = onSelectImage
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SettingIcon(icon = Icons.Outlined.Opacity)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AutoFitText(stringResource(R.string.bg_change_opacity), style = MaterialTheme.typography.titleMedium.localeScaled())
                            OutlinedTextField(
                                shape = RoundedCornerShape(16.dp),
                                value = opacityText,
                                onValueChange = { value ->
                                    opacityText = value
                                    val percentage = value.toFloatOrNull()
                                    opacityError = percentage == null || percentage !in 0f..100f
                                    if (!opacityError) {
                                        temporaryOpacity = percentage!! / 100f
                                    }
                                },
                                singleLine = true,
                                isError = opacityError,
                                suffix = { Text("%") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(100.dp)
                            )
                        }
                        if (opacityError) {
                            Text(
                                text = stringResource(R.string.bg_opacity_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.localeScaled()
                            )
                        }
                        Slider(
                            value = temporaryOpacity,
                            onValueChange = {
                                temporaryOpacity = it
                                opacityText = (it * 100).roundToInt().toString()
                                opacityError = false
                            },
                            valueRange = 0f..1f,
                            onValueChangeFinished = { onOpacityChanged(temporaryOpacity) },
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.20f)
                                    .compositeOver(MaterialTheme.colorScheme.background),
                                inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            )
                        )
                    }
                }

                BackgroundSettingRow(
                    icon = Icons.Outlined.Restore,
                    title = stringResource(R.string.bg_restore_default),
                    description = stringResource(R.string.bg_restore_default_desc),
                    enabled = hasBackgroundImage || temporaryOpacity != PreferencesManager.DEFAULT_CHAT_BACKGROUND_OPACITY,
                    onClick = {
                        temporaryOpacity = PreferencesManager.DEFAULT_CHAT_BACKGROUND_OPACITY
                        opacityText = (PreferencesManager.DEFAULT_CHAT_BACKGROUND_OPACITY * 100)
                            .roundToInt()
                            .toString()
                        opacityError = false
                        onRestoreDefault()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOpacityChanged(temporaryOpacity)
                    onDismiss()
                },
                enabled = !opacityError
            ) {
                AutoFitText(
                    stringResource(R.string.common_done),
                    style = MaterialTheme.typography.labelLarge.localeScaled(),
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
private fun BackgroundSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon = icon, enabled = enabled)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            AutoFitText(
                text = title,
                style = MaterialTheme.typography.titleMedium.localeScaled(),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.localeScaled(),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f)
            )
        }
    }
}

@Composable
private fun SettingIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.15f else 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BackgroundCropDialog(
    sourceUri: Uri,
    onCropped: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadFinished by remember(sourceUri) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var zoom by remember(sourceUri) { mutableFloatStateOf(1f) }
    var offset by remember(sourceUri) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(sourceUri) {
        bitmap = loadBackgroundBitmap(context, sourceUri)
        loadFinished = true
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !saving,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        AutoFitText(
                            stringResource(R.string.common_cancel),
                            style = MaterialTheme.typography.labelLarge.localeScaled(),
                            textAlign = TextAlign.Center
                        )
                    }
                    AutoFitText(
                        text = stringResource(R.string.bg_crop_title),
                        style = MaterialTheme.typography.titleLarge.localeScaled(),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    TextButton(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        enabled = bitmap != null && viewportSize.width > 0 && viewportSize.height > 0 && !saving,
                        onClick = {
                            val source = bitmap ?: return@TextButton
                            val savedZoom = zoom
                            val savedOffset = offset
                            val savedViewport = viewportSize
                            saving = true
                            scope.launch {
                                val outputUri = withContext(Dispatchers.IO) {
                                    cropAndSaveBackground(context, source, savedZoom, savedOffset, savedViewport)
                                }
                                saving = false
                                if (outputUri != null) onCropped(outputUri) else onDismiss()
                            }
                        }
                    ) {
                        AutoFitText(
                            stringResource(R.string.common_done),
                            style = MaterialTheme.typography.labelLarge.localeScaled(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                BackgroundCropFrame(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    val source = bitmap
                    when {
                        source != null -> {
                            val boundedOffset = clampCropOffset(source, zoom, offset, viewportSize)
                            val currentZoom by rememberUpdatedState(zoom)
                            val currentOffset by rememberUpdatedState(boundedOffset)
                            val sourceImage = remember(source) { source.asImageBitmap() }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged {
                                        // Offsets are preview pixels: preserve the selected image center on resize.
                                        val previousSize = viewportSize
                                        val resizedOffset = if (previousSize.width > 0 && previousSize.height > 0) {
                                            val previousScale = max(
                                                previousSize.width.toFloat() / source.width,
                                                previousSize.height.toFloat() / source.height
                                            )
                                            val nextScale = max(
                                                it.width.toFloat() / source.width,
                                                it.height.toFloat() / source.height
                                            )
                                            offset * (nextScale / previousScale)
                                        } else Offset.Zero
                                        viewportSize = it
                                        offset = clampCropOffset(source, zoom, resizedOffset, it)
                                    }
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .pointerInput(source, viewportSize, saving) {
                                        detectTransformGestures { _, pan, gestureZoom, _ ->
                                            if (saving) return@detectTransformGestures
                                            val nextZoom = (currentZoom * gestureZoom).coerceIn(1f, 5f)
                                            zoom = nextZoom
                                            offset = clampCropOffset(source, nextZoom, currentOffset + pan, viewportSize)
                                        }
                                    }
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(28.dp)
                                    )
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val baseScale = max(
                                        size.width / source.width,
                                        size.height / source.height
                                    )
                                    val totalScale = baseScale * zoom
                                    val imageWidth = source.width * totalScale
                                    val imageHeight = source.height * totalScale
                                    val imageLeft = (size.width - imageWidth) / 2f + boundedOffset.x
                                    val imageTop = (size.height - imageHeight) / 2f + boundedOffset.y
                                    drawImage(
                                        image = sourceImage,
                                        dstOffset = IntOffset(
                                            imageLeft.roundToInt(),
                                            imageTop.roundToInt()
                                        ),
                                        dstSize = IntSize(
                                            imageWidth.roundToInt().coerceAtLeast(1),
                                            imageHeight.roundToInt().coerceAtLeast(1)
                                        ),
                                        filterQuality = FilterQuality.High
                                    )
                                }
                            }
                        }
                        !loadFinished -> CircularProgressIndicator()
                        else -> Text(
                            stringResource(R.string.bg_load_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium.localeScaled()
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.bg_crop_hint),
                    style = MaterialTheme.typography.bodyMedium.localeScaled(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                )
            }

            if (saving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                        CircularProgressIndicator(modifier = Modifier.padding(20.dp))
                    }
                }
            }
        }
    }
}

private fun clampCropOffset(
    bitmap: Bitmap,
    zoom: Float,
    offset: Offset,
    viewport: IntSize
): Offset {
    if (viewport.width <= 0 || viewport.height <= 0) return Offset.Zero
    val baseScale = max(
        viewport.width.toFloat() / bitmap.width,
        viewport.height.toFloat() / bitmap.height
    )
    val scaledWidth = bitmap.width * baseScale * zoom
    val scaledHeight = bitmap.height * baseScale * zoom
    val maxX = max(0f, (scaledWidth - viewport.width) / 2f - 1f)
    val maxY = max(0f, (scaledHeight - viewport.height) / 2f - 1f)
    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY)
    )
}

private suspend fun loadBackgroundBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longestSide = max(info.size.width, info.size.height)
                if (longestSide > 4096) {
                    decoder.setTargetSampleSize(ceil(longestSide / 4096f).toInt())
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
    }.getOrNull()
}

private fun cropAndSaveBackground(
    context: Context,
    bitmap: Bitmap,
    zoom: Float,
    offset: Offset,
    viewport: IntSize
): String? = runCatching {
    val boundedOffset = clampCropOffset(bitmap, zoom, offset, viewport)
    val baseScale = max(
        viewport.width.toFloat() / bitmap.width,
        viewport.height.toFloat() / bitmap.height
    )
    val totalScale = baseScale * zoom
    val cropWidth = (viewport.width / totalScale).coerceIn(1f, bitmap.width.toFloat())
    val cropHeight = (viewport.height / totalScale).coerceIn(1f, bitmap.height.toFloat())
    val centerX = bitmap.width / 2f - boundedOffset.x / totalScale
    val centerY = bitmap.height / 2f - boundedOffset.y / totalScale
    val left = (centerX - cropWidth / 2f).coerceIn(0f, bitmap.width - cropWidth)
    val top = (centerY - cropHeight / 2f).coerceIn(0f, bitmap.height - cropHeight)
    val cropped = Bitmap.createBitmap(
        bitmap,
        left.roundToInt(),
        top.roundToInt(),
        cropWidth.roundToInt().coerceAtMost(bitmap.width - left.roundToInt()),
        cropHeight.roundToInt().coerceAtMost(bitmap.height - top.roundToInt())
    )

    // Apply the same resolution limit in either orientation, including wide tablet displays.
    val outputScale = min(1f, 2560f / max(cropped.width, cropped.height))
    val output = if (outputScale < 1f) {
        Bitmap.createScaledBitmap(
            cropped,
            (cropped.width * outputScale).roundToInt().coerceAtLeast(1),
            (cropped.height * outputScale).roundToInt().coerceAtLeast(1),
            true
        )
    } else {
        cropped
    }

    val directory = File(context.filesDir, "backgrounds").apply { mkdirs() }
    val file = File(directory, "chat_background_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { stream ->
        check(output.compress(Bitmap.CompressFormat.JPEG, 92, stream))
    }
    if (output !== cropped) output.recycle()
    cropped.recycle()
    Uri.fromFile(file).toString()
}.getOrNull()

fun deleteStoredChatBackground(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    runCatching {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            val backgroundDirectory = File(context.filesDir, "backgrounds").canonicalFile
            val backgroundFile = File(requireNotNull(uri.path)).canonicalFile
            if (backgroundFile.parentFile == backgroundDirectory) {
                backgroundFile.delete()
            }
        } else if (uri.scheme == "content") {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
}

fun pruneStoredChatBackgrounds(context: Context, keepUriString: String?) {
    val keepFile = keepUriString?.let { uriString ->
        runCatching { Uri.parse(uriString).path?.let(::File)?.canonicalFile }.getOrNull()
    }
    runCatching {
        File(context.filesDir, "backgrounds").listFiles()?.forEach { file ->
            if (file.canonicalFile != keepFile) file.delete()
        }
    }
}
