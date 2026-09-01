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
            put("groupId", groupId ?: "")
            put("groupTitle", groupTitle ?: "")
            put("lastAccessedAt", lastAccessedAt)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): TabSession {
            return TabSession(
                id = json.optString("id", UUID.randomUUID().toString()),
                url = json.optString("url", "https://search.azokle.com"),
                title = json.optString("title", "New Tab"),
                isPrivate = false,
                isTor = false,
                groupId = json.optString("groupId", "").takeIf { it.isNotBlank() },
                groupTitle = json.optString("groupTitle", "").takeIf { it.isNotBlank() },
                lastAccessedAt = json.optLong("lastAccessedAt", System.currentTimeMillis()),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
