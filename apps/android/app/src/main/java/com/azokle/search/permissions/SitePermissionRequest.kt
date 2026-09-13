package com.azokle.search.permissions

import android.Manifest
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import org.mozilla.geckoview.GeckoSession
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

enum class SitePermissionType(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    CAMERA(
        displayName = "Camera",
        description = "Access camera to capture video and photos",
        icon = Icons.Default.CameraAlt
    ),
    MICROPHONE(
        displayName = "Microphone",
        description = "Access microphone to record audio",
        icon = Icons.Default.Mic
    ),
    GEOLOCATION(
        displayName = "Location",
        description = "Access your precise geographical location",
        icon = Icons.Default.LocationOn
    ),
    NOTIFICATIONS(
        displayName = "Notifications",
        description = "Send alerts, sounds, and push notifications",
        icon = Icons.Default.Notifications
    ),
    MEDIA_KEY_SYSTEM(
        displayName = "DRM Content",
        description = "Play protected digital media and DRM content",
        icon = Icons.Default.Lock
    ),
    PERSISTENT_STORAGE(
        displayName = "Persistent Storage",
        description = "Store persistent offline browsing data",
        icon = Icons.Default.Storage
    ),
    AUTOPLAY(
        displayName = "Autoplay",
        description = "Allow videos with audio to play automatically",
        icon = Icons.AutoMirrored.Filled.VolumeUp
    ),
    CLIPBOARD(
        displayName = "Clipboard Access",
        description = "Read text from your clipboard",
        icon = Icons.AutoMirrored.Filled.Assignment
    ),
    SCREEN_SHARE(
        displayName = "Screen Sharing",
        description = "Capture and share your device screen",
        icon = Icons.Default.CameraAlt
    ),
    VR_AR(
        displayName = "Virtual & Augmented Reality",
        description = "Access XR headsets, motion sensors, and 3D tracking",
        icon = Icons.Default.Lock
    );

    val requiredAndroidPermissions: List<String>
        get() = when (this) {
            CAMERA -> listOf(Manifest.permission.CAMERA)
            MICROPHONE -> listOf(Manifest.permission.RECORD_AUDIO)
            GEOLOCATION -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.POST_NOTIFICATIONS)
            } else emptyList()
            else -> emptyList()
        }

    fun hasAndroidPermissions(context: android.content.Context): Boolean {
        return when (this) {
            GEOLOCATION -> {
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            else -> requiredAndroidPermissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
    }
}

enum class SitePermissionState {
    ALLOW,
    BLOCK,
    ASK
}

data class SitePermissionRequest(
    val id: String = UUID.randomUUID().toString(),
    val host: String,
    val origin: String = host,
    val uri: String,
    val isPrivate: Boolean = false,
    val requestedTypes: List<SitePermissionType>,
    val videoSources: Array<out GeckoSession.PermissionDelegate.MediaSource>? = null,
    val audioSources: Array<out GeckoSession.PermissionDelegate.MediaSource>? = null,
    val mediaCallback: GeckoSession.PermissionDelegate.MediaCallback? = null,
    val contentCallback: ((Int) -> Unit)? = null,
    val clipboardCallback: ((org.mozilla.geckoview.AllowOrDeny) -> Unit)? = null
) {
    private val isResolved = AtomicBoolean(false)

    fun grantMedia(
        video: GeckoSession.PermissionDelegate.MediaSource?,
        audio: GeckoSession.PermissionDelegate.MediaSource?
    ) {
        if (mediaCallback != null && isResolved.compareAndSet(false, true)) {
            try {
                mediaCallback.grant(video, audio)
            } catch (e: Exception) {
                android.util.Log.e("SitePermissionRequest", "grantMedia error: ${e.message}")
            }
        }
    }

    fun grantContent(grantValue: Int = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW) {
        if (contentCallback != null && isResolved.compareAndSet(false, true)) {
            try {
                contentCallback.invoke(grantValue)
            } catch (e: Exception) {
                android.util.Log.e("SitePermissionRequest", "grantContent error: ${e.message}")
            }
        }
    }

    fun grantClipboard(allowOrDeny: org.mozilla.geckoview.AllowOrDeny = org.mozilla.geckoview.AllowOrDeny.ALLOW) {
        if (clipboardCallback != null && isResolved.compareAndSet(false, true)) {
            try {
                clipboardCallback.invoke(allowOrDeny)
            } catch (e: Exception) {
                android.util.Log.e("SitePermissionRequest", "grantClipboard error: ${e.message}")
            }
        }
    }

    fun reject(denyValue: Int = GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY) {
        if (isResolved.compareAndSet(false, true)) {
            try {
                mediaCallback?.reject()
            } catch (e: Exception) {
                android.util.Log.e("SitePermissionRequest", "mediaCallback.reject error: ${e.message}")
            }
            try {
                contentCallback?.invoke(denyValue)
            } catch (e: Exception) {
                android.util.Log.e("SitePermissionRequest", "contentCallback reject error: ${e.message}")
            }
            try {
                clipboardCallback?.invoke(org.mozilla.geckoview.AllowOrDeny.DENY)
            } catch (e: Exception) {
                android.util.Log.e("SitePermissionRequest", "clipboardCallback reject error: ${e.message}")
            }
        }
    }
}
