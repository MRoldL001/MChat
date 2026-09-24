package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.chat.components.SettingsContainerDialog
import com.mroldl001.mimochat.ui.chat.components.SettingsDialogIcon
import com.mroldl001.mimochat.ui.chat.components.SettingsTransition

private data class OpenSourceLibrary(
    val name: String,
    val license: String
)

private data class OpenSourceGroup(
    val company: String,
    val libraries: List<OpenSourceLibrary>
)

/** 仅「其它」需要本地化，其余为公司专有名称，保持不变。 */
private val COMPANY_LABELS = mapOf(
    "其它" to R.string.open_source_other
)

/** 升级依赖时无需同步版本号，名称与许可证保持即可。 */
private val openSourceGroups = listOf(
    OpenSourceGroup(
        company = "Google",
        libraries = listOf(
            OpenSourceLibrary("AndroidX Core KTX", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Lifecycle（runtime-ktx）", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Activity Compose", "Apache-2.0"),
            OpenSourceLibrary("Jetpack Compose（BOM 统一管理）", "Apache-2.0"),
            OpenSourceLibrary("Material 3", "Apache-2.0"),
            OpenSourceLibrary("Material Icons Extended", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Room（runtime / ktx）", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Hilt Navigation", "Apache-2.0"),
            OpenSourceLibrary("Hilt / Dagger", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Security Crypto", "Apache-2.0"),
            OpenSourceLibrary("Gson", "Apache-2.0")
        )
    ),
    OpenSourceGroup(
        company = "Square",
        libraries = listOf(
            OpenSourceLibrary("Retrofit / converter-gson", "Apache-2.0"),
            OpenSourceLibrary("OkHttp / logging-interceptor", "Apache-2.0"),
            OpenSourceLibrary("Okio", "Apache-2.0")
        )
    ),
    OpenSourceGroup(
        company = "JetBrains",
        libraries = listOf(
            OpenSourceLibrary("Kotlin Standard Library", "Apache-2.0"),
            OpenSourceLibrary("kotlinx.coroutines（android）", "Apache-2.0")
        )
    ),
    OpenSourceGroup(
        company = "其它",
        libraries = listOf(
            OpenSourceLibrary("Simple Icons（代码语言图标）", "CC0-1.0"),
            OpenSourceLibrary("Coil", "Apache-2.0"),
            OpenSourceLibrary("compose-markdown", "MIT"),
            OpenSourceLibrary(
                "Markwon（core / html / linkify / ext-tables / ext-strikethrough / ext-tasklist）",
                "Apache-2.0"
            ),
            OpenSourceLibrary(
                "JLaTeXMath（内置 latexlibrary 模块）",
                "GPL-2.0-or-later"
            )
        )
    )
)

@Composable
internal fun OpenSourceLibrariesDialog(
    transition: SettingsTransition,
    onDismiss: () -> Unit
) {
    SettingsContainerDialog(
        anchorBounds = Rect.Zero,
        transition = transition,
        onDismissRequest = onDismiss,
        icon = { SettingsDialogIcon(Icons.Outlined.Code) },
        title = {
            Text(
                text = stringResource(R.string.about_open_source),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.open_source_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                openSourceGroups.forEach { group ->
                    OpenSourceGroupCard(group)
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

@Composable
private fun OpenSourceGroupCard(group: OpenSourceGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = COMPANY_LABELS[group.company]?.let { stringResource(it) } ?: group.company,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                group.libraries.forEach { library ->
                    OpenSourceLibraryRow(library)
                }
            }
        }
    }
}

@Composable
private fun OpenSourceLibraryRow(library: OpenSourceLibrary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = library.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Surface(
            modifier = Modifier.widthIn(max = 168.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Text(
                text = library.license,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
