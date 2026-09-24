package com.azokle.search.common.util

import android.content.Context
import android.content.ContextWrapper
import org.junit.Assert.assertFalse
import org.junit.Test

class VoiceSearchHelperTest {

    @Test
    fun testVoiceSearchHelperGracefullyHandlesException() {
        val dummyContext = object : ContextWrapper(null) {
            override fun getPackageManager(): android.content.pm.PackageManager {
                throw RuntimeException("Simulated missing package manager or speech service failure")
            }
        }

        val available = VoiceSearchHelper.isVoiceSearchAvailable(dummyContext)
        assertFalse("VoiceSearchHelper should safely return false when speech services throw or are missing", available)
    }
}

