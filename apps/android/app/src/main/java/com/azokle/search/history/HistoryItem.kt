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

package com.azokle.search.history

data class HistoryItem(
    val id: String,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
    val typedCount: Int = 0,
    val frecency: Long = 1000L,
    val isSearch: Boolean = false,
    val searchQuery: String? = null
) {
    val cleanHost: String by lazy(LazyThreadSafetyMode.NONE) {
        try {
            val parsedHost = android.net.Uri.parse(url).host?.removePrefix("www.")?.trim()
            if (!parsedHost.isNullOrBlank()) {
                parsedHost
            } else {
                fallbackHostExtract(url)
            }
        } catch (_: Exception) {
            fallbackHostExtract(url)
        }
    }

    val displayTitle: String by lazy(LazyThreadSafetyMode.NONE) {
        if (isSearch && !searchQuery.isNullOrBlank()) {
            searchQuery
        } else if (title.isNotBlank()) {
            title
        } else {
            cleanHost
        }
    }

    val faviconUrl: String by lazy(LazyThreadSafetyMode.NONE) {
        if (cleanHost.contains(".")) {
            "https://www.google.com/s2/favicons?domain=$cleanHost&sz=128"
        } else {
            ""
        }
    }

    val formattedTime: String by lazy(LazyThreadSafetyMode.NONE) {
        try {
            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private fun fallbackHostExtract(url: String): String {
            val withoutScheme = if (url.startsWith("https://", ignoreCase = true)) {
                url.substring(8)
            } else if (url.startsWith("http://", ignoreCase = true)) {
                url.substring(7)
            } else {
                url.substringAfter("://", url)
            }
            val hostPart = withoutScheme.substringBefore('/').substringBefore('?').substringBefore(':')
            return hostPart.removePrefix("www.").ifBlank { url }
        }
    }
}
