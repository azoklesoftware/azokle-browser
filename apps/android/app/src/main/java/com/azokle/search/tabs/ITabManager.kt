package com.azokle.search.tabs

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.GeckoSession

/**
 * Public Contract Interface for Tab & Session Management.
 * Follows DuckDuckGo's -api pattern to decouple consumers from concrete implementations.
 */
interface ITabManager {
    val tabs: StateFlow<List<TabSession>>
    val groups: StateFlow<List<TabGroup>>
    val activeTabId: StateFlow<String?>
    val activeTab: TabSession?
    val recentlyClosed: List<TabSession>

    fun init(context: Context)
    fun isTabLoaded(tabId: String, url: String): Boolean
    fun markTabLoaded(tabId: String, url: String)
    fun clearTabLoaded(tabId: String)
    fun updateTabSessionState(id: String, state: GeckoSession.SessionState)
    fun saveTabSessionState(id: String)
    fun restoreTabSessionState(id: String, session: GeckoSession): Boolean
    fun registerSession(id: String, session: GeckoSession)
    fun getSession(id: String): GeckoSession?
    fun getActiveSession(): GeckoSession?
    fun getOrCreateSession(
        id: String,
        context: Context,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): GeckoSession
    fun openTab(
        url: String,
        title: String = "New Tab",
        isPrivate: Boolean = false,
        isTor: Boolean = false,
        groupId: String? = null,
        groupTitle: String? = null,
        groupColor: Long? = null,
        parentTabId: String? = null,
        wallpaperResId: Int? = null
    ): String
    fun updateTabWallpaper(tabId: String, wallpaperResId: Int)
    fun openPrivateTab(url: String = "", title: String = "Private Tab"): String
    fun openTorTab(context: Context? = null, url: String = "", title: String = "Tor Window"): String
    fun openTabFromParent(
        url: String,
        title: String = "New Tab",
        parentTabId: String,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): String
    fun selectTab(id: String)
    fun closeTab(id: String)
    fun undoCloseTab(): TabSession?
    fun closeAllTabs()
    fun updateActiveTab(url: String, title: String)
    fun updateActiveTabThumbnail(bitmap: Bitmap)
    fun updateTabThumbnail(id: String, bitmap: Bitmap)
}
