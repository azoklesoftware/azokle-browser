package com.azokle.search.data.news

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit

sealed class NewsResult {
    data class Success(val articles: List<NewsArticle>, val isFromCache: Boolean = false) : NewsResult()
    data class Error(val message: String, val cachedArticles: List<NewsArticle> = emptyList()) : NewsResult()
}

/**
 * Sovereign Repository for Fetching, Sanitizing, and Caching Discover News Feeds.
 */
class NewsRepository(private val context: Context) {

    companion object {
        private const val TAG = "AzokleNewsRepository"
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes cache validity
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetches articles for a given category. Tries network first; falls back to disk cache on failure.
     */
    suspend fun getNews(
        category: NewsCategory,
        forceRefresh: Boolean = false
    ): NewsResult = withContext(Dispatchers.IO) {
        val cacheFile = getCacheFile(category)

        // Return fresh cache if not forcing refresh
        if (!forceRefresh && isCacheValid(cacheFile)) {
            val cached = loadFromDisk(cacheFile)
            if (cached.isNotEmpty()) {
                return@withContext NewsResult.Success(cached, isFromCache = true)
            }
        }

        // Determine user locale / region params
        val prefs = com.azokle.search.data.PreferencesManager(context)
        val appLang = prefs.getAppLanguage().lowercase()
        val (hl, gl, ceid) = mapLocaleToRssParams(appLang)

        val feedUrl = category.getFeedUrl(hl = hl, gl = gl, ceid = ceid)

        try {
            val request = Request.Builder()
                .url(feedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:154.0) Gecko/154.0 Azokle/1.0")
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val xmlBody = response.body?.string() ?: ""
            if (xmlBody.isBlank()) {
                throw Exception("Empty feed response")
            }

            val parsedArticles = NewsRssParser.parse(xmlBody, category.id)
            val deduplicated = deduplicateArticles(parsedArticles)

            // Asynchronously resolve direct publisher URLs and true Open Graph thumbnails
            val resolvedArticles = kotlinx.coroutines.coroutineScope {
                deduplicated.map { article ->
                    async(Dispatchers.IO) {
                        val directUrl = if (article.url.contains("news.google.com")) {
                            GoogleNewsUrlDecoder.resolveDirectUrl(article.url)
                        } else {
                            article.url
                        }
                        val directDomain = try {
                            android.net.Uri.parse(directUrl).host?.removePrefix("www.") ?: article.sourceDomain
                        } catch (e: Exception) {
                            article.sourceDomain
                        }
                        val thumbnail = article.thumbnailUrl ?: GoogleNewsUrlDecoder.extractPublisherOgImage(directUrl)
                        article.copy(url = directUrl, sourceDomain = directDomain, thumbnailUrl = thumbnail)
                    }
                }.awaitAll()
            }

            if (resolvedArticles.isNotEmpty()) {
                saveToDisk(cacheFile, resolvedArticles)
                NewsResult.Success(resolvedArticles, isFromCache = false)
            } else {
                val cached = loadFromDisk(cacheFile)
                if (cached.isNotEmpty()) {
                    NewsResult.Success(cached, isFromCache = true)
                } else {
                    NewsResult.Error("No articles found in feed", emptyList())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch live feed for ${category.title}: ${e.message}")
            val cached = loadFromDisk(cacheFile)
            if (cached.isNotEmpty()) {
                NewsResult.Success(cached, isFromCache = true)
            } else {
                NewsResult.Error(e.message ?: "Network error", emptyList())
            }
        }
    }

    private fun deduplicateArticles(articles: List<NewsArticle>): List<NewsArticle> {
        val seenTitles = mutableSetOf<String>()
        val seenUrls = mutableSetOf<String>()
        val result = mutableListOf<NewsArticle>()

        for (art in articles) {
            val normTitle = art.title.lowercase().replace(Regex("[^a-z0-9]"), "")
            val normUrl = art.url.lowercase().split("?")[0]

            if (normTitle.length >= 10 && normTitle in seenTitles) continue
            if (normUrl in seenUrls) continue

            seenTitles.add(normTitle)
            seenUrls.add(normUrl)
            result.add(art)
        }
        return result
    }

    private fun getCacheFile(category: NewsCategory): File {
        return File(context.cacheDir, "news_cache_${category.id}.json")
    }

    private fun isCacheValid(file: File): Boolean {
        if (!file.exists()) return false
        val age = System.currentTimeMillis() - file.lastModified()
        return age < CACHE_EXPIRY_MS
    }

    private fun saveToDisk(file: File, articles: List<NewsArticle>) {
        try {
            val jsonArray = JSONArray()
            articles.take(50).forEach { jsonArray.put(it.toJson()) }
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cache: ${e.message}")
        }
    }

    private fun loadFromDisk(file: File): List<NewsArticle> {
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<NewsArticle>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(NewsArticle.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cache: ${e.message}")
            emptyList()
        }
    }

    private fun mapLocaleToRssParams(lang: String): Triple<String, String, String> {
        return when (lang) {
            "es" -> Triple("es-ES", "ES", "ES:es")
            "de" -> Triple("de-DE", "DE", "DE:de")
            "fr" -> Triple("fr-FR", "FR", "FR:fr")
            "it" -> Triple("it-IT", "IT", "IT:it")
            "pt" -> Triple("pt-BR", "BR", "BR:pt-419")
            "ru" -> Triple("ru-RU", "RU", "RU:ru")
            "ja" -> Triple("ja-JP", "JP", "JP:ja")
            "zh" -> Triple("zh-CN", "CN", "CN:zh-Hans")
            "hi" -> Triple("hi-IN", "IN", "IN:hi")
            "ar" -> Triple("ar-AE", "AE", "AE:ar")
            else -> Triple("en-US", "US", "US:en")
        }
    }
}
