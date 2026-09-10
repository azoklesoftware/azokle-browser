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
    fun onPageStart(session: GeckoSession, url: String) {}
    fun onPageStop(session: GeckoSession, success: Boolean) {}
    fun onLocationChange(session: GeckoSession, url: String) {}
    fun onProgressChange(session: GeckoSession, progress: Int) {}
    fun onTitleChange(session: GeckoSession, title: String) {}
    fun onCrash(session: GeckoSession) {}
    fun onKill(session: GeckoSession) {}
    fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {}
}

interface BrowserPermissionPlugin {
    fun onContentPermissionRequest(
        session: GeckoSession,
        permRequest: GeckoSession.PermissionDelegate.ContentPermission
    ): GeckoResult<Int>? = null
}
