package com.azokle.search.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class FilterDecision(
    val shouldBlock: Boolean = false,
    val category: Int = 0,
    val matchedRule: String? = null,
    val isException: Boolean = false
)

data class RustShieldStats(
    val totalLifetimeBlocked: Int = 0,
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val telemetryBlocked: Int = 0,
    val annoyancesBlocked: Int = 0,
    val fingerprintingBlocked: Int = 0,
    val cryptominersBlocked: Int = 0
)

/**
 * Sovereign Azokle Native Rust Core Singleton.
 * High-performance AdBlocker, Tracker Hunter, Cosmetic Filter & Sovereign Shield Engine.
 */
object AzokleCore {
    private const val TAG = "AzokleCore"
    private var isNativeLoaded = false
    private var isInitialized = false

    private val _shieldStats = MutableStateFlow(RustShieldStats())
    val shieldStats: StateFlow<RustShieldStats> = _shieldStats.asStateFlow()

    init {
        try {
            System.loadLibrary("azokle_core")
            isNativeLoaded = true
            initNative()
            Log.i(TAG, "libazokle_core.so loaded successfully via JNI")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library libazokle_core.so: ${e.message}")
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing AzokleCore: ${e.message}")
        }
    }

    // JNI Native Declarations
    private external fun initNative()
    private external fun loadFilterRules(content: String, categoryId: Int): Int
    private external fun loadTrackerList(content: String): Int
    private external fun loadCosmeticRules(content: String): Int
    private external fun checkUrl(url: String, sourceOrigin: String?, isThirdParty: Boolean): String
    private external fun cleanUrl(url: String): String
    private external fun getCosmeticCss(domain: String): String
    private external fun getScriptlets(domain: String): String
    private external fun isTracker(host: String): Boolean
    private external fun toggleHostShield(host: String, enabled: Boolean): Boolean
    private external fun isHostShieldEnabled(host: String): Boolean
    private external fun getShieldStats(): String
    private external fun recordBlock(host: String, categoryId: Int)

    // Tor & DNS JNI Native Declarations
    private external fun torProbeSocks5(port: Int, timeoutMs: Int): Boolean
    private external fun torRotateCircuit(controlPort: Int, auth: String): String
    private external fun torVerifyExitIp(socksPort: Int): String
    private external fun torResolveUrl(rawInput: String, searchQueryUrl: String): String
    private external fun torIsOnionUrl(url: String): Boolean
    private external fun dnsGetDoHEndpoint(providerName: String): String

    // Website Permissions JNI Declarations
    private external fun permSet(origin: String, permType: String, state: String, isPrivate: Boolean)
    private external fun permGet(origin: String, permType: String, isPrivate: Boolean): String
    private external fun permClearForOrigin(origin: String)
    private external fun permClearAll()
    private external fun permGetSitePermissionsJson(origin: String, isPrivate: Boolean): String
    private external fun permGetAllCustomizedOrigins(): String
    private external fun permExportJson(): String
    private external fun permImportJson(jsonData: String): Int

    // Browsing History JNI Declarations
    private external fun historyRecordVisit(title: String, url: String, isPrivate: Boolean): String
    private external fun historySearch(query: String, limit: Int): String
    private external fun historyGetRecent(limit: Int, offset: Int): String
    private external fun historyDeleteEntry(id: String): Boolean
    private external fun historyDeleteUrl(url: String): Boolean
    private external fun historyClearRange(fromMs: Long, toMs: Long): Int
    private external fun historyClearAll()
    private external fun historyExportJson(): String
    private external fun historyImportJson(jsonData: String): Int

    // Bookmarks JNI Declarations
    private external fun bookmarkAdd(title: String, url: String, parentFolder: String, faviconUrl: String): String
    private external fun bookmarkCreateFolder(title: String, parentFolder: String): String
    private external fun bookmarkRemove(id: String): Boolean
    private external fun bookmarkIsBookmarked(url: String): Boolean
    private external fun bookmarkGetAll(): String
    private external fun bookmarkSearch(query: String): String
    private external fun bookmarkExportJson(): String
    private external fun bookmarkImportJson(jsonData: String): Int

