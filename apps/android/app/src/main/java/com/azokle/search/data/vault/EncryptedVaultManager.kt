package com.azokle.search.data.vault

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

class EncryptedVaultManager(context: Context) {
    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    private val _bookmarksFlow = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarksFlow: StateFlow<List<BookmarkItem>> = _bookmarksFlow.asStateFlow()

    companion object {
        private const val KEY_BOOKMARKS = "vault_bookmarks"
        private const val PREFS_FILE = "azokle_secure_vault_v2"
        private var instance: EncryptedVaultManager? = null

        fun getInstance(context: Context): EncryptedVaultManager {
            return instance ?: synchronized(this) {
                instance ?: EncryptedVaultManager(context.applicationContext).also { instance = it }
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
                    // Reset corrupted key file to allow creating new secure key
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
                } catch (e2: Exception) {
                    android.util.Log.e("EncryptedVaultManager", "Hardware Keystore recovery failed: ${e2.message}")
                    // Fail closed: return a temporary in-memory instance or rethrow, never persist unencrypted
                    throw IllegalStateException("Secure Keystore unavailable. Plaintext fallback denied for sovereign security.", e2)
                }
            }
        }
    }

    init {
        loadBookmarks()
    }

    private fun loadBookmarks() {
        val rawStr = prefs.getString(KEY_BOOKMARKS, null)
        if (rawStr.isNullOrBlank()) {
            val defaults = listOf(
                BookmarkItem(
                    id = "1",
                    title = "Azokle Official — Sovereign Private Search",
                    url = "https://azokle.com",
                    dateAdded = "Aug 26"
                ),
                BookmarkItem(
                    id = "2",
                    title = "Electronic Frontier Foundation (EFF)",
                    url = "https://eff.org",
                    dateAdded = "Aug 25"
                ),
                BookmarkItem(
                    id = "3",
                    title = "Rust Programming Language — Memory Safety",
                    url = "https://rust-lang.org",
                    dateAdded = "Aug 24"
                )
            )
            _bookmarksFlow.value = defaults
            saveBookmarksToPrefs(defaults)
            return
        }

        try {
            val jsonStr = rawStr.trim()
            if (!jsonStr.startsWith("[") || !jsonStr.endsWith("]")) {
                _bookmarksFlow.value = emptyList()
                return
            }

            val arr = JSONArray(jsonStr)
            val list = mutableListOf<BookmarkItem>()
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
            val jsonPayload = arr.toString()
            prefs.edit().putString(KEY_BOOKMARKS, jsonPayload).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun isBookmarked(url: String): Boolean {
        return _bookmarksFlow.value.any { it.url.equals(url, ignoreCase = true) }
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

    fun removeBookmark(id: String) {
        val updated = _bookmarksFlow.value.filter { it.id != id }
        _bookmarksFlow.value = updated
        saveBookmarksToPrefs(updated)
    }

    fun deleteBookmark(id: String) = removeBookmark(id)

    fun clearVault() {
        prefs.edit().clear().apply()
        _bookmarksFlow.value = emptyList()
    }

    fun purgeVault() = clearVault()
}
