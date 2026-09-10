package com.azokle.search.browser.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdBlockEngineTest {

    @Before
    fun setUp() {
        AdBlockStats.resetForPage("https://example.com", "example.com", true)
    }

    @Test
    fun testAdBlockStats_resetForPage() {
        AdBlockStats.resetForPage("https://kick.com", "kick.com", true)
        val stats = AdBlockStats.currentStats.value

        assertEquals("https://kick.com", stats.url)
        assertEquals("kick.com", stats.host)
        assertEquals(0, stats.adsBlockedCount)
        assertEquals(0, stats.trackersBlockedCount)
        assertTrue(stats.isShieldEnabled)
        assertTrue(stats.blockedDomainsList.isEmpty())
    }

    @Test
    fun testAdBlockStats_recordAdAndTrackerSeparation() {
        AdBlockStats.resetForPage("https://news.com", "news.com", true)

        // Record Ad (Category 1)
        AdBlockStats.recordBlock("doubleclick.net", 1)
        var stats = AdBlockStats.currentStats.value
        assertEquals(1, stats.adsBlockedCount)
        assertEquals(0, stats.trackersBlockedCount)
        assertEquals(listOf("doubleclick.net"), stats.blockedDomainsList)

        // Record Tracker (Category 2)
        AdBlockStats.recordBlock("google-analytics.com", 2)
        stats = AdBlockStats.currentStats.value
        assertEquals(1, stats.adsBlockedCount)
        assertEquals(1, stats.trackersBlockedCount)
        assertTrue(stats.blockedDomainsList.contains("google-analytics.com"))
        assertTrue(stats.blockedDomainsList.contains("doubleclick.net"))
    }

    @Test
    fun testAdBlockStats_sovereignDomainsNeverRecorded() {
        AdBlockStats.resetForPage("https://search.azokle.com", "search.azokle.com", true)

        AdBlockStats.recordBlock("azokle.com", 1)
        AdBlockStats.recordBlock("search.azokle.com", 1)
        AdBlockStats.recordBlock("cloudflare.com", 1)
        AdBlockStats.recordBlock("challenges.cloudflare.com", 2)

        val stats = AdBlockStats.currentStats.value
        assertEquals(0, stats.adsBlockedCount)
        assertEquals(0, stats.trackersBlockedCount)
        assertTrue(stats.blockedDomainsList.isEmpty())
    }

    @Test
    fun testAdBlockStats_duplicateDomainsDeduplicated() {
        AdBlockStats.resetForPage("https://twitch.tv", "twitch.tv", true)

        AdBlockStats.recordBlock("adservice.google.com", 1)
        AdBlockStats.recordBlock("adservice.google.com", 1)
        AdBlockStats.recordBlock("adservice.google.com", 1)

        val stats = AdBlockStats.currentStats.value
        assertEquals(3, stats.adsBlockedCount)
        assertEquals(1, stats.blockedDomainsList.size)
        assertEquals("adservice.google.com", stats.blockedDomainsList.first())
    }

    @Test
    fun testAdBlockEngine_toggleHostShield() {
        val engine = AdBlockEngine.instance
        val testHost = "custom-test-site.org"

        // Initially enabled (not in disabled set)
        assertTrue(engine.isShieldEnabled(testHost))

        // Toggle off
        val stateAfterToggleOff = engine.toggleShield(testHost)
        assertFalse(stateAfterToggleOff)
        assertFalse(engine.isShieldEnabled(testHost))
        assertTrue(engine.getDisabledHostsList().contains(testHost))

        // Toggle back on
        val stateAfterToggleOn = engine.toggleShield(testHost)
        assertTrue(stateAfterToggleOn)
        assertTrue(engine.isShieldEnabled(testHost))
        assertFalse(engine.getDisabledHostsList().contains(testHost))
    }

    @Test
    fun testAdBlockStats_multiPlatformGenericTracking() {
        val platforms = listOf(
            "youtube.com", "kick.com", "twitch.tv", "facebook.com",
            "instagram.com", "x.com", "reddit.com", "vimeo.com"
        )

        for (platform in platforms) {
            AdBlockStats.resetForPage("https://$platform", platform, true)
            AdBlockStats.recordBlock("ads.$platform", 1)
            AdBlockStats.recordBlock("telemetry.$platform", 2)

            val stats = AdBlockStats.currentStats.value
            assertEquals(1, stats.adsBlockedCount)
            assertEquals(1, stats.trackersBlockedCount)
            assertEquals(2, stats.blockedDomainsList.size)
        }
    }
}
