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
}
