package com.azokle.search.tabs

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.util.AtomicFile
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
import org.mozilla.geckoview.GeckoSession
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Sovereign High-Performance Tab & Session Orchestrator for Azokle Search.
 * Features:
 *  - Full Hierarchical Tab Grouping (Brave/Chrome style Group Tabs & Bottom Strip Switcher)
 *  - Atomic JSON disk persistence for non-private tabs & groups (survives Android LMK / process death)
 *  - Strict ephemeral in-memory isolation for Private and Tor tabs (Zero disk footprint)
 *  - Thread-Safe Recently closed tab undo stack (max 10 items)
 *  - Real-time tab grouping (manual and auto-parent child collections)
 *  - Foreground/background session throttling
 */
object TabManager {
    private const val TAG = "AzokleTabManager"
    private const val TABS_STATE_FILE = "azokle_tabs_state_v1.json"
    const val MAX_WARM_SESSIONS = 3

    private val _tabs = MutableStateFlow<List<TabSession>>(listOf(TabSession()))
    val tabs: StateFlow<List<TabSession>> = _tabs.asStateFlow()

    private val _groups = MutableStateFlow<List<TabGroup>>(emptyList())
    val groups: StateFlow<List<TabGroup>> = _groups.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(_tabs.value.firstOrNull()?.id)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val sessionMap = ConcurrentHashMap<String, GeckoSession>()
    private val loadedUrls = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isTabLoaded(tabId: String, url: String): Boolean {
        val loaded = loadedUrls[tabId]
        return loaded != null && loaded.equals(url, ignoreCase = true)
    }

    fun markTabLoaded(tabId: String, url: String) {
        loadedUrls[tabId] = url
    }

    fun clearTabLoaded(tabId: String) {
        loadedUrls.remove(tabId)
    }

    // Thread-Safe Recently Closed Tabs Deque for Instant Undo
    private val _recentlyClosed = ConcurrentLinkedDeque<TabSession>()
    val recentlyClosed: List<TabSession> get() = _recentlyClosed.toList()

    private var appContext: Context? = null
    private var isInitialized = false

