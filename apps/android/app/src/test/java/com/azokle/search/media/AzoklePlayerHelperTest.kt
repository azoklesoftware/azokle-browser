package com.azokle.search.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AzoklePlayerHelperTest {

    @Test
    fun testIsEligibleVideoUrl_youtubeUrls() {
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=42s"))
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun testIsEligibleVideoUrl_vimeoUrls() {
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://vimeo.com/76979871"))
        assertTrue(AzoklePlayerHelper.isEligibleVideoUrl("https://player.vimeo.com/video/76979871"))
    }

    @Test
    fun testIsEligibleVideoUrl_nonVideoUrls() {
        assertFalse(AzoklePlayerHelper.isEligibleVideoUrl("https://www.youtube.com/feed/subscriptions"))
        assertFalse(AzoklePlayerHelper.isEligibleVideoUrl("https://www.youtube.com/channel/UC123456789"))
        assertFalse(AzoklePlayerHelper.isEligibleVideoUrl("https://google.com"))
        assertFalse(AzoklePlayerHelper.isEligibleVideoUrl("https://vimeo.com/channels/staffpicks"))
        assertFalse(AzoklePlayerHelper.isEligibleVideoUrl(""))
    }

    @Test
    fun testExtractVideoId_youtube() {
        assertEquals("dQw4w9WgXcQ", AzoklePlayerHelper.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", AzoklePlayerHelper.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ&feature=share"))
        assertEquals("dQw4w9WgXcQ", AzoklePlayerHelper.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", AzoklePlayerHelper.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", AzoklePlayerHelper.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test
    fun testExtractVideoId_vimeo() {
        assertEquals("76979871", AzoklePlayerHelper.extractVideoId("https://vimeo.com/76979871"))
        assertEquals("76979871", AzoklePlayerHelper.extractVideoId("https://player.vimeo.com/video/76979871"))
    }

    @Test
    fun testBuildSovereignEmbedUrl_youtubeNocookie() {
        val embedUrl = AzoklePlayerHelper.buildSovereignEmbedUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(embedUrl)
        assertTrue(embedUrl!!.startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?"))
        assertTrue(embedUrl.contains("autoplay=1"))
        assertTrue(embedUrl.contains("rel=0"))
        assertTrue(embedUrl.contains("playsinline=1"))
    }

    @Test
    fun testBuildSovereignEmbedUrl_vimeoDnt() {
        val embedUrl = AzoklePlayerHelper.buildSovereignEmbedUrl("https://vimeo.com/76979871")
        assertNotNull(embedUrl)
        assertTrue(embedUrl!!.startsWith("https://player.vimeo.com/video/76979871?"))
        assertTrue(embedUrl.contains("dnt=1"))
        assertTrue(embedUrl.contains("autoplay=1"))
    }

    @Test
    fun testBuildSovereignEmbedUrl_nonVideoReturnsNull() {
        assertNull(AzoklePlayerHelper.buildSovereignEmbedUrl("https://example.com/article"))
    }
}
