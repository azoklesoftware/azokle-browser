package com.azokle.search.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpiHandlerTest {

    @Test
    fun testValidUpiParsing() {
        val uriStr = "upi://pay?pa=merchant@okhdfcbank&pn=Store%20Name&am=499.00&cu=INR&tn=Order123"
        val parsed = UpiHandler.parseUpiUri(uriStr)

        assertNotNull(parsed)
        assertEquals("merchant@okhdfcbank", parsed?.payeeAddress)
        assertEquals("Store Name", parsed?.payeeName)
        assertEquals(499.00, parsed?.amount)
        assertEquals("INR", parsed?.currency)
        assertEquals("Order123", parsed?.transactionNote)
    }

    @Test
    fun testUpiParsingMissingPayeeAddress() {
        val uriStr = "upi://pay?pn=Store&am=100"
        val parsed = UpiHandler.parseUpiUri(uriStr)
        assertNull(parsed)
    }

    @Test
    fun testUpiParsingInvalidPayeeAddressNoAtSymbol() {
        val uriStr = "upi://pay?pa=invalidpayee&pn=Store&am=100"
        val parsed = UpiHandler.parseUpiUri(uriStr)
        assertNull(parsed)
    }

    @Test
    fun testUpiDefaultCurrency() {
        val uriStr = "upi://pay?pa=test@upi&am=50"
        val parsed = UpiHandler.parseUpiUri(uriStr)
        assertNotNull(parsed)
        assertEquals("INR", parsed?.currency)
    }
}
