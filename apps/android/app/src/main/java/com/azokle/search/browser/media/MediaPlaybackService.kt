package com.azokle.search.browser.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.azokle.search.MainActivity
import com.azokle.search.R

class MediaPlaybackService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val CHANNEL_ID = "azokle_media_playback"
        const val NOTIFICATION_ID = 2001

        const val ACTION_PLAY = "com.azokle.search.media.PLAY"
        const val ACTION_PAUSE = "com.azokle.search.media.PAUSE"
        const val ACTION_REWIND = "com.azokle.search.media.REWIND"
        const val ACTION_FORWARD = "com.azokle.search.media.FORWARD"
        const val ACTION_STOP = "com.azokle.search.media.STOP"

        fun start(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaPlaybackService", "startForegroundService exception suppressed: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                android.util.Log.e("MediaPlaybackService", "stopService exception suppressed: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            val initialNotification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaPlaybackService", "onCreate startForeground error: ${e.message}")
        }
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld != true) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Azokle:MediaPlaybackWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire(6 * 60 * 60 * 1000L) // 6 hours safety timeout
                }
            }
        } catch (e: Exception) {
            // Ignore security or power exceptions safely
        }

        try {
            if (wifiLock?.isHeld != true) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager?.createWifiLock(
                    lockMode,
                    "Azokle:MediaPlaybackWifiLock"
                )?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            // Ignore wifi lock exceptions safely
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (e: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // Unconditionally fulfill Android's startForeground requirement
            val currentNotification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    currentNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, currentNotification)
            }

            when (intent?.action) {
                ACTION_PLAY -> MediaPlaybackManager.play()
                ACTION_PAUSE -> MediaPlaybackManager.pause()
                ACTION_REWIND -> MediaPlaybackManager.rewind()
                ACTION_FORWARD -> MediaPlaybackManager.fastForward()
                ACTION_STOP -> {
                    MediaPlaybackManager.stop()
                    releaseLocks()
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                    } catch (e: Exception) {}
                    stopSelf()
                    return START_NOT_STICKY
                }
            }

            updateNotification()
        } catch (e: Exception) {
            android.util.Log.e("MediaPlaybackService", "onStartCommand error: ${e.message}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val state = MediaPlaybackManager.mediaState.value
        val isPlaying = state.status == PlaybackStatus.PLAYING
        val meta = state.metadata
        val title = if (meta.title.isNotBlank()) meta.title else "Media Playing"
        val subtitle = if (meta.artist.isNotBlank()) meta.artist else "Azokle Browser"
        val sessionToken = MediaPlaybackManager.mediaSessionCompat?.sessionToken

        // Open App on click
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val rewindPending = PendingIntent.getService(
            this,
            1,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_REWIND },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        val playPausePending = PendingIntent.getService(
            this,
            2,
            Intent(this, MediaPlaybackService::class.java).apply { action = playPauseAction },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val forwardPending = PendingIntent.getService(
            this,
            3,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_FORWARD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this,
            4,
            Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_media_rewind_10, "Rewind 10s", rewindPending)
            .addAction(
                if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePending
            )
            .addAction(R.drawable.ic_media_forward_10, "Forward 10s", forwardPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)

        if (sessionToken != null) {
            builder.setStyle(
                MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopPending)
            )
        }

        val bmp = meta.artworkBitmap
        if (bmp != null && !bmp.isRecycled) {
            try {
                builder.setLargeIcon(bmp)
            } catch (e: Exception) {}
        }

        return builder.build()
    }

    fun updateNotification() {
        try {
            val state = MediaPlaybackManager.mediaState.value
            if (state.status == PlaybackStatus.IDLE || state.status == PlaybackStatus.STOPPED) {
                releaseLocks()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                } catch (e: Exception) {}
                stopSelf()
                return
            }

            val isPlaying = state.status == PlaybackStatus.PLAYING
            if (isPlaying) {
                acquireLocks()
            } else {
                releaseLocks()
            }

            val notification = buildNotification()
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.e("MediaPlaybackService", "updateNotification exception: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background audio and video playback controls"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
