package com.azokle.search.engine.delegates

import com.azokle.search.history.HistoryManager
import com.azokle.search.tabs.TabManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Sovereign History & Navigation Stack Sync Delegate for Azokle Browser.
 * Handles:
 * 1. Strict filtering of HTTP redirects (VISIT_REDIRECT_PERMANENT, VISIT_REDIRECT_TEMPORARY) and unrecoverable errors
 * 2. Asynchronous visit recording with strict Private & Tor zero-leakage isolation
 * 3. Back/Forward navigation stack state synchronization
 * 4. Tab session state persistence for instant restoration
 */
class AzokleHistoryDelegate(
    private val tabId: String?,
    private val isPrivate: Boolean,
    private val isTor: Boolean,
    private val onCanGoBackChanged: (Boolean) -> Unit,
    private val onCanGoForwardChanged: (Boolean) -> Unit
) : GeckoSession.HistoryDelegate {

    override fun onVisited(
        session: GeckoSession,
        url: String,
        lastVisitedURL: String?,
        flags: Int
    ): GeckoResult<Boolean>? {
        // Zero disk or state writes if running in a private tab, Tor circuit session, or during active data burn
        if (isPrivate || isTor || HistoryManager.isIncinerating() || com.azokle.search.incineration.DataBurnEngine.isBurning()) {
            return GeckoResult.fromValue(true)
        }

        val isRedirect = (flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT) != 0 ||
                (flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY) != 0
        val isError = (flags and GeckoSession.HistoryDelegate.VISIT_UNRECOVERABLE_ERROR) != 0

        if (!isRedirect && !isError && url.isNotBlank() && !isIgnoredUrl(url)) {
            val activeTab = TabManager.activeTab
            if (activeTab != null && (activeTab.isPrivate || activeTab.isTor)) {
                return GeckoResult.fromValue(true)
            }

            HistoryManager.recordVisit(
                url = url,
                title = "",
                isPrivate = false
            )
        }
        return GeckoResult.fromValue(true)
    }

    override fun onHistoryStateChange(
        session: GeckoSession,
        historyList: GeckoSession.HistoryDelegate.HistoryList
    ) {
        onCanGoBackChanged(historyList.currentIndex > 0)
        onCanGoForwardChanged(historyList.currentIndex < historyList.size - 1)
        if (tabId != null && historyList is GeckoSession.SessionState) {
            TabManager.updateTabSessionState(tabId, historyList)
        }
    }

    private fun isIgnoredUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.startsWith("about:") ||
                lower.startsWith("chrome:") ||
                lower.startsWith("javascript:") ||
                lower.startsWith("data:") ||
                lower.startsWith("blob:") ||
                lower.startsWith("file:") ||
                lower.startsWith("resource:") ||
                lower.startsWith("intent:")
    }
}
