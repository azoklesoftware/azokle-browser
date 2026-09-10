package com.azokle.search.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SitePermissionsTest {

    @Test
    fun testSitePermissionTypesExist() {
        val types = SitePermissionType.values()
        assertTrue(types.contains(SitePermissionType.AUTOPLAY))
        assertTrue(types.contains(SitePermissionType.CAMERA))
        assertTrue(types.contains(SitePermissionType.MICROPHONE))
        assertTrue(types.contains(SitePermissionType.GEOLOCATION))
        assertTrue(types.contains(SitePermissionType.NOTIFICATIONS))
    }

    @Test
    fun testSitePermissionStates() {
        assertEquals("ALLOW", SitePermissionState.ALLOW.name)
        assertEquals("BLOCK", SitePermissionState.BLOCK.name)
        assertEquals("ASK", SitePermissionState.ASK.name)
    }

    @Test
    fun testAutoplayRequiredAndroidPermissionsIsEmpty() {
        // Autoplay is a browser content permission and does not require OS-level runtime hardware permissions
        assertTrue(SitePermissionType.AUTOPLAY.requiredAndroidPermissions.isEmpty())
        assertTrue(SitePermissionType.PERSISTENT_STORAGE.requiredAndroidPermissions.isEmpty())
        assertTrue(SitePermissionType.MEDIA_KEY_SYSTEM.requiredAndroidPermissions.isEmpty())
    }

    @Test
    fun testHardwarePermissionsRequireAndroidManifestPermissions() {
        assertFalse(SitePermissionType.CAMERA.requiredAndroidPermissions.isEmpty())
        assertFalse(SitePermissionType.MICROPHONE.requiredAndroidPermissions.isEmpty())
        assertFalse(SitePermissionType.GEOLOCATION.requiredAndroidPermissions.isEmpty())
    }

    @Test
    fun testSitePermissionRequestResolution() {
        var grantCount = 0
        var denyCount = 0

        val request = SitePermissionRequest(
            host = "example.com",
            origin = "https://example.com",
            uri = "https://example.com/video",
            requestedTypes = listOf(SitePermissionType.AUTOPLAY),
            contentCallback = { result ->
                if (result == 1) grantCount++ else denyCount++
            }
        )

        request.grantContent(1)
        assertEquals(1, grantCount)
        assertEquals(0, denyCount)

        // Verify idempotency (AtomicBoolean prevents double completion)
        request.grantContent(1)
        request.reject()
        assertEquals(1, grantCount)
        assertEquals(0, denyCount)
    }
}
