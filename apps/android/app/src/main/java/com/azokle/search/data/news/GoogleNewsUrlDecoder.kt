package com.azokle.search.data.news

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Sovereign Direct URL Resolver for Google News RSS articles.
 * Decodes Google's encrypted `CBMi...` tokens into the true, original publisher URLs
 * with zero Google tracking, intermediate splash screens, or Google redirect hops.
 */
object GoogleNewsUrlDecoder {

    private const val TAG = "GoogleNewsUrlDecoder"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // LRU / In-memory resolution cache
    private val urlCache = ConcurrentHashMap<String, String>()

    private val SIGNATURE_PATTERN = Pattern.compile("data-n-a-sg=\"([^\"]+)\"")
    private val TIMESTAMP_PATTERN = Pattern.compile("data-n-a-ts=\"([^\"]+)\"")

    /**
     * Resolves a Google News RSS article URL to the direct publisher destination URL.
     * If resolution fails or is not a Google News URL, returns the original URL with stripped tracking.
     */
    suspend fun resolveDirectUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        val cleanInput = NewsRssParser.cleanUrlTracking(rawUrl)

        // Check if not a Google News URL
        if (!cleanInput.contains("news.google.com")) {
            return@withContext cleanInput
        }

        // Check in-memory cache
        urlCache[cleanInput]?.let { cached ->
            return@withContext cached
        }

        val base64Token = extractBase64Token(cleanInput)
        if (base64Token.isNullOrBlank()) {
            return@withContext cleanInput
        }

        try {
            // Step 1: Fetch signing parameters from Google News article endpoint
            val params = fetchDecodingParams(base64Token)
            if (params != null) {
                // Step 2: Execute RPC to decode true publisher destination URL
                val decodedUrl = executeDecodeRpc(params.signature, params.timestamp, base64Token)
                if (!decodedUrl.isNullOrBlank() && (decodedUrl.startsWith("http://") || decodedUrl.startsWith("https://"))) {
                    val cleanDirectUrl = NewsRssParser.cleanUrlTracking(decodedUrl)
                    urlCache[cleanInput] = cleanDirectUrl
                    Log.d(TAG, "Successfully resolved Google News URL directly to: $cleanDirectUrl")
                    return@withContext cleanDirectUrl
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve direct URL for $cleanInput: ${e.message}")
        }

        cleanInput
    }

    private data class DecodingParams(
        val signature: String,
        val timestamp: String
    )

    private fun fetchDecodingParams(base64Token: String): DecodingParams? {
        val urlsToTry = listOf(
            "https://news.google.com/articles/$base64Token",
            "https://news.google.com/rss/articles/$base64Token"
        )

        for (url in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) continue

                val html = response.body?.string() ?: continue

                val sgMatcher = SIGNATURE_PATTERN.matcher(html)
                val tsMatcher = TIMESTAMP_PATTERN.matcher(html)

                if (sgMatcher.find() && tsMatcher.find()) {
                    val signature = sgMatcher.group(1)
                    val timestamp = tsMatcher.group(1)
                    if (!signature.isNullOrBlank() && !timestamp.isNullOrBlank()) {
                        return DecodingParams(signature, timestamp)
                    }
                }
            } catch (e: Exception) {
                // Try next URL fallback
            }
        }
        return null
    }

    private fun executeDecodeRpc(signature: String, timestamp: String, base64Token: String): String? {
        try {
            val rpcUrl = "https://news.google.com/_/DotsSplashUi/data/batchexecute"

            val innerReq = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$base64Token\",$timestamp,\"$signature\"]"

            val payloadArray = JSONArray().apply {
                val item = JSONArray().apply {
                    put("Fbv4je")
                    put(innerReq)
                    put(null)
                    put("generic")
                }
                val subArray = JSONArray().apply {
                    put(item)
                }
                put(subArray)
            }

            val encodedReq = "f.req=" + URLEncoder.encode(payloadArray.toString(), "UTF-8")
            val mediaType = "application/x-www-form-urlencoded;charset=UTF-8".toMediaType()
            val body = encodedReq.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(rpcUrl)
                .post(body)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                .header("Referer", "https://news.google.com/articles/$base64Token")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val responseText = response.body?.string() ?: return null
            val chunks = responseText.split("\n\n")
            if (chunks.size < 2) return null

            val jsonArray = JSONArray(chunks[1])
            if (jsonArray.length() == 0) return null

            val firstElement = jsonArray.getJSONArray(0)
            val innerPayloadString = firstElement.getString(2)

            val innerArray = JSONArray(innerPayloadString)
            if (innerArray.length() >= 2) {
                return innerArray.getString(1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "RPC execution error: ${e.message}")
        }
        return null
    }

    private fun extractBase64Token(urlStr: String): String? {
        return try {
            val uri = Uri.parse(urlStr)
            val pathSegments = uri.pathSegments
            if (pathSegments.isNotEmpty()) {
                val last = pathSegments.last()
                if (last.length > 20) last else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private val thumbnailCache = ConcurrentHashMap<String, String>()

    private val OG_IMAGE_PATTERN = Pattern.compile("property=[\"']og:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
    private val OG_IMAGE_PATTERN_ALT = Pattern.compile("content=[\"']([^\"']+)[\"']\\s+property=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE)
    private val TWITTER_IMAGE_PATTERN = Pattern.compile("name=[\"']twitter:image[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE)

    /**
     * Streams the first 16 KB of the publisher article page to extract the original high-resolution Open Graph thumbnail.
     */
    fun extractPublisherOgImage(directUrl: String): String? {
        if (directUrl.isBlank() || directUrl.contains("news.google.com")) return null

        thumbnailCache[directUrl]?.let { return it }

        return try {
            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile; rv:154.0) Gecko/154.0 Firefox/154.0")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val byteStream = response.body?.byteStream() ?: return null
            val buffer = ByteArray(16384) // 16 KB head snippet
            val bytesRead = byteStream.read(buffer)
            byteStream.close()

            if (bytesRead <= 0) return null
            val headHtml = String(buffer, 0, bytesRead, Charsets.UTF_8)

            var imageUrl: String? = null
            val m1 = OG_IMAGE_PATTERN.matcher(headHtml)
            if (m1.find()) {
                imageUrl = m1.group(1)
            } else {
                val m2 = OG_IMAGE_PATTERN_ALT.matcher(headHtml)
                if (m2.find()) {
                    imageUrl = m2.group(1)
                } else {
                    val m3 = TWITTER_IMAGE_PATTERN.matcher(headHtml)
                    if (m3.find()) {
                        imageUrl = m3.group(1)
                    }
                }
            }

            if (!imageUrl.isNullOrBlank()) {
                var absUrl = imageUrl.trim()
                if (absUrl.startsWith("//")) {
                    absUrl = "https:$absUrl"
                } else if (absUrl.startsWith("/")) {
                    val uri = Uri.parse(directUrl)
                    val base = "${uri.scheme}://${uri.host}"
                    absUrl = "$base$absUrl"
                }
                val clean = NewsRssParser.sanitizeImageUrl(absUrl)
                if (clean != null) {
                    thumbnailCache[directUrl] = clean
                    return clean
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
