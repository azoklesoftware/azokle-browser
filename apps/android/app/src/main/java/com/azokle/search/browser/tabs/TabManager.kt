package com.azokle.search.browser.tabs

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
import java.util.concurrent.ConcurrentHashMap

/**
 * Sovereign High-Performance Tab & Session Orchestrator for Azokle Search.
 * Features:
 *  - Atomic JSON disk persistence for non-private tabs (survives Android LMK / process death)
 *  - Strict ephemeral in-memory isolation for Private and Tor tabs (Zero disk footprint)
 *  - Recently closed tab undo stack (max 10 items)
 *  - Real-time tab grouping (manual and auto-domain collections)
 *  - Foreground/background session throttling
 */
object TabManager {
    private const val TAG = "AzokleTabManager"
    private const val TABS_STATE_FILE = "azokle_tabs_state_v1.json"

    private val _tabs = MutableStateFlow<List<TabSession>>(listOf(TabSession()))
    val tabs: StateFlow<List<TabSession>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(_tabs.value.firstOrNull()?.id)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val sessionMap = ConcurrentHashMap<String, GeckoSession>()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Recently Closed Tabs Stack for Instant Undo
    private val _recentlyClosed = mutableListOf<TabSession>()
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
        return sessionMap.getOrPut(id) {
            val s = com.azokle.search.browser.gecko.GeckoEngineManager.createSession(context, isPrivate, isTor)
            val isActive = id == _activeTabId.value
            try {
                s.setActive(isActive)
            } catch (e: Exception) {
                // Ignore
            }
            s
        }
    }

    fun openTab(
        url: String,
        title: String = "New Tab",
        isPrivate: Boolean = false,
        isTor: Boolean = false,
        groupId: String? = null,
        groupTitle: String? = null
    ): String {
        val newTab = TabSession(
            url = url,
            title = title,
            isPrivate = isPrivate,
            isTor = isTor,
            groupId = groupId,
            groupTitle = groupTitle
        )
        _tabs.update { it + newTab }
        selectTab(newTab.id)
        if (!isPrivate && !isTor) {
            persistTabs()
        }
        return newTab.id
    }

    fun openTorTab(context: Context? = null, url: String = "about:tor", title: String = "Tor Window"): String {
        if (context != null) {
            com.azokle.search.security.TorCircuitManager.startTorService(context)
        } else {
            com.azokle.search.security.TorCircuitManager.startTorConnection()
        }
        return openTab(url = url, title = title, isPrivate = true, isTor = true)
    }

    fun selectTab(id: String) {
        if (_tabs.value.any { it.id == id }) {
            _activeTabId.value = id
            wakeTab(id)

            // Throttling: activate foreground session and throttle background sessions
            for ((tabId, session) in sessionMap) {
                try {
                    session.setActive(tabId == id)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            persistTabs()
        }
    }

    fun wakeTab(id: String) {
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == id) {
                    tab.copy(isSleeping = false, lastAccessedAt = System.currentTimeMillis())
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
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    tab.copy(
                        url = if (url.isNotBlank()) url else tab.url,
                        title = if (title.isNotBlank()) title else tab.title
                    )
                } else {
                    tab
                }
            }
        }
        persistTabs()
    }

    fun updateActiveTabThumbnail(thumbnail: Bitmap) {
        val currentId = _activeTabId.value ?: return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id == currentId) {
                    tab.copy(thumbnail = thumbnail)
                } else {
                    tab
                }
            }
        }
    }

    fun closeTab(id: String): TabSession? {
        val currentList = _tabs.value
        val tabToClose = currentList.find { it.id == id }

        // Clean up and close native GeckoSession
        sessionMap.remove(id)?.let { session ->
            try {
                session.close()
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (tabToClose != null && !tabToClose.isPrivate && !tabToClose.isTor) {
            // Only save regular non-private/non-tor tabs to recently closed (max 10 items)
            _recentlyClosed.add(0, tabToClose)
            if (_recentlyClosed.size > 10) {
                _recentlyClosed.removeAt(_recentlyClosed.lastIndex)
            }
        }

        val remaining = currentList.filter { it.id != id }
        _tabs.value = remaining

        if (_activeTabId.value == id) {
            val newActiveId = remaining.lastOrNull()?.id
            _activeTabId.value = newActiveId
            if (newActiveId != null) {
                selectTab(newActiveId)
            }
        }

        persistTabs()
        return tabToClose
    }

    fun undoCloseTab(): TabSession? {
        if (_recentlyClosed.isEmpty()) return null
        val restoredTab = _recentlyClosed.removeAt(0)
        _tabs.update { it + restoredTab }
        selectTab(restoredTab.id)
        persistTabs()
        return restoredTab
    }

    fun closeCategoryTabs(isPrivateOnly: Boolean = false, isTorOnly: Boolean = false) {
        val tabsToClose = when {
            isTorOnly -> _tabs.value.filter { it.isTor }
            isPrivateOnly -> _tabs.value.filter { it.isPrivate && !it.isTor }
            else -> _tabs.value.filter { !it.isPrivate && !it.isTor }
        }
        for (tab in tabsToClose) {
            sessionMap.remove(tab.id)?.let {
                try { it.close() } catch (e: Exception) {}
            }
        }
        val remaining = _tabs.value.filter { it !in tabsToClose }
        _tabs.value = remaining
        if (_activeTabId.value !in remaining.map { it.id }) {
            val nextId = remaining.firstOrNull()?.id
            _activeTabId.value = nextId
            if (nextId != null) selectTab(nextId)
        }
        persistTabs()
    }

    fun closeAllTabs(isPrivateOnly: Boolean = false) {
        if (isPrivateOnly) {
            closeCategoryTabs(isPrivateOnly = true)
        } else {
            for ((_, session) in sessionMap) {
                try { session.close() } catch (e: Exception) {}
            }
            sessionMap.clear()
            _tabs.value = emptyList()
            _activeTabId.value = null
            _recentlyClosed.clear()
        }
        persistTabs()
    }

    fun groupTabsByDomain() {
        _tabs.update { list ->
            list.map { tab ->
                if (!tab.isPrivate && !tab.isTor) {
                    val domain = try {
                        Uri.parse(tab.url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() } ?: "Other"
                    } catch (e: Exception) {
                        "Other"
                    }
                    tab.copy(groupId = domain, groupTitle = domain.replaceFirstChar { it.uppercase() })
                } else tab
            }
        }
        persistTabs()
    }

    fun clearTabGrouping() {
        _tabs.update { list ->
            list.map { it.copy(groupId = null, groupTitle = null) }
        }
        persistTabs()
    }

    private fun persistTabs() {
        val context = appContext ?: return
        scope.launch {
            try {
                // Strictly persist ONLY non-private, non-Tor tabs
                val persistableTabs = _tabs.value.filter { !it.isPrivate && !it.isTor }
                val root = JSONObject()
                val array = JSONArray()
                persistableTabs.forEach { tab ->
                    array.put(tab.toJson())
                }
                root.put("activeTabId", _activeTabId.value ?: "")
                root.put("tabs", array)

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
        val ephemeralTabs = _tabs.value.filter { it.isPrivate || it.isTor }
        for (tab in ephemeralTabs) {
            sessionMap.remove(tab.id)?.let {
                try { it.close() } catch (e: Exception) {}
            }
        }
        val remaining = _tabs.value.filter { !it.isPrivate && !it.isTor }
        _tabs.value = remaining
        if (_activeTabId.value !in remaining.map { it.id }) {
            _activeTabId.value = remaining.firstOrNull()?.id
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
                val savedActiveId = root.optString("activeTabId", "")

                val restoredTabs = mutableListOf<TabSession>()
                for (i in 0 until array.length()) {
                    val tabJson = array.getJSONObject(i)
                    restoredTabs.add(TabSession.fromJson(tabJson))
                }

                // Strictly ensure only standard non-Tor, non-private tabs are ever loaded into memory
                val standardPersisted = restoredTabs.filter { !it.isPrivate && !it.isTor }
                if (standardPersisted.isNotEmpty()) {
                    _tabs.value = standardPersisted
                    val targetId = if (standardPersisted.any { it.id == savedActiveId }) savedActiveId else standardPersisted.first().id
                    _activeTabId.value = targetId
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load persisted tabs: ${e.message}")
            }
        }
    }
}

