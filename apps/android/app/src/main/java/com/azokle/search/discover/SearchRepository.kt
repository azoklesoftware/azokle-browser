package com.azokle.search.discover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

data class SearchItem(
    val title: String,
    val url: String,
    val snippet: String,
    val engine: String = "",
    val thumbnail: String? = null
)

data class SearchResultResponse(
    val query: String,
    val page: Int,
    val results: List<SearchItem>,
    val totalResults: Long,
    val searchTime: Double,
    val error: String? = null
)

class SearchRepository(
    private val baseUrl: String = "https://search.azokle.com"
) {
    companion object {
        private val sharedConnectionPool = okhttp3.ConnectionPool(
            maxIdleConnections = 8,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )

        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(sharedConnectionPool)
                .dns(com.azokle.search.core.DnsOverHttpsManager.sharedDns)
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private val client = sharedClient

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun Call.executeCancellably(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, _, _ ->
                    response.close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    suspend fun getAutosuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val jsonBody = JSONObject().apply {
                put("query", trimmed)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/autosuggest")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).executeCancellably().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()

                val jsonArray = JSONArray(bodyStr)
                if (jsonArray.length() >= 2) {
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    val result = mutableListOf<String>()
                    for (i in 0 until suggestionsArray.length()) {
                        result.add(suggestionsArray.getString(i))
                    }
                    return@withContext result
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
        emptyList()
    }

    suspend fun search(
        query: String,
        category: String = "web",
        page: Int = 1,
        location: String = "auto",
        safeSearch: String = "1"
    ): SearchResultResponse = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return@withContext SearchResultResponse(
                query = "",
                page = page,
                results = emptyList(),
                totalResults = 0,
                searchTime = 0.0
            )
        }

        try {
            val urlBuilder = "$baseUrl/api/v1/search".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext SearchResultResponse(query, page, emptyList(), 0, 0.0, "Invalid URL")

            urlBuilder.addQueryParameter("q", trimmed)
            urlBuilder.addQueryParameter("p", page.toString())
            urlBuilder.addQueryParameter("format", "json")
            if (location.isNotEmpty() && location != "auto") {
                urlBuilder.addQueryParameter("lc", location)
            }
            urlBuilder.addQueryParameter("safesearch", safeSearch)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36")
                .build()

            client.newCall(request).executeCancellably().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext SearchResultResponse(
                        query = trimmed,
                        page = page,
                        results = emptyList(),
                        totalResults = 0,
                        searchTime = 0.0,
                        error = "Search failed (HTTP ${response.code})"
                    )
                }

                val json = JSONObject(bodyStr)
                val resultsList = mutableListOf<SearchItem>()
                val jsonResults = json.optJSONArray("results") ?: JSONArray()

                for (i in 0 until jsonResults.length()) {
                    val item = jsonResults.getJSONObject(i)
                    resultsList.add(
                        SearchItem(
                            title = item.optString("title", "Untitled Result"),
                            url = item.optString("url", ""),
                            snippet = item.optString("snippet", item.optString("content", "")),
                            engine = item.optString("engine", ""),
                            thumbnail = if (item.has("thumbnail") && !item.isNull("thumbnail")) item.optString("thumbnail")
                                        else if (item.has("img_src") && !item.isNull("img_src")) item.optString("img_src")
                                        else null
                        )
                    )
                }

                return@withContext SearchResultResponse(
                    query = trimmed,
                    page = page,
                    results = resultsList,
                    totalResults = json.optLong("number_of_results", resultsList.size.toLong()),
                    searchTime = json.optDouble("search_time", 0.12)
                )
            }
        } catch (e: Exception) {
            return@withContext SearchResultResponse(
                query = trimmed,
                page = page,
                results = emptyList(),
                totalResults = 0,
                searchTime = 0.0,
                error = e.localizedMessage ?: "Network connection error"
            )
        }
    }
}
