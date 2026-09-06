package com.azokle.search.util

import android.content.Context
import android.net.Uri

object UrlHelper {
    private val TLD_REGEX = Regex("""^([a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}(:[0-9]{1,5})?(/.*)?$""")
    private val IP_REGEX = Regex("""^(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?::[0-9]{1,5})?(/.*)?$""")
    private val IPV6_REGEX = Regex("""^\[(?:[0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}\](?::[0-9]{1,5})?(/.*)?$""")

    private val ALLOWED_SCHEMES = setOf("https", "http", "about")

    // Surveillance & Ad-Tech Query Tokens (Brave / Firefox ETP / PrivacyBadger Standard)
    private val TRACKING_PARAMS = hashSetOf(
        "fbclid", "igshid", "gclid", "gbraid", "wbraid", "_ga", "_gl", "_gl_s",
        "twclid", "mc_eid", "mc_cid", "msclkid", "yclid", "zanpid", "sc_cookie",
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
        "utm_name", "utm_cid", "utm_reader", "utm_userid", "utm_brand",
        "hsCtsTracking", "_hsenc", "_hsmi", "_openstat", "wickedid",
        "ttclid", "irclid", "sc_customer", "s_cid", "mkt_tok", "dclid",
        "adposition", "kwid", "adid", "ref_src", "partner_id", "affiliate_id",
        "tag_id", "campid", "customid", "trk_contact", "trk_msg", "trk_module"
    )

    /**
     * RFC 3986 compliant scheme extractor. Differentiates URI schemes from host:port and IPv6.
     */
    fun extractScheme(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.startsWith("[")) return null

        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) return null

        val candidate = trimmed.substring(0, colonIndex)
        if (candidate.contains('.') || candidate.contains('/')) return null

        val afterColon = trimmed.substring(colonIndex + 1)
        val digits = afterColon.takeWhile { it.isDigit() }
        if (digits.isNotEmpty() && (digits.length == afterColon.length || afterColon[digits.length] == '/' || afterColon[digits.length] == '?')) {
            return null
        }

        if (candidate.first().isLetter() && candidate.all { it.isLetterOrDigit() || it == '+' || it == '-' }) {
            return candidate.lowercase()
        }
        return null
    }

    fun isAllowedScheme(url: String): Boolean {
        val scheme = extractScheme(url)
        return scheme == null || scheme in ALLOWED_SCHEMES
    }

    fun stripTrackingParameters(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || trimmed.startsWith("about:", ignoreCase = true)) return trimmed

        return try {
            val questionIdx = trimmed.indexOf('?')
            if (questionIdx == -1) return trimmed

            val base = trimmed.substring(0, questionIdx)
            val queryWithFragment = trimmed.substring(questionIdx + 1)
            val hashIdx = queryWithFragment.indexOf('#')
            val query = if (hashIdx != -1) queryWithFragment.substring(0, hashIdx) else queryWithFragment
            val fragment = if (hashIdx != -1) queryWithFragment.substring(hashIdx) else ""

            if (query.isBlank()) return trimmed

            val params = query.split('&').filter { it.isNotBlank() }
            var hasTracking = false
            val retained = mutableListOf<String>()

            for (p in params) {
                val key = p.substringBefore('=').lowercase()
                if (key in TRACKING_PARAMS) {
                    hasTracking = true
                } else {
                    retained.add(p)
                }
            }

            if (!hasTracking) return trimmed

            if (retained.isEmpty()) {
                "$base$fragment"
            } else {
                "$base?${retained.joinToString("&")}$fragment"
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    fun isDirectUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.contains(" ") || trimmed.isEmpty()) return false

        // Block dangerous schemes
        val scheme = extractScheme(trimmed)
        if (scheme != null && scheme !in ALLOWED_SCHEMES) {
            return false
        }

        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("about:blank", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true) ||
            trimmed.startsWith("localhost:", ignoreCase = true) ||
            trimmed.startsWith("localhost/", ignoreCase = true) ||
            trimmed.equals("localhost", ignoreCase = true)
        ) {
            return true
        }

        return TLD_REGEX.matches(trimmed) || IP_REGEX.matches(trimmed) || IPV6_REGEX.matches(trimmed)
    }

    fun getSearchEngineQueryUrl(context: Context?): String {
        if (context != null) {
            try {
                val engineId = com.azokle.search.data.PreferencesManager(context).getSearchEngine()
                val provider = com.azokle.search.data.SEARCH_ENGINE_PROVIDERS.find { it.id == engineId }
                if (provider != null) return provider.queryUrl
            } catch (e: Exception) {
                // Safe fallback
            }
        }
        return com.azokle.search.data.SEARCH_ENGINE_PROVIDERS[0].queryUrl
    }

    fun resolveSearchOrUrl(
        context: Context?,
        input: String,
        basePath: String = "/search"
    ): String {
        val prefs = context?.let { com.azokle.search.data.PreferencesManager(it) }
        val engineId = prefs?.getSearchEngine() ?: "azokle"
        val queryUrl = com.azokle.search.data.SEARCH_ENGINE_PROVIDERS.find { it.id == engineId }?.queryUrl
            ?: com.azokle.search.data.SEARCH_ENGINE_PROVIDERS[0].queryUrl
        val locationCode = prefs?.getLocation() ?: "auto"
        val safeSearch = prefs?.getSafeSearch() ?: "1"

        return resolveSearchOrUrl(
            input = input,
            searchEngineQueryUrl = queryUrl,
            locationCode = locationCode,
            safeSearch = safeSearch,
            basePath = basePath
        )
    }

    fun resolveSearchOrUrl(
        input: String,
        searchEngineQueryUrl: String = "https://search.azokle.com/search?q=",
        locationCode: String = "auto",
        safeSearch: String = "1",
        basePath: String = "/search"
    ): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        // Pass external application schemes (tel, mailto, sms, geo, market, intent) directly
        if (com.azokle.search.browser.handlers.ExternalSchemeHandler.isExternalScheme(trimmed)) {
            return trimmed
        }

        // Block dangerous protocol injections
        val scheme = extractScheme(trimmed)
        if (scheme != null && scheme !in ALLOWED_SCHEMES) {
            return buildSearchUrl(trimmed, searchEngineQueryUrl, locationCode, safeSearch, basePath)
        }

        if (isDirectUrl(trimmed)) {
            val resolvedUrl = when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("about:blank", ignoreCase = true) -> trimmed
                else -> "https://$trimmed"
            }
            return stripTrackingParameters(resolvedUrl)
        }

        return buildSearchUrl(trimmed, searchEngineQueryUrl, locationCode, safeSearch, basePath)
    }

    private fun buildSearchUrl(
        query: String,
        searchEngineQueryUrl: String,
        locationCode: String,
        safeSearch: String,
        basePath: String
    ): String {
        val encodedQuery = try { java.net.URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }
        return if (searchEngineQueryUrl.contains("azokle.com")) {
            val encodedLc = try { java.net.URLEncoder.encode(locationCode, "UTF-8") } catch (e: Exception) { locationCode }
            val encodedSafe = try { java.net.URLEncoder.encode(safeSearch, "UTF-8") } catch (e: Exception) { safeSearch }
            "https://search.azokle.com$basePath?q=$encodedQuery&lc=$encodedLc&safesearch=$encodedSafe"
        } else {
            "${searchEngineQueryUrl}${encodedQuery}"
        }
    }
}
