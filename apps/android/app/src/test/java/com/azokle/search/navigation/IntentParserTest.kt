package com.azokle.search.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserTest {

    @Test
    fun testValidPackageNames() {
        assertTrue(IntentParser.isValidPackageName("com.azokle.search"))
        assertTrue(IntentParser.isValidPackageName("com.whatsapp"))
        assertTrue(IntentParser.isValidPackageName("org.mozilla.firefox"))
        assertTrue(IntentParser.isValidPackageName("in.org.npci.upiapp"))
    }

    @Test
    fun testInvalidPackageNames() {
        assertFalse(IntentParser.isValidPackageName(null))
        assertFalse(IntentParser.isValidPackageName(""))
        assertFalse(IntentParser.isValidPackageName("no_dots"))
        assertFalse(IntentParser.isValidPackageName("123.numeric.start"))
        assertFalse(IntentParser.isValidPackageName("com.example.app;malicious"))
        assertFalse(IntentParser.isValidPackageName("com.example.app\"quoted\""))
        assertFalse(IntentParser.isValidPackageName("../traversal/pkg"))
    }

    @Test
    fun testSanitizeFallbackUrl() {
        assertEquals("https://azokle.com", IntentParser.sanitizeFallbackUrl("https://azokle.com"))
        assertEquals("http://example.org/path?q=1", IntentParser.sanitizeFallbackUrl("http://example.org/path?q=1"))
        assertNull(IntentParser.sanitizeFallbackUrl("javascript:alert(1)"))
        assertNull(IntentParser.sanitizeFallbackUrl("file:///etc/passwd"))
        assertNull(IntentParser.sanitizeFallbackUrl("content://media/external"))
        assertNull(IntentParser.sanitizeFallbackUrl("data:text/html,hack"))
        assertNull(IntentParser.sanitizeFallbackUrl("about:blank"))
        assertNull(IntentParser.sanitizeFallbackUrl("https://"))
        assertNull(IntentParser.sanitizeFallbackUrl(""))
        assertNull(IntentParser.sanitizeFallbackUrl(null))
    }

    @Test
    fun testParseStandardTelephonyUri() {
        val parsed = IntentParser.parse("tel:+1234567890")
        assertNotNull(parsed)
        assertEquals("android.intent.action.DIAL", parsed?.action)
        assertEquals("tel", parsed?.scheme)
    }

    @Test
    fun testParseStandardMailtoUri() {
        val parsed = IntentParser.parse("mailto:support@azokle.com")
        assertNotNull(parsed)
        assertEquals("android.intent.action.SENDTO", parsed?.action)
        assertEquals("mailto", parsed?.scheme)
    }

    @Test
    fun testParseIntentUriWithFallback() {
        val intentStr = "intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;S.browser_fallback_url=https%3A%2F%2Fzxing.org;end"
        val parsed = IntentParser.parse(intentStr)
        assertNotNull(parsed)
        assertEquals("com.google.zxing.client.android", parsed?.packageName)
        assertEquals("https://zxing.org", parsed?.fallbackUrl)
        assertNull("Component should always be nullified for security", parsed?.componentName)
    }

    @Test
    fun testParseIntentUriWithMaliciousFallback() {
        val intentStr = "intent://test/#Intent;scheme=test;S.browser_fallback_url=javascript%3Aalert(1);end"
        val parsed = IntentParser.parse(intentStr)
        assertNotNull(parsed)
        assertNull("Malicious javascript fallback URL must be stripped", parsed?.fallbackUrl)
    }
}
