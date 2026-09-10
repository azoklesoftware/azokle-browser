package com.azokle.search.engine.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserErrorTypeTest {

    @Test
    fun testDnsNotFoundFactory() {
        val error = BrowserErrorType.dnsNotFound("https://invalid-nonexistent-domain.xyz")
        assertEquals(BrowserErrorCategory.DNS, error.category)
        assertEquals("ERR_NAME_NOT_RESOLVED", error.errorCode)
        assertEquals("Server IP Not Found", error.title)
        assertNotNull(error.iconVector)
        assertTrue(error.troubleshootSteps.isNotEmpty())
    }

    @Test
    fun testNoInternetFactory() {
        val error = BrowserErrorType.noInternet("https://example.com")
        assertEquals(BrowserErrorCategory.NETWORK, error.category)
        assertEquals("ERR_INTERNET_DISCONNECTED", error.errorCode)
        assertEquals("No Internet Connection", error.title)
        assertNotNull(error.iconVector)
        assertTrue(error.troubleshootSteps.isNotEmpty())
    }

    @Test
    fun testTimeoutFactory() {
        val error = BrowserErrorType.timeout("https://slow-server.org")
        assertEquals(BrowserErrorCategory.TIMEOUT, error.category)
        assertEquals("ERR_CONNECTION_TIMED_OUT", error.errorCode)
        assertEquals("Connection Timed Out", error.title)
    }

    @Test
    fun testSslWarningFactory() {
        val error = BrowserErrorType.sslWarning("https://expired.badssl.com")
        assertEquals(BrowserErrorCategory.SSL, error.category)
        assertEquals("ERR_CERT_AUTHORITY_INVALID", error.errorCode)
        assertTrue(error.canBypass)
    }

    @Test
    fun testConnectionRefusedFactory() {
        val error = BrowserErrorType.connectionRefused("https://localhost:9999")
        assertEquals(BrowserErrorCategory.REFUSED, error.category)
        assertEquals("ERR_CONNECTION_REFUSED", error.errorCode)
    }

    @Test
    fun testServerErrorFactory() {
        val error = BrowserErrorType.serverError("https://example.com", 502)
        assertEquals(BrowserErrorCategory.SERVER, error.category)
        assertEquals("HTTP_ERROR_502", error.errorCode)
    }
}
