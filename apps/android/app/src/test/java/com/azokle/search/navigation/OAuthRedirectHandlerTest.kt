package com.azokle.search.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthRedirectHandlerTest {

    @Test
    fun testDetectsOAuthCode() {
        assertTrue(OAuthRedirectHandler.isOAuthCallback("myapp://oauth-callback?code=auth_code_123&state=xyz"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("slack://oauth?access_token=token_abc"))
        assertTrue(OAuthRedirectHandler.isOAuthCallback("com.example.app:/oauth2redirect?code=456"))
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
