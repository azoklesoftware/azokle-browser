package com.azokle.search.reader

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReaderExtractorSecurityTest {

    @Before
    fun setUp() {
        ReaderExtractor.clearCache()
    }

    @Test
    fun testCacheAndNormalizedRetrieval() {
        val article = ArticleContent(
            title = "Test Headline",
            byline = "Author Name",
            blocks = listOf(
                ArticleBlock(BlockType.HEADING, "Section 1"),
                ArticleBlock(BlockType.PARAGRAPH, "Paragraph text here.")
            ),
            paragraphs = listOf("Paragraph text here."),
            estimatedMinutes = 2
        )

        ReaderExtractor.cacheArticle("https://EXAMPLE.com/path/article?query=1#section", article)

        val retrieved = ReaderExtractor.getCachedArticle("https://example.com/path/article")
        assertNotNull(retrieved)
        assertEquals("Test Headline", retrieved?.title)
        assertEquals(2, retrieved?.blocks?.size)
        assertEquals(BlockType.HEADING, retrieved?.blocks?.get(0)?.type)
    }

    @Test
    fun testSsrfBlockingLoopbackAndPrivateRanges() = runBlocking {
        // Loopback endpoints
        val loopbackResult = ReaderExtractor.extractArticleFromUrl("http://127.0.0.1:9050", "Fallback")
        assertTrue(loopbackResult.paragraphs.first().contains("disabled for local and internal network"))

        val localhostResult = ReaderExtractor.extractArticleFromUrl("http://localhost:8080/secret", "Fallback")
        assertTrue(localhostResult.paragraphs.first().contains("disabled for local and internal network"))

        // RFC 1918 Private IP ranges
        val privateResult1 = ReaderExtractor.extractArticleFromUrl("http://192.168.1.1/admin", "Fallback")
        assertTrue(privateResult1.paragraphs.first().contains("disabled for local and internal network"))

        val privateResult2 = ReaderExtractor.extractArticleFromUrl("http://10.0.0.1/status", "Fallback")
        assertTrue(privateResult2.paragraphs.first().contains("disabled for local and internal network"))

        val privateResult3 = ReaderExtractor.extractArticleFromUrl("http://172.16.0.1/config", "Fallback")
        assertTrue(privateResult3.paragraphs.first().contains("disabled for local and internal network"))

        // Non-HTTP schemes
        val fileResult = ReaderExtractor.extractArticleFromUrl("file:///android_asset/secret.txt", "Fallback")
        assertTrue(fileResult.paragraphs.first().contains("disabled for local and internal network"))
    }

    @Test
    fun testInternalBrowserPageHandledGracefully() = runBlocking {
        val result = ReaderExtractor.extractArticleFromUrl("about:home", "Fallback")
        assertTrue(result.paragraphs.first().contains("internal browser page"))
    }
}
