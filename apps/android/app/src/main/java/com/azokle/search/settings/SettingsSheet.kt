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
