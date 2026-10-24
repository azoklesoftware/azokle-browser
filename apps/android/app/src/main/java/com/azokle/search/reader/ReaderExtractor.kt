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

package com.azokle.search.reader

import android.net.Uri
import android.text.Html
import android.util.Log
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
    private const val TAG = "ReaderExtractor"
    private const val MAX_CACHED_ARTICLES = 50

    // Bounded LRU in-memory cache to prevent OutOfMemoryError DoS
    private val articleCache = object : LinkedHashMap<String, ArticleContent>(MAX_CACHED_ARTICLES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ArticleContent>?): Boolean {
            return size > MAX_CACHED_ARTICLES
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .dns(com.azokle.search.core.DnsOverHttpsManager.sharedDns)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    /**
     * Executes the cache article operation.
     */
    @Synchronized
    fun cacheArticle(url: String, article: ArticleContent) {
        val key = normalizeUrl(url)
        if (key.isNotBlank()) {
            articleCache[key] = article
        }
    }

    /**
     * Retrieves the cached article.
     */
    @Synchronized
    fun getCachedArticle(url: String): ArticleContent? {
        val key = normalizeUrl(url)
        if (key.isBlank()) return null
        return articleCache[key]
    }

    /**
     * Clears all cache.
     */
    @Synchronized
    fun clearCache() {
        articleCache.clear()
    }

    /**
     * Executes the normalize url operation.
     */
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

    /**
     * Evaluates whether an IP address belongs to a private network, loopback, or link-local subnet.
     * Hardened against RFC 1918, RFC 3927 (169.254.x.x cloud metadata), IPv6 ULA, and IPv4-mapped loopback.
     */
    internal fun isPrivateOrLoopbackIp(inetAddr: java.net.InetAddress): Boolean {
        return com.azokle.search.downloads.DownloadManager.isPrivateOrLoopbackAddress(inetAddr)
    }

    /**
     * Checks whether safe public url.
     */
    internal fun isSafePublicUrl(url: String, isTor: Boolean): Boolean {
        val trimmed = url.trim().lowercase()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false

        val hostAndRest = trimmed.substringAfter("://")
        val hostWithPort = hostAndRest.substringBefore("/").substringBefore("?").substringBefore("#")
        val authority = if (hostWithPort.contains('@')) hostWithPort.substringAfterLast('@') else hostWithPort
        val cleanHost = if (authority.startsWith("[")) {
            authority.substringAfter("[").substringBefore("]")
        } else {
            authority.substringBefore(":")
        }.trim().substringBefore("%").trimEnd('.')

        if (cleanHost.isBlank()) return false

        // AZK-VULN-07: Handle .onion hidden service isolation
        val isOnion = cleanHost == "onion" || cleanHost.endsWith(".onion")
        if (isOnion) {
            // Never attempt to resolve or access .onion endpoints over clearnet (prevents DNS leaks).
            // In Tor mode, .onion is routed via SOCKS5/Tor circuit; return true without local DNS lookup.
            return isTor
        }

        // Disallow loopback and local domain probing (SSRF defense)
        if (cleanHost == "localhost" || cleanHost == "127.0.0.1" || cleanHost == "::1" ||
            cleanHost.endsWith(".localhost") || cleanHost.endsWith(".local") || cleanHost.endsWith(".internal")) {
            return false
        }

        // AZK-SEC-01: In Tor mode, NEVER execute host OS DNS resolution (java.net.InetAddress.getAllByName)
        // for domain names. Doing so leaks visited clearnet domains over unencrypted local/cellular DNS.
        // In Tor mode, OkHttp SOCKS5 proxy handles remote DNS resolution securely over the onion circuit.
        // We only inspect raw IP literals locally to block private subnets.
        val isIpLiteral = (cleanHost.all { it.isDigit() || it == '.' } && cleanHost.count { it == '.' } == 3) || cleanHost.contains(':')
        if (isTor) {
            if (isIpLiteral) {
                try {
                    val addr = java.net.InetAddress.getByName(cleanHost)
                    if (isPrivateOrLoopbackIp(addr)) return false
                } catch (_: Exception) {
                    return false
                }
            }
            return true
        }

        // Validate IP literals and resolve hostname to block private IP and cloud metadata SSRF
        try {
            val addrs = java.net.InetAddress.getAllByName(cleanHost)
            if (addrs.any { isPrivateOrLoopbackIp(it) }) {
                return false
            }
        } catch (_: Exception) {
            // Cannot resolve or offline; if it's an onion in Tor mode, handled above, else safe to fail closed or let network handle
        }

        return true
    }

    /**
     * Retrieves the client.
     */
    private fun getClient(isTor: Boolean, socksPort: Int): OkHttpClient {
        return if (isTor) {
            OkHttpClient.Builder()
                .proxy(java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress.createUnresolved("127.0.0.1", socksPort)))
                .dns(com.azokle.search.tor.TorRemoteDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false)
                .build()
        } else {
            httpClient
        }
    }

    /**
     * Executes the extract article from url operation.
     */
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
            var currentUrl = url
            var redirectCount = 0
            var rawHtml = ""
            val clientToUse = getClient(isTor || com.azokle.search.downloads.DownloadManager.isOnionUrl(url), socksPort)

            // AZK-VULN-08: Follow redirects safely with step-by-step SSRF validation (max 5 hops)
            while (redirectCount <= 5) {
                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()

                val response = clientToUse.newCall(request).execute()
                if (response.isRedirect) {
                    val location = response.header("Location")
                    val resolvedUrl = location?.let { response.request.url.resolve(it)?.toString() }
                    response.close()

                    if (resolvedUrl == null || !isSafePublicUrl(resolvedUrl, isTor)) {
                        Log.w(TAG, "Blocked unsafe redirect in Reader Mode: $location (resolved: $resolvedUrl)")
                        return@withContext ArticleContent(
                            title = fallbackTitle.ifBlank { "Restricted Content" },
                            byline = "Azokle Shield",
                            paragraphs = listOf("Reader Mode is disabled for local and internal network endpoints for your security."),
                            blocks = listOf(ArticleBlock(BlockType.PARAGRAPH, "Reader Mode is disabled for local and internal network endpoints for your security.")),
                            estimatedMinutes = 1
                        )
                    }
                    currentUrl = resolvedUrl
                    redirectCount++
                } else {
                    rawHtml = response.use { it.body.string() }
                    break
                }
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
                .replace(Regex("(?is)<script\\b[^>]*>.*?</script[^>]*>"), "")
                .replace(Regex("(?is)<style\\b[^>]*>.*?</style[^>]*>"), "")
                .replace(Regex("(?is)<noscript\\b[^>]*>.*?</noscript[^>]*>"), "")
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
            val rawLeadImageUrl = if (ogImageMatcher.find()) ogImageMatcher.group(1)?.trim() else null
            val leadImageUrl = if (!rawLeadImageUrl.isNullOrBlank() && isSafePublicUrl(rawLeadImageUrl, isTor)) {
                rawLeadImageUrl
            } else null


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
