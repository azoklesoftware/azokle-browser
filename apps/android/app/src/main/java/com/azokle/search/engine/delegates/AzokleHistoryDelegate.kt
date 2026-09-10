package com.azokle.search.engine.delegates

import com.azokle.search.history.HistoryManager
import com.azokle.search.tabs.TabManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Enterprise History & Navigation Stack Sync Delegate extracted from GeckoBrowserView.
 * Handles:
 * 1. Filtering redirects (VISIT_REDIRECT_PERMANENT, VISIT_REDIRECT_TEMPORARY) and internal about: schemes
 * 2. Asynchronous visit recording with private/Tor isolation awareness
 * 3. Back/Forward navigation stack state synchronization
 * 4. Session state persistence for tab restoration
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
        val isRedirect = (flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT) != 0 ||
                (flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY) != 0
        if (!isRedirect && url.isNotBlank() && !url.startsWith("about:", ignoreCase = true)) {
            HistoryManager.recordVisit(
                url = url,
                title = "",
                isPrivate = isPrivate || isTor
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
}
