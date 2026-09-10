package com.azokle.search.adblock

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

enum class ThreatType(val label: String, val description: String) {
    PHISHING("Deceptive Site Ahead", "Attackers may attempt to trick you into revealing sensitive information such as passwords, phone numbers, or credit card details."),
    MALWARE("Harmful Software Detected", "This website has been identified as a distribution endpoint for malicious software or device exploitation."),
    CRYPTO_DRAINER("Cryptojacking & Asset Drainer", "This site contains scripts that attempt unauthorized cryptocurrency mining or Web3 wallet draining."),
    NONE("Safe", "No known security threats detected.")
}

data class ThreatAssessment(
    val url: String,
    val host: String,
    val isThreat: Boolean,
    val threatType: ThreatType
)

/**
 * Sovereign Malicious Site & Phishing Guard.
 * Inspired by DuckDuckGo's `malicious-site-protection` architecture.
 * Protects users from visiting known deceptive, credential-harvesting, or malware domains
 * with zero remote URL leaking (all assessments occur in-memory).
 */
object SafeBrowsingGuard {

    private val userAllowedThreats = ConcurrentHashMap.newKeySet<String>()

    // High-confidence deceptive, phishing, and malware domains
    private val KNOWN_MALICIOUS_DOMAINS = mapOf(
        "paypal-security-verification.com" to ThreatType.PHISHING,
        "appleid-support-recover.com" to ThreatType.PHISHING,
        "google-login-verify-account.com" to ThreatType.PHISHING,
        "chase-online-secure-auth.com" to ThreatType.PHISHING,
        "metamask-wallet-validate.org" to ThreatType.CRYPTO_DRAINER,
        "ledger-drainer-sync.io" to ThreatType.CRYPTO_DRAINER,
        "phantom-wallet-connect-airdrop.net" to ThreatType.CRYPTO_DRAINER,
        "android-security-system-update.apk-download.net" to ThreatType.MALWARE,
        "cleaner-boost-battery-virus.xyz" to ThreatType.MALWARE,
        "cracked-software-keygen-installer.ru" to ThreatType.MALWARE
    )

    // Suspicious deceptive patterns commonly seen in automated phishing kits
    private val PHISHING_KEYWORDS = listOf(
        "account-verification-login",
        "update-billing-security",
        "wallet-drainer",
        "web3-airdrop-claim-free",
        "secure-banking-token-login"
    )

    /**
     * Evaluates whether a URL presents an active security threat.
     */
    fun assessUrl(rawUrl: String): ThreatAssessment {
        if (rawUrl.isBlank()) {
            return ThreatAssessment(rawUrl, "", false, ThreatType.NONE)
        }

        val host = extractHost(rawUrl) ?: return ThreatAssessment(rawUrl, "", false, ThreatType.NONE)
        val cleanHost = host.lowercase().trim()

        // If user already acknowledged and bypassed this warning for the session
        if (userAllowedThreats.contains(cleanHost)) {
            return ThreatAssessment(rawUrl, cleanHost, false, ThreatType.NONE)
        }

        // 1. Exact match against verified malicious dataset
        KNOWN_MALICIOUS_DOMAINS[cleanHost]?.let { threat ->
            return ThreatAssessment(rawUrl, cleanHost, true, threat)
        }

        // 2. Subdomain walk
        for ((knownHost, threat) in KNOWN_MALICIOUS_DOMAINS) {
            if (cleanHost.endsWith(".$knownHost")) {
                return ThreatAssessment(rawUrl, cleanHost, true, threat)
            }
        }

        // 3. Heuristic pattern detection on deceptive subdomains/paths
        val lowerUrl = rawUrl.lowercase()
        for (keyword in PHISHING_KEYWORDS) {
            if (cleanHost.contains(keyword) || lowerUrl.contains(keyword)) {
                return ThreatAssessment(rawUrl, cleanHost, true, ThreatType.PHISHING)
            }
        }

        return ThreatAssessment(rawUrl, cleanHost, false, ThreatType.NONE)
    }

    /**
     * Temporarily allows the user to visit an assessed threat domain for the current session.
     */
    fun bypassThreatForSession(host: String) {
        val cleanHost = host.lowercase().trim()
        if (cleanHost.isNotBlank()) {
            userAllowedThreats.add(cleanHost)
        }
    }

    fun clearSessionBypasses() {
        userAllowedThreats.clear()
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
