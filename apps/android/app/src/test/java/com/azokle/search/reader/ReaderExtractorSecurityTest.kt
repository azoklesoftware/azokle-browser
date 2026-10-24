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

        // Cloud Metadata and link-local (RFC 3927)
        val cloudMetadataResult = ReaderExtractor.extractArticleFromUrl("http://169.254.169.254/latest/meta-data/", "Fallback")
        assertTrue(cloudMetadataResult.paragraphs.first().contains("disabled for local and internal network"))

        // IPv6 Loopback and link-local
        val ipv6Loopback = ReaderExtractor.extractArticleFromUrl("http://[::1]:8080/secret", "Fallback")
        assertTrue(ipv6Loopback.paragraphs.first().contains("disabled for local and internal network"))

        val ipv6LinkLocal = ReaderExtractor.extractArticleFromUrl("http://[fe80::1]/admin", "Fallback")
        assertTrue(ipv6LinkLocal.paragraphs.first().contains("disabled for local and internal network"))

        // Non-HTTP schemes
        val fileResult = ReaderExtractor.extractArticleFromUrl("file:///android_asset/secret.txt", "Fallback")
        assertTrue(fileResult.paragraphs.first().contains("disabled for local and internal network"))
    }

    @Test
    fun testCacheLruBounding() {
        val sampleArticle = ArticleContent("Title", "Author", emptyList(), listOf("p"), 1)
        for (i in 1..60) {
            ReaderExtractor.cacheArticle("https://example.com/article$i", sampleArticle)
        }
        // Oldest articles (e.g., article1) should have been evicted by the bounded LRU cache (max 50)
        assertNull(ReaderExtractor.getCachedArticle("https://example.com/article1"))
        // Most recent articles should be present
        assertNotNull(ReaderExtractor.getCachedArticle("https://example.com/article60"))
    }

    @Test
    fun testInternalBrowserPageHandledGracefully() = runBlocking {
        val result = ReaderExtractor.extractArticleFromUrl("about:home", "Fallback")
        assertTrue(result.paragraphs.first().contains("internal browser page"))
    }

    @Test
    fun testClearCacheEmptiesAllEntries() {
        val article = ArticleContent("Headline", "Author", emptyList(), listOf("p"), 1)
        ReaderExtractor.cacheArticle("https://example.com/item1", article)
        ReaderExtractor.cacheArticle("https://example.com/item2", article)
        assertNotNull(ReaderExtractor.getCachedArticle("https://example.com/item1"))

        ReaderExtractor.clearCache()
        assertNull(ReaderExtractor.getCachedArticle("https://example.com/item1"))
        assertNull(ReaderExtractor.getCachedArticle("https://example.com/item2"))
    }

    @Test
    fun testTorModeDnsLeakDefenseAndSsrfMitigation() {
        // AZK-SEC-01: Clearnet domains in Tor mode must be allowed without triggering host OS DNS leaks
        assertTrue(ReaderExtractor.isSafePublicUrl("https://en.wikipedia.org/wiki/Privacy", isTor = true))
        assertTrue(ReaderExtractor.isSafePublicUrl("https://reuters.com/news", isTor = true))

        // Onion hidden services in Tor mode must always be allowed
        assertTrue(ReaderExtractor.isSafePublicUrl("http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion", isTor = true))

        // Private IP literals must still be blocked even in Tor mode
        org.junit.Assert.assertFalse(ReaderExtractor.isSafePublicUrl("http://192.168.1.1/admin", isTor = true))
        org.junit.Assert.assertFalse(ReaderExtractor.isSafePublicUrl("http://10.0.0.1/status", isTor = true))
        org.junit.Assert.assertFalse(ReaderExtractor.isSafePublicUrl("http://127.0.0.1:8080", isTor = true))
        org.junit.Assert.assertFalse(ReaderExtractor.isSafePublicUrl("http://localhost:8080", isTor = true))
    }
}

