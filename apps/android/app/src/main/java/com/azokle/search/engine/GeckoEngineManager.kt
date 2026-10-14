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

package com.azokle.search.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension

/**
 * High-Performance Singleton Manager for Mozilla GeckoView Quantum Engine.
 * Configured with JNI stability, tracking protection, theme synchronization, and hardware acceleration.
 *
 * Extension Messaging Architecture (GeckoView Android):
 *  - extension.setMessageDelegate(delegate, "nativeApp") registers handlers
 *  - JS: browser.runtime.sendNativeMessage("nativeApp", msg) → onMessage() responds
 *  - Works ONLY for built-in extensions with geckoViewAddons + nativeMessaging permissions
 */
object GeckoEngineManager {
    private var runtime: GeckoRuntime? = null
    private var applicationContext: Context? = null
    var currentThemeMode: String = "system"
        private set

    /**
     * Retrieves the active shield blocking mode string.
     */
    fun getActiveBlockingMode(): String {
        return applicationContext?.let {
            com.azokle.search.common.util.PreferencesManager.getInstance(it).getShieldBlockingMode()
        } ?: com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_STANDARD
    }

    /**
     * Updates theme mode.
     */
    fun updateThemeMode(mode: String) {
        currentThemeMode = mode.lowercase()
        val scheme = when (currentThemeMode) {
            "light" -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            "dark" -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
            else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        }
        try {
            runtime?.settings?.preferredColorScheme = scheme
        } catch (e: Exception) {
            // Safe fallback
        }
        notifyThemeChanged(currentThemeMode)
    }

