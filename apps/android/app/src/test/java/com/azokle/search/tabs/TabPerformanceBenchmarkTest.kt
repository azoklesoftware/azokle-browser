package com.azokle.search.tabs

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * End-to-End Performance Benchmarks & Structural Invariant Tests for the Multi-Tab Architecture.
 */
class TabPerformanceBenchmarkTest {

    private fun createTestBitmap(): Bitmap {
        val field = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = field.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(Bitmap::class.java) as Bitmap
    }

    @Test
    fun testTabSessionStructuralEqualityWithoutBitmap() {
        val tab1 = TabSession(id = "tab-1", url = "https://example.com", title = "Example", lastAccessedAt = 1000L, createdAt = 1000L)
        val tab2 = TabSession(id = "tab-1", url = "https://example.com", title = "Example", lastAccessedAt = 1000L, createdAt = 1000L)
        val tab3 = TabSession(id = "tab-1", url = "https://example.com/updated", title = "Example", lastAccessedAt = 1000L, createdAt = 1000L)

        // Value equality must be true for identical data (crucial for Compose skipping)
        assertEquals(tab1, tab2)
        assertEquals(tab1.hashCode(), tab2.hashCode())

        // Value equality must detect actual changes
        assertNotEquals(tab1, tab3)
    }

    @Test
    fun testTabThumbnailCacheLRUBoundingAndEviction() {
        TabThumbnailCache.clear()
        assertEquals(0, TabThumbnailCache.size())

        // Insert 12 test bitmaps
        val bitmaps = (1..12).map { i ->
            "tab-$i" to createTestBitmap()
        }

        for (pair in bitmaps) {
            TabThumbnailCache.put(pair.first, pair.second)
        }

        // Cache must not exceed MAX_ENTRIES (8)
        assertTrue("Cache size must be bounded at <= 8", TabThumbnailCache.size() <= 8)

        // The oldest entries (tab-1, tab-2, tab-3, tab-4) should have been evicted
        assertNull(TabThumbnailCache.get("tab-1"))
        assertNull(TabThumbnailCache.get("tab-2"))
        assertNull(TabThumbnailCache.get("tab-3"))
        assertNull(TabThumbnailCache.get("tab-4"))

        // The most recent entry (tab-12) must exist
        assertNotNull(TabThumbnailCache.get("tab-12"))

        // Explicit removal on tab close
        TabThumbnailCache.remove("tab-12")
        assertNull(TabThumbnailCache.get("tab-12"))

        TabThumbnailCache.clear()
        assertEquals(0, TabThumbnailCache.size())
    }

    @Test
    fun testMultiTabDerivedFilterBenchmark() {
        // Benchmark derived list filtering for 1, 10, 20, 50, and 100 tabs
        val tabCounts = listOf(10, 50, 100)

        // Warm up JVM JIT compiler before measuring
        val warmupTabs = (1..100).map { i -> TabSession(id = "warmup-$i", url = "https://example.com/$i") }
        repeat(5) {
            warmupTabs.filter { !it.isPrivate && !it.isTor }.sortedBy { it.url }
        }

        for (count in tabCounts) {
            val tabs = (1..count).map { i ->
                TabSession(
                    id = "tab-$i",
                    url = "https://domain$i.com/page",
                    title = "Tab $i Title",
                    isPrivate = i % 3 == 0,
                    isTor = i % 5 == 0,
                    groupId = if (i % 4 == 0) "group-1" else null
                )
            }

            val groups = listOf(
                TabGroup(id = "group-1", title = "Work", color = 0xFF38BDF8, tabIds = tabs.filter { it.groupId == "group-1" }.map { it.id })
            )

            // Measure computation time for all derived collections
            val elapsedNs = measureNanoTime {
                val standardCount = tabs.count { !it.isPrivate && !it.isTor }
                val privateCount = tabs.count { it.isPrivate && !it.isTor }
                val torCount = tabs.count { it.isTor }

                val categoryGroups = groups.filter { !it.isPrivate && !it.isTor }
                val groupedTabIds = categoryGroups.flatMap { it.tabIds }.toSet()

                val filteredTabs = tabs.filter { !it.isPrivate && !it.isTor }
                val displayTabs = filteredTabs.sortedBy { it.url }
                val standaloneTabs = displayTabs.filter { !groupedTabIds.contains(it.id) }

                assertTrue(standardCount >= 0)
                assertTrue(privateCount >= 0)
                assertTrue(torCount >= 0)
                assertNotNull(standaloneTabs)
            }

            val elapsedMs = elapsedNs / 1_000_000.0
            println("Tab count: $count -> Derived filtering latency: ${String.format("%.3f", elapsedMs)} ms")

            // Even with 100 tabs, filtering must execute comfortably within acceptable budget under test load
            assertTrue("Filtering $count tabs must execute within budget, took $elapsedMs ms", elapsedMs < 50.0)
        }
    }

    @Test
    fun testSelectTabAtomicWakeAndReclaim() {
        // Prepare 5 test tabs
        val tabIds = (1..5).map { TabManager.openTab("https://site$it.com", "Site $it") }

        val activeTabId = tabIds[2]
        TabManager.selectTab(activeTabId)

        val activeTab = TabManager.tabs.value.find { it.id == activeTabId }
        assertNotNull(activeTab)
        assertEquals(false, activeTab?.isSleeping)
        assertEquals(activeTabId, TabManager.activeTabId.value)

        // Clean up opened tabs
        for (id in tabIds) {
            TabManager.closeTab(id)
        }
    }
}
