/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
