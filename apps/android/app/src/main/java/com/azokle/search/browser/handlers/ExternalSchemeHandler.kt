package com.azokle.search.browser.handlers

import android.content.Context
import com.azokle.search.browser.navigation.ExternalNavigationHandler
import com.azokle.search.browser.navigation.UriClassifier
import org.mozilla.geckoview.GeckoSession

/**
 * Handles external URL schemes by delegating to the production-grade [ExternalNavigationHandler].
 * Maintains backwards compatibility for existing callers.
 */
object ExternalSchemeHandler {

    val SAFE_EXTERNAL_SCHEMES = setOf(
        "tel",
        "telprompt",
        "callto",
        "mailto",
        "sms",
        "smsto",
        "mms",
        "mmsto",
        "geo",
        "market",
        "webcal",
        "upi",
        "paytmmp",
        "intent"
    )

    fun isExternalScheme(uriStr: String): Boolean {
        return ExternalNavigationHandler.isExternalNavigation(uriStr)
    }

    /**
     * Dispatches an external URI to the appropriate native Android application.
     * Returns true if the scheme was recognized and processed, false otherwise.
     */
    fun handleExternalUri(
        context: Context,
        uriStr: String,
        session: GeckoSession? = null,
        showToastOnError: Boolean = true,
        hasUserGesture: Boolean = true,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): Boolean {
        return ExternalNavigationHandler.handleNavigation(
            context = context,
            uriStr = uriStr,
            session = session,
            hasUserGesture = hasUserGesture,
            isPrivate = isPrivate,
            isTor = isTor
        )
    }
}

