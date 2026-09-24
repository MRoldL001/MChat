package com.mroldl001.mimochat.ui.chat.components
import com.mroldl001.mimochat.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.*
import com.mroldl001.mimochat.ui.theme.LocalCodeBlockDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.mroldl001.mimochat.domain.model.WebSearchResult
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay

enum class ContentType {
    MARKDOWN,
    TABLE,
    CODE_BLOCK,
    INLINE_CODE,
    LATEX_INLINE,
    LATEX_BLOCK
}

data class ContentSegment(
    val type: ContentType,
    val content: String,
    val info: String? = null
)

private data class MarkdownFence(val marker: Char, val length: Int, val info: String?)

private data class InlineMathPart(val content: String, val isMath: Boolean)

private fun markdownFenceAt(line: String): MarkdownFence? {
    val indent = line.takeWhile { it == ' ' }.length
    if (indent > 3) return null
    val fenceLine = line.drop(indent)
    if (fenceLine.isEmpty()) return null
    val marker = fenceLine.first()
    if (marker != '`' && marker != '~') return null
    val length = fenceLine.takeWhile { it == marker }.length
    if (length < 3) return null
    val info = fenceLine.drop(length).trim().takeIf { it.isNotEmpty() }
    if (marker == '`' && info?.contains('`') == true) return null
    return MarkdownFence(marker, length, info)
}

private fun isClosingFence(line: String, fence: MarkdownFence): Boolean {
    val indent = line.takeWhile { it == ' ' }.length
    if (indent > 3) return false
    val fenceLine = line.drop(indent)
    if (fenceLine.firstOrNull() != fence.marker) return false
    val length = fenceLine.takeWhile { it == fence.marker }.length
    return length >= fence.length && fenceLine.drop(length).isBlank()
}

private fun lineEnd(text: String, start: Int): Int =
    text.indexOf('\n', start).takeIf { it >= 0 } ?: text.length

private fun nextLineStart(text: String, end: Int): Int =
    if (end < text.length) end + 1 else end

private fun withoutFenceLineBreak(content: String): String = when {
    content.endsWith("\r\n") -> content.dropLast(2)
    content.endsWith('\n') || content.endsWith('\r') -> content.dropLast(1)
    else -> content
}

private fun findUnescaped(text: String, target: String, start: Int): Int {
    var index = start
    while (index <= text.length - target.length) {
        val found = text.indexOf(target, index)
        if (found < 0) return -1
        var slashCount = 0
        var cursor = found - 1
        while (cursor >= 0 && text[cursor] == '\\') {
            slashCount++
            cursor--
        }
        if (slashCount % 2 == 0) return found
        index = found + target.length
    }
    return -1
}

private fun isEscaped(text: String, index: Int): Boolean {
    var slashCount = 0
    var cursor = index - 1
    while (cursor >= 0 && text[cursor] == '\\') {
        slashCount++
        cursor--
    }
    return slashCount % 2 != 0
}

private fun parseInlineMath(line: String): List<InlineMathPart>? {
    val parts = mutableListOf<InlineMathPart>()
    var plainStart = 0
    var cursor = 0
    var foundMath = false

    fun appendText(end: Int) {
        if (end > plainStart) parts += InlineMathPart(line.substring(plainStart, end), false)
    }

    while (cursor < line.length) {
        if (line[cursor] == '`') {
            val ticks = line.substring(cursor).takeWhile { it == '`' }.length
            val closing = line.indexOf("`".repeat(ticks), cursor + ticks)
            cursor = if (closing >= 0) closing + ticks else line.length
            continue
        }

        val delimiter = when {
            line.startsWith("\\(", cursor) && !isEscaped(line, cursor) -> "\\(" to "\\)"
            line[cursor] == '$' && !line.startsWith("$$", cursor) && !isEscaped(line, cursor) -> "$" to "$"
            else -> null
        }
        if (delimiter == null) {
            cursor++
            continue
        }

        val closing = findUnescaped(line, delimiter.second, cursor + delimiter.first.length)
        if (closing < 0) {
            cursor += delimiter.first.length
            continue
        }
        appendText(cursor)
        parts += InlineMathPart(
            line.substring(cursor + delimiter.first.length, closing),
            true
        )
        foundMath = true
        cursor = closing + delimiter.second.length
        plainStart = cursor
    }
    appendText(line.length)
    return parts.takeIf { foundMath }
}

