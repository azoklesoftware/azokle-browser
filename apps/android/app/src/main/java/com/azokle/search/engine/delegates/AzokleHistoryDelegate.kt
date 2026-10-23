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

    /**
     * Handles on visited events.
     */
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
            // AZK-BUG-05: Check session's own tab instead of foreground activeTab to prevent race conditions during tab switching
            val sessionTab = tabId?.let { id -> TabManager.tabs.value.find { it.id == id } }
            if (sessionTab != null && (sessionTab.isPrivate || sessionTab.isTor)) {
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

    /**
     * Handles on history state change events.
     */
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

    /**
     * Checks whether ignored url.
     */
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
