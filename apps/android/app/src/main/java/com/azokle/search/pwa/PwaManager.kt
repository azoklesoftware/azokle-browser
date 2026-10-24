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

package com.azokle.search.pwa

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class TabPwaState(
    val currentUrl: String = "",
    val pageTitle: String = "",
    val manifestUrl: String? = null,
    val manifest: WebAppManifest? = null,
    val fallbackFaviconUrl: String? = null,
    val fallbackThemeColor: Int? = null,
    val eligibility: PwaInstallEligibility? = null
)

object PwaManager {
    private const val TAG = "PwaManager"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .dns(com.azokle.search.downloads.DownloadManager.ClearnetSafeDns)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val tabStateMap = ConcurrentHashMap<String, TabPwaState>()

    private val _activeTabPwaState = MutableStateFlow<TabPwaState?>(null)
    val activeTabPwaState: StateFlow<TabPwaState?> = _activeTabPwaState.asStateFlow()

    /**
     * Clears all cached PWA tab states and active eligibility state.
     */
    fun clear() {
        tabStateMap.clear()
        _activeTabPwaState.value = null
    }


    /**
     * Handles on page started events.
     */
    fun onPageStarted(tabId: String, url: String) {
        val newState = TabPwaState(
            currentUrl = url,
            pageTitle = "",
            manifestUrl = null,
            manifest = null,
            eligibility = PwaInstallabilityValidator.evaluate(
                currentUrl = url,
                pageTitle = "",
                manifest = null
            )
        )
        tabStateMap[tabId] = newState
        _activeTabPwaState.value = newState
    }

    /**
     * Handles on title changed events.
     */
    fun onTitleChanged(tabId: String, title: String) {
        val existing = tabStateMap[tabId] ?: TabPwaState()
        val updated = existing.copy(
            pageTitle = title,
            eligibility = PwaInstallabilityValidator.evaluate(
                currentUrl = existing.currentUrl,
                pageTitle = title,
                manifest = existing.manifest,
                fallbackFaviconUrl = existing.fallbackFaviconUrl,
                fallbackThemeColor = existing.fallbackThemeColor
            )
        )
        tabStateMap[tabId] = updated
        _activeTabPwaState.value = updated
    }

    /**
     * Handles on manifest discovered events.
     */
    fun onManifestDiscovered(
        tabId: String,
        manifestUrl: String?,
        manifestJson: String?,
        documentUrl: String,
        pageTitle: String,
        fallbackFaviconUrl: String?,
        fallbackThemeColor: Int?
    ) {
        scope.launch {
            try {
                var parsedManifest: WebAppManifest? = null

                val isTor = com.azokle.search.tor.TorCircuitManager.isTorActive ||
                    com.azokle.search.downloads.DownloadManager.isOnionUrl(documentUrl) ||
                    com.azokle.search.downloads.DownloadManager.isOnionUrl(manifestUrl)

                val isSafeManifestUrl = if (!manifestUrl.isNullOrBlank()) {
                    try {
                        val uri = java.net.URI(manifestUrl)
                        val scheme = uri.scheme?.lowercase()
                        val host = uri.host?.lowercase()?.trimEnd('.') ?: ""
                        val isIpLiteral = (host.all { it.isDigit() || it == '.' } && host.count { it == '.' } == 3) || host.contains(':')
                        if (isIpLiteral) {
                            val addr = java.net.InetAddress.getByName(host)
                            !com.azokle.search.downloads.DownloadManager.isPrivateOrLoopbackAddress(addr)
                        } else if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
                            false
                        } else {
                            scheme == "https" || scheme == "http"
                        }
                    } catch (_: Exception) {
                        false
                    }
                } else false

                if (!manifestJson.isNullOrBlank()) {
                    parsedManifest = WebAppManifest.parse(
                        jsonString = manifestJson,
                        manifestUrl = manifestUrl ?: "",
                        documentUrl = documentUrl
                    )
                } else if (!isTor && isSafeManifestUrl && !manifestUrl.isNullOrBlank() && PwaInstallabilityValidator.isSecureOrigin(manifestUrl)) {
                    // Fetch manifest JSON directly over HTTPS only on clearnet
                    try {
                        val req = Request.Builder()
                            .url(manifestUrl)
                            .header("User-Agent", "Mozilla/5.0 (Android; Mobile) AzokleSearch/1.0")
                            .build()
                        val res = httpClient.newCall(req).execute()
                        if (res.isSuccessful) {
                            val bodyStr = res.body.string()
                            if (!bodyStr.isNullOrBlank()) {
                                parsedManifest = WebAppManifest.parse(
                                    jsonString = bodyStr,
                                    manifestUrl = manifestUrl,
                                    documentUrl = documentUrl
                                )
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to fetch manifest from $manifestUrl: ${e.message}")
                    }
                }

                val eligibility = PwaInstallabilityValidator.evaluate(
                    currentUrl = documentUrl,
                    pageTitle = pageTitle,
                    manifest = parsedManifest,
                    fallbackFaviconUrl = fallbackFaviconUrl,
                    fallbackThemeColor = fallbackThemeColor
                )

                val state = TabPwaState(
                    currentUrl = documentUrl,
                    pageTitle = pageTitle,
                    manifestUrl = manifestUrl,
                    manifest = parsedManifest,
                    fallbackFaviconUrl = fallbackFaviconUrl,
                    fallbackThemeColor = fallbackThemeColor,
                    eligibility = eligibility
                )

                tabStateMap[tabId] = state
                _activeTabPwaState.value = state
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error processing manifest discovery: ${e.message}")
            }
        }
    }

    /**
     * Retrieves the eligibility for tab.
     */
    fun getEligibilityForTab(tabId: String): PwaInstallEligibility? {
        return tabStateMap[tabId]?.eligibility ?: _activeTabPwaState.value?.eligibility
    }
}
