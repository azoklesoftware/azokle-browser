package com.azokle.search.navigation

import android.content.Intent
import android.net.Uri

/**
 * Parses and sanitizes Android intent:// URIs and external navigation requests.
 */
object IntentParser {

    private val PACKAGE_NAME_REGEX = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$""")

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

    private fun parseIntentUri(uriStr: String): ParsedIntent? {
        val pkg = extractPackageFromFragment(uriStr)
        val rawFallback = extractFallbackFromFragment(uriStr)
        val safeFallback = sanitizeFallbackUrl(rawFallback)
        val scheme = extractSchemeFromFragment(uriStr)
            ?: UriClassifier.extractScheme(uriStr)

        val cleanIntent = try {
            val intent = Intent.parseUri(uriStr, Intent.URI_INTENT_SCHEME)
            intent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addCategory(Intent.CATEGORY_BROWSABLE)
                if (pkg != null) {
                    `package` = pkg
                }
                component = null
                selector = null
                clipData = null
                val dataScheme = data?.scheme?.lowercase()
                if (dataScheme != null && NavigationSecurity.isForbiddenWebScheme(dataScheme)) {
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

    private fun parseStandardUri(uriStr: String): ParsedIntent? {
        val scheme = UriClassifier.extractScheme(uriStr) ?: return null

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

    fun isValidPackageName(pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        if (pkg.length > 256) return false
        return PACKAGE_NAME_REGEX.matches(pkg)
    }

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
            val raw = uriStr.substring(start, end).trim()
            return decodeUriComponent(raw)
        }
        val start = idx + marker.length
        val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
        val raw = uriStr.substring(start, end).trim()
        return decodeUriComponent(raw)
    }

    fun extractPackageFromFragment(uriStr: String): String? {
        val marker = "package="
        val idx = uriStr.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
        val raw = uriStr.substring(start, end).trim()
        return raw.takeIf { isValidPackageName(it) }
    }

    fun extractSchemeFromFragment(uriStr: String): String? {
        val marker = "scheme="
        val idx = uriStr.indexOf(marker)
        if (idx == -1) return null
        val start = idx + marker.length
        val end = uriStr.indexOf(';', start).takeIf { it != -1 } ?: uriStr.length
        val raw = uriStr.substring(start, end).trim()
        return raw.lowercase().takeIf { UriClassifier.isCustomAppScheme(it) }
    }

    private fun decodeUriComponent(raw: String): String {
        return try {
            java.net.URLDecoder.decode(raw, "UTF-8")
        } catch (e: Exception) {
            raw
        }
    }
}
