package com.azokle.search.media

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Rational
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object MediaPlaybackManager {

    private const val TAG = "MediaPlaybackManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var artworkDownloadJob: Job? = null

    private var appContext: Context? = null
    var mediaSessionCompat: MediaSessionCompat? = null
        private set

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isAudioFocusGranted = false

    private var currentBridge: GeckoMediaSessionBridge? = null
    private var activeTabId: String? = null
    private var isUserExplicitlyPaused = false
    private var wasPlayingBeforeFocusLoss = false

    private val _mediaState = MutableStateFlow(MediaSessionState())
    val mediaState: StateFlow<MediaSessionState> = _mediaState.asStateFlow()

    var currentPositionMs: Long = 0L
        private set
    var currentDurationMs: Long = 0L
        private set

    // Noisy Audio Broadcast Receiver (Headphone disconnect auto-pause)
    private var isNoisyReceiverRegistered = false
    private val noisyAudioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    // PiP Action Broadcast Receiver (Interactive controls in Picture-in-Picture window)
    private var isPipReceiverRegistered = false
    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PictureInPictureHelper.ACTION_PIP_PLAY -> play()
                PictureInPictureHelper.ACTION_PIP_PAUSE -> pause()
                PictureInPictureHelper.ACTION_PIP_REWIND -> rewind()
                PictureInPictureHelper.ACTION_PIP_FORWARD -> fastForward()
            }
        }
    }

    // Audio Focus Change Listener (Handles auto-pause when another app plays and auto-resume when it pauses)
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                isAudioFocusGranted = false
                val isCurrentlyPlaying = _mediaState.value.status == PlaybackStatus.PLAYING
                if (isCurrentlyPlaying) {
                    wasPlayingBeforeFocusLoss = true
                    android.util.Log.d(TAG, "AudioFocus loss: Auto-pausing background media")
                    currentBridge?.pause()
                    activeTabId?.let { updatePlaybackStatus(it, PlaybackStatus.PAUSED) }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                isAudioFocusGranted = false
                val isCurrentlyPlaying = _mediaState.value.status == PlaybackStatus.PLAYING
                if (isCurrentlyPlaying) {
                    wasPlayingBeforeFocusLoss = true
                    android.util.Log.d(TAG, "AudioFocus transient loss: Auto-pausing background media for another app")
                    currentBridge?.pause()
                    activeTabId?.let { updatePlaybackStatus(it, PlaybackStatus.PAUSED) }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                isAudioFocusGranted = true
                if (wasPlayingBeforeFocusLoss && !isUserExplicitlyPaused) {
                    wasPlayingBeforeFocusLoss = false
                    android.util.Log.d(TAG, "AudioFocus gained: Auto-resuming background playback")
                    currentBridge?.play()
                    activeTabId?.let { updatePlaybackStatus(it, PlaybackStatus.PLAYING) }
                }
            }
        }
    }

    fun initialize(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val session = MediaSessionCompat(app, "AzokleMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    play()
                }

                override fun onPause() {
                    pause()
                }

                override fun onSkipToNext() {
                    currentBridge?.nextTrack()
                }

                override fun onSkipToPrevious() {
                    currentBridge?.previousTrack()
                }

                override fun onFastForward() {
                    fastForward()
                }

                override fun onRewind() {
                    rewind()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }

                override fun onStop() {
                    stop()
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    when (action) {
                        PictureInPictureHelper.ACTION_PIP_PLAY -> play()
                        PictureInPictureHelper.ACTION_PIP_PAUSE -> pause()
                        PictureInPictureHelper.ACTION_PIP_REWIND -> rewind()
                        PictureInPictureHelper.ACTION_PIP_FORWARD -> fastForward()
                    }
                }
            })
            isActive = true
        }
        mediaSessionCompat = session

        if (!isPipReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(PictureInPictureHelper.ACTION_PIP_PLAY)
                    addAction(PictureInPictureHelper.ACTION_PIP_PAUSE)
                    addAction(PictureInPictureHelper.ACTION_PIP_REWIND)
                    addAction(PictureInPictureHelper.ACTION_PIP_FORWARD)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    app.registerReceiver(pipActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    app.registerReceiver(pipActionReceiver, filter)
                }
                isPipReceiverRegistered = true
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to register PiP receiver: ${e.message}")
            }
        }
    }

    fun onSessionActivated(
        tabId: String,
        isPrivate: Boolean,
        bridge: GeckoMediaSessionBridge
    ) {
        activeTabId = tabId
        currentBridge = bridge
        isUserExplicitlyPaused = false
        _mediaState.update {
            it.copy(
                tabId = tabId,
                isPrivateSession = isPrivate,
                status = PlaybackStatus.BUFFERING
            )
        }
        requestAudioFocus()
        registerNoisyReceiver()
        val ctx = appContext
        if (ctx != null) {
            val allowBackground = com.azokle.search.common.util.PreferencesManager(ctx).isBackgroundAudioEnabled()
            if (allowBackground) {
                MediaPlaybackService.start(ctx)
            }
        }
    }

    fun onSessionDeactivated(tabId: String) {
        if (activeTabId == tabId) {
            isUserExplicitlyPaused = false
            wasPlayingBeforeFocusLoss = false
            _mediaState.update {
                it.copy(status = PlaybackStatus.STOPPED)
            }
            abandonAudioFocus()
            unregisterNoisyReceiver()
            appContext?.let { MediaPlaybackService.stop(it) }
            currentBridge = null
            activeTabId = null
            currentPositionMs = 0L
            currentDurationMs = 0L
            updatePlaybackStateCompat(PlaybackStatus.STOPPED, 0L, 0.0)
        }
    }

    fun onSessionPaused(tabId: String, isBackgroundPlaybackHost: Boolean = false, activeSession: org.mozilla.geckoview.MediaSession? = null) {
        if (activeTabId != tabId) return
        updatePlaybackStatus(tabId, PlaybackStatus.PAUSED)
    }

    fun updatePlaybackStatus(tabId: String, status: PlaybackStatus) {
        if (activeTabId != tabId) return

        if (status == PlaybackStatus.PLAYING) {
            isUserExplicitlyPaused = false
        }

        _mediaState.update { it.copy(status = status) }
        updatePlaybackStateCompat(status, currentPositionMs, _mediaState.value.metadata.playbackRate)

        val ctx = appContext ?: return
        val allowBackground = com.azokle.search.common.util.PreferencesManager(ctx).isBackgroundAudioEnabled()

        if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.PAUSED || status == PlaybackStatus.BUFFERING) {
            requestAudioFocus()
            registerNoisyReceiver()
            if (allowBackground) {
                MediaPlaybackService.start(ctx)
            }
        } else if (status == PlaybackStatus.STOPPED || status == PlaybackStatus.IDLE) {
            abandonAudioFocus()
            unregisterNoisyReceiver()
            MediaPlaybackService.stop(ctx)
        }
    }

    fun updateMetadata(
        tabId: String,
        title: String,
        artist: String,
        album: String,
        artworkUrl: String?
    ) {
        if (activeTabId != tabId) return

        _mediaState.update { current ->
            current.copy(
                metadata = current.metadata.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    artworkUrl = artworkUrl
                )
            )
        }

        updateMediaMetadataCompat(title, artist, album, null)
        val status = _mediaState.value.status
        if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.PAUSED || status == PlaybackStatus.BUFFERING) {
            appContext?.let { MediaPlaybackService.start(it) }
        }

        // Asynchronously load artwork
        if (!artworkUrl.isNullOrBlank()) {
            artworkDownloadJob?.cancel()
            artworkDownloadJob = scope.launch {
                val bitmap = fetchArtworkBitmap(artworkUrl)
                if (bitmap != null && !bitmap.isRecycled) {
                    _mediaState.update { current ->
                        current.copy(
                            metadata = current.metadata.copy(artworkBitmap = bitmap)
                        )
                    }
                    updateMediaMetadataCompat(title, artist, album, bitmap)
                    val s = _mediaState.value.status
                    if (s == PlaybackStatus.PLAYING || s == PlaybackStatus.PAUSED || s == PlaybackStatus.BUFFERING) {
                        appContext?.let { MediaPlaybackService.start(it) }
                    }
                }
            }
        }
    }

    fun updateMetadataWithBitmap(
        tabId: String,
        title: String,
        artist: String,
        album: String,
        bitmap: Bitmap
    ) {
        if (activeTabId != tabId) return

        _mediaState.update { current ->
            current.copy(
                metadata = current.metadata.copy(
                    title = title,
                    artist = artist,
                    album = album,
                    artworkBitmap = bitmap
                )
            )
        }

        updateMediaMetadataCompat(title, artist, album, bitmap)
        val status = _mediaState.value.status
        if (status == PlaybackStatus.PLAYING || status == PlaybackStatus.PAUSED || status == PlaybackStatus.BUFFERING) {
            appContext?.let { MediaPlaybackService.start(it) }
        }
    }

    fun updatePositionState(
        tabId: String,
        durationMs: Long,
        positionMs: Long,
        playbackRate: Double
    ) {
        if (activeTabId != tabId) return

        val durationChanged = durationMs > 0 && currentDurationMs != durationMs
        currentDurationMs = durationMs
        currentPositionMs = positionMs

        _mediaState.update { current ->
            current.copy(
                metadata = current.metadata.copy(
                    durationMs = durationMs,
                    positionMs = positionMs,
                    playbackRate = playbackRate
                )
            )
        }

        if (durationChanged) {
            val meta = _mediaState.value.metadata
            updateMediaMetadataCompat(meta.title, meta.artist, meta.album, meta.artworkBitmap)
        }

        updatePlaybackStateCompat(_mediaState.value.status, positionMs, playbackRate)
    }

    fun updateFullscreenState(
        tabId: String,
        isFullscreen: Boolean,
        videoAspectRatio: Rational?
    ) {
        if (activeTabId != tabId) return

        _mediaState.update { current ->
            current.copy(
                isFullscreen = isFullscreen,
                metadata = current.metadata.copy(
                    hasVideo = isFullscreen || videoAspectRatio != null,
                    videoAspectRatio = videoAspectRatio ?: current.metadata.videoAspectRatio
                )
            )
        }
    }

    fun play() {
        isUserExplicitlyPaused = false
        wasPlayingBeforeFocusLoss = false
        requestAudioFocus()
        currentBridge?.play()
    }

    fun pause() {
        isUserExplicitlyPaused = true
        wasPlayingBeforeFocusLoss = false
        currentBridge?.pause()
    }

    fun stop() {
        isUserExplicitlyPaused = false
        wasPlayingBeforeFocusLoss = false
        currentBridge?.stop()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        appContext?.let { MediaPlaybackService.stop(it) }
    }

    fun seekTo(positionMs: Long) {
        currentBridge?.seekTo(positionMs)
    }

    fun fastForward(offsetMs: Long = 10000L) {
        currentBridge?.fastForward(offsetMs)
    }

    fun rewind(offsetMs: Long = 10000L) {
        currentBridge?.rewind(offsetMs)
    }

    private fun requestAudioFocus() {
        if (isAudioFocusGranted) return
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            val res = am.requestAudioFocus(req)
            isAudioFocusGranted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val res = am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            isAudioFocusGranted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (!isAudioFocusGranted) return
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
        isAudioFocusGranted = false
    }

    private fun registerNoisyReceiver() {
        val ctx = appContext ?: return
        if (!isNoisyReceiverRegistered) {
            try {
                ctx.registerReceiver(
                    noisyAudioReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isNoisyReceiverRegistered = true
            } catch (e: Exception) {
                // Safe fallback
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        val ctx = appContext ?: return
        if (isNoisyReceiverRegistered) {
            try {
                ctx.unregisterReceiver(noisyAudioReceiver)
            } catch (e: Exception) {
                // Safe fallback
            }
            isNoisyReceiverRegistered = false
        }
    }

    private fun updatePlaybackStateCompat(status: PlaybackStatus, positionMs: Long, rate: Double) {
        val session = mediaSessionCompat ?: return

        val stateCompat = when (status) {
            PlaybackStatus.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlaybackStatus.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlaybackStatus.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            PlaybackStatus.STOPPED -> PlaybackStateCompat.STATE_STOPPED
            else -> PlaybackStateCompat.STATE_NONE
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(stateCompat, positionMs, rate.toFloat())

        session.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaMetadataCompat(
        title: String,
        artist: String,
        album: String,
        artwork: Bitmap?
    ) {
        val session = mediaSessionCompat ?: return
        try {
            val metaBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDurationMs)

            if (artwork != null && !artwork.isRecycled) {
                try {
                    metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                    metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork)
                } catch (e: Exception) {}
            }

            session.setMetadata(metaBuilder.build())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "updateMediaMetadataCompat exception: ${e.message}")
        }
    }

    private suspend fun fetchArtworkBitmap(url: String): Bitmap? {
        val ctx = appContext ?: return null
        return try {
            val loader = ImageLoader(ctx)
            val request = ImageRequest.Builder(ctx)
                .data(url)
                .size(512, 512)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun release(context: Context) {
        stop()
        if (isPipReceiverRegistered) {
            try {
                appContext?.unregisterReceiver(pipActionReceiver)
            } catch (e: Exception) {}
            isPipReceiverRegistered = false
        }
        mediaSessionCompat?.release()
        mediaSessionCompat = null
        appContext = null
    }
}
