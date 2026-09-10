package com.azokle.search.engine

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebExtensionMessagingContractTest {

    @Test
    fun testPushAppStatusPayloadContract() {
        val payload = JSONObject().apply {
            put("type", "PUSH_APP_STATUS")
            put("isTorEnabled", false)
            put("socksPort", 9050)
            put("theme", "dark")
            val arr = JSONArray()
            arr.put("example.com")
            arr.put("testsite.org")
            put("disabledHosts", arr)
        }

        assertEquals("PUSH_APP_STATUS", payload.getString("type"))
        assertFalse(payload.getBoolean("isTorEnabled"))
        assertEquals(9050, payload.getInt("socksPort"))
        assertEquals("dark", payload.getString("theme"))
        assertEquals(2, payload.getJSONArray("disabledHosts").length())
        assertEquals("example.com", payload.getJSONArray("disabledHosts").getString(0))
    }

    @Test
    fun testPushThemePayloadContract() {
        val payload = JSONObject().apply {
            put("type", "PUSH_THEME")
            put("theme", "light")
        }

        assertEquals("PUSH_THEME", payload.getString("type"))
        assertEquals("light", payload.getString("theme"))
    }

    @Test
    fun testPushTorStatusPayloadContract() {
        val payload = JSONObject().apply {
            put("type", "PUSH_TOR_STATUS")
            put("isTorEnabled", true)
            put("socksPort", 9150)
        }

        assertEquals("PUSH_TOR_STATUS", payload.getString("type"))
        assertTrue(payload.getBoolean("isTorEnabled"))
        assertEquals(9150, payload.getInt("socksPort"))
    }

    @Test
    fun testPushDisabledHostsPayloadContract() {
        val payload = JSONObject().apply {
            put("type", "PUSH_DISABLED_HOSTS")
            val arr = JSONArray()
            arr.put("bank.com")
            put("disabledHosts", arr)
        }

        assertEquals("PUSH_DISABLED_HOSTS", payload.getString("type"))
        assertEquals(1, payload.getJSONArray("disabledHosts").length())
        assertEquals("bank.com", payload.getJSONArray("disabledHosts").getString(0))
    }
}
