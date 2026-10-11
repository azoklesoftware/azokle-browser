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
