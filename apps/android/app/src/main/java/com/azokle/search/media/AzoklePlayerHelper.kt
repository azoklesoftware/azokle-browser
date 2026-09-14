package com.azokle.search.media

import java.util.regex.Pattern

enum class AzoklePlayerMode(val id: String, val label: String) {
    ALWAYS("always", "Always in Azokle Player"),
    ASK("ask", "Ask Every Time"),
    DISABLED("disabled", "Disabled (Standard Site)")
}

/**
 * Helper utility for detecting video platform links and constructing
 * zero-cookie, sovereign embed containers.
 */
object AzoklePlayerHelper {

    private val YOUTUBE_WATCH_REGEX = Pattern.compile(
        "^https?://(?:www\\.|m\\.)?youtube\\.com/watch\\?.*v=([a-zA-Z0-9_-]{11})",
        Pattern.CASE_INSENSITIVE
    )

    private val YOUTUBE_SHORT_REGEX = Pattern.compile(
        "^https?://youtu\\.be/([a-zA-Z0-9_-]{11})",
        Pattern.CASE_INSENSITIVE
    )

    private val YOUTUBE_SHORTS_REGEX = Pattern.compile(
        "^https?://(?:www\\.|m\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})",
        Pattern.CASE_INSENSITIVE
    )

    private val YOUTUBE_EMBED_REGEX = Pattern.compile(
        "^https?://(?:www\\.|m\\.)?(?:youtube\\.com|youtube-nocookie\\.com)/embed/([a-zA-Z0-9_-]{11})",
        Pattern.CASE_INSENSITIVE
    )

    private val VIMEO_REGEX = Pattern.compile(
        "^https?://(?:www\\.|player\\.)?vimeo\\.com/(?:video/)?(\\d{6,12})",
        Pattern.CASE_INSENSITIVE
    )

    private val V_PARAM_REGEX = Pattern.compile(
        "[?&]v=([a-zA-Z0-9_-]{11})"
    )

    fun isSovereignEmbedUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("youtube-nocookie.com/embed/", ignoreCase = true) ||
               (url.contains("player.vimeo.com/video/", ignoreCase = true) && url.contains("dnt=1", ignoreCase = true))
    }

    fun isEligibleVideoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return isYouTubeWatchUrl(url) || isVimeoUrl(url)
    }

    fun isYouTubeWatchUrl(url: String): Boolean {
        return YOUTUBE_WATCH_REGEX.matcher(url).find() ||
                YOUTUBE_SHORT_REGEX.matcher(url).find() ||
                YOUTUBE_SHORTS_REGEX.matcher(url).find() ||
                YOUTUBE_EMBED_REGEX.matcher(url).find()
    }

    fun isVimeoUrl(url: String): Boolean {
        return VIMEO_REGEX.matcher(url).find()
    }

    fun extractVideoId(url: String): String? {
        val ytMatcher = YOUTUBE_WATCH_REGEX.matcher(url)
        if (ytMatcher.find()) return ytMatcher.group(1)

        val shortMatcher = YOUTUBE_SHORT_REGEX.matcher(url)
        if (shortMatcher.find()) return shortMatcher.group(1)

        val shortsMatcher = YOUTUBE_SHORTS_REGEX.matcher(url)
        if (shortsMatcher.find()) return shortsMatcher.group(1)

        val embedMatcher = YOUTUBE_EMBED_REGEX.matcher(url)
        if (embedMatcher.find()) return embedMatcher.group(1)

        val vimeoMatcher = VIMEO_REGEX.matcher(url)
        if (vimeoMatcher.find()) return vimeoMatcher.group(1)

        val paramMatcher = V_PARAM_REGEX.matcher(url)
        if (paramMatcher.find()) return paramMatcher.group(1)

        return null
    }

    fun buildSovereignEmbedUrl(url: String): String? {
        val videoId = extractVideoId(url) ?: return null
        return if (isVimeoUrl(url)) {
            "https://player.vimeo.com/video/$videoId?autoplay=1&dnt=1"
        } else {
            "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&rel=0&playsinline=1&enablejsapi=1"
        }
    }

    fun buildPlatformWatchUrl(url: String): String? {
        val videoId = extractVideoId(url) ?: return null
        return if (isVimeoUrl(url)) {
            "https://vimeo.com/$videoId"
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }
    }
}
