package com.mroldl001.mimochat.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisclaimerScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("免责声明") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                DisclaimerSection("非官方声明") {
                    Text(
                        text = buildAnnotatedString {
                            append("本应用（MIMO Chat）为第三方开发的开源客户端，")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("与小米公司（Xiaomi Corporation）及其关联公司（下称小米公司）无任何隶属、授权或合作关系")
                            }
                            append("。")
                        }
                    )
                }

                DisclaimerSection("知识产权") {
                    Text(
                        text = buildAnnotatedString {
                            append("“小米”、“Xiaomi”、“MiMo” 等商标及图形标识归小米公司所有。本应用使用上述标识仅用于描述功能兼容性（")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("即指示性合理使用")
                            }
                            append("）。")
                        }
                    )
                }

                DisclaimerSection("隐私保护") {
                    Text("本应用所有数据均存储在本地设备，对话直接通过 API 与小米服务器通信，开发者不收集或存储任何用户数据。")
                }

                DisclaimerSection("风险承担") {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("本应用按“as-is”原则提供，不包含任何明示或暗示的保证")
                            }
                            append("。用户需自行承担使用本应用带来的风险，包括但不限于数据丢失、隐私泄露或设备损坏。开发者与小米公司不对本应用的合法性、安全性及功能性负责。")
                        }
                    )
                }

                DisclaimerSection("反馈与支持") {
                    MarkdownText(
                        markdown = "若在使用中遇到问题，请前往 [Issues](https://github.com/MRoldL001/MIMO-Chat/issues) 反馈，若有能力也可尝试 fork 源码自行修改后发起 PR。",
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
