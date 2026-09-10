package com.azokle.search.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.mozilla.geckoview.GeckoSession

/**
 * Production-grade external navigation coordinator for Azokle Browser.
 * Hardened against intent injection, scheme hijacking, malicious redirects,
 * and Tor/Private mode clearnet identity leaks.
 */
object ExternalNavigationHandler {

    /**
     * Checks if a URI string requires external navigation rather than internal GeckoView rendering.
     */
    fun isExternalNavigation(uriStr: String?): Boolean {
        if (uriStr.isNullOrBlank()) return false
        val trimmed = uriStr.trim()

        if (trimmed.startsWith("intent:", ignoreCase = true)) {
            return true
        }

        if (isPlayStoreLink(trimmed)) {
            return true
        }

        val type = UriClassifier.classify(trimmed)
        return when (type) {
            UriClassifier.UriType.TELEPHONY,
            UriClassifier.UriType.MESSAGING,
            UriClassifier.UriType.EMAIL,
            UriClassifier.UriType.MAPS,
            UriClassifier.UriType.APP_STORE,
            UriClassifier.UriType.CALENDAR,
            UriClassifier.UriType.UPI_PAYMENT,
            UriClassifier.UriType.CUSTOM_APP_SCHEME -> true
            else -> false
        }
    }

    /**
     * Checks if an HTTP/HTTPS URI is a direct Google Play Store link.
     */
    fun isPlayStoreLink(uriStr: String?): Boolean {
        if (uriStr.isNullOrBlank()) return false
        val lower = uriStr.trim().lowercase()
        return (lower.startsWith("https://play.google.com/store/apps/") ||
                lower.startsWith("http://play.google.com/store/apps/") ||
                lower.startsWith("https://market.android.com/details") ||
                lower.startsWith("http://market.android.com/details"))
    }

    /**
     * Main dispatch method for all navigation requests in Azokle Browser.
     *
     * @param context Android context
     * @param uriStr Target URI
     * @param session Active GeckoSession for fallback URL loading
     * @param hasUserGesture True if triggered by direct user interaction (tap, click)
     * @param targetWindow Window target type from GeckoView navigation delegate
     * @param isPrivate True if browsing in private mode
     * @param isTor True if browsing over Tor network
     * @param onTorConfirmationRequired Optional callback when Tor mode requires explicit user confirmation
     * @return True if navigation was handled externally or blocked securely, False if GeckoView should load internally.
     */
    fun handleNavigation(
        context: Context,
        uriStr: String?,
        session: GeckoSession? = null,
        hasUserGesture: Boolean = true,
        targetWindow: Int = 0,
        isPrivate: Boolean = false,
        isTor: Boolean = false,
        onTorConfirmationRequired: (((proceed: () -> Unit) -> Unit))? = null
    ): Boolean {
        if (uriStr.isNullOrBlank()) return false
        val trimmed = uriStr.trim()

        // 1. Strict blocking of dangerous internal schemes
        if (NavigationSecurity.isForbiddenWebScheme(UriClassifier.extractScheme(trimmed))) {
            return true // blocked
        }

        // 2. Classify URI
        val isPlayStore = isPlayStoreLink(trimmed)
        val type = if (isPlayStore) UriClassifier.UriType.APP_STORE else UriClassifier.classify(trimmed)

        // Web internal schemes (http, https, about, blob, data, javascript) are handled by Gecko
        // unless it is an explicit external AppLink/PlayStore link
        if (type == UriClassifier.UriType.WEB_INTERNAL && !isPlayStore) {
            return false
        }

        if (type == UriClassifier.UriType.UNKNOWN && !isPlayStore) {
            return false
        }

        // 3. Enforce user gesture for external application launches
        if (!NavigationSecurity.isGestureAllowed(hasUserGesture, targetWindow)) {
            return true // blocked unauthorized auto-redirect
        }

        // 4. Tor Mode Privacy Guard
        // Leaving the browser in Tor mode to open a clearnet Android app bypasses Tor proxying.
        if (NavigationSecurity.isTorIsolationRisk(isTor)) {
            if (onTorConfirmationRequired != null) {
                onTorConfirmationRequired {
                    dispatchExternalUri(context, trimmed, session, type)
                }
                return true
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "External application launch blocked to prevent Tor IP address leakage",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return true
            }
        }

        // 5. Normal dispatch
        return dispatchExternalUri(context, trimmed, session, type)
    }

