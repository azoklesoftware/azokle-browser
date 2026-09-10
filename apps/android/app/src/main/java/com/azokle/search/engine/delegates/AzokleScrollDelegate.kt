package com.azokle.search.engine.delegates

import com.azokle.search.engine.GeckoSwipeRefreshLayout
import org.mozilla.geckoview.GeckoSession

/**
 * Enterprise Scroll Delegate extracted from monolithic GeckoBrowserView.
 * Coordinates overscroll pacing and pull-to-refresh activation at scroll top.
 */
class AzokleScrollDelegate(
    private val swipeRefreshProvider: () -> GeckoSwipeRefreshLayout?,
    private val onScrollChanged: (scrollX: Int, scrollY: Int) -> Unit
) : GeckoSession.ScrollDelegate {

    override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
        onScrollChanged.invoke(scrollX, scrollY)
        val swipeRefresh = swipeRefreshProvider()
        swipeRefresh?.currentScrollY = scrollY
        swipeRefresh?.isContentAtTop = scrollY <= 15
    }
}
