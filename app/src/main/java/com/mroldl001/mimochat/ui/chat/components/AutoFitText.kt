package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
internal fun AutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    minFontSize: TextUnit = 10.sp,
    softWrap: Boolean = false,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val resolvedBaseSp = when {
        style.fontSize != TextUnit.Unspecified -> style.fontSize.value
        LocalTextStyle.current.fontSize != TextUnit.Unspecified -> LocalTextStyle.current.fontSize.value
        else -> 14f
    }
    val minSp = minFontSize.value

    var fontSizeSp by remember(text, minFontSize, style.fontSize) {
        mutableStateOf(resolvedBaseSp)
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(
            fontSize = fontSizeSp.sp,
            textAlign = textAlign ?: style.textAlign
        ),
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        onTextLayout = { result ->
            if (result.hasVisualOverflow) {
                val next = fontSizeSp * 0.9f
                fontSizeSp = if (next >= minSp) next else minSp
            }
        }
    )
}
