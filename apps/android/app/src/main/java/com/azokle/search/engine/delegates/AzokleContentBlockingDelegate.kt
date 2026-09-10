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
