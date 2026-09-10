package com.azokle.search.permissions

import kotlinx.coroutines.flow.StateFlow

/**
 * Public Contract Interface for Site Permissions Management.
 * Follows DuckDuckGo's -api pattern to decouple consumers from concrete implementations.
 */
interface ISitePermissionsManager {
    val updatesFlow: StateFlow<Long>

    fun canonicalizeOrigin(rawHostOrUri: String): String
    fun getDisplayHost(originOrUri: String): String
    fun getPermission(origin: String, type: SitePermissionType, isPrivate: Boolean = false): SitePermissionState
    fun setPermission(origin: String, type: SitePermissionType, state: SitePermissionState, isPrivate: Boolean = false)
    fun resetPermission(origin: String, type: SitePermissionType, isPrivate: Boolean = false)
    fun clearAllPermissions()
    fun getEffectivePermissions(origin: String, isPrivate: Boolean = false): Map<SitePermissionType, SitePermissionState>
}
