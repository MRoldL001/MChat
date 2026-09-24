package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.chat.components.SettingsContainerDialog
import com.mroldl001.mimochat.ui.chat.components.SettingsTransition

@Composable
internal fun AboutDialog(
    transition: SettingsTransition,
    onDismiss: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showOpenSource by remember { mutableStateOf(false) }
    val versionName = remember(context) {
        runCatching {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName
                .orEmpty()
        }.getOrDefault("")
    }

    SettingsContainerDialog(
        anchorBounds = Rect.Zero,
        transition = transition,
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_round),
                    contentDescription = stringResource(R.string.about_app_icon_desc),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
                Text(
                    text = "MChat",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.about_version) + " " + versionName.ifBlank { stringResource(R.string.about_unknown) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(22.dp))
                AboutInfoRow(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.about_author_blog),
                    description = stringResource(R.string.about_author_blog_desc),
                    onClick = { uriHandler.openUri("https://mroldl001.top") }
                )
                AboutInfoRow(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_open_source),
                    description = stringResource(R.string.about_open_source_desc),
                    onClick = { showOpenSource = true }
                )
                AboutInfoRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_disclaimer),
                    description = stringResource(R.string.about_disclaimer_desc),
                    onClick = onDisclaimerClick
                )
                Spacer(Modifier.height(16.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        },
        dismissButton = {}
    )

    if (showOpenSource) {
        OpenSourceLibrariesDialog(
            transition = transition,
            onDismiss = { showOpenSource = false }
        )
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier.padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
