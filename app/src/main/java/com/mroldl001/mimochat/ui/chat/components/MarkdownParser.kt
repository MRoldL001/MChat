package com.mroldl001.mimochat.ui.chat.components


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
internal fun normalizeEscapedEmphasis(markdown: String): String {
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

private fun isMarkdownTableRow(line: String): Boolean =
    line.isNotBlank() && line.count { it == '|' } >= 1

internal fun isMarkdownTableDelimiter(line: String): Boolean {
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

internal fun stabilizeStreamingMarkdown(text: String): String {
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
