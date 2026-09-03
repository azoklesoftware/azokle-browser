package com.azokle.search.browser.tabs

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class TabManagerTest {

    @Test
    fun testTabSessionIsTorProperty() {
        val standardTab = TabSession(id = "tab-1", url = "https://example.com", title = "Example", isPrivate = false, isTor = false)
        val privateTab = TabSession(id = "tab-2", url = "https://search.azokle.com", title = "Private Search", isPrivate = true, isTor = false)
        val torTab = TabSession(id = "tab-3", url = "about:tor", title = "Tor Window", isPrivate = true, isTor = true)

        assertFalse(standardTab.isTor)
        assertFalse(standardTab.isPrivate)

        assertFalse(privateTab.isTor)
        assertTrue(privateTab.isPrivate)

        assertTrue(torTab.isTor)
        assertTrue(torTab.isPrivate)
    }

    @Test
    fun testEphemeralAndTorTabFiltering() {
        val tabs = listOf(
            TabSession(id = "1", url = "https://example.com", title = "1", isPrivate = false, isTor = false),
            TabSession(id = "2", url = "https://news.com", title = "2", isPrivate = false, isTor = false),
            TabSession(id = "3", url = "https://private.com", title = "3", isPrivate = true, isTor = false),
            TabSession(id = "4", url = "about:tor", title = "4", isPrivate = true, isTor = true)
        )

        val standardTabs = tabs.filter { !it.isPrivate && !it.isTor }
        val privateTabs = tabs.filter { it.isPrivate && !it.isTor }
        val torTabs = tabs.filter { it.isTor }
        val ephemeralTabs = tabs.filter { it.isPrivate || it.isTor }

        assertEquals(2, standardTabs.size)
        assertEquals(1, privateTabs.size)
        assertEquals(1, torTabs.size)
        assertEquals(2, ephemeralTabs.size)

        assertTrue(standardTabs.none { it.isTor || it.isPrivate })
        assertTrue(ephemeralTabs.all { it.isTor || it.isPrivate })
    }

    @Test
    fun testTabGroupSerialization() {
        val group = TabGroup(
            id = "grp-1",
            title = "Research",
            color = 0xFF38BDF8,
            tabIds = listOf("tab-1", "tab-2"),
            parentTabId = "tab-1",
            isPrivate = false,
            isTor = false
        )

        val json = group.toJson()
        assertEquals("grp-1", json.getString("id"))
        assertEquals("Research", json.getString("title"))
        assertEquals(2, json.getJSONArray("tab_ids").length())

        val restored = TabGroup.fromJson(json)
        assertEquals(group.id, restored.id)
        assertEquals(group.title, restored.title)
        assertEquals(group.color, restored.color)
        assertEquals(2, restored.tabIds.size)
        assertEquals("tab-1", restored.parentTabId)
    }

    @Test
    fun testTabSessionGroupFieldsSerialization() {
        val tab = TabSession(
            id = "t-1",
            url = "https://example.org",
            title = "Example Org",
            faviconUrl = "https://example.org/favicon.ico",
            groupId = "grp-1",
            groupTitle = "Research",
            groupColor = 0xFF34D399,
            parentTabId = "p-1"
        )

        val json = tab.toJson()
        assertEquals("grp-1", json.getString("group_id"))
        assertEquals("Research", json.getString("group_title"))
        assertEquals(0xFF34D399, json.getLong("group_color"))
        assertEquals("p-1", json.getString("parent_tab_id"))

        val restored = TabSession.fromJson(json)
        assertEquals(tab.id, restored.id)
        assertEquals(tab.groupId, restored.groupId)
        assertEquals(tab.groupTitle, restored.groupTitle)
        assertEquals(tab.groupColor, restored.groupColor)
        assertEquals(tab.parentTabId, restored.parentTabId)
        assertEquals("https://example.org/favicon.ico", restored.faviconUrl)
    }

    @Test
    fun testThreadSafeRecentlyClosedDequeCap() {
        val deque = ConcurrentLinkedDeque<TabSession>()
        val threadPool = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(20)

        for (i in 1..20) {
            threadPool.submit {
                val tab = TabSession(id = "tab-$i", url = "https://example-$i.com", title = "Tab $i")
                deque.addFirst(tab)
                while (deque.size > 10) {
                    deque.pollLast()
                }
                latch.countDown()
            }
        }

        latch.await()
        threadPool.shutdown()

        assertTrue(deque.size <= 10)
        assertNotNull(deque.pollFirst())
    }

    @Test
    fun testUrlAntiSpoofingHostSanitation() {
        val spoofUrls = listOf(
            "https://paypal.com@evil-attacker.com/login" to "evil-attacker.com",
            "https://user:password@bank.com.fake.org/path" to "bank.com.fake.org",
            "https://www.azokle.com/search?q=test" to "azokle.com",
            "http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/home" to "duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion"
        )

        for ((raw, expectedHost) in spoofUrls) {
            val uri = try { URI(raw) } catch (e: Exception) { null }
            val cleanHost = try {
                val extracted = uri?.host?.removePrefix("www.")?.trim()
                if (!extracted.isNullOrBlank()) {
                    extracted
                } else {
                    raw.removePrefix("https://")
                        .removePrefix("http://")
                        .substringBefore("/")
                        .substringAfterLast("@")
                        .removePrefix("www.")
                        .trim()
                }
            } catch (e: Exception) {
                raw
            }

            assertEquals(expectedHost, cleanHost)
        }
    }

    @Test
    fun testTabSessionDesktopModeSerialization() {
        val tabMobile = TabSession(id = "tab-mob", url = "https://example.com", isDesktopMode = false)
        val tabDesktop = TabSession(id = "tab-desk", url = "https://example.com", isDesktopMode = true)

        assertFalse(tabMobile.isDesktopMode)
        assertTrue(tabDesktop.isDesktopMode)

        val jsonMobile = tabMobile.toJson()
        val jsonDesktop = tabDesktop.toJson()

        assertEquals(false, jsonMobile.getBoolean("is_desktop_mode"))
        assertEquals(true, jsonDesktop.getBoolean("is_desktop_mode"))

        val restoredMobile = TabSession.fromJson(jsonMobile)
        val restoredDesktop = TabSession.fromJson(jsonDesktop)

        assertFalse(restoredMobile.isDesktopMode)
        assertTrue(restoredDesktop.isDesktopMode)
    }

    @Test
    fun testTabSessionWallpaperResIdSerialization() {
        val tab = TabSession(
            id = "tab-wp",
            url = "",
            title = "New Tab",
            wallpaperResId = 2131230845
        )

        val json = tab.toJson()
        assertTrue(json.has("wallpaper_res_id"))
        assertEquals(2131230845, json.getInt("wallpaper_res_id"))

        val restored = TabSession.fromJson(json)
        assertEquals(2131230845, restored.wallpaperResId)
    }

    @Test
    fun testTabGroupSecurityIsolationRules() {
        val standardTab = TabSession(id = "std-1", url = "https://standard.com", isPrivate = false, isTor = false)
        val privateTab = TabSession(id = "priv-1", url = "https://private.com", isPrivate = true, isTor = false)
        val torTab = TabSession(id = "tor-1", url = "about:tor", isPrivate = true, isTor = true)

        val standardGroup = TabGroup(id = "grp-std", title = "Standard", color = 0xFF38BDF8, isPrivate = false, isTor = false)
        val privateGroup = TabGroup(id = "grp-priv", title = "Private", color = 0xFFA855F7, isPrivate = true, isTor = false)
        val torGroup = TabGroup(id = "grp-tor", title = "Tor", color = 0xFF8B5CF6, isPrivate = true, isTor = true)

        // Strict isolation validation
        fun canAddTabToGroup(tab: TabSession, group: TabGroup): Boolean {
            return tab.isTor == group.isTor && tab.isPrivate == group.isPrivate
        }

        // Standard tab should only be allowed in standard group
        assertTrue(canAddTabToGroup(standardTab, standardGroup))
        assertFalse(canAddTabToGroup(standardTab, privateGroup))
        assertFalse(canAddTabToGroup(standardTab, torGroup))

        // Private tab should only be allowed in private group
        assertFalse(canAddTabToGroup(privateTab, standardGroup))
        assertTrue(canAddTabToGroup(privateTab, privateGroup))
        assertFalse(canAddTabToGroup(privateTab, torGroup))

        // Tor tab should only be allowed in tor group
        assertFalse(canAddTabToGroup(torTab, standardGroup))
        assertFalse(canAddTabToGroup(torTab, privateGroup))
        assertTrue(canAddTabToGroup(torTab, torGroup))
    }
}
