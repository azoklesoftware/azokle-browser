package com.azokle.search.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkAzokleColors.actionPrimaryBg,
    onPrimary = DarkAzokleColors.actionPrimaryText,
    primaryContainer = DarkAzokleColors.surfaceElevated,
    onPrimaryContainer = DarkAzokleColors.textPrimary,
    secondary = DarkAzokleColors.textSecondary,
    onSecondary = DarkAzokleColors.actionPrimaryText,
    background = DarkAzokleColors.background,
    onBackground = DarkAzokleColors.textPrimary,
    surface = DarkAzokleColors.cardBg,
    onSurface = DarkAzokleColors.textPrimary,
    surfaceVariant = DarkAzokleColors.surface,
    onSurfaceVariant = DarkAzokleColors.textSecondary,
    outline = DarkAzokleColors.border
)

private val LightColorScheme = lightColorScheme(
    primary = LightAzokleColors.actionPrimaryBg,
    onPrimary = LightAzokleColors.actionPrimaryText,
    primaryContainer = LightAzokleColors.surfaceElevated,
    onPrimaryContainer = LightAzokleColors.textPrimary,
    secondary = LightAzokleColors.textSecondary,
    onSecondary = LightAzokleColors.actionPrimaryText,
    background = LightAzokleColors.background,
    onBackground = LightAzokleColors.textPrimary,
    surface = LightAzokleColors.cardBg,
    onSurface = LightAzokleColors.textPrimary,
    surfaceVariant = LightAzokleColors.surface,
    onSurfaceVariant = LightAzokleColors.textSecondary,
    outline = LightAzokleColors.border
)

@Composable
fun AzokleSearchTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val themeColors = if (isDark) DarkAzokleColors else LightAzokleColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = themeColors.background.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = themeColors.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalAzokleColors provides themeColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
