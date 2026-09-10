package com.azokle.search.permissions

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.azokle.search.core.AzokleCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Sovereign Website Permission Manager for Azokle Search.
 * Powered directly by the native Rust Permissions Core (crates/permissions).
 * Strictly adheres to W3C WebAppSec Origin isolation with ephemeral Private/Tor browsing sandboxing.
 */
class SitePermissionsManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _updatesFlow = MutableStateFlow(System.currentTimeMillis())
    val updatesFlow: StateFlow<Long> = _updatesFlow.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            val jsonStr = prefs.getString(KEY_SITE_PERMISSIONS, null) ?: return
            AzokleCore.importPermissionsJson(jsonStr)
        } catch (e: Exception) {
            android.util.Log.e("SitePermissionsManager", "Failed to load permissions: ${e.message}")
        }
    }

    private fun saveToDisk() {
        try {
            val jsonStr = AzokleCore.exportPermissionsJson()
            prefs.edit().putString(KEY_SITE_PERMISSIONS, jsonStr).apply()
            _updatesFlow.value = System.currentTimeMillis()
        } catch (e: Exception) {
            android.util.Log.e("SitePermissionsManager", "Failed to save permissions: ${e.message}")
        }
    }

    /**
     * Strict W3C Origin Canonicalization: Scheme + Host + Port
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

        val rustStateStr = AzokleCore.getSitePermission(origin, type.name, isPrivate)
        return when (rustStateStr.uppercase()) {
            "ALLOW", "GRANTED" -> SitePermissionState.ALLOW
            "BLOCK", "DENIED" -> SitePermissionState.BLOCK
            else -> getGlobalDefault(type)
        }
    }

    fun setPermission(
        originOrUri: String,
        type: SitePermissionType,
        state: SitePermissionState,
        isPrivate: Boolean = false
    ) {
        val origin = canonicalizeOrigin(originOrUri)
        if (origin.isBlank()) return

        AzokleCore.setSitePermission(origin, type.name, state.name, isPrivate)
        if (!isPrivate) {
            saveToDisk()
        } else {
            _updatesFlow.value = System.currentTimeMillis()
        }
    }

    fun removePermission(originOrUri: String, type: SitePermissionType, isPrivate: Boolean = false) {
        setPermission(originOrUri, type, SitePermissionState.ASK, isPrivate)
    }

    fun clearHostPermissions(originOrUri: String) {
        val origin = canonicalizeOrigin(originOrUri)
        AzokleCore.clearPermissionsForOrigin(origin)
        saveToDisk()
    }

    fun clearPrivatePermissions() {
        AzokleCore.clearEphemeralPermissions()
        _updatesFlow.value = System.currentTimeMillis()
    }

    fun clearAllPermissions() {
        AzokleCore.clearAllPermissions()
        saveToDisk()
    }

    fun getHostPermissions(originOrUri: String, isPrivate: Boolean = false): Map<SitePermissionType, SitePermissionState> {
        val origin = canonicalizeOrigin(originOrUri)
        val json = AzokleCore.getSitePermissionsJson(origin, isPrivate)
        val result = mutableMapOf<SitePermissionType, SitePermissionState>()
        for (type in SitePermissionType.values()) {
            if (json.has(type.name)) {
                val stateStr = json.optString(type.name, "ASK")
                val state = when (stateStr.uppercase()) {
                    "ALLOW" -> SitePermissionState.ALLOW
                    "BLOCK" -> SitePermissionState.BLOCK
                    else -> SitePermissionState.ASK
                }
                result[type] = state
            }
        }
        return result
    }

    fun getAllCustomizedSites(): Map<String, Map<SitePermissionType, SitePermissionState>> {
        val origins = AzokleCore.getAllCustomizedPermissionOrigins()
        val result = mutableMapOf<String, Map<SitePermissionType, SitePermissionState>>()
        for (origin in origins) {
            val perms = getHostPermissions(origin, isPrivate = false)
            if (perms.isNotEmpty()) {
                result[origin] = perms
            }
        }
        return result
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
