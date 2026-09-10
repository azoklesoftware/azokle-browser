package com.azokle.search.tabs

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabSessionTest {

    @Test
    fun testTabSessionJsonRoundTrip() {
        val original = TabSession(
            id = "test-tab-123",
            url = "https://azokle.com",
            title = "Azokle Sovereign Search",
            faviconUrl = "https://azokle.com/favicon.ico",
            isPrivate = true,
            isTor = true,
            isSleeping = true,
            canGoBack = true,
            canGoForward = false,
            groupId = "group-456",
            groupTitle = "Research",
            groupColor = 0xFF123456L,
            parentTabId = "parent-001",
            isDesktopMode = true,
            wallpaperResId = 1234,
            lastAccessedAt = 1700000000000L,
            createdAt = 1699999000000L
        )

        val json = original.toJson()
        val restored = TabSession.fromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.url, restored.url)
        assertEquals(original.title, restored.title)
        assertEquals(original.faviconUrl, restored.faviconUrl)
        assertEquals(original.isPrivate, restored.isPrivate)
        assertEquals(original.isTor, restored.isTor)
        assertEquals(original.isSleeping, restored.isSleeping)
        assertEquals(original.isDesktopMode, restored.isDesktopMode)
        assertEquals(original.groupId, restored.groupId)
        assertEquals(original.groupTitle, restored.groupTitle)
        assertEquals(original.groupColor, restored.groupColor)
        assertEquals(original.parentTabId, restored.parentTabId)
        assertEquals(original.wallpaperResId, restored.wallpaperResId)
        assertEquals(original.lastAccessedAt, restored.lastAccessedAt)
        assertEquals(original.createdAt, restored.createdAt)
    }

    @Test
    fun testTabSessionLegacyJsonFallback() {
        val legacyJson = JSONObject().apply {
            put("id", "legacy-tab-789")
            put("url", "https://example.org")
            put("title", "Example")
            put("isPrivate", false)
            put("isTor", false)
            put("isSleeping", false)
            put("isDesktopMode", false)
        }

        val restored = TabSession.fromJson(legacyJson)

        assertEquals("legacy-tab-789", restored.id)
        assertEquals("https://example.org", restored.url)
        assertEquals("Example", restored.title)
        assertFalse(restored.isPrivate)
        assertFalse(restored.isTor)
        assertFalse(restored.isSleeping)
        assertNull(restored.faviconUrl)
        assertNull(restored.groupId)
    }
}
