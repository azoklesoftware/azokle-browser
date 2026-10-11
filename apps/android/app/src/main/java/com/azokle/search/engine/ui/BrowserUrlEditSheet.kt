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

package com.azokle.search.engine.ui

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.components.FaviconImage
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.DomainAutocompleteHelper
import com.azokle.search.common.util.DomainMatch
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.common.util.UrlHelper
import com.azokle.search.common.util.VoiceSearchHelper
import com.azokle.search.discover.SearchRepository
import com.azokle.search.history.HistoryItem
import com.azokle.search.history.HistoryManager
import com.azokle.search.qr.QrScannerScreen
import com.azokle.search.search.SearchEngineIconDropdown
import com.azokle.search.search.VoiceSearchModal
import com.azokle.search.vault.EncryptedVaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ultra-Minimalist URL & Search Edit Sheet:
 *  - Streamlined top Omnibar input capsule with engine picker, voice search, and QR scanner.
 *  - 1-tap clipboard "Paste & Go" pill.
 *  - Grouped single-line suggestion rows with hairline dividers.
 *  - Minimal, clean Top Sites tiles, Recent Searches, and Saved Bookmarks.
 */
@Composable
fun BrowserUrlEditSheet(
    initialText: String,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit,
    initialSearchEngine: String? = null,
    onEngineSelected: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val preferences = remember { PreferencesManager(context) }
    val isVoiceAvailable = remember(context) { VoiceSearchHelper.isVoiceSearchAvailable(context) }
    val repository = remember { SearchRepository() }
    val vaultManager = remember { EncryptedVaultManager.getInstance(context) }
    val bookmarks by vaultManager.bookmarksFlow.collectAsState()

    var recentSearches by remember { mutableStateOf(preferences.getRecentSearches()) }

    var selectedEngineId by remember {
        mutableStateOf(initialSearchEngine ?: preferences.getSearchEngine())
    }

    val cleanInitialText = remember(initialText) {
        if (initialText.startsWith("about:", ignoreCase = true) ||
            initialText.startsWith("azokle://", ignoreCase = true)) {
            ""
        } else {
            initialText
        }
    }

    var textValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = cleanInitialText,
                selection = TextRange(0, cleanInitialText.length)
            )
        )
    }

    val searchEngineConfig = remember(selectedEngineId) {
        SEARCH_ENGINE_PROVIDERS.find { it.id.equals(selectedEngineId, ignoreCase = true) }
            ?: SEARCH_ENGINE_PROVIDERS[0]
    }
    val locationCode = remember(preferences) { preferences.getLocation() }
    val safeSearch = remember(preferences) { preferences.getSafeSearch() }

    /**
     * Executes the resolve target operation.
     */
    fun resolveTarget(query: String): String {
        return UrlHelper.resolveSearchOrUrl(
            input = query,
            searchEngineQueryUrl = searchEngineConfig.queryUrl,
            locationCode = locationCode,
            safeSearch = safeSearch
        )
    }

    val focusRequester = remember { FocusRequester() }
    val isUrl = remember(textValue.text) { UrlHelper.isDirectUrl(textValue.text) }

    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var historySuggestions by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var topFrecencySites by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    var showVoiceModal by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    var clipboardUrl by remember { mutableStateOf<String?>(null) }

    val domainMatch = remember(textValue.text, historySuggestions, topFrecencySites, bookmarks, recentSearches) {
        val historyHosts = (historySuggestions.map { it.cleanHost } +
                topFrecencySites.map { it.cleanHost } +
                bookmarks.map { it.url }).distinct()
        DomainAutocompleteHelper.findBestMatch(
            input = textValue.text,
            historyDomains = historyHosts,
            recentSearches = recentSearches
        )
    }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        try {
            delay(100)
            focusRequester.requestFocus()
        } catch (_: Exception) {}
        try {
            topFrecencySites = withContext(Dispatchers.IO) {
                HistoryManager.getTopFrecency(5)
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val primaryClip = clipboard.primaryClip
                if (primaryClip != null && primaryClip.itemCount > 0) {
                    val clip = primaryClip.getItemAt(0)?.text?.toString()?.trim()
                    if (!clip.isNullOrBlank() && UrlHelper.isDirectUrl(clip) && !clip.equals(cleanInitialText.trim(), ignoreCase = true)) {
                        clipboardUrl = clip
                    }
                }
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(680.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Omnibar Search & Navigation Capsule (Full Width)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .height(46.dp)
                    .clip(AzokleRadius.lg)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                    .padding(start = 8.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Engine Selector Icon & Dropdown
                SearchEngineIconDropdown(
                    selectedEngineId = selectedEngineId,
                    onEngineSelected = { newEngineId ->
                        preferences.setSearchEngine(newEngineId)
                        selectedEngineId = newEngineId
                        onEngineSelected?.invoke(newEngineId)
                    },
                    iconSize = 18.dp,
                    showDropdownArrow = true,
                    modifier = Modifier.padding(start = 0.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                BasicTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        textValue = newValue
                        debounceJob?.cancel()
                        val query = newValue.text.trim()
                        if (query.isEmpty()) {
                            suggestions = emptyList()
                            historySuggestions = emptyList()
                            isLoadingSuggestions = false
                        } else {
                            debounceJob = scope.launch {
                                val histMatches = withContext(Dispatchers.IO) {
                                    try {
                                        HistoryManager.searchFrecency(query, 3)
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                }
                                historySuggestions = histMatches

                                if (query.length < 2 || UrlHelper.isDirectUrl(query)) {
                                    suggestions = emptyList()
                                    isLoadingSuggestions = false
                                } else {
                                    delay(140)
                                    isLoadingSuggestions = true
                                    try {
                                        suggestions = withContext(Dispatchers.IO) {
                                            repository.getAutosuggestions(query)
                                        }
                                    } catch (_: Exception) {
                                        suggestions = emptyList()
                                    } finally {
                                        isLoadingSuggestions = false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.textPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isUrl) KeyboardType.Uri else KeyboardType.Text,
                        imeAction = if (isUrl || domainMatch != null) ImeAction.Go else ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val target = if (domainMatch != null && !isUrl) domainMatch.fullUrl else resolveTarget(textValue.text)
                            preferences.saveRecentSearch(if (domainMatch != null && !isUrl) domainMatch.matchedDomain else textValue.text.trim())
                            onNavigate(target)
                        },
                        onSearch = {
                            val target = if (domainMatch != null && !isUrl) domainMatch.fullUrl else resolveTarget(textValue.text)
                            preferences.saveRecentSearch(if (domainMatch != null && !isUrl) domainMatch.matchedDomain else textValue.text.trim())
                            onNavigate(target)
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textValue.text.isEmpty()) {
                                Text(
                                    text = "Search or type URL",
                                    color = colors.textMuted,
                                    fontSize = 13.5.sp
                                )
                            } else if (domainMatch != null && domainMatch.inlineSuffix.isNotEmpty() && textValue.selection.collapsed && textValue.selection.end == textValue.text.length) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = textValue.text,
                                        style = TextStyle(
                                            color = Color.Transparent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = domainMatch.inlineSuffix,
                                        style = TextStyle(
                                            color = colors.textMuted.copy(alpha = 0.5f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                            innerTextField()
                        }
                    }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Clear Text Action
                    if (textValue.text.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                                    onClick = {
                                        textValue = TextFieldValue("")
                                        suggestions = emptyList()
                                    }
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Clear input text"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // QR Code Scanner Action
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showQrScanner = true
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Scan QR code"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    if (isVoiceAvailable) {
                        // Voice Search Mic Action
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showVoiceModal = true
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Voice search"
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Clipboard Link Instant Pill
            if (clipboardUrl != null) {
                val clipLink = clipboardUrl!!
                val cleanDomain = try { Uri.parse(clipLink).host?.removePrefix("www.") ?: clipLink } catch (_: Exception) { clipLink }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                        .clip(AzokleRadius.full)
                        .background(colors.surfaceElevated)
                        .border(0.8.dp, colors.borderSubtle, AzokleRadius.full)
                        .clickable {
                            val target = resolveTarget(clipLink)
                            preferences.saveRecentSearch(clipLink.trim())
                            onNavigate(target)
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = colors.statusAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Paste & Go: ",
                        color = colors.statusAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = cleanDomain,
                        color = colors.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowOutward,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Dynamic Discovery Feed (Suggestions / Recent / Bookmarks / Top Sites)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cleanText = textValue.text.trim()

                // When user is typing
                if (cleanText.isNotEmpty()) {
                    // Top Domain Auto-suggestion Card
                    if (domainMatch != null) {
                        item {
                            DomainAutocompleteCard(
                                match = domainMatch,
                                onClick = {
                                    preferences.saveRecentSearch(domainMatch.matchedDomain)
                                    onNavigate(domainMatch.fullUrl)
                                },
                                onFill = {
                                    textValue = TextFieldValue(
                                        text = domainMatch.matchedDomain,
                                        selection = TextRange(domainMatch.matchedDomain.length)
                                    )
                                }
                            )
                        }
                    }

                    // Primary Direct Action Card
                    item {
                        PrimaryDirectActionCard(
                            isUrl = isUrl,
                            text = cleanText,
                            onClick = {
                                val target = resolveTarget(cleanText)
                                preferences.saveRecentSearch(cleanText)
                                onNavigate(target)
                            }
                        )
                    }

                    // Grouped Suggestions Container (History + Live Search Autosuggestions)
                    val allSuggestions = historySuggestions.map { hist ->
                        SuggestionRowData(
                            icon = Icons.Default.History,
                            title = hist.displayTitle,
                            subtitle = hist.cleanHost,
                            target = hist.url,
                            badge = "History"
                        )
                    } + suggestions.map { suggestion ->
                        SuggestionRowData(
                            icon = Icons.Default.Search,
                            title = suggestion,
                            subtitle = null,
                            target = suggestion,
                            badge = null
                        )
                    }

                    if (allSuggestions.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AzokleRadius.lg)
                                    .background(colors.surfaceElevated)
                                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                            ) {
                                allSuggestions.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = colors.borderSubtle,
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 44.dp)
                                        )
                                    }
                                    SuggestionItemRow(
                                        item = item,
                                        onClick = {
                                            val target = resolveTarget(item.target)
                                            preferences.saveRecentSearch(item.title)
                                            onNavigate(target)
                                        },
                                        onFill = {
                                            textValue = TextFieldValue(item.target, selection = TextRange(item.target.length))
                                            debounceJob?.cancel()
                                            debounceJob = scope.launch {
                                                isLoadingSuggestions = true
                                                try {
                                                    suggestions = withContext(Dispatchers.IO) {
                                                        repository.getAutosuggestions(item.target)
                                                    }
                                                } catch (_: Exception) {
                                                    suggestions = emptyList()
                                                } finally {
                                                    isLoadingSuggestions = false
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isLoadingSuggestions) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = colors.textPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                } else {
                    // 1. Top Sites (Compact Tiles Row)
                    if (topFrecencySites.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Top Sites",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 6.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(topFrecencySites) { topSite ->
                                        TopSiteTile(
                                            site = topSite,
                                            onClick = { onNavigate(topSite.url) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Recent Searches (Grouped Container)
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 2.dp, top = 10.dp, bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Recent",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )

                                    Text(
                                        text = "Clear",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted,
                                        modifier = Modifier
                                            .clickable {
                                                preferences.clearRecentSearches()
                                                recentSearches = emptyList()
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AzokleRadius.lg)
                                        .background(colors.surfaceElevated)
                                        .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                                ) {
                                    recentSearches.forEachIndexed { index, recent ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = colors.borderSubtle,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 44.dp)
                                            )
                                        }
                                        RecentSearchItemRow(
                                            text = recent,
                                            onClick = {
                                                val target = resolveTarget(recent)
                                                preferences.saveRecentSearch(recent)
                                                onNavigate(target)
                                            },
                                            onRemove = {
                                                val updated = recentSearches.toMutableList().apply { remove(recent) }
                                                recentSearches = updated
                                                preferences.saveRecentSearches(updated)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Saved Bookmarks (Grouped Container)
                    if (bookmarks.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Bookmarks",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 6.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AzokleRadius.lg)
                                        .background(colors.surfaceElevated)
                                        .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                                ) {
                                    bookmarks.take(4).forEachIndexed { index, bookmark ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = colors.borderSubtle,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 44.dp)
                                            )
                                        }
                                        BookmarkItemRow(
                                            title = bookmark.title,
                                            url = bookmark.url,
                                            onClick = { onNavigate(bookmark.url) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Search Modal
        if (showVoiceModal) {
            VoiceSearchModal(
                onResult = { voiceQuery ->
                    showVoiceModal = false
                    if (voiceQuery.isNotBlank()) {
                        textValue = TextFieldValue(voiceQuery, selection = TextRange(voiceQuery.length))
                        val target = resolveTarget(voiceQuery)
                        preferences.saveRecentSearch(voiceQuery.trim())
                        onNavigate(target)
                    }
                },
                onDismiss = { showVoiceModal = false }
            )
        }

        // QR Code Scanner Modal
        if (showQrScanner) {
            QrScannerScreen(
                onScanResult = { qrResult ->
                    showQrScanner = false
                    if (qrResult.isNotBlank()) {
                        val target = resolveTarget(qrResult)
                        preferences.saveRecentSearch(qrResult.trim())
                        onNavigate(target)
                    }
                },
                onDismiss = { showQrScanner = false }
            )
        }
    }
}

private data class SuggestionRowData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String?,
    val target: String,
    val badge: String?
)

/**
 * Composable that renders the top site tile UI component.
 */
@Composable
private fun TopSiteTile(
    site: HistoryItem,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp, color = colors.textPrimary),
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = site.displayTitle
            }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(AzokleRadius.md)
                .background(colors.surfaceElevated)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.md),
            contentAlignment = Alignment.Center
        ) {
            FaviconImage(
                url = site.url,
                faviconUrl = site.faviconUrl,
                size = 20.dp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = site.cleanHost.ifBlank { site.displayTitle },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Composable that renders the domain autocomplete card UI component.
 */
@Composable
private fun DomainAutocompleteCard(
    match: DomainMatch,
    onClick: () -> Unit,
    onFill: () -> Unit
) {
    val colors = AzokleTheme.colors
    val shape = AzokleRadius.lg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.statusAccent.copy(alpha = 0.4f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.statusAccent.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Navigate to ${match.matchedDomain}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = colors.statusAccent,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = match.typedPrefix,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.statusAccent
                )
                Text(
                    text = match.inlineSuffix,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
            }
            Text(
                text = if (match.isFromHistory) "Visited site" else "Top site",
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 14.dp, color = colors.textSecondary),
                    onClick = onFill
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = "Fill domain",
                tint = colors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.statusAccent,
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * Composable that renders the primary direct action card UI component.
 */
@Composable
private fun PrimaryDirectActionCard(
    isUrl: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val shape = AzokleRadius.lg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(0.8.dp, colors.borderSubtle, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (isUrl) "Navigate to $text" else "Search for $text"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUrl) Icons.Default.Public else Icons.Default.Search,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isUrl) "Visit link" else "Search web",
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Composable that renders the suggestion item row UI component.
 */
@Composable
private fun SuggestionItemRow(
    item: SuggestionRowData,
    onClick: () -> Unit,
    onFill: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!item.badge.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.cardBg)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted
                        )
                    }
                }
            }
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 14.dp, color = colors.textSecondary),
                    onClick = onFill
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Fill suggestion"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Composable that renders the recent search item row UI component.
 */
@Composable
private fun RecentSearchItemRow(
    text: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = AzokleTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 14.dp, color = colors.textSecondary),
                    onClick = onRemove
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Remove search"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Composable that renders the bookmark item row UI component.
 */
@Composable
private fun BookmarkItemRow(
    title: String,
    url: String,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    val host = remember(url) {
        try { Uri.parse(url).host?.removePrefix("www.") ?: url } catch (_: Exception) { url }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(AzokleRadius.sm)
                .background(colors.cardBg)
                .border(0.8.dp, colors.borderSubtle, AzokleRadius.sm),
            contentAlignment = Alignment.Center
        ) {
            FaviconImage(
                url = url,
                size = 14.dp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifBlank { host },
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = host,
                fontSize = 11.sp,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(13.dp)
        )
    }
}
