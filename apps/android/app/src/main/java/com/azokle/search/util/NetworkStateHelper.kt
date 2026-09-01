package com.azokle.search.util

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

    fun isOnline(context: Context): Boolean {
        return getNetworkStatus(context) == DeviceNetworkStatus.ONLINE
    }
}
