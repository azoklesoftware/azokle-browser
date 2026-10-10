package com.azokle.search.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnterprisePolicyManagerTest {

    @Test
    fun testDomainBlocklistMatching() {
        fun matchesBlocklist(host: String, blocklist: Set<String>): Boolean {
            val lower = host.lowercase()
            return blocklist.any { lower == it || lower.endsWith(".$it") }
        }

        val rules = setOf("malicious.com", "gambling.org")

        assertTrue(matchesBlocklist("malicious.com", rules))
        assertTrue(matchesBlocklist("sub.malicious.com", rules))
        assertTrue(matchesBlocklist("deep.sub.gambling.org", rules))
        assertFalse(matchesBlocklist("safe-site.com", rules))
        assertFalse(matchesBlocklist("notmalicious.com", rules))
    }
}
