package com.azokle.search.browser.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.mozilla.geckoview.GeckoSession

/**
 * Handles OAuth callback redirects and authentication response flows securely.
 */
object OAuthRedirectHandler {

    private val OAUTH_PARAMS = setOf("code", "access_token", "id_token", "state", "error")

    /**
     * Checks if a URI represents an OAuth or authentication callback.
     */
    fun isOAuthCallback(uriStr: String?): Boolean {
        if (uriStr.isNullOrBlank()) return false
        val trimmed = uriStr.trim()
        val scheme = UriClassifier.extractScheme(trimmed) ?: return false

        // Internal browser schemes cannot be external OAuth callbacks
        if (scheme in UriClassifier.INTERNAL_SCHEMES || scheme in UriClassifier.DANGEROUS_SCHEMES) {
            return false
        }

        val queryIdx = trimmed.indexOf('?')
        val query = if (queryIdx != -1) trimmed.substring(queryIdx + 1).lowercase() else ""
        val path = (if (queryIdx != -1) trimmed.substring(0, queryIdx) else trimmed).lowercase()

        val hasOAuthParam = OAUTH_PARAMS.any { param ->
            query.contains("$param=") || query.startsWith("$param&") || query.endsWith("&$param")
        }

        val isAuthPath = path.contains("oauth") || path.contains("callback") || path.contains("auth")

        return hasOAuthParam || isAuthPath
    }

    /**
     * Safely routes an OAuth callback URI to the matching application.
     */
    fun handleOAuthRedirect(
        context: Context,
        uriStr: String,
        session: GeckoSession? = null
    ): Boolean {
        val uri = try { Uri.parse(uriStr.trim()) } catch (e: Exception) { return false }

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
