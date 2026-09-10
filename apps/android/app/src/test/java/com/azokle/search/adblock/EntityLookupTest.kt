package com.azokle.search.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityLookupTest {

    @Test
    fun testLookup_googleDomains() {
        val testDomains = listOf(
            "google-analytics.com",
            "doubleclick.net",
            "googlesyndication.com",
            "adservice.google.com",
            "googletagmanager.com",
            "sub.google-analytics.com"
        )

        for (domain in testDomains) {
            val entity = EntityLookup.resolve(domain)
            assertEquals("Alphabet Inc.", entity.companyName)
        }
    }

    @Test
    fun testLookup_metaDomains() {
        val testDomains = listOf(
            "connect.facebook.net",
            "pixel.facebook.com",
            "facebook.net",
            "sub.connect.facebook.net"
        )

        for (domain in testDomains) {
            val entity = EntityLookup.resolve(domain)
            assertEquals("Meta Platforms, Inc.", entity.companyName)
            assertEquals(TrackerCategory.SOCIAL, entity.category)
        }
    }

    @Test
    fun testLookup_amazonDomains() {
        val testDomains = listOf(
            "amazon-adsystem.com",
            "aax.amazon-adsystem.com",
            "adservice.amazon.com"
        )

        for (domain in testDomains) {
            val entity = EntityLookup.resolve(domain)
            assertEquals("Amazon.com, Inc.", entity.companyName)
            assertEquals(TrackerCategory.ADVERTISING, entity.category)
        }
    }

    @Test
    fun testLookup_microsoftDomains() {
        val testDomains = listOf(
            "bat.bing.com",
            "clarity.ms",
            "adnxs.com",
            "appnexus.com"
        )

        for (domain in testDomains) {
            val entity = EntityLookup.resolve(domain)
            assertEquals("Microsoft Corporation", entity.companyName)
        }
    }

    @Test
    fun testLookup_criteoAndTaboolaAndOutbrain() {
        assertEquals("Criteo SA", EntityLookup.resolve("static.criteo.net").companyName)
        assertEquals("Taboola, Inc.", EntityLookup.resolve("trc.taboola.com").companyName)
        assertEquals("Outbrain Inc.", EntityLookup.resolve("widgets.outbrain.com").companyName)
        assertEquals("ByteDance Ltd.", EntityLookup.resolve("analytics.tiktok.com").companyName)
    }

    @Test
    fun testLookup_categories() {
        assertEquals(TrackerCategory.ANALYTICS, EntityLookup.resolve("google-analytics.com").category)
        assertEquals(TrackerCategory.ADVERTISING, EntityLookup.resolve("doubleclick.net").category)
        assertEquals(TrackerCategory.ANALYTICS, EntityLookup.resolve("hotjar.com").category)
        assertEquals(TrackerCategory.ANALYTICS, EntityLookup.resolve("clarity.ms").category)
        assertEquals(TrackerCategory.SOCIAL, EntityLookup.resolve("connect.facebook.net").category)
        assertEquals(TrackerCategory.CONTENT, EntityLookup.resolve("taboola.com").category)
        assertEquals(TrackerCategory.DATA_BROKER, EntityLookup.resolve("liveramp.com").category)
    }

    @Test
    fun testLookup_unknownDomainFallback() {
        val generalDomain = "unknownexampledomain.com"
        val generalEntity = EntityLookup.resolve(generalDomain)
        assertNotNull(generalEntity)
        assertEquals(TrackerCategory.GENERAL, generalEntity.category)
        assertEquals("Unknownexampledomain", generalEntity.companyName)

        val trackerDomain = "customtelemetrytracker.co.uk"
        val trackerEntity = EntityLookup.resolve(trackerDomain)
        assertNotNull(trackerEntity)
        assertEquals(TrackerCategory.DATA_BROKER, trackerEntity.category)
    }
}
