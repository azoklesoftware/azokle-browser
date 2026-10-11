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

package com.azokle.search.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdClickManagerTest {

    @Before
    fun setUp() {
        AdClickManager.clearAll()
    }

    @Test
    fun testCleanTrackingParameters_removesKnownTrackers() {
        val rawUrl = "https://example.com/product?item=123&fbclid=IwAR12345&utm_source=newsletter&utm_medium=email&gclid=Cj0KCQj&keep=true"
        val cleaned = AdClickManager.cleanTrackingParameters(rawUrl)

        assertFalse(cleaned.contains("fbclid"))
        assertFalse(cleaned.contains("utm_source"))
        assertFalse(cleaned.contains("utm_medium"))
        assertFalse(cleaned.contains("gclid"))
        assertTrue(cleaned.contains("item=123"))
        assertTrue(cleaned.contains("keep=true"))
    }

    @Test
    fun testCleanTrackingParameters_allParametersStrippedLeavesBareUrl() {
        val rawUrl = "https://example.com/page?utm_campaign=winter_sale&utm_source=twitter"
        val cleaned = AdClickManager.cleanTrackingParameters(rawUrl)

        assertEquals("https://example.com/page", cleaned)
    }

    @Test
    fun testCleanTrackingParameters_noTrackingParamsUnmodified() {
        val rawUrl = "https://example.com/search?q=kotlin+coroutine&page=2"
        val cleaned = AdClickManager.cleanTrackingParameters(rawUrl)

        assertEquals(rawUrl, cleaned)
    }

    @Test
    fun testCleanTrackingParameters_invalidOrBlankUrlHandledGracefully() {
        assertEquals("", AdClickManager.cleanTrackingParameters(""))
        assertEquals("   ", AdClickManager.cleanTrackingParameters("   "))
        assertEquals("not-a-url", AdClickManager.cleanTrackingParameters("not-a-url"))
    }

    @Test
    fun testAdClickAttribution_lifecycle() {
        val tabId = "tab_42"
        val landingUrl = "https://shop.store.com/checkout?gclid=ad123"

        assertFalse(AdClickManager.isAttributionActive(tabId))
        assertFalse(AdClickManager.isAttributionExempt(tabId, "shop.store.com"))

        // Register ad click
        AdClickManager.registerAdClick(tabId, landingUrl)

        assertTrue(AdClickManager.isAttributionActive(tabId))
        // Target domain is exempt
        assertTrue(AdClickManager.isAttributionExempt(tabId, "shop.store.com"))
        // Subdomain is exempt
        assertTrue(AdClickManager.isAttributionExempt(tabId, "api.shop.store.com"))
        // Unrelated third-party domain is NOT exempt
        assertFalse(AdClickManager.isAttributionExempt(tabId, "adservice.google.com"))

        // Explicit clear
        AdClickManager.clearAttribution(tabId)
        assertFalse(AdClickManager.isAttributionActive(tabId))
        assertFalse(AdClickManager.isAttributionExempt(tabId, "shop.store.com"))
    }

    @Test
    fun testGetActiveExemptions() {
        AdClickManager.registerAdClick("tab_1", "https://store.com/item?gclid=123")
        val exemptions = AdClickManager.getActiveExemptions()
        assertEquals(1, exemptions.size)
        assertEquals("tab_1", exemptions[0].tabId)
        assertEquals("store.com", exemptions[0].advertiserHost)
    }

    @Test
    fun testIsExempt_strictlyScopedToAdvertiserOrAttribution() {
        val tabId = "tab_100"
        AdClickManager.registerAdClick(tabId, "https://merchant.com/deal?gclid=xyz")

        // 1. First-party subresource on advertiser site is exempt
        assertTrue(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://merchant.com/style.css"))
        assertTrue(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://cdn.merchant.com/image.png"))

        // 2. Specific conversion attribution endpoint is exempt
        assertTrue(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://googleadservices.com/pagead/conversion/123"))
        assertTrue(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://bat.bing.com/action/0"))

        // 3. Unrelated third-party ad networks or trackers are NEVER exempt
        assertFalse(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://criteo.com/delivery/ajs.php"))
        assertFalse(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://taboola.com/feed"))
        assertFalse(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://facebook.com/tr?id=123"))
        assertFalse(AdClickManager.isExempt(tabId, "https://merchant.com/deal", "https://analytics.tiktok.com/pixel"))
    }
}
