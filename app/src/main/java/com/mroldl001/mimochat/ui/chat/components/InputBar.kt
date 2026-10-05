package com.mroldl001.mimochat.ui.chat.components
import android.text.InputType
import android.view.Gravity
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.MessageAttachment
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import java.lang.ref.WeakReference

@Composable
fun InputBar(
    onSendMessage: (String) -> Unit,
    onStopGenerating: () -> Unit,
    isGenerating: Boolean = false,
    onTakePhoto: () -> Unit = {},
    onSelectFile: () -> Unit = {},
    onAttachmentRemoved: (Int) -> Unit = {},
    onAttachmentClick: ((Int) -> Unit)? = null,
    attachments: List<MessageAttachment> = emptyList(),
    isAttachmentEnabled: Boolean = true,
    draftText: String = "",
    draftToken: Int = 0,
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    var attachmentMenuExpanded by remember { mutableStateOf(false) }
    var attachmentMenuMounted by remember { mutableStateOf(false) }
    val attachmentMenuProgress = remember { Animatable(0f) }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val canSend = (messageText.isNotBlank() || attachments.isNotEmpty()) && !isGenerating
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

    LaunchedEffect(draftToken) {
        if (draftToken != 0) {
            messageText = draftText
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
            if (attachments.isNotEmpty()) {
                AttachmentPreviewList(
                    attachments = attachments,
                    onClear = onAttachmentRemoved,
                    onClick = onAttachmentClick,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        SystemInputFocus.clear()
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
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.add_attachment))
                }
                // 暗色下是近黑底，文字用 onSurface 自动取反
                val opaqueMenuColor = MaterialTheme.colorScheme.surfaceContainerLowest
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
            Spacer(Modifier.width(8.dp))
            AnimatedVisibility(
                visible = isEditing,
                enter = fadeIn(tween(200)) + expandHorizontally(tween(200), expandFrom = Alignment.Start),
                exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200), shrinkTowards = Alignment.Start)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = onCancelEdit,
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
                        Icon(
                            imageVector = Icons.Outlined.Undo,
                            contentDescription = stringResource(R.string.cancel_edit)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            SystemTextField(
                value = messageText,
                onValueChange = { messageText = it },
                enabled = !isGenerating,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))
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
                        contentDescription = stringResource(R.string.stop_generating)
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
                        contentDescription = stringResource(R.string.send)
                    )
                }
            }
            }
        }
    }
}

// AndroidView 里的 EditText 焦点不在 Compose 焦点树里，focusManager.clearFocus() 管不到它，
// 点击聊天区收起键盘时需要手动清掉
internal object SystemInputFocus {
    private var target: WeakReference<android.widget.EditText>? = null

    fun attach(view: android.widget.EditText) {
        target = WeakReference(view)
    }

    fun detach(view: android.widget.EditText) {
        if (target?.get() === view) target = null
    }

    fun clear() {
        target?.get()?.clearFocus()
    }
}

@Composable
private fun SystemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // 用真实 EditText，长按弹出的选择菜单才是系统原生那套（与消息气泡内的 TextView 一致）
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val textSize = MaterialTheme.typography.bodyLarge.fontSize.value
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    var focused by remember { mutableStateOf(false) }
    var hasText by remember { mutableStateOf(value.isNotEmpty()) }
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) primary else outline,
                shape = shape
            )
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        // EditText 高度自适应，单行时撑不满 56dp，不居中会偏上
        contentAlignment = Alignment.CenterStart
    ) {
        if (!hasText) {
            Text(
                text = stringResource(R.string.input_message_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = hintColor
            )
        }
        AndroidView(
            factory = { context ->
                android.widget.EditText(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setPadding(0, 0, 0, 0)
                    includeFontPadding = false
                    gravity = Gravity.TOP or Gravity.START
                    setLineSpacing(0f, 1.1f)
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    maxLines = 4
                    setOnFocusChangeListener { _, hasFocus ->
                        focused = hasFocus
                    }
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                        override fun afterTextChanged(s: android.text.Editable?) {
                            val text = s?.toString().orEmpty()
                            hasText = text.isNotEmpty()
                            currentOnValueChange(text)
                        }
                    })
                    SystemInputFocus.attach(this)
                }
            },
            update = { editText ->
                if (editText.text.toString() != value) {
                    val selection = editText.selectionStart.coerceAtLeast(0)
                    editText.setText(value)
                    editText.setSelection(selection.coerceIn(0, value.length))
                }
                hasText = value.isNotEmpty()
                editText.isEnabled = enabled
                editText.setTextColor((if (enabled) textColor else hintColor).toArgb())
                editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSize)
            },
            onRelease = { editText ->
                SystemInputFocus.detach(editText)
            },
            modifier = Modifier.fillMaxWidth()
        )
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
                }
                .shadow(elevation = 8.dp, shape = morphShape, clip = true)
                .background(containerColor, morphShape)
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
                    text = stringResource(R.string.take_photo),
                    icon = Icons.Outlined.CameraAlt,
                    onClick = onTakePhoto
                )
                AttachmentMenuItem(
                    text = stringResource(R.string.select_file),
                    icon = Icons.Outlined.FolderOpen,
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
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.collapse_attachment_menu)
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
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
