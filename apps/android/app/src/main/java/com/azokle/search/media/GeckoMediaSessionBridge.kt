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

import android.os.Handler
import android.os.Looper
import android.util.Rational
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.MediaSession

/**
 * Enterprise-grade GeckoSession MediaSession Delegate Bridge.
 * Binds GeckoView Quantum HTML5 MediaSession events directly to Azokle's native MediaPlaybackManager.
 */
class GeckoMediaSessionBridge(
    private val tabId: String,
    private val isPrivate: Boolean,
    private val isTor: Boolean,
    private val currentUrlProvider: (() -> String?)? = null
) : MediaSession.Delegate {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeMediaSession: MediaSession? = null

    /**
     * Checks whether background playback host.
     */
    private fun isBackgroundPlaybackHost(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val uri = try { android.net.Uri.parse(url) } catch (e: Exception) { null }
        val host = uri?.host?.lowercase() ?: return false
        return host == "youtube.com" ||
               host.endsWith(".youtube.com") ||
               host == "youtube-nocookie.com" ||
               host.endsWith(".youtube-nocookie.com") ||
               host == "youtu.be" ||
               host == "spotify.com" ||
               host.endsWith(".spotify.com") ||
               host == "vimeo.com" ||
               host.endsWith(".vimeo.com") ||
               host == "soundcloud.com" ||
               host.endsWith(".soundcloud.com") ||
               host == "twitch.tv" ||
               host.endsWith(".twitch.tv")
    }

    /**
     * Handles on activated events.
     */
    override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
        activeMediaSession = mediaSession
        try {
            session.settings.suspendMediaWhenInactive = false
        } catch (e: Exception) {}
        mainHandler.post {
            MediaPlaybackManager.onSessionActivated(
                tabId = tabId,
                isPrivate = isPrivate || isTor,
                bridge = this
            )
        }
    }

    /**
     * Handles on deactivated events.
     */
    override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
        if (activeMediaSession == mediaSession) {
            activeMediaSession = null
        }
        mainHandler.post {
            MediaPlaybackManager.onSessionDeactivated(tabId)
        }
    }

    /**
     * Handles on metadata events.
     */
    override fun onMetadata(
        session: GeckoSession,
        mediaSession: MediaSession,
        meta: MediaSession.Metadata
    ) {
        try {
            session.settings.suspendMediaWhenInactive = false
        } catch (e: Exception) {}

        val title = meta.title ?: ""
        val artist = meta.artist ?: ""
        val album = meta.album ?: ""

        val artwork = meta.artwork
        val bitmapResult = artwork?.getBitmap(512)
        if (bitmapResult != null) {
            bitmapResult.then({ bitmap ->
                if (bitmap != null && !bitmap.isRecycled) {
                    mainHandler.post {
                        try {
                            MediaPlaybackManager.updateMetadataWithBitmap(
                                tabId = tabId,
                                title = title,
                                artist = artist,
                                album = album,
                                bitmap = bitmap
                            )
                        } catch (e: Exception) {}
                    }
                } else {
                    mainHandler.post {
                        try {
                            MediaPlaybackManager.updateMetadata(
                                tabId = tabId,
                                title = title,
                                artist = artist,
                                album = album,
                                artworkUrl = null
                            )
                        } catch (e: Exception) {}
                    }
                }
                org.mozilla.geckoview.GeckoResult.fromValue(null)
            }, {
                mainHandler.post {
                    try {
                        MediaPlaybackManager.updateMetadata(
                            tabId = tabId,
                            title = title,
                            artist = artist,
                            album = album,
                            artworkUrl = null
                        )
                    } catch (e: Exception) {}
                }
                org.mozilla.geckoview.GeckoResult.fromValue(null)
            })
        } else {
            mainHandler.post {
                try {
                    MediaPlaybackManager.updateMetadata(
                        tabId = tabId,
                        title = title,
                        artist = artist,
                        album = album,
                        artworkUrl = null
                    )
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Handles on play events.
     */
    override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
        try {
            session.settings.suspendMediaWhenInactive = false
        } catch (e: Exception) {}
        mainHandler.post {
            MediaPlaybackManager.updatePlaybackStatus(tabId, PlaybackStatus.PLAYING)
        }
    }

    /**
     * Handles on pause events.
     */
    override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
        val currentUrl = currentUrlProvider?.invoke()
        val isBgHost = isBackgroundPlaybackHost(currentUrl)
        mainHandler.post {
            MediaPlaybackManager.onSessionPaused(tabId, isBgHost, activeMediaSession)
        }
    }

    /**
     * Handles on stop events.
     */
    override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
        mainHandler.post {
            MediaPlaybackManager.updatePlaybackStatus(tabId, PlaybackStatus.STOPPED)
        }
    }

    /**
     * Handles on position state events.
     */
    override fun onPositionState(
        session: GeckoSession,
        mediaSession: MediaSession,
        state: MediaSession.PositionState
    ) {
        val durationMs = (state.duration * 1000).toLong().coerceAtLeast(0L)
        val positionMs = (state.position * 1000).toLong().coerceAtLeast(0L)
        val rate = state.playbackRate

        mainHandler.post {
            MediaPlaybackManager.updatePositionState(
                tabId = tabId,
                durationMs = durationMs,
                positionMs = positionMs,
                playbackRate = rate
            )
        }
    }

    /**
     * Handles on fullscreen events.
     */
    override fun onFullscreen(
        session: GeckoSession,
        mediaSession: MediaSession,
        fullscreen: Boolean,
        element: MediaSession.ElementMetadata?
    ) {
        var ratio: Rational? = null
        if (element != null && element.width > 0 && element.height > 0) {
            ratio = Rational(element.width.toInt(), element.height.toInt())
        }

        mainHandler.post {
            MediaPlaybackManager.updateFullscreenState(
                tabId = tabId,
                isFullscreen = fullscreen,
                videoAspectRatio = ratio
            )
        }
    }

    // Transport Action Callbacks driven by Android MediaSession / PiP / Notification Controls
    fun play() {
        activeMediaSession?.play()
    }

    /**
     * Executes the pause operation.
     */
    fun pause() {
        activeMediaSession?.pause()
    }

    /**
     * Executes the stop operation.
     */
    fun stop() {
        activeMediaSession?.stop()
    }

    /**
     * Executes the seek to operation.
     */
    fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        activeMediaSession?.seekTo(seconds, false)
    }

    /**
     * Executes the fast forward operation.
     */
    fun fastForward(offsetMs: Long = 10000L) {
        val current = MediaPlaybackManager.currentPositionMs
        val duration = MediaPlaybackManager.currentDurationMs
        val target = (current + offsetMs).coerceAtMost(if (duration > 0) duration else current + offsetMs)
        seekTo(target)
    }

    /**
     * Executes the rewind operation.
     */
    fun rewind(offsetMs: Long = 10000L) {
        val current = MediaPlaybackManager.currentPositionMs
        val target = (current - offsetMs).coerceAtLeast(0L)
        seekTo(target)
    }

    /**
     * Executes the next track operation.
     */
    fun nextTrack() {
        activeMediaSession?.nextTrack()
    }

    /**
     * Executes the previous track operation.
     */
    fun previousTrack() {
        activeMediaSession?.previousTrack()
    }
}
