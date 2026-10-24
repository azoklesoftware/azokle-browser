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

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

enum class DisplayMode {
    STANDALONE,
    FULLSCREEN,
    MINIMAL_UI,
    BROWSER;

    companion object {
        /**
         * Executes the from string operation.
         */
        fun fromString(value: String?): DisplayMode {
            return when (value?.lowercase()?.trim()) {
                "standalone" -> STANDALONE
                "fullscreen" -> FULLSCREEN
                "minimal-ui" -> MINIMAL_UI
                else -> BROWSER
            }
        }
    }
}

data class ManifestIcon(
    val src: String,
    val sizes: String = "",
    val type: String = "",
    val purpose: String = "any" // "any", "maskable", "monochrome"
) {
    /**
     * Retrieves the dimensions.
     */
    fun getDimensions(): Pair<Int, Int> {
        val match = Regex("(\\d+)x(\\d+)").find(sizes)
        return if (match != null) {
            val w = match.groupValues[1].toIntOrNull() ?: 0
            val h = match.groupValues[2].toIntOrNull() ?: 0
            Pair(w, h)
        } else {
            Pair(0, 0)
        }
    }

    /**
     * Checks whether maskable.
     */
    fun isMaskable(): Boolean = purpose.contains("maskable", ignoreCase = true)
}

data class ManifestShortcut(
    val name: String,
    val shortName: String? = null,
    val url: String,
    val icons: List<ManifestIcon> = emptyList()
)

