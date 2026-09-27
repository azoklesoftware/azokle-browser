package com.azokle.search.adblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class FilterUpdateManagerTest {

    @Test
    fun testInitialUpdateStates() {
        assertFalse(FilterUpdateManager.isUpdateInProgress.value)
        assertNotNull(FilterUpdateManager.lastUpdateTimestamp.value)
    }
}
