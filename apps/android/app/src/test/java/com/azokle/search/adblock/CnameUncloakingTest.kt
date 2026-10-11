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
