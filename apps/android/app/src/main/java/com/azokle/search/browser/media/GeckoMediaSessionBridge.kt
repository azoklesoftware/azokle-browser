package com.azokle.search.browser.media

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
    private val isTor: Boolean
) : MediaSession.Delegate {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeMediaSession: MediaSession? = null

    override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
        activeMediaSession = mediaSession
        mainHandler.post {
            MediaPlaybackManager.onSessionActivated(
                tabId = tabId,
                isPrivate = isPrivate || isTor,
                bridge = this
            )
        }
    }

    override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
        if (activeMediaSession == mediaSession) {
            activeMediaSession = null
        }
        mainHandler.post {
            MediaPlaybackManager.onSessionDeactivated(tabId)
        }
    }

    override fun onMetadata(
        session: GeckoSession,
        mediaSession: MediaSession,
        meta: MediaSession.Metadata
    ) {
        val title = meta.title ?: ""
        val artist = meta.artist ?: ""
        val album = meta.album ?: ""

        val artwork = meta.artwork
        if (artwork != null) {
            artwork.getBitmap(512)?.then({ bitmap ->
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

    override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
        mainHandler.post {
            MediaPlaybackManager.updatePlaybackStatus(tabId, PlaybackStatus.PLAYING)
        }
    }

    override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
        mainHandler.post {
            MediaPlaybackManager.updatePlaybackStatus(tabId, PlaybackStatus.PAUSED)
        }
    }

    override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
        mainHandler.post {
            MediaPlaybackManager.updatePlaybackStatus(tabId, PlaybackStatus.STOPPED)
        }
    }

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

    fun pause() {
        activeMediaSession?.pause()
    }

    fun stop() {
        activeMediaSession?.stop()
    }

    fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        activeMediaSession?.seekTo(seconds, false)
    }

    fun fastForward(offsetMs: Long = 10000L) {
        val current = MediaPlaybackManager.currentPositionMs
        val duration = MediaPlaybackManager.currentDurationMs
        val target = (current + offsetMs).coerceAtMost(if (duration > 0) duration else current + offsetMs)
        seekTo(target)
    }

    fun rewind(offsetMs: Long = 10000L) {
        val current = MediaPlaybackManager.currentPositionMs
        val target = (current - offsetMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun nextTrack() {
        activeMediaSession?.nextTrack()
    }

    fun previousTrack() {
        activeMediaSession?.previousTrack()
    }
}
