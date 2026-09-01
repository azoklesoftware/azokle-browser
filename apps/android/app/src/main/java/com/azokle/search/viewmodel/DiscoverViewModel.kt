package com.azokle.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azokle.search.data.news.DiscoverLayoutMode
import com.azokle.search.data.news.DiscoverPreferencesManager
import com.azokle.search.data.news.GoogleNewsUrlDecoder
import com.azokle.search.data.news.NewsArticle
import com.azokle.search.data.news.NewsCategory
import com.azokle.search.data.news.NewsRepository
import com.azokle.search.data.news.NewsResult
import com.azokle.search.ui.components.NEWS_REGIONS
import com.azokle.search.ui.components.NewsRegionEdition
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DiscoverTabSection {
    FEED,
    SAVED_STORIES
}

data class DiscoverUiState(
    val currentSection: DiscoverTabSection = DiscoverTabSection.FEED,
    val selectedCategory: NewsCategory = NewsCategory.TOP_STORIES,
    val newsArticles: List<NewsArticle> = emptyList(),
    val savedArticles: List<NewsArticle> = emptyList(),
    val mutedPublishers: Set<String> = emptySet(),
    val isNewsLoading: Boolean = false,
    val isNewsRefreshing: Boolean = false,
    val isNewsLoadingMore: Boolean = false,
    val isNewsOffline: Boolean = false,
    val newsError: String? = null,
    val layoutMode: DiscoverLayoutMode = DiscoverLayoutMode.DYNAMIC_YOUTUBE,
    val regionCode: String = "US"
)

/**
 * Dedicated, Sovereign ViewModel for Discover Page Subsystem with Endless Infinite Scroll & Custom Preferences.
 */
class DiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val newsRepository = NewsRepository(application)
    private val discoverPreferences = DiscoverPreferencesManager(application)

    private var newsFetchJob: Job? = null
    private var newsLoadMoreJob: Job? = null
    private var paginationCycleIndex = 0

    private val _uiState = MutableStateFlow(
        DiscoverUiState(
            selectedCategory = discoverPreferences.getPreferredCategory(),
            savedArticles = discoverPreferences.getSavedArticles(),
            mutedPublishers = discoverPreferences.getMutedPublishers(),
            layoutMode = discoverPreferences.getLayoutMode(),
            regionCode = discoverPreferences.getRegionEdition()
        )
    )
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadNews(category = _uiState.value.selectedCategory, forceRefresh = false)
    }

    fun selectSection(section: DiscoverTabSection) {
        _uiState.update { it.copy(currentSection = section) }
        if (section == DiscoverTabSection.SAVED_STORIES) {
            refreshSavedArticles()
        }
    }

    fun selectCategory(category: NewsCategory) {
        if (_uiState.value.selectedCategory == category && _uiState.value.newsArticles.isNotEmpty()) return
        discoverPreferences.setPreferredCategory(category)
        paginationCycleIndex = category.ordinal
        _uiState.update { it.copy(selectedCategory = category) }
        loadNews(category = category, forceRefresh = false)
    }

    fun refreshNews() {
        loadNews(category = _uiState.value.selectedCategory, forceRefresh = true)
    }

    fun loadNews(
        category: NewsCategory = _uiState.value.selectedCategory,
        forceRefresh: Boolean = false
    ) {
        newsFetchJob?.cancel()
        newsFetchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isNewsLoading = !forceRefresh && it.newsArticles.isEmpty(),
                    isNewsRefreshing = forceRefresh,
                    newsError = null
                )
            }

            when (val result = newsRepository.getNews(category, forceRefresh)) {
                is NewsResult.Success -> {
                    val filtered = filterMuted(result.articles)
                    _uiState.update {
                        it.copy(
                            newsArticles = filtered,
                            isNewsLoading = false,
                            isNewsRefreshing = false,
                            isNewsOffline = result.isFromCache,
                            newsError = null
                        )
                    }
                }
                is NewsResult.Error -> {
                    val filteredCached = filterMuted(result.cachedArticles)
                    _uiState.update {
                        it.copy(
                            newsArticles = if (filteredCached.isNotEmpty()) filteredCached else it.newsArticles,
                            isNewsLoading = false,
                            isNewsRefreshing = false,
                            isNewsOffline = true,
                            newsError = if (filteredCached.isEmpty() && it.newsArticles.isEmpty()) result.message else null
                        )
                    }
                }
            }
        }
    }

    /**
     * Endless Infinite Scroll Pagination: Cycles continuously across categories and topics.
     */
    fun loadMoreNews() {
        if (_uiState.value.isNewsLoading || _uiState.value.isNewsLoadingMore || _uiState.value.isNewsRefreshing) return
        val allCategories = NewsCategory.values()
        paginationCycleIndex = (paginationCycleIndex + 1) % allCategories.size
        val nextCategory = allCategories[paginationCycleIndex]

        newsLoadMoreJob?.cancel()
        newsLoadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isNewsLoadingMore = true) }
            when (val result = newsRepository.getNews(nextCategory, forceRefresh = false)) {
                is NewsResult.Success -> {
                    val currentList = _uiState.value.newsArticles
                    val currentUrls = currentList.map { it.url }.toSet()
                    val newUnique = filterMuted(result.articles).filter { it.url !in currentUrls }
                    _uiState.update {
                        it.copy(
                            newsArticles = currentList + newUnique,
                            isNewsLoadingMore = false
                        )
                    }
                }
                is NewsResult.Error -> {
                    _uiState.update { it.copy(isNewsLoadingMore = false) }
                }
            }
        }
    }

    // --- Saved Reading List Management ---

    fun toggleSaveArticle(article: NewsArticle) {
        if (discoverPreferences.isArticleSaved(article.id)) {
            discoverPreferences.removeSavedArticle(article.id)
        } else {
            discoverPreferences.saveArticle(article)
        }
        refreshSavedArticles()
    }

    fun isArticleSaved(articleId: String): Boolean {
        return discoverPreferences.isArticleSaved(articleId)
    }

    private fun refreshSavedArticles() {
        _uiState.update {
            it.copy(savedArticles = discoverPreferences.getSavedArticles())
        }
    }

    // --- Muting & Blacklisting ---

    fun mutePublisher(sourceName: String, domain: String) {
        discoverPreferences.mutePublisher(sourceName)
        discoverPreferences.mutePublisher(domain)
        _uiState.update {
            it.copy(
                mutedPublishers = discoverPreferences.getMutedPublishers(),
                newsArticles = filterMuted(it.newsArticles)
            )
        }
    }

    fun unmutePublisher(publisher: String) {
        discoverPreferences.unmutePublisher(publisher)
        _uiState.update {
            it.copy(mutedPublishers = discoverPreferences.getMutedPublishers())
        }
        refreshNews()
    }

    private fun filterMuted(articles: List<NewsArticle>): List<NewsArticle> {
        return articles.filter { !discoverPreferences.isPublisherMuted(it.sourceName, it.sourceDomain) }
    }

    // --- Layout & Region Settings ---

    fun setLayoutMode(mode: DiscoverLayoutMode) {
        discoverPreferences.setLayoutMode(mode)
        _uiState.update { it.copy(layoutMode = mode) }
    }

    fun setNewsRegion(region: NewsRegionEdition) {
        discoverPreferences.setRegionEdition(region.code)
        _uiState.update { it.copy(regionCode = region.code) }
        refreshNews()
    }

    fun clearOfflineCache() {
        discoverPreferences.clearOfflineCache(getApplication())
        refreshNews()
    }

    suspend fun resolveArticleUrl(url: String): String {
        return GoogleNewsUrlDecoder.resolveDirectUrl(url)
    }
}
