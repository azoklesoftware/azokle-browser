package com.azokle.search.common.util

/**
 * Result representing an inline domain auto-completion match.
 */
data class DomainMatch(
    val typedPrefix: String,
    val matchedDomain: String,
    val inlineSuffix: String,
    val fullUrl: String,
    val isFromHistory: Boolean = true
)

/**
 * Sovereign, privacy-first domain autocompletion engine for search and URL inputs.
 * Driven strictly by the user's own browsing history, visited websites, saved bookmarks, and searches.
 * No hardcoded telemetry or static domain lists.
 */
object DomainAutocompleteHelper {

    /**
     * Finds the best domain auto-completion for the user's typed input,
     * matching strictly against the user's visited domains, bookmarks, and recent searches.
     *
     * @param input Raw typed input from text field (e.g. "yo", "git", "https://red")
     * @param historyDomains List of domains/URLs from user history and bookmarks (in frecency/priority order)
     * @param recentSearches Optional list of recent search queries from the user
     * @return [DomainMatch] containing matched domain and remaining inline suffix, or null if no match.
     */
    fun findBestMatch(
        input: String,
        historyDomains: List<String> = emptyList(),
        recentSearches: List<String> = emptyList()
    ): DomainMatch? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ") || trimmed.contains("\n")) {
            return null
        }

        // Clean query of protocol and www prefix
        var query = trimmed.lowercase()
        if (query.startsWith("https://")) {
            query = query.removePrefix("https://")
        } else if (query.startsWith("http://")) {
            query = query.removePrefix("http://")
        }
        if (query.startsWith("www.")) {
            query = query.removePrefix("www.")
        }

        if (query.isBlank()) return null

        // 1. Search through user's visited website history and bookmarks
        for (raw in historyDomains) {
            val domain = cleanDomain(raw)
            if (domain.isNotBlank() && domain.startsWith(query) && domain.length > query.length) {
                val suffix = domain.substring(query.length)
                return DomainMatch(
                    typedPrefix = input,
                    matchedDomain = domain,
                    inlineSuffix = suffix,
                    fullUrl = "https://$domain",
                    isFromHistory = true
                )
            }
        }

        // 2. Search through user's recent searches (if the search query looks like a domain or starts with query)
        for (search in recentSearches) {
            val domain = cleanDomain(search)
            if (domain.contains(".") && domain.startsWith(query) && domain.length > query.length) {
                val suffix = domain.substring(query.length)
                return DomainMatch(
                    typedPrefix = input,
                    matchedDomain = domain,
                    inlineSuffix = suffix,
                    fullUrl = "https://$domain",
                    isFromHistory = true
                )
            }
        }

        return null
    }

    /**
     * Finds matching domain suggestions for dropdown/list display based solely on user history.
     */
    fun findMatches(
        input: String,
        historyDomains: List<String> = emptyList(),
        recentSearches: List<String> = emptyList(),
        limit: Int = 3
    ): List<DomainMatch> {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) return emptyList()

        val query = trimmed.lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")

        if (query.isBlank()) return emptyList()

        val results = mutableListOf<DomainMatch>()
        val seenDomains = mutableSetOf<String>()

        val candidateSources = historyDomains + recentSearches

        for (source in candidateSources) {
            val domain = cleanDomain(source)
            if (domain.isNotBlank() && domain.startsWith(query) && !seenDomains.contains(domain)) {
                seenDomains.add(domain)
                val suffix = if (domain.length > query.length) domain.substring(query.length) else ""
                results.add(
                    DomainMatch(
                        typedPrefix = input,
                        matchedDomain = domain,
                        inlineSuffix = suffix,
                        fullUrl = "https://$domain",
                        isFromHistory = true
                    )
                )
                if (results.size >= limit) break
            }
        }

        return results
    }

    /**
     * Extracts a clean host/domain from a raw URL or string.
     */
    fun cleanDomain(raw: String): String {
        var d = raw.lowercase().trim()
        if (d.startsWith("https://")) d = d.removePrefix("https://")
        if (d.startsWith("http://")) d = d.removePrefix("http://")
        if (d.startsWith("www.")) d = d.removePrefix("www.")
        val slashIdx = d.indexOf('/')
        if (slashIdx != -1) {
            d = d.substring(0, slashIdx)
        }
        val colonIdx = d.indexOf(':')
        if (colonIdx != -1) {
            d = d.substring(0, colonIdx)
        }
        val queryIdx = d.indexOf('?')
        if (queryIdx != -1) {
            d = d.substring(0, queryIdx)
        }
        return d
    }
}
