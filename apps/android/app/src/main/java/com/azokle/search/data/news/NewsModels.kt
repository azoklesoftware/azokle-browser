package com.azokle.search.data.news

import org.json.JSONObject
import java.util.UUID

/**
 * Supported News Categories for the Sovereign Discover Feed.
 */
enum class NewsCategory(
    val id: String,
    val title: String,
    val topicPath: String?
) {
    TOP_STORIES("top", "Top Stories", null),
    TECHNOLOGY("tech", "Technology", "TECHNOLOGY"),
    BUSINESS("business", "Business", "BUSINESS"),
    SCIENCE("science", "Science", "SCIENCE"),
    HEALTH("health", "Health", "HEALTH"),
    ENTERTAINMENT("entertainment", "Entertainment", "ENTERTAINMENT"),
    SPORTS("sports", "Sports", "SPORTS"),
    WORLD("world", "World", "WORLD");

    fun getFeedUrl(hl: String = "en-US", gl: String = "US", ceid: String = "US:en"): String {
        return if (topicPath == null) {
            "https://news.google.com/rss?hl=$hl&gl=$gl&ceid=$ceid"
        } else {
            "https://news.google.com/rss/headlines/section/topic/$topicPath?hl=$hl&gl=$gl&ceid=$ceid"
        }
    }

    companion object {
        fun fromId(id: String?): NewsCategory {
            return values().find { it.id == id } ?: TOP_STORIES
        }
    }
}

/**
 * Strongly typed, privacy-cleaned News Article Model.
 */
data class NewsArticle(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val sourceName: String,
    val sourceUrl: String? = null,
    val sourceDomain: String = "",
    val thumbnailUrl: String? = null,
    val publishedAtMs: Long = System.currentTimeMillis(),
    val category: String = NewsCategory.TOP_STORIES.id,
    val snippet: String? = null
) {
    val cleanHost: String
        get() {
            if (sourceDomain.isNotBlank()) return sourceDomain.removePrefix("www.")
            return try {
                val uri = android.net.Uri.parse(url)
                val host = uri.host ?: ""
                host.removePrefix("www.")
            } catch (e: Exception) {
                ""
            }
        }

    val faviconUrl: String
        get() {
            val host = cleanHost
            return if (host.isNotBlank()) "https://icons.duckduckgo.com/ip3/$host.ico" else ""
        }

    val timeAgoFormatted: String
        get() {
            val diffMs = System.currentTimeMillis() - publishedAtMs
            if (diffMs < 0) return "Just now"
            val mins = diffMs / 60000
            if (mins < 1) return "Just now"
            if (mins < 60) return "${mins}m ago"
            val hours = mins / 60
            if (hours < 24) return "${hours}h ago"
            val days = hours / 24
            if (days < 7) return "${days}d ago"
            return "${days / 7}w ago"
        }

    val readingTimeMinutes: Int
        get() {
            val wordCount = (title.split(" ").size) + (snippet?.split(" ")?.size ?: 0)
            return (wordCount / 40).coerceAtLeast(2)
        }

    val gradientThemeIndex: Int
        get() = kotlin.math.abs(id.hashCode() + title.hashCode()) % 6

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("url", url)
            put("sourceName", sourceName)
            put("sourceUrl", sourceUrl ?: "")
            put("sourceDomain", sourceDomain)
            put("thumbnailUrl", thumbnailUrl ?: "")
            put("publishedAtMs", publishedAtMs)
            put("category", category)
            put("snippet", snippet ?: "")
        }
    }

    companion object {
        fun fromJson(json: JSONObject): NewsArticle {
            return NewsArticle(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", ""),
                url = json.optString("url", ""),
                sourceName = json.optString("sourceName", "News"),
                sourceUrl = json.optString("sourceUrl", "").takeIf { it.isNotBlank() },
                sourceDomain = json.optString("sourceDomain", ""),
                thumbnailUrl = json.optString("thumbnailUrl", "").takeIf { it.isNotBlank() },
                publishedAtMs = json.optLong("publishedAtMs", System.currentTimeMillis()),
                category = json.optString("category", NewsCategory.TOP_STORIES.id),
                snippet = json.optString("snippet", "").takeIf { it.isNotBlank() }
            )
        }
    }
}
