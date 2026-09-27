package com.azokle.search.common.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class SearchLocation(
    val name: String,
    val code: String,
    val flag: String
)

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val KEY_SAFE_SEARCH = "safe_search"
        private const val KEY_LOCATION = "location"
        private const val KEY_THEME = "app_theme_mode"
        private const val MAX_RECENT = 6
        private const val PREFS_FILE = "azokle_search_secure_prefs"
        private const val LEGACY_PREFS_FILE = "azokle_search_prefs"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            val encPrefs = try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                try {
                    context.deleteSharedPreferences(PREFS_FILE)
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (e2: Exception) {
                    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                }
            }

            // Seamless migration from legacy unencrypted SharedPreferences
            try {
                val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
                val legacySearches = legacyPrefs.getString(KEY_RECENT_SEARCHES, null)
                if (!legacySearches.isNullOrBlank() && !encPrefs.contains(KEY_RECENT_SEARCHES)) {
                    encPrefs.edit().putString(KEY_RECENT_SEARCHES, legacySearches).apply()
                    legacyPrefs.edit().remove(KEY_RECENT_SEARCHES).apply()
                }
            } catch (e: Exception) {
                // Ignore migration error
            }

            return encPrefs
        }

        val DEFAULT_LOCATIONS = listOf(
            SearchLocation("All Regions", "auto", "🌐"),
            SearchLocation("United States", "us-en", "🇺🇸"),
            SearchLocation("United Kingdom", "uk-en", "🇬🇧"),
            SearchLocation("India", "in-en", "🇮🇳"),
            SearchLocation("Germany", "de-de", "🇩🇪"),
            SearchLocation("France", "fr-fr", "🇫🇷"),
            SearchLocation("Japan", "jp-jp", "🇯🇵"),
            SearchLocation("Canada", "ca-en", "🇨🇦"),
            SearchLocation("Australia", "au-en", "🇦🇺"),
            SearchLocation("Brazil", "br-pt", "🇧🇷")
        )
    }

    fun getRecentSearches(): List<String> {
        val jsonStr = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return

        val current = getRecentSearches().toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)

        val trimmed = if (current.size > MAX_RECENT) current.take(MAX_RECENT) else current
        val jsonArray = JSONArray(trimmed)
        prefs.edit().putString(KEY_RECENT_SEARCHES, jsonArray.toString()).apply()
    }

    fun saveRecentSearches(list: List<String>) {
        val trimmed = if (list.size > MAX_RECENT) list.take(MAX_RECENT) else list
        val jsonArray = JSONArray(trimmed)
        prefs.edit().putString(KEY_RECENT_SEARCHES, jsonArray.toString()).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
        try {
            context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_RECENT_SEARCHES)
                .apply()
        } catch (_: Exception) {}
    }

    fun getSafeSearch(): String {
        return prefs.getString(KEY_SAFE_SEARCH, "Moderate") ?: "Moderate"
    }

    fun setSafeSearch(mode: String) {
        prefs.edit().putString(KEY_SAFE_SEARCH, mode).apply()
    }

    fun getLocation(): String {
        return prefs.getString(KEY_LOCATION, "All Regions") ?: "All Regions"
    }

    fun setLocation(locationName: String) {
        prefs.edit().putString(KEY_LOCATION, locationName).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }

    fun setThemeMode(theme: String) {
        prefs.edit().putString(KEY_THEME, theme.lowercase()).apply()
        try {
            com.azokle.search.widget.AzokleSearchWidgetProvider.updateAllWidgets(context)
            com.azokle.search.widget.AzokleTorSearchWidgetProvider.updateAllWidgets(context)
            com.azokle.search.widget.AzokleShieldsWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean("has_completed_onboarding", false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    fun getBrowserEngine(): String {
        return prefs.getString("browser_engine", "gecko") ?: "gecko"
    }

    fun setBrowserEngine(engine: String) {
        prefs.edit().putString("browser_engine", engine.lowercase()).apply()
    }

    fun getDohProvider(): String {
        return prefs.getString("doh_provider", "cloudflare") ?: "cloudflare"
    }

    fun setDohProvider(providerId: String) {
        val cleanId = providerId.lowercase()
        prefs.edit().putString("doh_provider", cleanId).apply()
        com.azokle.search.core.DnsOverHttpsManager.activeProviderId = cleanId
        try {
            com.azokle.search.engine.GeckoConfigManager.getOrCreateConfigFile(context)
        } catch (e: Exception) {
            // Ignore in testing environments
        }
    }

    fun getSearchEngine(): String {
        return prefs.getString("search_engine", "azokle") ?: "azokle"
    }

    fun setSearchEngine(engineId: String) {
        prefs.edit().putString("search_engine", engineId.lowercase()).apply()
        try {
            com.azokle.search.widget.AzokleSearchWidgetProvider.updateAllWidgets(context)
            com.azokle.search.widget.AzokleTorSearchWidgetProvider.updateAllWidgets(context)
            com.azokle.search.widget.AzokleShieldsWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            // Ignore in testing environments
        }
    }

    fun getAppLanguage(): String {
        return prefs.getString("app_language", "system") ?: "system"
    }

    fun setAppLanguage(languageCode: String) {
        prefs.edit().putString("app_language", languageCode.lowercase()).apply()
    }

    fun isBackgroundAudioEnabled(): Boolean {
        return prefs.getBoolean("pref_background_audio", true)
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_background_audio", enabled).apply()
    }

    fun hasCompletedTour(): Boolean {
        return hasCompletedHomeTour()
    }

    fun setTourCompleted(completed: Boolean) {
        setHomeTourCompleted(completed)
    }

    fun hasCompletedHomeTour(): Boolean {
        return prefs.getBoolean("has_completed_home_tour", false)
    }

    fun setHomeTourCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_home_tour", completed).apply()
    }

    fun hasCompletedTabsTour(): Boolean {
        return prefs.getBoolean("has_completed_tabs_tour", false)
    }

    fun setTabsTourCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_tabs_tour", completed).apply()
    }

    fun resetTours() {
        prefs.edit()
            .remove("has_completed_home_tour")
            .remove("has_completed_tabs_tour")
            .remove("has_completed_driver_tour")
            .apply()
    }
}


