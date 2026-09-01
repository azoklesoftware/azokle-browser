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
}
