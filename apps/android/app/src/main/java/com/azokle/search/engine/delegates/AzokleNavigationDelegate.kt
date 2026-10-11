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

    /**
     * Handles on location change events.
     */
    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
        hasUserGesture: Boolean
    ) {
        if (com.azokle.search.incineration.DataBurnEngine.isBurning() ||
            com.azokle.search.history.HistoryManager.isIncinerating()
        ) {
            return
        }

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

        // 3. Android AppLinks for HTTP/HTTPS with explicit user click/tap (e.g. YouTube, Maps, Play Store, Reddit, Instagram)
        if ((scheme == "http" || scheme == "https") && request.hasUserGesture && !isTor && !isPrivate) {
            if (com.azokle.search.navigation.AppLinkHandler.hasSpecializedNativeHandler(context, uriStr)) {
                val launched = com.azokle.search.navigation.AppLinkHandler.openInNativeApp(context, uriStr)
                if (launched) {
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
            }
        }

        // 4. Standard web browsing, data/blob, and javascript schemes
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "blob" || scheme == "data" || scheme == "javascript") {
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        return GeckoResult.fromValue(AllowOrDeny.DENY)
    }

    /**
     * Handles on can go back events.
     */
    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        onCanGoBackChanged(canGoBack)
    }

    /**
     * Handles on can go forward events.
     */
    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
        onCanGoForwardChanged(canGoForward)
    }

    /**
     * Handles on new session events.
     */
    override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
        if (uri.isNotBlank()) {
            val scheme = UrlHelper.extractScheme(uri)?.lowercase()
            if (scheme != null && NavigationSecurity.isForbiddenWebScheme(scheme)) {
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
                    isTor = isTor,
                    selectAfterOpen = false
                )
            } else {
                TabManager.openTab(
                    url = targetUrl,
                    title = "New Tab",
                    isPrivate = isPrivate,
                    isTor = isTor,
                    selectAfterOpen = false
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

            // Switch to the new tab on main thread
            Handler(Looper.getMainLooper()).post {
                if (TabManager.activeTabId.value != newTabId) {
                    TabManager.selectTab(newTabId)
                }
            }

            GeckoResult.fromValue(newSession)
        } catch (e: Throwable) {
            android.util.Log.e("AzokleNavigationDelegate", "Failed to create new session for window.open / popup: ${e.message}", e)
            null
        }
    }

    /**
     * Handles on load error events.
     */
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

    /**
     * Checks whether page reload.
     */
    private fun isPageReload(uriStr: String, request: GeckoSession.NavigationDelegate.LoadRequest): Boolean {
        /**
         * Executes the normalize operation.
         */
        fun normalize(url: String?): String {
            if (url.isNullOrBlank()) return ""
            return url.substringBefore('#').trimEnd('/')
        }

        val targetNorm = normalize(uriStr)
        if (targetNorm.isBlank()) return false

        val lastNorm = normalize(lastLoadedUrl)
        if (lastNorm.isNotBlank() && targetNorm.equals(lastNorm, ignoreCase = true)) {
            return true
        }

        val tabUrlNorm = normalize(tabId?.let { TabManager.getTab(it)?.url })
        if (tabUrlNorm.isNotBlank() && targetNorm.equals(tabUrlNorm, ignoreCase = true)) {
            return true
        }

        val triggerNorm = normalize(request.triggerUri)
        if (triggerNorm.isNotBlank() && targetNorm.equals(triggerNorm, ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Checks whether internal platform navigation.
     */
    private fun isInternalPlatformNavigation(uriStr: String, request: GeckoSession.NavigationDelegate.LoadRequest): Boolean {
        /**
         * Executes the extract domain operation.
         */
        fun extractDomain(url: String?): String? {
            if (url.isNullOrBlank()) return null
            return try {
                val host = Uri.parse(url).host?.lowercase() ?: return null
                host.removePrefix("www.").removePrefix("m.")
            } catch (e: Exception) {
                null
            }
        }

        val targetDomain = extractDomain(uriStr) ?: return false
        if (targetDomain != "youtube.com" && targetDomain != "vimeo.com" && targetDomain != "youtu.be") {
            return false
        }

        val triggerDomain = extractDomain(request.triggerUri)
        val lastDomain = extractDomain(lastLoadedUrl)
        val tabDomain = extractDomain(tabId?.let { TabManager.getTab(it)?.url })

        /**
         * Executes the matches target operation.
         */
        fun matchesTarget(d: String?): Boolean {
            if (d == null) return false
            if (targetDomain == "youtube.com" || targetDomain == "youtu.be") {
                return d == "youtube.com" || d == "youtu.be"
            }
            return d == targetDomain
        }

        return matchesTarget(triggerDomain) || matchesTarget(lastDomain) || matchesTarget(tabDomain)
    }
}
