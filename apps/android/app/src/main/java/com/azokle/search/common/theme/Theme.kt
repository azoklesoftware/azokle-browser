/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.common.theme

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
import com.azokle.search.common.util.findActivity

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

/**
 * Composable that renders the azokle search theme UI component.
 */
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
        androidx.compose.runtime.DisposableEffect(isDark) {
            val activity = view.context.findActivity()
            if (activity is com.azokle.search.MainActivity) {
                activity.updateSystemBars(isDark)
            } else if (activity != null) {
                val window = activity.window
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
            onDispose {}
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
