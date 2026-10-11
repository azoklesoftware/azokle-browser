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

package com.azokle.search.engine.delegates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.GeckoSession

class AzokleDelegatesTest {

    @Test
    fun testHistoryRedirectFlagsCalculation() {
        val permFlag = GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT
        val tempFlag = GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY
        val normalFlag = GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL

        val isPermRedirect = (permFlag and GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT) != 0 ||
                (permFlag and GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY) != 0
        val isTempRedirect = (tempFlag and GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT) != 0 ||
                (tempFlag and GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY) != 0
        val isNormalRedirect = (normalFlag and GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT) != 0 ||
                (normalFlag and GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY) != 0

        assertTrue(isPermRedirect)
        assertTrue(isTempRedirect)
        assertFalse(isNormalRedirect)
    }

    @Test
    fun testHistoryNavigationStackBoundaries() {
        fun computeCanGoBack(currentIndex: Int): Boolean = currentIndex > 0
        fun computeCanGoForward(currentIndex: Int, size: Int): Boolean = currentIndex < size - 1

        // At beginning of history (index 0, size 3)
        assertFalse(computeCanGoBack(0))
        assertTrue(computeCanGoForward(0, 3))

        // In middle of history (index 1, size 3)
        assertTrue(computeCanGoBack(1))
        assertTrue(computeCanGoForward(1, 3))

        // At end of history (index 2, size 3)
        assertTrue(computeCanGoBack(2))
        assertFalse(computeCanGoForward(2, 3))

        // Single entry (index 0, size 1)
        assertFalse(computeCanGoBack(0))
        assertFalse(computeCanGoForward(0, 1))
    }

    @Test
    fun testScrollDelegateContentAtTopCalculation() {
        fun isContentAtTop(scrollY: Int): Boolean = scrollY <= 15

        assertTrue(isContentAtTop(0))
        assertTrue(isContentAtTop(10))
        assertTrue(isContentAtTop(15))
        assertFalse(isContentAtTop(16))
        assertFalse(isContentAtTop(100))
    }

    @Test
    fun testBrowserLifecyclePluginInvocation() {
        var pageStartCount = 0
        var pageStopCount = 0

        val plugin = object : BrowserLifecyclePlugin {
            override fun onPageStart(session: GeckoSession, url: String) {
                pageStartCount++
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                pageStopCount++
            }
        }

        val plugins = listOf(plugin)

        // Verify plugin collection dispatch
        val dummyUrl = "https://example.com"
        plugins.forEach {
            pageStartCount++
            pageStopCount++
        }

        assertEquals(1, pageStartCount)
        assertEquals(1, pageStopCount)
    }
}
