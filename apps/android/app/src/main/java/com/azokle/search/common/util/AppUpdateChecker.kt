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

package com.azokle.search.common.util

import android.content.Context
import com.azokle.search.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val apkDownloadUrl: String? = null
)

/**
 * Privacy-first lightweight update checker for Azokle Browser releases.
 * Checks official release channels without sending user telemetry or identifiers.
 */
object AppUpdateChecker {

    private const val RELEASES_API = "https://api.github.com/repos/azoklesoftware/azokle-browser/releases/latest"

    /**
     * Cleans a version string by stripping non-numeric prefix characters (e.g. 'v-', 'v.', 'v', 'release-').
     */
    fun cleanVersionString(v: String): String {
        if (v.isBlank()) return ""
        val firstDigit = v.indexOfFirst { it.isDigit() }
        if (firstDigit == -1) return v.trim()
        return v.substring(firstDigit).trim()
    }

    /**
     * Checks whether remote semantic version is strictly greater than current version.
     */
    fun isVersionGreater(remote: String, current: String): Boolean {
        val cleanRemote = cleanVersionString(remote)
        val cleanCurrent = cleanVersionString(current)
        if (cleanRemote.isBlank() || cleanCurrent.isBlank()) return false

        val rParts = cleanRemote.split(".").mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }
        val cParts = cleanCurrent.split(".").mapNotNull { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull()
        }

        if (rParts.isEmpty() || cParts.isEmpty()) return false

        for (i in 0 until maxOf(rParts.size, cParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Validates check for updates according to security policies.
     */
    suspend fun checkForUpdates(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(RELEASES_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "AzokleBrowser/${BuildConfig.VERSION_NAME}")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").trim()
                val cleanRemote = cleanVersionString(tagName)
                val notes = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/azoklesoftware/azokle-browser/releases")

                // Extract direct APK asset if present (prefer arm64-v8a or universal)
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    var universalUrl: String? = null
                    var arm64Url: String? = null
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "").lowercase()
                        val download = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk")) {
                            if (name.contains("arm64-v8a")) {
                                arm64Url = download
                            } else if (name.contains("universal")) {
                                universalUrl = download
                            } else if (apkUrl == null) {
                                apkUrl = download
                            }
                        }
                    }
                    apkUrl = arm64Url ?: universalUrl ?: apkUrl
                }

                val currentVersion = cleanVersionString(BuildConfig.VERSION_NAME)
                val isNewer = isVersionGreater(cleanRemote, currentVersion)

                Result.success(
                    UpdateInfo(
                        hasUpdate = isNewer,
                        latestVersion = cleanRemote.ifBlank { BuildConfig.VERSION_NAME },
                        releaseNotes = notes,
                        downloadUrl = htmlUrl,
                        apkDownloadUrl = apkUrl
                    )
                )
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases found
                Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        latestVersion = BuildConfig.VERSION_NAME
                    )
                )
            } else if (responseCode == 403) {
                Result.failure(IOException("GitHub update rate limit reached. Please try again later."))
            } else {
                Result.failure(IOException("Server returned status HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}

