package com.azokle.search.engine

import com.azokle.search.common.util.UrlHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Web Compatibility and Web Standards regression test suite.
 * Verifies RFC 3986 URI parsing, scheme compliance, SafeSearch, and engine config integrity.
 */
class WebCompatibilitySuiteTest {

    @Test
    fun testRfc3986UriSchemeCompliance() {
        assertEquals("https", UrlHelper.extractScheme("https://azokle.com/search?q=gecko"))
        assertEquals("http", UrlHelper.extractScheme("http://example.org"))
        assertEquals("about", UrlHelper.extractScheme("about:blank"))
        assertEquals("azokle", UrlHelper.extractScheme("azokle://settings"))
        assertEquals("javascript", UrlHelper.extractScheme("javascript:void(0)"))
        assertEquals(null, UrlHelper.extractScheme("azokle.com"))
        assertEquals(null, UrlHelper.extractScheme("192.168.1.1:8080/path"))
    }

    @Test
    fun testTrackingParameterStripping() {
        val dirtyUrl = "https://example.com/shop?item=123&fbclid=IwAR0987&utm_source=adwords&utm_campaign=winter"
        val cleanUrl = UrlHelper.stripTrackingParameters(dirtyUrl)
        assertFalse(cleanUrl.contains("fbclid"))
        assertFalse(cleanUrl.contains("utm_source"))
        assertFalse(cleanUrl.contains("utm_campaign"))
        assertTrue(cleanUrl.contains("item=123"))
    }

    @Test
    fun testGeckoPerformanceConfigYamlGeneration() {
        val configYaml = GeckoConfigManager.generatePerformanceConfigYaml("cloudflare")
        assertNotNull(configYaml)
        assertTrue(configYaml.contains("browser.cache.disk.enable: true"))
        assertTrue(configYaml.contains("network.trr.mode: 2"))
        assertTrue(configYaml.contains("cloudflare"))
    }

    @Test
    fun testGeckoConfigSupportsSystemDnsFallback() {
        val systemConfig = GeckoConfigManager.generatePerformanceConfigYaml("system")
        assertNotNull(systemConfig)
        assertTrue(systemConfig.contains("network.trr.mode: 0"))
    }
}
