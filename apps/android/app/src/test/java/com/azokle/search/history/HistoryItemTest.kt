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
}