    /**
     * Retrieves the runtime.
     */
    @Synchronized
    fun getRuntime(context: Context): GeckoRuntime {
        applicationContext = context.applicationContext
        if (runtime == null) {
            val prefs = com.azokle.search.common.util.PreferencesManager.getInstance(context)
            val savedTheme = prefs.getThemeMode().lowercase()
            val cookiePolicy = prefs.getCookiePolicy()
            val cookieBannerEnabled = prefs.isCookieBannerHandlingEnabled()
            val bannerMode = if (cookieBannerEnabled) {
                ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT
            } else {
                ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED
            }

            currentThemeMode = savedTheme
            val initialScheme = when (savedTheme) {
                "light" -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
                "dark" -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
                else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
            }

            val blockingMode = prefs.getShieldBlockingMode()
            val initialAntiTracking = when (blockingMode) {
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_AGGRESSIVE -> ContentBlocking.AntiTracking.STRICT
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF -> ContentBlocking.AntiTracking.NONE
                else -> ContentBlocking.AntiTracking.DEFAULT
            }
            val initialEtpLevel = when (blockingMode) {
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF -> ContentBlocking.EtpLevel.NONE
                else -> ContentBlocking.EtpLevel.STRICT
            }

            val configFile = GeckoConfigManager.getOrCreateConfigFile(context.applicationContext)
            val settings = GeckoRuntimeSettings.Builder()
                .configFilePath(configFile.absolutePath)
                .javaScriptEnabled(true)
                .consoleOutput(false)
                .aboutConfigEnabled(false)
                .remoteDebuggingEnabled(false)
                .fissionEnabled(false)
                .lowMemoryDetection(false)
                .enterpriseRootsEnabled(true)
                .globalPrivacyControlEnabled(true)
                .forceUserScalableEnabled(true)
                .doubleTapZoomingEnabled(true)
                .inputAutoZoomEnabled(false)
                .webManifest(true)
                .extensionsProcessEnabled(true)
                .glMsaaLevel(0)
                // Keep multi-process tab architecture active while permitting access to Android OS MediaCodec
                // and MediaManager service for HTML5 video (MP4/H.264/AAC/HLS) hardware decoding
                .isolatedProcessEnabled(false)
                .appZygoteProcessEnabled(false)
                .loginAutofillEnabled(true)
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(initialAntiTracking)
                        .enhancedTrackingProtectionLevel(initialEtpLevel)
                        .cookieBehavior(cookiePolicy)
                        .cookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                        .cookiePurging(true)
                        .cookieBannerHandlingMode(bannerMode)
                        .cookieBannerHandlingModePrivateBrowsing(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
                        .cookieBannerGlobalRulesEnabled(true)
                        .cookieBannerGlobalRulesSubFramesEnabled(true)
                        .bounceTrackingProtectionMode(ContentBlocking.BounceTrackingProtectionMode.BOUNCE_TRACKING_PROTECTION_MODE_ENABLED)
                        .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                        .build()
                )
                .build()

            val r = GeckoRuntime.create(context.applicationContext, settings)
            try {
                r.setAutocompleteStorageDelegate(com.azokle.search.vault.AzokleAutocompleteStorageDelegate)
            } catch (e: Throwable) {
                android.util.Log.w("GeckoEngineManager", "Failed to register AutocompleteStorageDelegate: ${e.message}")
            }
            try {
                r.settings.preferredColorScheme = initialScheme
            } catch (e: Throwable) {
                // Safe fallback if native JNI bridge is warming up
            }

            // Clean up any stale extension with undefined/wrong ID from previous installBuiltIn calls
            // This prevents "Addon with ID undefined already installed" error which blocks setMessageDelegate
            r.webExtensionController.list().then({ installedList ->
                val stale = installedList?.find { ext ->
                    ext.id == "undefined" || (ext.id != "shield@azokle.com" &&
                        ext.metaData.let { meta ->
                            meta.name?.contains("Azokle", ignoreCase = true) == true
                        } == true)
                }
                if (stale != null) {
                    android.util.Log.w("GeckoEngineManager", "Removing stale extension: ${stale.id}")
                    r.webExtensionController.uninstall(stale)
                }
                GeckoResult.fromValue(null)
            }, { GeckoResult.fromValue(null) })

            // ensureBuiltIn: installs extension if not present, or updates it if version changed.
            // Requires manifest.json to have browser_specific_settings.gecko.id = "shield@azokle.com"
            r.webExtensionController.ensureBuiltIn(
                "resource://android/assets/extensions/azokle-shield/",
                "shield@azokle.com"
            ).then({ extension ->
                if (extension == null) return@then GeckoResult.fromValue(null)

                shieldExtension = extension

                // FIX: Use MessageDelegate with onMessage.
                // The JS calls: browser.runtime.sendNativeMessage("nativeApp", {type: "..."})
                // GeckoView routes it here. Return type MUST be GeckoResult<JSONObject>.
                // Returning GeckoResult<Any> with a JSONObject inside causes "Invalid event data for callback"
                extension.setMessageDelegate(object : WebExtension.MessageDelegate {
                    /**
                     * Handles on connect events.
                     */
                    override fun onConnect(port: WebExtension.Port) {
                        extensionPort = port
                        port.setDelegate(object : WebExtension.PortDelegate {
                            /**
                             * Handles on port message events.
                             */
                            override fun onPortMessage(message: Any, port: WebExtension.Port) {}
                            /**
                             * Handles on disconnect events.
                             */
                            override fun onDisconnect(port: WebExtension.Port) {
                                if (extensionPort == port) {
                                    extensionPort = null
                                }
                            }
                        })
                        pushFullStatus(port)
                    }

                    /**
                     * Handles on message events.
                     */
                    override fun onMessage(
                        nativeApp: String,
                        message: Any,
                        sender: WebExtension.MessageSender
                    ): GeckoResult<Any>? {
                        // VULN-03: Validate that native messaging originates strictly from the extension background script
                        if (sender.environmentType != WebExtension.MessageSender.ENV_TYPE_EXTENSION) {
                            android.util.Log.w("GeckoEngineManager", "Blocked native message from unauthorized environment: ${sender.environmentType}")
                            return GeckoResult.fromValue<Any>(JSONObject().apply {
                                put("error", "unauthorized_sender")
                            }.toString())
                        }

                        return try {
                            val json = when (message) {
                                is JSONObject -> message
                                is String -> if (message.length < 262144) JSONObject(message) else null
                                else -> null
                            } ?: return GeckoResult.fromValue<Any>(JSONObject().apply { put("error", "invalid_payload") })

                            val type = json.optString("type", "").trim().take(64)

                            val response = when (type) {
                                "GET_APP_STATUS", "GET_TOR_STATUS" -> JSONObject().apply {
                                    put("isTorEnabled", com.azokle.search.tor.TorCircuitManager.isTorActive)
                                    put("socksPort", com.azokle.search.tor.TorCircuitManager.socksPort)
                                    put("theme", currentThemeMode)
                                    put("blockingMode", getActiveBlockingMode())
                                    val arr = org.json.JSONArray()
                                    com.azokle.search.adblock.AdBlockEngine.instance.getDisabledHostsList().forEach { arr.put(it) }
                                    put("disabledHosts", arr)
                                    val exArr = org.json.JSONArray()
                                    com.azokle.search.adblock.AdClickManager.getActiveExemptions().forEach { ex ->
                                        exArr.put(JSONObject().apply {
                                            put("tabId", ex.tabId)
                                            put("advertiserHost", ex.advertiserHost)
                                            put("expiresAtMs", ex.expiresAtMs)
                                        })
                                    }
                                    put("adClickExemptions", exArr)
                                }
                                "GET_THEME" -> JSONObject().apply {
                                    put("theme", currentThemeMode)
                                }
                                "CHECK_URL" -> {
                                    val url = json.optString("url", "")
                                    val origin = json.optString("origin", "").takeIf { it.isNotBlank() }
                                    val is3p = json.optBoolean("isThirdParty", true)
                                    val reqType = json.optString("requestType", "").takeIf { it.isNotBlank() }
                                        ?: json.optString("reqType", "").takeIf { it.isNotBlank() }
                                    val decision = com.azokle.search.core.AzokleCore.shouldBlock(url, origin, reqType, is3p)
                                    JSONObject().apply {
                                        put("shouldBlock", decision.shouldBlock)
                                        put("category", decision.category)
                                        put("matchedRule", decision.matchedRule)
                                        put("isException", decision.isException)
                                        put("redirectUrl", decision.redirectUrl)
                                        put("rewrittenUrl", decision.rewrittenUrl)
                                        put("isReady", decision.isReady)
                                    }
                                }
                                "GET_COSMETIC_CSS" -> {
                                    val domain = json.optString("domain", "").trim().take(256)
                                    val css = com.azokle.search.core.AzokleCore.getCosmeticStyles(domain)
                                    JSONObject().apply {
                                        put("css", css)
                                    }
                                }
                                "GET_SCRIPTLETS" -> {
                                    val domain = json.optString("domain", "").trim().take(256)
                                    val scriptlet = com.azokle.search.core.AzokleCore.getDefuserScriptlet(domain)
                                    JSONObject().apply {
                                        put("scriptlet", scriptlet)
                                    }
                                }
                                "CLEAN_URL" -> {
                                    val url = json.optString("url", "").trim().take(4096)
                                    val cleaned = com.azokle.search.core.AzokleCore.cleanTrackingParameters(url)
                                    JSONObject().apply {
                                        put("cleanedUrl", cleaned)
                                    }
                                }
                                "RECORD_BLOCK" -> {
                                    val host = json.optString("host", "").trim().take(128)
                                    val cat = if (json.has("category")) json.optInt("category") else null
                                    if (host.isNotBlank()) {
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                                            com.azokle.search.adblock.AdBlockStats.recordBlock(host, cat)
                                        }
                                    }
                                    JSONObject().apply { put("ok", true) }
                                }
                                "PWA_MANIFEST_DISCOVERED" -> {
                                    val manifestUrl = json.optString("manifestUrl", "").trim().take(2048).takeIf { it.isNotBlank() }
                                    val manifestJson = json.optString("manifestJson", "").trim().take(262144).takeIf { it.isNotBlank() }
                                    val documentUrl = json.optString("documentUrl", "").trim().take(2048)
                                    val pageTitle = json.optString("pageTitle", "").trim().take(256)
                                    val fallbackIcon = json.optString("fallbackIcon", "").trim().take(2048).takeIf { it.isNotBlank() }
                                    val themeColorStr = json.optString("themeColor", "").trim().take(64).takeIf { it.isNotBlank() }
                                    val themeColor = themeColorStr?.let {
                                        try { android.graphics.Color.parseColor(it) } catch (e: Exception) { null }
                                    }

                                    val activeTab = com.azokle.search.tabs.TabManager.activeTab
                                    val targetTab = if (activeTab != null && (activeTab.url == documentUrl || documentUrl.isBlank())) {
                                        activeTab
                                    } else {
                                        com.azokle.search.tabs.TabManager.tabs.value.find { it.url == documentUrl } ?: activeTab
                                    }
                                    val targetTabId = targetTab?.id ?: (com.azokle.search.tabs.TabManager.activeTabId.value ?: "active")
                                    if (fallbackIcon != null && targetTab != null && targetTab.id == activeTab?.id) {
                                        com.azokle.search.tabs.TabManager.updateActiveTabFavicon(fallbackIcon)
                                    }
                                    com.azokle.search.pwa.PwaManager.onManifestDiscovered(
                                        tabId = targetTabId,
                                        manifestUrl = manifestUrl,
                                        manifestJson = manifestJson,
                                        documentUrl = documentUrl,
                                        pageTitle = pageTitle,
                                        fallbackFaviconUrl = fallbackIcon,
                                        fallbackThemeColor = themeColor
                                    )
                                    JSONObject().apply { put("ok", true) }
                                }
                                "ARTICLE_CONTENT_DISCOVERED" -> {
                                    val url = json.optString("url", "").trim().take(2048)
                                    val title = json.optString("title", "").trim().take(512)
                                    val byline = json.optString("byline", "").trim().take(256)
                                    val leadImageUrl = json.optString("leadImageUrl", "").trim().take(2048).takeIf { it.isNotBlank() }
                                    val readingMinutes = json.optInt("readingMinutes", 1).coerceAtLeast(1)

                                    val blocksArr = json.optJSONArray("blocks")
                                    val blocksList = mutableListOf<com.azokle.search.reader.ArticleBlock>()
                                    if (blocksArr != null) {
                                        for (i in 0 until blocksArr.length()) {
                                            val blockObj = blocksArr.optJSONObject(i)
                                            if (blockObj != null) {
                                                val bTypeStr = blockObj.optString("type", "PARAGRAPH")
                                                val bType = when (bTypeStr) {
                                                    "HEADING" -> com.azokle.search.reader.BlockType.HEADING
                                                    "BLOCKQUOTE" -> com.azokle.search.reader.BlockType.BLOCKQUOTE
                                                    else -> com.azokle.search.reader.BlockType.PARAGRAPH
                                                }
                                                val bText = blockObj.optString("text", "").trim()
                                                if (bText.isNotBlank()) {
                                                    blocksList.add(com.azokle.search.reader.ArticleBlock(bType, bText))
                                                }
                                            }
                                        }
                                    }

                                    val paragraphsArr = json.optJSONArray("paragraphs")
                                    val paragraphsList = mutableListOf<String>()
                                    if (paragraphsArr != null) {
                                        for (i in 0 until paragraphsArr.length()) {
                                            val pText = paragraphsArr.optString(i, "").trim()
                                            if (pText.isNotBlank()) paragraphsList.add(pText)
                                        }
                                    }

                                    if (url.isNotBlank() && (blocksList.isNotEmpty() || paragraphsList.isNotEmpty())) {
                                        val article = com.azokle.search.reader.ArticleContent(
                                            title = title.ifBlank { "Article Reader" },
                                            byline = byline.ifBlank { "Azokle Reader" },
                                            blocks = blocksList,
                                            paragraphs = if (paragraphsList.isNotEmpty()) paragraphsList else blocksList.map { it.text },
                                            estimatedMinutes = readingMinutes,
                                            leadImageUrl = leadImageUrl
                                        )
                                        com.azokle.search.reader.ReaderExtractor.cacheArticle(url, article)
                                    }

                                    JSONObject().apply { put("ok", true) }
                                }
                                else -> JSONObject().apply {
                                    put("ok", false)
                                    put("error", "unsupported_type")
                                }
                            }
                            GeckoResult.fromValue<Any>(response.toString())
                        } catch (e: Throwable) {
                            android.util.Log.e("GeckoEngineManager", "onMessage processing error")
                            GeckoResult.fromValue<Any>("""{"error":"handler_exception"}""")
                        }
                    }
                }, "nativeApp")

                GeckoResult.fromValue(null)
            }, { error ->
                // Log extension install errors for debugging
                android.util.Log.e("GeckoEngineManager", "Extension install error: $error")
                GeckoResult.fromValue(null)
            })

            runtime = r
        }
        return runtime!!
    }

    const val MODERN_MOBILE_UA =
        "Mozilla/5.0 (Android 14; Mobile; rv:154.0) Gecko/154.0 Firefox/154.0"

    const val MODERN_DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64; rv:154.0) Gecko/20100101 Firefox/154.0"

    /**
     * Executes the apply desktop mode operation.
     */
    fun applyDesktopMode(session: GeckoSession, enabled: Boolean) {
        try {
            val targetUaMode = if (enabled) {
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            }
            val targetViewportMode = if (enabled) {
                GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            }
            val targetUaOverride = if (enabled) MODERN_DESKTOP_UA else null

            if (session.settings.userAgentMode != targetUaMode) {
                session.settings.userAgentMode = targetUaMode
            }
            if (session.settings.viewportMode != targetViewportMode) {
                session.settings.viewportMode = targetViewportMode
            }
            if (session.settings.userAgentOverride != targetUaOverride) {
                session.settings.userAgentOverride = targetUaOverride
            }
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to apply desktop mode: ${e.message}")
        }
    }

    /**
     * Executes the create session operation.
     */
    fun createSession(
        context: Context? = null,
        isPrivate: Boolean = false,
        isTor: Boolean = false,
        isDesktopMode: Boolean = false
    ): GeckoSession {
        val allowBackgroundAudio = context?.let {
            com.azokle.search.common.util.PreferencesManager(it).isBackgroundAudioEnabled()
        } ?: true
        val currentBlockingMode = context?.let {
            com.azokle.search.common.util.PreferencesManager.getInstance(it).getShieldBlockingMode()
        } ?: getActiveBlockingMode()
        val useTp = currentBlockingMode != com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF

        val builder = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate || isTor)
            .useTrackingProtection(useTp)
            .userAgentMode(if (isDesktopMode) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(if (isDesktopMode) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .suspendMediaWhenInactive(!allowBackgroundAudio)

        if (isDesktopMode) {
            builder.userAgentOverride(MODERN_DESKTOP_UA)
        }

        return GeckoSession(builder.build())
    }

    /**
     * Clears all all data.
     */
    fun clearAllData(context: Context) {
        try {
            getRuntime(context).storageController.clearData(StorageController.ClearFlags.ALL)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Clears all host data.
     */
    fun clearHostData(context: Context, host: String) {
        if (host.isBlank()) return
        try {
            val sc = getRuntime(context).storageController
            sc.clearDataFromHost(host, StorageController.ClearFlags.ALL)
            val baseDomain = com.azokle.search.vault.OriginMatcher.extractRegistrableDomain(host)
            if (baseDomain.isNotBlank() && !baseDomain.equals(host, ignoreCase = true)) {
                sc.clearDataFromBaseDomain(baseDomain, StorageController.ClearFlags.ALL)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Volatile
    private var shieldExtension: WebExtension? = null
    @Volatile
    private var extensionPort: WebExtension.Port? = null

    /**
     * Executes the push full status operation.
     */
    fun pushFullStatus(port: WebExtension.Port? = extensionPort) {
        val targetPort = port ?: extensionPort ?: return
        try {
            val payload = JSONObject().apply {
                put("type", "PUSH_APP_STATUS")
                put("isTorEnabled", com.azokle.search.tor.TorCircuitManager.isTorActive)
                put("socksPort", com.azokle.search.tor.TorCircuitManager.socksPort)
                put("theme", currentThemeMode)
                put("blockingMode", getActiveBlockingMode())
                val arr = org.json.JSONArray()
                com.azokle.search.adblock.AdBlockEngine.instance.getDisabledHostsList().forEach { arr.put(it) }
                put("disabledHosts", arr)
                val exArr = org.json.JSONArray()
                com.azokle.search.adblock.AdClickManager.getActiveExemptions().forEach { ex ->
                    exArr.put(JSONObject().apply {
                        put("tabId", ex.tabId)
                        put("advertiserHost", ex.advertiserHost)
                        put("expiresAtMs", ex.expiresAtMs)
                    })
                }
                put("adClickExemptions", exArr)
            }
            targetPort.postMessage(payload)
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to push full app status: ${e.message}")
        }
    }

    /**
     * Executes the notify ad click exemptions changed operation.
     */
    fun notifyAdClickExemptionsChanged(exemptions: List<com.azokle.search.adblock.AdClickExemption>) {
        try {
            val payload = JSONObject().apply {
                put("type", "PUSH_AD_CLICK_EXEMPTIONS")
                val arr = org.json.JSONArray()
                exemptions.forEach { ex ->
                    arr.put(JSONObject().apply {
                        put("tabId", ex.tabId)
                        put("advertiserHost", ex.advertiserHost)
                        put("expiresAtMs", ex.expiresAtMs)
                    })
                }
                put("exemptions", arr)
            }
            extensionPort?.postMessage(payload)
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to push ad click exemptions: ${e.message}")
        }
    }

    /**
     * Executes the notify theme changed operation.
     */
    fun notifyThemeChanged(theme: String) {
        try {
            val payload = JSONObject().apply {
                put("type", "PUSH_THEME")
                put("theme", theme)
            }
            extensionPort?.postMessage(payload)
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to push theme status: ${e.message}")
        }
    }

    /**
     * Executes the notify disabled hosts changed operation.
     */
    fun notifyDisabledHostsChanged(hosts: List<String>) {
        try {
            val payload = JSONObject().apply {
                put("type", "PUSH_DISABLED_HOSTS")
                val arr = org.json.JSONArray()
                hosts.forEach { arr.put(it) }
                put("disabledHosts", arr)
            }
            extensionPort?.postMessage(payload)
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to push disabled hosts: ${e.message}")
        }
    }

    /**
     * Executes the notify tor state changed operation.
     */
    fun notifyTorStateChanged(enabled: Boolean, socksPort: Int) {
        try {
            val payload = JSONObject().apply {
                put("type", "PUSH_TOR_STATUS")
                put("isTorEnabled", enabled)
                put("socksPort", socksPort)
            }
            extensionPort?.postMessage(payload)
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to push Tor status: ${e.message}")
        }
    }

    /**
     * Executes the notify engine ready operation.
     */
    fun notifyEngineReady() {
        try {
            val payload = JSONObject().apply {
                put("type", "CLEAR_CACHE")
            }
            extensionPort?.postMessage(payload)
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Failed to push CLEAR_CACHE: ${e.message}")
        }
    }

    /**
     * Executes the print page to pdf operation.
     */
    fun printPageToPdf(
        context: Context,
        session: GeckoSession,
        title: String,
        url: String,
        onComplete: (java.io.File?) -> Unit
    ) {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40).ifBlank { "webpage" }
        val fileName = "$safeTitle-${System.currentTimeMillis()}.pdf"
        val downloadDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val targetFile = java.io.File(downloadDir, fileName)

        session.setPrintDelegate(object : GeckoSession.PrintDelegate {
            /**
             * Handles on print events.
             */
            override fun onPrint(input: java.io.InputStream) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        targetFile.outputStream().use { out ->
                            input.copyTo(out)
                        }
                        com.azokle.search.downloads.DownloadManager.recordCompletedDownload(
                            context = context,
                            url = url,
                            file = targetFile,
                            mimeType = "application/pdf"
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onComplete(targetFile)
                        }
                    } catch (e: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onComplete(null)
                        }
                    }
                }
            }
        })

        session.printPageContent()
    }

    /**
     * Handles on trim memory events.
     */
    @Suppress("DEPRECATION")
    fun onTrimMemory(level: Int) {
        try {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
                level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
            ) {
                com.azokle.search.tabs.TabManager.reclaimInactiveTabMemory(maxActiveSessions = 1)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Clears cookies and site storage for a specific host and its registrable base domain.
     */
    fun clearStorageForHost(
        host: String,
        flags: Long = org.mozilla.geckoview.StorageController.ClearFlags.ALL,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val r = runtime
        if (r == null || host.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        try {
            val baseDomain = com.azokle.search.vault.OriginMatcher.extractRegistrableDomain(host)
            val hostResult = r.storageController.clearDataFromHost(host, flags)
            if (baseDomain.isNotBlank() && !baseDomain.equals(host, ignoreCase = true)) {
                hostResult.then({
                    r.storageController.clearDataFromBaseDomain(baseDomain, flags)
                }, {
                    r.storageController.clearDataFromBaseDomain(baseDomain, flags)
                }).then({ _ ->
                    android.util.Log.i("GeckoEngineManager", "Cleared storage for host $host and base domain $baseDomain")
                    onComplete?.invoke(true)
                    org.mozilla.geckoview.GeckoResult.fromValue(0)
                }, { throwable: Throwable? ->
                    android.util.Log.e("GeckoEngineManager", "Failed to clear storage for domain $baseDomain: ${throwable?.message}")
                    onComplete?.invoke(false)
                    org.mozilla.geckoview.GeckoResult.fromValue(0)
                })
            } else {
                hostResult.then({ _ ->
                    android.util.Log.i("GeckoEngineManager", "Cleared storage for host: $host")
                    onComplete?.invoke(true)
                    org.mozilla.geckoview.GeckoResult.fromValue(0)
                }, { throwable: Throwable? ->
                    android.util.Log.e("GeckoEngineManager", "Failed to clear storage for host $host: ${throwable?.message}")
                    onComplete?.invoke(false)
                    org.mozilla.geckoview.GeckoResult.fromValue(0)
                })
            }
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Exception clearing host storage: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * Clears global cookies and web storage.
     */
    fun clearAllCookiesAndSiteData(
        context: Context? = null,
        flags: Long = org.mozilla.geckoview.StorageController.ClearFlags.ALL,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val r = runtime ?: context?.let { getRuntime(it) }
        if (r == null) {
            onComplete?.invoke(false)
            return
        }

        try {
            r.storageController.clearData(flags).then({ _ ->
                android.util.Log.i("GeckoEngineManager", "Cleared all cookies and site storage (flags: $flags).")
                onComplete?.invoke(true)
                org.mozilla.geckoview.GeckoResult.fromValue(null)
            }, { throwable: Throwable? ->
                android.util.Log.e("GeckoEngineManager", "Failed to clear storage: ${throwable?.message}")
                onComplete?.invoke(false)
                org.mozilla.geckoview.GeckoResult.fromValue(null)
            })
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngineManager", "Exception clearing storage: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * Dynamically updates ContentBlocking cookie policy and cookie banner auto-rejection.
     */
    fun updateCookiePolicy(policy: Int, cookieBannerEnabled: Boolean = true) {
        try {
            val bannerMode = if (cookieBannerEnabled) {
                ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT
            } else {
                ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED
            }
            runtime?.settings?.contentBlocking?.let { cb ->
                cb.setCookieBehavior(policy)
                cb.setCookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                cb.setCookieBannerMode(bannerMode)
                cb.setCookieBannerModePrivateBrowsing(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
                cb.setCookiePurging(true)
                cb.setCookieBannerGlobalRulesEnabled(true)
                cb.setBounceTrackingProtectionMode(ContentBlocking.BounceTrackingProtectionMode.BOUNCE_TRACKING_PROTECTION_MODE_ENABLED)
            }
            android.util.Log.i("GeckoEngineManager", "Dynamically updated cookie policy to $policy (Banner mode: $bannerMode)")
        } catch (e: Exception) {
            android.util.Log.w("GeckoEngineManager", "Failed to update dynamic cookie policy: ${e.message}")
        }
    }

    /**
     * Dynamically updates Ad & Tracker Blocker, Fingerprinting protection, and Cookie policies across active engine.
     */
    fun updateShieldSettings(context: Context) {
        try {
            val prefs = com.azokle.search.common.util.PreferencesManager.getInstance(context)
            val blockingMode = prefs.getShieldBlockingMode()
            val cookiePolicy = prefs.getCookiePolicy()
            val cookieBannerEnabled = prefs.isCookieBannerHandlingEnabled()

            val antiTracking = when (blockingMode) {
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_AGGRESSIVE -> ContentBlocking.AntiTracking.STRICT
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF -> ContentBlocking.AntiTracking.NONE
                else -> ContentBlocking.AntiTracking.DEFAULT
            }
            val etpLevel = when (blockingMode) {
                com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF -> ContentBlocking.EtpLevel.NONE
                else -> ContentBlocking.EtpLevel.STRICT
            }
            val bannerMode = if (cookieBannerEnabled) {
                ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT
            } else {
                ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED
            }

            runtime?.settings?.contentBlocking?.let { cb ->
                cb.setAntiTracking(antiTracking)
                cb.setEnhancedTrackingProtectionLevel(etpLevel)
                cb.setCookieBehavior(cookiePolicy)
                cb.setCookieBehaviorPrivateMode(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                cb.setCookieBannerMode(bannerMode)
                cb.setCookieBannerModePrivateBrowsing(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
                cb.setCookiePurging(true)
                cb.setCookieBannerGlobalRulesEnabled(true)
                cb.setBounceTrackingProtectionMode(ContentBlocking.BounceTrackingProtectionMode.BOUNCE_TRACKING_PROTECTION_MODE_ENABLED)
            }

            // Regenerate config yaml with updated fingerprinting / cookie / tracker prefs
            GeckoConfigManager.getOrCreateConfigFile(context.applicationContext)

            // Dynamically update warm tab sessions tracking protection
            val useTp = blockingMode != com.azokle.search.common.util.PreferencesManager.BLOCKING_MODE_OFF
            com.azokle.search.tabs.TabManager.updateTrackingProtectionOnActiveSessions(useTp)

            // Notify extension port so background ad blocker syncs state
            pushFullStatus()

            android.util.Log.i("GeckoEngineManager", "Shield settings dynamically applied: mode=$blockingMode, cookiePolicy=$cookiePolicy")
        } catch (e: Exception) {
            android.util.Log.w("GeckoEngineManager", "Failed to dynamically update shield settings: ${e.message}")
        }
    }

    /**
     * Executes the shutdown operation.
     */
    fun shutdown() {
        runtime?.shutdown()
        runtime = null
    }
}
