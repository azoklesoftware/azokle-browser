package com.azokle.search.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordVaultManagerTest {

    @Test
    fun testSavedLoginModelStructure() {
        val login = SavedLogin(
            origin = "https://github.com",
            username = "octocat",
            password = "supersecretpassword123"
        )
        assertEquals("https://github.com", login.origin)
        assertEquals("octocat", login.username)
        assertEquals("supersecretpassword123", login.password)
        assertTrue(login.id.isNotBlank())
        assertTrue(login.dateModified > 0)
    }

    @Test
    fun testOriginMatcherHostExtraction() {
        assertEquals("github.com", OriginMatcher.extractHost("https://github.com"))
        assertEquals("github.com", OriginMatcher.extractHost("https://www.github.com/login?ref=1#hash"))
        assertEquals("login.github.com", OriginMatcher.extractHost("http://login.github.com:443/session"))
        assertEquals("bbc.co.uk", OriginMatcher.extractHost("https://www.bbc.co.uk/news"))
        assertEquals("127.0.0.1", OriginMatcher.extractHost("http://127.0.0.1:8080/admin"))
    }

    @Test
    fun testOriginMatcherRegistrableDomain() {
        assertEquals("github.com", OriginMatcher.extractRegistrableDomain("login.github.com"))
        assertEquals("github.com", OriginMatcher.extractRegistrableDomain("github.com"))
        assertEquals("bbc.co.uk", OriginMatcher.extractRegistrableDomain("news.bbc.co.uk"))
        assertEquals("google.co.in", OriginMatcher.extractRegistrableDomain("accounts.google.co.in"))
        assertEquals("localhost", OriginMatcher.extractRegistrableDomain("localhost"))
        assertEquals("192.168.1.1", OriginMatcher.extractRegistrableDomain("192.168.1.1"))
    }

    @Test
    fun testOriginMatchingStrictIsolation() {
        // Legitimate origin matches
        assertTrue(OriginMatcher.matches("https://github.com", "github.com"))
        assertTrue(OriginMatcher.matches("github.com", "https://github.com/login"))
        assertTrue(OriginMatcher.matches("login.github.com", "https://github.com"))
        assertTrue(OriginMatcher.matches("https://accounts.google.co.in", "google.co.in"))

        // Hostile / Phishing attacks must strictly NOT match (CWE-346 prevention)
        assertFalse("Attacker prefix must not match", OriginMatcher.matches("evil-github.com", "github.com"))
        assertFalse("Attacker subdomain must not match", OriginMatcher.matches("github.com.attacker.com", "github.com"))
        assertFalse("Unrelated bank must not match", OriginMatcher.matches("mybank.com", "bank.com"))
        assertFalse("Different TLD must not match", OriginMatcher.matches("https://gitlab.com", "github.com"))
        assertFalse("Phishing TLD must not match", OriginMatcher.matches("paypal.com-auth.net", "paypal.com"))
    }

    @Test
    fun testGenerateStrongPasswordEntropyAndComposition() {
        val pw = PasswordVaultManager.generateStrongPassword(20, true)
        assertEquals(20, pw.length)

        val pwShort = PasswordVaultManager.generateStrongPassword(4, true)
        assertEquals(8, pwShort.length) // coerced to min 8

        // Verify composition: contains upper, lower, digit, symbol
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#$%^&*()-_=+[]{}?"

        val pwComplex = PasswordVaultManager.generateStrongPassword(16, true)
        assertTrue("Must contain uppercase", pwComplex.any { upper.contains(it) })
        assertTrue("Must contain lowercase", pwComplex.any { lower.contains(it) })
        assertTrue("Must contain digit", pwComplex.any { digits.contains(it) })
        assertTrue("Must contain symbol", pwComplex.any { symbols.contains(it) })
    }
}
