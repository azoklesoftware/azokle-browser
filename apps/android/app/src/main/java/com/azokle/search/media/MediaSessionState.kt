package com.azokle.search.media

import android.graphics.Bitmap
import android.util.Rational

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED
}

data class ActiveMediaMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String? = null,
    val artworkBitmap: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playbackRate: Double = 1.0,
    val isSeekable: Boolean = true,
    val hasVideo: Boolean = false,
    val videoAspectRatio: Rational? = null
)

data class MediaSessionState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val metadata: ActiveMediaMetadata = ActiveMediaMetadata(),
    val isFullscreen: Boolean = false,
    val isMuted: Boolean = false,
    val isPrivateSession: Boolean = false,
    val tabId: String? = null
)
