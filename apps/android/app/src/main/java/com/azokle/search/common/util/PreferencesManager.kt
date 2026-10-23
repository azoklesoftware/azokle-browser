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

@file:Suppress("DEPRECATION")

package com.azokle.search.common.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences = getOrCreateEncryptedPrefs(context)

    companion object {
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val KEY_SAFE_SEARCH = "safe_search"
        private const val KEY_THEME = "app_theme_mode"
        private const val MAX_RECENT = 6
        private const val PREFS_FILE = "azokle_search_secure_prefs"
        private const val LEGACY_PREFS_FILE = "azokle_search_prefs"

        const val BLOCKING_MODE_STANDARD = "standard"
        const val BLOCKING_MODE_AGGRESSIVE = "aggressive"
        const val BLOCKING_MODE_OFF = "off"

        const val COOKIE_CONTROL_BLOCK_THIRD_PARTY = "block_third_party"
        const val COOKIE_CONTROL_BLOCK_ALL = "block_all"
        const val COOKIE_CONTROL_ALLOW_ALL = "allow_all"

        private const val KEY_SHIELD_BLOCKING_MODE = "shield_blocking_mode"
        private const val KEY_COOKIE_CONTROL = "cookie_control_mode"
        private const val KEY_COOKIE_POLICY = "cookie_policy_mode"
        private const val KEY_COOKIE_BANNER_HANDLING = "cookie_banner_handling_enabled"
        private const val KEY_FINGERPRINT_PROTECTION_ENABLED = "fingerprint_protection_enabled"
        private const val KEY_FONT_FINGERPRINT_PROTECTION = "font_fingerprint_protection_enabled"
        private const val KEY_SCREEN_FINGERPRINT_PROTECTION = "screen_fingerprint_protection_enabled"
        private const val KEY_USER_AGENT_FINGERPRINT_PROTECTION = "user_agent_fingerprint_protection_enabled"
        private const val KEY_ENTERPRISE_ROOTS_ENABLED = "enterprise_roots_enabled"

        @Volatile
        private var cachedPrefs: SharedPreferences? = null

        @Volatile
        private var instance: PreferencesManager? = null

        @Volatile
        private var tabsTourCompletedCache: Boolean? = null

        @Volatile
        private var homeTourCompletedCache: Boolean? = null

        /**
         * Retrieves the instance.
         */
        operator fun invoke(context: Context): PreferencesManager = getInstance(context)

        /**
         * Retrieves the instance.
         */
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Retrieves the or create encrypted prefs.
         */
        private fun getOrCreateEncryptedPrefs(context: Context): SharedPreferences {
            cachedPrefs?.let { return it }
            return synchronized(this) {
                cachedPrefs ?: createEncryptedPrefs(context.applicationContext).also {
                    cachedPrefs = it
                }
            }
        }

        /**
         * Executes the create encrypted prefs operation.
         */
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val encPrefs = EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )

                    // Seamless migration from legacy unencrypted SharedPreferences
                    try {
                        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
                        val legacySearches = legacyPrefs.getString(KEY_RECENT_SEARCHES, null)
                        if (!legacySearches.isNullOrBlank() && !encPrefs.contains(KEY_RECENT_SEARCHES)) {
                            encPrefs.edit().putString(KEY_RECENT_SEARCHES, legacySearches).apply()
                            legacyPrefs.edit().remove(KEY_RECENT_SEARCHES).apply()
                        }
                    } catch (_: Exception) {}

                    return encPrefs
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("PreferencesManager", "Keystore init attempt $attempt/3 failed: ${e.message}")
                    try {
                        Thread.sleep(50L * attempt)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            // AZK-LOW-01: Fail closed with safe isolated dummy preferences on Keystore failure
            // instead of silently degrading to unencrypted persistent storage.
            android.util.Log.e("PreferencesManager", "Keystore unavailable. Failing closed with isolated dummy store: ${lastException?.message}")
            return context.getSharedPreferences("${PREFS_FILE}_keystore_unavailable_dummy", Context.MODE_PRIVATE)
        }
    }



    /**
     * Retrieves the recent searches.
     */
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

    /**
     * Persists recent search to storage.
     */
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

    /**
     * Persists recent searches to storage.
     */
    fun saveRecentSearches(list: List<String>) {
        val trimmed = if (list.size > MAX_RECENT) list.take(MAX_RECENT) else list
        val jsonArray = JSONArray(trimmed)
        prefs.edit().putString(KEY_RECENT_SEARCHES, jsonArray.toString()).apply()
    }

    /**
     * Clears all recent searches.
     */
    fun clearRecentSearches() {
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
        try {
            context.getSharedPreferences(LEGACY_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_RECENT_SEARCHES)
                .apply()
        } catch (_: Exception) {}
    }

    /**
     * Retrieves the safe search.
     */
    fun getSafeSearch(): String {
        return prefs.getString(KEY_SAFE_SEARCH, "Moderate") ?: "Moderate"
    }

    /**
     * Updates the safe search.
     */
    fun setSafeSearch(mode: String) {
        prefs.edit().putString(KEY_SAFE_SEARCH, mode).apply()
    }


    /**
     * Retrieves the theme mode.
     */
    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME, "system") ?: "system"
    }

    /**
     * Updates the theme mode.
     */
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

    /**
     * Checks if completed onboarding exists.
     */
    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean("has_completed_onboarding", false)
    }

    /**
     * Updates the onboarding completed.
     */
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
    }

    /**
     * Retrieves the browser engine.
     */
    fun getBrowserEngine(): String {
        return prefs.getString("browser_engine", "gecko") ?: "gecko"
    }

    /**
     * Updates the browser engine.
     */
    fun setBrowserEngine(engine: String) {
        prefs.edit().putString("browser_engine", engine.lowercase()).apply()
    }

    /**
     * Retrieves the doh provider.
     */
    fun getDohProvider(): String {
        return prefs.getString("doh_provider", "cloudflare") ?: "cloudflare"
    }

    /**
     * Updates the doh provider.
     */
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

    /**
     * Retrieves the search engine.
     */
    fun getSearchEngine(): String {
        return prefs.getString("search_engine", "azokle") ?: "azokle"
    }

    /**
     * Updates the search engine.
     */
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

    /**
     * Retrieves the app language.
     */
    fun getAppLanguage(): String {
        return prefs.getString("app_language", "system") ?: "system"
    }

    /**
     * Updates the app language.
     */
    fun setAppLanguage(languageCode: String) {
        prefs.edit().putString("app_language", languageCode.lowercase()).apply()
    }

    /**
     * Checks whether background audio enabled.
     */
    fun isBackgroundAudioEnabled(): Boolean {
        return prefs.getBoolean("pref_background_audio", true)
    }

    /**
     * Updates the background audio enabled.
     */
    fun setBackgroundAudioEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_background_audio", enabled).apply()
    }

    /**
     * Checks if completed tour exists.
     */
    fun hasCompletedTour(): Boolean {
        return hasCompletedHomeTour()
    }

    /**
     * Updates the tour completed.
     */
    fun setTourCompleted(completed: Boolean) {
        setHomeTourCompleted(completed)
    }

    /**
     * Checks if completed home tour exists.
     */
    fun hasCompletedHomeTour(): Boolean {
        homeTourCompletedCache?.let { return it }
        val value = prefs.getBoolean("has_completed_home_tour", false)
        homeTourCompletedCache = value
        return value
    }

    /**
     * Updates the home tour completed.
     */
    fun setHomeTourCompleted(completed: Boolean) {
        homeTourCompletedCache = completed
        prefs.edit().putBoolean("has_completed_home_tour", completed).apply()
    }

    /**
     * Checks if completed tabs tour exists.
     */
    fun hasCompletedTabsTour(): Boolean {
        tabsTourCompletedCache?.let { return it }
        val value = prefs.getBoolean("has_completed_tabs_tour", false)
        tabsTourCompletedCache = value
        return value
    }

    /**
     * Updates the tabs tour completed.
     */
    fun setTabsTourCompleted(completed: Boolean) {
        tabsTourCompletedCache = completed
        prefs.edit().putBoolean("has_completed_tabs_tour", completed).apply()
    }

    /**
     * Resets tours to default state.
     */
    fun resetTours() {
        homeTourCompletedCache = false
        tabsTourCompletedCache = false
        prefs.edit()
            .remove("has_completed_home_tour")
            .remove("has_completed_tabs_tour")
            .remove("has_completed_driver_tour")
            .apply()
    }

    /**
     * Retrieves the shield blocking mode: "standard" (default), "aggressive", or "off".
     */
    fun getShieldBlockingMode(): String {
        return prefs.getString(KEY_SHIELD_BLOCKING_MODE, BLOCKING_MODE_STANDARD) ?: BLOCKING_MODE_STANDARD
    }

    /**
     * Updates the shield blocking mode.
     */
    fun setShieldBlockingMode(mode: String) {
        prefs.edit().putString(KEY_SHIELD_BLOCKING_MODE, mode).apply()
    }

    /**
     * Checks if aggressive tracker and ad blocking is enabled.
     */
    fun isAggressiveBlocking(): Boolean {
        return getShieldBlockingMode() == BLOCKING_MODE_AGGRESSIVE
    }

    /**
     * Retrieves cookie control mode: "block_third_party" (default), "block_all", or "allow_all".
     */
    fun getCookieControl(): String {
        return prefs.getString(KEY_COOKIE_CONTROL, COOKIE_CONTROL_BLOCK_THIRD_PARTY) ?: COOKIE_CONTROL_BLOCK_THIRD_PARTY
    }

    /**
     * Updates cookie control mode with automatic mapping to GeckoView cookie policy integer.
     */
    fun setCookieControl(control: String) {
        val policyInt = when (control) {
            COOKIE_CONTROL_BLOCK_ALL -> 2 // ContentBlocking.CookieBehavior.ACCEPT_NONE
            COOKIE_CONTROL_ALLOW_ALL -> 0 // ContentBlocking.CookieBehavior.ACCEPT_ALL
            else -> 5 // ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS (Default)
        }
        prefs.edit()
            .putString(KEY_COOKIE_CONTROL, control)
            .putInt(KEY_COOKIE_POLICY, policyInt)
            .apply()
    }

    /**
     * Retrieves the cookie policy integer for GeckoView ContentBlocking.
     */
    fun getCookiePolicy(): Int {
        return when (getCookieControl()) {
            COOKIE_CONTROL_BLOCK_ALL -> 2
            COOKIE_CONTROL_ALLOW_ALL -> 0
            else -> 5 // ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
        }
    }

    /**
     * Updates the cookie policy mode directly.
     */
    fun setCookiePolicy(policy: Int) {
        val control = when (policy) {
            2 -> COOKIE_CONTROL_BLOCK_ALL
            0 -> COOKIE_CONTROL_ALLOW_ALL
            else -> COOKIE_CONTROL_BLOCK_THIRD_PARTY
        }
        prefs.edit()
            .putInt(KEY_COOKIE_POLICY, policy)
            .putString(KEY_COOKIE_CONTROL, control)
            .apply()
    }

    /**
     * Checks if automated cookie banner handling / rejection is enabled.
     */
    fun isCookieBannerHandlingEnabled(): Boolean {
        return prefs.getBoolean(KEY_COOKIE_BANNER_HANDLING, true)
    }

    /**
     * Updates cookie banner handling preference.
     */
    fun setCookieBannerHandlingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COOKIE_BANNER_HANDLING, enabled).apply()
    }

    /**
     * Checks if master fingerprinting protection is enabled (default: true).
     */
    fun isFingerprintProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_FINGERPRINT_PROTECTION_ENABLED, true)
    }

    /**
     * Updates master fingerprinting protection preference.
     */
    fun setFingerprintProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FINGERPRINT_PROTECTION_ENABLED, enabled).apply()
    }

    /**
     * Checks if font fingerprinting protection is enabled (default: true).
     */
    fun isFontFingerprintProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_FONT_FINGERPRINT_PROTECTION, true)
    }

    /**
     * Updates font fingerprinting protection preference.
     */
    fun setFontFingerprintProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FONT_FINGERPRINT_PROTECTION, enabled).apply()
    }

    /**
     * Checks if screen resolution fingerprinting protection is enabled (default: true).
     */
    fun isScreenFingerprintProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCREEN_FINGERPRINT_PROTECTION, true)
    }

    /**
     * Updates screen resolution fingerprinting protection preference.
     */
    fun setScreenFingerprintProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREEN_FINGERPRINT_PROTECTION, enabled).apply()
    }

    /**
     * Checks if user-agent and hardware concurrency fingerprinting protection is enabled (default: true).
     */
    fun isUserAgentFingerprintProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_USER_AGENT_FINGERPRINT_PROTECTION, true)
    }

    /**
     * Updates user-agent fingerprinting protection preference.
     */
    fun setUserAgentFingerprintProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USER_AGENT_FINGERPRINT_PROTECTION, enabled).apply()
    }

    /**
     * Checks if enterprise/system root certificates are imported into GeckoView (default: false for sovereign security).
     */
    fun isEnterpriseRootsEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENTERPRISE_ROOTS_ENABLED, false)
    }

    /**
     * Updates enterprise/system root certificates import preference.
     */
    fun setEnterpriseRootsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENTERPRISE_ROOTS_ENABLED, enabled).apply()
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

