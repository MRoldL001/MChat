package com.mroldl001.mimochat.ui.chat.components

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Region
import android.graphics.RegionIterator
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.LineBackgroundSpan
import android.text.style.MetricAffectingSpan
import android.widget.TextView
import io.noties.markwon.core.spans.CodeSpan
import kotlin.math.max
import kotlin.math.min

internal fun applyInlineCodeStyle(textView: TextView, backgroundColor: Int, textColor: Int) {
    val rendered = textView.text as? Spanned ?: return
    val codeSpans = rendered.getSpans(0, rendered.length, CodeSpan::class.java)
    if (codeSpans.isEmpty()) return
    val styled = (rendered as? Spannable) ?: SpannableString(rendered)
    codeSpans.forEach { span ->
        val start = styled.getSpanStart(span)
        val end = styled.getSpanEnd(span)
        val flags = styled.getSpanFlags(span)
        styled.removeSpan(span)
        if (start < end) {
            styled.setSpan(
                RoundedInlineCodeSpan(textView, start, end, backgroundColor, textColor),
                start, end, flags
            )
        }
    }
    if (styled !== rendered) {
        textView.text = styled
    } else {
        textView.invalidate()
    }
}

/** Keep real text spans so long code can wrap and selection/copy retains the original text. */
private class RoundedInlineCodeSpan(
    private val textView: TextView,
    private val codeStart: Int,
    private val codeEnd: Int,
    backgroundColor: Int,
    private val textColor: Int
) : MetricAffectingSpan(), LineBackgroundSpan {
    private val density = textView.resources.displayMetrics.density
    private val radius = 4f * density
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    private val selectionPath = Path()
    private val selectionRegion = Region()
    private val clipRegion = Region()
    private val bounds = Rect()

    override fun updateMeasureState(paint: TextPaint) {
        paint.typeface = Typeface.MONOSPACE
        paint.textSize *= 0.95f
    }

    override fun updateDrawState(paint: TextPaint) {
        updateMeasureState(paint)
        paint.color = textColor
        paint.bgColor = Color.TRANSPARENT
    }

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        val from = max(start, codeStart)
        val to = min(end, codeEnd)
        if (from >= to) return
        val layout = textView.layout ?: return
        // Layout resolves wrapping, nested styles and bidirectional text; measuring raw strings cannot.
        selectionPath.reset()
        layout.getSelectionPath(from, to, selectionPath)
        clipRegion.set(left, top, right, bottom)
        selectionRegion.setPath(selectionPath, clipRegion)
        val rectangles = RegionIterator(selectionRegion)
        while (rectangles.next(bounds)) {
            canvas.drawRoundRect(
                bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.bottom.toFloat(),
                radius, radius, backgroundPaint
            )
        }
    }
}
