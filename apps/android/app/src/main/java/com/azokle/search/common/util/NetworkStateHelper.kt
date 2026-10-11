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

package com.azokle.search.common.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class DeviceNetworkStatus {
    ONLINE,
    CAPTIVE_PORTAL,
    CONNECTED_NO_INTERNET,
    DISCONNECTED
}

object NetworkStateHelper {
    /**
     * Inspects Android hardware network capabilities in real time.
     * Accurately determines if the device is truly online, disconnected, or behind a captive portal.
     */
    fun getNetworkStatus(context: Context): DeviceNetworkStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return DeviceNetworkStatus.DISCONNECTED

        val activeNetwork = cm.activeNetwork ?: return DeviceNetworkStatus.DISCONNECTED
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return DeviceNetworkStatus.DISCONNECTED

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val hasCaptivePortal = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

        return when {
            hasCaptivePortal -> DeviceNetworkStatus.CAPTIVE_PORTAL
            hasInternet && isValidated -> DeviceNetworkStatus.ONLINE
            hasInternet && !isValidated -> DeviceNetworkStatus.CONNECTED_NO_INTERNET
            else -> DeviceNetworkStatus.DISCONNECTED
        }
    }

    /**
     * Checks whether online.
     */
    fun isOnline(context: Context): Boolean {
        return getNetworkStatus(context) == DeviceNetworkStatus.ONLINE
    }
}
