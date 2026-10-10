package com.azokle.search.reader

import android.net.Uri
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

enum class BlockType {
    HEADING,
    PARAGRAPH,
    BLOCKQUOTE
}

data class ArticleBlock(
    val type: BlockType,
    val text: String
)

data class ArticleContent(
    val title: String,
    val byline: String = "",
    val blocks: List<ArticleBlock> = emptyList(),
    val paragraphs: List<String> = emptyList(),
    val estimatedMinutes: Int = 1,
    val leadImageUrl: String? = null
)

/**
 * Azokle Reader Extractor Engine:
 * - In-Realm DOM Cache for instant 0ms reader mode without network overhead or cookie loss.
 * - SSRF Protection: Blocks loopback, private intranet CIDRs, and invalid schemes.
 * - Comprehensive HTML Entity Decoding via Android OS native Html parser.
 * - ReDoS-Safe bounded parser preserving headings, blockquotes, and paragraphs.
 * - Privacy-First Tor SOCKS5 & Clearnet DoH support.
 */
object ReaderExtractor {
    // In-memory normalized URL cache for instant DOM-first retrieval
    private val articleCache = ConcurrentHashMap<String, ArticleContent>()

    private val httpClient = OkHttpClient.Builder()
        .dns(com.azokle.search.core.DnsOverHttpsManager.sharedDns)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun cacheArticle(url: String, article: ArticleContent) {
        val key = normalizeUrl(url)
        if (key.isNotBlank()) {
            articleCache[key] = article
        }
    }

    fun getCachedArticle(url: String): ArticleContent? {
        val key = normalizeUrl(url)
        if (key.isBlank()) return null
        return articleCache[key]
    }

    fun clearCache() {
        articleCache.clear()
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim().lowercase()
        return try {
            val withoutScheme = trimmed.substringAfter("://")
            val withoutQuery = withoutScheme.substringBefore("?").substringBefore("#")
            withoutQuery.trimEnd('/')
        } catch (e: Exception) {
            trimmed
        }
    }

    private fun isSafePublicUrl(url: String, isTor: Boolean): Boolean {
        val trimmed = url.trim().lowercase()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false

        val hostAndRest = trimmed.substringAfter("://")
        val hostWithPort = hostAndRest.substringBefore("/").substringBefore("?").substringBefore("#")
        val host = hostWithPort.substringBefore(":")

        if (isTor && host.endsWith(".onion")) return true

        // Disallow loopback and local domain probing (SSRF defense)
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host.endsWith(".local") || host.endsWith(".internal")) {
            return false
        }

