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

package com.azokle.search.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testCleanVersionString() {
        assertEquals("2.0.8", AppUpdateChecker.cleanVersionString("v-2.0.8"))
        assertEquals("2.0.9", AppUpdateChecker.cleanVersionString("v2.0.9"))
        assertEquals("2.1.0", AppUpdateChecker.cleanVersionString("v.2.1.0"))
        assertEquals("3.0.0", AppUpdateChecker.cleanVersionString("release-3.0.0"))
        assertEquals("1.0.0", AppUpdateChecker.cleanVersionString("1.0.0"))
        assertEquals("", AppUpdateChecker.cleanVersionString(""))
        assertEquals("", AppUpdateChecker.cleanVersionString("   "))
    }

    @Test
    fun testIsVersionGreater_newerVersions() {
        // Newer minor release
        assertTrue(AppUpdateChecker.isVersionGreater("v-2.1.0", "2.0.9"))
        assertTrue(AppUpdateChecker.isVersionGreater("2.1.0", "2.0.9"))

        // Newer patch release with multi-digit number
        assertTrue(AppUpdateChecker.isVersionGreater("v-2.0.10", "2.0.9"))
        assertTrue(AppUpdateChecker.isVersionGreater("2.0.10", "2.0.9"))

        // Newer major release
        assertTrue(AppUpdateChecker.isVersionGreater("v-3.0.0", "2.0.9"))
        assertTrue(AppUpdateChecker.isVersionGreater("3.0.0", "2.0.9"))

        // Revision / point release
        assertTrue(AppUpdateChecker.isVersionGreater("v-2.0.9.1", "2.0.9"))

        // Pre-release tag
        assertTrue(AppUpdateChecker.isVersionGreater("v-2.1.0-rc1", "2.0.9"))
    }

    @Test
    fun testIsVersionGreater_olderOrEqualVersions() {
        // Older version (crucially verifying the v- hyphen bug where -2 < 2 is prevented)
        assertFalse(AppUpdateChecker.isVersionGreater("v-2.0.8", "2.0.9"))
        assertFalse(AppUpdateChecker.isVersionGreater("2.0.8", "2.0.9"))
        assertFalse(AppUpdateChecker.isVersionGreater("v-1.9.9", "2.0.9"))

        // Exactly equal version
        assertFalse(AppUpdateChecker.isVersionGreater("v-2.0.9", "2.0.9"))
        assertFalse(AppUpdateChecker.isVersionGreater("v2.0.9", "2.0.9"))
        assertFalse(AppUpdateChecker.isVersionGreater("2.0.9", "2.0.9"))

        // Pre-release of current version
        assertFalse(AppUpdateChecker.isVersionGreater("2.0.9-beta", "2.0.9"))

        // Blank or invalid
        assertFalse(AppUpdateChecker.isVersionGreater("", "2.0.9"))
        assertFalse(AppUpdateChecker.isVersionGreater("v-2.0.9", ""))
        assertFalse(AppUpdateChecker.isVersionGreater("unknown", "2.0.9"))
    }
}