    val activeTab: TabSession?
        get() = _tabs.value.find { it.id == _activeTabId.value } ?: _tabs.value.firstOrNull()

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext
        loadPersistedTabs(context)
    }

    private val sessionStateMap = ConcurrentHashMap<String, GeckoSession.SessionState>()

    fun updateTabSessionState(id: String, state: GeckoSession.SessionState) {
        sessionStateMap[id] = state
    }

    fun saveTabSessionState(id: String) {
        val session = sessionMap[id] ?: return
        try {
            session.flushSessionState()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun restoreTabSessionState(id: String, session: GeckoSession): Boolean {
        val state = sessionStateMap[id] ?: return false
        return try {
            session.restoreState(state)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun registerSession(id: String, session: GeckoSession) {
        sessionMap[id] = session
        val isActive = id == _activeTabId.value
        try {
            session.setActive(isActive)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun getSession(id: String): GeckoSession? = sessionMap[id]

    fun getActiveSession(): GeckoSession? {
        val currentId = _activeTabId.value ?: return null
        return sessionMap[currentId]
    }

    fun getOrCreateSession(
        id: String,
        context: Context,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): GeckoSession {
        // Enforce warm session budget before instantiating a new heavy GeckoSession
        if (!sessionMap.containsKey(id) && sessionMap.size >= MAX_WARM_SESSIONS) {
            reclaimInactiveTabMemory(MAX_WARM_SESSIONS - 1)
        }
        return sessionMap.getOrPut(id) {
            val s = com.azokle.search.engine.GeckoEngineManager.createSession(context, isPrivate, isTor)
            val isActive = id == _activeTabId.value
            try {
                s.setActive(isActive)
            } catch (e: Exception) {
                // Ignore
            }
            restoreTabSessionState(id, s)
            s
        }
    }

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
    ): String {
        val assignedWallpaper = wallpaperResId ?: com.azokle.search.settings.WallpaperManager.rotateWallpaper()
        val newTab = TabSession(
            url = url,
            title = title,
            isPrivate = isPrivate,
            isTor = isTor,
            groupId = groupId,
            groupTitle = groupTitle,
            groupColor = groupColor,
            parentTabId = parentTabId,
            wallpaperResId = assignedWallpaper
        )
        _tabs.update { it + newTab }

        // If assigned to a group, update group tabIds
        if (groupId != null) {
            _groups.update { list ->
                list.map { g ->
                    if (g.id == groupId && !g.tabIds.contains(newTab.id)) {
                        g.copy(tabIds = g.tabIds + newTab.id)
                    } else g
                }
            }
        }

        selectTab(newTab.id)
        if (!isPrivate && !isTor) {
            persistTabs()
        }
        return newTab.id
    }

    fun updateTabWallpaper(tabId: String, wallpaperResId: Int) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == tabId) {
                    tab.copy(wallpaperResId = wallpaperResId)
                } else tab
            }
        }
        persistTabs()
    }

    fun openPrivateTab(url: String = "", title: String = "Private Tab"): String {
        return openTab(url = url, title = title, isPrivate = true, isTor = false)
    }

    fun openTorTab(context: Context? = null, url: String = "", title: String = "Tor Window"): String {
        if (context != null) {
            com.azokle.search.tor.TorCircuitManager.startTorService(context)
        } else {
            com.azokle.search.tor.TorCircuitManager.startTorConnection()
        }
        return openTab(url = url, title = title, isPrivate = true, isTor = true)
    }

    /**
     * Automatically groups child tabs opened via target="_blank" or long-press with their parent tab.
     */
    fun openTabFromParent(
        url: String,
        title: String = "New Tab",
        parentTabId: String,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): String {
        val parent = _tabs.value.find { it.id == parentTabId }
        val targetGroupId: String
        val targetGroupTitle: String
        val targetGroupColor: Long

        val effectivePrivate = parent?.isPrivate ?: isPrivate
        val effectiveTor = parent?.isTor ?: isTor

        if (parent != null && parent.groupId != null) {
            targetGroupId = parent.groupId
            val group = _groups.value.find { it.id == targetGroupId }
            targetGroupTitle = group?.title ?: parent.groupTitle ?: "Group"
            targetGroupColor = group?.color ?: parent.groupColor ?: 0xFF38BDF8
        } else if (parent != null) {
            // Create a brand new group linking Parent + Child with enforced isolation
            val cleanTitle = parent.title.ifBlank {
                try { Uri.parse(parent.url).host?.removePrefix("www.") ?: "Group" } catch (e: Exception) { "Group" }
            }
            val colorIndex = (_groups.value.size) % TabGroup.GROUP_PALETTE.size
            targetGroupColor = TabGroup.GROUP_PALETTE[colorIndex]
            targetGroupTitle = cleanTitle
            targetGroupId = UUID.randomUUID().toString()

            val newGroup = TabGroup(
                id = targetGroupId,
                title = targetGroupTitle,
                color = targetGroupColor,
                tabIds = listOf(parent.id),
                parentTabId = parent.id,
                isPrivate = effectivePrivate,
                isTor = effectiveTor
            )
            _groups.update { it + newGroup }

            // Update parent tab with groupId
            _tabs.update { list ->
                list.map {
                    if (it.id == parent.id) {
                        it.copy(groupId = targetGroupId, groupTitle = targetGroupTitle, groupColor = targetGroupColor)
                    } else it
                }
            }
        } else {
            targetGroupId = UUID.randomUUID().toString()
            targetGroupTitle = "Group"
            targetGroupColor = 0xFF38BDF8
        }

        return openTab(
            url = url,
            title = title,
            isPrivate = effectivePrivate,
            isTor = effectiveTor,
            groupId = targetGroupId,
            groupTitle = targetGroupTitle,
            groupColor = targetGroupColor,
            parentTabId = parentTabId
        )
    }

    fun selectTab(id: String) {
        if (_tabs.value.any { it.id == id }) {
            val oldId = _activeTabId.value
            if (oldId != null && oldId != id) {
                saveTabSessionState(oldId)
            }
            _activeTabId.value = id

            // Throttling & Priority: activate foreground session with PRIORITY_HIGH and throttle background
            for ((tabId, session) in sessionMap) {
                try {
                    val isForeground = tabId == id
                    session.setActive(isForeground)
                    session.setFocused(isForeground)
                    session.setPriorityHint(
                        if (isForeground) org.mozilla.geckoview.GeckoSession.PRIORITY_HIGH
                        else org.mozilla.geckoview.GeckoSession.PRIORITY_DEFAULT
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }

            // RAM Reclamation: prune background GeckoSessions if exceeding max warm capacity
            val maxWarmSessions = MAX_WARM_SESSIONS
            val currentTabs = _tabs.value
            val sortedTabs = currentTabs.sortedByDescending { if (it.id == id) Long.MAX_VALUE else it.lastAccessedAt }
            val tabsToKeep = sortedTabs.take(maxWarmSessions).map { it.id }.toSet() + id
            val sessionsToDiscard = sessionMap.keys.filter { it !in tabsToKeep }

            for (tabId in sessionsToDiscard) {
                saveTabSessionState(tabId)
                clearTabLoaded(tabId)
                sessionMap.remove(tabId)?.let { session ->
                    try {
                        session.close()
                    } catch (e: Exception) {
                        // Safe ignore
                    }
                }
            }

            // Single atomic state update: wake target tab + sleep reclaimed tabs in 1 emission
            val now = System.currentTimeMillis()
            _tabs.update { list ->
                list.map { tab ->
                    when {
                        tab.id == id -> tab.copy(isSleeping = false, lastAccessedAt = now)
                        tab.id in sessionsToDiscard -> tab.copy(isSleeping = true)
                        else -> tab
                    }
                }
            }
            persistTabs()
        }
    }

    fun reclaimInactiveTabMemory(maxActiveSessions: Int = MAX_WARM_SESSIONS) {
        val currentId = _activeTabId.value ?: return
        if (sessionMap.size <= maxActiveSessions) return

        val sortedTabs = _tabs.value.sortedByDescending { it.lastAccessedAt }
        val tabsToKeep = sortedTabs.take(maxActiveSessions).map { it.id }.toSet() + currentId

        val sessionsToDiscard = sessionMap.keys.filter { it !in tabsToKeep }
        if (sessionsToDiscard.isEmpty()) return

        for (tabId in sessionsToDiscard) {
            saveTabSessionState(tabId)
            clearTabLoaded(tabId)
            sessionMap.remove(tabId)?.let { session ->
                try {
                    session.close()
                } catch (e: Exception) {
                    // Safe ignore
                }
            }
        }
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id in sessionsToDiscard) tab.copy(isSleeping = true) else tab
            }
        }
    }

    fun wakeTab(id: String) {
        val now = System.currentTimeMillis()
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == id) {
                    tab.copy(isSleeping = false, lastAccessedAt = now)
                } else {
                    tab
                }
            }
        }
    }

    fun sleepInactiveTabs(thresholdMs: Long = 5 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        val currentId = _activeTabId.value
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id != currentId && (now - tab.lastAccessedAt) > thresholdMs) {
                    tab.copy(isSleeping = true)
                } else {
                    tab
                }
            }
        }
    }

    fun updateActiveTab(url: String, title: String) {
        val currentId = _activeTabId.value ?: return
        var hasChanged = false
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    val newUrl = if (url.isNotBlank() || title == "New Tab") url else tab.url
                    val newTitle = if (title.isNotBlank()) title else tab.title
                    if (newUrl != tab.url || newTitle != tab.title) {
                        hasChanged = true
                        tab.copy(url = newUrl, title = newTitle)
                    } else {
                        tab
                    }
                } else {
                    tab
                }
            }
        }
        if (hasChanged) {
            persistTabs()
        }
    }

    fun toggleDesktopMode(tabId: String? = null): Boolean {
        val targetId = tabId ?: _activeTabId.value ?: return false
        var newMode = false
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == targetId) {
                    newMode = !tab.isDesktopMode
                    tab.copy(isDesktopMode = newMode)
                } else {
                    tab
                }
            }
        }
        persistTabs()
        return newMode
    }

    fun updateActiveTabFavicon(faviconUrl: String) {
        val currentId = _activeTabId.value ?: return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    tab.copy(faviconUrl = faviconUrl)
                } else {
                    tab
                }
            }
        }
    }

    fun updateActiveTabThumbnail(thumbnail: Bitmap) {
        val currentId = _activeTabId.value ?: return
        updateTabThumbnail(currentId, thumbnail)
    }

    fun updateTabThumbnail(tabId: String, thumbnail: Bitmap) {
        TabThumbnailCache.put(tabId, thumbnail)
    }

    fun closeTab(id: String): TabSession? = synchronized(this) {
        val tabToClose = _tabs.value.find { it.id == id } ?: return null

        // Remove cached thumbnail bitmap
        TabThumbnailCache.remove(id)

        // Add to thread-safe recently closed (if not private or tor)
        if (!tabToClose.isPrivate && !tabToClose.isTor) {
            _recentlyClosed.addFirst(tabToClose)
            while (_recentlyClosed.size > 10) {
                _recentlyClosed.pollLast()
            }
        }

        // Close GeckoSession
        clearTabLoaded(id)
        sessionStateMap.remove(id)
        sessionMap.remove(id)?.let { session ->
            try {
                session.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing GeckoSession: ${e.message}")
            }
        }

        // Determine the ideal fallback tab BEFORE removing the tab
        val currentTabList = _tabs.value
        val currentIndex = currentTabList.indexOfFirst { it.id == id }
        val gid = tabToClose.groupId
        val groupTabs = if (gid != null) currentTabList.filter { it.groupId == gid } else emptyList()
        val groupIndex = groupTabs.indexOfFirst { it.id == id }

        val fallbackTabId: String? = when {
            // 1. In the same group: pick previous tab in group, or next tab in group
            groupIndex > 0 -> groupTabs[groupIndex - 1].id
            groupIndex == 0 && groupTabs.size > 1 -> groupTabs[1].id
            // 2. Otherwise: pick previous tab in general tab list, or next tab
            currentIndex > 0 -> currentTabList[currentIndex - 1].id
            currentIndex == 0 && currentTabList.size > 1 -> currentTabList[1].id
            else -> null
        }

        // Clean up tabs atomically
        val updatedTabs = currentTabList.filter { it.id != id }
        _tabs.value = updatedTabs

        // Only delete tab group if all tabs in it have been closed
        if (gid != null) {
            val remainingTabIds = updatedTabs.filter { it.groupId == gid }.map { it.id }
            _groups.update { list ->
                if (remainingTabIds.isEmpty()) {
                    list.filter { it.id != gid }
                } else {
                    list.map { g ->
                        if (g.id == gid) g.copy(tabIds = remainingTabIds) else g
                    }
                }
            }
        }

        // If the active tab was closed, fallback to previous opened tab
        if (_activeTabId.value == id) {
            _activeTabId.value = fallbackTabId ?: updatedTabs.firstOrNull()?.id
        }

        // Ensure at least 1 tab always exists (opens clean home tab, never forces search.azokle.com)
        if (_tabs.value.isEmpty()) {
            openTab("", "New Tab")
        }

        persistTabs()
        return tabToClose
    }

    fun undoCloseTab(): TabSession? {
        val restored = _recentlyClosed.pollFirst() ?: return null
        val gid = restored.groupId
        if (gid != null) {
            val existingGroup = _groups.value.find { it.id == gid }
            if (existingGroup != null) {
                // Re-add to existing group
                _groups.update { list ->
                    list.map { if (it.id == gid && !it.tabIds.contains(restored.id)) it.copy(tabIds = it.tabIds + restored.id) else it }
                }
            } else {
                // Recreate group if it was closed when the last tab was closed
                val recreatedGroup = TabGroup(
                    id = gid,
                    title = restored.groupTitle ?: "Group",
                    color = restored.groupColor ?: 0xFF38BDF8,
                    tabIds = listOf(restored.id),
                    isPrivate = restored.isPrivate,
                    isTor = restored.isTor
                )
                _groups.update { it + recreatedGroup }
            }
        }
        _tabs.update { it + restored }
        selectTab(restored.id)
        persistTabs()
        return restored
    }

    fun closeCategoryTabs(isPrivateOnly: Boolean = false, isTorOnly: Boolean = false) {
        val tabsToClose = _tabs.value.filter {
            when {
                isTorOnly -> it.isTor
                isPrivateOnly -> it.isPrivate && !it.isTor
                else -> !it.isPrivate && !it.isTor
            }
        }
        if (tabsToClose.isEmpty()) return

        val idsToClose = tabsToClose.map { it.id }.toSet()

        // Close GeckoSessions and clean associated caches
        for (id in idsToClose) {
            TabThumbnailCache.remove(id)
            clearTabLoaded(id)
            sessionStateMap.remove(id)
            sessionMap.remove(id)?.let { session ->
                try {
                    session.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing GeckoSession: ${e.message}")
                }
            }
        }

        // Batch update tabs
        _tabs.update { list ->
            list.filter { it.id !in idsToClose }
        }

        // Clean groups belonging to this category only
        _groups.update { list ->
            list.filterNot { g ->
                val matchesCategory = when {
                    isTorOnly -> g.isTor
                    isPrivateOnly -> g.isPrivate && !g.isTor
                    else -> !g.isPrivate && !g.isTor
                }
                matchesCategory && g.tabIds.all { it in idsToClose }
            }.map { g ->
                g.copy(tabIds = g.tabIds.filter { it !in idsToClose })
            }
        }

        // Fix lone tab group markers
        _tabs.update { list ->
            list.map { tab ->
                val gid = tab.groupId
                if (gid != null && _groups.value.none { it.id == gid }) {
                    tab.copy(groupId = null, groupTitle = null, groupColor = null)
                } else tab
            }
        }

        if (_tabs.value.isEmpty()) {
            openTab("", "New Tab")
        } else if (_activeTabId.value in idsToClose) {
            val fallbackId = _tabs.value.first().id
            selectTab(fallbackId)
        }

        // Persist once in batch
        persistTabs()
    }

    fun closeAllTabs() {
        for ((_, session) in sessionMap) {
            try {
                session.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        sessionMap.clear()
        _recentlyClosed.clear()
        _groups.value = emptyList()
        val defaultTab = TabSession()
        _tabs.value = listOf(defaultTab)
        _activeTabId.value = defaultTab.id
        persistTabs()
    }

    // --- Tab Groups Operations ---

    fun createGroup(
        title: String,
        color: Long = 0xFF38BDF8,
        tabIds: List<String> = emptyList(),
        parentTabId: String? = null,
        isPrivate: Boolean = false,
        isTor: Boolean = false
    ): String {
        // Enforce strict security isolation: only group tabs matching this group's isPrivate and isTor mode
        val validTabIds = _tabs.value.filter {
            tabIds.contains(it.id) && it.isPrivate == isPrivate && it.isTor == isTor
        }.map { it.id }

        val groupId = UUID.randomUUID().toString()
        val newGroup = TabGroup(
            id = groupId,
            title = title,
            color = color,
            tabIds = validTabIds,
            parentTabId = parentTabId,
            isPrivate = isPrivate,
            isTor = isTor
        )
        _groups.update { it + newGroup }

        // Update target tabs
        _tabs.update { list ->
            list.map { tab ->
                if (validTabIds.contains(tab.id)) {
                    tab.copy(groupId = groupId, groupTitle = title, groupColor = color)
                } else tab
            }
        }
        persistTabs()
        return groupId
    }

    fun addTabToGroup(tabId: String, groupId: String): Boolean {
        val group = _groups.value.find { it.id == groupId } ?: return false
        val tab = _tabs.value.find { it.id == tabId } ?: return false

        // Security & Privacy isolation enforcement:
        // Tor group can ONLY accept Tor tabs.
        // Private group can ONLY accept Private tabs.
        // Standard group can ONLY accept Standard tabs.
        if (tab.isTor != group.isTor || tab.isPrivate != group.isPrivate) {
            Log.w(TAG, "Security isolation violation: cannot mix tabs across Tor/Private/Standard modes")
            return false
        }

        if (!group.tabIds.contains(tabId)) {
            _groups.update { list ->
                list.map { if (it.id == groupId) it.copy(tabIds = it.tabIds + tabId) else it }
            }
        }
        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(groupId = groupId, groupTitle = group.title, groupColor = group.color) else it }
        }
        persistTabs()
        return true
    }

    fun removeTabFromGroup(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return
        val gid = tab.groupId ?: return

        _tabs.update { list ->
            list.map { if (it.id == tabId) it.copy(groupId = null, groupTitle = null, groupColor = null) else it }
        }

        val remainingInGroup = _tabs.value.filter { it.groupId == gid && it.id != tabId }
        if (remainingInGroup.isEmpty()) {
            // Disband group only if 0 tabs remain
            _groups.update { it.filter { g -> g.id != gid } }
        } else {
            _groups.update { list ->
                list.map { if (it.id == gid) it.copy(tabIds = it.tabIds.filter { id -> id != tabId }) else it }
            }
        }
        persistTabs()
    }

    fun closeGroup(groupId: String) {
        val tabsToClose = _tabs.value.filter { it.groupId == groupId }
        for (t in tabsToClose) {
            TabThumbnailCache.remove(t.id)
            clearTabLoaded(t.id)
            sessionStateMap.remove(t.id)
            sessionMap.remove(t.id)?.let {
                try { it.close() } catch (e: Exception) {}
            }
        }
        _groups.update { it.filter { g -> g.id != groupId } }
        _tabs.update { list ->
            val remaining = list.filter { it.groupId != groupId }
            if (remaining.isNotEmpty()) remaining else listOf(TabSession())
        }
        if (_activeTabId.value !in _tabs.value.map { it.id }) {
            val fallbackId = _tabs.value.first().id
            selectTab(fallbackId)
        }
        persistTabs()
    }

    fun ungroupAll(groupId: String) {
        _groups.update { it.filter { g -> g.id != groupId } }
        _tabs.update { list ->
            list.map { if (it.groupId == groupId) it.copy(groupId = null, groupTitle = null, groupColor = null) else it }
        }
        persistTabs()
    }

    fun renameGroup(groupId: String, newTitle: String, newColor: Long? = null) {
        _groups.update { list ->
            list.map { g ->
                if (g.id == groupId) g.copy(title = newTitle, color = newColor ?: g.color) else g
            }
        }
        val targetColor = newColor ?: _groups.value.find { it.id == groupId }?.color
        _tabs.update { list ->
            list.map { tab ->
                if (tab.groupId == groupId) tab.copy(groupTitle = newTitle, groupColor = targetColor ?: tab.groupColor) else tab
            }
        }
        persistTabs()
    }

    fun getActiveGroup(): TabGroup? {
        val currentTab = activeTab ?: return null
        val gid = currentTab.groupId ?: return null
        return _groups.value.find { it.id == gid }
    }

    fun getActiveGroupTabs(): List<TabSession> {
        val currentTab = activeTab ?: return emptyList()
        val gid = currentTab.groupId ?: return emptyList()
        return _tabs.value.filter { it.groupId == gid }
    }

    fun getTabsInGroup(groupId: String): List<TabSession> {
        return _tabs.value.filter { it.groupId == groupId }
    }

    fun openTabInGroup(url: String, title: String = "New Tab", groupId: String): String {
        val group = _groups.value.find { it.id == groupId }
        return openTab(
            url = url,
            title = title,
            isPrivate = group?.isPrivate ?: false,
            isTor = group?.isTor ?: false,
            groupId = groupId,
            groupTitle = group?.title,
            groupColor = group?.color
        )
    }

    private var persistJob: kotlinx.coroutines.Job? = null

    private fun persistTabs() {
        val context = appContext ?: return
        persistJob?.cancel()
        persistJob = scope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(500L)
            try {
                val standardTabs = _tabs.value.filter { !it.isPrivate && !it.isTor }
                val array = JSONArray()
                for (tab in standardTabs) {
                    array.put(tab.toJson())
                }

                val standardGroups = _groups.value.filter { !it.isPrivate && !it.isTor }
                val groupsArray = JSONArray()
                for (group in standardGroups) {
                    groupsArray.put(group.toJson())
                }

                // Privacy hardened: NEVER write ephemeral Tor or Private Tab IDs to disk!
                val safeActiveTabId = if (standardTabs.any { it.id == _activeTabId.value }) {
                    _activeTabId.value ?: ""
                } else {
                    standardTabs.firstOrNull()?.id ?: ""
                }

                val root = JSONObject().apply {
                    put("activeTabId", safeActiveTabId)
                    put("tabs", array)
                    put("groups", groupsArray)
                }

                withContext(Dispatchers.IO) {
                    val atomicFile = AtomicFile(File(context.filesDir, TABS_STATE_FILE))
                    val stream = atomicFile.startWrite()
                    try {
                        stream.write(root.toString().toByteArray(Charsets.UTF_8))
                        atomicFile.finishWrite(stream)
                    } catch (e: Exception) {
                        atomicFile.failWrite(stream)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist tabs: ${e.message}")
            }
        }
    }

    fun closeEphemeralAndTorTabs() {
        com.azokle.search.core.AzokleCore.tabClosePrivateSessions()
        appContext?.let {
            com.azokle.search.permissions.SitePermissionsManager.getInstance(it).clearPrivatePermissions()
        }
        val ephemeralTabs = _tabs.value.filter { it.isPrivate || it.isTor }
        for (tab in ephemeralTabs) {
            TabThumbnailCache.remove(tab.id)
            clearTabLoaded(tab.id)
            sessionStateMap.remove(tab.id)
            sessionMap.remove(tab.id)?.let {
                try { it.close() } catch (e: Exception) {}
            }
        }
        _tabs.update { list ->
            val remaining = list.filter { !it.isPrivate && !it.isTor }
            if (remaining.isNotEmpty()) remaining else listOf(TabSession())
        }
        _groups.update { list ->
            list.filter { !it.isPrivate && !it.isTor }
        }
        if (_activeTabId.value !in _tabs.value.map { it.id }) {
            _activeTabId.value = _tabs.value.firstOrNull()?.id
        }
        persistTabs()
    }

    private fun loadPersistedTabs(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val atomicFile = AtomicFile(File(context.filesDir, TABS_STATE_FILE))
                if (!atomicFile.baseFile.exists()) return@launch
                val bytes = atomicFile.readFully()
                if (bytes.isEmpty()) return@launch
                val jsonStr = String(bytes, Charsets.UTF_8)
                val root = JSONObject(jsonStr)
                val array = root.optJSONArray("tabs") ?: return@launch
                val groupsArray = root.optJSONArray("groups") ?: JSONArray()
                val savedActiveId = root.optString("activeTabId", "")

                val restoredTabs = mutableListOf<TabSession>()
                for (i in 0 until array.length()) {
                    val tabJson = array.getJSONObject(i)
                    restoredTabs.add(TabSession.fromJson(tabJson))
                }

                val restoredGroups = mutableListOf<TabGroup>()
                for (i in 0 until groupsArray.length()) {
                    val groupJson = groupsArray.getJSONObject(i)
                    restoredGroups.add(TabGroup.fromJson(groupJson))
                }

                val standardPersisted = restoredTabs.filter { !it.isPrivate && !it.isTor }
                if (standardPersisted.isNotEmpty()) {
                    _tabs.value = standardPersisted
                    _groups.value = restoredGroups.filter { !it.isPrivate && !it.isTor }
                    val targetId = if (standardPersisted.any { it.id == savedActiveId }) savedActiveId else standardPersisted.first().id
                    _activeTabId.value = targetId
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load persisted tabs: ${e.message}")
            }
        }
    }
}
