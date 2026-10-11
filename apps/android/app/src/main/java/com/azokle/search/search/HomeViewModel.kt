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

package com.azokle.search.search

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azokle.search.tabs.TabManager
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.discover.SearchItem
import com.azokle.search.common.util.SearchLocation
import com.azokle.search.discover.SearchRepository
import com.azokle.search.common.util.UrlHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchCategory(val id: String, val label: String, @param:androidx.annotation.StringRes val labelRes: Int) {
    WEB("web", "Web", com.azokle.search.R.string.cat_web),
    IMAGES("images", "Images", com.azokle.search.R.string.cat_images),
    VIDEOS("videos", "Videos", com.azokle.search.R.string.cat_videos),
    NEWS("news", "News", com.azokle.search.R.string.cat_news)
}

data class HomeUiState(
    val searchQuery: String = "",
    val activeCategory: SearchCategory = SearchCategory.WEB,
    val suggestions: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val isSearchExpanded: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isSearchEngineSheetOpen: Boolean = false,
    val isLanguageSheetOpen: Boolean = false,
    val isPrivacyModalOpen: Boolean = false,
    val isTermsModalOpen: Boolean = false,
    val isAboutModalOpen: Boolean = false,
    val isVoiceModalOpen: Boolean = false,
    val isQrScannerOpen: Boolean = false,
    val selectedLocation: SearchLocation = PreferencesManager.DEFAULT_LOCATIONS[0],
    val selectedSafeSearch: String = "Moderate",
    val selectedThemeMode: String = "system",
    val selectedBrowserEngine: String = "gecko",
    val selectedDohProvider: String = "cloudflare",
    val selectedSearchEngine: String = "azokle",
    val selectedLanguage: String = "system",
    val placeholderText: String = "Search the web privately...",
    val isOnboardingVisible: Boolean = false,

    // Search & In-Built Browser State
    val inAppBrowserUrl: String? = null,
    val isResultsScreenVisible: Boolean = false,
    val searchResults: List<SearchItem> = emptyList(),
    val isSearchingResults: Boolean = false,
    val searchTotalCount: Long = 0,
    val searchTimeSeconds: Double = 0.0,
    val isInPipMode: Boolean = false,

)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SearchRepository()
    private val preferences = PreferencesManager.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Updates the pip mode.
     */
    fun setPipMode(inPip: Boolean) {
        _uiState.update { it.copy(isInPipMode = inPip) }
    }

    /**
     * Updates the voice modal open.
     */
    fun setVoiceModalOpen(open: Boolean) {
        _uiState.update { it.copy(isVoiceModalOpen = open) }
    }

    /**
     * Updates the qr scanner open.
     */
    fun setQrScannerOpen(open: Boolean) {
        _uiState.update { it.copy(isQrScannerOpen = open) }
    }

    private var debounceJob: Job? = null
    private var typewriterJob: Job? = null

    private val placeholders = listOf(
        "Search the web privately...",
        "Search without tracking or logs...",
        "Search images, videos & news...",
        "Search anything in the universe..."
    )

    init {
        val savedSafe = preferences.getSafeSearch()
        val savedTheme = preferences.getThemeMode()
        val savedEngine = preferences.getBrowserEngine()
        val savedDoh = preferences.getDohProvider()
        val savedSearchEngine = preferences.getSearchEngine()
        val savedLanguage = preferences.getAppLanguage()
        val savedLocName = preferences.getLocation()
        val hasCompletedOnboarding = preferences.hasCompletedOnboarding()
        val matchedLoc = PreferencesManager.DEFAULT_LOCATIONS.find { it.name == savedLocName }
            ?: PreferencesManager.DEFAULT_LOCATIONS[0]

        _uiState.update {
            it.copy(
                recentSearches = preferences.getRecentSearches(),
                selectedSafeSearch = savedSafe,
                selectedThemeMode = savedTheme,
                selectedBrowserEngine = savedEngine,
                selectedDohProvider = savedDoh,
                selectedSearchEngine = savedSearchEngine,
                selectedLanguage = savedLanguage,
                selectedLocation = matchedLoc,
                placeholderText = "Search web or type URL",
                isOnboardingVisible = !hasCompletedOnboarding
            )
        }
    }

    /**
     * Handles on query change events.
     */
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }

        debounceJob?.cancel()
        val trimmed = newQuery.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(suggestions = emptyList(), isLoadingSuggestions = false) }
            return
        }

        // Instant local match from encrypted recent searches (< 1ms)
        val localMatches = _uiState.value.recentSearches.filter {
            it.contains(trimmed, ignoreCase = true) && !it.equals(trimmed, ignoreCase = true)
        }
        if (localMatches.isNotEmpty()) {
            _uiState.update { it.copy(suggestions = localMatches) }
        }

        debounceJob = viewModelScope.launch {
            delay(120)
            _uiState.update { it.copy(isLoadingSuggestions = true) }
            try {
                val results = repository.getAutosuggestions(trimmed)
                val combined = (localMatches + results).distinct().take(8)
                _uiState.update {
                    it.copy(
                        suggestions = combined,
                        isLoadingSuggestions = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSuggestions = false) }
            }
        }
    }

    /**
     * Executes the select category operation.
     */
    fun selectCategory(category: SearchCategory) {
        _uiState.update { it.copy(activeCategory = category) }
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            executeSearch(currentQuery, category)
        }
    }

    /**
     * Executes the execute search operation.
     */
    fun executeSearch(query: String, category: SearchCategory = _uiState.value.activeCategory) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        val safeParam = when (_uiState.value.selectedSafeSearch.lowercase()) {
            "strict" -> "2"
            "off" -> "0"
            else -> "1"
        }

        val basePath = when (category) {
            SearchCategory.IMAGES -> "/images"
            SearchCategory.VIDEOS -> "/videos"
            SearchCategory.NEWS -> "/news"
            else -> "/search"
        }

        val searchEngineConfig = com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS.find { it.id == _uiState.value.selectedSearchEngine }
            ?: com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS[0]

        val targetUrl = UrlHelper.resolveSearchOrUrl(
            input = cleanQuery,
            searchEngineQueryUrl = searchEngineConfig.queryUrl,
            locationCode = _uiState.value.selectedLocation.code,
            safeSearch = safeParam,
            basePath = basePath
        )

        preferences.saveRecentSearch(cleanQuery)
        com.azokle.search.history.HistoryManager.recordSearch(
            query = cleanQuery,
            searchUrl = targetUrl,
            isPrivate = false
        )

        // Create/Activate in TabManager and open In-Built Browser directly
        TabManager.openTab(targetUrl, cleanQuery)

        _uiState.update {
            it.copy(
                searchQuery = "",
                suggestions = emptyList(),
                activeCategory = category,
                isSearchExpanded = false,
                inAppBrowserUrl = targetUrl,
                isResultsScreenVisible = false,
                recentSearches = preferences.getRecentSearches()
            )
        }
    }

    /**
     * Executes the open in app browser operation.
     */
    fun openInAppBrowser(url: String, isTor: Boolean = false) {
        if (url.isNotBlank() && TabManager.tabs.value.none { it.id == TabManager.activeTabId.value && it.url == url }) {
            TabManager.openTab(url, if (isTor) "Tor Browsing" else "Webpage", isTor = isTor)
        }
        _uiState.update {
            it.copy(
                inAppBrowserUrl = url,
                searchQuery = "",
                suggestions = emptyList(),
                isOnboardingVisible = false
            )
        }
    }

    /**
     * Executes the close in app browser operation.
     */
    fun closeInAppBrowser() {
        _uiState.update {
            it.copy(
                inAppBrowserUrl = null,
                searchQuery = "",
                suggestions = emptyList(),
                isSearchExpanded = false,
                isResultsScreenVisible = false
            )
        }
    }

    /**
     * Executes the close results screen operation.
     */
    fun closeResultsScreen() {
        _uiState.update {
            it.copy(
                isResultsScreenVisible = false,
                searchQuery = "",
                suggestions = emptyList(),
                inAppBrowserUrl = null
            )
        }
    }

    /**
     * Updates the search expanded.
     */
    fun setSearchExpanded(expanded: Boolean) {
        _uiState.update {
            it.copy(
                isSearchExpanded = expanded,
                searchQuery = if (!expanded) "" else it.searchQuery,
                suggestions = if (!expanded) emptyList() else it.suggestions,
                recentSearches = preferences.getRecentSearches()
            )
        }
    }

    /**
     * Updates the settings open.
     */
    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    /**
     * Updates the privacy modal open.
     */
    fun setPrivacyModalOpen(open: Boolean) {
        _uiState.update { it.copy(isPrivacyModalOpen = open) }
    }

    /**
     * Updates the terms modal open.
     */
    fun setTermsModalOpen(open: Boolean) {
        _uiState.update { it.copy(isTermsModalOpen = open) }
    }

    /**
     * Updates the about modal open.
     */
    fun setAboutModalOpen(open: Boolean) {
        _uiState.update { it.copy(isAboutModalOpen = open) }
    }

    /**
     * Updates the safe search.
     */
    fun setSafeSearch(mode: String) {
        preferences.setSafeSearch(mode)
        _uiState.update { it.copy(selectedSafeSearch = mode) }
    }

    /**
     * Updates the theme mode.
     */
    fun setThemeMode(mode: String) {
        val cleanMode = mode.lowercase()
        preferences.setThemeMode(cleanMode)
        com.azokle.search.AzokleApplication.applyThemeMode(cleanMode)
        com.azokle.search.engine.GeckoEngineManager.updateThemeMode(cleanMode)
        _uiState.update { it.copy(selectedThemeMode = cleanMode) }
    }

    /**
     * Updates the location.
     */
    fun setLocation(location: SearchLocation) {
        preferences.setLocation(location.name)
        _uiState.update { it.copy(selectedLocation = location) }
    }

    /**
     * Clears all recent searches.
     */
    fun clearRecentSearches() {
        preferences.clearRecentSearches()
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    /**
     * Executes the complete onboarding operation.
     */
    fun completeOnboarding() {
        preferences.setOnboardingCompleted(true)
        _uiState.update { it.copy(isOnboardingVisible = false) }
    }

    /**
     * Updates the browser engine.
     */
    fun setBrowserEngine(engine: String) {
        preferences.setBrowserEngine(engine)
        _uiState.update { it.copy(selectedBrowserEngine = engine.lowercase()) }
    }

    /**
     * Updates the doh provider.
     */
    fun setDohProvider(providerId: String) {
        preferences.setDohProvider(providerId)
        _uiState.update { it.copy(selectedDohProvider = providerId.lowercase()) }
    }

    /**
     * Updates the search engine.
     */
    fun setSearchEngine(engineId: String) {
        preferences.setSearchEngine(engineId)
        _uiState.update { it.copy(selectedSearchEngine = engineId.lowercase()) }
    }

    /**
     * Updates the search engine sheet open.
     */
    fun setSearchEngineSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isSearchEngineSheetOpen = open) }
    }

    /**
     * Updates the app language.
     */
    fun setAppLanguage(languageCode: String) {
        preferences.setAppLanguage(languageCode)
        com.azokle.search.common.util.LocaleHelper.setAppLocale(languageCode)
        _uiState.update { it.copy(selectedLanguage = languageCode.lowercase()) }
    }

    /**
     * Updates the language sheet open.
     */
    fun setLanguageSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isLanguageSheetOpen = open) }
    }

    /**
     * Executes the show onboarding operation.
     */
    fun showOnboarding() {
        _uiState.update { it.copy(isOnboardingVisible = true) }
    }
}
