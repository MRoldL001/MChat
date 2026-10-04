package com.mroldl001.mimochat.ui.chat.components

import com.mroldl001.mimochat.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.*
import com.mroldl001.mimochat.ui.theme.LocalCodeBlockDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CodeBlockView(code: String, info: String?) {
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

// 浅色用 primary 6% 叠白而非纯白：跟主题联动，又不压过语法高亮
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
