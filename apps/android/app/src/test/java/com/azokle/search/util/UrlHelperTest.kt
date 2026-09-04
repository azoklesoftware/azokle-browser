package com.azokle.search.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlHelperTest {

    @Test
    fun testDirectUrlResolution() {
        val resolvedHttps = UrlHelper.resolveSearchOrUrl("https://example.com/test")
        assertEquals("https://example.com/test", resolvedHttps)

        val resolvedDomain = UrlHelper.resolveSearchOrUrl("github.com/azokle")
        assertEquals("https://github.com/azokle", resolvedDomain)
    }

    @Test
    fun testDuckDuckGoSearchEngineResolution() {
        val ddgQueryUrl = "https://duckduckgo.com/?q="
        val query = "kotlin coroutines guide"
        val resolved = UrlHelper.resolveSearchOrUrl(query, searchEngineQueryUrl = ddgQueryUrl)
        assertTrue(resolved.startsWith("https://duckduckgo.com/?q="))
    }

    @Test
    fun testBraveSearchEngineResolution() {
        val braveQueryUrl = "https://search.brave.com/search?q="
        val query = "android jetpack compose"
        val resolved = UrlHelper.resolveSearchOrUrl(query, searchEngineQueryUrl = braveQueryUrl)
        assertTrue(resolved.startsWith("https://search.brave.com/search?q="))
    }

    @Test
    fun testStartpageSearchEngineResolution() {
        val startpageQueryUrl = "https://www.startpage.com/sp/search?query="
        val query = "privacy browser engine"
        val resolved = UrlHelper.resolveSearchOrUrl(query, searchEngineQueryUrl = startpageQueryUrl)
        assertTrue(resolved.startsWith("https://www.startpage.com/sp/search?query="))
    }

    @Test
    fun testCustomPortResolution() {
        val resolvedDomainPort = UrlHelper.resolveSearchOrUrl("example.com:8080/test")
        assertEquals("https://example.com:8080/test", resolvedDomainPort)

        val resolvedLocalhostPort = UrlHelper.resolveSearchOrUrl("localhost:3000")
        assertEquals("https://localhost:3000", resolvedLocalhostPort)

        val resolvedIpPort = UrlHelper.resolveSearchOrUrl("192.168.1.1:8443/admin")
        assertEquals("https://192.168.1.1:8443/admin", resolvedIpPort)
    }

    @Test
    fun testIPv6Resolution() {
        val resolvedIpv6 = UrlHelper.resolveSearchOrUrl("[::1]:8080")
        assertEquals("https://[::1]:8080", resolvedIpv6)

        val resolvedIpv6Http = UrlHelper.resolveSearchOrUrl("http://[2001:db8::1]/status")
        assertEquals("http://[2001:db8::1]/status", resolvedIpv6Http)
    }

    @Test
    fun testTrackingParameterStripping() {
        val dirtyUrl = "https://example.com/item?fbclid=123&utm_source=feed&category=tech#specs"
        val cleaned = UrlHelper.stripTrackingParameters(dirtyUrl)
        assertEquals("https://example.com/item?category=tech#specs", cleaned)

        val allTracking = "https://example.com/?gclid=xyz&utm_campaign=winter"
        val cleanedAll = UrlHelper.stripTrackingParameters(allTracking)
        assertEquals("https://example.com/", cleanedAll)
    }

    @Test
    fun testDangerousSchemesRedirectToSearch() {
        val jsUri = "javascript:alert(1)"
        val resolvedJs = UrlHelper.resolveSearchOrUrl(jsUri)
        assertTrue("javascript: scheme must be redirected to search", resolvedJs.contains("search.azokle.com"))

        val fileUri = "file:///etc/passwd"
        val resolvedFile = UrlHelper.resolveSearchOrUrl(fileUri)
        assertTrue("file: scheme must be redirected to search", resolvedFile.contains("search.azokle.com"))

        val contentUri = "content://contacts/people"
        val resolvedContent = UrlHelper.resolveSearchOrUrl(contentUri)
        assertTrue("content: scheme must be redirected to search", resolvedContent.contains("search.azokle.com"))
    }
}
