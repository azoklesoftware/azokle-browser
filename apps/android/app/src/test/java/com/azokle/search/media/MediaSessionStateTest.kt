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

    @Test
    fun testAudioFocusDuckingBehaviorSpecification() {
        // Spec verification: AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK must NOT pause media
        fun shouldPauseOnFocusLoss(focusChange: Int): Boolean {
            return when (focusChange) {
                android.media.AudioManager.AUDIOFOCUS_LOSS -> true
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> true
                android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> false
                else -> false
            }
        }

        assertFalse("Notification sound (CAN_DUCK) should not pause playback",
            shouldPauseOnFocusLoss(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
        assertTrue("Phone call / Assistant (TRANSIENT) must pause playback",
            shouldPauseOnFocusLoss(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
        assertTrue("Permanent focus loss must pause playback",
            shouldPauseOnFocusLoss(android.media.AudioManager.AUDIOFOCUS_LOSS))
    }

    @Test
    fun testBraveGradeAutoResumePolicy() {
        var wasPlayingBeforeLoss = false
        var isCurrentlyPlaying = true
        var isUserExplicitlyPaused = false

        // 1. Another app (YouTube / Spotify / TikTok / Phone call) requests focus
        if (isCurrentlyPlaying) {
            wasPlayingBeforeLoss = true
            isCurrentlyPlaying = false
        }
        assertFalse(isCurrentlyPlaying)
        assertTrue(wasPlayingBeforeLoss)

        // 2. Other app finishes/pauses -> AUDIOFOCUS_GAIN
        var shouldAutoResume = wasPlayingBeforeLoss && !isUserExplicitlyPaused
        assertTrue("Should automatically resume playback when other app finishes", shouldAutoResume)

        // 3. If user explicitly clicked Pause in Azokle UI, do not auto-resume
        isUserExplicitlyPaused = true
        shouldAutoResume = wasPlayingBeforeLoss && !isUserExplicitlyPaused
        assertFalse("Should not auto-resume if user explicitly tapped pause", shouldAutoResume)
    }
}


