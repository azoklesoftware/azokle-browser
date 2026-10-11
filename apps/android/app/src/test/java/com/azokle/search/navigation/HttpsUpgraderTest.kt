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

package com.azokle.search.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpsUpgraderTest {

    @Before
    fun setUp() {
        HttpsUpgrader.clearSessionFailures()
        HttpsUpgrader.removeException("insecure-legacy.org")
    }

    @Test
    fun testShouldUpgrade_standardHttpUrl() {
        val rawUrl = "http://example.com/search?q=privacy"
        assertTrue(HttpsUpgrader.shouldUpgrade(rawUrl))
        assertEquals("https://example.com/search?q=privacy", HttpsUpgrader.upgrade(rawUrl))
    }

    @Test
    fun testShouldUpgrade_alreadyHttps() {
        val rawUrl = "https://example.com/account"
        assertFalse(HttpsUpgrader.shouldUpgrade(rawUrl))
        assertEquals(rawUrl, HttpsUpgrader.upgrade(rawUrl))
    }

    @Test
    fun testShouldUpgrade_onionAndLocalAddressesExempt() {
        assertFalse(HttpsUpgrader.shouldUpgrade("http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/"))
        assertFalse(HttpsUpgrader.shouldUpgrade("http://localhost:8080/dashboard"))
        assertFalse(HttpsUpgrader.shouldUpgrade("http://127.0.0.1/admin"))
        assertFalse(HttpsUpgrader.shouldUpgrade("http://192.168.1.1/setup"))
        assertFalse(HttpsUpgrader.shouldUpgrade("http://10.0.0.1/status"))
        assertFalse(HttpsUpgrader.shouldUpgrade("http://device.local/index.html"))
    }

    @Test
    fun testShouldUpgrade_explicitException() {
        val exemptDomain = "insecure-legacy.org"
        val rawUrl = "http://insecure-legacy.org/docs"

        assertTrue(HttpsUpgrader.shouldUpgrade(rawUrl))

        HttpsUpgrader.addException(exemptDomain)
        assertTrue(HttpsUpgrader.isException(exemptDomain))
        assertTrue(HttpsUpgrader.isException("sub.$exemptDomain"))
        assertFalse(HttpsUpgrader.shouldUpgrade(rawUrl))
        assertEquals(rawUrl, HttpsUpgrader.upgrade(rawUrl))

        HttpsUpgrader.removeException(exemptDomain)
        assertFalse(HttpsUpgrader.isException(exemptDomain))
        assertTrue(HttpsUpgrader.shouldUpgrade(rawUrl))
    }

    @Test
    fun testShouldUpgrade_recordedFailurePreventsRepeatedUpgrade() {
        val failingHost = "failing-tls.net"
        val rawUrl = "http://failing-tls.net/page"

        assertTrue(HttpsUpgrader.shouldUpgrade(rawUrl))

        // Record that HTTPS failed for this host
        HttpsUpgrader.recordUpgradeFailure(failingHost)

        // Should now gracefully stay on HTTP
        assertFalse(HttpsUpgrader.shouldUpgrade(rawUrl))
        assertEquals(rawUrl, HttpsUpgrader.upgrade(rawUrl))

        // Clear failures restores upgrading
        HttpsUpgrader.clearSessionFailures()
        assertTrue(HttpsUpgrader.shouldUpgrade(rawUrl))
    }
}
