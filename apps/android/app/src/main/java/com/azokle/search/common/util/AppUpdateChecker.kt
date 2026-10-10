package com.azokle.search.common.util

import android.content.Context
import com.azokle.search.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

/**
 * Privacy-first lightweight update checker for Azokle Browser releases.
 * Checks official release channels without sending user telemetry or identifiers.
 */
object AppUpdateChecker {

    private const val RELEASES_API = "https://api.github.com/repos/azoklesoftware/azokle-browser/releases/latest"

    suspend fun checkForUpdates(context: Context): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "AzokleBrowser/${BuildConfig.VERSION_NAME}")
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val notes = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/azoklesoftware/azokle-browser/releases")

                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
                val isNewer = isVersionGreater(tagName, currentVersion)

                Result.success(
                    UpdateInfo(
                        hasUpdate = isNewer,
                        latestVersion = tagName.ifBlank { currentVersion },
                        releaseNotes = notes,
                        downloadUrl = htmlUrl
                    )
                )
            } else {
                // Offline fallback or rate-limited
                Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        latestVersion = BuildConfig.VERSION_NAME
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isVersionGreater(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(rParts.size, cParts.size)) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
