package com.azokle.search.browser.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.mozilla.geckoview.GeckoSession

/**
 * Executes external application intent dispatching safely with fallback handling.
 */
object ExternalAppLauncher {

    enum class LaunchResult {
        SUCCESS,
        FALLBACK_LOADED,
        NO_HANDLER,
        SECURITY_REJECTED,
        FAILED
    }

    /**
     * Launches an external activity using a sanitized Intent.
     */
    fun launch(
        context: Context,
        intent: Intent,
        fallbackUrl: String? = null,
        session: GeckoSession? = null,
        showToastOnError: Boolean = true
    ): LaunchResult {
        val sanitized = NavigationSecurity.sanitizeIntent(intent)

        try {
            context.startActivity(sanitized)
            return LaunchResult.SUCCESS
        } catch (e: ActivityNotFoundException) {
            return handleLaunchFailure(
                context = context,
                intent = sanitized,
                fallbackUrl = fallbackUrl,
                session = session,
                showToastOnError = showToastOnError
            )
        } catch (e: SecurityException) {
            return handleSecurityFailure(
                context = context,
                fallbackUrl = fallbackUrl,
                session = session,
                showToastOnError = showToastOnError
            )
        } catch (e: Exception) {
            return LaunchResult.FAILED
        }
    }

    private fun handleLaunchFailure(
        context: Context,
        intent: Intent,
        fallbackUrl: String?,
        session: GeckoSession?,
        showToastOnError: Boolean
    ): LaunchResult {
        // 1. If fallback URL is present, load in GeckoSession
        val safeFallback = IntentParser.sanitizeFallbackUrl(fallbackUrl)
        if (safeFallback != null && session != null) {
            Handler(Looper.getMainLooper()).post {
                session.loadUri(safeFallback)
            }
            return LaunchResult.FALLBACK_LOADED
        }

        // 2. Special fallback for market:// links without Play Store app -> use Web Play Store
        val dataStr = intent.dataString
        if (dataStr != null && dataStr.startsWith("market://details?", ignoreCase = true)) {
            val webPlayUrl = dataStr.replaceFirst("market://details?", "https://play.google.com/store/apps/details?")
            if (session != null) {
                Handler(Looper.getMainLooper()).post {
                    session.loadUri(webPlayUrl)
                }
                return LaunchResult.FALLBACK_LOADED
            }
        }

        // 3. Inform user
        if (showToastOnError) {
            val scheme = intent.scheme ?: intent.data?.scheme ?: "app"
            val readableName = when (scheme.lowercase()) {
                "tel", "telprompt", "callto" -> "phone dialer"
                "mailto" -> "email app"
                "sms", "smsto", "mms", "mmsto" -> "messaging app"
                "geo" -> "maps app"
                "market" -> "app store"
                "upi" -> "UPI payment app"
                "webcal" -> "calendar app"
                else -> "application"
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No $readableName found to handle this link", Toast.LENGTH_SHORT).show()
            }
        }

        return LaunchResult.NO_HANDLER
    }

    private fun handleSecurityFailure(
        context: Context,
        fallbackUrl: String?,
        session: GeckoSession?,
        showToastOnError: Boolean
    ): LaunchResult {
        val safeFallback = IntentParser.sanitizeFallbackUrl(fallbackUrl)
        if (safeFallback != null && session != null) {
            Handler(Looper.getMainLooper()).post {
                session.loadUri(safeFallback)
            }
            return LaunchResult.FALLBACK_LOADED
        }

        if (showToastOnError) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Cannot open external application securely", Toast.LENGTH_SHORT).show()
            }
        }
        return LaunchResult.SECURITY_REJECTED
    }
}
