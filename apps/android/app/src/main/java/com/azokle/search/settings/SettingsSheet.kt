package com.azokle.search.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.azokle.search.settings.SettingsScreen

/**
 * Compatibility wrapper for [SettingsScreen].
 * The bottom-sheet approach has been migrated to a dedicated full-screen Settings screen.
 */
@Composable
fun SettingsSheet(
    selectedThemeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    selectedDohProvider: String = "cloudflare",
    onDohProviderChange: (String) -> Unit = {},
    selectedSearchEngine: String = "azokle",
    onOpenSearchEngine: () -> Unit = {},
    selectedLanguageCode: String = "system",
    onOpenLanguage: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onReplayTour: (() -> Unit)? = null,
    onReplayHomeTour: (() -> Unit)? = null,
    onReplayTabsTour: (() -> Unit)? = null,
    onBurnData: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        selectedThemeMode = selectedThemeMode,
        onThemeModeChange = onThemeModeChange,
        selectedDohProvider = selectedDohProvider,
        onDohProviderChange = onDohProviderChange,
        selectedSearchEngine = selectedSearchEngine,
        onOpenSearchEngine = onOpenSearchEngine,
        selectedLanguageCode = selectedLanguageCode,
        onOpenLanguage = onOpenLanguage,
        onDismiss = onDismiss,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenTerms = onOpenTerms,
        onOpenAbout = onOpenAbout,
        onOpenUrl = onOpenUrl,
        onReplayTour = onReplayTour,
        onReplayHomeTour = onReplayHomeTour,
        onReplayTabsTour = onReplayTabsTour,
        onBurnData = onBurnData,
        modifier = modifier
    )
}
