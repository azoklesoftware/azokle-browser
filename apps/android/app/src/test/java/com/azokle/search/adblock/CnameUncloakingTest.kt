package com.azokle.search.adblock

import com.azokle.search.core.DnsOverHttpsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CnameUncloakingTest {

    @Test
    fun testIpAndLocalhostAddressesAreNotLookedUpForCname() {
        assertNull(DnsOverHttpsManager.resolveCname("127.0.0.1"))
        assertNull(DnsOverHttpsManager.resolveCname("192.168.1.1"))
        assertNull(DnsOverHttpsManager.resolveCname("localhost"))
        assertNull(DnsOverHttpsManager.resolveCname("router.local"))
        assertNull(DnsOverHttpsManager.resolveCname(""))
    }

    @Test
    fun testCnameCacheClearing() {
        DnsOverHttpsManager.clearCnameCache()
        // Cache clearing must be successful and safe without exceptions
        assertTrue(true)
    }
}
