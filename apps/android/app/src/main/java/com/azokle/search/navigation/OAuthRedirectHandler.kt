package com.azokle.search.navigation

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

        // Internal and dangerous schemes cannot be external OAuth callbacks
        if (scheme in UriClassifier.INTERNAL_SCHEMES || scheme in UriClassifier.DANGEROUS_SCHEMES) {
            return false
        }

        // Must be a valid custom application scheme
        if (!UriClassifier.isCustomAppScheme(scheme)) {
            return false
        }

        val queryIdx = trimmed.indexOf('?')
        val hashIdx = trimmed.indexOf('#')

        val query = if (queryIdx != -1) {
            val end = if (hashIdx != -1 && hashIdx > queryIdx) hashIdx else trimmed.length
            trimmed.substring(queryIdx + 1, end).lowercase()
        } else ""

        val fragment = if (hashIdx != -1) {
            trimmed.substring(hashIdx + 1).lowercase()
        } else ""

        val path = if (queryIdx != -1) {
            trimmed.substring(0, queryIdx).lowercase()
        } else if (hashIdx != -1) {
            trimmed.substring(0, hashIdx).lowercase()
        } else {
            trimmed.lowercase()
        }

        // Check query parameters and fragment parameters (for implicit OAuth / PKCE flows)
        val hasCoreOAuthParam = listOf("code=", "access_token=", "id_token=").any {
            query.contains(it) || fragment.contains(it)
        }

        val isExplicitOAuthPath = path.endsWith("/oauth2callback") ||
                path.endsWith("/oauth2redirect") ||
                path.endsWith("/oauth-callback") ||
                path.endsWith("/oauth/callback") ||
                path.endsWith("/auth/callback") ||
                path.endsWith("/callback") ||
                path.endsWith("/oauth")

        val hasStateOrError = (query.contains("state=") || fragment.contains("state=")) ||
                (query.contains("error=") || fragment.contains("error="))

        return hasCoreOAuthParam || (isExplicitOAuthPath && hasStateOrError)
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
