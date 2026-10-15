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
        val sanitized = NavigationSecurity.sanitizeIntent(intent, context)

        // Strictly prohibit web content from self-targeting Azokle internal components (H-3)
        val targetPkg = sanitized.`package`
        if (targetPkg != null && (targetPkg.equals(context.packageName, ignoreCase = true) ||
                    targetPkg.equals("com.azokle.search", ignoreCase = true) ||
                    targetPkg.equals("com.azokle.browser", ignoreCase = true))) {
            return handleSecurityFailure(
                context = context,
                fallbackUrl = fallbackUrl,
                session = session,
                showToastOnError = showToastOnError
            )
        }

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

    /**
     * Handles launch failure.
     */
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

    /**
     * Handles security failure.
     */
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