    private fun dispatchExternalUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?,
        type: UriClassifier.UriType
    ): Boolean {
        return when (type) {
            UriClassifier.UriType.UPI_PAYMENT -> {
                UpiHandler.handlePayment(context, uriStr, session)
            }

            UriClassifier.UriType.INTENT -> {
                handleIntentUri(context, uriStr, session)
            }

            UriClassifier.UriType.TELEPHONY -> {
                handleTelephonyUri(context, uriStr, session)
            }

            UriClassifier.UriType.MESSAGING -> {
                handleMessagingUri(context, uriStr, session)
            }

            UriClassifier.UriType.EMAIL -> {
                handleEmailUri(context, uriStr, session)
            }

            UriClassifier.UriType.MAPS -> {
                handleMapsUri(context, uriStr, session)
            }

            UriClassifier.UriType.APP_STORE -> {
                handleMarketUri(context, uriStr, session)
            }

            UriClassifier.UriType.CALENDAR -> {
                handleCalendarUri(context, uriStr, session)
            }

            UriClassifier.UriType.CUSTOM_APP_SCHEME -> {
                if (OAuthRedirectHandler.isOAuthCallback(uriStr)) {
                    OAuthRedirectHandler.handleOAuthRedirect(context, uriStr, session)
                } else {
                    handleCustomScheme(context, uriStr, session)
                }
            }

            else -> false
        }
    }

    private fun handleIntentUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val parsed = IntentParser.parse(uriStr) ?: return false
        val androidIntent = parsed.androidIntent

        if (androidIntent != null) {
            val launchResult = ExternalAppLauncher.launch(
                context = context,
                intent = androidIntent,
                fallbackUrl = parsed.fallbackUrl,
                session = session,
                showToastOnError = false
            )
            if (launchResult == ExternalAppLauncher.LaunchResult.SUCCESS ||
                launchResult == ExternalAppLauncher.LaunchResult.FALLBACK_LOADED) {
                return true
            }
        }

        // Fallback handling if app is not installed
        if (!parsed.fallbackUrl.isNullOrBlank() && session != null) {
            Handler(Looper.getMainLooper()).post {
                session.loadUri(parsed.fallbackUrl)
            }
            return true
        }

        // If package is specified, allow user to view app on Play Store
        if (!parsed.packageName.isNullOrBlank()) {
            val marketUri = Uri.parse("market://details?id=${parsed.packageName}")
            val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val playStoreResult = ExternalAppLauncher.launch(
                context = context,
                intent = marketIntent,
                session = session,
                showToastOnError = true
            )
            return playStoreResult == ExternalAppLauncher.LaunchResult.SUCCESS ||
                   playStoreResult == ExternalAppLauncher.LaunchResult.FALLBACK_LOADED
        }

        return ExternalAppLauncher.launch(
            context = context,
            intent = androidIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)),
            fallbackUrl = parsed.fallbackUrl,
            session = session,
            showToastOnError = true
        ) != ExternalAppLauncher.LaunchResult.FAILED
    }

    private fun handleTelephonyUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val uri = try { Uri.parse(uriStr) } catch (e: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: return false
        val ssp = uri.schemeSpecificPart

        // Normalize telprompt: and callto: to tel: for universal Android dialer support
        val normalizedUri = if (scheme == "telprompt" || scheme == "callto") {
            Uri.parse("tel:$ssp")
        } else {
            uri
        }

        val intent = Intent(Intent.ACTION_DIAL, normalizedUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }

    private fun handleMessagingUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val uri = try { Uri.parse(uriStr) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }

    private fun handleEmailUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val uri = try { Uri.parse(uriStr) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }

    private fun handleMapsUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val uri = try { Uri.parse(uriStr) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }

    private fun handleMarketUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val targetUri = try {
            val lower = uriStr.trim().lowercase()
            if (lower.startsWith("https://play.google.com/store/apps/details?") || lower.startsWith("http://play.google.com/store/apps/details?")) {
                val query = uriStr.substringAfter('?')
                Uri.parse("market://details?$query")
            } else if (lower.startsWith("https://market.android.com/details?") || lower.startsWith("http://market.android.com/details?")) {
                val query = uriStr.substringAfter('?')
                Uri.parse("market://details?$query")
            } else {
                Uri.parse(uriStr)
            }
        } catch (e: Exception) { return false }

        val intent = Intent(Intent.ACTION_VIEW, targetUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            `package` = "com.android.vending" // Directly target Google Play Store
        }

        var result = ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            fallbackUrl = if (targetUri.scheme == "market") uriStr else null,
            session = session,
            showToastOnError = false
        )

        // If direct targeting failed (e.g. non-Google Android device with alternate store), retry without package
        if (result == ExternalAppLauncher.LaunchResult.NO_HANDLER || result == ExternalAppLauncher.LaunchResult.FAILED) {
            val genericIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            result = ExternalAppLauncher.launch(
                context = context,
                intent = genericIntent,
                fallbackUrl = uriStr,
                session = session,
                showToastOnError = false
            )
        }

        return result == ExternalAppLauncher.LaunchResult.SUCCESS ||
               result == ExternalAppLauncher.LaunchResult.FALLBACK_LOADED
    }

    private fun handleCalendarUri(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val uri = try { Uri.parse(uriStr) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // If no native calendar handles webcal://, convert to https:// and load in session
        val handlers = IntentResolver.resolveHandlers(context, intent)
        if (handlers.isEmpty() && session != null) {
            val httpsUrl = "https://" + uriStr.substringAfter("webcal://")
            Handler(Looper.getMainLooper()).post {
                session.loadUri(httpsUrl)
            }
            return true
        }

        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }

    private fun handleCustomScheme(
        context: Context,
        uriStr: String,
        session: GeckoSession?
    ): Boolean {
        val uri = try { Uri.parse(uriStr) } catch (e: Exception) { return false }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        return ExternalAppLauncher.launch(
            context = context,
            intent = intent,
            session = session,
            showToastOnError = true
        ) == ExternalAppLauncher.LaunchResult.SUCCESS
    }
}
