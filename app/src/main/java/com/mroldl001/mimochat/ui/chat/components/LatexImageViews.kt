package com.mroldl001.mimochat.ui.chat.components

import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.roundToInt

private const val LatexTextSizeSp = 18f

private data class InlineFormula(
    val token: String,
    val latex: String
)

@Composable
internal fun LatexBlockImage(
    latex: String,
    textColor: Color,
    compact: Boolean = false,
    onWidthMeasured: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textSizePx = with(density) { LatexTextSizeSp.sp.toPx() }
    val result by produceState<LatexRenderResult?>(null, latex, textColor, textSizePx) {
        value = LatexBitmapRenderer.render(
            context = context,
            latex = latex,
            textColor = textColor.toArgb(),
            textSizePx = textSizePx,
            displayMode = true
        )
    }
    LaunchedEffect(result) {
        result?.let {
            onWidthMeasured?.invoke(with(density) { it.bitmap.width.toDp().value })
        }
    }
    val blockVerticalPadding = if (compact) {
        0.dp
    } else {
        result?.let {
            with(density) {
                (it.bitmap.height * 0.12f).toDp().coerceIn(8.dp, 18.dp)
            }
        } ?: 10.dp
    }

    if (result == null) {
        Box(
            modifier = modifier
                .padding(vertical = blockVerticalPadding)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = latex,
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
        return
    }
    val image = result!!.bitmap
    Box(
        modifier = modifier
            .padding(vertical = blockVerticalPadding)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = latex,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(with(density) { image.width.toDp() })
                .height(with(density) { image.height.toDp() })
        )
    }
}

@Composable
internal fun LatexInlineLine(
    line: String,
    textColor: Color,
    onWidthMeasured: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textSizePx = with(density) { 15.sp.toPx() }
    val latexTextSizePx = with(density) { LatexTextSizeSp.sp.toPx() }
    val colorArgb = textColor.toArgb()
    val tokenized = remember(line) { tokenizeInlineLatex(line) }
    val results by produceState<List<LatexRenderResult?>>(
        initialValue = List(tokenized.second.size) { null },
        tokenized.first,
        latexTextSizePx,
        colorArgb
    ) {
        value = tokenized.second.map { formula ->
            LatexBitmapRenderer.render(
                context = context,
                latex = formula.latex,
                textColor = colorArgb,
                textSizePx = latexTextSizePx,
                displayMode = false
            )
        }
    }
    LaunchedEffect(results) {
        val widest = results.filterNotNull().maxOfOrNull { it.bitmap.width } ?: 0
        if (widest > 0) {
            onWidthMeasured?.invoke(with(density) { widest.toDp().value })
        }
    }
    val markwon = remember(context) { Markwon.create(context) }
    val minInlineVerticalPaddingPx = with(density) { 2.dp.roundToPx() }
    val spanned = remember(tokenized, results, colorArgb, minInlineVerticalPaddingPx) {
        val builder = SpannableStringBuilder(markwon.toMarkdown(tokenized.first))
        tokenized.second.forEachIndexed { index, formula ->
            val result = results.getOrNull(index)
            val start = builder.indexOf(formula.token)
            if (start < 0) return@forEachIndexed
            if (result != null) {
                builder.replace(start, start + formula.token.length, formula.latex)
                builder.setSpan(
                    LatexReplacementSpan(result, minInlineVerticalPaddingPx),
                    start,
                    start + formula.latex.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                builder.replace(start, start + formula.token.length, formula.latex)
            }
        }
        builder
    }

    AndroidView(
        factory = { viewContext ->
            TextView(viewContext).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(colorArgb)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                setLineSpacing(0f, 1f)
                gravity = Gravity.START
                movementMethod = LinkMovementMethod.getInstance()
                setTextIsSelectable(true)
                includeFontPadding = false
            }
        },
        update = { textView ->
            textView.setTextColor(colorArgb)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            textView.text = spanned
        },
        modifier = modifier.fillMaxWidth()
    )
}

private fun tokenizeInlineLatex(line: String): Pair<String, List<InlineFormula>> {
    val output = StringBuilder(line.length)
    val formulas = mutableListOf<InlineFormula>()
    var cursor = 0
    while (cursor < line.length) {
        if (line[cursor] == '`') {
            val ticks = line.substring(cursor).takeWhile { it == '`' }.length
            val delimiter = "`".repeat(ticks)
            val closing = line.indexOf(delimiter, cursor + ticks)
            val end = if (closing >= 0) closing + ticks else line.length
            output.append(line, cursor, end)
            cursor = end
            continue
        }
        val delimiter = when {
            line.startsWith("\\(", cursor) && !isEscapedLatex(line, cursor) -> "\\)" to "\\("
            line[cursor] == '$' && !line.startsWith("$$", cursor) && !isEscapedLatex(line, cursor) -> "$" to "$"
            else -> null
        }
        if (delimiter == null) {
            output.append(line[cursor++])
            continue
        }
        val contentStart = cursor + delimiter.second.length
        val closing = findLatexDelimiter(line, delimiter.first, contentStart)
        if (closing < 0) {
            output.append(line[cursor++])
            continue
        }
        val token = "LTXINLINE${formulas.size}TOKEN"
        formulas += InlineFormula(token, line.substring(contentStart, closing).trim())
        output.append(token)
        cursor = closing + delimiter.first.length
    }
    return output.toString() to formulas
}

private fun findLatexDelimiter(text: String, delimiter: String, start: Int): Int {
    var cursor = start
    while (cursor <= text.length - delimiter.length) {
        val index = text.indexOf(delimiter, cursor)
        if (index < 0) return -1
        if (!isEscapedLatex(text, index)) return index
        cursor = index + delimiter.length
    }
    return -1
}

private fun isEscapedLatex(text: String, index: Int): Boolean {
    var slashCount = 0
    var cursor = index - 1
    while (cursor >= 0 && text[cursor] == '\\') {
        slashCount++
        cursor--
    }
    return slashCount % 2 != 0
}

private class LatexReplacementSpan(
    private val result: LatexRenderResult,
    private val minVerticalPaddingPx: Int
) : ReplacementSpan() {
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        fm?.let { metrics ->
            // Keep a small, height-aware gap around tall formulas so the
            // following line never sits directly against the bitmap.
            val verticalPadding = (result.bitmap.height * 0.06f)
                .roundToInt()
                .coerceAtLeast(minVerticalPaddingPx)
            val ascent = -(result.baselinePx + verticalPadding)
            val descent = result.depthPx + verticalPadding
            metrics.ascent = minOf(metrics.ascent, ascent)
            metrics.top = minOf(metrics.top, ascent)
            metrics.descent = maxOf(metrics.descent, descent)
            metrics.bottom = maxOf(metrics.bottom, descent)
        }
        return result.bitmap.width
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        canvas.drawBitmap(
            result.bitmap,
            x,
            (y - result.baselinePx).toFloat(),
            paint
        )
    }
}
