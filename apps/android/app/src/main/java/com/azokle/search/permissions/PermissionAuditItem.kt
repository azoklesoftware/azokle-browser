package com.azokle.search.permissions

data class PermissionAuditItem(
    val id: String,
    val origin: String,
    val permissionType: String,
    val state: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPrivate: Boolean = false
) {
    val cleanHost: String = run {
        try {
            val jHost = java.net.URI(origin).host
            if (!jHost.isNullOrBlank()) {
                return@run jHost.removePrefix("www.")
            }
        } catch (_: Exception) {}

        try {
            val aHost = android.net.Uri.parse(origin).host
            if (!aHost.isNullOrBlank()) {
                return@run aHost.removePrefix("www.")
            }
        } catch (_: Exception) {}

        val stripped = origin.substringAfter("://", origin).substringBefore('/').substringBefore(':')
        stripped.removePrefix("www.").ifBlank { origin }
    }
}
