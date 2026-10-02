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
        private var cachedPrefs: SharedPreferences? = null

        @Volatile
        private var instance: EncryptedVaultManager? = null

        fun getInstance(context: Context): EncryptedVaultManager {
            return instance ?: synchronized(this) {
                instance ?: EncryptedVaultManager(context.applicationContext).also { instance = it }
            }
        }

        private fun getOrCreateEncryptedPrefs(context: Context): SharedPreferences {
            cachedPrefs?.let { return it }
            return synchronized(this) {
                cachedPrefs ?: createEncryptedPrefs(context.applicationContext).also {
                    cachedPrefs = it
                }
            }
        }

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                android.util.Log.e("EncryptedVaultManager", "Hardware Keystore init error: ${e.message}, attempting secure reset")
                try {
                    context.deleteSharedPreferences(PREFS_FILE)
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (ex: Exception) {
                    context.getSharedPreferences("${PREFS_FILE}_fallback", Context.MODE_PRIVATE)
                }
            }
        }
    }

    init {
        loadBookmarksFromPrefs()
        loadQuickLinksFromPrefs()
    }

    // --- Bookmarks Management ---

    private fun loadBookmarksFromPrefs() {
        try {
            val json = prefs.getString(KEY_BOOKMARKS, null) ?: return
            val arr = JSONArray(json)
            val list = ArrayList<BookmarkItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BookmarkItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        dateAdded = obj.optString("dateAdded", "")
                    )
                )
            }
            _bookmarksFlow.value = list
        } catch (e: Exception) {
            _bookmarksFlow.value = emptyList()
        }
    }

    private fun saveBookmarksToPrefs(bookmarks: List<BookmarkItem>) {
        try {
            val arr = JSONArray()
            for (b in bookmarks) {
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("url", b.url)
                    put("dateAdded", b.dateAdded)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun isBookmarked(url: String): Boolean {
        return _bookmarksFlow.value.any { it.url.equals(url, ignoreCase = true) }
    }

    fun addBookmark(title: String, url: String): BookmarkItem {
        val current = _bookmarksFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(url, ignoreCase = true) }
        val df = SimpleDateFormat("MMM dd", Locale.getDefault())
        val item = BookmarkItem(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { url },
            url = url,
            dateAdded = df.format(Date())
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

    fun toggleBookmark(title: String, url: String): Boolean {
        val current = _bookmarksFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.url.equals(url, ignoreCase = true) }
        val added: Boolean

        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            added = false
        } else {
            val df = SimpleDateFormat("MMM dd", Locale.getDefault())
            val item = BookmarkItem(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { url },
                url = url,
                dateAdded = df.format(Date())
            )
            current.add(0, item)
            added = true
        }

        _bookmarksFlow.value = current
        saveBookmarksToPrefs(current)
        return added
    }

    fun updateBookmark(id: String, newTitle: String, newUrl: String) {
        val current = _bookmarksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(title = newTitle, url = newUrl)
            _bookmarksFlow.value = current
            saveBookmarksToPrefs(current)
        }
    }

    fun removeBookmark(id: String) {
        val updated = _bookmarksFlow.value.filter { it.id != id }
        _bookmarksFlow.value = updated
        saveBookmarksToPrefs(updated)
    }

    fun deleteBookmark(id: String) = removeBookmark(id)

    // --- Quick Links Management ---

    private fun loadQuickLinksFromPrefs() {
        try {
            val json = prefs.getString(KEY_QUICK_LINKS, null) ?: return
            val arr = JSONArray(json)
            val list = ArrayList<QuickLinkItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    QuickLinkItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
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

    private fun saveQuickLinksToPrefs(links: List<QuickLinkItem>) {
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

    fun addQuickLink(title: String, url: String, icon: String? = null) {
        val current = _quickLinksFlow.value.toMutableList()
        if (current.none { it.url.equals(url, ignoreCase = true) }) {
            current.add(QuickLinkItem(title = title.ifBlank { url }, url = url, icon = icon))
            _quickLinksFlow.value = current
            saveQuickLinksToPrefs(current)
        }
    }

    fun removeQuickLink(id: String) {
        val updated = _quickLinksFlow.value.filter { it.id != id }
        _quickLinksFlow.value = updated
        saveQuickLinksToPrefs(updated)
    }

    fun isQuickLinkPinned(url: String): Boolean {
        return _quickLinksFlow.value.any { it.url.equals(url, ignoreCase = true) }
    }

    fun exportBookmarksJson(): String {
        val arr = JSONArray()
        for (b in _bookmarksFlow.value) {
            arr.put(JSONObject().apply {
                put("title", b.title)
                put("url", b.url)
                put("dateAdded", b.dateAdded)
            })
        }
        return arr.toString(2)
    }

    fun clearVault() {
        prefs.edit().clear().apply()
        _bookmarksFlow.value = emptyList()
        _quickLinksFlow.value = emptyList()
    }

    fun purgeVault() = clearVault()
}
