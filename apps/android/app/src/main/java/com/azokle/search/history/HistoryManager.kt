package com.azokle.search.history

import android.content.Context
import android.util.Log
import com.azokle.search.core.AzokleCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sovereign Hybrid (In-Memory + SQLite) History Manager for Azokle Search.
 * - In-Memory (Rust): Sub-millisecond Frecency ranking, search filtering, and instant StateFlow UI emission
 * - SQLite (WAL + FTS): Durable ACID storage, Full-Text Search, and forensic secure deletion
 * - Strict Privacy Guard: Zero-leakage policy for Tor & Private browsing tabs
 */
object HistoryManager {
    private const val TAG = "AzokleHistoryManager"
    private const val HISTORY_FILE = "azokle_browser_history.json"
    private const val LEGACY_HISTORY_FILE = "history.json"
    private const val MAX_HISTORY_ITEMS = 1000

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val isIncinerating = AtomicBoolean(false)

    private var isInitialized = false
    private var appContext: Context? = null
    private var database: AzokleHistoryDatabase? = null

    fun setIncinerating(active: Boolean) {
        isIncinerating.set(active)
    }

    fun isIncinerating(): Boolean = isIncinerating.get()

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext
        database = AzokleHistoryDatabase.getInstance(context)
        loadHistory(context)
    }

    /**
     * Records a standard visited web page in both the In-Memory Rust Engine and SQLite Database.
     */
    fun recordVisit(url: String, title: String = "", isPrivate: Boolean = false) {
        if (isIncinerating.get() || isPrivate || url.isBlank() || isIgnoredScheme(url)) return
        val active = com.azokle.search.tabs.TabManager.activeTab
        if (active != null && (active.isPrivate || active.isTor)) return
        val context = appContext ?: return

        scope.launch {
            if (isIncinerating.get()) return@launch
            // 1. In-Memory Fast Path
            AzokleCore.recordHistoryVisit(title, url, isPrivate)
            refreshFromRust(context)

            // 2. SQLite Persistent Tier
            database?.recordPlaceVisit(
                url = url,
                title = title,
                isTyped = false
            )
        }
    }

    /**
     * Records an explicitly typed omnibox visit, granting higher Frecency priority in both memory and SQLite.
     */
    fun recordTypedVisit(url: String, title: String = "", isPrivate: Boolean = false) {
        if (isIncinerating.get() || isPrivate || url.isBlank() || isIgnoredScheme(url)) return
        val active = com.azokle.search.tabs.TabManager.activeTab
        if (active != null && (active.isPrivate || active.isTor)) return
        val context = appContext ?: return

        scope.launch {
            if (isIncinerating.get()) return@launch
            // 1. In-Memory Fast Path
            AzokleCore.recordHistoryTypedVisit(title, url, isPrivate)
            refreshFromRust(context)

            // 2. SQLite Persistent Tier
            database?.recordPlaceVisit(
                url = url,
                title = title,
                isTyped = true
            )
        }
    }

    /**
     * Updates page title after GeckoView completes parsing document title.
     */
    fun updateTitle(url: String, title: String, isPrivate: Boolean = false) {
        if (isIncinerating.get() || isPrivate || url.isBlank() || title.isBlank() || isIgnoredScheme(url)) return
        val active = com.azokle.search.tabs.TabManager.activeTab
        if (active != null && (active.isPrivate || active.isTor)) return
        val context = appContext ?: return

        scope.launch {
            if (isIncinerating.get()) return@launch
            AzokleCore.recordHistoryVisit(title, url, false)
            refreshFromRust(context)
            database?.updatePlaceTitle(url, title)
        }
    }

    /**
     * Records a search query in history.
     */
    fun recordSearch(query: String, searchUrl: String, isPrivate: Boolean = false) {
        if (isIncinerating.get() || isPrivate || query.isBlank()) return
        val context = appContext ?: return

        scope.launch {
            if (isIncinerating.get()) return@launch
            AzokleCore.recordHistoryVisit(query.trim(), searchUrl, isPrivate)
            refreshFromRust(context)
            database?.recordPlaceVisit(
                url = searchUrl,
                title = query.trim(),
                isTyped = false,
                isSearch = true,
                searchQuery = query.trim()
            )
        }
    }

    /**
     * Searches history and ranks results by Frecency score descending.
     */
    fun searchFrecency(query: String, limit: Int = 20): List<HistoryItem> {
        if (isIncinerating.get()) return emptyList()
        val array = AzokleCore.searchHistoryFrecency(query, limit)
        val list = mutableListOf<HistoryItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(parseHistoryItem(obj))
        }
        if (list.isEmpty()) {
            return database?.searchFts(query, limit) ?: emptyList()
        }
        return list
    }

    /**
     * Returns top historical destinations ranked by Frecency score.
     */
    fun getTopFrecency(limit: Int = 20): List<HistoryItem> {
        if (isIncinerating.get()) return emptyList()
        val array = AzokleCore.getTopFrecencyHistory(limit)
        val list = mutableListOf<HistoryItem>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(parseHistoryItem(obj))
        }
        return list
    }

    /**
     * Returns top visited domains for UI filtering chips.
     */
    fun getTopDomains(limit: Int = 10): List<Pair<String, Int>> {
        if (isIncinerating.get()) return emptyList()
        val array = AzokleCore.getTopHistoryDomains(limit)
        val list = mutableListOf<Pair<String, Int>>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val domain = obj.optString("domain", "")
            val count = obj.optInt("count", 0)
            if (domain.isNotBlank()) {
                list.add(domain to count)
            }
        }
        return list
    }

    fun deleteEntry(id: String) {
        val context = appContext ?: return
        scope.launch {
            AzokleCore.deleteHistoryEntry(id)
            database?.deletePlace(id)
            refreshFromRust(context)
        }
    }

    fun deleteEntries(ids: List<String>) {
        val context = appContext ?: return
        scope.launch {
            for (id in ids) {
                AzokleCore.deleteHistoryEntry(id)
                database?.deletePlace(id)
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
            database?.clearTimeRange(startOfToday, now)
            refreshFromRust(context)
        }
    }

    fun clearInMemoryHistory() {
        _history.value = emptyList()
    }

    fun clearAllSync(context: Context) {
        isIncinerating.set(true)
        try {
            supervisorJob.cancelChildren()
        } catch (_: Exception) {}

        _history.value = emptyList()
        AzokleCore.clearAllHistory()
        database?.clearAllSecurely()
        
        try {
            val file = File(context.filesDir, HISTORY_FILE)
            if (file.exists()) {
                file.delete()
            }
            val legacyFile = File(context.filesDir, LEGACY_HISTORY_FILE)
            if (legacyFile.exists()) {
                legacyFile.delete()
            }
        } catch (_: Exception) {}
    }

    fun clearAll() {
        val context = appContext ?: return
        clearInMemoryHistory()
        scope.launch {
            clearAllSync(context)
            isIncinerating.set(false)
        }
    }

    private fun isIgnoredScheme(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.startsWith("about:") ||
                lower.startsWith("chrome:") ||
                lower.startsWith("javascript:") ||
                lower.startsWith("data:") ||
                lower.startsWith("blob:") ||
                lower.startsWith("file:") ||
                lower.startsWith("content:") ||
                lower.startsWith("resource:") ||
                lower.startsWith("intent:")
    }

    private fun parseHistoryItem(obj: JSONObject): HistoryItem {
        val url = obj.optString("url", "")
        val isSearch = url.contains("search.azokle.com") || url.contains("duckduckgo.com")
        return HistoryItem(
            id = obj.optString("id", UUID.randomUUID().toString()),
            url = url,
            title = obj.optString("title", ""),
            timestamp = obj.optLong("visit_time", obj.optLong("last_visit_time", obj.optLong("timestamp", System.currentTimeMillis()))),
            visitCount = obj.optInt("visit_count", obj.optInt("visitCount", 1)),
            typedCount = obj.optInt("typed_count", 0),
            frecency = obj.optLong("frecency", 1000L),
            isSearch = isSearch,
            searchQuery = if (isSearch) obj.optString("title", "") else null
        )
    }

    private fun refreshFromRust(context: Context) {
        if (isIncinerating.get()) return
        try {
            val array = AzokleCore.getRecentHistory(MAX_HISTORY_ITEMS, 0)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(parseHistoryItem(obj))
            }
            if (list.isNotEmpty()) {
                _history.value = list
            } else {
                // Fallback to SQLite
                val sqlList = database?.getAllPlaces(MAX_HISTORY_ITEMS) ?: emptyList()
                _history.value = sqlList
            }
            if (!isIncinerating.get()) {
                saveToDisk(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh history: ${e.message}")
        }
    }

    private fun saveToDisk(context: Context) {
        if (isIncinerating.get()) return
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
        if (isIncinerating.get()) return
        scope.launch(Dispatchers.IO) {
            try {
                if (isIncinerating.get()) return@launch
                // 1. Check SQLite first
                val sqliteRecords = database?.getAllPlaces(MAX_HISTORY_ITEMS) ?: emptyList()
                if (sqliteRecords.isNotEmpty()) {
                    // Populate Rust in-memory engine from SQLite
                    for (item in sqliteRecords.reversed()) {
                        AzokleCore.recordHistoryVisit(item.title, item.url, false)
                    }
                } else {
                    // Migrate from JSON backup if present
                    val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, HISTORY_FILE))
                    if (atomicFile.baseFile.exists()) {
                        val bytes = atomicFile.readFully()
                        if (bytes.isNotEmpty()) {
                            val jsonStr = String(bytes, Charsets.UTF_8)
                            AzokleCore.importHistoryJson(jsonStr)
                            val array = JSONArray(jsonStr)
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                val item = parseHistoryItem(obj)
                                database?.recordPlaceVisit(
                                    url = item.url,
                                    title = item.title,
                                    isTyped = item.typedCount > 0,
                                    isSearch = item.isSearch,
                                    searchQuery = item.searchQuery,
                                    timestamp = item.timestamp
                                )
                            }
                        }
                    }
                }
                if (!isIncinerating.get()) {
                    refreshFromRust(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history: ${e.message}")
            }
        }
    }
}
