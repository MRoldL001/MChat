package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

private val openSourceGroups = listOf(
    OpenSourceGroup(
        company = "Google",
        libraries = listOf(
            OpenSourceLibrary("AndroidX Core KTX", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Lifecycle", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Activity Compose", "Apache-2.0"),
            OpenSourceLibrary("Jetpack Compose", "Apache-2.0"),
            OpenSourceLibrary("Material 3", "Apache-2.0"),
            OpenSourceLibrary("Material Icons Extended", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Room", "Apache-2.0"),
            OpenSourceLibrary("AndroidX Hilt Navigation", "Apache-2.0"),
            OpenSourceLibrary("Hilt / Dagger", "Apache-2.0"),
            OpenSourceLibrary("Gson", "Apache-2.0")
        )
    ),
    OpenSourceGroup(
        company = "Square",
        libraries = listOf(
            OpenSourceLibrary("Retrofit / converter-gson", "Apache-2.0"),
            OpenSourceLibrary("OkHttp / logging-interceptor", "Apache-2.0")
        )
    ),
    OpenSourceGroup(
        company = "JetBrains",
        libraries = listOf(
            OpenSourceLibrary("Kotlin Standard Library", "Apache-2.0"),
            OpenSourceLibrary("kotlinx.coroutines", "Apache-2.0")
        )
    ),
    OpenSourceGroup(
        company = "其它",
        libraries = listOf(
            OpenSourceLibrary("Coil", "Apache-2.0"),
            OpenSourceLibrary("compose-markdown", "MIT"),
            OpenSourceLibrary("Markwon", "Apache-2.0"),
            OpenSourceLibrary("JLaTeXMath (sixgodIT fork)", "GPL-2.0-or-later")
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
        icon = { SettingsDialogIcon(Icons.Default.Code) },
        title = {
            Text(
                text = "开源库",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "以下为主要运行时依赖，许可证信息以各项目仓库和 Maven 元数据为准。",
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
                Text("关闭")
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun OpenSourceGroupCard(group: OpenSourceGroup) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = group.company,
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
            text = library.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(12.dp))
        Surface(
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
