package com.azokle.search.adblock

import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Tab-scoped Ad Click & Conversion Exemption Entry.
 * Mirrors DuckDuckGo's ethical ad attribution model: Allows conversion verification
 * strictly within the origin tab and destination advertiser domain, expiring automatically.
 */
data class AdClickExemption(
    val tabId: String,
    val advertiserHost: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val expiresAtMs: Long = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24)
)

/**
 * Sovereign Tracking Parameter Cleanser and Tab-Scoped Ad Click Attribution Manager.
 * 1. Sanitizes incoming navigation URLs by stripping telemetry link decorators (fbclid, gclid, utm_*, etc.)
 * 2. Manages isolated tab-level exemptions for explicit sponsored ad clicks with strict TTL expiration.
 */
object AdClickManager {

    private val activeExemptions = ConcurrentHashMap<String, AdClickExemption>()

    // High-confidence tracking parameters commonly appended to URLs
    private val TRACKING_PARAMS = setOf(
        // Google Ads & Analytics
        "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "_ga", "_gl",
        // Meta / Facebook / Instagram
        "fbclid", "fref", "igshid",
        // Microsoft / Bing
        "msclkid",
        // Twitter / X
        "twclid",
        // TikTok / Snapchat / Pinterest
        "ttclid", "sc_lid", "epik",
        // Yandex / Marketing
        "yclid", "ym_debug", "mc_cid", "mc_eid"
    )

    /**
     * Extracts host name without relying on Android framework classes.
     */
    fun extractHost(rawUrl: String): String? {
        if (rawUrl.isBlank()) return null
        return try {
            val uri = URI(rawUrl)
            uri.host?.lowercase() ?: run {
                val withoutScheme = rawUrl.substringAfter("://")
                withoutScheme.substringBefore('/').substringBefore('?').substringBefore(':').lowercase().takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            val withoutScheme = rawUrl.substringAfter("://")
            withoutScheme.substringBefore('/').substringBefore('?').substringBefore(':').lowercase().takeIf { it.isNotBlank() }
        }
    }

    /**
     * Strips all tracking parameters and UTM tags from an incoming URL.
     * Preserves legitimate query parameters (search query, video ID, pagination, etc.).
     */
    fun cleanTrackingParameters(rawUrl: String): String {
        if (rawUrl.isBlank() || !rawUrl.contains('?')) {
            return rawUrl
        }

        // Delegate to high-performance canonical native Rust tracker cleanser if available
        try {
            val cleaned = com.azokle.search.core.AzokleCore.cleanTrackingParameters(rawUrl)
            if (cleaned.isNotBlank() && cleaned != rawUrl) {
                return cleaned
            }
        } catch (_: Throwable) {
            // Fall back to pure-Kotlin cleanser (e.g. during JVM unit tests or before native library initialization)
        }

        return try {
            val qIndex = rawUrl.indexOf('?')
            val baseUrl = rawUrl.substring(0, qIndex)
            val fullQueryAndHash = rawUrl.substring(qIndex + 1)

            val hashIndex = fullQueryAndHash.indexOf('#')
            val query = if (hashIndex != -1) fullQueryAndHash.substring(0, hashIndex) else fullQueryAndHash
            val hash = if (hashIndex != -1) fullQueryAndHash.substring(hashIndex) else ""

            val queryPairs = query.split('&').filter { it.isNotBlank() }
            val retainedPairs = mutableListOf<String>()

            for (pair in queryPairs) {
                val key = pair.substringBefore('=').lowercase()
                if (!TRACKING_PARAMS.contains(key) && !key.startsWith("utm_")) {
                    retainedPairs.add(pair)
                }
            }

            if (retainedPairs.isEmpty()) {
                baseUrl + hash
            } else {
                "$baseUrl?${retainedPairs.joinToString("&")}$hash"
            }
        } catch (e: Exception) {
            rawUrl
        }
    }

    /**
     * Detects if the URL was decorated with an ad-click parameter.
     */
    fun hasAdClickParameter(rawUrl: String): Boolean {
        if (rawUrl.isBlank() || !rawUrl.contains('?')) return false
        return try {
            val query = rawUrl.substringAfter('?').substringBefore('#')
            query.split('&').any { pair ->
                val key = pair.substringBefore('=').lowercase()
                key == "gclid" || key == "fbclid" || key == "msclkid" || key == "ttclid"
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Registers a tab-scoped exemption for an advertiser when an ad is clicked.
     */
    fun registerAdClick(tabId: String, landingUrl: String) {
        val host = extractHost(landingUrl) ?: return
        val key = "$tabId:$host"
        activeExemptions[key] = AdClickExemption(tabId = tabId, advertiserHost = host)
        syncToWebExtension()
    }

    /**
     * Checks if an exemption exists and is active for the given tab and document/advertiser host.
     */
    fun isAttributionActive(tabId: String): Boolean {
        return activeExemptions.values.any { it.tabId == tabId && System.currentTimeMillis() <= it.expiresAtMs }
    }

    /**
     * Checks whether a specific host is exempt under this tab.
     */
    fun isAttributionExempt(tabId: String, host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        val exemption = activeExemptions.values.firstOrNull { it.tabId == tabId && (cleanHost == it.advertiserHost || cleanHost.endsWith(".${it.advertiserHost}")) }
            ?: return false

        if (System.currentTimeMillis() > exemption.expiresAtMs) {
            activeExemptions.remove("${tabId}:${exemption.advertiserHost}")
            syncToWebExtension()
            return false
        }
        return true
    }

    /**
     * Checks whether a subresource request is exempt from blocking due to an active ad click in this tab.
     */
    fun isExempt(tabId: String?, documentUrl: String?, targetUrl: String?): Boolean {
        if (tabId.isNullOrBlank() || documentUrl.isNullOrBlank() || targetUrl.isNullOrBlank()) return false

        val docHost = extractHost(documentUrl) ?: return false
        val key = "$tabId:$docHost"
        val exemption = activeExemptions[key] ?: return false

        // Check if exemption has expired
        if (System.currentTimeMillis() > exemption.expiresAtMs) {
            activeExemptions.remove(key)
            syncToWebExtension()
            return false
        }

        return true
    }

    fun getActiveExemptions(): List<AdClickExemption> {
        val now = System.currentTimeMillis()
        activeExemptions.values.removeIf { now > it.expiresAtMs }
        return activeExemptions.values.toList()
    }

    private fun syncToWebExtension() {
        try {
            com.azokle.search.engine.GeckoEngineManager.notifyAdClickExemptionsChanged(getActiveExemptions())
        } catch (e: Throwable) {
            // Ignored during testing or if engine is not running
        }
    }

    fun clearAttribution(tabId: String) {
        clearTab(tabId)
    }

    fun clearTab(tabId: String) {
        activeExemptions.keys.removeIf { it.startsWith("$tabId:") }
        syncToWebExtension()
    }

    fun clearExpired() {
        val now = System.currentTimeMillis()
        val removed = activeExemptions.values.removeIf { now > it.expiresAtMs }
        if (removed) {
            syncToWebExtension()
        }
    }

    fun clearAll() {
        activeExemptions.clear()
        syncToWebExtension()
    }
}
