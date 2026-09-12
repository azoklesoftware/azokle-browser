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
}
