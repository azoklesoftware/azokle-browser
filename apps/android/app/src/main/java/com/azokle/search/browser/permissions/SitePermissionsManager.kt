package com.azokle.search.browser.permissions

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * High-Performance, Privacy-Centric Website Permission Manager for Azokle Search.
 * Modeled after Brave & Firefox Quantum site permission architectures.
 * Strictly adheres to W3C WebAppSec Origin isolation with ephemeral Private/Tor browsing sandboxing.
 */
class SitePermissionsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Persistent Cache: Origin (e.g. "https://example.com:8443") -> (PermissionType -> PermissionState)
    private val memoryCache = ConcurrentHashMap<String, MutableMap<SitePermissionType, SitePermissionState>>()

    // Volatile Ephemeral Cache for Incognito / Tor Tabs (NEVER persisted to disk)
    private val privateMemoryCache = ConcurrentHashMap<String, MutableMap<SitePermissionType, SitePermissionState>>()

    private val _updatesFlow = MutableStateFlow(System.currentTimeMillis())
    val updatesFlow: StateFlow<Long> = _updatesFlow.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            val jsonStr = prefs.getString(KEY_SITE_PERMISSIONS, null) ?: return
            val root = JSONObject(jsonStr)
            val keys = root.keys()
            while (keys.hasNext()) {
                val originKey = keys.next()
                val siteObj = root.optJSONObject(originKey) ?: continue
                val canonicalKey = canonicalizeOrigin(originKey)
                val map = mutableMapOf<SitePermissionType, SitePermissionState>()
                for (type in SitePermissionType.values()) {
                    if (siteObj.has(type.name)) {
                        val stateStr = siteObj.getString(type.name)
                        try {
                            map[type] = SitePermissionState.valueOf(stateStr)
                        } catch (e: Exception) {
                            // Ignore invalid entries
                        }
                    }
                }
                if (map.isNotEmpty()) {
                    memoryCache[canonicalKey] = map
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SitePermissionsManager", "Failed to load permissions: ${e.message}")
        }
    }

    private fun saveToDisk() {
        try {
            val root = JSONObject()
            for ((origin, map) in memoryCache) {
                if (map.isEmpty()) continue
                val siteObj = JSONObject()
                for ((type, state) in map) {
                    siteObj.put(type.name, state.name)
                }
                root.put(origin, siteObj)
            }
            prefs.edit().putString(KEY_SITE_PERMISSIONS, root.toString()).apply()
            _updatesFlow.value = System.currentTimeMillis()
        } catch (e: Exception) {
            android.util.Log.e("SitePermissionsManager", "Failed to save permissions: ${e.message}")
        }
    }

    /**
     * Strict W3C Origin Canonicalization: Scheme + Host + Port
     * Preserves port isolation (e.g. https://domain.com:8443 vs https://domain.com:443)
     * Normalizes default ports (:80 on http, :443 on https).
     */
    fun canonicalizeOrigin(rawHostOrUri: String): String {
        if (rawHostOrUri.isBlank() || rawHostOrUri.equals("about:blank", ignoreCase = true)) {
            return ""
        }
        val trimmed = rawHostOrUri.trim().lowercase()
        return try {
            val uri = if (!trimmed.contains("://")) {
                Uri.parse("https://$trimmed")
            } else {
                Uri.parse(trimmed)
            }

            val scheme = uri.scheme?.lowercase() ?: "https"
            val host = uri.host?.lowercase() ?: return trimmed
            val port = uri.port

            val hasNonDefaultPort = port != -1 &&
                !((scheme == "http" && port == 80) || (scheme == "https" && port == 443))

            if (hasNonDefaultPort) {
                "$scheme://$host:$port"
            } else {
                "$scheme://$host"
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    /**
     * Extracts clean display host for UI headers (e.g. "github.com:8443").
     */
    fun getDisplayHost(originOrUri: String): String {
        val canonical = canonicalizeOrigin(originOrUri)
        return try {
            val uri = Uri.parse(canonical)
            val host = uri.host ?: canonical
            val port = uri.port
            if (port != -1 && !((uri.scheme == "http" && port == 80) || (uri.scheme == "https" && port == 443))) {
                "$host:$port"
            } else {
                host
            }
        } catch (e: Exception) {
            originOrUri
        }
    }

    fun getPermission(
        originOrUri: String,
        type: SitePermissionType,
        isPrivate: Boolean = false
    ): SitePermissionState {
        val origin = canonicalizeOrigin(originOrUri)
        if (origin.isBlank()) return getGlobalDefault(type)

        if (isPrivate) {
            privateMemoryCache[origin]?.get(type)?.let { return it }
        }

        memoryCache[origin]?.get(type)?.let { return it }

        return getGlobalDefault(type)
    }

    fun setPermission(
        originOrUri: String,
        type: SitePermissionType,
        state: SitePermissionState,
        isPrivate: Boolean = false
    ) {
        val origin = canonicalizeOrigin(originOrUri)
        if (origin.isBlank()) return

        if (isPrivate) {
            // Ephemeral in-memory store for Private / Tor mode (Zero disk footprint)
            val map = privateMemoryCache.getOrPut(origin) { mutableMapOf() }
            map[type] = state
            _updatesFlow.value = System.currentTimeMillis()
            return
        }

        val map = memoryCache.getOrPut(origin) { mutableMapOf() }
        map[type] = state
        saveToDisk()
    }

    fun removePermission(originOrUri: String, type: SitePermissionType, isPrivate: Boolean = false) {
        val origin = canonicalizeOrigin(originOrUri)
        if (isPrivate) {
            privateMemoryCache[origin]?.let { map ->
                map.remove(type)
                if (map.isEmpty()) privateMemoryCache.remove(origin)
                _updatesFlow.value = System.currentTimeMillis()
            }
            return
        }

        val map = memoryCache[origin] ?: return
        map.remove(type)
        if (map.isEmpty()) {
            memoryCache.remove(origin)
        }
        saveToDisk()
    }

    fun clearHostPermissions(originOrUri: String) {
        val origin = canonicalizeOrigin(originOrUri)
        memoryCache.remove(origin)
        privateMemoryCache.remove(origin)
        saveToDisk()
    }

    fun clearPrivatePermissions() {
        privateMemoryCache.clear()
        _updatesFlow.value = System.currentTimeMillis()
    }

    fun clearAllPermissions() {
        memoryCache.clear()
        privateMemoryCache.clear()
        saveToDisk()
    }

    fun getHostPermissions(originOrUri: String, isPrivate: Boolean = false): Map<SitePermissionType, SitePermissionState> {
        val origin = canonicalizeOrigin(originOrUri)
        if (isPrivate) {
            val privateMap = privateMemoryCache[origin]?.toMap()
            if (!privateMap.isNullOrEmpty()) return privateMap
        }
        return memoryCache[origin]?.toMap() ?: emptyMap()
    }

    fun getAllCustomizedSites(): Map<String, Map<SitePermissionType, SitePermissionState>> {
        return memoryCache.mapValues { it.value.toMap() }
    }

    fun getGlobalDefault(type: SitePermissionType): SitePermissionState {
        val key = KEY_DEFAULT_PREFIX + type.name
        val defaultFallback = if (type == SitePermissionType.AUTOPLAY) {
            SitePermissionState.ALLOW.name
        } else {
            SitePermissionState.ASK.name
        }
        val defaultStateStr = prefs.getString(key, defaultFallback) ?: defaultFallback
        return try {
            SitePermissionState.valueOf(defaultStateStr)
        } catch (e: Exception) {
            if (type == SitePermissionType.AUTOPLAY) SitePermissionState.ALLOW else SitePermissionState.ASK
        }
    }

    fun setGlobalDefault(type: SitePermissionType, state: SitePermissionState) {
        val key = KEY_DEFAULT_PREFIX + type.name
        prefs.edit().putString(key, state.name).apply()
        _updatesFlow.value = System.currentTimeMillis()
    }

    fun getAutoplayPolicy(originOrUri: String, isPrivate: Boolean = false): SitePermissionState {
        return getPermission(originOrUri, SitePermissionType.AUTOPLAY, isPrivate = isPrivate)
    }

    fun setAutoplayPolicy(originOrUri: String, state: SitePermissionState, isPrivate: Boolean = false) {
        setPermission(originOrUri, SitePermissionType.AUTOPLAY, state, isPrivate = isPrivate)
    }

    fun getGlobalAutoplayPolicy(): SitePermissionState {
        return getGlobalDefault(SitePermissionType.AUTOPLAY)
    }

    fun setGlobalAutoplayPolicy(state: SitePermissionState) {
        setGlobalDefault(SitePermissionType.AUTOPLAY, state)
    }

    companion object {
        private const val PREFS_NAME = "azokle_site_permissions"
        private const val KEY_SITE_PERMISSIONS = "saved_site_permissions_json"
        private const val KEY_DEFAULT_PREFIX = "default_perm_"

        @Volatile
        private var INSTANCE: SitePermissionsManager? = null

        fun getInstance(context: Context): SitePermissionsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SitePermissionsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