data class SearchEngineConfig(
    val id: String,
    val name: String,
    val queryUrl: String,
    val subtitle: String,
    val isDefault: Boolean = false,
    val domain: String,
    val iconRes: Int = 0
)

val SEARCH_ENGINE_PROVIDERS = listOf(
    SearchEngineConfig(
        id = "azokle",
        name = "Azokle Search",
        queryUrl = "https://search.azokle.com/search?q=",
        subtitle = "Decentralized AI & sovereign privacy search",
        isDefault = true,
        domain = "search.azokle.com",
        iconRes = com.azokle.search.R.drawable.ic_engine_azokle
    ),
    SearchEngineConfig(
        id = "duckduckgo",
        name = "DuckDuckGo",
        queryUrl = "https://duckduckgo.com/?q=",
        subtitle = "Privacy, simplified. No profiling or tracker tracking",
        isDefault = false,
        domain = "duckduckgo.com",
        iconRes = com.azokle.search.R.drawable.ic_engine_duckduckgo
    ),
    SearchEngineConfig(
        id = "brave",
        name = "Brave Search",
        queryUrl = "https://search.brave.com/search?q=",
        subtitle = "Independent search index with complete privacy",
        isDefault = false,
        domain = "search.brave.com",
        iconRes = com.azokle.search.R.drawable.ic_engine_brave
    ),
    SearchEngineConfig(
        id = "startpage",
        name = "Startpage",
        queryUrl = "https://www.startpage.com/sp/search?query=",
        subtitle = "Private search engine with Google-grade indexing",
        isDefault = false,
        domain = "startpage.com",
        iconRes = com.azokle.search.R.drawable.ic_engine_startpage
    ),
    SearchEngineConfig(
        id = "mojeek",
        name = "Mojeek",
        queryUrl = "https://www.mojeek.com/search?q=",
        subtitle = "Independent web crawler with complete search privacy",
        isDefault = false,
        domain = "mojeek.com",
        iconRes = com.azokle.search.R.drawable.ic_engine_mojeek
    ),
    SearchEngineConfig(
        id = "qwant",
        name = "Qwant",
        queryUrl = "https://www.qwant.com/?q=",
        subtitle = "European privacy search with zero profiling or ad-tracking",
        isDefault = false,
        domain = "qwant.com",
        iconRes = com.azokle.search.R.drawable.ic_engine_qwant
    )
)

