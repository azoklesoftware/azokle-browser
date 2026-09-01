package com.azokle.search.browser.history

data class HistoryItem(
    val id: String,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
    val isSearch: Boolean = false,
    val searchQuery: String? = null
) {
    val cleanHost: String
        get() {
            return try {
                val uri = android.net.Uri.parse(url)
                val host = uri.host ?: url
                host.removePrefix("www.")
            } catch (e: Exception) {
                url
            }
        }

    val displayTitle: String
        get() {
            if (isSearch && !searchQuery.isNullOrBlank()) return searchQuery
            if (title.isNotBlank()) return title
            return cleanHost
        }

    val faviconUrl: String
        get() = "https://icons.duckduckgo.com/ip3/$cleanHost.ico"
}
