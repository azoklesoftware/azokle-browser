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

import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

/**
 * Enterprise declarative plugin contract for GeckoView browser engine events.
 * Inspired by DuckDuckGo Android's modular PluginPoint architecture.
 *
 * Allows privacy shield, Tor controller, PWA scanner, history, and media subsystems
 * to observe and react to browser events without coupling into GeckoBrowserView.
 */
interface BrowserLifecyclePlugin {
    /**
     * Handles on page start events.
     */
    fun onPageStart(session: GeckoSession, url: String) {}
    /**
     * Handles on page stop events.
     */
    fun onPageStop(session: GeckoSession, success: Boolean) {}
    /**
     * Handles on location change events.
     */
    fun onLocationChange(session: GeckoSession, url: String) {}
    /**
     * Handles on progress change events.
     */
    fun onProgressChange(session: GeckoSession, progress: Int) {}
    /**
     * Handles on title change events.
     */
    fun onTitleChange(session: GeckoSession, title: String) {}
    /**
     * Handles on crash events.
     */
    fun onCrash(session: GeckoSession) {}
    /**
     * Handles on kill events.
     */
    fun onKill(session: GeckoSession) {}
    /**
     * Handles on web app manifest events.
     */
    fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {}
}

interface BrowserPermissionPlugin {
    /**
     * Handles on content permission request events.
     */
    fun onContentPermissionRequest(
        session: GeckoSession,
        permRequest: GeckoSession.PermissionDelegate.ContentPermission
    ): GeckoResult<Int>? = null
}
