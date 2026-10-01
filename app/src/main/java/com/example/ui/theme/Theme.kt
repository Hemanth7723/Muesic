package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    AUTO, AMOLED_BLACK, DEEP_GLASS, LIGHT, TITANIUM_GRAY
}

@Immutable
data class AppThemeColors(
    val background: Color,
    val backgroundGradientTop: Color,
    val backgroundGradientBottom: Color,
    val surfaceGlass: Color,
    val cardGlass: Color,
    val cardBorder: Color = Color(0x38FFFFFF),
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color = Color(0xFF64748B),
    val accent: Color = NeonCyan,
    val accentSecondary: Color = ElectricViolet,
    val pillBackground: Color = Color(0x1FFFFFFF),
    val pillBackgroundSelected: Color = Color(0x3300F5D4),
    val pillBorder: Color = Color(0x33FFFFFF),
    val pillBorderSelected: Color = NeonCyan,
    val divider: Color = Color(0x1AFFFFFF),
    val isLight: Boolean = false
) {
    val backgroundPrimary: Color get() = background
    val backgroundSecondary: Color get() = backgroundGradientTop
    val surfaceElevated: Color get() = surfaceGlass
}

val LocalAppThemeColors = staticCompositionLocalOf {
    AppThemeColors(
        background = MidnightBlack,
        backgroundGradientTop = Color(0xFF101324),
        backgroundGradientBottom = MidnightBlack,
        surfaceGlass = GlassSurface,
        cardGlass = GlassCard,
        cardBorder = GlassBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted,
        accent = NeonCyan,
        accentSecondary = ElectricViolet,
        pillBackground = Color(0x1FFFFFFF),
        pillBackgroundSelected = Color(0x3300F5D4),
        pillBorder = Color(0x33FFFFFF),
        pillBorderSelected = NeonCyan,
        divider = Color(0x1AFFFFFF),
        isLight = false
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF003831),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF70F7E0),
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0E80),
    onSecondaryContainer = Color(0xFFE8B4FF),
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = MidnightBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary
)

private val AmoledColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = ElectricViolet,
    onSecondary = Color.White,
    tertiary = NeonPink,
    background = PureAmoled,
    onBackground = TextPrimary,
    surface = PureAmoled,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2F1),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF3B0764),
    tertiary = Color(0xFFD81B60),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF334155)
)

private val TitaniumGrayColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF1A202C),
    secondary = Color(0xFF90CDF4),
    onSecondary = Color(0xFF1A202C),
    tertiary = NeonPink,
    background = Color(0xFF1E2430),
    onBackground = Color(0xFFEDF2F7),
    surface = Color(0xFF252D3D),
    onSurface = Color(0xFFEDF2F7),
    surfaceVariant = Color(0xFF2D3748),
    onSurfaceVariant = Color(0xFFA0AEC0)
)

