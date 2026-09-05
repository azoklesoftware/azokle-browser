package com.azokle.search.browser.adblock

import android.content.Context
import android.content.SharedPreferences
import com.azokle.search.core.AzokleCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

data class PageShieldStats(
    val url: String = "",
    val host: String = "",
    val adsBlockedCount: Int = 0,
    val trackersBlockedCount: Int = 0,
    val isShieldEnabled: Boolean = true,
    val blockedDomainsList: List<String> = emptyList()
)

data class LifetimeShieldStats(
    val totalAdsBlocked: Long = 0L,
    val trackersBlocked: Long = 0L
) {
    val totalLifetimeBlocked: Long
        get() = totalAdsBlocked + trackersBlocked
}

object AdBlockStats {
    private const val PREFS_NAME = "azokle_shield_stats"
    private const val KEY_LIFETIME_ADS_BLOCKED = "lifetime_ads_blocked"
    private const val KEY_LIFETIME_TRACKERS_BLOCKED = "lifetime_trackers_blocked"

    private val isInitialized = AtomicBoolean(false)
    private var prefs: SharedPreferences? = null

    private val _currentStats = MutableStateFlow(PageShieldStats())
    val currentStats: StateFlow<PageShieldStats> = _currentStats.asStateFlow()

    private val _lifetimeStats = MutableStateFlow(LifetimeShieldStats())
    val lifetimeStats: StateFlow<LifetimeShieldStats> = _lifetimeStats.asStateFlow()

    fun init(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            val appContext = context.applicationContext ?: context
            val sp = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sp
            val savedAds = sp.getLong(KEY_LIFETIME_ADS_BLOCKED, 0L)
            val savedTrackers = sp.getLong(KEY_LIFETIME_TRACKERS_BLOCKED, 0L)

            // Synchronize with Rust native stats if loaded
            val rustStats = AzokleCore.shieldStats.value
            val initialAds = maxOf(savedAds, rustStats.adsBlocked.toLong())
            val initialTrackers = maxOf(savedTrackers, rustStats.trackersBlocked.toLong())

            _lifetimeStats.value = LifetimeShieldStats(
                totalAdsBlocked = initialAds,
                trackersBlocked = initialTrackers
            )
        }
    }

    fun resetForPage(url: String, host: String, isEnabled: Boolean) {
        _currentStats.value = PageShieldStats(
            url = url,
            host = host,
            adsBlockedCount = 0,
            trackersBlockedCount = 0,
            isShieldEnabled = isEnabled,
            blockedDomainsList = emptyList()
        )
    }

    fun recordBlock(blockedHost: String, categoryId: Int? = null) {
        val cleanHost = blockedHost.lowercase().trim()
        if (cleanHost.isBlank() ||
            cleanHost == "azokle.com" || cleanHost.endsWith(".azokle.com") || cleanHost.contains("azokle") ||
            cleanHost == "cloudflare.com" || cleanHost.endsWith(".cloudflare.com") ||
            cleanHost == "cloudflareinsights.com" || cleanHost.endsWith(".cloudflareinsights.com") ||
            cleanHost.contains("cloudflare")) {
            return
        }

        val isTracker = categoryId == 2 || AzokleCore.isTrackerHost(cleanHost) ||
                cleanHost.contains("analytics") || cleanHost.contains("telemetry") ||
                cleanHost.contains("tracker") || cleanHost.contains("pixel") ||
                cleanHost.contains("beacon") || cleanHost.contains("stats") ||
                cleanHost.contains("facebook") || cleanHost.contains("hotjar") ||
                cleanHost.contains("segment") || cleanHost.contains("mixpanel") ||
                cleanHost.contains("amplitude") || cleanHost.contains("sentry") ||
                cleanHost.contains("clarity") || cleanHost.contains("quantserve")

        val isAd = !isTracker
        val cat = if (isTracker) 2 else (categoryId ?: 1)
        AzokleCore.recordBlockedHost(cleanHost, cat)

        // 1. Update active page stats
        _currentStats.update { current ->
            val list = if (current.blockedDomainsList.contains(cleanHost)) {
                current.blockedDomainsList
            } else {
                (listOf(cleanHost) + current.blockedDomainsList).take(50)
            }
            current.copy(
                adsBlockedCount = if (isAd) current.adsBlockedCount + 1 else current.adsBlockedCount,
                trackersBlockedCount = if (isTracker) current.trackersBlockedCount + 1 else current.trackersBlockedCount,
                blockedDomainsList = list
            )
        }

        // 2. Update persistent lifetime stats
        _lifetimeStats.update { current ->
            val newAds = if (isAd) current.totalAdsBlocked + 1 else current.totalAdsBlocked
            val newTrackers = if (isTracker) current.trackersBlocked + 1 else current.trackersBlocked

            prefs?.edit()?.apply {
                if (isAd) putLong(KEY_LIFETIME_ADS_BLOCKED, newAds)
                if (isTracker) putLong(KEY_LIFETIME_TRACKERS_BLOCKED, newTrackers)
                apply()
            }

            LifetimeShieldStats(
                totalAdsBlocked = newAds,
                trackersBlocked = newTrackers
            )
        }
    }

    fun toggleShield(forcedState: Boolean? = null) {
        _currentStats.update { current ->
            current.copy(isShieldEnabled = forcedState ?: !current.isShieldEnabled)
        }
    }

    fun getTotalLifetimeBlocked(): Long {
        val rustTotal = AzokleCore.shieldStats.value.totalLifetimeBlocked.toLong()
        val flowTotal = _lifetimeStats.value.totalLifetimeBlocked
        return maxOf(rustTotal, flowTotal)
    }
}