    // Downloads JNI Declarations
    private external fun downloadCreate(url: String, filename: String, destinationPath: String, totalBytes: Long, mimeType: String): String
    private external fun downloadUpdateProgress(id: String, downloadedBytes: Long, totalBytes: Long, state: String): Boolean
    private external fun downloadGetAll(): String
    private external fun downloadDeleteItem(id: String): Boolean
    private external fun downloadExtractFilename(url: String, contentDisposition: String, mimeType: String): String

    // Settings JNI Declarations
    private external fun settingsGetSearchEngine(): String
    private external fun settingsSetSearchEngine(engine: String)
    private external fun settingsGetRecentSearches(): String
    private external fun settingsAddRecentSearch(query: String)
    private external fun settingsClearRecentSearches()
    private external fun settingsExportJson(): String
    private external fun settingsImportJson(jsonData: String): Boolean

    // Tab Management JNI Declarations
    private external fun tabOpen(url: String, title: String, isPrivate: Boolean, isTor: Boolean, groupId: String, groupTitle: String): String
    private external fun tabClose(id: String): String
    private external fun tabRestoreRecentlyClosed(): String
    private external fun tabGetActive(): String
    private external fun tabSetActive(id: String): Boolean
    private external fun tabUpdate(id: String, url: String, title: String): Boolean
    private external fun tabGetAll(): String
    private external fun tabCloseAll(includePrivate: Boolean)
    private external fun tabClosePrivate()
    private external fun tabAutoGroupByDomain()
    private external fun tabExportJson(): String
    private external fun tabImportJson(jsonData: String): Int

    // Universal Protocol Dispatcher JNI Declaration
    private external fun protocolDispatch(envelopeJson: String): String

