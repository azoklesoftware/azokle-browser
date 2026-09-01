package com.azokle.search.browser.history

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
            val now = System.currentTimeMillis()
            _history.update { currentList ->
                val mutable = currentList.toMutableList()
                val existingIndex = mutable.indexOfFirst { it.url.equals(url, ignoreCase = true) }

                if (existingIndex >= 0) {
                    val existing = mutable.removeAt(existingIndex)
                    val updated = existing.copy(
                        title = if (title.isNotBlank()) title else existing.title,
                        timestamp = now,
                        visitCount = existing.visitCount + 1
                    )
                    listOf(updated) + mutable
                } else {
                    val newItem = HistoryItem(
                        id = UUID.randomUUID().toString(),
                        url = url,
                        title = title,
                        timestamp = now,
                        visitCount = 1,
                        isSearch = false
                    )
                    (listOf(newItem) + mutable).take(MAX_HISTORY_ITEMS)
                }
            }
            saveHistory(context)
        }
    }

    /**
     * Updates page title after GeckoView completes parsing document title.
     */
    fun updateTitle(url: String, title: String) {
        if (url.isBlank() || title.isBlank() || isIgnoredScheme(url)) return
        val context = appContext ?: return

        scope.launch {
            _history.update { list ->
                list.map {
                    if (it.url.equals(url, ignoreCase = true) && it.title != title) {
                        it.copy(title = title)
                    } else it
                }
            }
            saveHistory(context)
        }
    }

    /**
     * Records a search query in history.
     */
    fun recordSearch(query: String, searchUrl: String, isPrivate: Boolean = false) {
        if (isPrivate || query.isBlank()) return
        val context = appContext ?: return

        scope.launch {
            val now = System.currentTimeMillis()
            _history.update { currentList ->
                val mutable = currentList.toMutableList()
                val existingIndex = mutable.indexOfFirst {
                    it.isSearch && it.searchQuery.equals(query.trim(), ignoreCase = true)
                }

                if (existingIndex >= 0) {
                    val existing = mutable.removeAt(existingIndex)
                    val updated = existing.copy(
                        url = searchUrl,
                        timestamp = now,
                        visitCount = existing.visitCount + 1
                    )
                    listOf(updated) + mutable
                } else {
                    val newItem = HistoryItem(
                        id = UUID.randomUUID().toString(),
                        url = searchUrl,
                        title = query.trim(),
                        timestamp = now,
                        visitCount = 1,
                        isSearch = true,
                        searchQuery = query.trim()
                    )
                    (listOf(newItem) + mutable).take(MAX_HISTORY_ITEMS)
                }
            }
            saveHistory(context)
        }
    }

    fun deleteEntry(id: String) {
        val context = appContext ?: return
        _history.update { list -> list.filterNot { it.id == id } }
        saveHistory(context)
    }

    fun deleteEntries(ids: List<String>) {
        val context = appContext ?: return
        val set = ids.toSet()
        _history.update { list -> list.filterNot { set.contains(it.id) } }
        saveHistory(context)
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
        _history.update { list -> list.filter { it.timestamp < startOfToday } }
        saveHistory(context)
    }

    private var saveJob: Job? = null

    fun clearAll() {
        val context = appContext ?: return
        _history.value = emptyList()
        saveHistory(context, debounceMs = 0L)
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

    private fun saveHistory(context: Context, debounceMs: Long = 2000L) {
        saveJob?.cancel()
        saveJob = scope.launch {
            if (debounceMs > 0) {
                kotlinx.coroutines.delay(debounceMs)
            }
            try {
                val array = JSONArray()
                _history.value.take(MAX_HISTORY_ITEMS).forEach { item ->
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("url", item.url)
                        put("title", item.title)
                        put("timestamp", item.timestamp)
                        put("visitCount", item.visitCount)
                        put("isSearch", item.isSearch)
                        put("searchQuery", item.searchQuery ?: "")
                    }
                    array.put(obj)
                }
                withContext(Dispatchers.IO) {
                    val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, HISTORY_FILE))
                    val stream = atomicFile.startWrite()
                    try {
                        stream.write(array.toString().toByteArray(Charsets.UTF_8))
                        atomicFile.finishWrite(stream)
                    } catch (e: Exception) {
                        atomicFile.failWrite(stream)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save history: ${e.message}")
            }
        }
    }

    private fun loadHistory(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val atomicFile = androidx.core.util.AtomicFile(File(context.filesDir, HISTORY_FILE))
                if (!atomicFile.baseFile.exists()) return@launch
                val bytes = atomicFile.readFully()
                if (bytes.isEmpty()) return@launch
                val jsonStr = String(bytes, Charsets.UTF_8)
                val array = JSONArray(jsonStr)
                val list = mutableListOf<HistoryItem>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HistoryItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            url = obj.getString("url"),
                            title = obj.optString("title", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            visitCount = obj.optInt("visitCount", 1),
                            isSearch = obj.optBoolean("isSearch", false),
                            searchQuery = obj.optString("searchQuery", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                _history.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load history: ${e.message}")
            }
        }
    }
}
