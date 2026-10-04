package com.mroldl001.mimochat.ui.chat.components

import com.mroldl001.mimochat.R
import android.content.Intent
import android.net.Uri
import android.text.util.Linkify
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.mroldl001.mimochat.domain.model.WebSearchResult
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

@Composable
fun ThinkingCard(
    reasoningContent: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rotation"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.thinking_process),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                        .offset {
                            IntOffset(0, (-2).dp.roundToPx())
                        }
                )
            }
            
            if (isExpanded) {
                Text(
                    text = reasoningContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
fun SearchResultsCard(
    searchResults: List<WebSearchResult>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rotation"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.search_results),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                        .offset {
                            IntOffset(0, (-2).dp.roundToPx())
                        }
                )
            }
            
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                ) {
                    searchResults.forEachIndexed { index, result ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        SearchResultItem(result = result)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: WebSearchResult) {
    val context = LocalContext.current
    val faviconUrl = remember(result.logoUrl, result.url) {
        result.logoUrl?.takeIf { it.isNotBlank() } ?: runCatching {
            val host = Uri.parse(result.url).host?.removePrefix("www.")
            host?.let { "https://icons.duckduckgo.com/ip3/$it.ico" }
        }.getOrNull()
    }
    var faviconLoadFailed by remember(faviconUrl) { mutableStateOf(false) }
    val hostName = remember(result.url) {
        runCatching { Uri.parse(result.url).host?.removePrefix("www.") }.getOrNull()
    }
    val sourceName = result.siteName?.takeIf { it.isNotBlank() } ?: hostName
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_link_failed), Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            if (faviconUrl != null && !faviconLoadFailed) {
                AsyncImage(
                    model = faviconUrl,
                    contentDescription = sourceName,
                    contentScale = ContentScale.Fit,
                    onError = { faviconLoadFailed = true },
                    modifier = Modifier.size(36.dp).padding(3.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(3.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (sourceName != null) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MixedMarkdownLatex(
    text: String,
    textColor: Color,
    isStreaming: Boolean = false,
    compact: Boolean = false,
    onLatexWidthMeasured: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val inlineCodeBackground by rememberUpdatedState(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f).toArgb())
    val inlineCodeText by rememberUpdatedState(MaterialTheme.colorScheme.primary.toArgb())
    val markdownLinkColor = MaterialTheme.colorScheme.primary
    val markdownSelectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    )
    val latestText by rememberUpdatedState(text)
    var renderedText by remember {
        mutableStateOf(if (isStreaming) stabilizeStreamingMarkdown(text) else text)
    }
    LaunchedEffect(text, isStreaming) {
        if (!isStreaming) renderedText = text
    }
    LaunchedEffect(isStreaming) {
        if (!isStreaming) return@LaunchedEffect
        while (true) {
            val stableText = stabilizeStreamingMarkdown(latestText)
            if (stableText != renderedText) renderedText = stableText
            delay(80)
        }
    }
    val segments by remember(renderedText, isStreaming) {
        derivedStateOf { splitContent(renderedText, allowUnclosedCodeFence = isStreaming) }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        segments.forEach { segment ->
            when (segment.type) {
                ContentType.MARKDOWN -> {
                    val markdown = remember(segment.content) {
                        normalizeEscapedEmphasis(segment.content)
                    }
                    key(markdownLinkColor, textColor) {
                    MarkdownText(
                        markdown = markdown,
                        modifier = Modifier.fillMaxWidth(),
                        linkColor = markdownLinkColor,
                        linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                        textSelectionColors = markdownSelectionColors,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        isTextSelectable = true,
                        afterSetMarkdown = { textView ->
                            applyInlineCodeStyle(
                                textView,
                                inlineCodeBackground,
                                inlineCodeText,
                                markdown
                            )
                        }
                    )
                    }
                }
                ContentType.TABLE -> {
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        MarkdownTableView(
                            markdown = segment.content,
                            textColor = textColor
                        )
                    }
                }
                ContentType.CODE_BLOCK -> {
                    CodeBlockView(code = segment.content, info = segment.info)
                }
                ContentType.LATEX_BLOCK -> {
                    LatexBlockImage(
                        latex = segment.content,
                        textColor = textColor,
                        compact = compact,
                        onWidthMeasured = onLatexWidthMeasured
                    )
                }
                ContentType.LATEX_INLINE -> {
                    LatexInlineLine(
                        line = segment.content,
                        textColor = textColor,
                        onWidthMeasured = onLatexWidthMeasured
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun MarkdownTableView(
    markdown: String,
    textColor: Color
) {
    val rows = remember(markdown) {
        markdown.lineSequence()
            .map { it.removeSuffix("\r").trim() }
            .filter { it.isNotBlank() && !isMarkdownTableDelimiter(it) }
            .map { line ->
                line.removePrefix("|")
                    .removeSuffix("|")
                    .split(Regex("(?<!\\\\)\\|"))
                    .map(String::trim)
            }
            .toList()
    }

    if (rows.isEmpty()) return

    val tableShape = RoundedCornerShape(10.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
    val headerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val maxColumnCount = rows.maxOf { it.size }
    val renderedFormulaWidths = remember(markdown) { mutableStateMapOf<Int, Float>() }
    val columnWidths = List(maxColumnCount) { columnIndex ->
        val widestCell = rows.fold(0) { widest, row ->
            val cellText = stripLatexForTableWidth(row.getOrNull(columnIndex).orEmpty())
            val cellWidth = cellText.fold(0) { width, character ->
                width + if (character.code > 0x7F) 14 else 8
            }
            maxOf(widest, cellWidth)
        }
        val estimatedWidth = (widestCell + 28).coerceIn(96, 260).dp
        val renderedWidth = (renderedFormulaWidths[columnIndex] ?: 0f).dp + 28.dp
        maxOf(estimatedWidth, renderedWidth)
    }
    val tableWidth = columnWidths.fold(0.dp) { total, width -> total + width }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .then(if (maxColumnCount == 1) Modifier.fillMaxWidth() else Modifier.width(tableWidth))
                .clip(tableShape)
                .border(BorderStroke(1.dp, borderColor), tableShape)
        ) {
            rows.forEachIndexed { rowIndex, cells ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    cells.forEachIndexed { columnIndex, cell ->
                        Box(
                            modifier = Modifier
                                .then(if (maxColumnCount == 1) Modifier.fillMaxWidth() else Modifier.width(columnWidths[columnIndex]))
                                .fillMaxHeight()
                                .background(if (rowIndex == 0) headerColor else MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(0.5.dp, borderColor))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            MixedMarkdownLatex(
                                text = cell.replace("\\|", "|").let {
                                    if (rowIndex == 0 && !it.startsWith("**")) "**$it**" else it
                                },
                                textColor = textColor,
                                compact = true,
                                onLatexWidthMeasured = { width ->
                                    val previous = renderedFormulaWidths[columnIndex] ?: 0f
                                    if (width > previous) {
                                        renderedFormulaWidths[columnIndex] = width
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun stripLatexForTableWidth(cell: String): String {
    return cell
        .replace(Regex("\\$\\$[\\s\\S]*?\\$\\$"), "")
        .replace(Regex("(?<!\\\\)\\$(?!\\$)[^$]*?\\$"), "")
        .replace(Regex("\\\\\\[[\\s\\S]*?\\\\\\]"), "")
        .replace(Regex("\\\\\\([\\s\\S]*?\\\\\\)"), "")
        .trim()
}

private val winkFrames = listOf(
    R.drawable.ic_wink_0,
    R.drawable.ic_wink_1,
    R.drawable.ic_wink_2,
    R.drawable.ic_wink_3,
    R.drawable.ic_wink_4
)

@Composable
fun StreamingIndicator(
    modifier: Modifier = Modifier,
    startTime: Long? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val softer = lerp(primary, Color.White, 0.3f)
    val actualStart = startTime ?: remember { System.currentTimeMillis() }
    var elapsedText by remember(actualStart) {
        mutableStateOf(formatElapsedTime(System.currentTimeMillis() - actualStart))
    }
    var frameIndex by remember { mutableStateOf(0) }
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }

    LaunchedEffect(actualStart) {
        while (true) {
            delay(100)
            elapsedText = formatElapsedTime(System.currentTimeMillis() - actualStart)
        }
    }

    LaunchedEffect(Unit) {
        val idle = spring<Float>(dampingRatio = 0.45f, stiffness = 110f)
        val bounce = spring<Float>(dampingRatio = 0.35f, stiffness = 320f)
        while (true) {
            scaleY.animateTo(1.05f, idle)
            scaleY.animateTo(1f, idle)
            delay(1100)
            listOf(
                async { scaleY.animateTo(0.86f, bounce) },
                async { scaleX.animateTo(1.10f, bounce) },
                async {
                    for (i in 1..4) {
                        frameIndex = i
                        delay(45)
                    }
                    delay(140)
                }
            ).awaitAll()
            listOf(
                async { scaleY.animateTo(1f, bounce) },
                async { scaleX.animateTo(1f, bounce) },
                async {
                    for (i in 3 downTo 1) {
                        frameIndex = i
                        delay(45)
                    }
                    frameIndex = 0
                }
            ).awaitAll()
            delay(900)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(winkFrames[frameIndex]),
            contentDescription = null,
            colorFilter = ColorFilter.tint(softer),
            modifier = Modifier
                .size(22.dp)
                .scale(scaleX.value, scaleY.value)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.ai_replying),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = softer
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = elapsedText,
            style = MaterialTheme.typography.bodySmall,
            color = softer
        )
    }
}

private fun formatElapsedTime(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000
    val tenths = (elapsedMillis % 1000) / 100
    return if (totalSeconds < 60) {
        "${totalSeconds}.${tenths}s"
    } else {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        "${minutes}m ${seconds}.${tenths}s"
    }
}
