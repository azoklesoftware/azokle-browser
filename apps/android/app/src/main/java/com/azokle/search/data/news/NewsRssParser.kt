package com.azokle.search.data.news

import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Privacy-First, High-Performance Streaming RSS Parser for Discover News.
 * Extracts original publisher metadata, eliminates tracking parameters,
 * and strips Google-hosted image proxies.
 */
object NewsRssParser {

    private val RFC822_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }
    private val RFC822_NO_SEC_FORMAT = SimpleDateFormat("EEE, dd MMM yyyy HH:mm z", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    private val GOOGLE_PROXY_DOMAINS = listOf(
        "googleusercontent.com",
        "gstatic.com",
        "google.com",
        "googleapis.com"
    )

    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "ved", "usg", "oc", "hl", "gl", "fbclid", "gclid", "msclkid", "ref", "source"
    )

    /**
     * Parses an RSS XML stream into normalized, sanitized NewsArticle items.
     */
    fun parse(xmlString: String, category: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlString))

            var eventType = parser.eventType
            var insideItem = false

            var currentTitle: String? = null
            var currentLink: String? = null
            var currentPubDate: String? = null
            var currentDescription: String? = null
            var currentSourceName: String? = null
            var currentSourceUrl: String? = null
            var currentThumbnailUrl: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.ROOT)

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item") {
                            insideItem = true
                            currentTitle = null
                            currentLink = null
                            currentPubDate = null
                            currentDescription = null
                            currentSourceName = null
                            currentSourceUrl = null
                            currentThumbnailUrl = null
                        } else if (insideItem) {
                            when (tagName) {
                                "title" -> currentTitle = parser.nextText().trim()
                                "link" -> currentLink = parser.nextText().trim()
                                "pubdate" -> currentPubDate = parser.nextText().trim()
                                "description" -> currentDescription = parser.nextText().trim()
                                "source" -> {
                                    currentSourceUrl = parser.getAttributeValue(null, "url")?.trim()
                                    currentSourceName = parser.nextText().trim()
                                }
                                "media:content", "enclosure" -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    val type = parser.getAttributeValue(null, "type")
                                    if (!url.isNullOrBlank() && (type == null || type.startsWith("image/"))) {
                                        val sanitizedImg = sanitizeImageUrl(url)
                                        if (sanitizedImg != null && currentThumbnailUrl == null) {
                                            currentThumbnailUrl = sanitizedImg
                                        }
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" && insideItem) {
                            insideItem = false
                            val article = buildArticle(
                                rawTitle = currentTitle,
                                rawLink = currentLink,
                                rawPubDate = currentPubDate,
                                rawDescription = currentDescription,
                                rawSourceName = currentSourceName,
                                rawSourceUrl = currentSourceUrl,
                                rawThumbnailUrl = currentThumbnailUrl,
                                category = category
                            )
                            if (article != null) {
                                articles.add(article)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            android.util.Log.e("NewsRssParser", "Error parsing RSS: ${e.message}")
        }
        return articles
    }

    private fun buildArticle(
        rawTitle: String?,
        rawLink: String?,
        rawPubDate: String?,
        rawDescription: String?,
        rawSourceName: String?,
        rawSourceUrl: String?,
        rawThumbnailUrl: String?,
        category: String
    ): NewsArticle? {
        if (rawTitle.isNullOrBlank() || rawLink.isNullOrBlank()) return null

        // 1. Clean Title and Extract Publisher Suffix (e.g. "SpaceX Launches Starship - SpaceNews")
        var title = rawTitle
        var sourceName = rawSourceName

        if (title.contains(" - ")) {
            val lastDashIdx = title.lastIndexOf(" - ")
            val potentialSource = title.substring(lastDashIdx + 3).trim()
            val mainTitle = title.substring(0, lastDashIdx).trim()

            if (mainTitle.isNotBlank() && potentialSource.isNotBlank() && potentialSource.length < 50) {
                title = mainTitle
                if (sourceName.isNullOrBlank()) {
                    sourceName = potentialSource
                }
            }
        }

        if (sourceName.isNullOrBlank()) {
            sourceName = extractDomainName(rawSourceUrl ?: rawLink) ?: "News"
        }

        // 2. Extract Source Domain
        val sourceDomain = extractHost(rawSourceUrl) ?: extractHost(rawLink) ?: ""

        // 3. Clean Outbound Link (Remove tracking parameters)
        val cleanUrl = cleanUrlTracking(rawLink)

        // 4. Parse Published Date
        val publishedAtMs = parseDateToMillis(rawPubDate)

        // 5. Extract Snippet & Image from HTML Description if not already found
        var snippet: String? = null
        var thumbnail = rawThumbnailUrl

        if (!rawDescription.isNullOrBlank()) {
            if (thumbnail == null) {
                thumbnail = extractImageFromHtml(rawDescription)
            }
            snippet = cleanHtmlText(rawDescription)
        }

        return NewsArticle(
            id = UUID.nameUUIDFromBytes(cleanUrl.toByteArray()).toString(),
            title = title,
            url = cleanUrl,
            sourceName = sourceName,
            sourceUrl = rawSourceUrl,
            sourceDomain = sourceDomain,
            thumbnailUrl = thumbnail,
            publishedAtMs = publishedAtMs,
            category = category,
            snippet = snippet
        )
    }

    /**
     * Sanitizes image URLs: Rejects Google proxy / cache images; accepts only legitimate publisher images.
     */
    fun sanitizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return null
        }

        return try {
            val host = Uri.parse(trimmed).host?.lowercase(Locale.ROOT) ?: return null
            val isGoogleProxy = GOOGLE_PROXY_DOMAINS.any { host == it || host.endsWith(".$it") }
            if (isGoogleProxy) null else trimmed
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Strips tracking query parameters from URLs.
     */
    fun cleanUrlTracking(urlStr: String): String {
        return try {
            val uri = Uri.parse(urlStr)
            if (!uri.isHierarchical) return urlStr

            val queryParamNames = uri.queryParameterNames
            if (queryParamNames.isEmpty()) return urlStr

            val builder = uri.buildUpon().clearQuery()
            for (param in queryParamNames) {
                if (param.lowercase(Locale.ROOT) !in TRACKING_PARAMS) {
                    builder.appendQueryParameter(param, uri.getQueryParameter(param))
                }
            }
            builder.build().toString()
        } catch (e: Exception) {
            urlStr
        }
    }

    private fun extractImageFromHtml(html: String): String? {
        val imgTagRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val match = imgTagRegex.find(html) ?: return null
        val src = match.groupValues.getOrNull(1) ?: return null
        return sanitizeImageUrl(src)
    }

    private fun cleanHtmlText(html: String): String? {
        val plainText = html
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()

        return if (plainText.length > 180) plainText.take(177) + "..." else plainText.takeIf { it.isNotBlank() }
    }

    private fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        try {
            val parsed = synchronized(RFC822_FORMAT) { RFC822_FORMAT.parse(dateStr) }
            if (parsed != null) return parsed.time
        } catch (e: Exception) {}

        try {
            val parsed = synchronized(RFC822_NO_SEC_FORMAT) { RFC822_NO_SEC_FORMAT.parse(dateStr) }
            if (parsed != null) return parsed.time
        } catch (e: Exception) {}

        return System.currentTimeMillis()
    }

    private fun extractHost(urlStr: String?): String? {
        if (urlStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(urlStr)
            uri.host?.lowercase(Locale.ROOT)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractDomainName(urlStr: String?): String? {
        val host = extractHost(urlStr) ?: return null
        val clean = host.removePrefix("www.")
        val parts = clean.split(".")
        return if (parts.isNotEmpty()) {
            parts[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            clean
        }
    }
}
