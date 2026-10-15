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

import android.content.Intent
import android.net.Uri

/**
 * Parses and sanitizes Android intent:// URIs and external navigation requests.
 */
object IntentParser {

    private val PACKAGE_NAME_REGEX = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")

    private val SCHEME_REGEX = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*$""")

    data class ParsedIntent(
        val uri: Uri?,
        val action: String = Intent.ACTION_VIEW,
        val categories: Set<String> = setOf(Intent.CATEGORY_BROWSABLE),
        val packageName: String? = null,
        val componentName: String? = null,
        val fallbackUrl: String? = null,
        val scheme: String? = null,
        val stringExtras: Map<String, String> = emptyMap(),
        val androidIntent: Intent? = null
    )

    /**
     * Parses an intent:// or custom URI string into a safe ParsedIntent object.
     */
    fun parse(rawUriStr: String?): ParsedIntent? {
        if (rawUriStr.isNullOrBlank()) return null
        val trimmed = rawUriStr.trim()

        return if (trimmed.startsWith("intent:", ignoreCase = true)) {
            parseIntentUri(trimmed)
        } else {
            parseStandardUri(trimmed)
        }
    }

    /**
     * Parses intent uri from the input data.
     */
    private fun parseIntentUri(uriStr: String): ParsedIntent? {
        val pkg = extractPackageFromFragment(uriStr)
        // Strictly prohibit web content from self-targeting Azokle internal browser components (H-3)
        if (pkg != null && (pkg.equals("com.azokle.search", ignoreCase = true) || pkg.equals("com.azokle.browser", ignoreCase = true))) {
            return null
        }

        val rawFallback = extractFallbackFromFragment(uriStr)
        val safeFallback = sanitizeFallbackUrl(rawFallback)
        val rawScheme = extractSchemeFromFragment(uriStr)
            ?: UriClassifier.extractScheme(uriStr)
        val scheme = rawScheme?.takeIf { isValidScheme(it) && !NavigationSecurity.isForbiddenWebScheme(it) }

        val cleanIntent = try {
            val intent = Intent.parseUri(uriStr, Intent.URI_INTENT_SCHEME)
            intent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Unconditionally enforce CATEGORY_BROWSABLE to prevent invoking non-browsable exported activities (H-3)
                addCategory(Intent.CATEGORY_BROWSABLE)
                if (pkg != null) {
                    `package` = pkg
                }
                component = null
                selector = null
                clipData = null
                // Drop ALL attacker-controlled extras (S., i., B.) from intent schemes (H-3)
                replaceExtras(null as android.os.Bundle?)
                val dataScheme = data?.scheme?.lowercase()
                if (dataScheme != null && (NavigationSecurity.isForbiddenWebScheme(dataScheme) || !isValidScheme(dataScheme))) {
                    data = null
                }
            }
        } catch (e: Throwable) {
            null
        }

        return ParsedIntent(
            uri = null,
            action = Intent.ACTION_VIEW,
            categories = setOf(Intent.CATEGORY_BROWSABLE),
            packageName = pkg,
            componentName = null, // strictly stripped
            fallbackUrl = safeFallback,
            scheme = scheme,
            stringExtras = emptyMap(),
            androidIntent = cleanIntent
        )
    }

    /**
     * Parses standard uri from the input data.
     */
    private fun parseStandardUri(uriStr: String): ParsedIntent? {
        val scheme = UriClassifier.extractScheme(uriStr)?.takeIf { isValidScheme(it) } ?: return null

        if (NavigationSecurity.isForbiddenWebScheme(scheme)) {
            return null
        }

        val action = when (scheme) {
            "tel", "telprompt", "callto" -> Intent.ACTION_DIAL
            "mailto", "sms", "smsto", "mms", "mmsto" -> Intent.ACTION_SENDTO
            else -> Intent.ACTION_VIEW
        }

        val cleanIntent = try {
            val uri = Uri.parse(uriStr)
            Intent(action, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (action == Intent.ACTION_VIEW) {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                component = null
                selector = null
                clipData = null
                replaceExtras(null as android.os.Bundle?)
            }
        } catch (e: Throwable) {
            null
        }

        return ParsedIntent(
            uri = null,
            action = action,
            categories = if (action == Intent.ACTION_VIEW) setOf(Intent.CATEGORY_BROWSABLE) else emptySet(),
            packageName = null,
            componentName = null,
            fallbackUrl = null,
            scheme = scheme,
            androidIntent = cleanIntent
        )
    }

    /**
     * Checks whether valid package name.
     */
    fun isValidPackageName(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        if (pkg.length > 256) return false
        return PACKAGE_NAME_REGEX.matches(pkg)
    }

    /**
     * Checks whether valid scheme.
     */
    fun isValidScheme(scheme: String?): Boolean {
        if (scheme.isNullOrBlank()) return false
        if (scheme.length > 64) return false
        return SCHEME_REGEX.matches(scheme)
    }

    /**
     * Executes the sanitize fallback url operation.
     */
    fun sanitizeFallbackUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        val lower = trimmed.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return null
        }
        val withoutScheme = if (lower.startsWith("https://")) trimmed.substring(8) else trimmed.substring(7)
        val host = withoutScheme.takeWhile { it != '/' && it != ':' && it != '?' && it != '#' }
        if (host.isBlank()) return null
        return trimmed
    }

    /**
     * Executes the extract fallback from fragment operation.
     */
    fun extractFallbackFromFragment(uriStr: String): String? {
        val marker = "S.browser_fallback_url="
        val idx = uriStr.indexOf(marker)
        if (idx == -1) {
            // Also check alternate notation: browser_fallback_url=
            val altMarker = "browser_fallback_url="
            val altIdx = uriStr.indexOf(altMarker)
            if (altIdx == -1) return null
            val start = altIdx + altMarker.length
            val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
            if (start >= end || start >= uriStr.length) return null
            val raw = uriStr.substring(start, end).trim()
            return decodeUriComponent(raw)
        }
        val start = idx + marker.length
        val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
        if (start >= end || start >= uriStr.length) return null
        val raw = uriStr.substring(start, end).trim()
        return decodeUriComponent(raw)
    }

    /**
     * Executes the extract package from fragment operation.
     */
    fun extractPackageFromFragment(uriStr: String): String? {
        val marker = "package="
        val idx = uriStr.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
        if (start >= end || start >= uriStr.length) return null
        val raw = uriStr.substring(start, end).trim()
        return raw.takeIf { isValidPackageName(it) }
    }

    /**
     * Executes the extract scheme from fragment operation.
     */
    fun extractSchemeFromFragment(uriStr: String): String? {
        val marker = "scheme="
        val idx = uriStr.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
        if (start >= end || start >= uriStr.length) return null
        val raw = uriStr.substring(start, end).trim().lowercase()
        return raw.takeIf { isValidScheme(it) && UriClassifier.isCustomAppScheme(it) }
    }

    /**
     * Executes the decode uri component operation.
     */
    private fun decodeUriComponent(raw: String): String {
        if (raw.isBlank()) return raw
        return try {
            java.net.URLDecoder.decode(raw, "UTF-8")
        } catch (e: Throwable) {
            raw
        }
    }
}
