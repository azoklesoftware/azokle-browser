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
}
