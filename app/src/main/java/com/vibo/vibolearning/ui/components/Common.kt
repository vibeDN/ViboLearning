package com.vibo.vibolearning.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibo.vibolearning.ui.theme.Vli

/** Nocturne primary: an accent outline on transparent, never a fill. */
@Composable
fun VliButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
) {
    val p = Vli.palette
    val container = when {
        !enabled -> Color.Transparent
        filled -> p.accentSoft
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .border(1.5.dp, if (enabled) p.accent else p.lockedBorder, RoundedCornerShape(10.dp))
            .let { if (enabled) it.clickableNoRipple(onClick) else it }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) p.accent else p.textMuted,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Vli.palette
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, p.divider, RoundedCornerShape(10.dp))
            .clickableNoRipple(onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.textMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

enum class SegState { Done, Current, Todo }

@Composable
fun SegmentedProgressBar(segments: List<SegState>, modifier: Modifier = Modifier) {
    val p = Vli.palette
    Row(
        modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { seg ->
            val color = when (seg) {
                SegState.Done -> p.accent
                SegState.Current -> p.accent.copy(alpha = 0.55f)
                SegState.Todo -> p.hairline
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

@Composable
fun CodeBlock(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
) {
    val p = Vli.palette
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(p.codeBg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = p.text,
        )
    }
}

@Composable
fun Kicker(text: String) {
    Text(
        text.uppercase(),
        color = Vli.palette.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
    )
}

@Composable
fun RowScope.LinearBar(fraction: Float, color: Color, track: Color, height: Int = 4) {
    Box(
        Modifier
            .weight(1f)
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(track),
    ) {
        val animated by animateFloatAsState(
            targetValue = fraction.coerceIn(0f, 1f),
            animationSpec = tween(600, easing = LinearEasing),
            label = "bar",
        )
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp))
                .background(color),
        )
    }
}
