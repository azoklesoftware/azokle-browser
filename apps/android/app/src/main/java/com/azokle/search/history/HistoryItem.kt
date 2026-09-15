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
    val cleanHost: String = run {
        try {
            val jHost = java.net.URI(url).host
            if (!jHost.isNullOrBlank()) {
                return@run jHost.removePrefix("www.")
            }
        } catch (_: Exception) {}

        try {
            val aHost = android.net.Uri.parse(url).host
            if (!aHost.isNullOrBlank()) {
                return@run aHost.removePrefix("www.")
            }
        } catch (_: Exception) {}

        val stripped = url.substringAfter("://", url).substringBefore('/').substringBefore(':')
        stripped.removePrefix("www.").ifBlank { url }
    }

    val displayTitle: String = if (isSearch && !searchQuery.isNullOrBlank()) {
        searchQuery
    } else if (title.isNotBlank()) {
        title
    } else {
        cleanHost
    }

    val faviconUrl: String = "https://icons.duckduckgo.com/ip3/$cleanHost.ico"
}
