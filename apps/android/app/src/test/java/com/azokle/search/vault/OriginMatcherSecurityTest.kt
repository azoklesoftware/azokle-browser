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

package com.azokle.search.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression and security validation tests for OriginMatcher (Audit finding H-4).
 * Verifies Public Suffix isolation, userinfo parsing, scheme/port enforcement, and subdomain boundaries.
 */
class OriginMatcherSecurityTest {

    @Test
    fun testMultiPartCctldIsolation_com_tr() {
        val saved = "https://mybank.com.tr"
        val attacker = "https://attacker.com.tr"
        assertFalse("Differing registrable domains under .com.tr must not match", OriginMatcher.matches(saved, attacker))
    }

    @Test
    fun testMultiPartCctldIsolation_co_kr() {
        val saved = "https://x.co.kr"
        val attacker = "https://y.co.kr"
        assertFalse("Differing registrable domains under .co.kr must not match", OriginMatcher.matches(saved, attacker))
    }

    @Test
    fun testSharedHostingIsolation_herokuapp_com() {
        val victim = "https://victim.herokuapp.com"
        val evil = "https://evil.herokuapp.com"
        assertFalse("Shared hosting tenants under .herokuapp.com must never share credentials", OriginMatcher.matches(victim, evil))
    }

    @Test
    fun testSharedHostingIsolation_github_io_and_pages_dev() {
        assertFalse(OriginMatcher.matches("https://alice.github.io", "https://bob.github.io"))
        assertFalse(OriginMatcher.matches("https://site-a.pages.dev", "https://site-b.pages.dev"))
        assertFalse(OriginMatcher.matches("https://app-a.vercel.app", "https://app-b.vercel.app"))
    }

    @Test
    fun testSchemeMismatchDisallowed() {
        val secure = "https://bank.com"
        val insecure = "http://bank.com"
        assertFalse("Credentials must never be offered over plaintext HTTP for HTTPS logins", OriginMatcher.matches(secure, insecure))
    }

    @Test
    fun testPortMismatchDisallowed() {
        val defaultPort = "https://example.com"
        val customPort = "https://example.com:8443"
        assertFalse("Port mismatch must be strictly rejected", OriginMatcher.matches(defaultPort, customPort))
    }

    @Test
    fun testSiblingSubdomainIsolation() {
        val accounts = "https://accounts.example.com"
        val uploads = "https://uploads.example.com"
        assertFalse("Sibling subdomains must not cross-leak credentials", OriginMatcher.matches(accounts, uploads))
    }

    @Test
    fun testUserinfoSpoofingExtraction() {
        val fullAttackerUrl = "https://evil.example/x@bank.com"
        val extracted = OriginMatcher.extractHost(fullAttackerUrl)
        assertEquals("Host must be parsed as the actual authority, not substring after '@'", "evil.example", extracted)
        assertFalse(OriginMatcher.matches("https://bank.com", fullAttackerUrl))
    }

    @Test
    fun testBlankAndWildcardOriginsFailClosed() {
        assertFalse(OriginMatcher.matches("", "https://example.com"))
        assertFalse(OriginMatcher.matches("https://example.com", ""))
        assertFalse(OriginMatcher.matches("   ", "   "))
        assertFalse(OriginMatcher.matches(null, "https://example.com"))
    }

    @Test
    fun testLegitimateParentChildSubdomainMatching() {
        assertTrue("Base domain login should be available to specific login subdomain",
            OriginMatcher.matches("https://example.com", "https://login.example.com"))
        assertTrue("Exact host and scheme match should succeed",
            OriginMatcher.matches("https://login.example.com", "https://login.example.com"))
    }
}
