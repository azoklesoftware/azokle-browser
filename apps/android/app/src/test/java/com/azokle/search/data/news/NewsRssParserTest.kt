package com.azokle.search.data.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRssParserTest {

    @Test
    fun testSanitizeImageUrl_rejectsGoogleProxies() {
        val googleProxy1 = "https://lh3.googleusercontent.com/proxy/abc123xyz"
        val googleProxy2 = "https://news.google.com/api/attachments/xyz"
        val googleProxy3 = "http://encrypted-tbn0.gstatic.com/images?q=tbn:xyz"

        assertNull(NewsRssParser.sanitizeImageUrl(googleProxy1))
        assertNull(NewsRssParser.sanitizeImageUrl(googleProxy2))
        assertNull(NewsRssParser.sanitizeImageUrl(googleProxy3))
    }

    @Test
    fun testSanitizeImageUrl_acceptsPublisherImages() {
        val publisherImg = "https://media.cnn.com/api/v1/images/stellar/prod/test.jpg"
        val techcrunchImg = "https://techcrunch.com/wp-content/uploads/2026/09/ai-chip.png"

        assertEquals(publisherImg, NewsRssParser.sanitizeImageUrl(publisherImg))
        assertEquals(techcrunchImg, NewsRssParser.sanitizeImageUrl(techcrunchImg))
    }

    @Test
    fun testCleanUrlTracking_stripsUtmAndGoogleTrackingParams() {
        val dirtyUrl = "https://arstechnica.com/gadgets/2026/09/new-chip/?utm_source=google&utm_medium=rss&utm_campaign=feed&ved=2ahUKEwj123&usg=AOvVaw456&article_id=999"
        val cleaned = NewsRssParser.cleanUrlTracking(dirtyUrl)

        assertTrue(cleaned.contains("arstechnica.com"))
        assertTrue(cleaned.contains("article_id=999"))
        assertTrue(!cleaned.contains("utm_source"))
        assertTrue(!cleaned.contains("utm_medium"))
        assertTrue(!cleaned.contains("ved="))
        assertTrue(!cleaned.contains("usg="))
    }

    @Test
    fun testParse_extractsValidArticles() {
        val sampleRss = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
                <channel>
                    <title>Google News</title>
                    <item>
                        <title>SpaceX Starship Completes Orbital Refueling Test - SpaceNews</title>
                        <link>https://spacenews.com/spacex-starship-orbital-refueling/?utm_source=news</link>
                        <guid isPermaLink="false">tag:google.com,2013:googleid_1</guid>
                        <pubDate>Tue, 01 Sep 2026 12:30:00 GMT</pubDate>
                        <description>&lt;a href="https://spacenews.com"&gt;SpaceX successfully transferred propellants in orbit.&lt;/a&gt;</description>
                        <source url="https://spacenews.com">SpaceNews</source>
                    </item>
                    <item>
                        <title>Quantum Processor Achieves New Error Threshold - Quanta Magazine</title>
                        <link>https://quantamagazine.org/quantum-processor-error-threshold/</link>
                        <guid isPermaLink="false">tag:google.com,2013:googleid_2</guid>
                        <pubDate>Tue, 01 Sep 2026 14:00:00 GMT</pubDate>
                        <source url="https://quantamagazine.org">Quanta Magazine</source>
                        <media:content url="https://d2r55xnwy6nx47.cloudfront.net/uploads/2026/09/quantum.jpg" type="image/jpeg" />
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val articles = NewsRssParser.parse(sampleRss, NewsCategory.SCIENCE.id)

        assertEquals(2, articles.size)

        val first = articles[0]
        assertEquals("SpaceX Starship Completes Orbital Refueling Test", first.title)
        assertEquals("SpaceNews", first.sourceName)
        assertEquals("spacenews.com", first.sourceDomain)
        assertTrue(first.url.startsWith("https://spacenews.com"))
        assertTrue(!first.url.contains("utm_source"))
        assertNotNull(first.snippet)

        val second = articles[1]
        assertEquals("Quantum Processor Achieves New Error Threshold", second.title)
        assertEquals("Quanta Magazine", second.sourceName)
        assertEquals("https://d2r55xnwy6nx47.cloudfront.net/uploads/2026/09/quantum.jpg", second.thumbnailUrl)
    }
}
