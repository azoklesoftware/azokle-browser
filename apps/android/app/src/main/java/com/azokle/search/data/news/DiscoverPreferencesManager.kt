package com.azokle.search.data.news

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

enum class DiscoverLayoutMode {
    DYNAMIC_YOUTUBE,
    COMPACT_EDITORIAL,
    MEDIA_CARDS
}

/**
 * Manages persisted preferences for the Dedicated Discover Subsystem:
 *  - Saved Reading List / Offline Articles
 *  - Muted Publisher Blacklist
 *  - Preferred News Edition / Region
 *  - Preferred Feed Layout Mode
 */
class DiscoverPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("azokle_discover_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVED_ARTICLES = "saved_reading_list_json"
        private const val KEY_MUTED_PUBLISHERS = "muted_publishers_set"
        private const val KEY_DEFAULT_CATEGORY = "default_category_id"
        private const val KEY_LAYOUT_MODE = "feed_layout_mode"
        private const val KEY_REGION_EDITION = "region_edition_code"
    }

    // --- Saved Reading List ---

    fun getSavedArticles(): List<NewsArticle> {
        val jsonStr = prefs.getString(KEY_SAVED_ARTICLES, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<NewsArticle>()
            for (i in 0 until array.length()) {
                val itemObj = array.getJSONObject(i)
                NewsArticle.fromJson(itemObj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveArticle(article: NewsArticle) {
        val current = getSavedArticles().toMutableList()
        if (current.none { it.id == article.id || it.url == article.url }) {
            current.add(0, article)
            persistArticles(current)
        }
    }

    fun removeSavedArticle(articleId: String) {
        val current = getSavedArticles().filter { it.id != articleId }
        persistArticles(current)
    }

    fun isArticleSaved(articleId: String): Boolean {
        return getSavedArticles().any { it.id == articleId }
    }

    private fun persistArticles(articles: List<NewsArticle>) {
        val array = JSONArray()
        articles.take(100).forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SAVED_ARTICLES, array.toString()).apply()
    }

    // --- Muted Publishers Blacklist ---

    fun getMutedPublishers(): Set<String> {
        return prefs.getStringSet(KEY_MUTED_PUBLISHERS, emptySet()) ?: emptySet()
    }

    fun mutePublisher(publisherDomainOrName: String) {
        val current = getMutedPublishers().toMutableSet()
        current.add(publisherDomainOrName.trim().lowercase())
        prefs.edit().putStringSet(KEY_MUTED_PUBLISHERS, current).apply()
    }

    fun unmutePublisher(publisherDomainOrName: String) {
        val current = getMutedPublishers().toMutableSet()
        current.remove(publisherDomainOrName.trim().lowercase())
        prefs.edit().putStringSet(KEY_MUTED_PUBLISHERS, current).apply()
    }

    fun isPublisherMuted(sourceName: String, domain: String): Boolean {
        val muted = getMutedPublishers()
        return muted.contains(sourceName.trim().lowercase()) || muted.contains(domain.trim().lowercase())
    }

    // --- Layout & Region Preferences ---

    fun getLayoutMode(): DiscoverLayoutMode {
        val name = prefs.getString(KEY_LAYOUT_MODE, DiscoverLayoutMode.DYNAMIC_YOUTUBE.name)
        return try {
            DiscoverLayoutMode.valueOf(name ?: DiscoverLayoutMode.DYNAMIC_YOUTUBE.name)
        } catch (e: Exception) {
            DiscoverLayoutMode.DYNAMIC_YOUTUBE
        }
    }

    fun setLayoutMode(mode: DiscoverLayoutMode) {
        prefs.edit().putString(KEY_LAYOUT_MODE, mode.name).apply()
    }

    fun getPreferredCategory(): NewsCategory {
        val id = prefs.getString(KEY_DEFAULT_CATEGORY, NewsCategory.TOP_STORIES.id)
        return NewsCategory.fromId(id)
    }

    fun setPreferredCategory(category: NewsCategory) {
        prefs.edit().putString(KEY_DEFAULT_CATEGORY, category.id).apply()
    }

    fun getRegionEdition(): String {
        return prefs.getString(KEY_REGION_EDITION, "US") ?: "US"
    }

    fun setRegionEdition(code: String) {
        prefs.edit().putString(KEY_REGION_EDITION, code.uppercase()).apply()
    }

    fun clearOfflineCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("news_feed_") && file.name.endsWith(".json")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
