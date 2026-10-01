package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Palette
val NeonCyan = Color(0xFF00F5D4)
val ElectricViolet = Color(0xFF7928CA)
val NeonPink = Color(0xFFFF0080)
val CyberBlue = Color(0xFF00B4D8)
val LosslessGold = Color(0xFFFFB703)
val EmeraldHifi = Color(0xFF10B981)

// Dark Theme Canvases
val MidnightBlack = Color(0xFF090A10)
val PureAmoled = Color(0xFF000000)
val SurfaceDark = Color(0xFF121524)
val SurfaceElevated = Color(0xFF181D33)

// Glassmorphism Tints (Translucent)
val GlassSurface = Color(0xB3151A2E) // 70% opacity
val GlassCard = Color(0x8C1B223D)    // 55% opacity
val GlassCardLight = Color(0x33FFFFFF)
val GlassBorder = Color(0x38FFFFFF)
val GlassBorderCyan = Color(0x6600F5D4)

// Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val GlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x3300F5D4),
        Color(0x1A7928CA),
        Color(0x0A090A10)
    )
)

val NeonWaveGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, ElectricViolet, NeonPink)
)

val HifiBadgeGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0D9488), Color(0xFF00F5D4))
)
