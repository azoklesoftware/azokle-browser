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

package com.azokle.search.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HistoryItemTest {

    @Test
    fun testHistoryItemPrecomputedPropertiesForWebpage() {
        val item = HistoryItem(
            id = "1",
            url = "https://www.wikipedia.org/wiki/Kotlin_(programming_language)",
            title = "Kotlin (programming language) - Wikipedia",
            timestamp = 1000L
        )

        assertEquals("wikipedia.org", item.cleanHost)
        assertEquals("Kotlin (programming language) - Wikipedia", item.displayTitle)
        assertEquals("https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128", item.faviconUrl)
        assertFalse(item.isSearch)
    }

    @Test
    fun testHistoryItemPrecomputedPropertiesForSearch() {
        val item = HistoryItem(
            id = "2",
            url = "https://azokle.com/search?q=jetpack+compose",
            title = "jetpack compose - Azokle Search",
            timestamp = 2000L,
            isSearch = true,
            searchQuery = "jetpack compose"
        )

        assertEquals("azokle.com", item.cleanHost)
        assertEquals("jetpack compose", item.displayTitle)
        assertTrue(item.isSearch)
    }

    @Test
    fun testHistoryItemFallbackTitleWhenEmpty() {
        val item = HistoryItem(
            id = "3",
            url = "https://github.com/torvalds/linux",
            title = "",
            timestamp = 3000L
        )

        assertEquals("github.com", item.cleanHost)
        assertEquals("github.com", item.displayTitle)
    }

    @Test
    fun testComplexHostExtraction() {
        val itemWithPort = HistoryItem(
            id = "4",
            url = "http://localhost:8080/dashboard?user=admin",
            title = "Dashboard",
            timestamp = 4000L
        )
        assertEquals("localhost", itemWithPort.cleanHost)

        val itemWithSubdomain = HistoryItem(
            id = "5",
            url = "https://blog.rust-lang.org/2026/01/01/new-year.html",
            title = "Rust Blog",
            timestamp = 5000L
        )
        assertEquals("blog.rust-lang.org", itemWithSubdomain.cleanHost)
    }

    @Test
    fun testHistoryFilteringLogic() {
        val items = listOf(
            HistoryItem(id = "1", url = "https://azokle.com", title = "Azokle", isSearch = false),
            HistoryItem(id = "2", url = "https://search.azokle.com?q=kotlin", title = "kotlin", isSearch = true, searchQuery = "kotlin"),
            HistoryItem(id = "3", url = "https://wikipedia.org", title = "Wikipedia", isSearch = false)
        )

        val pagesOnly = items.filter { !it.isSearch }
        assertEquals(2, pagesOnly.size)
        assertEquals("1", pagesOnly[0].id)
        assertEquals("3", pagesOnly[1].id)

        val searchesOnly = items.filter { it.isSearch }
        assertEquals(1, searchesOnly.size)
        assertEquals("2", searchesOnly[0].id)
    }

    @Test
    fun testTimelineDateGroupingBuckets() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis

        val todayItem = HistoryItem(id = "t1", url = "https://a.com", title = "A", timestamp = todayStart + 1000L)
        val yesterdayItem = HistoryItem(id = "y1", url = "https://b.com", title = "B", timestamp = todayStart - (dayMs / 2))
        val lastWeekItem = HistoryItem(id = "w1", url = "https://c.com", title = "C", timestamp = todayStart - (3 * dayMs))
        val lastMonthItem = HistoryItem(id = "m1", url = "https://d.com", title = "D", timestamp = todayStart - (15 * dayMs))
        val olderItem = HistoryItem(id = "o1", url = "https://e.com", title = "E", timestamp = todayStart - (40 * dayMs))

        val list = listOf(todayItem, yesterdayItem, lastWeekItem, lastMonthItem, olderItem)

        val yesterdayStart = todayStart - dayMs
        val last7DaysStart = todayStart - (6 * dayMs)
        val last30DaysStart = todayStart - (29 * dayMs)

        val buckets = list.groupBy { item ->
            when {
                item.timestamp >= todayStart -> "Today"
                item.timestamp >= yesterdayStart -> "Yesterday"
                item.timestamp >= last7DaysStart -> "Previous 7 Days"
                item.timestamp >= last30DaysStart -> "Last 30 Days"
                else -> "Older"
            }
        }

        assertEquals(listOf(todayItem), buckets["Today"])
        assertEquals(listOf(yesterdayItem), buckets["Yesterday"])
        assertEquals(listOf(lastWeekItem), buckets["Previous 7 Days"])
        assertEquals(listOf(lastMonthItem), buckets["Last 30 Days"])
        assertEquals(listOf(olderItem), buckets["Older"])
    }

    @Test
    fun testGroupPositioningLogic() {
        val items = listOf("a", "b", "c")
        val positions = items.indices.map { index ->
            when {
                items.size == 1 -> "SINGLE"
                index == 0 -> "FIRST"
                index == items.size - 1 -> "LAST"
                else -> "MIDDLE"
            }
        }
        assertEquals(listOf("FIRST", "MIDDLE", "LAST"), positions)

        val singleList = listOf("only")
        val singlePositions = singleList.indices.map { index ->
            when {
                singleList.size == 1 -> "SINGLE"
                index == 0 -> "FIRST"
                index == singleList.size - 1 -> "LAST"
                else -> "MIDDLE"
            }
        }
        assertEquals(listOf("SINGLE"), singlePositions)
    }

    @Test
    fun testIgnoredSchemesForHistory() {
        fun isIgnoredScheme(url: String): Boolean {
            val lower = url.lowercase().trim()
            return lower.startsWith("about:") ||
                    lower.startsWith("chrome:") ||
                    lower.startsWith("javascript:") ||
                    lower.startsWith("data:") ||
                    lower.startsWith("blob:") ||
                    lower.startsWith("file:") ||
                    lower.startsWith("content:") ||
                    lower.startsWith("resource:") ||
                    lower.startsWith("intent:")
        }

        assertTrue(isIgnoredScheme("about:blank"))
        assertTrue(isIgnoredScheme("about:config"))
        assertTrue(isIgnoredScheme("chrome://version"))
        assertTrue(isIgnoredScheme("javascript:alert(1)"))
        assertTrue(isIgnoredScheme("data:text/html,<h1>Hello</h1>"))
        assertTrue(isIgnoredScheme("blob:https://example.com/uuid"))
        assertTrue(isIgnoredScheme("file:///android_asset/page.html"))
        assertTrue(isIgnoredScheme("content://media/external/images"))
        assertTrue(isIgnoredScheme("resource://android/res"))
        assertTrue(isIgnoredScheme("intent://#Intent;action=android.intent.action.VIEW;end"))

        assertFalse(isIgnoredScheme("https://example.com"))
        assertFalse(isIgnoredScheme("http://localhost:8080"))
        assertFalse(isIgnoredScheme("https://search.azokle.com/q?test"))
    }

    @Test
    fun testEmptyHistorySerializationGuards() {
        val emptyJson = "[]"
        val blankJson = "   "
        val validJson = "[{\"id\":\"1\",\"url\":\"https://example.com\",\"title\":\"Example\"}]"

        fun shouldPurgeFile(jsonStr: String): Boolean {
            return jsonStr.isBlank() || jsonStr == "[]"
        }

        assertTrue(shouldPurgeFile(emptyJson))
        assertTrue(shouldPurgeFile(blankJson))
        assertFalse(shouldPurgeFile(validJson))
    }
}
