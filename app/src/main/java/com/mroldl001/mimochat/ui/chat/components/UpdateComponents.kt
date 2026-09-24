package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.data.update.GitHubRelease
import com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState

@Composable
internal fun UpdateSettingsItem(
    state: UpdateUiState,
    onCheck: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = state !is UpdateUiState.Checking,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onCheck
                )
                .padding(vertical = 12.dp)
        ) {
            AnimatedUpdateIcon(
                icon = Icons.Outlined.SystemUpdate,
                loading = state is UpdateUiState.Checking
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.check_update),
                    style = MaterialTheme.typography.titleMedium,
                    color = animateThemeColor(MaterialTheme.colorScheme.onSurface, "update_item_title")
                )
                Text(
                    text = stringResource(R.string.check_update_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = animateThemeColor(MaterialTheme.colorScheme.onSurfaceVariant, "update_item_desc")
                )
            }
        }
    }
}

@Composable
private fun AnimatedUpdateIcon(
    icon: ImageVector,
    loading: Boolean = false
) {
    val iconColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 450),
        label = "update_icon_color"
    )
    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 450),
        label = "update_icon_container_color"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = iconColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun PrereleaseUpdateSetting(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedUpdateIcon(icon = Icons.Outlined.NewReleases)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.prerelease_update_title),
                style = MaterialTheme.typography.titleMedium,
                color = animateThemeColor(MaterialTheme.colorScheme.onSurface, "prerelease_title")
            )
            Text(
                stringResource(R.string.prerelease_update_desc),
                style = MaterialTheme.typography.bodySmall,
                color = animateThemeColor(MaterialTheme.colorScheme.onSurfaceVariant, "prerelease_desc")
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun UpdateReleaseDialog(
    release: GitHubRelease,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.settingsDialogWidth(),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = { SettingsDialogIcon(Icons.Outlined.SystemUpdate) },
        title = {
            Text(release.name)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.update_release_notes), fontWeight = FontWeight.Bold)
                Text(
                    text = release.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) { Text(stringResource(R.string.download_and_install)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
