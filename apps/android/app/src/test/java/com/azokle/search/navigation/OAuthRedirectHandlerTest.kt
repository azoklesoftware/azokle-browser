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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthRedirectHandlerTest {

    @Test
    fun testDetectsOAuthCode() {
        assertTrue(OAuthRedirectHandler.isOAuthCallback("myapp://oauth-callback?code=auth_code_123&state=xyz"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("com.auth0.myapp://callback?access_token=token_abc"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("com.example.app:/oauth2redirect?code=456"))
    }

    @Test
    fun testRejectsNonOAuthAppSchemes() {
        // AZK-VULN-13: Messaging, telephony, and social schemes must never be gesture-exempt
        assertFalse(OAuthRedirectHandler.isOAuthCallback("slack://oauth?access_token=token_abc"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("whatsapp://send?code=123"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("tg://resolve?domain=test&code=123"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("tel:1234567890?code=123"))
    }

    @Test
    fun testRejectsInternalWebSchemes() {
        assertFalse(OAuthRedirectHandler.isOAuthCallback("https://azokle.com/login?code=123"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("http://example.com/oauth?code=123"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("about:blank"))
    }

    @Test
    fun testRejectsDangerousSchemes() {
        assertFalse(OAuthRedirectHandler.isOAuthCallback("file:///etc/passwd?code=123"))
        assertFalse(OAuthRedirectHandler.isOAuthCallback("content://media/external?code=123"))
    }
}
