package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
fun InputBar(
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    isGenerating: Boolean = false,
    onTakePhoto: () -> Unit = {},
    onSelectFile: () -> Unit = {},
    onAttachmentCleared: () -> Unit = {},
    attachmentUri: String? = null,
    attachmentMimeType: String? = null,
    attachmentLabel: String? = null,
    isAttachmentEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var attachmentMenuExpanded by remember { mutableStateOf(false) }
    var attachmentMenuMounted by remember { mutableStateOf(false) }
    val attachmentMenuProgress = remember { Animatable(0f) }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val canSend = (messageText.isNotBlank() || attachmentUri != null) && !isGenerating
    val menuGapPx = with(density) { 8.dp.roundToPx() }
    val menuMarginPx = with(density) { 8.dp.roundToPx() }
    val menuHeightPx = with(density) { 128.dp.roundToPx() }
    val attachmentMenuPositionProvider = remember(menuGapPx, menuMarginPx, menuHeightPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = anchorBounds.left.coerceIn(
                    menuMarginPx,
                    (windowSize.width - popupContentSize.width - menuMarginPx)
                        .coerceAtLeast(menuMarginPx)
                )
                val aboveY = anchorBounds.top - menuHeightPx - menuGapPx
                val y = if (aboveY >= menuMarginPx) {
                    aboveY
                } else {
                    (anchorBounds.bottom + menuGapPx).coerceAtMost(
                        (windowSize.height - popupContentSize.height - menuMarginPx)
                            .coerceAtLeast(menuMarginPx)
                    )
                }
                return IntOffset(x, y)
            }
        }
    }

    LaunchedEffect(attachmentMenuExpanded) {
        if (attachmentMenuExpanded) {
            attachmentMenuProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 280,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            attachmentMenuProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 220,
                    easing = FastOutSlowInEasing
                )
            )
            if (!attachmentMenuExpanded) attachmentMenuMounted = false
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (attachmentUri != null) {
                AttachmentPreview(
                    uri = attachmentUri,
                    mimeType = attachmentMimeType,
                    label = attachmentLabel,
                    compact = attachmentMimeType?.startsWith("image/") == true ||
                        attachmentMimeType?.startsWith("video/") == true,
                    onClear = onAttachmentCleared,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // 附件按钮可用/不可用（如切换到不支持多模态的模型时）的过渡动画，
            // 与发送按钮的可用/不可用变换保持一致。
            val attachmentActive = !isGenerating && isAttachmentEnabled
            val attachmentPrimary = MaterialTheme.colorScheme.primary
            val attachmentContainerColor by animateColorAsState(
                targetValue = if (attachmentActive) attachmentPrimary else attachmentPrimary.copy(alpha = 0.15f),
                label = "attachmentContainerColor"
            )
            val attachmentContentColor by animateColorAsState(
                targetValue = if (attachmentActive) MaterialTheme.colorScheme.onPrimary else attachmentPrimary.copy(alpha = 0.4f),
                label = "attachmentContentColor"
            )
            val attachmentAlpha by animateFloatAsState(
                targetValue = if (attachmentActive) 1f else 0.7f,
                label = "attachmentAlpha"
            )
            Box {
                FilledIconButton(
                    onClick = {
                        focusManager.clearFocus()
                        if (attachmentMenuExpanded) {
                            attachmentMenuExpanded = false
                        } else {
                            attachmentMenuMounted = true
                            attachmentMenuExpanded = true
                        }
                    },
                    enabled = attachmentActive,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = attachmentContainerColor,
                        contentColor = attachmentContentColor,
                        disabledContainerColor = attachmentContainerColor,
                        disabledContentColor = attachmentContentColor
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(attachmentAlpha)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加附件")
                }
                val opaqueMenuColor = MaterialTheme.colorScheme.surfaceVariant
                    .compositeOver(MaterialTheme.colorScheme.surface)
                if (attachmentMenuMounted) {
                    Popup(
                        popupPositionProvider = attachmentMenuPositionProvider,
                        onDismissRequest = { attachmentMenuExpanded = false },
                        properties = PopupProperties(
                            focusable = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            clippingEnabled = false
                        )
                    ) {
                        AttachmentContainerTransformMenu(
                            progress = attachmentMenuProgress.value,
                            collapsedColor = MaterialTheme.colorScheme.primary,
                            collapsedContentColor = MaterialTheme.colorScheme.onPrimary,
                            expandedColor = opaqueMenuColor,
                            onDismiss = { attachmentMenuExpanded = false },
                            onTakePhoto = {
                                attachmentMenuExpanded = false
                                onTakePhoto()
                            },
                            onSelectFile = {
                                attachmentMenuExpanded = false
                                onSelectFile()
                            }
                        )
                    }
                }
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
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送"
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun AttachmentContainerTransformMenu(
    progress: Float,
    collapsedColor: Color,
    collapsedContentColor: Color,
    expandedColor: Color,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onSelectFile: () -> Unit
) {
    val density = LocalDensity.current
    val clampedProgress = progress.coerceIn(0f, 1f)
    val startScaleX = 48f / 216f
    val startScaleY = 48f / 128f
    val scaleX = startScaleX + (1f - startScaleX) * clampedProgress
    val scaleY = startScaleY + (1f - startScaleY) * clampedProgress
    val sourceOffsetY = with(density) { 56.dp.toPx() }
    val contentOffsetY = with(density) { 10.dp.toPx() }
    val endRadiusPx = with(density) { 28.dp.toPx() }
    val morphShape = remember(clampedProgress, endRadiusPx) {
        AttachmentMenuMorphShape(clampedProgress, endRadiusPx)
    }
    val contentProgress = ((clampedProgress - 0.42f) / 0.58f).coerceIn(0f, 1f)
    val containerColor = lerp(collapsedColor, expandedColor, clampedProgress)

    Box(modifier = Modifier.size(width = 216.dp, height = 184.dp)) {
        Box(
            modifier = Modifier
                .size(width = 216.dp, height = 128.dp)
                .graphicsLayer {
                    alpha = (clampedProgress * 4f).coerceIn(0f, 1f)
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    translationY = sourceOffsetY * (1f - clampedProgress)
                    transformOrigin = TransformOrigin(0f, 1f)
                    shape = morphShape
                    clip = true
                }
                .background(containerColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentProgress
                        translationY = contentOffsetY * (1f - contentProgress)
                    }
                    .padding(vertical = 8.dp)
            ) {
                AttachmentMenuItem(
                    text = "拍摄照片",
                    icon = Icons.Default.CameraAlt,
                    onClick = onTakePhoto
                )
                AttachmentMenuItem(
                    text = "选取文件",
                    icon = Icons.Default.FolderOpen,
                    onClick = onSelectFile
                )
            }
        }
        Surface(
            onClick = onDismiss,
            modifier = Modifier
                .offset(y = 136.dp)
                .size(48.dp),
            shape = CircleShape,
            color = collapsedColor,
            contentColor = collapsedContentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "收起附件菜单"
                )
            }
        }
    }
}

private class AttachmentMenuMorphShape(
    private val progress: Float,
    private val endRadiusPx: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusX = size.width / 2f + (endRadiusPx - size.width / 2f) * progress
        val radiusY = size.height / 2f + (endRadiusPx - size.height / 2f) * progress
        val cornerRadius = CornerRadius(radiusX, radiusY)
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                topLeftCornerRadius = cornerRadius,
                topRightCornerRadius = cornerRadius,
                bottomRightCornerRadius = cornerRadius,
                bottomLeftCornerRadius = cornerRadius
            )
        )
    }
}

@Composable
private fun AttachmentMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
                    .compositeOver(MaterialTheme.colorScheme.surface),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(18.dp)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    )
}
