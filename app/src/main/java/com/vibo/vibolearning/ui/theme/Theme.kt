package com.vibo.vibolearning.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Ported from the "Nocturne" design system (de-branded): a quiet, compact dark
 * interface — near-neutral blue-grey ground, one accent used as a line and a
 * glow rather than a flood. The accent is per-course; everything else is fixed.
 */
data class VliPalette(
    val bg: Color = Color(0xFF161826),
    val surface: Color = Color(0xFF232532),
    val surfaceRaised: Color = Color(0xFF2B2D3B),
    val text: Color = Color(0xFFE9E9ED),
    val textMuted: Color = Color(0xFF9397AB),   // neutral-500
    val divider: Color = Color(0x29E9E9ED),      // text @ 16%
    val hairline: Color = Color(0xFF3F424D),     // neutral-800
    val locked: Color = Color(0xFF3F424D),
    val lockedBorder: Color = Color(0xFF595D6C), // neutral-700
    val codeBg: Color = Color(0xFF292B31),       // neutral-900
    val accent: Color = Color(0xFF9184D9),
    val onAccent: Color = Color(0xFF161826),
    val danger: Color = Color(0xFFE5484D),
) {
    val accentSoft: Color get() = accent.copy(alpha = 0.22f)
    val dangerSoft: Color get() = danger.copy(alpha = 0.20f)
}

val LocalVliPalette = staticCompositionLocalOf { VliPalette() }

/** Convenience accessor: `Vli.palette.accent`. */
object Vli {
    val palette: VliPalette
        @Composable get() = LocalVliPalette.current
}

fun parseAccent(hex: String?): Color? {
    val h = hex?.trim()?.removePrefix("#") ?: return null
    return runCatching {
        when (h.length) {
            6 -> Color(("FF$h").toLong(16))
            8 -> Color(h.toLong(16))
            else -> null
        }
    }.getOrNull()
}

private val Inter = FontFamily.SansSerif

private val VliTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

@Composable
fun VliTheme(
    accentHex: String? = null,
    content: @Composable () -> Unit,
) {
    // Dark-only design regardless of the OS light/dark setting.
    val palette = remember(accentHex) {
        val accent = parseAccent(accentHex) ?: VliPalette().accent
        VliPalette(accent = accent)
    }

    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.bg,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.surfaceRaised,
        onSurfaceVariant = palette.textMuted,
        error = palette.danger,
        outline = palette.hairline,
    )

    CompositionLocalProvider(LocalVliPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = VliTypography, content = content)
    }
}
