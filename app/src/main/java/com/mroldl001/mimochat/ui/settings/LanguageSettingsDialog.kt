package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.chat.components.SettingsContainerDialog
import com.mroldl001.mimochat.ui.chat.components.SettingsDialogIcon
import com.mroldl001.mimochat.ui.chat.components.rememberSettingsTransition

@Composable
private fun LanguageOptionRow(
    label: AnnotatedString,
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

/** 除简体中文与「跟随系统」外，在语言名后追加（AI 翻译）标注，使用次级小字样式。 */
@Composable
private fun languageDisplayLabel(code: String): AnnotatedString {
    val name = AppLocale.label(code)
    return buildAnnotatedString {
        append(name)
        if (code != AppLocale.ZH_CN && code != AppLocale.SYSTEM) {
            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
            append("  （AI 翻译）")
            pop()
        }
    }
}

@Composable
fun LanguageSettingsDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val transition = rememberSettingsTransition()
    SettingsContainerDialog(
        anchorBounds = Rect.Zero,
        transition = transition,
        onDismissRequest = onDismiss,
        icon = { SettingsDialogIcon(Icons.Outlined.Language) },
        title = {
            Text(
                text = stringResource(R.string.language_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppLocale.ORDERED.forEach { lang ->
                    LanguageOptionRow(
                        label = languageDisplayLabel(lang),
                        selected = lang == currentLanguage,
                        onClick = { onLanguageSelected(lang) }
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
