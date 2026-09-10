package com.azokle.search.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.geckoview.GeckoSession.SelectionActionDelegate

class SelectionDelegateLogicTest {

    @Test
    fun testEditableFlagEvaluation() {
        val flags = SelectionActionDelegate.FLAG_IS_EDITABLE
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0
        val isPassword = (flags and SelectionActionDelegate.FLAG_IS_PASSWORD) != 0
        val isCollapsed = (flags and SelectionActionDelegate.FLAG_IS_COLLAPSED) != 0

        assertTrue(isEditable)
        assertFalse(isPassword)
        assertFalse(isCollapsed)
    }

    @Test
    fun testPasswordFlagEvaluation() {
        val flags = SelectionActionDelegate.FLAG_IS_EDITABLE or SelectionActionDelegate.FLAG_IS_PASSWORD
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0
        val isPassword = (flags and SelectionActionDelegate.FLAG_IS_PASSWORD) != 0

        assertTrue(isEditable)
        assertTrue(isPassword)
    }

    @Test
    fun testCollapsedSelectionInInputField() {
        val flags = SelectionActionDelegate.FLAG_IS_EDITABLE or SelectionActionDelegate.FLAG_IS_COLLAPSED
        val isCollapsed = (flags and SelectionActionDelegate.FLAG_IS_COLLAPSED) != 0
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0
        val text = ""
        val hasSelection = text.isNotEmpty() && !isCollapsed

        assertTrue(isEditable)
        assertTrue(isCollapsed)
        assertFalse(hasSelection)
    }

    @Test
    fun testPasswordSecurityPolicy() {
        val flags = SelectionActionDelegate.FLAG_IS_EDITABLE or SelectionActionDelegate.FLAG_IS_PASSWORD
        val isPassword = (flags and SelectionActionDelegate.FLAG_IS_PASSWORD) != 0
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0

        val text = "secret_password123"
        val canCut = !isPassword && isEditable
        val canCopy = !isPassword
        val canSearch = !isPassword && text.isNotBlank()
        val canShare = !isPassword && text.isNotBlank()
        val canPaste = isEditable
        val canSelectAll = true

        val payload = TextSelectionPayload(
            text = text,
            screenRect = android.graphics.RectF(0f, 0f, 100f, 20f),
            canCopy = canCopy,
            canCut = canCut,
            canPaste = canPaste,
            canSelectAll = canSelectAll,
            isPassword = isPassword,
            isEditable = isEditable,
            canSearch = canSearch,
            canShare = canShare
        )

        assertTrue(payload.isPassword)
        assertFalse(payload.canCopy)
        assertFalse(payload.canCut)
        assertFalse(payload.canSearch)
        assertFalse(payload.canShare)
        assertTrue(payload.canPaste)
        assertTrue(payload.canSelectAll)
    }

    @Test
    fun testCollapsedSelectionPasteAllowed() {
        val flags = SelectionActionDelegate.FLAG_IS_EDITABLE or SelectionActionDelegate.FLAG_IS_COLLAPSED
        val isCollapsed = (flags and SelectionActionDelegate.FLAG_IS_COLLAPSED) != 0
        val isEditable = (flags and SelectionActionDelegate.FLAG_IS_EDITABLE) != 0

        val payload = TextSelectionPayload(
            text = "",
            screenRect = android.graphics.RectF(50f, 100f, 50f, 120f), // width = 0
            canCopy = false,
            canCut = false,
            canPaste = isEditable,
            canSelectAll = isEditable,
            isEditable = isEditable,
            isCollapsed = isCollapsed
        )

        val isVisible = payload.text.isNotBlank() || payload.screenRect.width() > 0 || payload.canPaste || payload.canSelectAll
        assertTrue(isVisible)
        assertTrue(payload.canPaste)
        assertFalse(payload.canCopy)
    }

    @Test
    fun testLinkContextTargetMediaTypes() {
        val imageTarget = com.azokle.search.engine.ui.LinkContextTarget(
            url = "https://example.com",
            mediaType = com.azokle.search.engine.ui.ContextMediaType.IMAGE,
            mediaSrc = "https://example.com/pic.png"
        )
        assertTrue(imageTarget.isMedia)
        assertTrue(imageTarget.isImage)

        val videoTarget = com.azokle.search.engine.ui.LinkContextTarget(
            url = "https://example.com",
            mediaType = com.azokle.search.engine.ui.ContextMediaType.VIDEO,
            mediaSrc = "https://example.com/video.mp4"
        )
        assertTrue(videoTarget.isMedia)
        assertFalse(videoTarget.isImage)
        org.junit.Assert.assertEquals(com.azokle.search.engine.ui.ContextMediaType.VIDEO, videoTarget.mediaType)
    }
}
