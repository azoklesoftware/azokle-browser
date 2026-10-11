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

package com.azokle.search.common.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.azokle.search.R
import kotlin.math.abs

/**
 * Robust, high-speed, high-res Favicon Image Loader:
 *  - Tor Isolation: Displays onion icon.
 *  - Azokle / New Tab: Displays official Azokle Emblem icon.
 *  - Primary: High-speed Google S2 Global Edge CDN (128px PNG) / direct webpage favicon.
 *  - Fallback: Deterministic Domain Monogram Letter or Vector Earth/Language Icon.
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

    // Check if the tab is an internal Azokle tab, home tab, or new blank tab
    val isAzokleOrBlank = remember(url) {
        val u = url.trim().lowercase()
        u.isEmpty() || u == "about:blank" || u == "about:home" ||
        u.startsWith("azokle://") || u.contains("search.azokle.com") || u.contains("azokle.com")
    }

    if (isAzokleOrBlank) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_engine_azokle),
                contentDescription = "Azokle",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
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
        clean.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: ""
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
        if (!faviconUrl.isNullOrBlank() && !faviconUrl.contains("icons.duckduckgo.com")) {
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
            val imageRequest = remember(context, activeFaviconUrl) {
                ImageRequest.Builder(context)
                    .data(activeFaviconUrl)
                    .crossfade(60)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
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
            // Fallback Domain Monogram Letter or Vector Earth Icon
            val cleanLetter = initialLetter.trim()
            if (cleanLetter.isNotEmpty() && cleanLetter.first().isLetterOrDigit()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(monogramBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cleanLetter,
                        fontSize = (size.value * 0.55).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(monogramBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Earth",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.70f)
                    )
                }
            }
        }
    }
}

/**
 * Checks whether null ex or blank.
 */
private fun String?.isNullExOrBlank(): Boolean = this == null || this.isBlank()
