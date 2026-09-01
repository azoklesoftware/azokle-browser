package com.azokle.search.browser.adblock

import com.azokle.search.core.AzokleCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PageShieldStats(
    val url: String = "",
    val host: String = "",
    val adsBlockedCount: Int = 0,
    val trackersBlockedCount: Int = 0,
    val isShieldEnabled: Boolean = true,
    val blockedDomainsList: List<String> = emptyList()
)

object AdBlockStats {
    private val _currentStats = MutableStateFlow(PageShieldStats())
    val currentStats: StateFlow<PageShieldStats> = _currentStats.asStateFlow()

    private val totalLifetimeBlocked = java.util.concurrent.atomic.AtomicInteger(0)

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
        totalLifetimeBlocked.incrementAndGet()

        val isTracker = categoryId == 2 || AzokleCore.isTrackerHost(cleanHost) ||
                cleanHost.contains("analytics") || cleanHost.contains("telemetry") ||
                cleanHost.contains("tracker") || cleanHost.contains("pixel") ||
                cleanHost.contains("beacon") || cleanHost.contains("stats") ||
                cleanHost.contains("facebook") || cleanHost.contains("hotjar") ||
                cleanHost.contains("segment") || cleanHost.contains("mixpanel") ||
                cleanHost.contains("amplitude") || cleanHost.contains("sentry") ||
                cleanHost.contains("clarity") || cleanHost.contains("quantserve")

        val cat = if (isTracker) 2 else (categoryId ?: 1)
        AzokleCore.recordBlockedHost(cleanHost, cat)

        _currentStats.update { current ->
            val list = if (current.blockedDomainsList.contains(cleanHost)) {
                current.blockedDomainsList
            } else {
                (listOf(cleanHost) + current.blockedDomainsList).take(50)
            }
            current.copy(
                adsBlockedCount = current.adsBlockedCount + 1,
                trackersBlockedCount = if (isTracker) current.trackersBlockedCount + 1 else current.trackersBlockedCount,
                blockedDomainsList = list
            )
        }
    }

    fun toggleShield(forcedState: Boolean? = null) {
        _currentStats.update { current ->
            current.copy(isShieldEnabled = forcedState ?: !current.isShieldEnabled)
        }
    }

    fun getTotalLifetimeBlocked(): Int {
        val rustTotal = AzokleCore.shieldStats.value.totalLifetimeBlocked
        return if (rustTotal > 0) rustTotal else totalLifetimeBlocked.get()
    }
}
