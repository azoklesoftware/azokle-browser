package com.azokle.search.engine.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.azokle.search.common.theme.DarkAzokleColors

enum class BrowserErrorCategory(val label: String) {
    NETWORK("NETWORK ERROR"),
    DNS("DNS RESOLUTION FAILED"),
    TIMEOUT("CONNECTION TIMEOUT"),
    REFUSED("CONNECTION REFUSED"),
    SSL("SECURITY CERTIFICATE WARNING"),
    SERVER("SERVER ERROR"),
    GENERIC("PAGE LOAD FAILED")
}

data class BrowserErrorType(
    val category: BrowserErrorCategory,
    val errorCode: String,
    val title: String,
    val subtitle: String,
    val iconVector: ImageVector,
    val accentColor: Color,
    val troubleshootSteps: List<String>,
    val canBypass: Boolean = false
) {
    companion object {
        fun noInternet(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.NETWORK,
            errorCode = "ERR_INTERNET_DISCONNECTED",
            title = "No Internet Connection",
            subtitle = "Azokle could not connect to the network. Your device appears to be offline.",
            iconVector = Icons.Default.WifiOff,
            accentColor = DarkAzokleColors.statusDestructive,
            troubleshootSteps = listOf(
                "Check your Wi-Fi or Mobile Data connection.",
                "Ensure Airplane Mode is turned off.",
                "Verify your router or cellular signal strength."
            )
        )

        fun dnsNotFound(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.DNS,
            errorCode = "ERR_NAME_NOT_RESOLVED",
            title = "Server IP Not Found",
            subtitle = "The domain name could not be resolved. The server might not exist or DNS is unreachable.",
            iconVector = Icons.Default.Dns,
            accentColor = DarkAzokleColors.statusWarning,
            troubleshootSteps = listOf(
                "Check the URL spelling in the address bar.",
                "Switch your DNS provider to Cloudflare (1.1.1.1) or Quad9 in Settings.",
                "The website domain may have expired or changed addresses."
            )
        )

        fun timeout(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.TIMEOUT,
            errorCode = "ERR_CONNECTION_TIMED_OUT",
            title = "Connection Timed Out",
            subtitle = "The server took too long to respond. The website might be experiencing extreme load.",
            iconVector = Icons.Default.Schedule,
            accentColor = DarkAzokleColors.statusWarning,
            troubleshootSteps = listOf(
                "Wait a moment and try refreshing the page.",
                "Check if other websites are loading normally.",
                "The web server may be undergoing temporary maintenance."
            )
        )

        fun connectionRefused(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.REFUSED,
            errorCode = "ERR_CONNECTION_REFUSED",
            title = "Connection Refused",
            subtitle = "The target server actively rejected the connection attempt.",
            iconVector = Icons.Default.Block,
            accentColor = DarkAzokleColors.statusDestructive,
            troubleshootSteps = listOf(
                "The website or port may currently be down.",
                "If using a proxy or VPN, verify its configuration.",
                "Check if firewall rules or regional blocks are active."
            )
        )

        fun sslWarning(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.SSL,
            errorCode = "ERR_CERT_AUTHORITY_INVALID",
            title = "Potential Security Risk Ahead",
            subtitle = "Azokle detected an untrusted or expired TLS/SSL certificate. Attackers could be attempting to intercept your private data.",
            iconVector = Icons.Default.Security,
            accentColor = DarkAzokleColors.statusDestructive,
            troubleshootSteps = listOf(
                "Do not enter passwords or financial information.",
                "Your device date and time clock may be inaccurate.",
                "Public Wi-Fi networks may be intercepting secure traffic."
            ),
            canBypass = true
        )

        fun redirectLoop(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.SERVER,
            errorCode = "ERR_TOO_MANY_REDIRECTS",
            title = "Too Many Redirects",
            subtitle = "The page isn't redirecting properly. Azokle stopped loading this page to prevent an infinite loop.",
            iconVector = Icons.Default.SyncProblem,
            accentColor = DarkAzokleColors.statusWarning,
            troubleshootSteps = listOf(
                "Clear cookies and site data for this domain in Settings.",
                "The website may have a server-side misconfiguration.",
                "Try loading the page in a Private tab."
            )
        )

        fun portBlocked(failedUrl: String) = BrowserErrorType(
            category = BrowserErrorCategory.REFUSED,
            errorCode = "ERR_UNSAFE_PORT",
            title = "Restricted Port Blocked",
            subtitle = "Azokle blocked access to this non-standard port to protect your device and network.",
            iconVector = Icons.Default.Shield,
            accentColor = DarkAzokleColors.statusDestructive,
            troubleshootSteps = listOf(
                "The website requested a port typically restricted for security.",
                "Ensure the URL uses standard HTTP (80) or HTTPS (443) ports.",
                "If accessing a local dev server, verify the port configuration."
            )
        )

        fun serverError(failedUrl: String, statusCode: Int = 500) = BrowserErrorType(
            category = BrowserErrorCategory.SERVER,
            errorCode = "HTTP_ERROR_$statusCode",
            title = "Web Server Error ($statusCode)",
            subtitle = "The remote web server encountered an internal error while processing this request.",
            iconVector = Icons.Default.CloudOff,
            accentColor = DarkAzokleColors.statusWarning,
            troubleshootSteps = listOf(
                "The issue is on the website's server, not your device.",
                "Try reloading the page in a few minutes.",
                "Contact the site administrator if the problem persists."
            )
        )

        fun generic(failedUrl: String, code: String = "ERR_LOAD_FAILED") = BrowserErrorType(
            category = BrowserErrorCategory.GENERIC,
            errorCode = code,
            title = "Webpage Failed to Load",
            subtitle = "An unexpected error prevented Azokle from rendering this webpage.",
            iconVector = Icons.Default.ErrorOutline,
            accentColor = DarkAzokleColors.statusAccent,
            troubleshootSteps = listOf(
                "Verify the web address in the address bar.",
                "Tap 'Try Again' to re-request the page.",
                "Clear browsing cache in Settings if the problem continues."
            )
        )
    }
}

