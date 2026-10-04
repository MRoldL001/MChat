package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.domain.model.Chat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// key 加前缀免得和 chat.id 撞
sealed interface ChatHistoryEntry {
    val key: Any
    data class Header(val title: String) : ChatHistoryEntry {
        override val key get() = "header_$title"
    }
    data class Item(val chat: Chat) : ChatHistoryEntry {
        override val key get() = chat.id
    }
}

fun buildChatHistoryEntries(chats: List<Chat>): List<ChatHistoryEntry> {
    if (chats.isEmpty()) return emptyList()
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val weekAgoInclusive = today.minusDays(7) // 7 天前当天也计入「最近一周」
    val sorted = chats.sortedByDescending { it.updatedAt }
    val todayList = ArrayList<Chat>()
    val weekList = ArrayList<Chat>()
    val olderList = ArrayList<Chat>()
    for (chat in sorted) {
        val date = Instant.ofEpochMilli(chat.updatedAt).atZone(zoneId).toLocalDate()
        when {
            date == today -> todayList.add(chat)
            !date.isBefore(weekAgoInclusive) -> weekList.add(chat)
            else -> olderList.add(chat)
        }
    }
    val entries = ArrayList<ChatHistoryEntry>(sorted.size + 3)
    if (todayList.isNotEmpty()) {
        entries += ChatHistoryEntry.Header("今天")
        todayList.forEach { entries += ChatHistoryEntry.Item(it) }
    }
    if (weekList.isNotEmpty()) {
        entries += ChatHistoryEntry.Header("最近一周")
        weekList.forEach { entries += ChatHistoryEntry.Item(it) }
    }
    if (olderList.isNotEmpty()) {
        entries += ChatHistoryEntry.Header("更早")
        olderList.forEach { entries += ChatHistoryEntry.Item(it) }
    }
    return entries
}

@Composable
internal fun ChatListDateHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}
