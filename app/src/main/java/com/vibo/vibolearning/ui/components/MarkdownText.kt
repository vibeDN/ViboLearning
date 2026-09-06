package com.vibo.vibolearning.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.vibo.vibolearning.ui.theme.Vli

/** Renders a small subset of inline markdown: **bold**, *italic* / _italic_, `code`. */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Vli.palette.text,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
) {
    val codeColor = Vli.palette.accent.copy(alpha = 0.9f)
    val annotated = remember(text) { parseInlineMarkdown(text, codeColor) }
    Text(text = annotated, modifier = modifier, color = color, fontSize = fontSize, lineHeight = fontSize * 1.5f)
}

private fun parseInlineMarkdown(src: String, codeColor: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < src.length) {
        val c = src[i]
        when {
            c == '*' && src.startsWith("**", i) -> {
                val end = src.indexOf("**", i + 2)
                if (end != -1) {
                    withStyleAppend(SpanStyle(fontWeight = FontWeight.SemiBold), src.substring(i + 2, end))
                    i = end + 2
                } else { append(c); i++ }
            }
            (c == '*' || c == '_') -> {
                val end = src.indexOf(c, i + 1)
                if (end != -1 && end > i + 1) {
                    withStyleAppend(SpanStyle(fontStyle = FontStyle.Italic), src.substring(i + 1, end))
                    i = end + 1
                } else { append(c); i++ }
            }
            c == '`' -> {
                val end = src.indexOf('`', i + 1)
                if (end != -1) {
                    withStyleAppend(SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor), src.substring(i + 1, end))
                    i = end + 1
                } else { append(c); i++ }
            }
            else -> { append(c); i++ }
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.withStyleAppend(style: SpanStyle, text: String) {
    pushStyle(style)
    append(text)
    pop()
}
