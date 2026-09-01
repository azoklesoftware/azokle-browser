package com.azokle.search.browser.gecko

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
}