/**
 * Some model responses escape Markdown emphasis delimiters (for example
 * `\\*\\*重点\\*\\*`). Treat paired escaped delimiters as emphasis while leaving
 * inline code untouched.
 */
private fun normalizeEscapedEmphasis(markdown: String): String {
    val output = StringBuilder(markdown.length)
    var cursor = 0
    while (cursor < markdown.length) {
        if (markdown[cursor] == '`') {
            val ticks = markdown.substring(cursor).takeWhile { it == '`' }.length
            val delimiter = "`".repeat(ticks)
            val closing = markdown.indexOf(delimiter, cursor + ticks)
            val end = if (closing >= 0) closing + ticks else markdown.length
            output.append(markdown, cursor, end)
            cursor = end
            continue
        }
        when {
            markdown.startsWith("\\*\\*", cursor) -> {
                output.append("**")
                cursor += 4
            }
            markdown.startsWith("\\_\\_", cursor) -> {
                output.append("__")
                cursor += 4
            }
            else -> output.append(markdown[cursor++])
        }
    }
    return output.toString()
}

fun splitContent(text: String, allowUnclosedCodeFence: Boolean = false): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    var plainStart = 0
    var cursor = 0

    fun appendMarkdown(end: Int) {
        if (end <= plainStart) return
        val markdown = text.substring(plainStart, end)
        var lineStart = 0
        var index = 0
        while (index <= markdown.length) {
            val lineEnd = markdown.indexOf('\n', index).takeIf { it >= 0 } ?: markdown.length
            val line = markdown.substring(index, lineEnd).removeSuffix("\r")
            if (parseInlineMath(line) != null) {
                if (index > lineStart) {
                    segments += ContentSegment(ContentType.MARKDOWN, markdown.substring(lineStart, index))
                }
                segments += ContentSegment(ContentType.LATEX_INLINE, line)
                lineStart = if (lineEnd < markdown.length) lineEnd + 1 else lineEnd
            }
            if (lineEnd >= markdown.length) break
            index = lineEnd + 1
        }
        if (lineStart < markdown.length) {
            segments += ContentSegment(ContentType.MARKDOWN, markdown.substring(lineStart))
        }
    }

    while (cursor < text.length) {
        val openingEnd = lineEnd(text, cursor)
        val openingLine = text.substring(cursor, openingEnd).removeSuffix("\r")

        // Render tables separately so inline LaTeX inside a cell does not
        // turn the entire pipe-delimited row into one LaTeX block.
        val tableDelimiterEnd = nextLineStart(text, openingEnd)
            .takeIf { it < text.length }
            ?.let { lineEnd(text, it) }
        if (
            isMarkdownTableRow(openingLine) &&
            tableDelimiterEnd != null &&
            isMarkdownTableDelimiter(text.substring(nextLineStart(text, openingEnd), tableDelimiterEnd))
        ) {
            appendMarkdown(cursor)
            var tableEnd = nextLineStart(text, tableDelimiterEnd)
            while (tableEnd < text.length) {
                val rowEnd = lineEnd(text, tableEnd)
                if (!isMarkdownTableRow(text.substring(tableEnd, rowEnd).removeSuffix("\r"))) break
                tableEnd = nextLineStart(text, rowEnd)
            }
            segments += ContentSegment(ContentType.TABLE, text.substring(cursor, tableEnd))
            cursor = tableEnd
            plainStart = cursor
            continue
        }

        val fence = markdownFenceAt(openingLine)
        if (fence != null) {
            val contentStart = nextLineStart(text, openingEnd)
            var closingStart = -1
            var closingEnd = -1
            var search = contentStart
            while (search < text.length) {
                val candidateEnd = lineEnd(text, search)
                val candidate = text.substring(search, candidateEnd).removeSuffix("\r")
                if (isClosingFence(candidate, fence)) {
                    closingStart = search
                    closingEnd = candidateEnd
                    break
                }
                search = nextLineStart(text, candidateEnd)
            }

            if (closingStart >= 0 || allowUnclosedCodeFence) {
                appendMarkdown(cursor)
                val contentEnd = if (closingStart >= 0) closingStart else text.length
                val code = withoutFenceLineBreak(text.substring(contentStart, contentEnd))
                segments.add(ContentSegment(ContentType.CODE_BLOCK, code, fence.info))
                cursor = if (closingStart >= 0) nextLineStart(text, closingEnd) else text.length
                plainStart = cursor
                continue
            }
        }

        val trimmedOpeningLine = openingLine.trim()
        val blockClosingDelimiter = when (trimmedOpeningLine) {
            "\$\$" -> "\$\$"
            "\\[" -> "\\]"
            else -> null
        }
        if (blockClosingDelimiter != null) {
            val contentStart = nextLineStart(text, openingEnd)
            var closingStart = -1
            var closingEnd = -1
            var search = contentStart
            while (search < text.length) {
                val candidateEnd = lineEnd(text, search)
                if (text.substring(search, candidateEnd).removeSuffix("\r").trim() == blockClosingDelimiter) {
                    closingStart = search
                    closingEnd = candidateEnd
                    break
                }
                search = nextLineStart(text, candidateEnd)
            }
            if (closingStart >= 0) {
                appendMarkdown(cursor)
                val latex = withoutFenceLineBreak(text.substring(contentStart, closingStart))
                segments.add(ContentSegment(ContentType.LATEX_BLOCK, latex))
                cursor = nextLineStart(text, closingEnd)
                plainStart = cursor
                continue
            } else if (allowUnclosedCodeFence) {
                appendMarkdown(cursor)
                cursor = text.length
                plainStart = cursor
                continue
            }
        } else if (trimmedOpeningLine.startsWith("\$\$") && trimmedOpeningLine.endsWith("\$\$") && trimmedOpeningLine.length > 4) {
            appendMarkdown(cursor)
            segments.add(ContentSegment(ContentType.LATEX_BLOCK, trimmedOpeningLine.substring(2, trimmedOpeningLine.length - 2)))
            cursor = nextLineStart(text, openingEnd)
            plainStart = cursor
            continue
        } else if (trimmedOpeningLine.startsWith("\\[") && trimmedOpeningLine.endsWith("\\]") && trimmedOpeningLine.length > 4) {
            appendMarkdown(cursor)
            segments.add(ContentSegment(ContentType.LATEX_BLOCK, trimmedOpeningLine.substring(2, trimmedOpeningLine.length - 2)))
            cursor = nextLineStart(text, openingEnd)
            plainStart = cursor
            continue
        }

        cursor = nextLineStart(text, openingEnd)
    }

    appendMarkdown(text.length)
    return segments
}

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

