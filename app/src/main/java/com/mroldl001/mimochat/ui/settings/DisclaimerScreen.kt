package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.mroldl001.mimochat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisclaimerScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_disclaimer)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                DisclaimerSection(stringResource(R.string.disclaimer_section_non_official)) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.disclaimer_non_official_lead))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.disclaimer_non_official_emphasis))
                            }
                            append(stringResource(R.string.disclaimer_non_official_tail))
                        }
                    )
                }

                DisclaimerSection(stringResource(R.string.disclaimer_section_ip)) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.disclaimer_ip_lead))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.disclaimer_ip_emphasis))
                            }
                            append(stringResource(R.string.disclaimer_ip_tail))
                        }
                    )
                }

                DisclaimerSection(stringResource(R.string.disclaimer_section_privacy)) {
                    Text(stringResource(R.string.disclaimer_privacy))
                }

                DisclaimerSection(stringResource(R.string.disclaimer_section_risk)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.disclaimer_risk_emphasis))
                            }
                            append(stringResource(R.string.disclaimer_risk_tail))
                        }
                    )
                }

                DisclaimerSection(stringResource(R.string.disclaimer_section_feedback)) {
                    MarkdownText(
                        markdown = stringResource(R.string.disclaimer_feedback),
                        modifier = Modifier.fillMaxWidth(),
                        linkColor = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 27.sp
                        ),
                        isTextSelectable = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclaimerSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        ProvideTextStyle(
            MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 27.sp
            )
        ) {
            content()
        }
    }
}
