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

package com.azokle.search.tabs

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a logical collection of grouped tabs (Brave/Chrome style).
 */
data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val color: Long = 0xFF38BDF8, // Vibrant Cyan Default
    val tabIds: List<String> = emptyList(),
    val parentTabId: String? = null,
    val isPrivate: Boolean = false,
    val isTor: Boolean = false,
    val isCollapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Executes the to json operation.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("color", color)
            val arr = JSONArray()
            tabIds.forEach { arr.put(it) }
            put("tab_ids", arr)
            if (parentTabId != null) put("parent_tab_id", parentTabId)
            put("is_private", isPrivate)
            put("is_tor", isTor)
            put("is_collapsed", isCollapsed)
            put("created_at", createdAt)
        }
    }

    companion object {
        val GROUP_PALETTE = listOf(
            0xFF38BDF8, // Sky Blue
            0xFF34D399, // Emerald Green
            0xFFF43F5E, // Rose / Coral
            0xFFA78BFA, // Purple / Lavender
            0xFFFBBF24, // Amber / Yellow
            0xFFF97316, // Orange
            0xFF06B6D4, // Cyan
            0xFFEC4899  // Pink
        )

        /**
         * Executes the from json operation.
         */
        fun fromJson(json: JSONObject): TabGroup {
            val arr = json.optJSONArray("tab_ids") ?: JSONArray()
            val tabs = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                tabs.add(arr.getString(i))
            }
            return TabGroup(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title", "Group"),
                color = json.optLong("color", 0xFF38BDF8),
                tabIds = tabs,
                parentTabId = json.optString("parent_tab_id", "").takeIf { it.isNotBlank() },
                isPrivate = json.optBoolean("is_private", false),
                isTor = json.optBoolean("is_tor", false),
                isCollapsed = json.optBoolean("is_collapsed", false),
                createdAt = json.optLong("created_at", System.currentTimeMillis())
            )
        }
    }
}