private fun isMarkdownTableRow(line: String): Boolean =
    line.isNotBlank() && line.count { it == '|' } >= 1

private fun isMarkdownTableDelimiter(line: String): Boolean {
    val cells = line.trim().trim('|').split('|').map { it.trim() }
    if (cells.size < 2) return false
    return cells.all { it.matches(Regex("^:?-{3,}:?\$")) }
}

private fun markdownLineStart(text: String, lineIndex: Int): Int {
    var offset = 0
    repeat(lineIndex) {
        val end = text.indexOf('\n', offset)
        if (end < 0) return text.length
        offset = end + 1
    }
    return offset
}

private fun unclosedMathStart(line: String): Int? {
    var cursor = 0
    while (cursor < line.length) {
        if (line[cursor] == '`') {
            val ticks = line.substring(cursor).takeWhile { it == '`' }.length
            val closing = line.indexOf("`".repeat(ticks), cursor + ticks)
            if (closing < 0) return null
            cursor = closing + ticks
            continue
        }
        val delimiter = when {
            line.startsWith("$$", cursor) && !isEscaped(line, cursor) -> "$$" to "$$"
            line.startsWith("\\[", cursor) && !isEscaped(line, cursor) -> "\\[" to "\\]"
            line.startsWith("\\(", cursor) && !isEscaped(line, cursor) -> "\\(" to "\\)"
            line[cursor] == '$' && !isEscaped(line, cursor) -> "$" to "$"
            else -> null
        }
        if (delimiter == null) {
            cursor++
            continue
        }
        val closing = findUnescaped(line, delimiter.second, cursor + delimiter.first.length)
        if (closing < 0) return cursor
        cursor = closing + delimiter.second.length
    }
    return null
}

