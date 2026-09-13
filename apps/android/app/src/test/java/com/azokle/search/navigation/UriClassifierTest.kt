package com.azokle.search.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UriClassifierTest {

    @Test
    fun testTelephonySchemes() {
        assertEquals(UriClassifier.UriType.TELEPHONY, UriClassifier.classify("tel:+1234567890"))
        assertEquals(UriClassifier.UriType.TELEPHONY, UriClassifier.classify("tel:911"))
        assertEquals(UriClassifier.UriType.TELEPHONY, UriClassifier.classify("telprompt:+1987654321"))
        assertEquals(UriClassifier.UriType.TELEPHONY, UriClassifier.classify("callto:+1234567890"))
        assertTrue(UriClassifier.isExternalNavigation("tel:+1234567890"))
    }

    @Test
    fun testMessagingSchemes() {
        assertEquals(UriClassifier.UriType.MESSAGING, UriClassifier.classify("sms:+1234567890"))
        assertEquals(UriClassifier.UriType.MESSAGING, UriClassifier.classify("sms:+1234567890?body=Hello"))
        assertEquals(UriClassifier.UriType.MESSAGING, UriClassifier.classify("smsto:1234567890"))
        assertEquals(UriClassifier.UriType.MESSAGING, UriClassifier.classify("mms:1234567890"))
        assertEquals(UriClassifier.UriType.MESSAGING, UriClassifier.classify("mmsto:1234567890"))
        assertTrue(UriClassifier.isExternalNavigation("sms:+1234567890"))
    }

    @Test
    fun testEmailSchemes() {
        assertEquals(UriClassifier.UriType.EMAIL, UriClassifier.classify("mailto:support@azokle.com"))
        assertEquals(UriClassifier.UriType.EMAIL, UriClassifier.classify("mailto:test@example.com?subject=Hi"))
        assertTrue(UriClassifier.isExternalNavigation("mailto:support@azokle.com"))
    }

    @Test
    fun testMapsAndMarketSchemes() {
        assertEquals(UriClassifier.UriType.MAPS, UriClassifier.classify("geo:37.7749,-122.4194"))
        assertEquals(UriClassifier.UriType.MAPS, UriClassifier.classify("geo:0,0?q=San+Francisco"))
        assertEquals(UriClassifier.UriType.APP_STORE, UriClassifier.classify("market://details?id=com.azokle.search"))
        assertEquals(UriClassifier.UriType.APP_STORE, UriClassifier.classify("https://play.google.com/store/apps/details?id=com.whatsapp"))
        assertEquals(UriClassifier.UriType.APP_STORE, UriClassifier.classify("http://play.google.com/store/apps/details?id=com.whatsapp"))
        assertEquals(UriClassifier.UriType.APP_STORE, UriClassifier.classify("https://play.app.goo.gl/xyz123"))
        assertEquals(UriClassifier.UriType.APP_STORE, UriClassifier.classify("https://market.android.com/details?id=com.whatsapp"))

        assertTrue(UriClassifier.isExternalNavigation("geo:37.7749,-122.4194"))
        assertTrue(UriClassifier.isExternalNavigation("market://details?id=com.azokle.search"))
        assertTrue(UriClassifier.isExternalNavigation("https://play.google.com/store/apps/details?id=com.whatsapp"))
        assertTrue(UriClassifier.isExternalNavigation("https://play.app.goo.gl/xyz123"))
    }

    @Test
    fun testUpiPaymentSchemes() {
        assertEquals(UriClassifier.UriType.UPI_PAYMENT, UriClassifier.classify("upi://pay?pa=merchant@upi&pn=Merchant&am=500&cu=INR"))
        assertEquals(UriClassifier.UriType.UPI_PAYMENT, UriClassifier.classify("paytmmp://pay?pa=merchant@paytm"))
        assertEquals(UriClassifier.UriType.UPI_PAYMENT, UriClassifier.classify("phonepe://pay?pa=merchant@ybl"))
        assertEquals(UriClassifier.UriType.UPI_PAYMENT, UriClassifier.classify("gpay://upi/pay?pa=merchant@okaxis"))
        assertTrue(UriClassifier.isExternalNavigation("upi://pay?pa=merchant@upi"))
    }

    @Test
    fun testCalendarSchemes() {
        assertEquals(UriClassifier.UriType.CALENDAR, UriClassifier.classify("webcal://example.com/calendar.ics"))
        assertTrue(UriClassifier.isExternalNavigation("webcal://example.com/calendar.ics"))
    }

    @Test
    fun testIntentSchemes() {
        assertEquals(UriClassifier.UriType.INTENT, UriClassifier.classify("intent://scan/#Intent;scheme=zxing;package=com.google.zxing.client.android;end"))
        assertTrue(UriClassifier.isExternalNavigation("intent://scan/#Intent;scheme=zxing;end"))
    }

    @Test
    fun testWebInternalSchemes() {
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("https://azokle.com"))
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("http://example.org"))
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("about:blank"))
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("about:home"))
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("blob:https://azokle.com/uuid"))
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("data:text/html,<h1>Test</h1>"))
        assertEquals(UriClassifier.UriType.WEB_INTERNAL, UriClassifier.classify("javascript:void(0)"))
        assertFalse(UriClassifier.isExternalNavigation("https://azokle.com"))
        assertFalse(UriClassifier.isDangerous("https://azokle.com"))
    }

    @Test
    fun testDangerousInternalSchemes() {
        assertEquals(UriClassifier.UriType.DANGEROUS_INTERNAL, UriClassifier.classify("file:///data/data/com.azokle.search/databases"))
        assertEquals(UriClassifier.UriType.DANGEROUS_INTERNAL, UriClassifier.classify("content://media/external/images"))
        assertEquals(UriClassifier.UriType.DANGEROUS_INTERNAL, UriClassifier.classify("chrome://geckoview/content"))
        assertEquals(UriClassifier.UriType.DANGEROUS_INTERNAL, UriClassifier.classify("resource://android/assets"))
        assertTrue(UriClassifier.isDangerous("file:///etc/passwd"))
        assertTrue(UriClassifier.isDangerous("content://com.azokle.search.fileprovider"))
        assertFalse(UriClassifier.isExternalNavigation("file:///etc/passwd"))
    }

    @Test
    fun testCustomAppSchemes() {
        assertEquals(UriClassifier.UriType.CUSTOM_APP_SCHEME, UriClassifier.classify("whatsapp://send?text=Hello"))
        assertEquals(UriClassifier.UriType.CUSTOM_APP_SCHEME, UriClassifier.classify("tg://resolve?domain=telegram"))
        assertEquals(UriClassifier.UriType.CUSTOM_APP_SCHEME, UriClassifier.classify("slack://open"))
        assertEquals(UriClassifier.UriType.CUSTOM_APP_SCHEME, UriClassifier.classify("zoommtg://zoom.us/join?confno=12345"))
        assertTrue(UriClassifier.isExternalNavigation("whatsapp://send?text=Hello"))
    }

    @Test
    fun testMalformedAndEmptySchemes() {
        assertEquals(UriClassifier.UriType.UNKNOWN, UriClassifier.classify(""))
        assertEquals(UriClassifier.UriType.UNKNOWN, UriClassifier.classify("   "))
        assertEquals(UriClassifier.UriType.UNKNOWN, UriClassifier.classify(null))
        assertEquals(UriClassifier.UriType.UNKNOWN, UriClassifier.classify("just plain search text"))
        assertEquals(UriClassifier.UriType.UNKNOWN, UriClassifier.classify("123badscheme://test"))
        assertFalse(UriClassifier.isExternalNavigation("just plain search text"))
    }
}