@Composable
fun MuesicTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED_BLACK -> AmoledColorScheme
        ThemeMode.DEEP_GLASS -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.TITANIUM_GRAY -> TitaniumGrayColorScheme
        ThemeMode.AUTO -> if (systemDark) DarkColorScheme else LightColorScheme
    }

    val appThemeColors = when (themeMode) {
        ThemeMode.AMOLED_BLACK -> AppThemeColors(
            background = PureAmoled,
            backgroundGradientTop = PureAmoled,
            backgroundGradientBottom = PureAmoled,
            surfaceGlass = Color(0xEB0A0A0A),
            cardGlass = Color(0xFF101014),
            cardBorder = Color(0x2EFFFFFF),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B),
            accent = NeonCyan,
            accentSecondary = ElectricViolet,
            pillBackground = Color(0x1AFFFFFF),
            pillBackgroundSelected = Color(0x3300F5D4),
            pillBorder = Color(0x2EFFFFFF),
            pillBorderSelected = NeonCyan,
            divider = Color(0x1AFFFFFF),
            isLight = false
        )
        ThemeMode.DEEP_GLASS -> AppThemeColors(
            background = MidnightBlack,
            backgroundGradientTop = Color(0xFF101324),
            backgroundGradientBottom = MidnightBlack,
            surfaceGlass = GlassSurface,
            cardGlass = GlassCard,
            cardBorder = GlassBorder,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted,
            accent = NeonCyan,
            accentSecondary = ElectricViolet,
            pillBackground = Color(0x1FFFFFFF),
            pillBackgroundSelected = Color(0x3300F5D4),
            pillBorder = Color(0x33FFFFFF),
            pillBorderSelected = NeonCyan,
            divider = Color(0x1AFFFFFF),
            isLight = false
        )
        ThemeMode.LIGHT -> AppThemeColors(
            background = Color(0xFFF1F5F9),
            backgroundGradientTop = Color(0xFFE2E8F0),
            backgroundGradientBottom = Color(0xFFF1F5F9),
            surfaceGlass = Color(0xF8FFFFFF),
            cardGlass = Color(0xFFFFFFFF),
            cardBorder = Color(0x220F172A),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF334155),
            textMuted = Color(0xFF64748B),
            accent = Color(0xFF00796B),
            accentSecondary = Color(0xFF6D28D9),
            pillBackground = Color(0x0F0F172A),
            pillBackgroundSelected = Color(0x2400796B),
            pillBorder = Color(0x260F172A),
            pillBorderSelected = Color(0xFF00796B),
            divider = Color(0x140F172A),
            isLight = true
        )
        ThemeMode.TITANIUM_GRAY -> AppThemeColors(
            background = Color(0xFF1A1F2C),
            backgroundGradientTop = Color(0xFF242B3B),
            backgroundGradientBottom = Color(0xFF161A24),
            surfaceGlass = Color(0xD9222837),
            cardGlass = Color(0xBF293245),
            cardBorder = Color(0x2EFFFFFF),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFFA0AEC0),
            textMuted = Color(0xFF718096),
            accent = NeonCyan,
            accentSecondary = Color(0xFF90CDF4),
            pillBackground = Color(0x1EFFFFFF),
            pillBackgroundSelected = Color(0x3300F5D4),
            pillBorder = Color(0x2EFFFFFF),
            pillBorderSelected = NeonCyan,
            divider = Color(0x1AFFFFFF),
            isLight = false
        )
        ThemeMode.AUTO -> if (systemDark) {
            AppThemeColors(
                background = MidnightBlack,
                backgroundGradientTop = Color(0xFF101324),
                backgroundGradientBottom = MidnightBlack,
                surfaceGlass = GlassSurface,
                cardGlass = GlassCard,
                cardBorder = GlassBorder,
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textMuted = TextMuted,
                accent = NeonCyan,
                accentSecondary = ElectricViolet,
                pillBackground = Color(0x1FFFFFFF),
                pillBackgroundSelected = Color(0x3300F5D4),
                pillBorder = Color(0x33FFFFFF),
                pillBorderSelected = NeonCyan,
                divider = Color(0x1AFFFFFF),
                isLight = false
            )
        } else {
            AppThemeColors(
                background = Color(0xFFF1F5F9),
                backgroundGradientTop = Color(0xFFE2E8F0),
                backgroundGradientBottom = Color(0xFFF1F5F9),
                surfaceGlass = Color(0xF8FFFFFF),
                cardGlass = Color(0xFFFFFFFF),
                cardBorder = Color(0x220F172A),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF334155),
                textMuted = Color(0xFF64748B),
                accent = Color(0xFF00796B),
                accentSecondary = Color(0xFF6D28D9),
                pillBackground = Color(0x0F0F172A),
                pillBackgroundSelected = Color(0x2400796B),
                pillBorder = Color(0x260F172A),
                pillBorderSelected = Color(0xFF00796B),
                divider = Color(0x140F172A),
                isLight = true
            )
        }
    }

    CompositionLocalProvider(LocalAppThemeColors provides appThemeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) = MuesicTheme(content = content)