private fun hasUnclosedCodeFence(text: String): Boolean {
    var activeFence: MarkdownFence? = null
    text.lineSequence().forEach { rawLine ->
        val line = rawLine.removeSuffix("\r")
        val currentFence = activeFence
        if (currentFence == null) {
            markdownFenceAt(line)?.let { activeFence = it }
        } else if (isClosingFence(line, currentFence)) {
            activeFence = null
        }
    }
    return activeFence != null
}

private fun stabilizeStreamingMarkdown(text: String): String {
    val lastLineBreak = text.lastIndexOf('\n')
    val partialLineStart = lastLineBreak + 1
    val partialLine = text.substring(partialLineStart).removeSuffix("\r")
    if (!hasUnclosedCodeFence(text)) {
        unclosedMathStart(partialLine)?.let { mathStart ->
            return text.substring(0, partialLineStart + mathStart)
        }
    }
    if (lastLineBreak < 0) return text

    val completedLines = text.substring(0, lastLineBreak)
        .split('\n')
        .map { it.removeSuffix("\r") }
    val delimiterIndex = completedLines.indexOfLast(::isMarkdownTableDelimiter)

    if (delimiterIndex > 0) {
        val headerIndex = delimiterIndex - 1
        val completedRows = completedLines.drop(delimiterIndex + 1)
        val tableStillOpen =
            isMarkdownTableRow(completedLines[headerIndex]) &&
                completedRows.all(::isMarkdownTableRow) &&
                (partialLine.isEmpty() || isMarkdownTableRow(partialLine))
        if (tableStillOpen) {
            return text.substring(0, markdownLineStart(text, headerIndex))
        }
    }

    val lastCompletedLine = completedLines.lastOrNull().orEmpty()
    if (
        isMarkdownTableRow(lastCompletedLine) &&
        partialLine.contains('|') &&
        partialLine.contains('-')
    ) {
        val headerStart = text.lastIndexOf('\n', lastLineBreak - 1).let { it + 1 }
        return text.substring(0, headerStart)
    }

    return text
}

