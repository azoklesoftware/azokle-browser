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

package com.azokle.search.navigation

/**
 * Classifies URIs into explicit semantic and security categories for Azokle Browser.
 */
object UriClassifier {

    enum class UriType {
        WEB_INTERNAL,       // http, https, about, blob, data, javascript
        DANGEROUS_INTERNAL, // file, content, chrome, resource
        TELEPHONY,          // tel, telprompt, callto
        MESSAGING,          // sms, smsto, mms, mmsto
        EMAIL,              // mailto
        MAPS,               // geo
        APP_STORE,          // market
        CALENDAR,           // webcal
        UPI_PAYMENT,        // upi, paytmmp, etc.
        INTENT,             // intent://
        CUSTOM_APP_SCHEME,  // any valid custom scheme (whatsapp, tg, slack, zoommtg, reverse-domain OAuth, etc.)
        UNKNOWN             // malformed or empty
    }

    val INTERNAL_SCHEMES = setOf(
        "http", "https", "about", "blob", "data", "javascript"
    )

    val DANGEROUS_SCHEMES = setOf(
        "file", "content", "chrome", "resource", "jar", "view-source"
    )

    val TELEPHONY_SCHEMES = setOf(
        "tel", "telprompt", "callto"
    )

    val MESSAGING_SCHEMES = setOf(
        "sms", "smsto", "mms", "mmsto"
    )

    val EMAIL_SCHEMES = setOf(
        "mailto"
    )

    val MAPS_SCHEMES = setOf(
        "geo"
    )

    val APP_STORE_SCHEMES = setOf(
        "market"
    )

    val CALENDAR_SCHEMES = setOf(
        "webcal"
    )

    val UPI_SCHEMES = setOf(
        "upi", "paytmmp", "phonepe", "gpay"
    )

    val KNOWN_SCHEMES = INTERNAL_SCHEMES + DANGEROUS_SCHEMES + TELEPHONY_SCHEMES +
            MESSAGING_SCHEMES + EMAIL_SCHEMES + MAPS_SCHEMES + APP_STORE_SCHEMES +
            CALENDAR_SCHEMES + UPI_SCHEMES + setOf("intent")

    /**
     * Extracts scheme safely from a raw URI string without throwing exceptions.
     */
    fun extractScheme(uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return null
        val trimmed = uriStr.trim()
        if (trimmed.startsWith("[")) return null // IPv6 literal

        val colonIdx = trimmed.indexOf(':')
        if (colonIdx <= 0) return null
        val candidate = trimmed.substring(0, colonIdx)

        if (candidate.contains('/') || candidate.contains('?') || candidate.contains('#')) {
            return null
        }
        if (candidate.equals("localhost", ignoreCase = true)) return null

        val candidateLower = candidate.lowercase()
        val afterColon = trimmed.substring(colonIdx + 1)

        // 1. If it's a recognized standard scheme, return it directly
        if (candidateLower in KNOWN_SCHEMES) {
            return candidateLower
        }

        // 2. If followed by :// or :/, it's an explicit hierarchical or reverse-domain custom scheme
        if (afterColon.startsWith("/") || afterColon.startsWith("//")) {
            if (candidate.first().isLetter() && candidate.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) {
                return candidateLower
            }
        }

        // 3. If after colon are digits followed by /, ?, or end, it's host:port (e.g. domain.com:8080, localhost:3000)
        val digits = afterColon.takeWhile { it.isDigit() }
        if (digits.isNotEmpty() && (digits.length == afterColon.length || afterColon[digits.length] == '/' || afterColon[digits.length] == '?')) {
            return null
        }

        // 4. Candidate with dots but not followed by / is a domain name
        if (candidate.contains('.')) {
            return null
        }

        // 5. Opaque custom scheme
        if (candidate.first().isLetter() && candidate.all { it.isLetterOrDigit() || it == '+' || it == '-' }) {
            return candidateLower
        }

        return null
    }

    /**
     * Classifies a URI string into a UriType.
     */
    fun classify(uriStr: String?): UriType {
        if (uriStr.isNullOrBlank()) return UriType.UNKNOWN
        val trimmed = uriStr.trim()

        if (trimmed.startsWith("intent:", ignoreCase = true)) {
            return UriType.INTENT
        }

        if (isPlayStoreUrl(trimmed)) {
            return UriType.APP_STORE
        }

        val scheme = extractScheme(trimmed) ?: return UriType.UNKNOWN

        return when {
            scheme in DANGEROUS_SCHEMES -> UriType.DANGEROUS_INTERNAL
            scheme in INTERNAL_SCHEMES -> UriType.WEB_INTERNAL
            scheme in TELEPHONY_SCHEMES -> UriType.TELEPHONY
            scheme in MESSAGING_SCHEMES -> UriType.MESSAGING
            scheme in EMAIL_SCHEMES -> UriType.EMAIL
            scheme in MAPS_SCHEMES -> UriType.MAPS
            scheme in APP_STORE_SCHEMES -> UriType.APP_STORE
            scheme in CALENDAR_SCHEMES -> UriType.CALENDAR
            scheme in UPI_SCHEMES -> UriType.UPI_PAYMENT
            isCustomAppScheme(scheme) -> UriType.CUSTOM_APP_SCHEME
            else -> UriType.UNKNOWN
        }
    }

    /**
     * Checks if a scheme is a valid custom application scheme.
     * Must start with an ASCII letter and contain only alphanumeric, +, -, or . characters.
     */
    fun isCustomAppScheme(scheme: String): Boolean {
        if (scheme.isBlank()) return false
        if (scheme in INTERNAL_SCHEMES || scheme in DANGEROUS_SCHEMES) return false
        if (scheme.equals("localhost", ignoreCase = true)) return false
        val first = scheme[0]
        if (!first.isLetter()) return false
        return scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }
    }

    /**
     * Returns true if the scheme requires external application handling.
     */
    fun isExternalNavigation(uriStr: String?): Boolean {
        val type = classify(uriStr)
        return when (type) {
            UriType.TELEPHONY,
            UriType.MESSAGING,
            UriType.EMAIL,
            UriType.MAPS,
            UriType.APP_STORE,
            UriType.CALENDAR,
            UriType.UPI_PAYMENT,
            UriType.INTENT,
            UriType.CUSTOM_APP_SCHEME -> true
            else -> false
        }
    }

    /**
     * Checks if a URI string represents a Google Play Store / Android Market link.
     */
    fun isPlayStoreUrl(uriStr: String?): Boolean {
        if (uriStr.isNullOrBlank()) return false
        val lower = uriStr.trim().lowercase()
        return (lower.startsWith("market://") ||
                lower.startsWith("https://play.google.com/") ||
                lower.startsWith("http://play.google.com/") ||
                lower.startsWith("https://market.android.com/") ||
                lower.startsWith("http://market.android.com/") ||
                lower.startsWith("https://play.app.goo.gl/") ||
                lower.startsWith("http://play.app.goo.gl/"))
    }

    /**
     * Returns true if the URI is strictly dangerous and forbidden from web-triggered navigation.
     */
    fun isDangerous(uriStr: String?): Boolean {
        return classify(uriStr) == UriType.DANGEROUS_INTERNAL
    }
}
