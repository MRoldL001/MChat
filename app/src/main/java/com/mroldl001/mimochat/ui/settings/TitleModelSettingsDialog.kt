package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.AIModel
import com.mroldl001.mimochat.ui.chat.components.SettingsContainerDialog
import com.mroldl001.mimochat.ui.chat.components.SettingsDialogIcon
import com.mroldl001.mimochat.ui.chat.components.rememberSettingsTransition

private const val FOLLOW = "FOLLOW"

@Composable
private fun TitleModelOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TitleModelSettingsDialog(
    currentTitleModelId: String,
    models: List<AIModel>,
    onTitleModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val transition = rememberSettingsTransition()
    SettingsContainerDialog(
        anchorBounds = Rect.Zero,
        transition = transition,
        onDismissRequest = onDismiss,
        icon = { SettingsDialogIcon(Icons.Outlined.Title) },
        title = {
            Text(
                text = stringResource(R.string.title_model_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TitleModelOptionRow(
                    label = stringResource(R.string.title_model_follow),
                    selected = currentTitleModelId == FOLLOW,
                    onClick = { onTitleModelSelected(FOLLOW) }
                )
                models.forEach { model ->
                    TitleModelOptionRow(
                        label = model.name,
                        selected = currentTitleModelId == model.id,
                        onClick = { onTitleModelSelected(model.id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        },
        dismissButton = {}
    )
}
