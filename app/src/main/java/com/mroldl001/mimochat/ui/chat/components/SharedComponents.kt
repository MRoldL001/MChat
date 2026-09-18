package com.mroldl001.mimochat.ui.chat.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import kotlin.math.roundToInt
import com.mroldl001.mimochat.domain.model.WebSearchResult
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay

enum class ContentType {
    MARKDOWN,
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

private fun markdownFenceAt(line: String): MarkdownFence? {
    if (line.isEmpty()) return null
    val marker = line.first()
    if (marker != '`' && marker != '~') return null
    val length = line.takeWhile { it == marker }.length
    if (length < 3) return null
    val info = line.drop(length).trim().takeIf { it.isNotEmpty() }
    if (marker == '`' && info?.contains('`') == true) return null
    return MarkdownFence(marker, length, info)
}

private fun isClosingFence(line: String, fence: MarkdownFence): Boolean {
    if (line.firstOrNull() != fence.marker) return false
    val length = line.takeWhile { it == fence.marker }.length
    return length >= fence.length && line.drop(length).isBlank()
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

fun splitContent(text: String, allowUnclosedCodeFence: Boolean = false): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    var plainStart = 0
    var cursor = 0

    fun appendMarkdown(end: Int) {
        if (end > plainStart) {
            segments.add(ContentSegment(ContentType.MARKDOWN, text.substring(plainStart, end)))
        }
    }

    while (cursor < text.length) {
        val openingEnd = lineEnd(text, cursor)
        val openingLine = text.substring(cursor, openingEnd).removeSuffix("\r")
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

        if (openingLine == "\$\$") {
            val contentStart = nextLineStart(text, openingEnd)
            var closingStart = -1
            var closingEnd = -1
            var search = contentStart
            while (search < text.length) {
                val candidateEnd = lineEnd(text, search)
                if (text.substring(search, candidateEnd).removeSuffix("\r") == "\$\$") {
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
            }
        } else if (openingLine.startsWith("\$\$") && openingLine.endsWith("\$\$") && openingLine.length > 4) {
            appendMarkdown(cursor)
            segments.add(ContentSegment(ContentType.LATEX_BLOCK, openingLine.substring(2, openingLine.length - 2)))
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
                    text = "思考过程",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
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
                    text = "搜索结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "收起" else "展开",
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
                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
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
                    key(markdownLinkColor, textColor) {
                    MarkdownText(
                        markdown = segment.content,
                        modifier = Modifier.fillMaxWidth(),
                        linkColor = markdownLinkColor,
                        textSelectionColors = markdownSelectionColors,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        isTextSelectable = true,
                        afterSetMarkdown = { textView ->
                            applyInlineCodeStyle(textView, inlineCodeBackground, inlineCodeText)
                        }
                    )
                    }
                }
                ContentType.CODE_BLOCK -> {
                    CodeBlockView(code = segment.content, info = segment.info)
                }
                ContentType.LATEX_BLOCK -> {
                    LatexView(
                        latex = segment.content,
                        textColor = textColor,
                        isBlock = true
                    )
                }
                else -> {}
            }
        }
    }
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

private fun stabilizeStreamingMarkdown(text: String): String {
    val lastLineBreak = text.lastIndexOf('\n')
    if (lastLineBreak < 0) return text

    val completedLines = text.substring(0, lastLineBreak)
        .split('\n')
        .map { it.removeSuffix("\r") }
    val partialLine = text.substring(lastLineBreak + 1).removeSuffix("\r")
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
    val lines = code.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    val fenceLabel = info?.trim()?.substringBefore(' ').orEmpty()
    val language = normalizeCodeLanguage(fenceLabel).takeIf { it in supportedCodeLanguages }
    val codeLines = lines
    
    val lineCount = codeLines.size
    val maxLineNumberWidth = lineCount.toString().length
    val highlightedLines = remember(codeLines, language) {
        highlightCodeLines(codeLines, language)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = Color.Black,
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
                Text(
                    text = fenceLabel,
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
             
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("code", codeLines.joinToString("\n"))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "代码已复制", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制代码",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            highlightedLines.forEachIndexed { index, line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (index + 1).toString().padStart(maxLineNumberWidth, ' '),
                        modifier = Modifier.widthIn(min = (maxLineNumberWidth * 10).dp),
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = if (line.isEmpty()) AnnotatedString(" ") else line,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
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
        "sh", "bash", "zsh" -> "shell"
        "ps1" -> "powershell"
        "htm" -> "html"
        "yml" -> "yaml"
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

private fun highlightCodeLines(
    lines: List<String>,
    language: String?
): List<AnnotatedString> {
    val palette = CodeHighlightPalette()
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

@Composable
private fun LatexView(
    latex: String,
    textColor: Color,
    isBlock: Boolean
) {
    val hexColor = textColor.toHexString()
    val htmlContent = remember(latex, hexColor, isBlock) {
        buildKaTeXHtml(latex, hexColor, isBlock)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    setSupportZoom(false)
                    displayZoomControls = false
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://cdn.jsdelivr.net/npm/katex@0.16.44/dist/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = if (isBlock) {
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        } else {
            Modifier
                .wrapContentSize()
                .padding(vertical = 2.dp)
        }
    )
}

private fun buildKaTeXHtml(latex: String, textColor: String, isBlock: Boolean): String {
    val displayMode = if (isBlock) "true" else "false"
    val escapedLatex = latex
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", " ")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.44/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.44/dist/katex.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    display: flex;
                    ${if (isBlock) "justify-content: center;" else "justify-content: flex-start;"}
                    align-items: center;
                    min-height: ${if (isBlock) "40px" else "24px"};
                    background: transparent;
                }
                #math {
                    color: $textColor;
                }
                .katex { color: $textColor !important; }
            </style>
        </head>
        <body>
            <div id="math"></div>
            <script>
                try {
                    katex.render('$escapedLatex', document.getElementById('math'), {
                        throwOnError: false,
                        displayMode: $displayMode,
                        color: '$textColor'
                    });
                } catch (e) {
                    document.getElementById('math').textContent = '$escapedLatex';
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun Color.toHexString(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
