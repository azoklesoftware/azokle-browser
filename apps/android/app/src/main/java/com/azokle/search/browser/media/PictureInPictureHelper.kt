package com.azokle.search.browser.media

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.azokle.search.R

object PictureInPictureHelper {

    const val ACTION_PIP_PLAY = "com.azokle.search.PIP_PLAY"
    const val ACTION_PIP_PAUSE = "com.azokle.search.PIP_PAUSE"
    const val ACTION_PIP_REWIND = "com.azokle.search.PIP_REWIND"
    const val ACTION_PIP_FORWARD = "com.azokle.search.PIP_FORWARD"

    private const val MIN_ASPECT_RATIO = 1f / 2.39f // ~0.4184
    private const val MAX_ASPECT_RATIO = 2.39f

    fun calculateClampedDimensions(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) {
            return Pair(16, 9)
        }
        val floatRatio = width.toFloat() / height.toFloat()
        return when {
            floatRatio < MIN_ASPECT_RATIO -> Pair(100, 239)
            floatRatio > MAX_ASPECT_RATIO -> Pair(239, 100)
            else -> Pair(width, height)
        }
    }

    fun clampAspectRatio(ratio: Rational?): Rational {
        val (w, h) = if (ratio != null) calculateClampedDimensions(ratio.numerator, ratio.denominator) else Pair(16, 9)
        return Rational(w, h)
    }

    fun supportsPictureInPicture(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildPipParams(
        activity: Activity,
        isPlaying: Boolean,
        isFullscreen: Boolean,
        aspectRatio: Rational? = null,
        sourceRectHint: Rect? = null
    ): PictureInPictureParams {
        val clampedRatio = clampAspectRatio(aspectRatio)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(clampedRatio)

        val rect = if (sourceRectHint != null && sourceRectHint.width() > 0 && sourceRectHint.height() > 0) {
            sourceRectHint
        } else {
            val dm = activity.resources.displayMetrics
            val width = dm.widthPixels
            val targetHeight = (width.toFloat() * clampedRatio.denominator / clampedRatio.numerator).toInt().coerceIn(100, dm.heightPixels)
            Rect(0, 0, width, targetHeight)
        }
        builder.setSourceRectHint(rect)

        // Configure Interactive Actions
        val actions = ArrayList<RemoteAction>()

        // 1. Rewind 10s Action
        val rewindIntent = Intent(ACTION_PIP_REWIND).setPackage(activity.packageName)
        val rewindPendingIntent = PendingIntent.getBroadcast(
            activity,
            101,
            rewindIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(activity, R.drawable.ic_media_rewind_10),
                "Rewind 10s",
                "Rewind 10 seconds",
                rewindPendingIntent
            )
        )

        // 2. Play / Pause Action
        val playPauseActionName = if (isPlaying) ACTION_PIP_PAUSE else ACTION_PIP_PLAY
        val playPauseIntent = Intent(playPauseActionName).setPackage(activity.packageName)
        val playPausePendingIntent = PendingIntent.getBroadcast(
            activity,
            102,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIconRes = if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
        actions.add(
            RemoteAction(
                Icon.createWithResource(activity, playPauseIconRes),
                if (isPlaying) "Pause" else "Play",
                if (isPlaying) "Pause video" else "Play video",
                playPausePendingIntent
            )
        )

        // 3. Forward 10s Action
        val forwardIntent = Intent(ACTION_PIP_FORWARD).setPackage(activity.packageName)
        val forwardPendingIntent = PendingIntent.getBroadcast(
            activity,
            103,
            forwardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(
            RemoteAction(
                Icon.createWithResource(activity, R.drawable.ic_media_forward_10),
                "Forward 10s",
                "Forward 10 seconds",
                forwardPendingIntent
            )
        )

        builder.setActions(actions)

        // Android 12+ (API 31+) Seamless Auto-PiP and Smooth Resizing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isPlaying)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }
}
