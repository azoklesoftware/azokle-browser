package com.azokle.search.reader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ArticleContent(
    val title: String,
    val byline: String = "",
    val paragraphs: List<String> = emptyList(),
    val estimatedMinutes: Int = 1
)

object ReaderExtractor {
    private val httpClient = OkHttpClient.Builder()
        .dns(com.azokle.search.core.DnsOverHttpsManager.sharedDns)
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

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
        try {
            if (url.isBlank() || url.startsWith("about:", ignoreCase = true)) {
                return@withContext ArticleContent(
                    title = fallbackTitle.ifBlank { "Article Reader" },
                    byline = "Azokle Sovereign Reader",
                    paragraphs = listOf("This is an internal browser page. Open any news article or blog post to read distilled content."),
                    estimatedMinutes = 1
                )
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val clientToUse = getClient(isTor || url.contains(".onion"), socksPort)
            val rawHtml = clientToUse.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }

            if (rawHtml.isBlank()) {
                return@withContext ArticleContent(
                    title = fallbackTitle.ifBlank { "Article Reader" },
                    paragraphs = listOf("Unable to extract article text from this page. You can continue reading in normal browser view.")
                )
            }

            // Clean unwanted tags
            var cleanedHtml = rawHtml
                .replace(Regex("(?is)<script.*?</script>"), "")
                .replace(Regex("(?is)<style.*?</style>"), "")
                .replace(Regex("(?is)<header.*?</header>"), "")
                .replace(Regex("(?is)<footer.*?</footer>"), "")
                .replace(Regex("(?is)<nav.*?</nav>"), "")
                .replace(Regex("(?is)<aside.*?</aside>"), "")
                .replace(Regex("(?is)<noscript.*?</noscript>"), "")
                .replace(Regex("(?is)<!--.*?-->"), "")

            // Extract Title
            val titleMatcher = Pattern.compile("(?is)<h1[^>]*>(.*?)</h1>").matcher(cleanedHtml)
            var extractedTitle = if (titleMatcher.find()) {
                val groupVal = titleMatcher.group(1) ?: ""
                cleanHtmlEntities(groupVal.replace(Regex("<.*?>"), "").trim())
            } else {
                val pageTitleMatcher = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(cleanedHtml)
                if (pageTitleMatcher.find()) {
                    val groupVal = pageTitleMatcher.group(1) ?: ""
                    cleanHtmlEntities(groupVal.replace(Regex("<.*?>"), "").trim())
                } else fallbackTitle
            }

            if (extractedTitle.isBlank()) extractedTitle = fallbackTitle.ifBlank { "Article View" }

            // Extract Paragraphs & Blockquotes
            val paragraphsList = mutableListOf<String>()
            val pMatcher = Pattern.compile("(?is)<(?:p|h2|h3|blockquote)[^>]*>(.*?)</(?:p|h2|h3|blockquote)>").matcher(cleanedHtml)

            var totalWords = 0
            while (pMatcher.find()) {
                val groupVal = pMatcher.group(1) ?: ""
                val innerText = cleanHtmlEntities(groupVal.replace(Regex("<.*?>"), "").trim())
                // Filter out short noise/buttons/prompts
                if (innerText.length > 35 && !innerText.contains("cookie", ignoreCase = true) && !innerText.contains("privacy policy", ignoreCase = true)) {
                    paragraphsList.add(innerText)
                    totalWords += innerText.split("\\s+".toRegex()).size
                }
            }

            if (paragraphsList.isEmpty()) {
                paragraphsList.add("No main article paragraphs could be extracted from this page. You can read the page directly in the browser.")
            }

            val readingTime = (totalWords / 200).coerceAtLeast(1)

            ArticleContent(
                title = extractedTitle,
                byline = "Azokle Sovereign Reader",
                paragraphs = paragraphsList,
                estimatedMinutes = readingTime
            )
        } catch (e: Exception) {
            ArticleContent(
                title = fallbackTitle.ifBlank { "Article View" },
                byline = "Azokle Sovereign Reader",
                paragraphs = listOf("Unable to fetch distilled text: ${e.message ?: "Connection error"}. Please check your network and continue in standard view."),
                estimatedMinutes = 1
            )
        }
    }

    private fun cleanHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