    /**
     * Initializes the Rust Engine by asynchronously streaming all packaged AdBlock,
     * Tracker, Telemetry, Fingerprinting, and Cosmetic rules from assets.
     */
    fun initialize(context: Context) {
        if (isInitialized || !isNativeLoaded) return
        isInitialized = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                loadAssetsRules(context)
                refreshShieldStats()
                Log.i(TAG, "AzokleCore initialized and rules indexed in native memory.")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading adblock assets into AzokleCore: ${e.message}")
            }
        }
    }

    private fun loadAssetsRules(context: Context) {
        val assetManager = context.assets
        val adblockFolder = "adblock"

        // 1. Load Ads & Network Rules (Category: 1 = Ad)
        loadAssetFile(assetManager, "$adblockFolder/ads.txt")?.let { loadFilterRules(it, 1) }
        loadAssetFile(assetManager, "$adblockFolder/ad_domains.txt")?.let { loadFilterRules(it, 1) }

        // 2. Load Trackers & Analytics (Category: 2 = Tracker)
        loadAssetFile(assetManager, "$adblockFolder/trackers.txt")?.let {
            loadTrackerList(it)
            loadFilterRules(it, 2)
        }
        loadAssetFile(assetManager, "$adblockFolder/analytics.txt")?.let {
            loadTrackerList(it)
            loadFilterRules(it, 2)
        }

        // 3. Load Telemetry (Category: 3 = Telemetry)
        loadAssetFile(assetManager, "$adblockFolder/telemetry.txt")?.let { loadFilterRules(it, 3) }

        // 4. Load Fingerprinting (Category: 4 = Fingerprinting)
        loadAssetFile(assetManager, "$adblockFolder/fingerprinting.txt")?.let { loadFilterRules(it, 4) }

        // 5. Load Annoyances (Category: 5 = Annoyance)
        loadAssetFile(assetManager, "$adblockFolder/annoyances.txt")?.let { loadFilterRules(it, 5) }

        // 6. Load Social (Category: 7 = Social)
        loadAssetFile(assetManager, "$adblockFolder/social.txt")?.let { loadFilterRules(it, 7) }

        // 7. Load Cosmetic CSS rules
        loadAssetFile(assetManager, "$adblockFolder/cosmetic_css.txt")?.let { loadCosmeticRules(it) }
        loadAssetFile(assetManager, "extensions/azokle-shield/cosmetic.css")?.let { loadCosmeticRules(it) }
    }

    private fun loadAssetFile(assetManager: android.content.res.AssetManager, path: String): String? {
        return try {
            assetManager.open(path).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * High-speed Rust URL classification check.
     */
    fun shouldBlock(url: String, sourceOrigin: String? = null, isThirdParty: Boolean = true): FilterDecision {
        if (!isNativeLoaded || url.isBlank()) return FilterDecision()
        return try {
            val jsonStr = checkUrl(url, sourceOrigin, isThirdParty)
            val json = JSONObject(jsonStr)
            val should = json.optBoolean("should_block", false)
            val cat = json.optInt("category", 0)
            val rule = json.optString("matched_rule", "").takeIf { it.isNotBlank() }
            val isExc = json.optBoolean("is_exception", false)

            if (should) {
                refreshShieldStats()
            }

            FilterDecision(
                shouldBlock = should,
                category = cat,
                matchedRule = rule,
                isException = isExc
            )
        } catch (e: Exception) {
            FilterDecision()
        }
    }

    /**
     * Strips tracking URL query parameters (utm_*, fbclid, gclid, etc.)
     */
    fun cleanTrackingParameters(url: String): String {
        if (!isNativeLoaded || url.isBlank()) return url
        return try {
            cleanUrl(url)
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Generates optimized, pruned cosmetic CSS for the target origin.
     */
    fun getCosmeticStyles(domain: String): String {
        if (!isNativeLoaded || domain.isBlank()) return ""
        return try {
            getCosmeticCss(domain)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Returns defuser scriptlets for specific platforms (e.g. YouTube ad defusers).
     */
    fun getDefuserScriptlet(domain: String): String {
        if (!isNativeLoaded || domain.isBlank()) return ""
        return try {
            getScriptlets(domain)
        } catch (e: Exception) {
            ""
        }
    }

    fun isTrackerHost(host: String): Boolean {
        if (!isNativeLoaded || host.isBlank()) return false
        return try {
            isTracker(host)
        } catch (e: Exception) {
            false
        }
    }

    fun toggleShieldForHost(host: String, enabled: Boolean): Boolean {
        if (!isNativeLoaded || host.isBlank()) return enabled
        return try {
            toggleHostShield(host, enabled)
        } catch (e: Exception) {
            enabled
        }
    }

    fun isShieldActiveForHost(host: String): Boolean {
        if (!isNativeLoaded || host.isBlank()) return true
        return try {
            isHostShieldEnabled(host)
        } catch (e: Exception) {
            true
        }
    }

    fun recordBlockedHost(host: String, categoryId: Int = 1) {
        if (!isNativeLoaded || host.isBlank()) return
        try {
            recordBlock(host, categoryId)
            refreshShieldStats()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun refreshShieldStats() {
        if (!isNativeLoaded) return
        try {
            val jsonStr = getShieldStats()
            val json = JSONObject(jsonStr)
            _shieldStats.update {
                RustShieldStats(
                    totalLifetimeBlocked = json.optInt("total_lifetime_blocked", 0),
                    adsBlocked = json.optInt("ads_blocked", 0),
                    trackersBlocked = json.optInt("trackers_blocked", 0),
                    telemetryBlocked = json.optInt("telemetry_blocked", 0),
                    annoyancesBlocked = json.optInt("annoyances_blocked", 0),
                    fingerprintingBlocked = json.optInt("fingerprinting_blocked", 0),
                    cryptominersBlocked = json.optInt("cryptominers_blocked", 0)
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    // -------------------------------------------------------------------------
    // Tor & DNS Native Rust Subsystem API
    // -------------------------------------------------------------------------

    fun probeTorSocks5(port: Int = 9050, timeoutMs: Int = 1000): Boolean {
        if (!isNativeLoaded) return false
        return try {
            torProbeSocks5(port, timeoutMs)
        } catch (e: Exception) {
            false
        }
    }

    fun rotateTorCircuit(controlPort: Int = 9051, auth: String = ""): Pair<Boolean, String> {
        if (!isNativeLoaded) return Pair(false, "Native core not loaded")
        return try {
            val jsonStr = torRotateCircuit(controlPort, auth)
            val json = JSONObject(jsonStr)
            val success = json.optBoolean("success", false)
            val circuitId = json.optString("circuit_id", "")
            val err = json.optString("error", "Rotation failed")
            if (success) Pair(true, circuitId) else Pair(false, err)
        } catch (e: Exception) {
            Pair(false, e.message ?: "Rotation exception")
        }
    }

    fun verifyTorExit(socksPort: Int = 9050): JSONObject? {
        if (!isNativeLoaded) return null
        return try {
            val jsonStr = torVerifyExitIp(socksPort)
            val json = JSONObject(jsonStr)
            if (json.optBoolean("success", false)) json else null
        } catch (e: Exception) {
            null
        }
    }

    fun resolveTorUrl(rawInput: String, searchEngineQueryUrl: String = "https://duckduckgo.com/?q="): String {
        if (!isNativeLoaded || rawInput.isBlank()) return rawInput
        return try {
            torResolveUrl(rawInput, searchEngineQueryUrl)
        } catch (e: Exception) {
            rawInput
        }
    }

    fun isOnionUrl(url: String): Boolean {
        if (!isNativeLoaded || url.isBlank()) return url.contains(".onion", ignoreCase = true)
        return try {
            torIsOnionUrl(url)
        } catch (e: Exception) {
            url.contains(".onion", ignoreCase = true)
        }
    }

    fun getDoHEndpoint(providerName: String): String {
        if (!isNativeLoaded) return "https://cloudflare-dns.com/dns-query"
        return try {
            dnsGetDoHEndpoint(providerName)
        } catch (e: Exception) {
            "https://cloudflare-dns.com/dns-query"
        }
    }

    // -------------------------------------------------------------------------
    // Website Permissions Subsystem API
    // -------------------------------------------------------------------------

    fun setSitePermission(origin: String, permType: String, state: String, isPrivate: Boolean = false) {
        if (!isNativeLoaded || origin.isBlank()) return
        try {
            permSet(origin, permType, state, isPrivate)
        } catch (e: Exception) {}
    }

    fun getSitePermission(origin: String, permType: String, isPrivate: Boolean = false): String {
        if (!isNativeLoaded || origin.isBlank()) return "PROMPT"
        return try {
            permGet(origin, permType, isPrivate)
        } catch (e: Exception) {
            "PROMPT"
        }
    }

    fun clearPermissionsForOrigin(origin: String) {
        if (!isNativeLoaded || origin.isBlank()) return
        try {
            permClearForOrigin(origin)
        } catch (e: Exception) {}
    }

    fun clearAllPermissions() {
        if (!isNativeLoaded) return
        try {
            permClearAll()
        } catch (e: Exception) {}
    }

    fun getSitePermissionsJson(origin: String, isPrivate: Boolean = false): JSONObject {
        if (!isNativeLoaded || origin.isBlank()) return JSONObject()
        return try {
            JSONObject(permGetSitePermissionsJson(origin, isPrivate))
        } catch (e: Exception) {
            JSONObject()
        }
    }

    fun getAllCustomizedPermissionOrigins(): List<String> {
        if (!isNativeLoaded) return emptyList()
        return try {
            val arr = org.json.JSONArray(permGetAllCustomizedOrigins())
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun exportPermissionsJson(): String {
        if (!isNativeLoaded) return "{}"
        return try {
            permExportJson()
        } catch (e: Exception) {
            "{}"
        }
    }

    fun importPermissionsJson(jsonData: String): Int {
        if (!isNativeLoaded || jsonData.isBlank()) return 0
        return try {
            permImportJson(jsonData)
        } catch (e: Exception) {
            0
        }
    }

    // -------------------------------------------------------------------------
    // Browsing History Subsystem API
    // -------------------------------------------------------------------------

    fun recordHistoryVisit(title: String, url: String, isPrivate: Boolean = false): JSONObject? {
        if (!isNativeLoaded || url.isBlank() || isPrivate) return null
        return try {
            val res = historyRecordVisit(title, url, isPrivate)
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun searchHistory(query: String, limit: Int = 20): org.json.JSONArray {
        if (!isNativeLoaded) return org.json.JSONArray()
        return try {
            org.json.JSONArray(historySearch(query, limit))
        } catch (e: Exception) {
            org.json.JSONArray()
        }
    }

    fun getRecentHistory(limit: Int = 50, offset: Int = 0): org.json.JSONArray {
        if (!isNativeLoaded) return org.json.JSONArray()
        return try {
            org.json.JSONArray(historyGetRecent(limit, offset))
        } catch (e: Exception) {
            org.json.JSONArray()
        }
    }

    fun deleteHistoryEntry(id: String): Boolean {
        if (!isNativeLoaded || id.isBlank()) return false
        return try {
            historyDeleteEntry(id)
        } catch (e: Exception) {
            false
        }
    }

    fun deleteHistoryUrl(url: String): Boolean {
        if (!isNativeLoaded || url.isBlank()) return false
        return try {
            historyDeleteUrl(url)
        } catch (e: Exception) {
            false
        }
    }

    fun clearHistoryRange(fromMs: Long, toMs: Long): Int {
        if (!isNativeLoaded) return 0
        return try {
            historyClearRange(fromMs, toMs)
        } catch (e: Exception) {
            0
        }
    }

    fun clearAllHistory() {
        if (!isNativeLoaded) return
        try {
            historyClearAll()
        } catch (e: Exception) {}
    }

    fun exportHistoryJson(): String {
        if (!isNativeLoaded) return "[]"
        return try {
            historyExportJson()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun importHistoryJson(jsonData: String): Int {
        if (!isNativeLoaded || jsonData.isBlank()) return 0
        return try {
            historyImportJson(jsonData)
        } catch (e: Exception) {
            0
        }
    }

    // -------------------------------------------------------------------------
    // Bookmarks Subsystem API
    // -------------------------------------------------------------------------

    fun addBookmark(title: String, url: String, parentFolder: String = "", faviconUrl: String = ""): JSONObject? {
        if (!isNativeLoaded || url.isBlank()) return null
        return try {
            val res = bookmarkAdd(title, url, parentFolder, faviconUrl)
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun isUrlBookmarked(url: String): Boolean {
        if (!isNativeLoaded || url.isBlank()) return false
        return try {
            bookmarkIsBookmarked(url)
        } catch (e: Exception) {
            false
        }
    }

    fun getAllBookmarks(): org.json.JSONArray {
        if (!isNativeLoaded) return org.json.JSONArray()
        return try {
            org.json.JSONArray(bookmarkGetAll())
        } catch (e: Exception) {
            org.json.JSONArray()
        }
    }

    fun searchBookmarks(query: String): org.json.JSONArray {
        if (!isNativeLoaded) return org.json.JSONArray()
        return try {
            org.json.JSONArray(bookmarkSearch(query))
        } catch (e: Exception) {
            org.json.JSONArray()
        }
    }

    fun removeBookmark(id: String): Boolean {
        if (!isNativeLoaded || id.isBlank()) return false
        return try {
            bookmarkRemove(id)
        } catch (e: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // Downloads Subsystem API
    // -------------------------------------------------------------------------

    fun createDownload(
        url: String,
        filename: String = "",
        destinationPath: String = "",
        totalBytes: Long = 0L,
        mimeType: String = ""
    ): JSONObject? {
        if (!isNativeLoaded || url.isBlank()) return null
        return try {
            val res = downloadCreate(url, filename, destinationPath, totalBytes, mimeType)
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun updateDownloadProgress(
        id: String,
        downloadedBytes: Long,
        totalBytes: Long,
        state: String
    ): Boolean {
        if (!isNativeLoaded || id.isBlank()) return false
        return try {
            downloadUpdateProgress(id, downloadedBytes, totalBytes, state)
        } catch (e: Exception) {
            false
        }
    }

    fun getAllDownloads(): org.json.JSONArray {
        if (!isNativeLoaded) return org.json.JSONArray()
        return try {
            org.json.JSONArray(downloadGetAll())
        } catch (e: Exception) {
            org.json.JSONArray()
        }
    }

    fun deleteDownload(id: String): Boolean {
        if (!isNativeLoaded || id.isBlank()) return false
        return try {
            downloadDeleteItem(id)
        } catch (e: Exception) {
            false
        }
    }

    fun extractDownloadFilename(url: String, contentDisposition: String = "", mimeType: String = ""): String {
        if (!isNativeLoaded) return "download_${System.currentTimeMillis()}.bin"
        return try {
            downloadExtractFilename(url, contentDisposition, mimeType)
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}.bin"
        }
    }

    // -------------------------------------------------------------------------
    // Settings Subsystem API
    // -------------------------------------------------------------------------

    fun getSearchEngine(): String {
        if (!isNativeLoaded) return "duckduckgo"
        return try {
            settingsGetSearchEngine()
        } catch (e: Exception) {
            "duckduckgo"
        }
    }

    fun setSearchEngine(engine: String) {
        if (!isNativeLoaded || engine.isBlank()) return
        try {
            settingsSetSearchEngine(engine)
        } catch (e: Exception) {}
    }

    fun getRecentSearchQueries(): List<String> {
        if (!isNativeLoaded) return emptyList()
        return try {
            val arr = org.json.JSONArray(settingsGetRecentSearches())
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecentSearchQuery(query: String) {
        if (!isNativeLoaded || query.isBlank()) return
        try {
            settingsAddRecentSearch(query)
        } catch (e: Exception) {}
    }

    fun clearRecentSearchQueries() {
        if (!isNativeLoaded) return
        try {
            settingsClearRecentSearches()
        } catch (e: Exception) {}
    }

    // -------------------------------------------------------------------------
    // Tab Management Subsystem API
    // -------------------------------------------------------------------------

    fun tabOpenSession(
        url: String,
        title: String = "New Tab",
        isPrivate: Boolean = false,
        isTor: Boolean = false,
        groupId: String = "",
        groupTitle: String = ""
    ): JSONObject? {
        if (!isNativeLoaded) return null
        return try {
            val res = tabOpen(url, title, isPrivate, isTor, groupId, groupTitle)
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun tabCloseSession(id: String): JSONObject? {
        if (!isNativeLoaded || id.isBlank()) return null
        return try {
            val res = tabClose(id)
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun tabRestoreClosed(): JSONObject? {
        if (!isNativeLoaded) return null
        return try {
            val res = tabRestoreRecentlyClosed()
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun tabGetActiveSession(): JSONObject? {
        if (!isNativeLoaded) return null
        return try {
            val res = tabGetActive()
            if (res != "null") JSONObject(res) else null
        } catch (e: Exception) {
            null
        }
    }

    fun tabSetActiveSession(id: String): Boolean {
        if (!isNativeLoaded || id.isBlank()) return false
        return try {
            tabSetActive(id)
        } catch (e: Exception) {
            false
        }
    }

    fun tabUpdateSession(id: String, url: String, title: String): Boolean {
        if (!isNativeLoaded || id.isBlank()) return false
        return try {
            tabUpdate(id, url, title)
        } catch (e: Exception) {
            false
        }
    }

    fun tabGetAllSessions(): org.json.JSONArray {
        if (!isNativeLoaded) return org.json.JSONArray()
        return try {
            org.json.JSONArray(tabGetAll())
        } catch (e: Exception) {
            org.json.JSONArray()
        }
    }

    fun tabCloseAllSessions(includePrivate: Boolean = false) {
        if (!isNativeLoaded) return
        try {
            tabCloseAll(includePrivate)
        } catch (e: Exception) {}
    }

    fun tabClosePrivateSessions() {
        if (!isNativeLoaded) return
        try {
            tabClosePrivate()
        } catch (e: Exception) {}
    }

    fun tabAutoGroupDomains() {
        if (!isNativeLoaded) return
        try {
            tabAutoGroupByDomain()
        } catch (e: Exception) {}
    }

    fun tabExportPersistentJson(): String {
        if (!isNativeLoaded) return "[]"
        return try {
            tabExportJson()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun tabImportPersistentJson(jsonData: String): Int {
        if (!isNativeLoaded || jsonData.isBlank()) return 0
        return try {
            tabImportJson(jsonData)
        } catch (e: Exception) {
            0
        }
    }

    // -------------------------------------------------------------------------
    // Universal Protocol Dispatcher API
    // -------------------------------------------------------------------------

    fun dispatchProtocol(envelopeJson: String): JSONObject {
        if (!isNativeLoaded || envelopeJson.isBlank()) return JSONObject()
        return try {
            val responseJson = protocolDispatch(envelopeJson)
            JSONObject(responseJson)
        } catch (e: Exception) {
            JSONObject()
        }
    }
}
