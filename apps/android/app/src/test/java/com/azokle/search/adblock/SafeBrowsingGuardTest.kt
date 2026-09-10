package com.azokle.search.adblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SafeBrowsingGuardTest {

    @Before
    fun setUp() {
        SafeBrowsingGuard.clearSessionBypasses()
    }

    @Test
    fun testAssessUrl_safeUrl() {
        val assessment = SafeBrowsingGuard.assessUrl("https://example.com/blog/article")
        assertFalse(assessment.isThreat)
        assertEquals(ThreatType.NONE, assessment.threatType)
    }

    @Test
    fun testAssessUrl_knownPhishingDomain() {
        val url = "https://paypal-security-verification.com/login"
        val assessment = SafeBrowsingGuard.assessUrl(url)
        assertTrue(assessment.isThreat)
        assertEquals(ThreatType.PHISHING, assessment.threatType)
        assertEquals("paypal-security-verification.com", assessment.host)
    }

    @Test
    fun testAssessUrl_knownCryptoDrainer() {
        val url = "https://metamask-wallet-validate.org/connect"
        val assessment = SafeBrowsingGuard.assessUrl(url)
        assertTrue(assessment.isThreat)
        assertEquals(ThreatType.CRYPTO_DRAINER, assessment.threatType)
    }

    @Test
    fun testAssessUrl_knownMalware() {
        val url = "https://android-security-system-update.apk-download.net/app.apk"
        val assessment = SafeBrowsingGuard.assessUrl(url)
        assertTrue(assessment.isThreat)
        assertEquals(ThreatType.MALWARE, assessment.threatType)
    }

    @Test
    fun testAssessUrl_subdomainMatch() {
        val url = "https://sub.appleid-support-recover.com/auth"
        val assessment = SafeBrowsingGuard.assessUrl(url)
        assertTrue(assessment.isThreat)
        assertEquals(ThreatType.PHISHING, assessment.threatType)
    }

    @Test
    fun testAssessUrl_heuristicPatternMatch() {
        val url = "https://legit-looking-site.com/account-verification-login"
        val assessment = SafeBrowsingGuard.assessUrl(url)
        assertTrue(assessment.isThreat)
        assertEquals(ThreatType.PHISHING, assessment.threatType)
    }

    @Test
    fun testBypassThreatForSession() {
        val host = "paypal-security-verification.com"
        val url = "https://$host/login"

        assertTrue(SafeBrowsingGuard.assessUrl(url).isThreat)

        SafeBrowsingGuard.bypassThreatForSession(host)
        assertFalse(SafeBrowsingGuard.assessUrl(url).isThreat)

        SafeBrowsingGuard.clearSessionBypasses()
        assertTrue(SafeBrowsingGuard.assessUrl(url).isThreat)
    }
}
