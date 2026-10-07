package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.SoundPool
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.chat.components.BouncyWinkIcon
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

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
    }
    var duckId by remember { mutableStateOf(0) }
    var duckReady by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        soundPool.setOnLoadCompleteListener { _, _, status -> duckReady = status == 0 }
        duckId = soundPool.load(context, R.raw.duck, 1)
        onDispose { soundPool.release() }
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
                BouncyWinkIcon(
                    modifier = Modifier.size(100.dp),
                    contentDescription = stringResource(R.string.about_app_icon_desc),
                    onClick = {
                        val played = if (duckReady && duckId != 0) {
                            soundPool.play(duckId, 1f, 1f, 0, 0, 1f)
                        } else 0
                        if (played == 0) {
                            runCatching {
                                MediaPlayer.create(context, R.raw.duck)?.apply {
                                    setOnCompletionListener { it.release() }
                                    start()
                                }
                            }
                        }
                    }
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
    val rowInteractionSource = remember { MutableInteractionSource() }
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = rowInteractionSource,
                indication = null,
                onClick = onClick
            )
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

