package com.azokle.search.data.news

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
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(java.io.ByteArrayInputStream(xmlString.toByteArray(Charsets.UTF_8)))
            val items = doc.getElementsByTagName("item")

            for (i in 0 until items.length) {
                val itemNode = items.item(i)
                if (itemNode.nodeType != org.w3c.dom.Node.ELEMENT_NODE) continue
                val itemElem = itemNode as org.w3c.dom.Element

                fun getChildText(tagName: String): String? {
                    val nodes = itemElem.getElementsByTagName(tagName)
                    if (nodes.length > 0) {
                        return nodes.item(0).textContent?.trim()
                    }
                    return null
                }

                val title = getChildText("title")
                val link = getChildText("link")
                val pubDate = getChildText("pubDate") ?: getChildText("pubdate")
                val description = getChildText("description")

                var sourceName: String? = null
                var sourceUrl: String? = null
                val sourceNodes = itemElem.getElementsByTagName("source")
                if (sourceNodes.length > 0) {
                    val sElem = sourceNodes.item(0) as? org.w3c.dom.Element
                    sourceName = sElem?.textContent?.trim()
                    sourceUrl = sElem?.getAttribute("url")?.trim()
                }

                var thumbnailUrl: String? = null
                val mediaNodes = itemElem.getElementsByTagName("media:content")
                if (mediaNodes.length > 0) {
                    val mElem = mediaNodes.item(0) as? org.w3c.dom.Element
                    val mUrl = mElem?.getAttribute("url")
                    if (!mUrl.isNullOrBlank()) {
                        thumbnailUrl = sanitizeImageUrl(mUrl)
                    }
                }
                if (thumbnailUrl == null) {
                    val encNodes = itemElem.getElementsByTagName("enclosure")
                    if (encNodes.length > 0) {
                        val eElem = encNodes.item(0) as? org.w3c.dom.Element
                        val type = eElem?.getAttribute("type")
                        val eUrl = eElem?.getAttribute("url")
                        if (!eUrl.isNullOrBlank() && (type == null || type.startsWith("image/"))) {
                            thumbnailUrl = sanitizeImageUrl(eUrl)
                        }
                    }
                }

                val article = buildArticle(
                    rawTitle = title,
                    rawLink = link,
                    rawPubDate = pubDate,
                    rawDescription = description,
                    rawSourceName = sourceName,
                    rawSourceUrl = sourceUrl,
                    rawThumbnailUrl = thumbnailUrl,
                    category = category
                )
                if (article != null) {
                    articles.add(article)
                }
            }
        } catch (e: Exception) {
            // Ignore parse failures
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
            val host = extractHost(trimmed) ?: return null
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
            val questionIdx = urlStr.indexOf('?')
            if (questionIdx == -1) return urlStr

            val base = urlStr.substring(0, questionIdx)
            val queryWithFragment = urlStr.substring(questionIdx + 1)
            val hashIdx = queryWithFragment.indexOf('#')
            val query = if (hashIdx != -1) queryWithFragment.substring(0, hashIdx) else queryWithFragment
            val fragment = if (hashIdx != -1) queryWithFragment.substring(hashIdx) else ""

            val filteredParams = query.split('&')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filter { param ->
                    val key = param.substringBefore('=').lowercase(Locale.ROOT)
                    key !in TRACKING_PARAMS
                }

            if (filteredParams.isEmpty()) {
                "$base$fragment"
            } else {
                "$base?${filteredParams.joinToString("&")}$fragment"
            }
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
            val withoutScheme = if (urlStr.contains("://")) urlStr.substringAfter("://") else urlStr
            val hostPort = withoutScheme.substringBefore('/').substringBefore('?')
            hostPort.substringBefore(':').lowercase(Locale.ROOT)
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
