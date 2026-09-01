package com.azokle.search.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Obsidian & Luxury Monochrome Palette (Fallback Constants)
val AzokleBlack = Color(0xFF000000)
val AzokleDarkBg = Color(0xFF0A0A0A)
val AzokleCardBg = Color(0xFF141414)
val AzokleSurface = Color(0xFF1A1A1A)
val AzokleSurfaceElevated = Color(0xFF222222)
val AzokleBorder = Color(0xFF262626)
val AzokleBorderLight = Color(0x33FFFFFF)
val AzokleBorderSubtle = Color(0x1FFFFFFF)

// Text Colors (Dark Fallbacks)
val AzokleTextPrimary = Color(0xFFFFFFFF)
val AzokleTextSecondary = Color(0xFFA3A3A3)
val AzokleTextMuted = Color(0xFF737373)
val AzokleTextDisabled = Color(0xFF525252)

// Accent & Badges
val AzokleWhite = Color(0xFFFFFFFF)
val AzokleGlassBg = Color(0xCC0D0D0D)
val AzoklePillBg = Color(0x2BFFFFFF)
val AzokleProGold = Color(0xFFE5E5E5)
val AzokleSuccessGreen = Color(0xFF22C55E)

// Comprehensive Dynamic Theme Color Container
data class AzokleThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBg: Color,
    val border: Color,
    val borderLight: Color,
    val borderSubtle: Color = if (isDark) Color(0x1FFFFFFF) else Color(0x0F000000),
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color = if (isDark) Color(0xFF525252) else Color(0xFF94A3B8),
    val pillBg: Color,
    val glassBg: Color,
    val bottomNavBg: Color,
    val omnibarBg: Color,
    val modalBg: Color,
    val actionPrimaryBg: Color,
    val actionPrimaryText: Color,
    val actionSecondaryBg: Color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF1F5F9),
    val actionSecondaryText: Color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A),
    val statusSuccess: Color = if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A),
    val statusWarning: Color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
    val statusDestructive: Color = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
    val statusTor: Color = if (isDark) Color(0xFFA855F7) else Color(0xFF9333EA),
    val statusAccent: Color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
    val scrim: Color = if (isDark) Color(0xBF000000) else Color(0x66000000)
)

val DarkAzokleColors = AzokleThemeColors(
    isDark = true,
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141414),
    surfaceElevated = Color(0xFF1C1C1E),
    cardBg = Color(0xFF141414),
    border = Color(0xFF262626),
    borderLight = Color(0x33FFFFFF),
    borderSubtle = Color(0x1FFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    textDisabled = Color(0xFF3F3F46),
    pillBg = Color(0x2BFFFFFF),
    glassBg = Color(0xCC0D0D0D),
    bottomNavBg = Color(0xF0141414),
    omnibarBg = Color(0xFF121212),
    modalBg = Color(0xFF121214),
    actionPrimaryBg = Color(0xFFFFFFFF),
    actionPrimaryText = Color(0xFF000000),
    actionSecondaryBg = Color(0xFF1C1C1E),
    actionSecondaryText = Color(0xFFFFFFFF),
    statusSuccess = Color(0xFF22C55E),
    statusWarning = Color(0xFFF59E0B),
    statusDestructive = Color(0xFFEF4444),
    statusTor = Color(0xFFA855F7),
    statusAccent = Color(0xFF38BDF8),
    scrim = Color(0xBF000000)
)

val LightAzokleColors = AzokleThemeColors(
    isDark = false,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    cardBg = Color(0xFFFFFFFF),
    border = Color(0xFFE2E8F0),
    borderLight = Color(0x2B000000),
    borderSubtle = Color(0x0F000000),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    textDisabled = Color(0xFFCBD5E1),
    pillBg = Color(0x14000000),
    glassBg = Color(0xF2FFFFFF),
    bottomNavBg = Color(0xF5FFFFFF),
    omnibarBg = Color(0xFFFFFFFF),
    modalBg = Color(0xFFFFFFFF),
    actionPrimaryBg = Color(0xFF0F172A),
    actionPrimaryText = Color(0xFFFFFFFF),
    actionSecondaryBg = Color(0xFFF1F5F9),
    actionSecondaryText = Color(0xFF0F172A),
    statusSuccess = Color(0xFF16A34A),
    statusWarning = Color(0xFFD97706),
    statusDestructive = Color(0xFFDC2626),
    statusTor = Color(0xFF9333EA),
    statusAccent = Color(0xFF0284C7),
    scrim = Color(0x66000000)
)

val LocalAzokleColors = compositionLocalOf { DarkAzokleColors }

object AzokleTheme {
    val colors: AzokleThemeColors
        @Composable
        get() = LocalAzokleColors.current
}

object AzokleRadius {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(24.dp)
    val full = CircleShape
}
