package com.azokle.search.browser.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

/**
 * Robust, high-res Favicon Image Loader:
 *  - Primary: Uses tab.faviconUrl if available.
 *  - Secondary: Google S2 Favicon API (64px) -> DuckDuckGo Icon API.
 *  - Fallback: Deterministic Domain Monogram Letter on Vibrant Tinted Circle.
 *  - Tor Isolation: Displays onion icon.
 */
@Composable
fun FaviconImage(
    url: String,
    faviconUrl: String? = null,
    isTor: Boolean = false,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    if (isTor) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_tor),
                contentDescription = "Tor Favicon",
                modifier = Modifier.size(size)
            )
        }
        return
    }

    val context = LocalContext.current
    val host = remember(url) {
        try {
            val parsedHost = Uri.parse(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
            if (parsedHost != null) {
                parsedHost
            } else {
                url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/').substringBefore(':')
            }
        } catch (_: Exception) {
            url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/').substringBefore(':')
        }
    }

    val initialLetter = remember(host) {
        val clean = host.removePrefix("https://").removePrefix("http://").removePrefix("www.").trim()
        clean.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "🌐"
    }

    val monogramBgColor = remember(host) {
        val hash = abs(host.hashCode())
        val hues = listOf(
            Color(0xFF38BDF8), // Cyan
            Color(0xFF34D399), // Emerald
            Color(0xFFF43F5E), // Rose
            Color(0xFFA78BFA), // Lavender
            Color(0xFFFBBF24), // Amber
            Color(0xFFF97316), // Orange
            Color(0xFF06B6D4), // Teal
            Color(0xFFEC4899)  // Pink
        )
        hues[hash % hues.size]
    }

    val primaryFaviconUrl = remember(host, faviconUrl) {
        if (!faviconUrl.isNullExOrBlank()) {
            faviconUrl
        } else if (host.contains(".")) {
            "https://www.google.com/s2/favicons?domain=$host&sz=128"
        } else {
            null
        }
    }

    val fallbackFaviconUrl = remember(host) {
        if (host.contains(".")) {
            "https://www.google.com/s2/favicons?domain=$host&sz=128"
        } else {
            null
        }
    }

    var activeFaviconUrl by remember(primaryFaviconUrl) { mutableStateOf(primaryFaviconUrl) }
    var isLoadFailed by remember(primaryFaviconUrl) { mutableStateOf(primaryFaviconUrl == null) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoadFailed && activeFaviconUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(activeFaviconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                onError = {
                    if (activeFaviconUrl != fallbackFaviconUrl && fallbackFaviconUrl != null) {
                        activeFaviconUrl = fallbackFaviconUrl
                    } else {
                        isLoadFailed = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            // Fallback Domain Monogram Letter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(monogramBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialLetter,
                    fontSize = (size.value * 0.55).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

private fun String?.isNullExOrBlank(): Boolean = this == null || this.isBlank()
