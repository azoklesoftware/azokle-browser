package com.azokle.search.engine.delegates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.azokle.search.engine.ui.BrowserErrorType
import com.azokle.search.navigation.ExternalAppLauncher
import com.azokle.search.navigation.ExternalNavigationHandler
import com.azokle.search.navigation.IntentResolver
import com.azokle.search.navigation.NavigationSecurity
import com.azokle.search.tabs.TabManager
import com.azokle.search.common.util.UrlHelper
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebRequestError

/**
 * Enterprise Navigation Delegate extracted from monolithic GeckoBrowserView.
 * Handles:
 * 1. Forbidden protocol firewall (file:, content:, chrome:, resource:, jar:)
 * 2. External URI dispatch (tel:, mailto:, sms:, geo:, upi:, intent:, market:)
 * 3. AppLink interceptor with Tor Mode isolation
 * 4. Categorized WebRequestError mapping
 * 5. Back/forward state synchronization
 */
class AzokleNavigationDelegate(
    private val context: Context,
    private val tabId: String?,
    private val isPrivate: Boolean,
    private val isTor: Boolean,
    private val onUrlChanged: (String) -> Unit,
    private val onLoadErrorChanged: (BrowserErrorType?) -> Unit,
    private val onCanGoBackChanged: (Boolean) -> Unit,
    private val onCanGoForwardChanged: (Boolean) -> Unit,
    private val onNewWindowRequested: (String) -> Unit,
    private val onRefreshStateChanged: ((Boolean) -> Unit)? = null
) : GeckoSession.NavigationDelegate {

    var lastLoadedUrl: String = ""
        private set

    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean
    ) {
        url?.let {
            if (it.isNotBlank() && !it.equals("about:blank", ignoreCase = true)) {
                lastLoadedUrl = it
                if (tabId != null) {
                    TabManager.markTabLoaded(tabId, it)
                }
                onUrlChanged(it)
                TabManager.updateActiveTab(it, "")
                onLoadErrorChanged(null)
            }
        }
    }

    /**
     * Evaluates candidate URI loads initiated by user typing, links, script redirects, or subframes.
     *
     * # Policy Enforcement Hierarchy:
     * 1. Firewall: Rejects forbidden local/internal schemes (`file:`, `content:`, `chrome:`, `resource:`).
     * 2. External URI Schemes: Dispatches `tel:`, `mailto:`, `sms:`, `geo:`, `upi:`, `intent:` to native apps
     *    with strict Tor/Private isolation guards.
     * 3. Sovereign Media: Rewrites eligible YouTube/Vimeo URLs to the tracking-free distraction-free player.
     * 4. Android AppLinks: Only offered if triggered by explicit user gesture and NOT in Tor/Private mode.
     * 5. Standard Web: Allows legitimate HTTP/HTTPS/data/blob navigations.
     */
    override fun onLoadRequest(
        session: GeckoSession,
        request: GeckoSession.NavigationDelegate.LoadRequest
    ): GeckoResult<AllowOrDeny>? {
        val uriStr = request.uri
        val scheme = try { Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }

        // 1. Block dangerous internal and privileged schemes
        if (NavigationSecurity.isForbiddenWebScheme(scheme)) {
            return GeckoResult.fromValue(AllowOrDeny.DENY)
        }

        // 2. External application navigation & AppLinks (tel, mailto, sms, geo, market, upi, intent, Play Store)
        if (ExternalNavigationHandler.isExternalNavigation(uriStr)) {
            val handled = ExternalNavigationHandler.handleNavigation(
                context = context,
                uriStr = uriStr,
                session = session,
                hasUserGesture = request.hasUserGesture,
                targetWindow = request.target,
                isPrivate = isPrivate,
                isTor = isTor
            )
            if (handled) {
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }
        }

        // 2.5 Azokle Sovereign Player Check for eligible videos (YouTube / Vimeo)
        if (com.azokle.search.media.AzoklePlayerHelper.isEligibleVideoUrl(uriStr)) {
            val prefMode = com.azokle.search.common.util.PreferencesManager(context).getAzoklePlayerMode()
            if (prefMode == "always") {
                val embedUrl = com.azokle.search.media.AzoklePlayerHelper.buildSovereignEmbedUrl(uriStr)
                if (embedUrl != null && embedUrl != uriStr) {
                    Handler(Looper.getMainLooper()).post {
                        session.loadUri(embedUrl)
                    }
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
            }
        }

        // 3. Android AppLinks for HTTP/HTTPS with explicit user click/tap (e.g. YouTube, Play Store, Reddit)
        if ((scheme == "http" || scheme == "https") && request.hasUserGesture && !isTor && !isPrivate) {
            val targetUri = try { Uri.parse(uriStr) } catch (e: Exception) { null }
            if (targetUri != null) {
                val viewIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                if (IntentResolver.hasNativeAppHandler(context, viewIntent)) {
                    val launched = ExternalAppLauncher.launch(
                        context = context,
                        intent = viewIntent,
                        fallbackUrl = uriStr,
                        session = session,
                        showToastOnError = false
                    )
                    if (launched == ExternalAppLauncher.LaunchResult.SUCCESS) {
                        return GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
                }
            }
        }

        // 4. Standard web browsing, data/blob, and javascript schemes
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "blob" || scheme == "data" || scheme == "javascript") {
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        return GeckoResult.fromValue(AllowOrDeny.DENY)
    }

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        onCanGoBackChanged(canGoBack)
    }

    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
        onCanGoForwardChanged(canGoForward)
    }

    override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
        if (uri.isNotBlank()) {
            val scheme = UrlHelper.extractScheme(uri)?.lowercase()
            val dangerousSchemes = setOf("file", "content", "chrome", "resource", "javascript")
            if (scheme != null && scheme in dangerousSchemes) {
                return null
            }
        }

        return try {
            val targetUrl = uri.ifBlank { "about:blank" }
            val parentId = tabId ?: ""
            val newTabId = if (parentId.isNotBlank()) {
                TabManager.openTabFromParent(
                    url = targetUrl,
                    title = "New Tab",
                    parentTabId = parentId,
                    isPrivate = isPrivate,
                    isTor = isTor
                )
            } else {
                TabManager.openTab(
                    url = targetUrl,
                    title = "New Tab",
                    isPrivate = isPrivate,
                    isTor = isTor
                )
            }

            if (targetUrl.isNotBlank() && !targetUrl.equals("about:blank", ignoreCase = true)) {
                TabManager.markTabLoaded(newTabId, targetUrl)
            }

            val newSession = TabManager.getOrCreateSession(
                id = newTabId,
                context = context,
                isPrivate = isPrivate,
                isTor = isTor
            )

            // Switch to the new tab only AFTER GeckoView completes its internal session opening
            Handler(Looper.getMainLooper()).postDelayed({
                if (newSession.isOpen && TabManager.activeTabId.value != newTabId) {
                    TabManager.selectTab(newTabId)
                }
            }, 500)

            GeckoResult.fromValue(newSession)
        } catch (e: Throwable) {
            android.util.Log.e("AzokleNavigationDelegate", "Failed to create new session for window.open / popup: ${e.message}", e)
            null
        }
    }

    override fun onLoadError(
        session: GeckoSession,
        uri: String?,
        error: WebRequestError
    ): GeckoResult<String>? {
        val failedUri = uri ?: ""
        val errorType = when (error.code) {
            WebRequestError.ERROR_UNKNOWN_HOST -> BrowserErrorType.dnsNotFound(failedUri)
            WebRequestError.ERROR_CONNECTION_REFUSED -> BrowserErrorType.connectionRefused(failedUri)
            WebRequestError.ERROR_NET_TIMEOUT -> BrowserErrorType.timeout(failedUri)
            WebRequestError.ERROR_OFFLINE -> BrowserErrorType.noInternet(failedUri)
            WebRequestError.ERROR_REDIRECT_LOOP -> BrowserErrorType.redirectLoop(failedUri)
            WebRequestError.ERROR_PORT_BLOCKED -> BrowserErrorType.portBlocked(failedUri)
            WebRequestError.ERROR_SECURITY_SSL,
            WebRequestError.ERROR_SECURITY_BAD_CERT -> BrowserErrorType.sslWarning(failedUri)
            else -> {
                if (error.category == WebRequestError.ERROR_CATEGORY_SECURITY) {
                    BrowserErrorType.sslWarning(failedUri)
                } else if (error.code == WebRequestError.ERROR_UNKNOWN_HOST) {
                    BrowserErrorType.dnsNotFound(failedUri)
                } else {
                    BrowserErrorType.generic(failedUri, "ERR_CODE_${error.code}")
                }
            }
        }
        Handler(Looper.getMainLooper()).post {
            onRefreshStateChanged?.invoke(false)
        }
        onLoadErrorChanged(errorType)
        return null
    }
}
