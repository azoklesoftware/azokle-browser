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

    private fun getClient(): OkHttpClient {
        if (com.azokle.search.tor.TorCircuitManager.isTorActive) {
            val socksProxy = java.net.Proxy(
                java.net.Proxy.Type.SOCKS,
                java.net.InetSocketAddress.createUnresolved("127.0.0.1", com.azokle.search.tor.TorCircuitManager.socksPort)
            )
            return OkHttpClient.Builder()
                .proxy(socksProxy)
                .dns(com.azokle.search.tor.TorRemoteDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
        return sharedClient
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Executes the execute cancellably operation.
     */
    private suspend fun Call.executeCancellably(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback {
            /**
             * Handles on response events.
             */
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, _, _ ->
                    response.close()
                }
            }

            /**
             * Handles on failure events.
             */
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }

    /**
     * Retrieves the autosuggestions.
     */
    suspend fun getAutosuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        // AZK-VULN-16: Never query remote autosuggest in Tor mode to prevent keystroke timing analysis and IP leakage
        if (com.azokle.search.tor.TorCircuitManager.isTorActive) return@withContext emptyList()
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

            getClient().newCall(request).executeCancellably().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body.string()

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

    /**
     * Searches for .
     */
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

            getClient().newCall(request).executeCancellably().use { response ->
                val bodyStr = response.body.string()
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