data class WebAppManifest(
    val rawJson: String = "",
    val name: String,
    val shortName: String,
    val description: String = "",
    val startUrl: String,
    val scope: String,
    val display: DisplayMode = DisplayMode.STANDALONE,
    val orientation: String = "any",
    val themeColor: Int? = null,
    val backgroundColor: Int? = null,
    val icons: List<ManifestIcon> = emptyList(),
    val shortcuts: List<ManifestShortcut> = emptyList(),
    val id: String = ""
) {
    /**
     * Retrieves the best icon url.
     */
    fun getBestIconUrl(preferMaskable: Boolean = false): String? {
        if (icons.isEmpty()) return null

        // 1. If preferMaskable, try maskable first
        if (preferMaskable) {
            val maskable = icons.filter { it.isMaskable() }.maxByOrNull { it.getDimensions().first }
            if (maskable != null) return maskable.src
        }

        // 2. Look for best resolution (192 to 512 preferred)
        val sorted = icons.sortedByDescending { it.getDimensions().first }
        val ideal = sorted.find {
            val dim = it.getDimensions().first
            dim in 192..512
        }
        return ideal?.src ?: sorted.firstOrNull()?.src
    }

    companion object {
        /**
         * Parses  from the input data.
         */
        fun parse(
            jsonString: String,
            manifestUrl: String,
            documentUrl: String
        ): WebAppManifest? {
            return try {
                val json = JSONObject(jsonString)
                val baseUrl = if (manifestUrl.isNotBlank()) manifestUrl else documentUrl
                val host = try { URI(baseUrl).host ?: "Web App" } catch (e: Exception) { "Web App" }

                val name = json.optString("name", "").trim()
                val shortName = json.optString("short_name", "").trim()
                val fallbackName = if (name.isNotBlank()) name else (if (shortName.isNotBlank()) shortName else host)

                val rawStartUrl = json.optString("start_url", "").trim()
                val candidateStartUrl = if (rawStartUrl.isNotBlank()) rawStartUrl else documentUrl
                val startUrl = resolveUrl(baseUrl, candidateStartUrl).ifBlank { documentUrl }

                val rawScope = json.optString("scope", "").trim()
                val scope = if (rawScope.isNotBlank()) {
                    resolveUrl(baseUrl, rawScope).ifBlank {
                        val startUri = try { URI(startUrl) } catch (e: Exception) { null }
                        val path = startUri?.path ?: "/"
                        val parentPath = if (path.contains("/")) path.substringBeforeLast('/') + "/" else "/"
                        val scheme = startUri?.scheme ?: "https"
                        val auth = startUri?.authority ?: host
                        "$scheme://$auth$parentPath"
                    }
                } else {
                    // Default scope is the parent path of startUrl
                    val startUri = try { URI(startUrl) } catch (e: Exception) { null }
                    val path = startUri?.path ?: "/"
                    val parentPath = if (path.contains("/")) path.substringBeforeLast('/') + "/" else "/"
                    val scheme = startUri?.scheme ?: "https"
                    val auth = startUri?.authority ?: host
                    "$scheme://$auth$parentPath"
                }

                val display = DisplayMode.fromString(json.optString("display", "standalone"))
                val orientation = json.optString("orientation", "any")

                val themeColor = parseColor(json.optString("theme_color", ""))
                val backgroundColor = parseColor(json.optString("background_color", ""))

                // Parse icons
                val iconsList = mutableListOf<ManifestIcon>()
                val iconsArr = json.optJSONArray("icons")
                if (iconsArr != null) {
                    for (i in 0 until iconsArr.length()) {
                        val iconObj = iconsArr.optJSONObject(i) ?: continue
                        val src = iconObj.optString("src", "").trim()
                        if (src.isBlank()) continue
                        val resolvedSrc = resolveUrl(baseUrl, src, allowDataImage = true)
                        if (resolvedSrc.isBlank()) continue
                        iconsList.add(
                            ManifestIcon(
                                src = resolvedSrc,
                                sizes = iconObj.optString("sizes", ""),
                                type = iconObj.optString("type", ""),
                                purpose = iconObj.optString("purpose", "any")
                            )
                        )
                    }
                }

                // Parse shortcuts
                val shortcutsList = mutableListOf<ManifestShortcut>()
                val shortcutsArr = json.optJSONArray("shortcuts")
                if (shortcutsArr != null) {
                    for (i in 0 until shortcutsArr.length()) {
                        val scObj = shortcutsArr.optJSONObject(i) ?: continue
                        val scName = scObj.optString("name", "").trim()
                        val scUrl = scObj.optString("url", "").trim()
                        if (scName.isBlank() || scUrl.isBlank()) continue
                        val resolvedScUrl = resolveUrl(baseUrl, scUrl)
                        if (resolvedScUrl.isBlank()) continue
                        shortcutsList.add(
                            ManifestShortcut(
                                name = scName,
                                shortName = scObj.optString("short_name", "").takeIf { it.isNotBlank() },
                                url = resolvedScUrl
                            )
                        )
                    }
                }

                val appId = json.optString("id", startUrl).ifBlank { startUrl }

                WebAppManifest(
                    rawJson = jsonString,
                    name = fallbackName,
                    shortName = if (shortName.isNotBlank()) shortName else fallbackName,
                    description = json.optString("description", ""),
                    startUrl = startUrl,
                    scope = scope,
                    display = display,
                    orientation = orientation,
                    themeColor = themeColor,
                    backgroundColor = backgroundColor,
                    icons = iconsList,
                    shortcuts = shortcutsList,
                    id = appId
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * AZK-VULN-11: Resolves and strictly validates URLs against dangerous/privileged schemes.
         * Only http:// and https:// schemes are permitted (with optional data:image/ for icons).
         * Dangerous schemes (javascript:, file:, content:, chrome:, resource:, view-source:) return empty string.
         */
        fun resolveUrl(baseUrl: String, relativeOrAbsolute: String, allowDataImage: Boolean = false): String {
            val trimmed = relativeOrAbsolute.trim()
            if (trimmed.isBlank()) return ""
            if (allowDataImage && trimmed.startsWith("data:image/", ignoreCase = true)) {
                return trimmed
            }
            val resolvedStr = try {
                val base = URI(baseUrl)
                base.resolve(trimmed).toString()
            } catch (e: Exception) {
                return ""
            }
            val resolvedScheme = try {
                URI(resolvedStr).scheme?.lowercase()
            } catch (e: Exception) {
                null
            }
            return if (resolvedScheme == "http" || resolvedScheme == "https") {
                resolvedStr
            } else {
                ""
            }
        }

        /**
         * Parses color from the input data.
         */
        fun parseColor(colorStr: String?): Int? {
            if (colorStr.isNullOrBlank()) return null
            val trimmed = colorStr.trim()
            return try {
                if (trimmed.startsWith("#")) {
                    val hex = trimmed.substring(1)
                    when (hex.length) {
                        3 -> {
                            val r = hex[0].toString().repeat(2).toInt(16)
                            val g = hex[1].toString().repeat(2).toInt(16)
                            val b = hex[2].toString().repeat(2).toInt(16)
                            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                        }
                        6 -> {
                            (0xFF shl 24) or hex.toLong(16).toInt()
                        }
                        8 -> {
                            hex.toLong(16).toInt()
                        }
                        else -> null
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
