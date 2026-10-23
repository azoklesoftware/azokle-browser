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

package com.azokle.search.engine.delegates

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.permissions.SitePermissionState
import com.azokle.search.permissions.SitePermissionType
import com.azokle.search.permissions.SitePermissionsManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Enterprise Two-Tier Origin Security & Hardware Access Delegate extracted from GeckoBrowserView.
 * Handles:
 * 1. Autoplay policies (inaudible & audible)
 * 2. Media DRM / ClearKey streaming (PERMISSION_MEDIA_KEY_SYSTEM_ACCESS)
 * 3. W3C Storage Access API for embedded video iframes (PERMISSION_STORAGE_ACCESS)
 * 4. Origin isolation & insecure HTTP sensor denials
 * 5. Camera & Microphone hardware permissions
 * 6. User permission prompt dispatching
 */
class AzoklePermissionDelegate(
    private val context: Context,
    private val isPrivate: Boolean,
    private val isTor: Boolean,
    private val onPermissionPromptRequested: (SitePermissionRequest) -> Unit,
    private val plugins: List<BrowserPermissionPlugin> = emptyList()
) : GeckoSession.PermissionDelegate {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Handles on content permission request events.
     */
    override fun onContentPermissionRequest(
        session: GeckoSession,
        permRequest: GeckoSession.PermissionDelegate.ContentPermission
    ): GeckoResult<Int>? {
        // Allow plugins to intercept first
        for (plugin in plugins) {
            val result = plugin.onContentPermissionRequest(session, permRequest)
            if (result != null) return result
        }

        val uriStr = permRequest.uri
        val scheme = try { Uri.parse(uriStr).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
        val host = try { Uri.parse(uriStr).host ?: uriStr } catch (e: Exception) { uriStr }

        // 1. Autoplay policy handling
        if (permRequest.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
        }
        if (permRequest.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE) {
            val permManager = SitePermissionsManager.getInstance(context)
            val origin = permManager.canonicalizeOrigin(uriStr)
            val siteState = permManager.getPermission(origin, SitePermissionType.AUTOPLAY, isPrivate = isPrivate || isTor)
            return if (siteState == SitePermissionState.BLOCK) {
                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            } else {
                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }
        }

        // 2. Media DRM / Protected Content handling (allow DRM playback unless in Tor isolation)
        if (permRequest.permission == GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS) {
            return if (isTor) {
                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            } else {
                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }
        }

        // 3. Storage Access API: strictly deny on Tor; evaluate or prompt outside Tor
        if (permRequest.permission == GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS) {
            if (isTor) {
                return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            }
        }

        // 4. Deny all sensors & permissions on insecure HTTP origins (except localhost/testing and Tor onion)
        val isTrustworthyOrigin = scheme == "https" ||
            host.equals("localhost", true) ||
            host.equals("127.0.0.1", true) ||
            host.equals("::1", true) ||
            (isTor && host.endsWith(".onion", ignoreCase = true))

        if (!isTrustworthyOrigin) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }

        val permManager = SitePermissionsManager.getInstance(context)
        val origin = permManager.canonicalizeOrigin(uriStr)
        val isTabPrivate = isPrivate || isTor

        val mappedType = when (permRequest.permission) {
            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> SitePermissionType.GEOLOCATION
            GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> SitePermissionType.NOTIFICATIONS
            GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> SitePermissionType.MEDIA_KEY_SYSTEM
            GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> SitePermissionType.PERSISTENT_STORAGE
            GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> SitePermissionType.AUTOPLAY
            GeckoSession.PermissionDelegate.PERMISSION_XR -> SitePermissionType.VR_AR
            else -> null
        }

        if (mappedType == SitePermissionType.MEDIA_KEY_SYSTEM) {
            return if (isTor) {
                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            } else {
                GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }
        }

        // Hard-deny persistent storage in Private / Tor mode
        if (isTabPrivate && mappedType == SitePermissionType.PERSISTENT_STORAGE) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }

        // AZK-VULN-01: Hard-deny Geolocation and VR/AR in Tor mode to prevent physical coordinates and sensor deanonymization
        if (isTor && (mappedType == SitePermissionType.GEOLOCATION || mappedType == SitePermissionType.VR_AR)) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }

        // Hard-deny unknown hardware / sensor APIs by default
        if (mappedType == null) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }

        val siteState = permManager.getPermission(origin, mappedType, isPrivate = isTabPrivate)
        if (siteState == SitePermissionState.BLOCK) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
        }

        val hasAllAndroidPerms = mappedType.hasAndroidPermissions(context)

        if (siteState == SitePermissionState.ALLOW && hasAllAndroidPerms) {
            return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
        }

        // Needs user prompt
        val geckoResult = GeckoResult<Int>()
        mainHandler.post {
            val requestPayload = SitePermissionRequest(
                host = permManager.getDisplayHost(origin),
                origin = origin,
                uri = uriStr,
                isPrivate = isTabPrivate,
                requestedTypes = listOf(mappedType),
                contentCallback = { grantResult ->
                    geckoResult.complete(grantResult)
                }
            )
            onPermissionPromptRequested(requestPayload)
        }
        return geckoResult
    }

    /**
     * Handles on android permissions request events.
     */
    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>?,
        callback: GeckoSession.PermissionDelegate.Callback
    ) {
        if (permissions == null || permissions.isEmpty()) {
            callback.grant()
            return
        }

        val activity = unwrapActivity(context) as? com.azokle.search.MainActivity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            activity.requestAndroidPermissions(permissions.filterNotNull().toTypedArray(), callback)
        } else {
            val isLocationReq = permissions.any {
                it == android.Manifest.permission.ACCESS_FINE_LOCATION || it == android.Manifest.permission.ACCESS_COARSE_LOCATION
            }
            val locationOk = if (isLocationReq) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true

            val otherOk = permissions.filter {
                it != android.Manifest.permission.ACCESS_FINE_LOCATION && it != android.Manifest.permission.ACCESS_COARSE_LOCATION
            }.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

            if (locationOk && otherOk) {
                callback.grant()
            } else {
                callback.reject()
            }
        }
    }

    /**
     * Handles on media permission request events.
     */
    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback
    ) {
        val scheme = try { Uri.parse(uri).scheme?.lowercase() ?: "" } catch (e: Exception) { "" }
        val host = try { Uri.parse(uri).host ?: uri } catch (e: Exception) { uri }

        // Insecure origins are strictly denied media hardware access
        if (scheme != "https" && !host.equals("localhost", true) && !host.equals("127.0.0.1", true)) {
            callback.reject()
            return
        }

        // AZK-VULN-07: Reject camera, microphone, and screen-sharing in Tor mode to prevent hardware deanonymization & device fingerprinting
        if (isTor) {
            callback.reject()
            return
        }

        val permManager = SitePermissionsManager.getInstance(context)
        val origin = permManager.canonicalizeOrigin(uri)
        val isTabPrivate = isPrivate || isTor

        val hasScreen = video?.any { it.source == GeckoSession.PermissionDelegate.MediaSource.SOURCE_SCREEN } == true
        val requestedTypes = mutableListOf<SitePermissionType>()
        if (hasScreen) {
            requestedTypes.add(SitePermissionType.SCREEN_SHARE)
        } else if (video != null && video.isNotEmpty()) {
            requestedTypes.add(SitePermissionType.CAMERA)
        }
        if (audio != null && audio.isNotEmpty()) {
            requestedTypes.add(SitePermissionType.MICROPHONE)
        }

        if (requestedTypes.isEmpty()) {
            callback.reject()
            return
        }

        val isAnyBlocked = requestedTypes.any { permManager.getPermission(origin, it, isPrivate = isTabPrivate) == SitePermissionState.BLOCK }
        if (isAnyBlocked) {
            callback.reject()
            return
        }

        val isAllAllowed = requestedTypes.all { permManager.getPermission(origin, it, isPrivate = isTabPrivate) == SitePermissionState.ALLOW }
        val hasAllAndroidPerms = requestedTypes.all { it.hasAndroidPermissions(context) }

        if (isAllAllowed && hasAllAndroidPerms) {
            val selectedVideo = video?.let { v ->
                if (hasScreen) {
                    v.firstOrNull { it.source == GeckoSession.PermissionDelegate.MediaSource.SOURCE_SCREEN } ?: v.firstOrNull()
                } else {
                    v.firstOrNull { it.source == GeckoSession.PermissionDelegate.MediaSource.SOURCE_CAMERA } ?: v.firstOrNull()
                }
            }
            val selectedAudio = audio?.firstOrNull()
            callback.grant(selectedVideo, selectedAudio)
            return
        }

        mainHandler.post {
            val requestPayload = SitePermissionRequest(
                host = permManager.getDisplayHost(origin),
                origin = origin,
                uri = uri,
                isPrivate = isTabPrivate,
                requestedTypes = requestedTypes,
                videoSources = video,
                audioSources = audio,
                mediaCallback = callback
            )
            onPermissionPromptRequested(requestPayload)
        }
    }

    /**
     * Executes the unwrap activity operation.
     */
    private fun unwrapActivity(ctx: Context): android.app.Activity? {
        var c = ctx
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) return c
            c = c.baseContext
        }
        return null
    }
}
