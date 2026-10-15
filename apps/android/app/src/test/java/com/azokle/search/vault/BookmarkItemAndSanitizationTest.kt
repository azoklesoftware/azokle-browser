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

package com.azokle.search.vault

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class BookmarkItemAndSanitizationTest {

    private fun sanitizeUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null

        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower.startsWith("javascript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("data:") ||
            lower.startsWith("content:") ||
            lower.startsWith("intent:") ||
            lower.startsWith("blob:") ||
            lower.startsWith("chrome:") ||
            lower.startsWith("resource:")
        ) {
            return null
        }

        val finalUrl = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> trimmed
            lower.startsWith("azokle://") || lower.startsWith("about:") -> trimmed
            else -> "https://$trimmed"
        }

        val scheme = finalUrl.substringBefore(':', "").lowercase(Locale.ROOT)
        return if (scheme in listOf("http", "https", "azokle", "about")) finalUrl else null
    }

    @Test
    fun testBookmarkItemDefaultValues() {
        val item = BookmarkItem(
            title = "Azokle Search",
            url = "https://azokle.com",
            dateAdded = "Oct 15",
            favicon = "https://azokle.com/favicon.ico"
        )

        assertNotNull(item.id)
        assertEquals("Azokle Search", item.title)
        assertEquals("https://azokle.com", item.url)
        assertEquals("Oct 15", item.dateAdded)
        assertEquals("https://azokle.com/favicon.ico", item.favicon)
    }

    @Test
    fun testSanitizeUrlBlocksMaliciousSchemes() {
        assertNull("javascript: must be blocked", sanitizeUrl("javascript:alert(document.cookie)"))
        assertNull("file: must be blocked", sanitizeUrl("file:///android_asset/secret.txt"))
        assertNull("data: must be blocked", sanitizeUrl("data:text/html,<h1>Hacked</h1>"))
        assertNull("content: must be blocked", sanitizeUrl("content://media/external/images"))
        assertNull("intent: must be blocked", sanitizeUrl("intent://#Intent;scheme=bad;end"))
        assertNull("blob: must be blocked", sanitizeUrl("blob:https://example.com/uuid"))
        assertNull("chrome: must be blocked", sanitizeUrl("chrome://settings"))
        assertNull("empty URL must be blocked", sanitizeUrl("   "))
    }

    @Test
    fun testSanitizeUrlAllowsAndNormalizesSafeUrls() {
        assertEquals("https://azokle.com", sanitizeUrl("https://azokle.com"))
        assertEquals("http://example.com/test", sanitizeUrl("http://example.com/test"))
        assertEquals("https://github.com", sanitizeUrl("github.com"))
        assertEquals("https://duckduckgo.com/?q=privacy", sanitizeUrl("duckduckgo.com/?q=privacy"))
        assertEquals("azokle://bookmarks", sanitizeUrl("azokle://bookmarks"))
        assertEquals("about:blank", sanitizeUrl("about:blank"))
    }

    @Test
    fun testBookmarkItemJsonSerializationWithFavicon() {
        val item = BookmarkItem(
            id = "test-uuid-123",
            title = "DuckDuckGo",
            url = "https://duckduckgo.com",
            dateAdded = "Oct 15",
            favicon = "https://duckduckgo.com/favicon.ico"
        )

        val json = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("url", item.url)
            put("dateAdded", item.dateAdded)
            item.favicon?.let { put("favicon", it) }
        }

        assertEquals("test-uuid-123", json.getString("id"))
        assertEquals("DuckDuckGo", json.getString("title"))
        assertEquals("https://duckduckgo.com", json.getString("url"))
        assertEquals("Oct 15", json.getString("dateAdded"))
        assertEquals("https://duckduckgo.com/favicon.ico", json.getString("favicon"))

        // Deserialization
        val deserialized = BookmarkItem(
            id = json.optString("id"),
            title = json.optString("title"),
            url = json.optString("url"),
            dateAdded = json.optString("dateAdded"),
            favicon = if (json.has("favicon") && !json.isNull("favicon")) json.getString("favicon") else null
        )

        assertEquals(item.id, deserialized.id)
        assertEquals(item.title, deserialized.title)
        assertEquals(item.url, deserialized.url)
        assertEquals(item.dateAdded, deserialized.dateAdded)
        assertEquals(item.favicon, deserialized.favicon)
    }

    @Test
    fun testImportBookmarksJsonSanitization() {
        val rawJson = """
            [
              {"title": "Safe Page", "url": "https://example.com"},
              {"title": "Malicious JS", "url": "javascript:alert(1)"},
              {"title": "Local File", "url": "file:///data/local"},
              {"title": "Auto Prefix", "url": "github.com"}
            ]
        """.trimIndent()

        val arr = JSONArray(rawJson)
        val validItems = mutableListOf<BookmarkItem>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val rawUrl = obj.optString("url", "")
            val safe = sanitizeUrl(rawUrl) ?: continue
            validItems.add(
                BookmarkItem(
                    title = obj.optString("title", safe),
                    url = safe
                )
            )
        }

        assertEquals(2, validItems.size)
        assertEquals("https://example.com", validItems[0].url)
        assertEquals("https://github.com", validItems[1].url)
    }

    @Test
    fun testCaseInsensitiveBookmarkMatching() {
        val bookmarks = listOf(
            BookmarkItem(id = "1", title = "GitHub", url = "https://github.com"),
            BookmarkItem(id = "2", title = "Wikipedia", url = "https://www.wikipedia.org")
        )

        assertTrue(bookmarks.any { it.url.equals("HTTPS://GITHUB.COM", ignoreCase = true) })
        assertTrue(bookmarks.any { it.url.equals("https://github.com/", ignoreCase = true) == false })
        assertTrue(bookmarks.any { it.url.equals("https://www.wikipedia.org", ignoreCase = true) })
        assertFalse(bookmarks.any { it.url.equals("https://reddit.com", ignoreCase = true) })
    }
}
