package com.azokle.search.history

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Public Contract Interface for History Management.
 * Follows DuckDuckGo's -api pattern to decouple consumers from concrete implementations.
 */
interface IHistoryManager {
    val history: StateFlow<List<HistoryItem>>

    fun init(context: Context)
    fun recordVisit(url: String, title: String = "", isPrivate: Boolean = false)
    fun updateTitle(url: String, title: String, isPrivate: Boolean = false)
    fun recordSearch(query: String, searchUrl: String, isPrivate: Boolean = false)
    fun deleteEntry(id: String)
    fun deleteEntries(ids: List<String>)
    fun clearAllHistory()
    fun getGroupedHistory(): Map<String, List<HistoryItem>>
}
