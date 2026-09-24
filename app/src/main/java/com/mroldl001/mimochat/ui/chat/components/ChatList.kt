package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatList(
    chats: List<Chat>,
    selectedChatId: Long?,
    onChatSelected: (Chat) -> Unit,
    onCreateNewChat: () -> Unit,
    onDeleteChat: (Chat) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = { },
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "对话历史",
                    style = MaterialTheme.typography.titleLarge
                )
                FilledTonalButton(onClick = onCreateNewChat) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_chat))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新建")
                }
            }

            HorizontalDivider()

            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无对话记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                val isAnimating = remember { mutableStateOf(false) }
                val settledChatId = remember { mutableStateOf(selectedChatId) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                ) {
                    val selectedIndex = chats.indexOfFirst { it.id == selectedChatId }
                    ChatSelectionHighlight(
                        listState = listState,
                        selectedIndex = selectedIndex,
                        selectedChatId = selectedChatId,
                        settledChatId = settledChatId,
                        isAnimating = isAnimating,
                        itemHeight = CHAT_HISTORY_ITEM_HEIGHT
                    )
                    LazyColumn(state = listState) {
                        items(chats, key = { it.id }) { chat ->
                            ChatListItem(
                                chat = chat,
                                isSelected = chat.id == selectedChatId,
                                settledSelectedChatId = settledChatId.value,
                                isAnimating = isAnimating.value,
                                onClick = { onChatSelected(chat) },
                                onDelete = { onDeleteChat(chat) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    isSelected: Boolean,
    settledSelectedChatId: Long?,
    isAnimating: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val contentOffset by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "chatListItemContentOffset"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (chat.id == settledSelectedChatId && !isAnimating) 0.14f else 0f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .height(CHAT_HISTORY_ITEM_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { translationX = contentOffset.toPx() }
        ) {
            Text(
                text = chat.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = formatDate(chat.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val CHAT_HISTORY_ITEM_HEIGHT = 72.dp


private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * 选中高亮：动画期间用浮层滑动，动画结束即「固定」在条目上。
 * - 切换选中（selectedIndex 改变）时播放一次从旧位置到新位置的滑动动画（浮层从旧位置滑到新位置），
 *   动画期间 isAnimating=true，条目自身内联背景隐藏，避免双重高亮；
 * - 动画结束后 isAnimating=false，选中态「沉淀」为条目自身内联背景——它是条目的一部分，固定、跟随滚动，不漂移；
 * - 浮层在动画结束后 alpha=0 隐藏，仅作为切换动画的载体。
 * 相比旧版 snapshotFlow 持续对齐（快速滚动/切换时被反复打断导致偏移），这里只在切换瞬间触发一次动画，
 * 滚动时完全由内联背景承担，因此不再漂移。
 */
@Composable
internal fun ChatSelectionHighlight(
    listState: LazyListState,
    selectedIndex: Int,
    selectedChatId: Long? = null,
    settledChatId: MutableState<Long?>,
    isAnimating: MutableState<Boolean>,
    durationMillis: Int = 360,
    itemHeight: Dp = 72.dp
) {
    if (selectedIndex < 0) return

    val animatable = remember { Animatable(0f) }
    var prevIndex by remember { mutableStateOf(selectedIndex) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = animatable.value }
            .padding(horizontal = 8.dp)
            .height(itemHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.primary.copy(
                    alpha = if (isAnimating.value) 0.14f else 0f
                )
            )
    )

    // key 必须包含 selectedChatId：新建对话时新项插在分组最前，索引可能与旧选中项相同
    // （index 不变），若只监听 selectedIndex 则协程不会重启，高亮会留在旧对话上。
    LaunchedEffect(selectedChatId, selectedIndex) {
        if (selectedIndex == prevIndex) {
            settledChatId.value = selectedChatId
            return@LaunchedEffect
        }
        val oldOffset = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == prevIndex }?.offset
        val newOffset = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == selectedIndex }?.offset
        if (oldOffset != null && newOffset != null) {
            isAnimating.value = true
            animatable.snapTo(oldOffset.toFloat())
            try {
                animatable.animateTo(
                    newOffset.toFloat(),
                    animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
                )
            } finally {
                isAnimating.value = false
            }
        }
        prevIndex = selectedIndex
        settledChatId.value = selectedChatId
    }
}
