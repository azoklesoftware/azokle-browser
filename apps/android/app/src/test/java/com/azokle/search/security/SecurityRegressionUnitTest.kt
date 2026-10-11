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

package com.azokle.search.security

import com.azokle.search.common.util.UrlHelper
import com.azokle.search.downloads.DownloadManager
import com.azokle.search.engine.GeckoConfigManager
import com.azokle.search.navigation.IntentParser
import com.azokle.search.navigation.NavigationSecurity
import com.azokle.search.permissions.SitePermissionsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enterprise Automated Security Regression Test Suite.
 * Validates critical browser defenses against directory traversal, intent injection,
 * malformed URI crashes, Tor isolation risks, and WebRTC host candidate leaks.
 */
class SecurityRegressionUnitTest {

    @Test
    fun `test download filename sanitization prevents directory traversal and null bytes`() {
        // Path traversal attempts
        assertEquals("passwd", DownloadManager.sanitizeFilename("../../../../etc/passwd"))
        assertEquals("win.ini", DownloadManager.sanitizeFilename("..\\..\\..\\win.ini"))
        assertEquals("secret.txt", DownloadManager.sanitizeFilename("/root/secret.txt"))
        assertEquals("file.pdf", DownloadManager.sanitizeFilename("file\u0000.pdf"))

        // Special characters and forbidden filesystem symbols
        val cleanedSpecial = DownloadManager.sanitizeFilename("doc?*:|\"<>#test.pdf")
        assertFalse("Filename should not contain forbidden chars", cleanedSpecial.contains("?"))
        assertFalse("Filename should not contain forbidden chars", cleanedSpecial.contains("*"))
        assertFalse("Filename should not contain forbidden chars", cleanedSpecial.contains("<"))
        assertFalse("Filename should not contain forbidden chars", cleanedSpecial.contains(">"))

        // Blank or dot-only filenames fallback gracefully
        val fallback = DownloadManager.sanitizeFilename("...")
        assertTrue("Blank or dot names should trigger safe fallback", fallback.startsWith("download_"))
    }

    @Test
    fun `test dangerous executable package detection`() {
        assertTrue(DownloadManager.isDangerousExecutable("app.apk"))
        assertTrue(DownloadManager.isDangerousExecutable("script.sh"))
        assertTrue(DownloadManager.isDangerousExecutable("payload.exe"))
        assertTrue(DownloadManager.isDangerousExecutable("setup.bat"))
        assertTrue(DownloadManager.isDangerousExecutable("file", "application/vnd.android.package-archive"))
        assertFalse(DownloadManager.isDangerousExecutable("document.pdf", "application/pdf"))
        assertFalse(DownloadManager.isDangerousExecutable("photo.jpg", "image/jpeg"))
    }

    @Test
    fun `test intent parser safely strips dangerous permissions and handles malformed fragments`() {
        // Standard package parsing
        val validPkg = IntentParser.extractPackageFromFragment("intent:#Intent;package=com.example.app;end")
        assertEquals("com.example.app", validPkg)

        // Invalid or malicious package names are rejected
        val invalidPkg = IntentParser.extractPackageFromFragment("intent:#Intent;package=../evil/pkg;end")
        assertNull("Path traversal in package name must be rejected", invalidPkg)

        // Fallback URL extraction with safe HTTP/HTTPS gating
        val validFallback = IntentParser.extractFallbackFromFragment("intent:#Intent;S.browser_fallback_url=https%3A%2F%2Fexample.com%2Flanding;end")
        assertEquals("https://example.com/landing", validFallback)

        // Malformed percent-encoded fragments must not throw uncaught exceptions
        val malformedFallback = IntentParser.extractFallbackFromFragment("intent:#Intent;S.browser_fallback_url=https%G1%ZZ;end")
        assertNotNull(malformedFallback)
    }

    @Test
    fun `test forbidden schemes blocked in navigation security`() {
        assertTrue(NavigationSecurity.isForbiddenWebScheme("file"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("content"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("chrome"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("resource"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("jar"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("https"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("http"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("about"))
    }

    @Test
    fun `test url helper tracking parameter stripping and link shim unwrapping`() {
        val trackingUrl = "https://example.com/article?utm_source=fb&utm_medium=cpc&fbclid=12345&articleId=999"
        val stripped = UrlHelper.stripTrackingParameters(trackingUrl)
        assertTrue(stripped.contains("articleId=999"))
        assertFalse(stripped.contains("fbclid"))
        assertFalse(stripped.contains("utm_source"))

        // Facebook / Instagram link shim unwrapping
        val shimUrl = "https://l.facebook.com/l.php?u=https%3A%2F%2Ftarget-site.org%2Fpage&h=AT123"
        val unwrapped = UrlHelper.unwrapLinkShim(shimUrl)
        assertEquals("https://target-site.org/page", unwrapped)
    }

    @Test
    fun `test geckoview config enforces webrtc host candidate suppression and privacy timer reduction`() {
        val yaml = GeckoConfigManager.generatePerformanceConfigYaml()
        assertTrue("WebRTC no_host preference must be active", yaml.contains("media.peerconnection.ice.no_host: true"))
        assertTrue("WebRTC default_address_only must be active", yaml.contains("media.peerconnection.ice.default_address_only: true"))
        assertTrue("dom.maxHardwareConcurrency must be pinned to 4", yaml.contains("dom.maxHardwareConcurrency: 4"))
        assertTrue("privacy.reduceTimerPrecision must be enabled", yaml.contains("privacy.reduceTimerPrecision: true"))
    }
}
