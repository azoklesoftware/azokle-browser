package com.azokle.search.browser.handlers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.mozilla.geckoview.GeckoSession

/**
 * Handles external URL schemes (tel:, mailto:, sms:, geo:, market:, intent:).
 *
 * Security & Reliability Best Practices:
 * - Uses Intent.ACTION_DIAL for tel: (does not require CALL_PHONE permission, opens dialer safely).
 * - Uses Intent.ACTION_SENDTO for mailto: and sms: (restricts targets to email & messaging apps).
 * - Avoids Intent.CATEGORY_BROWSABLE on tel/sms/mailto (dialers & SMS apps do not declare it).
 * - Clears component and selector on intent: schemes to prevent unauthorized component launches.
 * - Handles ActivityNotFoundException gracefully with user-facing notification.
 */
object ExternalSchemeHandler {

    val SAFE_EXTERNAL_SCHEMES = setOf(
        "tel",
        "mailto",
        "sms",
        "smsto",
        "mms",
        "mmsto",
        "geo",
        "market",
        "intent"
    )

    fun isExternalScheme(uriStr: String): Boolean {
        val trimmed = uriStr.trim()
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex <= 0) return false
        val candidate = trimmed.substring(0, colonIndex).lowercase()
        return candidate in SAFE_EXTERNAL_SCHEMES
    }

    /**
     * Dispatches an external URI to the appropriate native Android application.
     * Returns true if the scheme was recognized and processed, false otherwise.
     */
    fun handleExternalUri(
        context: Context,
        uriStr: String,
        session: GeckoSession? = null,
        showToastOnError: Boolean = true
    ): Boolean {
        val uri = try {
            Uri.parse(uriStr.trim())
        } catch (e: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme !in SAFE_EXTERNAL_SCHEMES) return false

        try {
            when (scheme) {
                "tel" -> {
                    val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                "mailto" -> {
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                "sms", "smsto", "mms", "mmsto" -> {
                    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                "geo", "market" -> {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return true
                }
                "intent" -> {
                    val intent = Intent.parseUri(uriStr, Intent.URI_INTENT_SCHEME).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        component = null
                        selector = null
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    try {
                        context.startActivity(intent)
                        return true
                    } catch (e: ActivityNotFoundException) {
                        if (!fallbackUrl.isNullOrBlank() &&
                            (fallbackUrl.startsWith("http://", ignoreCase = true) || fallbackUrl.startsWith("https://", ignoreCase = true))
                        ) {
                            Handler(Looper.getMainLooper()).post {
                                session?.loadUri(fallbackUrl)
                            }
                            return true
                        }
                        if (showToastOnError) {
                            Toast.makeText(context, "No app found to handle this request", Toast.LENGTH_SHORT).show()
                        }
                        return true
                    }
                }
            }
        } catch (e: ActivityNotFoundException) {
            if (showToastOnError) {
                val appType = when (scheme) {
                    "tel" -> "phone dialer"
                    "mailto" -> "email app"
                    "sms", "smsto", "mms", "mmsto" -> "messaging app"
                    "geo" -> "maps app"
                    "market" -> "app store"
                    else -> "app"
                }
                Toast.makeText(context, "No $appType installed to handle this link", Toast.LENGTH_SHORT).show()
            }
            return true
        } catch (e: Exception) {
            return false
        }
        return false
    }
}
