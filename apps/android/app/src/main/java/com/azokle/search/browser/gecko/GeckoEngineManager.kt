package com.azokle.search.browser.gecko

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
    var currentThemeMode: String = "system"
        private set

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
    }

    @Synchronized
    fun getRuntime(context: Context): GeckoRuntime {
        if (runtime == null) {
            val savedTheme = com.azokle.search.data.PreferencesManager(context).getThemeMode().lowercase()
            currentThemeMode = savedTheme
            val initialScheme = when (savedTheme) {
                "light" -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
                "dark" -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
                else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
            }

            val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val settings = GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .consoleOutput(true)
                .aboutConfigEnabled(false)
                .remoteDebuggingEnabled(isDebug)
                .fissionEnabled(false)
                .lowMemoryDetection(true)
                .enterpriseRootsEnabled(true)
                .globalPrivacyControlEnabled(true)
                .forceUserScalableEnabled(true)
                .doubleTapZoomingEnabled(true)
                .webManifest(true)
                .extensionsProcessEnabled(true)
                .glMsaaLevel(0)
                .isolatedProcessEnabled(true)
                .appZygoteProcessEnabled(true)
                .contentBlocking(
                    ContentBlocking.Settings.Builder()
                        .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                        .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                        .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
                        .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                        .build()
                )
                .build()

            val r = GeckoRuntime.create(context.applicationContext, settings)
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
                        ext.metaData?.let { meta ->
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
                    override fun onConnect(port: WebExtension.Port) {
                        extensionPort = port
                        port.setDelegate(object : WebExtension.PortDelegate {
                            override fun onPortMessage(message: Any, port: WebExtension.Port) {}
                            override fun onDisconnect(port: WebExtension.Port) {
                                if (extensionPort == port) {
                                    extensionPort = null
                                }
                            }
                        })
                        notifyTorStateChanged(
                            com.azokle.search.security.TorCircuitManager.isTorActive,
                            com.azokle.search.security.TorCircuitManager.socksPort
                        )
                    }

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
                                    put("isTorEnabled", com.azokle.search.security.TorCircuitManager.isTorActive)
                                    put("socksPort", com.azokle.search.security.TorCircuitManager.socksPort)
                                    put("theme", currentThemeMode)
                                    val arr = org.json.JSONArray()
                                    com.azokle.search.browser.adblock.AdBlockEngine.instance.getDisabledHostsList().forEach { arr.put(it) }
                                    put("disabledHosts", arr)
                                }
                                "GET_THEME" -> JSONObject().apply {
                                    put("theme", currentThemeMode)
                                }
                                "CHECK_URL" -> {
                                    val url = json.optString("url", "")
                                    val origin = json.optString("origin", "").takeIf { it.isNotBlank() }
                                    val is3p = json.optBoolean("isThirdParty", true)
                                    val decision = com.azokle.search.core.AzokleCore.shouldBlock(url, origin, is3p)
                                    JSONObject().apply {
                                        put("shouldBlock", decision.shouldBlock)
                                        put("category", decision.category)
                                        put("matchedRule", decision.matchedRule)
                                        put("isException", decision.isException)
                                    }
                                }
                                "GET_COSMETIC_CSS" -> {
                                    val domain = json.optString("domain", "")
                                    val css = com.azokle.search.core.AzokleCore.getCosmeticStyles(domain)
                                    JSONObject().apply {
                                        put("css", css)
                                    }
                                }
                                "GET_SCRIPTLETS" -> {
                                    val domain = json.optString("domain", "")
                                    val scriptlet = com.azokle.search.core.AzokleCore.getDefuserScriptlet(domain)
                                    JSONObject().apply {
                                        put("scriptlet", scriptlet)
                                    }
                                }
                                "CLEAN_URL" -> {
                                    val url = json.optString("url", "")
                                    val cleaned = com.azokle.search.core.AzokleCore.cleanTrackingParameters(url)
                                    JSONObject().apply {
                                        put("cleanedUrl", cleaned)
                                    }
                                }
                                "RECORD_BLOCK" -> {
                                    val host = json.optString("host", "").trim().take(128)
                                    val cat = json.optInt("category", 1)
                                    if (host.isNotBlank()) {
                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                                            com.azokle.search.browser.adblock.AdBlockStats.recordBlock(host, cat)
                                        }
                                    }
                                    JSONObject().apply { put("ok", true) }
                                }
                                "PWA_MANIFEST_DISCOVERED" -> {
                                    val manifestUrl = json.optString("manifestUrl", "").takeIf { it.isNotBlank() }
                                    val manifestJson = json.optString("manifestJson", "").takeIf { it.isNotBlank() }
                                    val documentUrl = json.optString("documentUrl", "")
                                    val pageTitle = json.optString("pageTitle", "")
                                    val fallbackIcon = json.optString("fallbackIcon", "").takeIf { it.isNotBlank() }
                                    val themeColorStr = json.optString("themeColor", "").takeIf { it.isNotBlank() }
                                    val themeColor = themeColorStr?.let {
                                        try { android.graphics.Color.parseColor(it) } catch (e: Exception) { null }
                                    }

                                    val activeTabId = com.azokle.search.browser.tabs.TabManager.activeTabId.value ?: "active"
                                    if (fallbackIcon != null) {
                                        com.azokle.search.browser.tabs.TabManager.updateActiveTabFavicon(fallbackIcon)
                                    }
                                    com.azokle.search.browser.pwa.PwaManager.onManifestDiscovered(
                                        tabId = activeTabId,
                                        manifestUrl = manifestUrl,
                                        manifestJson = manifestJson,
                                        documentUrl = documentUrl,
                                        pageTitle = pageTitle,
                                        fallbackFaviconUrl = fallbackIcon,
                                        fallbackThemeColor = themeColor
                                    )
                                    JSONObject().apply { put("ok", true) }
                                }
                                else -> JSONObject().apply {
                                    put("ok", false)
                                    put("error", "unsupported_type")
                                }
                            }
                            GeckoResult.fromValue<Any>(response.toString())
                        } catch (e: Exception) {
                            android.util.Log.e("GeckoEngineManager", "onMessage error: ${e.message}")
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

    fun createSession(
        context: Context? = null,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): GeckoSession {
        val allowBackgroundAudio = context?.let {
            com.azokle.search.data.PreferencesManager(it).isBackgroundAudioEnabled()
        } ?: true

        val sessionSettings = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate || isTor)
            .useTrackingProtection(true)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .suspendMediaWhenInactive(!allowBackgroundAudio)
            .build()

        return GeckoSession(sessionSettings)
    }

    fun clearAllData(context: Context) {
        try {
            getRuntime(context).storageController.clearData(StorageController.ClearFlags.ALL)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun clearHostData(context: Context, host: String) {
        try {
            getRuntime(context).storageController.clearDataFromHost(
                host,
                StorageController.ClearFlags.ALL
            )
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Volatile
    private var shieldExtension: WebExtension? = null
    @Volatile
    private var extensionPort: WebExtension.Port? = null

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
            override fun onPrint(input: java.io.InputStream) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        targetFile.outputStream().use { out ->
                            input.copyTo(out)
                        }
                        com.azokle.search.browser.downloads.DownloadManager.recordCompletedDownload(
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

    fun onTrimMemory(level: Int) {
        try {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                com.azokle.search.browser.tabs.TabManager.reclaimInactiveTabMemory(maxActiveSessions = 1)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun shutdown() {
        runtime?.shutdown()
        runtime = null
    }
}
