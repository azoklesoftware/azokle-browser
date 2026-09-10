package com.azokle.search.common.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Soft Dark Neutral & Luxury Monochrome Palette (Fallback Constants)
val AzokleBlack = Color(0xFF0D0D0D)
val AzokleDarkBg = Color(0xFF101010)
val AzokleCardBg = Color(0xFF181818)
val AzokleSurface = Color(0xFF181818)
val AzokleSurfaceElevated = Color(0xFF222222)
val AzokleBorder = Color(0xFF282828)
val AzokleBorderLight = Color(0x26FFFFFF)
val AzokleBorderSubtle = Color(0x14FFFFFF)

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
    background = Color(0xFF101010), // Sleek, soft, neutral deep dark canvas (not pitch black, zero brown cast)
    surface = Color(0xFF181818), // Sleek elevated dark card
    surfaceElevated = Color(0xFF222222), // Modern elevated interactive surface
    cardBg = Color(0xFF181818),
    border = Color(0xFF282828), // Crisp, low-contrast neutral dark border
    borderLight = Color(0x26FFFFFF),
    borderSubtle = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF3F3F3), // Soft off-white text (calm, zero glare)
    textSecondary = Color(0xFFA0A0A0), // Balanced neutral secondary
    textMuted = Color(0xFF707070), // Neutral muted
    textDisabled = Color(0xFF424242),
    pillBg = Color(0x22FFFFFF),
    glassBg = Color(0xCC101010),
    bottomNavBg = Color(0xF0181818),
    omnibarBg = Color(0xFF181818),
    modalBg = Color(0xFF141414),
    actionPrimaryBg = Color(0xFFF3F3F3),
    actionPrimaryText = Color(0xFF101010),
    actionSecondaryBg = Color(0xFF222222),
    actionSecondaryText = Color(0xFFF3F3F3),
    statusSuccess = Color(0xFF22C55E),
    statusWarning = Color(0xFFF59E0B),
    statusDestructive = Color(0xFFEF4444),
    statusTor = Color(0xFFA855F7),
    statusAccent = Color(0xFF38BDF8),
    scrim = Color(0xBF000000)
)

val LightAzokleColors = AzokleThemeColors(
    isDark = false,
    background = Color(0xFFFAF8F5), // Soft, warm, eye-friendly ivory white (reduces glare & eye fatigue)
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF2EFEB), // Gentle warm elevated surface
    cardBg = Color(0xFFFFFFFF),
    border = Color(0xFFE8E4DE), // Warm soft stone border
    borderLight = Color(0x1F1C1917),
    borderSubtle = Color(0x0D1C1917),
    textPrimary = Color(0xFF1C1917), // Warm charcoal black (calm on the eyes)
    textSecondary = Color(0xFF57534E), // Warm stone secondary
    textMuted = Color(0xFF8C857E), // Warm muted text
    textDisabled = Color(0xFFC7C2BA),
    pillBg = Color(0x0F1C1917),
    glassBg = Color(0xF2FAF8F5),
    bottomNavBg = Color(0xF5FAF8F5),
    omnibarBg = Color(0xFFFFFFFF),
    modalBg = Color(0xFFFAF8F5),
    actionPrimaryBg = Color(0xFF1C1917),
    actionPrimaryText = Color(0xFFFAF8F5),
    actionSecondaryBg = Color(0xFFF2EFEB),
    actionSecondaryText = Color(0xFF1C1917),
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
