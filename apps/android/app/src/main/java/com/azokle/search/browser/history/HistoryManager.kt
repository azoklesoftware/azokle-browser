package com.azokle.search.browser.history

import android.content.Context
import android.util.Log
import com.azokle.search.core.AzokleCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

/**
 * High-Performance Sovereign Browsing History Manager for Azokle Search.
 * Backed by native Rust History Core (crates/history) for cross-platform consistency and zero data leakage.
 */
object HistoryManager {
    private const val TAG = "AzokleHistoryManager"
    private const val HISTORY_FILE = "azokle_browser_history.json"
    private const val MAX_HISTORY_ITEMS = 1000

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false
    private var appContext: Context? = null

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext
        loadHistory(context)
    }

    /**
     * Records a visited web page. Strictly excludes private/Tor browsing, about/blank schemes, and assets.
     */
    fun recordVisit(url: String, title: String = "", isPrivate: Boolean = false) {
        if (isPrivate || url.isBlank() || isIgnoredScheme(url)) return
        val context = appContext ?: return

        scope.launch {
            AzokleCore.recordHistoryVisit(title, url, isPrivate)
            refreshFromRust(context)
        }
    }

    /**
     * Updates page title after GeckoView completes parsing document title.
     */
    fun updateTitle(url: String, title: String) {
        if (url.isBlank() || title.isBlank() || isIgnoredScheme(url)) return
        val context = appContext ?: return

        scope.launch {
            AzokleCore.recordHistoryVisit(title, url, false)
            refreshFromRust(context)
        }
    }

    /**
     * Records a search query in history.
     */
    fun recordSearch(query: String, searchUrl: String, isPrivate: Boolean = false) {
        if (isPrivate || query.isBlank()) return
        val context = appContext ?: return

        scope.launch {
            AzokleCore.recordHistoryVisit(query.trim(), searchUrl, isPrivate)
            refreshFromRust(context)
        }
    }

    fun deleteEntry(id: String) {
        val context = appContext ?: return
        scope.launch {
            AzokleCore.deleteHistoryEntry(id)
            refreshFromRust(context)
        }
    }

    fun deleteEntries(ids: List<String>) {
        val context = appContext ?: return
        scope.launch {
            for (id in ids) {
                AzokleCore.deleteHistoryEntry(id)
            }
            refreshFromRust(context)
        }
    }

    fun clearToday() {
        val context = appContext ?: return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis
        val now = System.currentTimeMillis()
        scope.launch {
            AzokleCore.clearHistoryRange(startOfToday, now)
            refreshFromRust(context)
        }
    }

    fun clearAll() {
        val context = appContext ?: return
        scope.launch {
            AzokleCore.clearAllHistory()
            _history.value = emptyList()
            saveToDisk(context)
        }
    }

    private fun isIgnoredScheme(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("about:") ||
                lower.startsWith("chrome:") ||
                lower.startsWith("javascript:") ||
                lower.startsWith("data:") ||
                lower.startsWith("blob:") ||
                lower.startsWith("file:")
    }

    private fun refreshFromRust(context: Context) {
        try {
            val array = AzokleCore.getRecentHistory(MAX_HISTORY_ITEMS, 0)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val url = obj.optString("url", "")
                val isSearch = url.contains("search.azokle.com") || url.contains("duckduckgo.com")
                list.add(
                    HistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        url = url,
                        title = obj.optString("title", ""),
                        timestamp = obj.optLong("last_visit_time", obj.optLong("timestamp", System.currentTimeMillis())),
                        visitCount = obj.optInt("visit_count", obj.optInt("visitCount", 1)),
                        isSearch = isSearch,
                        searchQuery = if (isSearch) obj.optString("title", "") else null
                    )
                )
            }
            _history.value = list
            saveToDisk(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh history from Rust: ${e.message}")
        }
    }

    private fun saveToDisk(context: Context) {
        try {
            val jsonStr = AzokleCore.exportHistoryJson()
            val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, HISTORY_FILE))
            val stream = atomicFile.startWrite()
            try {
                stream.write(jsonStr.toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(stream)
            } catch (e: Exception) {
                atomicFile.failWrite(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history: ${e.message}")
        }
    }

    private fun loadHistory(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, HISTORY_FILE))
                if (atomicFile.baseFile.exists()) {
                    val bytes = atomicFile.readFully()
                    if (bytes.isNotEmpty()) {
                        val jsonStr = String(bytes, Charsets.UTF_8)
                        AzokleCore.importHistoryJson(jsonStr)
                    }
                }
                refreshFromRust(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history: ${e.message}")
            }
        }
    }
}
