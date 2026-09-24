package com.azokle.search.core

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsOverHttpsManagerTest {

    @Test
    fun testAllProvidersHaveValidBootstrapAddressesExceptSystem() {
        val providers = DnsOverHttpsManager.PROVIDERS

        val cloudflare = providers.find { it.id == "cloudflare" }
        assertNotNull(cloudflare)
        assertEquals("https://cloudflare-dns.com/dns-query", cloudflare?.url)
        assertTrue(cloudflare!!.bootstrapIps.contains("1.1.1.1"))
        assertEquals("1.1.1.1", cloudflare.bootstrapAddress)

        val quad9 = providers.find { it.id == "quad9" }
        assertNotNull(quad9)
        assertEquals("https://dns.quad9.net/dns-query", quad9?.url)
        assertTrue(quad9!!.bootstrapIps.contains("9.9.9.9"))
        assertEquals("9.9.9.9", quad9.bootstrapAddress)

        val adguard = providers.find { it.id == "adguard" }
        assertNotNull(adguard)
        assertEquals("https://dns.adguard-dns.com/dns-query", adguard?.url)
        assertTrue(adguard!!.bootstrapIps.contains("94.140.14.14"))
        assertEquals("94.140.14.14", adguard.bootstrapAddress)

        val system = providers.find { it.id == "system" }
        assertNotNull(system)
        assertEquals("", system?.url)
        assertTrue(system!!.bootstrapIps.isEmpty())
        assertEquals("", system.bootstrapAddress)
    }

    @Test
    fun testGetProviderFallback() {
        val valid = DnsOverHttpsManager.getProvider("quad9")
        assertEquals("quad9", valid.id)

        val unknown = DnsOverHttpsManager.getProvider("non_existent_provider_id")
        assertEquals("cloudflare", unknown.id)
    }

    @Test
    fun testGetDnsForSystemReturnsSystemDns() {
        val dns = DnsOverHttpsManager.getDnsForProvider("system")
        assertEquals(Dns.SYSTEM, dns)
    }

    @Test
    fun testGetDnsForKnownProvidersReturnsNonNullDns() {
        val cfDns = DnsOverHttpsManager.getDnsForProvider("cloudflare")
        assertNotNull(cfDns)

        val quad9Dns = DnsOverHttpsManager.getDnsForProvider("quad9")
        assertNotNull(quad9Dns)

        val adguardDns = DnsOverHttpsManager.getDnsForProvider("adguard")
        assertNotNull(adguardDns)
    }
}
