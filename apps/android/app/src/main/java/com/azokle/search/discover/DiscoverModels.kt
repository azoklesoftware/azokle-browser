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

package com.azokle.search.discover

enum class DiscoverCategory(val label: String) {
    ALL("All"),
    TECH_AI("Tech & AI"),
    MUSIC("Music"),
    SHOPPING("Deals"),
    STREAMING("Streaming & TV"),
    GAMES("Games"),
    SOCIAL("Trending")
}

data class DiscoverItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val domain: String,
    val category: DiscoverCategory,
    val badge: String? = null,
    val monogram: String = title.take(2).uppercase(),
    val primaryColorHex: Long = 0xFF6366F1,
    val secondaryColorHex: Long = 0xFF3B82F6,
    val isFeatured: Boolean = false,
    val actionText: String = "Open",
    val faviconUrl: String? = null
)

object DiscoverCatalog {
    val topPickIds = listOf(
        "chatgpt",
        "spotify",
        "amazon_in",
        "youtube",
        "reddit",
        "poki",
        "wikipedia",
        "mxplayer"
    )

    val sponsoredIds = topPickIds

    val items: List<DiscoverItem> = listOf(
        // --- 1. ChatGPT (First Number #1) ---
        DiscoverItem(
            id = "chatgpt",
            title = "ChatGPT",
            subtitle = "Leading conversational AI for writing, learning & analysis",
            url = "https://chatgpt.com",
            domain = "chatgpt.com",
            category = DiscoverCategory.TECH_AI,
            badge = "AI",
            primaryColorHex = 0xFF10A37F,
            secondaryColorHex = 0xFF1A7F64,
            isFeatured = true,
            actionText = "Chat",
            faviconUrl = "https://www.google.com/s2/favicons?domain=chatgpt.com&sz=128"
        ),

        // --- 2. Spotify (First Number #2) ---
        DiscoverItem(
            id = "spotify",
            title = "Spotify",
            subtitle = "Stream millions of songs, albums & podcasts free on the web",
            url = "https://open.spotify.com",
            domain = "spotify.com",
            category = DiscoverCategory.MUSIC,
            badge = "MUSIC",
            primaryColorHex = 0xFF1DB954,
            secondaryColorHex = 0xFF191414,
            isFeatured = true,
            actionText = "Listen",
            faviconUrl = "https://www.google.com/s2/favicons?domain=spotify.com&sz=128"
        ),

        // --- 3. Amazon.in (First Number #3) ---
        DiscoverItem(
            id = "amazon_in",
            title = "Amazon.in",
            subtitle = "Online shopping for electronics, fashion, mobile phones & essentials",
            url = "https://www.amazon.in",
            domain = "amazon.in",
            category = DiscoverCategory.SHOPPING,
            badge = "TOP",
            primaryColorHex = 0xFFFF9900,
            secondaryColorHex = 0xFF146EB4,
            isFeatured = true,
            actionText = "Shop",
            faviconUrl = "https://www.google.com/s2/favicons?domain=amazon.in&sz=128"
        ),

        // --- 4. YouTube ---
        DiscoverItem(
            id = "youtube",
            title = "YouTube",
            subtitle = "Watch trending videos, music, gaming & global creators",
            url = "https://www.youtube.com",
            domain = "youtube.com",
            category = DiscoverCategory.STREAMING,
            badge = "VIDEO",
            primaryColorHex = 0xFFFF0000,
            secondaryColorHex = 0xFF282828,
            isFeatured = true,
            actionText = "Watch",
            faviconUrl = "https://www.google.com/s2/favicons?domain=youtube.com&sz=128"
        ),

        // --- 5. Reddit ---
        DiscoverItem(
            id = "reddit",
            title = "Reddit",
            subtitle = "The front page of the internet — trending news & discussions",
            url = "https://www.reddit.com/r/popular",
            domain = "reddit.com",
            category = DiscoverCategory.SOCIAL,
            badge = "HOT",
            primaryColorHex = 0xFFFF4500,
            secondaryColorHex = 0xFFFF8717,
            isFeatured = true,
            actionText = "Browse",
            faviconUrl = "https://www.google.com/s2/favicons?domain=reddit.com&sz=128"
        ),

        // --- 6. Poki Games ---
        DiscoverItem(
            id = "poki",
            title = "Poki Games",
            subtitle = "Subway Surfers, Temple Run & 1000+ instant games with 0 install",
            url = "https://poki.com",
            domain = "poki.com",
            category = DiscoverCategory.GAMES,
            badge = "GAMES",
            primaryColorHex = 0xFF00D26A,
            secondaryColorHex = 0xFF00A3FF,
            isFeatured = true,
            actionText = "Play",
            faviconUrl = "https://www.google.com/s2/favicons?domain=poki.com&sz=128"
        ),

        // --- 7. Wikipedia ---
        DiscoverItem(
            id = "wikipedia",
            title = "Wikipedia",
            subtitle = "Free online encyclopedia with millions of articles and knowledge",
            url = "https://en.m.wikipedia.org/wiki/Main_Page",
            domain = "wikipedia.org",
            category = DiscoverCategory.TECH_AI,
            badge = "LEARN",
            primaryColorHex = 0xFF636466,
            secondaryColorHex = 0xFF000000,
            isFeatured = false,
            actionText = "Read",
            faviconUrl = "https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128"
        ),

        // --- 8. MX Player ---
        DiscoverItem(
            id = "mxplayer",
            title = "MX Player",
            subtitle = "Free blockbuster movies, web series, live channels & drama",
            url = "https://www.mxplayer.in",
            domain = "mxplayer.in",
            category = DiscoverCategory.STREAMING,
            badge = "FREE",
            primaryColorHex = 0xFF0072FF,
            secondaryColorHex = 0xFF00C6FF,
            isFeatured = true,
            actionText = "Watch",
            faviconUrl = "https://www.google.com/s2/favicons?domain=mxplayer.in&sz=128"
        ),

        // --- Additional Curated Portals ---
        DiscoverItem(
            id = "crazygames",
            title = "CrazyGames",
            subtitle = "Action, racing, 3D multiplayer & IO games directly in browser",
            url = "https://www.crazygames.com",
            domain = "crazygames.com",
            category = DiscoverCategory.GAMES,
            badge = "HOT",
            primaryColorHex = 0xFF7928CA,
            secondaryColorHex = 0xFFFF0080,
            isFeatured = true,
            actionText = "Play Free",
            faviconUrl = "https://www.google.com/s2/favicons?domain=crazygames.com&sz=128"
        ),
        DiscoverItem(
            id = "flipkart",
            title = "Flipkart",
            subtitle = "Top discounts on electronics, smartphones & apparel",
            url = "https://www.flipkart.com",
            domain = "flipkart.com",
            category = DiscoverCategory.SHOPPING,
            badge = "OFFERS",
            primaryColorHex = 0xFF2874F0,
            secondaryColorHex = 0xFFFFE500,
            isFeatured = false,
            actionText = "Shop",
            faviconUrl = "https://www.google.com/s2/favicons?domain=flipkart.com&sz=128"
        ),
        DiscoverItem(
            id = "minitv",
            title = "Amazon MiniTV",
            subtitle = "Free web series, comedy & short films with no subscription",
            url = "https://www.amazon.in/minitv",
            domain = "amazon.in",
            category = DiscoverCategory.STREAMING,
            badge = "EXCLUSIVE",
            primaryColorHex = 0xFFFF9900,
            secondaryColorHex = 0xFFFF5722,
            isFeatured = true,
            actionText = "Stream",
            faviconUrl = "https://www.google.com/s2/favicons?domain=amazon.in&sz=128"
        ),
        DiscoverItem(
            id = "hotstar",
            title = "Disney+ Hotstar",
            subtitle = "Top entertainment, drama, blockbuster movies & live cricket",
            url = "https://inr.deals/pCe7xY",
            domain = "hotstar.com",
            category = DiscoverCategory.STREAMING,
            badge = "HOTSTAR",
            primaryColorHex = 0xFF14805E,
            secondaryColorHex = 0xFF0C2044,
            isFeatured = false,
            actionText = "Watch",
            faviconUrl = "https://www.google.com/s2/favicons?domain=hotstar.com&sz=128"
        ),
        DiscoverItem(
            id = "jiosaavn",
            title = "JioSaavn",
            subtitle = "Trending Bollywood, international & regional music playlists",
            url = "https://www.jiosaavn.com",
            domain = "jiosaavn.com",
            category = DiscoverCategory.MUSIC,
            badge = "FREE",
            primaryColorHex = 0xFF2BC5B4,
            secondaryColorHex = 0xFF108276,
            isFeatured = false,
            actionText = "Play",
            faviconUrl = "https://www.google.com/s2/favicons?domain=jiosaavn.com&sz=128"
        ),
        DiscoverItem(
            id = "soundcloud",
            title = "SoundCloud",
            subtitle = "Discover underground creators, indie hits, EDM & remixes",
            url = "https://soundcloud.com/discover",
            domain = "soundcloud.com",
            category = DiscoverCategory.MUSIC,
            badge = "INDIE",
            primaryColorHex = 0xFFFF5500,
            secondaryColorHex = 0xFFFF7700,
            isFeatured = false,
            actionText = "Discover",
            faviconUrl = "https://www.google.com/s2/favicons?domain=soundcloud.com&sz=128"
        ),
        DiscoverItem(
            id = "x_explore",
            title = "X / Twitter",
            subtitle = "Real-time breaking trends, sports, technology & global news",
            url = "https://x.com/explore",
            domain = "x.com",
            category = DiscoverCategory.SOCIAL,
            badge = "LIVE",
            primaryColorHex = 0xFF1D9BF0,
            secondaryColorHex = 0xFF0C7ABF,
            isFeatured = false,
            actionText = "Trends",
            faviconUrl = "https://www.google.com/s2/favicons?domain=x.com&sz=128"
        ),
        DiscoverItem(
            id = "pinterest",
            title = "Pinterest",
            subtitle = "Visual inspiration, aesthetic wallpapers, DIY & fashion trends",
            url = "https://www.pinterest.com/today",
            domain = "pinterest.com",
            category = DiscoverCategory.SOCIAL,
            badge = "IDEAS",
            primaryColorHex = 0xFFE60023,
            secondaryColorHex = 0xFFAD081B,
            isFeatured = false,
            actionText = "Explore",
            faviconUrl = "https://www.google.com/s2/favicons?domain=pinterest.com&sz=128"
        ),
        DiscoverItem(
            id = "producthunt",
            title = "Product Hunt",
            subtitle = "The best new technology products, startups & tools every day",
            url = "https://www.producthunt.com",
            domain = "producthunt.com",
            category = DiscoverCategory.TECH_AI,
            badge = "NEW",
            primaryColorHex = 0xFFDA552F,
            secondaryColorHex = 0xFFFF6154,
            isFeatured = false,
            actionText = "Explore",
            faviconUrl = "https://www.google.com/s2/favicons?domain=producthunt.com&sz=128"
        ),
        DiscoverItem(
            id = "github_trending",
            title = "GitHub Trending",
            subtitle = "Discover open source repositories and developer projects",
            url = "https://github.com/trending",
            domain = "github.com",
            category = DiscoverCategory.TECH_AI,
            badge = "CODE",
            primaryColorHex = 0xFF24292E,
            secondaryColorHex = 0xFF6E5494,
            isFeatured = false,
            actionText = "Explore",
            faviconUrl = "https://www.google.com/s2/favicons?domain=github.com&sz=128"
        ),
        DiscoverItem(
            id = "1001games",
            title = "1001 Games",
            subtitle = "Classic arcade, puzzles, bubble shooter & physics games",
            url = "https://www.1001games.com",
            domain = "1001games.com",
            category = DiscoverCategory.GAMES,
            badge = "POPULAR",
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFFEF4444,
            isFeatured = false,
            actionText = "Play",
            faviconUrl = "https://www.google.com/s2/favicons?domain=1001games.com&sz=128"
        ),
        DiscoverItem(
            id = "y8",
            title = "Y8 Games",
            subtitle = "Retro classics, 2-player battles & web action hits",
            url = "https://www.y8.com",
            domain = "y8.com",
            category = DiscoverCategory.GAMES,
            badge = "CLASSIC",
            primaryColorHex = 0xFFEC4899,
            secondaryColorHex = 0xFF8B5CF6,
            isFeatured = false,
            actionText = "Play",
            faviconUrl = "https://www.google.com/s2/favicons?domain=y8.com&sz=128"
        ),
        DiscoverItem(
            id = "chess",
            title = "Chess.com",
            subtitle = "Play live chess online, solve puzzles & master tactics",
            url = "https://www.chess.com/play/online",
            domain = "chess.com",
            category = DiscoverCategory.GAMES,
            badge = "BRAIN",
            primaryColorHex = 0xFF81B64C,
            secondaryColorHex = 0xFF45753C,
            isFeatured = false,
            actionText = "Play",
            faviconUrl = "https://www.google.com/s2/favicons?domain=chess.com&sz=128"
        ),
        DiscoverItem(
            id = "ajio",
            title = "Ajio",
            subtitle = "Trending fashion, apparel & online shopping deals",
            url = "https://inr.deals/TzvsEC",
            domain = "ajio.com",
            category = DiscoverCategory.SHOPPING,
            badge = "DEALS",
            primaryColorHex = 0xFF2C4152,
            secondaryColorHex = 0xFF1C2B36,
            isFeatured = false,
            actionText = "Shop",
            faviconUrl = "https://www.google.com/s2/favicons?domain=ajio.com&sz=128"
        ),
        DiscoverItem(
            id = "aubank",
            title = "AU Small Finance Bank",
            subtitle = "High-interest savings, zero balance & credit cards",
            url = "https://inr.deals/WJ9CES",
            domain = "aubank.in",
            category = DiscoverCategory.SHOPPING,
            badge = "BANK",
            primaryColorHex = 0xFF691F74,
            secondaryColorHex = 0xFF491452,
            isFeatured = false,
            actionText = "Explore",
            faviconUrl = "https://www.google.com/s2/favicons?domain=aubank.in&sz=128"
        ),
        DiscoverItem(
            id = "angelone",
            title = "Angel One",
            subtitle = "Free Demat account, zero brokerage stock trading & IPOs",
            url = "https://inr.deals/uhNR8I",
            domain = "angelone.in",
            category = DiscoverCategory.SHOPPING,
            badge = "INVEST",
            primaryColorHex = 0xFF003777,
            secondaryColorHex = 0xFF0055B8,
            isFeatured = false,
            actionText = "Invest",
            faviconUrl = "https://www.google.com/s2/favicons?domain=angelone.in&sz=128"
        ),
        DiscoverItem(
            id = "bhimupi",
            title = "BHIM UPI",
            subtitle = "Simple, secure & instant digital UPI payments & transfers",
            url = "https://inr.deals/nNSk5C",
            domain = "bhimupi.org.in",
            category = DiscoverCategory.SHOPPING,
            badge = "UPI",
            primaryColorHex = 0xFF00529C,
            secondaryColorHex = 0xFF0073D8,
            isFeatured = false,
            actionText = "Pay",
            faviconUrl = "https://www.google.com/s2/favicons?domain=bhimupi.org.in&sz=128"
        )
    )
}
