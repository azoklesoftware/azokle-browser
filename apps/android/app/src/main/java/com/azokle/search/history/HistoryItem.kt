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
        val withoutScheme = if (url.startsWith("https://", ignoreCase = true)) {
            url.substring(8)
        } else if (url.startsWith("http://", ignoreCase = true)) {
            url.substring(7)
        } else {
            url.substringAfter("://", url)
        }
        val hostPart = withoutScheme.substringBefore('/').substringBefore('?').substringBefore(':')
        hostPart.removePrefix("www.").ifBlank { url }
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
        "https://www.google.com/s2/favicons?domain=$cleanHost&sz=128"
    }

    val formattedTime: String by lazy(LazyThreadSafetyMode.NONE) {
        try {
            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }
}
