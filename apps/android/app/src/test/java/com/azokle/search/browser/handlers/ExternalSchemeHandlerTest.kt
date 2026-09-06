package com.azokle.search.browser.handlers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalSchemeHandlerTest {

    @Test
    fun testIsExternalSchemeForTel() {
        assertTrue(ExternalSchemeHandler.isExternalScheme("tel:+1234567890"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("tel:911"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("TEL:123-456-7890"))
    }

    @Test
    fun testIsExternalSchemeForMailto() {
        assertTrue(ExternalSchemeHandler.isExternalScheme("mailto:support@azokle.com"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("mailto:someone@example.com?subject=Hello"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("MAILTO:admin@example.org"))
    }

    @Test
    fun testIsExternalSchemeForMessaging() {
        assertTrue(ExternalSchemeHandler.isExternalScheme("sms:+1234567890"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("sms:+1234567890?body=Hello"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("smsto:1234567890"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("mms:1234567890"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("mmsto:1234567890"))
    }

    @Test
    fun testIsExternalSchemeForGeoAndMarket() {
        assertTrue(ExternalSchemeHandler.isExternalScheme("geo:37.7749,-122.4194"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("market://details?id=com.azokle.search"))
        assertTrue(ExternalSchemeHandler.isExternalScheme("intent://#Intent;scheme=something;end"))
    }

    @Test
    fun testNonExternalSchemesReturnFalse() {
        assertFalse(ExternalSchemeHandler.isExternalScheme("https://azokle.com"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("http://example.com"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("about:blank"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("about:home"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("javascript:alert(1)"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("file:///etc/passwd"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("content://media/external"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("chrome://settings"))
        assertFalse(ExternalSchemeHandler.isExternalScheme("just some search text"))
    }
}
