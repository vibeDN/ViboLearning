package com.vibo.vibolearning.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Tap handler without the Material ripple — the design themes its own states. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
