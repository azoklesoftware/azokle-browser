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

import android.net.Uri
import com.azokle.search.adblock.AdBlockStats
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoSession

/**
 * Enterprise Content Blocking & Shield Delegate extracted from GeckoBrowserView.
 * Records ad/tracker block events in AdBlockStats for UI shields.
 */
class AzokleContentBlockingDelegate : ContentBlocking.Delegate {
    /**
     * Handles on content blocked events.
     */
    override fun onContentBlocked(
        session: GeckoSession,
        event: ContentBlocking.BlockEvent
    ) {
        try {
            val uriStr = event.uri
            if (!uriStr.isNullOrBlank()) {
                val host = try { Uri.parse(uriStr).host ?: uriStr } catch (e: Exception) { uriStr }
                val isAntiTracking = try { event.antiTrackingCategory != 0 } catch (e: Exception) { false }
                val cat = if (isAntiTracking) 2 else null
                AdBlockStats.recordBlock(host, cat)
            }
        } catch (e: Exception) {
            // Fallback
        }
    }
}
