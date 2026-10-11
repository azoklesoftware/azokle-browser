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

    /**
     * Handles on scroll changed events.
     */
    override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
        onScrollChanged.invoke(scrollX, scrollY)
        val swipeRefresh = swipeRefreshProvider()
        swipeRefresh?.currentScrollY = scrollY
        swipeRefresh?.isContentAtTop = scrollY <= 15
    }
}
