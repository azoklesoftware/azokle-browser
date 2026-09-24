package com.azokle.search.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainAutocompleteHelperTest {

    private val sampleHistory = listOf(
        "youtube.com",
        "github.com/azokle",
        "https://duckduckgo.com",
        "www.wikipedia.org/wiki/Kotlin"
    )

    private val sampleRecentSearches = listOf(
        "archlinux.org",
        "latest android news",
        "kotlinlang.org"
    )

    @Test
    fun findBestMatch_fromUserHistory_returnsCorrectSuffixAndUrl() {
        val match = DomainAutocompleteHelper.findBestMatch("yo", historyDomains = sampleHistory)
        assertNotNull(match)
        assertEquals("youtube.com", match?.matchedDomain)
        assertEquals("utube.com", match?.inlineSuffix)
        assertEquals("https://youtube.com", match?.fullUrl)
        assertTrue(match?.isFromHistory ?: false)
    }

    @Test
    fun findBestMatch_fromUserRecentSearches_returnsMatch() {
        val match = DomainAutocompleteHelper.findBestMatch("arch", recentSearches = sampleRecentSearches)
        assertNotNull(match)
        assertEquals("archlinux.org", match?.matchedDomain)
        assertEquals("linux.org", match?.inlineSuffix)
        assertEquals("https://archlinux.org", match?.fullUrl)
    }

    @Test
    fun findBestMatch_withHttpsPrefix_cleansAndMatches() {
        val match = DomainAutocompleteHelper.findBestMatch("https://git", historyDomains = sampleHistory)
        assertNotNull(match)
        assertEquals("github.com", match?.matchedDomain)
        assertEquals("hub.com", match?.inlineSuffix)
    }

    @Test
    fun findBestMatch_withWwwPrefix_cleansAndMatches() {
        val match = DomainAutocompleteHelper.findBestMatch("www.wik", historyDomains = sampleHistory)
        assertNotNull(match)
        assertEquals("wikipedia.org", match?.matchedDomain)
        assertEquals("ipedia.org", match?.inlineSuffix)
    }

    @Test
    fun findBestMatch_emptyHistory_returnsNull() {
        // Strict privacy check: No hardcoded fallbacks
        val match = DomainAutocompleteHelper.findBestMatch("yo", historyDomains = emptyList(), recentSearches = emptyList())
        assertNull(match)
    }

    @Test
    fun findBestMatch_multiWordQuery_returnsNull() {
        val match = DomainAutocompleteHelper.findBestMatch("yo video search", historyDomains = sampleHistory)
        assertNull(match)
    }

    @Test
    fun findBestMatch_emptyOrBlank_returnsNull() {
        assertNull(DomainAutocompleteHelper.findBestMatch("", historyDomains = sampleHistory))
        assertNull(DomainAutocompleteHelper.findBestMatch("   ", historyDomains = sampleHistory))
    }

    @Test
    fun findMatches_returnsMultipleDistinctSuggestionsFromHistory() {
        val customHistory = listOf("gitlab.com", "github.com", "gitea.com")
        val matches = DomainAutocompleteHelper.findMatches("git", historyDomains = customHistory, limit = 3)
        assertEquals(3, matches.size)
        assertEquals("gitlab.com", matches[0].matchedDomain)
        assertEquals("github.com", matches[1].matchedDomain)
        assertEquals("gitea.com", matches[2].matchedDomain)
    }
}
