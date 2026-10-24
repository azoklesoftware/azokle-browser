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

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSecurityTest {

    @Test
    fun testForbiddenWebSchemes() {
        assertTrue(NavigationSecurity.isForbiddenWebScheme("file"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("FILE"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("content"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("chrome"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("resource"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("jar"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("javascript"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("view-source"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("VIEW-SOURCE"))

        assertFalse(NavigationSecurity.isForbiddenWebScheme("https"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("http"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("about"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("intent"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("upi"))
    }

    @Test
    fun testSanitizeFlagsStripsDangerousPermissions() {
        val dangerousFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

        val sanitized = NavigationSecurity.sanitizeFlags(dangerousFlags)

        // Must strip all 4 dangerous bits
        assertEquals(0, sanitized and dangerousFlags)
        // Must ensure FLAG_ACTIVITY_NEW_TASK is present
        assertTrue((sanitized and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun testForbiddenActionsSanitized() {
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.PACKAGE_INSTALL"))
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.BOOT_COMPLETED"))
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.REBOOT"))
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.MASTER_CLEAR"))

        assertEquals(Intent.ACTION_VIEW, NavigationSecurity.sanitizeAction("android.intent.action.REBOOT"))
        assertEquals(Intent.ACTION_VIEW, NavigationSecurity.sanitizeAction("com.malicious.ACTION"))
        assertEquals(Intent.ACTION_VIEW, NavigationSecurity.sanitizeAction(null))
        assertEquals(Intent.ACTION_DIAL, NavigationSecurity.sanitizeAction(Intent.ACTION_DIAL))
    }

    @Test
    fun testTorIsolationCheck() {
        assertTrue(NavigationSecurity.isTorIsolationRisk(isTor = true))
        assertFalse(NavigationSecurity.isTorIsolationRisk(isTor = false))
    }

    @Test
    fun testOAuthRedirectClassification() {
        // Parameter spoofing attempts on generic messaging/social apps must be rejected
        assertFalse(OAuthRedirectHandler.isOAuthCallback("whatsapp://send?phone=+1234567890&code=12345"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("tg://resolve?domain=test&code=12345"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("slack://channel?code=12345"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("zoommtg://zoom.us/join?code=12345"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("arbitraryapp://open?code=12345"))

        // Legitimate reverse-domain or explicit OAuth endpoints must be accepted
        assertTrue(OAuthRedirectHandler.isOAuthCallback("com.example.app:/oauth2callback?code=abc123xyz"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("org.mozilla.firefox:/oauth-callback?code=auth_token"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("customapp:/oauth2callback?code=valid_code"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("customapp:/auth/callback?access_token=bearer_123"))
    }
}
