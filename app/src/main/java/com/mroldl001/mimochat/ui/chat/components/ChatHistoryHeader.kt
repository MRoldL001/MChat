package com.mroldl001.mimochat.ui.chat.components
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mroldl001.mimochat.R

@Composable
fun ChatHistoryHeader(
    title: String = "",
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onGitHubClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsBoundsChanged: (Rect) -> Unit = {}
) {
    val view = LocalView.current
    val displayTitle = if (title.isEmpty()) stringResource(R.string.chat_history) else title
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var fontSizeSp by remember(displayTitle) { mutableStateOf(24f) }
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = fontSizeSp.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
            onTextLayout = { result ->
                if (result.didOverflowWidth && fontSizeSp > 12f) {
                    fontSizeSp *= 0.9f
                }
            }
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconCircleButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    contentDescription = stringResource(R.string.sidebar_search),
                    onClick = onSearchClick
                )
                IconCircleButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    contentDescription = stringResource(R.string.sidebar_settings),
                    modifier = Modifier.onGloballyPositioned { onSettingsBoundsChanged(it.screenBounds(view)) },
                    onClick = onSettingsClick
                )
                IconCircleButton(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.github_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    contentDescription = stringResource(R.string.sidebar_github),
                    onClick = onGitHubClick
                )
            }
        }
    }
}

@Composable
private fun IconCircleButton(
    icon: @Composable () -> Unit,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier.size(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            icon()
        }
    }
}
