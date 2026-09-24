package com.mroldl001.mimochat.ui.settings
import com.mroldl001.mimochat.R

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mroldl001.mimochat.ui.chat.components.PrereleaseUpdateSetting
import com.mroldl001.mimochat.ui.chat.components.animateThemeColor
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentalFeaturesScreen(
    onNavigateBack: () -> Unit,
    isExpandedScreen: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.experimental_features)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "experimental_title"),
                    navigationIconContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "experimental_back_icon")
                )
            )
        }
    ) { paddingValues ->
        val columnModifier = if (isExpandedScreen) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = 560.dp).fillMaxWidth()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = columnModifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                PrereleaseUpdateSetting(
                    checked = uiState.acceptPrereleaseUpdates,
                    onCheckedChange = viewModel::setAcceptPrereleaseUpdates
                )
            }
        }
    }
}
