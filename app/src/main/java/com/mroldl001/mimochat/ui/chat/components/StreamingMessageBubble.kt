package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamingMessageBubble(
    content: String,
    reasoningContent: String,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val hasThinking = reasoningContent.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (hasThinking) {
            ThinkingCard(
                reasoningContent = reasoningContent,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (content.isNotBlank()) {
            MixedMarkdownLatex(
                text = content,
                textColor = textColor,
                isStreaming = true
            )
        }

        StreamingIndicator(
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
