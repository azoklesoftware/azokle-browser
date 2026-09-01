package com.azokle.search.browser.tabs

import android.graphics.Bitmap
import org.json.JSONObject
import java.util.UUID

data class TabSession(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://search.azokle.com",
    val title: String = "New Tab",
    val thumbnail: Bitmap? = null,
    val isPrivate: Boolean = false,
    val isTor: Boolean = false,
    val isSleeping: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val groupId: String? = null,
    val groupTitle: String? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("url", url)
            put("title", title)
            put("is_private", isPrivate)
            put("is_tor", isTor)
            put("is_sleeping", isSleeping)
            put("group_id", groupId ?: "")
            put("group_title", groupTitle ?: "")
            put("last_accessed_at", lastAccessedAt)
            put("created_at", createdAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): TabSession {
            return TabSession(
                id = json.optString("id", UUID.randomUUID().toString()),
                url = json.optString("url", "https://search.azokle.com"),
                title = json.optString("title", "New Tab"),
                isPrivate = json.optBoolean("is_private", json.optBoolean("isPrivate", false)),
                isTor = json.optBoolean("is_tor", json.optBoolean("isTor", false)),
                isSleeping = json.optBoolean("is_sleeping", json.optBoolean("isSleeping", false)),
                groupId = (json.optString("group_id", "").takeIf { it.isNotBlank() }
                    ?: json.optString("groupId", "").takeIf { it.isNotBlank() }),
                groupTitle = (json.optString("group_title", "").takeIf { it.isNotBlank() }
                    ?: json.optString("groupTitle", "").takeIf { it.isNotBlank() }),
                lastAccessedAt = if (json.has("last_accessed_at")) json.optLong("last_accessed_at") else json.optLong("lastAccessedAt", System.currentTimeMillis()),
                createdAt = if (json.has("created_at")) json.optLong("created_at") else json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
