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

package com.azokle.search.engine

import com.azokle.search.vault.OriginMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.ContentBlocking

class CookieManagementTest {

    @Test
    fun testBaseDomainExtractionForCookieClearing() {
        // Standard FQDNs
        assertEquals("google.com", OriginMatcher.extractRegistrableDomain("www.google.com"))
        assertEquals("google.com", OriginMatcher.extractRegistrableDomain("accounts.google.com"))
        assertEquals("github.com", OriginMatcher.extractRegistrableDomain("gist.github.com"))

        // Multi-level ccTLDs
        assertEquals("bbc.co.uk", OriginMatcher.extractRegistrableDomain("news.bbc.co.uk"))
        assertEquals("amazon.co.jp", OriginMatcher.extractRegistrableDomain("www.amazon.co.jp"))
        assertEquals("google.co.in", OriginMatcher.extractRegistrableDomain("mail.google.co.in"))

        // Root domains match themselves
        assertEquals("example.org", OriginMatcher.extractRegistrableDomain("example.org"))

        // Localhost and IPs remain unchanged
        assertEquals("localhost", OriginMatcher.extractRegistrableDomain("localhost"))
        assertEquals("127.0.0.1", OriginMatcher.extractRegistrableDomain("127.0.0.1"))
    }

    @Test
    fun testGeckoConfigContainsStorageAccessAndCookiePartitioning() {
        val configYaml = GeckoConfigManager.generatePerformanceConfigYaml()

        // Storage Access API (W3C standard for embedded logins)
        assertTrue(configYaml.contains("dom.storage_access.enabled: true"))
        assertTrue(configYaml.contains("dom.storage_access.forward_declares: true"))
        assertTrue(configYaml.contains("privacy.restrict3rdpartystorage.heuristics: true"))

        // SameSite cookie specifications (RFC 6265bis)
        assertTrue(configYaml.contains("network.cookie.sameSite.laxByDefault: true"))
        assertTrue(configYaml.contains("network.cookie.sameSite.noneRequiresSecure: true"))
        assertTrue(configYaml.contains("network.cookie.sameSite.schemeful: true"))

        // Total Cookie Protection storage isolation
        assertTrue(configYaml.contains("privacy.partition.always_partition_third_party_non_cookie_storage: true"))
        assertTrue(configYaml.contains("privacy.partition.serviceWorkers: true"))
        assertTrue(configYaml.contains("privacy.partition.blobURL: true"))
    }

    @Test
    fun testCookieBehaviorConstantsMapping() {
        assertEquals(5, ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS)
        assertEquals(4, ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
        assertEquals(1, ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)
        assertEquals(0, ContentBlocking.CookieBehavior.ACCEPT_ALL)
        assertEquals(2, ContentBlocking.CookieBehavior.ACCEPT_NONE)
    }

    @Test
    fun testCookieBannerModeConstants() {
        assertEquals(0, ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_DISABLED)
        assertEquals(1, ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT)
        assertEquals(2, ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
    }
}
