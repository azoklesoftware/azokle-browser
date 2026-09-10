package com.azokle.search.browser.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationSecurityTest {

    @Test
    fun testStripDangerousFlags() {
        val dangerousFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

        val sanitized = NavigationSecurity.sanitizeFlags(dangerousFlags)

        assertEquals(0, sanitized and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        assertEquals(0, sanitized and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        assertEquals(0, sanitized and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        assertEquals(0, sanitized and Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        assertTrue(sanitized and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun testForbiddenActions() {
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.BOOT_COMPLETED"))
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.PACKAGE_ADDED"))
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.SHUTDOWN"))
        assertTrue(NavigationSecurity.isForbiddenAction("android.intent.action.REBOOT"))
        assertFalse(NavigationSecurity.isForbiddenAction(Intent.ACTION_VIEW))
        assertFalse(NavigationSecurity.isForbiddenAction(Intent.ACTION_DIAL))
        assertFalse(NavigationSecurity.isForbiddenAction(Intent.ACTION_SENDTO))
    }

    @Test
    fun testSanitizeReplacesForbiddenActionWithActionView() {
        val sanitized = NavigationSecurity.sanitizeAction("android.intent.action.BOOT_COMPLETED")
        assertEquals(Intent.ACTION_VIEW, sanitized)

        val sanitizedAllowed = NavigationSecurity.sanitizeAction(Intent.ACTION_DIAL)
        assertEquals(Intent.ACTION_DIAL, sanitizedAllowed)
    }

    @Test
    fun testGestureRequirement() {
        assertTrue("User gesture must be allowed", NavigationSecurity.isGestureAllowed(hasUserGesture = true, targetWindow = 1))
        assertTrue("Top-level window without gesture allowed", NavigationSecurity.isGestureAllowed(hasUserGesture = false, targetWindow = 0))
        assertFalse("Subframe/popup without user gesture must be blocked", NavigationSecurity.isGestureAllowed(hasUserGesture = false, targetWindow = 1))
    }

    @Test
    fun testTorIsolationRisk() {
        assertTrue(NavigationSecurity.isTorIsolationRisk(isTor = true))
        assertFalse(NavigationSecurity.isTorIsolationRisk(isTor = false))
    }

    @Test
    fun testForbiddenWebSchemes() {
        assertTrue(NavigationSecurity.isForbiddenWebScheme("file"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("content"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("chrome"))
        assertTrue(NavigationSecurity.isForbiddenWebScheme("resource"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("https"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("http"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("upi"))
        assertFalse(NavigationSecurity.isForbiddenWebScheme("tel"))
    }
}