        // Disallow RFC 1918 Private & Link-Local CIDRs
        if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")) {
            return false
        }
        if (host.startsWith("172.")) {
            val parts = host.split(".")
            if (parts.size == 4) {
                val second = parts[1].toIntOrNull()
                if (second != null && second in 16..31) return false
            }
        }

        return true
    }

    private fun getClient(isTor: Boolean, socksPort: Int): OkHttpClient {
        return if (isTor) {
            OkHttpClient.Builder()
                .proxy(java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress.createUnresolved("127.0.0.1", socksPort)))
                .dns(com.azokle.search.tor.TorRemoteDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        } else {
            httpClient
        }
    }

    suspend fun extractArticleFromUrl(
        url: String,
        fallbackTitle: String,
        isTor: Boolean = false,
        socksPort: Int = 9050
    ): ArticleContent = withContext(Dispatchers.IO) {
        // 1. Check in-realm memory cache first
        val cached = getCachedArticle(url)
        if (cached != null) return@withContext cached

        // 2. Validate URL safety (SSRF Defense)
        if (url.isBlank() || url.startsWith("about:", ignoreCase = true)) {
            return@withContext ArticleContent(
                title = fallbackTitle.ifBlank { "Article Reader" },
                byline = "Azokle Reader",
                paragraphs = listOf("This is an internal browser page. Open any news article or blog post to read distilled content."),
                blocks = listOf(ArticleBlock(BlockType.PARAGRAPH, "This is an internal browser page. Open any news article or blog post to read distilled content.")),
                estimatedMinutes = 1
            )
        }

        if (!isSafePublicUrl(url, isTor)) {
            return@withContext ArticleContent(
                title = fallbackTitle.ifBlank { "Restricted Content" },
                byline = "Azokle Shield",
                paragraphs = listOf("Reader Mode is disabled for local and internal network endpoints for your security."),
                blocks = listOf(ArticleBlock(BlockType.PARAGRAPH, "Reader Mode is disabled for local and internal network endpoints for your security.")),
                estimatedMinutes = 1
            )
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val clientToUse = getClient(isTor || url.contains(".onion"), socksPort)
            val rawHtml = clientToUse.newCall(request).execute().use { response ->
                response.body.string()
            }

            if (rawHtml.isBlank()) {
                return@withContext ArticleContent(
                    title = fallbackTitle.ifBlank { "Article Reader" },
                    paragraphs = listOf("Unable to extract article text from this page. You can continue reading in normal browser view.")
                )
            }

            // Bound HTML size to 512KB to avoid ReDoS & memory spikes
            val boundedHtml = if (rawHtml.length > 524288) rawHtml.substring(0, 524288) else rawHtml

            // Safe Non-ReDoS tag cleaner
            var cleanedHtml = boundedHtml
                .replace(Regex("(?is)<script\\b[^>]*>.*?</script>"), "")
                .replace(Regex("(?is)<style\\b[^>]*>.*?</style>"), "")
                .replace(Regex("(?is)<noscript\\b[^>]*>.*?</noscript>"), "")
                .replace(Regex("(?is)<!--.*?-->"), "")

            // Extract Title
            val titleMatcher = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>").matcher(cleanedHtml)
            var extractedTitle = if (titleMatcher.find()) {
                val rawGroup = titleMatcher.group(1) ?: ""
                decodeHtmlEntities(rawGroup.replace(Regex("<.*?>"), "").trim())
            } else {
                val pageTitleMatcher = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(cleanedHtml)
                if (pageTitleMatcher.find()) {
                    val rawGroup = pageTitleMatcher.group(1) ?: ""
                    decodeHtmlEntities(rawGroup.replace(Regex("<.*?>"), "").trim())
                } else fallbackTitle
            }
            if (extractedTitle.isBlank()) extractedTitle = fallbackTitle.ifBlank { "Article View" }

            // Extract Lead Image
            val ogImageMatcher = Pattern.compile("(?is)<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']").matcher(cleanedHtml)
            val leadImageUrl = if (ogImageMatcher.find()) ogImageMatcher.group(1)?.trim() else null

            // Extract Structured Content Blocks (Headings, Paragraphs, Blockquotes)
            val blocksList = mutableListOf<ArticleBlock>()
            val paragraphsList = mutableListOf<String>()
            val elementMatcher = Pattern.compile("(?is)<(p|h2|h3|blockquote)[^>]*>(.*?)</\\1>").matcher(cleanedHtml)

            var totalWords = 0
            while (elementMatcher.find()) {
                val tag = elementMatcher.group(1)?.lowercase() ?: "p"
                val rawInner = elementMatcher.group(2) ?: ""
                val innerText = decodeHtmlEntities(rawInner.replace(Regex("<.*?>"), "").trim())

                // Filter out short noise, cookie banners, navigation links
                if (innerText.length > 25 &&
                    !innerText.contains("cookie", ignoreCase = true) &&
                    !innerText.contains("privacy policy", ignoreCase = true) &&
                    !innerText.contains("terms of service", ignoreCase = true)
                ) {
                    val blockType = when (tag) {
                        "h2", "h3" -> BlockType.HEADING
                        "blockquote" -> BlockType.BLOCKQUOTE
                        else -> BlockType.PARAGRAPH
                    }

                    blocksList.add(ArticleBlock(blockType, innerText))
                    if (blockType == BlockType.PARAGRAPH) {
                        paragraphsList.add(innerText)
                    }
                    totalWords += innerText.split("\\s+".toRegex()).size
                }
            }

            if (blocksList.isEmpty()) {
                val fallbackText = "No main article paragraphs could be extracted from this page. You can read the page directly in the browser."
                blocksList.add(ArticleBlock(BlockType.PARAGRAPH, fallbackText))
                paragraphsList.add(fallbackText)
            }

            val readingTime = (totalWords / 200).coerceAtLeast(1)

            val parsedArticle = ArticleContent(
                title = extractedTitle,
                byline = "Azokle Reader",
                blocks = blocksList,
                paragraphs = if (paragraphsList.isNotEmpty()) paragraphsList else blocksList.map { it.text },
                estimatedMinutes = readingTime,
                leadImageUrl = leadImageUrl
            )

            // Cache for subsequent fast access
            cacheArticle(url, parsedArticle)
            parsedArticle
        } catch (e: Exception) {
            ArticleContent(
                title = fallbackTitle.ifBlank { "Article View" },
                byline = "Azokle Reader",
                paragraphs = listOf("Unable to fetch distilled text: ${e.message ?: "Connection error"}. Please check your network and continue in standard view."),
                blocks = listOf(ArticleBlock(BlockType.PARAGRAPH, "Unable to fetch distilled text: ${e.message ?: "Connection error"}. Please check your network and continue in standard view.")),
                estimatedMinutes = 1
            )
        }
    }

    /**
     * Comprehensive HTML Entity Decoder using Android OS native parser.
     * Decodes all standard, decimal (&#8217;), hex (&#x27;), and named entities.
     */
    private fun decodeHtmlEntities(rawText: String): String {
        return try {
            Html.fromHtml(rawText, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            rawText
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .trim()
        }
    }
}
