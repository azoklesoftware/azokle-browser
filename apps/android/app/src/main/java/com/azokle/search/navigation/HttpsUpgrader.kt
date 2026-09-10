package com.azokle.search.navigation

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface contract for HTTPS Upgrade Engine.
 */
interface IHttpsUpgrader {
    fun shouldUpgrade(rawUrl: String): Boolean
    fun upgrade(rawUrl: String): String
    fun recordUpgradeFailure(host: String)
    fun addException(host: String)
    fun removeException(host: String)
    fun isException(host: String): Boolean
}

/**
 * Sovereign HTTPS Pre-Flight Upgrader.
 * Inspired by DuckDuckGo's `httpsupgrade` architecture:
 * Upgrades unencrypted `http://` navigations to encrypted `https://` prior to issuing
 * network packets over the wire, protecting user search queries, credentials, and browsing
 * traffic from cleartext interception and ISP manipulation.
 *
 * Includes automatic local network detection and failure fallback to prevent breakage.
 */
object HttpsUpgrader : IHttpsUpgrader {

    private val userExceptions = ConcurrentHashMap.newKeySet<String>()
    private val failedUpgradeHosts = ConcurrentHashMap.newKeySet<String>()

    /**
     * Determines whether an incoming URL is eligible for pre-flight HTTPS upgrade.
     */
    override fun shouldUpgrade(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false
        val trimmed = rawUrl.trim()

        if (!trimmed.startsWith("http://", ignoreCase = true)) {
            return false
        }

        val host = extractHost(trimmed) ?: return false

        // 1. Never upgrade Tor onion domains or local intranet hosts
        if (isLocalOrOnionHost(host)) {
            return false
        }

        // 2. Check if user explicitly exempted this host
        if (isException(host)) {
            return false
        }

        // 3. Check if previous HTTPS connection failed for this host in current session
        if (failedUpgradeHosts.contains(host)) {
            return false
        }

        return true
    }

    /**
     * Converts an unencrypted HTTP URL to an encrypted HTTPS URL.
     */
    override fun upgrade(rawUrl: String): String {
        if (!shouldUpgrade(rawUrl)) return rawUrl
        val trimmed = rawUrl.trim()
        return "https://" + trimmed.substring(7)
    }

    override fun recordUpgradeFailure(host: String) {
        val cleanHost = host.lowercase().trim()
        if (cleanHost.isNotBlank()) {
            failedUpgradeHosts.add(cleanHost)
        }
    }

    override fun addException(host: String) {
        val cleanHost = host.lowercase().trim()
        if (cleanHost.isNotBlank()) {
            userExceptions.add(cleanHost)
        }
    }

    override fun removeException(host: String) {
        userExceptions.remove(host.lowercase().trim())
    }

    override fun isException(host: String): Boolean {
        val cleanHost = host.lowercase().trim()
        return userExceptions.contains(cleanHost) ||
                userExceptions.any { cleanHost.endsWith(".$it") }
    }

    fun clearSessionFailures() {
        failedUpgradeHosts.clear()
    }

    private fun isLocalOrOnionHost(host: String): Boolean {
        val lower = host.lowercase()
        return lower == "localhost" ||
                lower.endsWith(".local") ||
                lower.endsWith(".onion") ||
                lower == "127.0.0.1" ||
                lower.startsWith("10.") ||
                lower.startsWith("192.168.") ||
                (lower.startsWith("172.") && isPrivateSubnet172(lower))
    }

    private fun isPrivateSubnet172(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size >= 2) {
            val second = parts[1].toIntOrNull()
            if (second != null && second in 16..31) return true
        }
        return false
    }

    private fun extractHost(rawUrl: String): String? {
        return try {
            val uri = URI(rawUrl)
            uri.host?.lowercase() ?: rawUrl.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':').lowercase().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            rawUrl.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':').lowercase().takeIf { it.isNotBlank() }
        }
    }
}
