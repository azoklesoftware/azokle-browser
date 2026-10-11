/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
