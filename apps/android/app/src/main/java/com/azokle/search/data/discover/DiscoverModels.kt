package com.azokle.search.data.discover

enum class DiscoverCategory(val label: String, val iconEmoji: String) {
    ALL("All", "✨"),
    GAMES("Games", "🎮"),
    STREAMING("Streaming & TV", "🎬"),
    MUSIC("Music", "🎵"),
    SOCIAL("Trending", "🔥"),
    SHOPPING("Deals", "🛍️"),
    TECH_AI("Tech & AI", "🤖")
}

data class DiscoverItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val url: String,
    val domain: String,
    val category: DiscoverCategory,
    val badge: String? = null,
    val iconEmoji: String,
    val monogram: String = title.take(2).uppercase(),
    val primaryColorHex: Long = 0xFF6366F1,
    val secondaryColorHex: Long = 0xFF3B82F6,
    val isFeatured: Boolean = false,
    val actionText: String = "Open",
    val faviconUrl: String? = null
)

object DiscoverCatalog {
    val sponsoredIds = listOf(
        "ajio",
        "aubank",
        "angelone",
        "mxplayer",
        "minitv",
        "hotstar",
        "bhimupi"
    )

    val topPickIds = sponsoredIds.toSet()

    val items: List<DiscoverItem> = listOf(
        // --- 1. Featured Instant Games (High Engagement) ---
        DiscoverItem(
            id = "poki",
            title = "Poki Games",
            subtitle = "Subway Surfers, Temple Run & 1000+ instant games with 0 install",
            url = "https://poki.com",
            domain = "poki.com",
            category = DiscoverCategory.GAMES,
            badge = "PLAY FREE",
            iconEmoji = "🕹️",
            primaryColorHex = 0xFF00D26A,
            secondaryColorHex = 0xFF00A3FF,
            isFeatured = true,
            actionText = "Play Now"
        ),
        DiscoverItem(
            id = "crazygames",
            title = "CrazyGames",
            subtitle = "Action, racing, 3D multiplayer & IO games directly in browser",
            url = "https://www.crazygames.com",
            domain = "crazygames.com",
            category = DiscoverCategory.GAMES,
            badge = "HOT",
            iconEmoji = "⚡",
            primaryColorHex = 0xFF7928CA,
            secondaryColorHex = 0xFFFF0080,
            isFeatured = true,
            actionText = "Play Free"
        ),
        DiscoverItem(
            id = "1001games",
            title = "1001 Games",
            subtitle = "Classic arcade, puzzles, bubble shooter & physics games",
            url = "https://www.1001games.com",
            domain = "1001games.com",
            category = DiscoverCategory.GAMES,
            badge = "POPULAR",
            iconEmoji = "🎯",
            primaryColorHex = 0xFFF59E0B,
            secondaryColorHex = 0xFFEF4444,
            isFeatured = false,
            actionText = "Play"
        ),
        DiscoverItem(
            id = "y8",
            title = "Y8 Games",
            subtitle = "Retro classics, 2-player battles & web action hits",
            url = "https://www.y8.com",
            domain = "y8.com",
            category = DiscoverCategory.GAMES,
            badge = "CLASSIC",
            iconEmoji = "👾",
            primaryColorHex = 0xFFEC4899,
            secondaryColorHex = 0xFF8B5CF6,
            isFeatured = false,
            actionText = "Play"
        ),
        DiscoverItem(
            id = "chess",
            title = "Chess.com",
            subtitle = "Play live chess against millions, solve puzzles & lessons",
            url = "https://www.chess.com/play/online",
            domain = "chess.com",
            category = DiscoverCategory.GAMES,
            badge = "BRAIN",
            iconEmoji = "♟️",
            primaryColorHex = 0xFF81B64C,
            secondaryColorHex = 0xFF45753C,
            isFeatured = false,
            actionText = "Play"
        ),

        // --- 2. Sponsored & Premier Portals ---
        DiscoverItem(
            id = "ajio",
            title = "Ajio",
            subtitle = "Trending fashion, apparel & online shopping deals",
            url = "https://inr.deals/TzvsEC",
            domain = "ajio.com",
            category = DiscoverCategory.SHOPPING,
            badge = "SPONSORED",
            iconEmoji = "🛍️",
            primaryColorHex = 0xFF2C4152,
            secondaryColorHex = 0xFF1C2B36,
            isFeatured = false,
            actionText = "Shop Now",
            faviconUrl = "https://www.google.com/s2/favicons?domain=ajio.com&sz=128"
        ),
        DiscoverItem(
            id = "aubank",
            title = "AU Small Bank",
            subtitle = "High-interest savings, zero balance & credit cards",
            url = "https://inr.deals/WJ9CES",
            domain = "aubank.in",
            category = DiscoverCategory.SHOPPING,
            badge = "SPONSORED",
            iconEmoji = "🏦",
            primaryColorHex = 0xFF691F74,
            secondaryColorHex = 0xFF491452,
            isFeatured = false,
            actionText = "Open Account",
            faviconUrl = "https://www.google.com/s2/favicons?domain=aubank.in&sz=128"
        ),
        DiscoverItem(
            id = "angelone",
            title = "Angel One",
            subtitle = "Free Demat account, zero brokerage stock trading & IPOs",
            url = "https://inr.deals/uhNR8I",
            domain = "angelone.in",
            category = DiscoverCategory.SHOPPING,
            badge = "SPONSORED",
            iconEmoji = "📈",
            primaryColorHex = 0xFF003777,
            secondaryColorHex = 0xFF0055B8,
            isFeatured = false,
            actionText = "Invest Now",
            faviconUrl = "https://www.google.com/s2/favicons?domain=angelone.in&sz=128"
        ),
        DiscoverItem(
            id = "mxplayer",
            title = "MX Player",
            subtitle = "Free blockbuster movies, web series, live channels & drama",
            url = "https://www.mxplayer.in",
            domain = "mxplayer.in",
            category = DiscoverCategory.STREAMING,
            badge = "100% FREE",
            iconEmoji = "🍿",
            primaryColorHex = 0xFF0072FF,
            secondaryColorHex = 0xFF00C6FF,
            isFeatured = true,
            actionText = "Watch Free",
            faviconUrl = "https://www.google.com/s2/favicons?domain=mxplayer.in&sz=128"
        ),
        DiscoverItem(
            id = "minitv",
            title = "Amazon Mini",
            subtitle = "Free web series, romance, comedy & short films with no subscription",
            url = "https://www.amazon.in/minitv",
            domain = "amazon.in",
            category = DiscoverCategory.STREAMING,
            badge = "EXCLUSIVE",
            iconEmoji = "🎬",
            primaryColorHex = 0xFFFF9900,
            secondaryColorHex = 0xFFFF5722,
            isFeatured = true,
            actionText = "Stream Now",
            faviconUrl = "https://www.google.com/s2/favicons?domain=amazon.in&sz=128"
        ),
        DiscoverItem(
            id = "hotstar",
            title = "Disney+ Hotstar",
            subtitle = "Top entertainment, drama, blockbuster movies & live cricket",
            url = "https://inr.deals/pCe7xY",
            domain = "hotstar.com",
            category = DiscoverCategory.STREAMING,
            badge = "SPONSORED",
            iconEmoji = "⭐",
            primaryColorHex = 0xFF14805E,
            secondaryColorHex = 0xFF0C2044,
            isFeatured = false,
            actionText = "Watch Now",
            faviconUrl = "https://www.google.com/s2/favicons?domain=hotstar.com&sz=128"
        ),
        DiscoverItem(
            id = "bhimupi",
            title = "BHIM UPI",
            subtitle = "Simple, secure & instant digital UPI payments & transfers",
            url = "https://inr.deals/nNSk5C",
            domain = "bhimupi.org.in",
            category = DiscoverCategory.SHOPPING,
            badge = "SPONSORED",
            iconEmoji = "⚡",
            primaryColorHex = 0xFF00529C,
            secondaryColorHex = 0xFF0073D8,
            isFeatured = false,
            actionText = "Pay Now",
            faviconUrl = "https://www.google.com/s2/favicons?domain=bhimupi.org.in&sz=128"
        ),

        // --- 3. Music & Audio ---
        DiscoverItem(
            id = "spotify",
            title = "Spotify Web",
            subtitle = "Stream millions of songs, albums & podcasts free on the web",
            url = "https://open.spotify.com",
            domain = "spotify.com",
            category = DiscoverCategory.MUSIC,
            badge = "TOP MUSIC",
            iconEmoji = "🎧",
            primaryColorHex = 0xFF1DB954,
            secondaryColorHex = 0xFF191414,
            isFeatured = true,
            actionText = "Listen"
        ),
        DiscoverItem(
            id = "jiosaavn",
            title = "JioSaavn",
            subtitle = "Trending Bollywood, international & regional music playlists",
            url = "https://www.jiosaavn.com",
            domain = "jiosaavn.com",
            category = DiscoverCategory.MUSIC,
            badge = "FREE",
            iconEmoji = "🎶",
            primaryColorHex = 0xFF2BC5B4,
            secondaryColorHex = 0xFF108276,
            isFeatured = false,
            actionText = "Play"
        ),
        DiscoverItem(
            id = "soundcloud",
            title = "SoundCloud",
            subtitle = "Discover underground creators, indie hits, EDM & remixes",
            url = "https://soundcloud.com/discover",
            domain = "soundcloud.com",
            category = DiscoverCategory.MUSIC,
            badge = "INDIE",
            iconEmoji = "☁️",
            primaryColorHex = 0xFFFF5500,
            secondaryColorHex = 0xFFFF7700,
            isFeatured = false,
            actionText = "Discover"
        ),

        // --- 4. Social & Trending ---
        DiscoverItem(
            id = "reddit",
            title = "Reddit Popular",
            subtitle = "The front page of the internet - trending memes & viral stories",
            url = "https://www.reddit.com/r/popular",
            domain = "reddit.com",
            category = DiscoverCategory.SOCIAL,
            badge = "HOT",
            iconEmoji = "🔥",
            primaryColorHex = 0xFFFF4500,
            secondaryColorHex = 0xFFFF8717,
            isFeatured = true,
            actionText = "Browse"
        ),
        DiscoverItem(
            id = "pinterest",
            title = "Pinterest Today",
            subtitle = "Visual inspiration, aesthetic wallpapers, DIY & fashion trends",
            url = "https://www.pinterest.com/today",
            domain = "pinterest.com",
            category = DiscoverCategory.SOCIAL,
            badge = "IDEAS",
            iconEmoji = "📌",
            primaryColorHex = 0xFFE60023,
            secondaryColorHex = 0xFFAD081B,
            isFeatured = false,
            actionText = "Explore"
        ),
        DiscoverItem(
            id = "x_explore",
            title = "X / Twitter Trends",
            subtitle = "Real-time breaking trends, sports, technology & global news",
            url = "https://x.com/explore",
            domain = "x.com",
            category = DiscoverCategory.SOCIAL,
            badge = "LIVE",
            iconEmoji = "🌐",
            primaryColorHex = 0xFF1D9BF0,
            secondaryColorHex = 0xFF0C7ABF,
            isFeatured = false,
            actionText = "View Trends"
        ),

        // --- 5. Shopping Deals ---
        DiscoverItem(
            id = "amazon_deals",
            title = "Amazon Lightning Deals",
            subtitle = "Today's best discounts, lightning sales & gadget deals",
            url = "https://www.amazon.in/deals",
            domain = "amazon.in",
            category = DiscoverCategory.SHOPPING,
            badge = "DEALS",
            iconEmoji = "🏷️",
            primaryColorHex = 0xFFFF9900,
            secondaryColorHex = 0xFF146EB4,
            isFeatured = false,
            actionText = "Shop Deals"
        ),
        DiscoverItem(
            id = "flipkart",
            title = "Flipkart",
            subtitle = "Top discounts on electronics, smartphones & apparel",
            url = "https://www.flipkart.com",
            domain = "flipkart.com",
            category = DiscoverCategory.SHOPPING,
            badge = "OFFERS",
            iconEmoji = "🛒",
            primaryColorHex = 0xFF2874F0,
            secondaryColorHex = 0xFFFFE500,
            isFeatured = false,
            actionText = "Shop"
        ),

        // --- 6. AI & Tech ---
        DiscoverItem(
            id = "chatgpt",
            title = "ChatGPT",
            subtitle = "Leading conversational AI for writing, learning & brainstorming",
            url = "https://chatgpt.com",
            domain = "chatgpt.com",
            category = DiscoverCategory.TECH_AI,
            badge = "AI",
            iconEmoji = "✨",
            primaryColorHex = 0xFF10A37F,
            secondaryColorHex = 0xFF1A7F64,
            isFeatured = true,
            actionText = "Chat"
        ),
        DiscoverItem(
            id = "producthunt",
            title = "Product Hunt",
            subtitle = "The best new technology products, startups & tools every day",
            url = "https://www.producthunt.com",
            domain = "producthunt.com",
            category = DiscoverCategory.TECH_AI,
            badge = "NEW",
            iconEmoji = "🚀",
            primaryColorHex = 0xFFDA552F,
            secondaryColorHex = 0xFFFF6154,
            isFeatured = false,
            actionText = "Explore"
        ),
        DiscoverItem(
            id = "github_trending",
            title = "GitHub Trending",
            subtitle = "Discover what the open source developer community is building today",
            url = "https://github.com/trending",
            domain = "github.com",
            category = DiscoverCategory.TECH_AI,
            badge = "CODE",
            iconEmoji = "💻",
            primaryColorHex = 0xFF24292E,
            secondaryColorHex = 0xFF6E5494,
            isFeatured = false,
            actionText = "Explore"
        ),
        DiscoverItem(
            id = "wikipedia",
            title = "Wikipedia Featured",
            subtitle = "Daily intriguing facts, featured history & encyclopedia articles",
            url = "https://en.m.wikipedia.org/wiki/Main_Page",
            domain = "wikipedia.org",
            category = DiscoverCategory.TECH_AI,
            badge = "LEARN",
            iconEmoji = "📖",
            primaryColorHex = 0xFF636466,
            secondaryColorHex = 0xFF000000,
            isFeatured = false,
            actionText = "Read",
            faviconUrl = "https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128"
        )
    )
}
