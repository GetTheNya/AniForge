package moe.GetTheNya.AniForge.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Dark Palette (Midnight Slate)
val BackgroundDark = Color(0xFF0C0C0E)
val SurfaceDark = Color(0xFF16161C)
val SurfaceCardDark = Color(0x3B20202A) // Glassmorphic translucent card base
val AlertBackground = Color(0xD90E0D12)

// Accents (Vibrant HSL-derived colors)
val NeonCoral = Color(0xFFFF2E93)
val ElectricViolet = Color(0xFF8A2BE2)
val CyberTeal = Color(0xFF00F5D4)

// Neutral Text & Outlines
val TextPrimary = Color(0xFFF3F3F7)
val TextSecondary = Color(0xFFA1A1B2)
val CardBorder = Color(0x22FFFFFF) // Translucent border for glassmorphism
val TransparentAccent = Color(0x11FF2E93)

// Centralized Rating / Scoring Color System
fun getScoreColor(score: Double): Color {
    return when {
        score >= 8.0 -> Color(0xFF10B981) // Green (Emerald)
        score >= 5.0 -> Color(0xFFF59E0B) // Orange (Amber)
        else -> Color(0xFFEF4444) // Red (Vibrant Red)
    }
}

fun getScoreBadgeColors(score: Double): Pair<Color, Color> {
    return when {
        score >= 8.0 -> Pair(Color(0xFF10B981), Color(0xFF34D399)) // Green (Emerald & Mint)
        score >= 5.0 -> Pair(Color(0xFFF59E0B), Color(0xFFFBBF24)) // Orange (Amber & Light Orange)
        else -> Pair(Color(0xFFEF4444), Color(0xFFF87171)) // Red (Vibrant Red & Coral)
    }
}