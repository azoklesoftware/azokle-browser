package com.azokle.search.browser.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}