@Composable
fun StreamingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun CodeBlockView(code: String, info: String?) {
    val context = LocalContext.current
    val darkCodeBlock = LocalCodeBlockDark.current
    val palette = codeBlockPalette(darkCodeBlock)
    val lines = code.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    val fenceLabel = info?.trim()?.substringBefore(' ').orEmpty()
    val normalizedLanguage = normalizeCodeLanguage(fenceLabel)
    val language = normalizedLanguage.takeIf { it in supportedCodeLanguages }
    val codeLines = lines
    
    val lineCount = codeLines.size
    val maxLineNumberWidth = lineCount.toString().length
    val highlightedLines = remember(codeLines, language, darkCodeBlock) {
        highlightCodeLines(codeLines, language, darkCodeBlock)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .background(
                color = palette.background,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            fenceLabel.takeIf { it.isNotEmpty() }?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    codeLanguageIcon(normalizedLanguage)?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.muted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = fenceLabel,
                        style = TextStyle(
                            color = palette.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
             
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("code", codeLines.joinToString("\n"))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.toast_code_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_code),
                    tint = palette.muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            // 行号列：不参与横向滚动，始终冻结在左侧
            Column {
                highlightedLines.forEachIndexed { index, _ ->
                    Text(
                        text = (index + 1).toString().padStart(maxLineNumberWidth, ' '),
                        modifier = Modifier.widthIn(min = (maxLineNumberWidth * 10).dp),
                        style = TextStyle(
                            color = palette.muted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 代码列：只有这部分横向滚动，行号不受影响
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                highlightedLines.forEach { line ->
                    Text(
                        text = if (line.isEmpty()) AnnotatedString(" ") else line,
                        style = TextStyle(
                            color = palette.text,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
    }
}

private fun codeLanguageIcon(language: String?): ImageVector? {
    if (language == null) return Icons.Default.Code
    return when (language) {
        "cpp", "go", "rust" -> Icons.Default.Code
        else -> codeLanguageVectorIcon(language) ?: when (language) {
            "java" -> Icons.Default.Coffee
            "csharp" -> Icons.Default.Code
            "powershell" -> Icons.Default.Terminal
            "sql" -> Icons.Default.Storage
            "scss" -> Icons.Default.Css
            else -> Icons.Default.Code
        }
    }
}

private val supportedCodeLanguages = setOf(
    "python", "javascript", "typescript", "java", "kotlin", "c", "cpp", "csharp",
    "go", "rust", "swift", "php", "ruby", "shell", "powershell", "sql", "dart",
    "html", "xml", "css", "scss", "json", "yaml", "toml", "lua", "r", "text"
)

private fun normalizeCodeLanguage(label: String): String {
    return when (label.lowercase().substringBefore(' ').trim()) {
        "py", "python3" -> "python"
        "js", "jsx", "node" -> "javascript"
        "ts", "tsx" -> "typescript"
        "kt", "kts" -> "kotlin"
        "c++", "cc", "cxx" -> "cpp"
        "c#", "cs" -> "csharp"
        "golang" -> "go"
        "rs" -> "rust"
        "sh", "zsh" -> "shell"
        "bash", "pwsh", "ps1" -> "powershell"
        "htm" -> "html"
        "yml" -> "yaml"
        "pl" -> "perl"
        "hs" -> "haskell"
        "ex", "exs" -> "elixir"
        "erl" -> "erlang"
        "clj", "cljs" -> "clojure"
        "jl" -> "julia"
        "sol" -> "solidity"
        "gql" -> "graphql"
        "f90", "f95", "f03" -> "fortran"
        "cr" -> "crystal"
        "ml", "mli" -> "ocaml"
        "fs", "fsi", "fsx" -> "fsharp"
        "rkt" -> "racket"
        "lisp", "cl" -> "commonlisp"
        "adb", "ads" -> "ada"
        "purs" -> "purescript"
        "re", "rei" -> "reason"
        "res", "resi" -> "rescript"
        "hx" -> "haxe"
        "ipynb" -> "jupyter"
        "wl", "nb" -> "wolframmathematica"
        "tex" -> "latex"
        "md" -> "markdown"
        "dockerfile" -> "docker"
        ".env", "dotenv" -> "env"
        "tf", "tfvars" -> "terraform"
        "el" -> "gnuemacs"
        "mysql" -> "mysql"
        "postgres", "postgresql", "psql" -> "postgresql"
        "plaintext", "txt" -> "text"
        else -> label.lowercase().substringBefore(' ').trim()
    }
}

private data class CodeHighlightPalette(
    val keyword: Color = Color(0xFFC792EA),
    val string: Color = Color(0xFFC3E88D),
    val number: Color = Color(0xFFF78C6C),
    val comment: Color = Color(0xFF7C8495),
    val type: Color = Color(0xFFFFCB6B),
    val function: Color = Color(0xFF82AAFF),
    val operator: Color = Color(0xFF89DDFF),
    val annotation: Color = Color(0xFFFF5370),
    val property: Color = Color(0xFFF07178)
)

private val codeKeywords = setOf(
    "as", "async", "await", "break", "case", "catch", "class", "const", "continue",
    "def", "default", "do", "else", "enum", "export", "extends", "false", "final",
    "finally", "for", "from", "fun", "function", "if", "implements", "import", "in",
    "interface", "internal", "is", "lambda", "let", "match", "module", "new", "nil",
    "none", "null", "object", "open", "override", "package", "pass", "private",
    "protected", "public", "raise", "readonly", "return", "sealed", "static", "struct",
    "super", "switch", "this", "throw", "throws", "trait", "true", "try", "typealias",
    "typeof", "using", "val", "var", "virtual", "void", "when", "where", "while", "with",
    "yield", "select", "insert", "update", "delete", "create", "drop", "alter", "join",
    "into", "values", "and", "or", "not", "then", "end", "local", "func", "defer", "go"
)

private val codeTypes = setOf(
    "any", "bool", "boolean", "byte", "char", "decimal", "double", "dynamic", "float",
    "int", "integer", "long", "never", "number", "object", "short", "string", "uint",
    "ulong", "ushort", "unit", "unknown", "list", "map", "set", "dict", "tuple", "array"
)

/** 代码块配色：深色黑底 / 浅色白底，语法高亮两套配色 */
internal data class CodeBlockPalette(
    val background: Color,
    val text: Color,
    val muted: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val function: Color,
    val operator: Color,
    val annotation: Color,
    val property: Color
)

internal val DarkCodeBlockPalette = CodeBlockPalette(
    background = Color.Black,
    text = Color.White,
    muted = Color.Gray,
    keyword = Color(0xFFC792EA),
    string = Color(0xFFC3E88D),
    number = Color(0xFFF78C6C),
    comment = Color(0xFF7C8495),
    type = Color(0xFFFFCB6B),
    function = Color(0xFF82AAFF),
    operator = Color(0xFF89DDFF),
    annotation = Color(0xFFFF5370),
    property = Color(0xFFF07178)
)

internal val LightCodeBlockPalette = CodeBlockPalette(
    background = Color.White,
    text = Color(0xFF1F2328),
    muted = Color(0xFF6B7280),
    keyword = Color(0xFFCF222E),
    string = Color(0xFF0A3069),
    number = Color(0xFF0550AE),
    comment = Color(0xFF6E7781),
    type = Color(0xFF953800),
    function = Color(0xFF8250DF),
    operator = Color(0xFF0550AE),
    annotation = Color(0xFF953800),
    property = Color(0xFF116329)
)

private fun CodeHighlightPalette(dark: Boolean = true): CodeHighlightPalette {
    val source = if (dark) DarkCodeBlockPalette else LightCodeBlockPalette
    return CodeHighlightPalette(
        keyword = source.keyword,
        string = source.string,
        number = source.number,
        comment = source.comment,
        type = source.type,
        function = source.function,
        operator = source.operator,
        annotation = source.annotation,
        property = source.property
    )
}

/**
 * 按明暗取代码块配色。浅色背景用极浅主题色（primary 6% 叠白）而非纯白，
 * 与主题联动、在浅色页面上有区分度，同时不压过语法高亮。
 */
@Composable
internal fun codeBlockPalette(dark: Boolean): CodeBlockPalette {
    return if (dark) {
        DarkCodeBlockPalette
    } else {
        LightCodeBlockPalette.copy(
            background = MaterialTheme.colorScheme.primary
                .copy(alpha = 0.06f)
                .compositeOver(Color.White)
        )
    }
}

private fun highlightCodeLines(
    lines: List<String>,
    language: String?,
    dark: Boolean = true
): List<AnnotatedString> {
    val palette = CodeHighlightPalette(dark)
    var inBlockComment = false
    var multiLineStringDelimiter: String? = null
    val lineCommentMarkers = when (language) {
        "python", "ruby", "shell", "powershell", "yaml", "r" -> listOf("#")
        "sql", "lua" -> listOf("--")
        "text" -> emptyList()
        else -> listOf("//")
    }
    val blockMarkers = when (language) {
        "html", "xml" -> "<!--" to "-->"
        "python", "ruby", "shell", "powershell", "yaml", "r", "text" -> null
        else -> "/*" to "*/"
    }

    return lines.map { line ->
        buildAnnotatedString {
            var index = 0
            while (index < line.length) {
                val activeDelimiter = multiLineStringDelimiter
                if (activeDelimiter != null) {
                    val end = line.indexOf(activeDelimiter, index)
                    val tokenEnd = if (end >= 0) end + activeDelimiter.length else line.length
                    withStyle(SpanStyle(color = palette.string)) {
                        append(line.substring(index, tokenEnd))
                    }
                    index = tokenEnd
                    if (end >= 0) multiLineStringDelimiter = null
                    continue
                }

                if (inBlockComment && blockMarkers != null) {
                    val end = line.indexOf(blockMarkers.second, index)
                    val tokenEnd = if (end >= 0) end + blockMarkers.second.length else line.length
                    withStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)) {
                        append(line.substring(index, tokenEnd))
                    }
                    index = tokenEnd
                    if (end >= 0) inBlockComment = false
                    continue
                }

                val blockStart = blockMarkers?.first
                if (blockStart != null && line.startsWith(blockStart, index)) {
                    val end = line.indexOf(blockMarkers.second, index + blockStart.length)
                    val tokenEnd = if (end >= 0) end + blockMarkers.second.length else line.length
                    withStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)) {
                        append(line.substring(index, tokenEnd))
                    }
                    index = tokenEnd
                    if (end < 0) inBlockComment = true
                    continue
                }

                val commentMarker = lineCommentMarkers.firstOrNull { line.startsWith(it, index) }
                if (commentMarker != null) {
                    withStyle(SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)) {
                        append(line.substring(index))
                    }
                    index = line.length
                    continue
                }

                val tripleDelimiter = when {
                    language == "python" && line.startsWith("\"\"\"", index) -> "\"\"\""
                    language == "python" && line.startsWith("'''", index) -> "'''"
                    else -> null
                }
                if (tripleDelimiter != null) {
                    val end = line.indexOf(tripleDelimiter, index + tripleDelimiter.length)
                    val tokenEnd = if (end >= 0) end + tripleDelimiter.length else line.length
                    withStyle(SpanStyle(color = palette.string)) {
                        append(line.substring(index, tokenEnd))
                    }
                    index = tokenEnd
                    if (end < 0) multiLineStringDelimiter = tripleDelimiter
                    continue
                }

                val current = line[index]
                if (current == '\"' || current == '\'' || current == '`') {
                    var end = index + 1
                    var escaped = false
                    while (end < line.length) {
                        val character = line[end]
                        if (!escaped && character == current) {
                            end++
                            break
                        }
                        escaped = !escaped && character == '\\'
                        if (character != '\\') escaped = false
                        end++
                    }
                    val isProperty = line.substring(end).trimStart().startsWith(":")
                    withStyle(SpanStyle(color = if (isProperty) palette.property else palette.string)) {
                        append(line.substring(index, end))
                    }
                    index = end
                    continue
                }

                if (current.isDigit()) {
                    var end = index + 1
                    while (end < line.length && (line[end].isLetterOrDigit() || line[end] in "._")) end++
                    withStyle(SpanStyle(color = palette.number)) { append(line.substring(index, end)) }
                    index = end
                    continue
                }

                if (current.isLetter() || current == '_' || current == '$') {
                    var end = index + 1
                    while (end < line.length && (line[end].isLetterOrDigit() || line[end] == '_' || line[end] == '$')) end++
                    val word = line.substring(index, end)
                    val normalizedWord = word.lowercase()
                    val nextNonWhitespace = line.drop(end).firstOrNull { !it.isWhitespace() }
                    val previousNonWhitespace = line.take(index).lastOrNull { !it.isWhitespace() }
                    val style = when {
                        normalizedWord in codeKeywords -> SpanStyle(color = palette.keyword, fontWeight = FontWeight.SemiBold)
                        normalizedWord in codeTypes || word.firstOrNull()?.isUpperCase() == true -> SpanStyle(color = palette.type)
                        previousNonWhitespace == '@' -> SpanStyle(color = palette.annotation)
                        nextNonWhitespace == '(' -> SpanStyle(color = palette.function)
                        (language == "html" || language == "xml") &&
                            (previousNonWhitespace == '<' || previousNonWhitespace == '/') -> SpanStyle(color = palette.function)
                        else -> null
                    }
                    if (style != null) withStyle(style) { append(word) } else append(word)
                    index = end
                    continue
                }

                if (current in "=+-*/%<>!&|^~?:.@") {
                    withStyle(SpanStyle(color = palette.operator)) { append(current) }
                } else {
                    append(current)
                }
                index++
            }
        }
    }
}
