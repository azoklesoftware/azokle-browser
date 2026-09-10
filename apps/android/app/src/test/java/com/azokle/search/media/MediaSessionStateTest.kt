package com.azokle.search.media

import android.util.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionStateTest {

    @Test
    fun testDefaultStateValues() {
        val state = MediaSessionState()
        assertEquals(PlaybackStatus.IDLE, state.status)
        assertFalse(state.isFullscreen)
        assertFalse(state.isPrivateSession)
        assertEquals("", state.metadata.title)
        assertEquals("", state.metadata.artist)
        assertEquals(0L, state.metadata.durationMs)
        assertEquals(0L, state.metadata.positionMs)
        assertEquals(1.0, state.metadata.playbackRate, 0.001)
    }

    @Test
    fun testMetadataCopyAndVideoAspectRatio() {
        val meta = ActiveMediaMetadata(
            title = "Sample HTML5 Video",
            artist = "video.example.com",
            durationMs = 120000L,
            positionMs = 30000L,
            hasVideo = true,
            videoAspectRatio = null
        )
        val state = MediaSessionState(
            status = PlaybackStatus.PLAYING,
            metadata = meta,
            isFullscreen = true,
            tabId = "tab_101"
        )

        assertEquals(PlaybackStatus.PLAYING, state.status)
        assertTrue(state.isFullscreen)
        assertTrue(state.metadata.hasVideo)
        assertEquals("Sample HTML5 Video", state.metadata.title)
        assertEquals(120000L, state.metadata.durationMs)
        assertEquals(30000L, state.metadata.positionMs)
        assertEquals("tab_101", state.tabId)
    }

    @Test
    fun testPlaybackStatusEnumCompleteness() {
        val statuses = PlaybackStatus.values()
        assertTrue(statuses.contains(PlaybackStatus.IDLE))
        assertTrue(statuses.contains(PlaybackStatus.BUFFERING))
        assertTrue(statuses.contains(PlaybackStatus.PLAYING))
        assertTrue(statuses.contains(PlaybackStatus.PAUSED))
        assertTrue(statuses.contains(PlaybackStatus.STOPPED))
    }
}
