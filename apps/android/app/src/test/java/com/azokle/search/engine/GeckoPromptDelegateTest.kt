package com.azokle.search.engine

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeckoPromptDelegateTest {

    private class MockContext : android.content.ContextWrapper(null)

    @Test
    fun testPopupBlocksAutomatedDriveByPopups() {
        val delegate = GeckoPromptDelegateImpl(
            context = MockContext(),
            hasRecentUserInteraction = { false },
            onPopupRequested = {}
        )

        assertFalse(delegate.shouldAllowPopup("https://malicious-ads.com"))
    }

    @Test
    fun testPopupBlocksForbiddenSchemesEvenWithGesture() {
        val delegate = GeckoPromptDelegateImpl(
            context = MockContext(),
            hasRecentUserInteraction = { true },
            onPopupRequested = {}
        )

        assertFalse(delegate.shouldAllowPopup("javascript:alert(1)"))
        assertFalse(delegate.shouldAllowPopup("chrome://settings"))
        assertFalse(delegate.shouldAllowPopup("resource://gre/modules/Services.jsm"))
    }

    @Test
    fun testPopupAllowsLegitimateUserPopups() {
        val delegate = GeckoPromptDelegateImpl(
            context = MockContext(),
            hasRecentUserInteraction = { true },
            onPopupRequested = {}
        )

        assertTrue(delegate.shouldAllowPopup("https://example.com/login-window"))
        assertTrue(delegate.shouldAllowPopup("https://azokle.com/auth"))
    }

    @Test
    fun testRedirectBlocksForbiddenSchemes() {
        val delegate = GeckoPromptDelegateImpl(context = MockContext())

        assertFalse(delegate.shouldAllowRedirect("javascript:document.cookie"))
        assertFalse(delegate.shouldAllowRedirect("chrome://browser/content"))
    }

    @Test
    fun testRedirectAllowsLegitimateUrls() {
        val delegate = GeckoPromptDelegateImpl(context = MockContext())

        assertTrue(delegate.shouldAllowRedirect("https://azokle.com/dashboard"))
        assertTrue(delegate.shouldAllowRedirect("https://github.com/login/oauth/authorize"))
    }

    @Test
    fun testTorModeSuppressesCredentialsAndFedCm() {
        val delegate = GeckoPromptDelegateImpl(
            context = MockContext(),
            isTor = true
        )

        assertFalse(delegate.isCredentialStoragePermitted())
        assertFalse(delegate.isFedCmPermitted())
        assertFalse(delegate.isWebAuthnRelatedOriginPermitted())
    }

    @Test
    fun testPrivateModeSuppressesCredentialsAndFedCm() {
        val delegate = GeckoPromptDelegateImpl(
            context = MockContext(),
            isPrivate = true
        )

        assertFalse(delegate.isCredentialStoragePermitted())
        assertFalse(delegate.isFedCmPermitted())
        assertFalse(delegate.isWebAuthnRelatedOriginPermitted())
    }

    @Test
    fun testNormalModeAllowsCredentialsAndFedCm() {
        val delegate = GeckoPromptDelegateImpl(
            context = MockContext(),
            isPrivate = false,
            isTor = false
        )

        assertTrue(delegate.isCredentialStoragePermitted())
        assertTrue(delegate.isFedCmPermitted())
        assertTrue(delegate.isWebAuthnRelatedOriginPermitted())
    }

    @Test
    fun testDismissAllPendingDialogsSafeWhenEmpty() {
        val delegate = GeckoPromptDelegateImpl(context = MockContext())
        delegate.dismissAllPendingDialogs()
    }
}
