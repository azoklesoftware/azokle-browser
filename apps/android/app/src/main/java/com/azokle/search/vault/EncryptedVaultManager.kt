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

@file:Suppress("DEPRECATION")

package com.azokle.search.vault

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class QuickLinkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val icon: String? = null,
    val isPinned: Boolean = true
)

class EncryptedVaultManager(context: Context) {
    private val prefs: SharedPreferences = getOrCreateEncryptedPrefs(context)

    private val _bookmarksFlow = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarksFlow: StateFlow<List<BookmarkItem>> = _bookmarksFlow.asStateFlow()

    private val _quickLinksFlow = MutableStateFlow<List<QuickLinkItem>>(emptyList())
    val quickLinksFlow: StateFlow<List<QuickLinkItem>> = _quickLinksFlow.asStateFlow()

    companion object {
        private const val KEY_BOOKMARKS = "vault_bookmarks"
        private const val KEY_QUICK_LINKS = "vault_quick_links"
        private const val PREFS_FILE = "azokle_secure_vault_v2"

        @Volatile
        var isEncryptedStoreActive: Boolean = false
            private set

        @Volatile
        private var cachedPrefs: SharedPreferences? = null

        @Volatile
        private var instance: EncryptedVaultManager? = null

        /**
         * Retrieves the instance.
         */
        fun getInstance(context: Context): EncryptedVaultManager {
            return instance ?: synchronized(this) {
                instance ?: EncryptedVaultManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Retrieves the or create encrypted prefs.
         */
        private fun getOrCreateEncryptedPrefs(context: Context): SharedPreferences {
            cachedPrefs?.let { return it }
            return synchronized(this) {
                cachedPrefs ?: createEncryptedPrefs(context.applicationContext).also {
                    cachedPrefs = it
                }
            }
        }

        /**
         * Executes the create encrypted prefs operation.
         * Fails closed without deleting user data or falling back to unencrypted plaintext (H-7).
         */
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val p = EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    isEncryptedStoreActive = true
                    return p
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.w("EncryptedVaultManager", "Keystore init attempt $attempt/3 failed: ${e.message}")
                    try {
                        Thread.sleep(50L * attempt)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            // FAIL CLOSED (H-7): Do NOT delete the Keystore master key, do NOT delete PREFS_FILE,
            // and NEVER fall back to plaintext SharedPreferences.
            android.util.Log.e(
                "EncryptedVaultManager",
                "Keystore unavailable. Preserving encrypted vault without deletion or plaintext fallback: ${lastException?.message}"
            )
            isEncryptedStoreActive = false
            return context.getSharedPreferences("${PREFS_FILE}_locked_dummy", Context.MODE_PRIVATE)
        }
    }

    init {
        loadBookmarksFromPrefs()
        loadQuickLinksFromPrefs()
    }

    // --- Bookmarks Management ---

    /**
     * Sanitizes bookmark and quick link titles against control characters and size bloat.
     */
    fun sanitizeTitle(rawTitle: String, fallback: String): String {
        val clean = rawTitle
            .filter { !it.isISOControl() }
            .trim()
            .take(256)
        return clean.ifBlank { fallback.take(256) }
    }

    /**
     * Sanitizes and validates a URL for storage as a bookmark or shortcut.
     * Blocks dangerous schemes (javascript:, file:, data:, content:, intent:, blob:, etc.)
     * and non-standard/privileged about: URLs (allowing only about:blank and about:tor).
     * Normalizes missing schemes to https://.
     */
    fun sanitizeUrl(rawUrl: String): String? {
        val clean = rawUrl
            .filter { !it.isISOControl() && it != '\uFEFF' && it != '\u200B' && it != '\u00A0' }
            .trim()
        if (clean.isEmpty()) return null

        val lower = clean.lowercase(Locale.ROOT)
        // Block dangerous / script / local-file schemes
        if (lower.startsWith("javascript:") ||
            lower.startsWith("file:") ||
            lower.startsWith("data:") ||
            lower.startsWith("content:") ||
            lower.startsWith("intent:") ||
            lower.startsWith("blob:") ||
            lower.startsWith("chrome:") ||
            lower.startsWith("resource:") ||
            lower.startsWith("view-source:") ||
            lower.startsWith("jar:")
        ) {
            return null
        }

        // AZK-VULN-26: Block internal privileged about: URLs (e.g. about:config, about:debugging)
        if (lower.startsWith("about:") && lower != "about:blank" && lower != "about:tor") {
            return null
        }

        val finalUrl = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> clean
            lower.startsWith("azokle://") || lower == "about:blank" || lower == "about:tor" -> clean
            else -> "https://$clean"
        }

        return try {
            val uri = android.net.Uri.parse(finalUrl)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            val isAllowedScheme = scheme in listOf("http", "https", "azokle") || finalUrl == "about:blank" || finalUrl == "about:tor"
            if (isAllowedScheme) {
                finalUrl
            } else {
                null
            }
        } catch (_: Throwable) {
            val scheme = finalUrl.substringBefore(':', "").lowercase(Locale.ROOT)
            val isAllowedScheme = scheme in listOf("http", "https", "azokle") || finalUrl == "about:blank" || finalUrl == "about:tor"
            if (isAllowedScheme) {
                finalUrl
            } else {
                null
            }
        }
    }

    private fun loadBookmarksFromPrefs() {
        try {
            val json = prefs.getString(KEY_BOOKMARKS, null) ?: return
            val arr = JSONArray(json)
            val list = ArrayList<BookmarkItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawUrl = obj.optString("url", "")
                val safeUrl = sanitizeUrl(rawUrl) ?: continue
                list.add(
                    BookmarkItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", safeUrl),
                        url = safeUrl,
                        dateAdded = obj.optString("dateAdded", ""),
                        favicon = if (obj.has("favicon") && !obj.isNull("favicon")) obj.getString("favicon") else null
                    )
                )
            }
            _bookmarksFlow.value = list
        } catch (e: Exception) {
            _bookmarksFlow.value = emptyList()
        }
    }

    /**
     * Persists bookmarks to prefs to storage.
     */
    private fun saveBookmarksToPrefs(bookmarks: List<BookmarkItem>) {
        if (!isEncryptedStoreActive) {
            android.util.Log.e("EncryptedVaultManager", "Refusing to persist bookmarks: hardware encryption inactive")
            return
        }
        try {
            val arr = JSONArray()
            for (b in bookmarks) {
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("url", b.url)
                    put("dateAdded", b.dateAdded)
                    b.favicon?.let { put("favicon", it) }
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Checks whether bookmarked.
     */
    fun isBookmarked(url: String): Boolean {
        val safeUrl = sanitizeUrl(url) ?: url.trim()
        return _bookmarksFlow.value.any { it.url.equals(safeUrl, ignoreCase = true) }
    }

    /**
     * Executes the add bookmark operation.
     * Preserves original ID and dateAdded when re-bookmarking an existing URL.
     */
    fun addBookmark(title: String, url: String, favicon: String? = null): BookmarkItem? {
        val safeUrl = sanitizeUrl(url) ?: return null
        val current = _bookmarksFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(safeUrl, ignoreCase = true) }
        val df = SimpleDateFormat("MMM dd", Locale.getDefault())
        val existingItem = if (existingIndex >= 0) current[existingIndex] else null

        val safeTitle = sanitizeTitle(title, safeUrl)
        val item = BookmarkItem(
            id = existingItem?.id ?: UUID.randomUUID().toString(),
            title = safeTitle,
            url = safeUrl,
            dateAdded = existingItem?.dateAdded?.takeIf { it.isNotBlank() } ?: df.format(Date()),
            favicon = favicon ?: existingItem?.favicon
        )
        if (existingIndex >= 0) {
            current[existingIndex] = item
        } else {
            current.add(0, item)
        }
        _bookmarksFlow.value = current
        saveBookmarksToPrefs(current)
        return item
    }

    /**
     * Toggles the state of bookmark.
     */
    fun toggleBookmark(title: String, url: String, favicon: String? = null): Boolean {
        val safeUrl = sanitizeUrl(url) ?: return false
        val current = _bookmarksFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(safeUrl, ignoreCase = true) }
        val added: Boolean

        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            added = false
        } else {
            val df = SimpleDateFormat("MMM dd", Locale.getDefault())
            val safeTitle = sanitizeTitle(title, safeUrl)
            val item = BookmarkItem(
                id = UUID.randomUUID().toString(),
                title = safeTitle,
                url = safeUrl,
                dateAdded = df.format(Date()),
                favicon = favicon
            )
            current.add(0, item)
            added = true
        }

        _bookmarksFlow.value = current
        saveBookmarksToPrefs(current)
        return added
    }

    /**
     * Updates bookmark title, URL, and optional favicon.
     */
    fun updateBookmark(id: String, newTitle: String, newUrl: String, newFavicon: String? = null): Boolean {
        val safeUrl = sanitizeUrl(newUrl) ?: return false
        val current = _bookmarksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            val safeTitle = sanitizeTitle(newTitle, safeUrl)
            current[index] = old.copy(
                title = safeTitle,
                url = safeUrl,
                favicon = newFavicon ?: old.favicon
            )
            _bookmarksFlow.value = current
            saveBookmarksToPrefs(current)
            return true
        }
        return false
    }

    /**
     * Removes the specified bookmark.
     */
    fun removeBookmark(id: String) {
        val updated = _bookmarksFlow.value.filter { it.id != id }
        _bookmarksFlow.value = updated
        saveBookmarksToPrefs(updated)
    }

    /**
     * Removes the specified bookmark.
     */
    fun deleteBookmark(id: String) = removeBookmark(id)

    /**
     * Batch removes multiple bookmarks by their IDs.
     */
    fun removeBookmarks(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val updated = _bookmarksFlow.value.filter { it.id !in idSet }
        _bookmarksFlow.value = updated
        saveBookmarksToPrefs(updated)
    }

    /**
     * Clears all saved bookmarks.
     */
    fun clearAllBookmarks() {
        _bookmarksFlow.value = emptyList()
        saveBookmarksToPrefs(emptyList())
    }

    // --- Quick Links Management ---

    private fun loadQuickLinksFromPrefs() {
        try {
            val json = prefs.getString(KEY_QUICK_LINKS, null) ?: return
            val arr = JSONArray(json)
            val list = ArrayList<QuickLinkItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawUrl = obj.optString("url", "")
                val safeUrl = sanitizeUrl(rawUrl) ?: continue
                list.add(
                    QuickLinkItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", safeUrl),
                        url = safeUrl,
                        icon = if (obj.has("icon") && !obj.isNull("icon")) obj.getString("icon") else null,
                        isPinned = obj.optBoolean("isPinned", true)
                    )
                )
            }
            _quickLinksFlow.value = list
        } catch (e: Exception) {
            _quickLinksFlow.value = emptyList()
        }
    }

    /**
     * Persists quick links to prefs to storage.
     */
    private fun saveQuickLinksToPrefs(links: List<QuickLinkItem>) {
        if (!isEncryptedStoreActive) {
            android.util.Log.e("EncryptedVaultManager", "Refusing to persist quick links: hardware encryption inactive")
            return
        }
        try {
            val arr = JSONArray()
            for (l in links) {
                val obj = JSONObject().apply {
                    put("id", l.id)
                    put("title", l.title)
                    put("url", l.url)
                    put("icon", l.icon)
                    put("isPinned", l.isPinned)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_QUICK_LINKS, arr.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Executes the add quick link operation.
     */
    fun addQuickLink(title: String, url: String, icon: String? = null): Boolean {
        val safeUrl = sanitizeUrl(url) ?: return false
        val safeTitle = sanitizeTitle(title, safeUrl)
        val current = _quickLinksFlow.value.toMutableList()
        if (current.none { it.url.equals(safeUrl, ignoreCase = true) }) {
            current.add(QuickLinkItem(title = safeTitle, url = safeUrl, icon = icon))
            _quickLinksFlow.value = current
            saveQuickLinksToPrefs(current)
            return true
        }
        return false
    }

    /**
     * Removes the specified quick link.
     */
    fun removeQuickLink(id: String) {
        val updated = _quickLinksFlow.value.filter { it.id != id }
        _quickLinksFlow.value = updated
        saveQuickLinksToPrefs(updated)
    }

    /**
     * Removes a quick link matching the given URL (case-insensitive).
     */
    fun removeQuickLinkByUrl(url: String) {
        val safeUrl = sanitizeUrl(url) ?: url.trim()
        val updated = _quickLinksFlow.value.filter { !it.url.equals(safeUrl, ignoreCase = true) }
        _quickLinksFlow.value = updated
        saveQuickLinksToPrefs(updated)
    }

    /**
     * Toggles a quick link (pins if not present, unpins if present).
     */
    fun toggleQuickLink(title: String, url: String, icon: String? = null): Boolean {
        val safeUrl = sanitizeUrl(url) ?: return false
        val current = _quickLinksFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(safeUrl, ignoreCase = true) }
        return if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            _quickLinksFlow.value = current
            saveQuickLinksToPrefs(current)
            false
        } else {
            current.add(QuickLinkItem(title = title.ifBlank { safeUrl }, url = safeUrl, icon = icon))
            _quickLinksFlow.value = current
            saveQuickLinksToPrefs(current)
            true
        }
    }

    /**
     * Checks whether quick link pinned.
     */
    fun isQuickLinkPinned(url: String): Boolean {
        val safeUrl = sanitizeUrl(url) ?: url.trim()
        return _quickLinksFlow.value.any { it.url.equals(safeUrl, ignoreCase = true) }
    }

    /**
     * Executes the export bookmarks json operation.
     */
    fun exportBookmarksJson(): String {
        val arr = JSONArray()
        for (b in _bookmarksFlow.value) {
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("url", b.url)
                put("dateAdded", b.dateAdded)
                b.favicon?.let { put("favicon", it) }
            })
        }
        return arr.toString(2)
    }

    /**
     * Imports bookmarks from a JSON array string or JSON backup object string.
     * Sanitizes all URLs and prevents duplicates.
     */
    fun importBookmarksJson(jsonString: String): Result<Int> {
        return try {
            val trimmed = jsonString.trim()
            val list = mutableListOf<JSONObject>()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    list.add(arr.getJSONObject(i))
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                if (root.has("bookmarks")) {
                    val arr = root.getJSONArray("bookmarks")
                    for (i in 0 until arr.length()) {
                        list.add(arr.getJSONObject(i))
                    }
                } else {
                    return Result.failure(IllegalArgumentException("No 'bookmarks' list found in JSON"))
                }
            } else {
                return Result.failure(IllegalArgumentException("Invalid JSON format"))
            }

            val current = _bookmarksFlow.value.toMutableList()
            val df = SimpleDateFormat("MMM dd", Locale.getDefault())
            var count = 0

            for (obj in list) {
                val rawUrl = obj.optString("url", "").trim()
                if (rawUrl.isEmpty()) continue
                val safeUrl = sanitizeUrl(rawUrl) ?: continue
                val rawTitle = obj.optString("title", safeUrl)
                val safeTitle = sanitizeTitle(rawTitle, safeUrl)
                val dateAdded = obj.optString("dateAdded", df.format(Date()))
                val favicon = if (obj.has("favicon") && !obj.isNull("favicon")) obj.getString("favicon") else null

                if (current.none { it.url.equals(safeUrl, ignoreCase = true) }) {
                    current.add(
                        BookmarkItem(
                            id = UUID.randomUUID().toString(),
                            title = safeTitle,
                            url = safeUrl,
                            dateAdded = dateAdded,
                            favicon = favicon
                        )
                    )
                    count++
                }
            }

            if (count > 0) {
                _bookmarksFlow.value = current
                saveBookmarksToPrefs(current)
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes the export full backup json operation.
     */
    fun exportFullBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val bookmarksArr = JSONArray()
        for (b in _bookmarksFlow.value) {
            bookmarksArr.put(JSONObject().apply {
                put("id", b.id)
                put("title", b.title)
                put("url", b.url)
                put("dateAdded", b.dateAdded)
                b.favicon?.let { put("favicon", it) }
            })
        }
        root.put("bookmarks", bookmarksArr)

        val quickLinksArr = JSONArray()
        for (q in _quickLinksFlow.value) {
            quickLinksArr.put(JSONObject().apply {
                put("id", q.id)
                put("title", q.title)
                put("url", q.url)
                put("icon", q.icon)
            })
        }
        root.put("quickLinks", quickLinksArr)
        return root.toString(2)
    }

    /**
     * Executes the import backup json operation.
     */
    fun importBackupJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            var count = 0
            if (root.has("bookmarks")) {
                val arr = root.getJSONArray("bookmarks")
                val existing = _bookmarksFlow.value.toMutableList()
                val df = SimpleDateFormat("MMM dd", Locale.getDefault())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val rawUrl = obj.getString("url")
                    val safeUrl = sanitizeUrl(rawUrl) ?: continue
                    val rawTitle = obj.optString("title", safeUrl)
                    val safeTitle = sanitizeTitle(rawTitle, safeUrl)
                    val dateAdded = obj.optString("dateAdded", df.format(Date()))
                    val favicon = if (obj.has("favicon") && !obj.isNull("favicon")) obj.getString("favicon") else null
                    if (existing.none { it.url.equals(safeUrl, ignoreCase = true) }) {
                        existing.add(BookmarkItem(url = safeUrl, title = safeTitle, dateAdded = dateAdded, favicon = favicon))
                        count++
                    }
                }
                _bookmarksFlow.value = existing
                saveBookmarksToPrefs(existing)
            }
            if (root.has("quickLinks")) {
                val arr = root.getJSONArray("quickLinks")
                val existingQ = _quickLinksFlow.value.toMutableList()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val rawUrl = obj.getString("url")
                    val safeUrl = sanitizeUrl(rawUrl) ?: continue
                    val rawTitle = obj.optString("title", safeUrl)
                    val safeTitle = sanitizeTitle(rawTitle, safeUrl)
                    val icon = if (obj.has("icon") && !obj.isNull("icon")) obj.getString("icon") else null
                    if (existingQ.none { it.url.equals(safeUrl, ignoreCase = true) }) {
                        existingQ.add(QuickLinkItem(url = safeUrl, title = safeTitle, icon = icon))
                    }
                }
                _quickLinksFlow.value = existingQ
                saveQuickLinksToPrefs(existingQ)
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears all vault.
     */
    fun clearVault() {
        prefs.edit().clear().apply()
        _bookmarksFlow.value = emptyList()
        _quickLinksFlow.value = emptyList()
    }

    /**
     * Executes the purge vault operation.
     */
    fun purgeVault() = clearVault()
}
