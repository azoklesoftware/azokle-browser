package com.azokle.search.qr

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeImageAnalyzerTest {

    @Test
    fun testAnalyzerInitialization() {
        var scannedText: String? = null
        val analyzer = QrCodeImageAnalyzer { result ->
            scannedText = result
        }

        assertNotNull(analyzer)
        analyzer.setScanningEnabled(false)
        analyzer.setScanningEnabled(true)
        assertNull(scannedText)
    }
}
