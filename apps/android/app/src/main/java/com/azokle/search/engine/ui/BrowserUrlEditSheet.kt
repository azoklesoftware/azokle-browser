package com.azokle.search.engine.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.util.PreferencesManager
import com.azokle.search.discover.SearchRepository
import com.azokle.search.vault.EncryptedVaultManager
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.UrlHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val repository = remember { SearchRepository() }
    val vaultManager = remember { EncryptedVaultManager.getInstance(context) }
    val bookmarks by vaultManager.bookmarksFlow.collectAsState()

    var recentSearches by remember { mutableStateOf(preferences.getRecentSearches()) }

    var selectedEngineId by remember {
        mutableStateOf(initialSearchEngine ?: preferences.getSearchEngine())
    }

    var textValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(0, initialText.length)
            )
        )
    }

    val searchEngineConfig = remember(selectedEngineId) {
        com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS.find { it.id.equals(selectedEngineId, ignoreCase = true) }
            ?: com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS[0]
    }
    val locationCode = remember(preferences) { preferences.getLocation() }
    val safeSearch = remember(preferences) { preferences.getSafeSearch() }

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
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    var showVoiceModal by remember { mutableStateOf(false) }

    var clipboardUrl by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (!clip.isNullOrBlank() && UrlHelper.isDirectUrl(clip) && !clip.equals(initialText.trim(), ignoreCase = true)) {
                clipboardUrl = clip
            }
        } catch (e: Exception) {
            // Ignore
        }
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
            // 1. Top Omnibar Search & Navigation Row (Full Width)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Omnibar Capsule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            elevation = if (colors.isDark) 8.dp else 4.dp,
                            shape = AzokleRadius.md,
                            ambientColor = colors.textPrimary.copy(alpha = if (colors.isDark) 0.08f else 0.04f),
                            spotColor = colors.textPrimary.copy(alpha = if (colors.isDark) 0.12f else 0.08f)
                        )
                        .clip(AzokleRadius.md)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, AzokleRadius.md)
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Engine Selector Icon & Dropdown
                    com.azokle.search.search.SearchEngineIconDropdown(
                        selectedEngineId = selectedEngineId,
                        onEngineSelected = { newEngineId ->
                            preferences.setSearchEngine(newEngineId)
                            selectedEngineId = newEngineId
                            onEngineSelected?.invoke(newEngineId)
                        },
                        iconSize = 20.dp,
                        showDropdownArrow = true,
                        modifier = Modifier.padding(start = 0.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            debounceJob?.cancel()
                            val query = it.text.trim()
                            if (query.length < 2 || UrlHelper.isDirectUrl(query)) {
                                suggestions = emptyList()
                                isLoadingSuggestions = false
                            } else {
                                debounceJob = scope.launch {
                                    delay(140)
                                    isLoadingSuggestions = true
                                    try {
                                        suggestions = repository.getAutosuggestions(query)
                                    } catch (e: Exception) {
                                        suggestions = emptyList()
                                    } finally {
                                        isLoadingSuggestions = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isUrl) KeyboardType.Uri else KeyboardType.Text,
                            imeAction = if (isUrl) ImeAction.Go else ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val target = resolveTarget(textValue.text)
                                preferences.saveRecentSearch(textValue.text.trim())
                                onNavigate(target)
                            },
                            onSearch = {
                                val target = resolveTarget(textValue.text)
                                preferences.saveRecentSearch(textValue.text.trim())
                                onNavigate(target)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (textValue.text.isEmpty()) {
                                Text(
                                    text = "Search ${searchEngineConfig.name} or type URL",
                                    color = colors.textMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clear Text Action (48dp touch delegate)
                        if (textValue.text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 38.dp, minHeight = 44.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
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
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Voice Search Mic Action (matching Home Screen Omnibar)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
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
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Clipboard Link Instant Pill (1-Tap Direct Navigation)
            if (clipboardUrl != null) {
                val clipLink = clipboardUrl!!
                val cleanDomain = try { android.net.Uri.parse(clipLink).host?.removePrefix("www.") ?: clipLink } catch (e: Exception) { clipLink }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .clip(AzokleRadius.full)
                        .background(colors.actionPrimaryBg.copy(alpha = 0.12f))
                        .border(1.dp, colors.actionPrimaryBg.copy(alpha = 0.35f), AzokleRadius.full)
                        .clickable {
                            val target = resolveTarget(clipLink)
                            preferences.saveRecentSearch(clipLink.trim())
                            onNavigate(target)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = null,
                        tint = colors.statusAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Paste & Go: ",
                        color = colors.statusAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cleanDomain,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowOutward,
                        contentDescription = null,
                        tint = colors.statusAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Dynamic Discovery Feed (Suggestions / Recent / Shortcuts)
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
                    // Primary Direct Action Card
                    item {
                        PrimaryDirectActionCard(
                            isUrl = isUrl,
                            text = cleanText,
                            colors = colors,
                            onClick = {
                                val target = resolveTarget(cleanText)
                                preferences.saveRecentSearch(cleanText)
                                onNavigate(target)
                            }
                        )
                    }

                    // Live autosuggestions
                    items(suggestions) { suggestion ->
                        UrlSuggestionCard(
                            icon = Icons.Default.Search,
                            title = suggestion,
                            subtitle = "Web Search Suggestion",
                            colors = colors,
                            categoryBadge = "Search",
                            onClick = {
                                val target = resolveTarget(suggestion)
                                preferences.saveRecentSearch(suggestion)
                                onNavigate(target)
                            },
                            onFill = {
                                textValue = TextFieldValue(suggestion, selection = TextRange(suggestion.length))
                                debounceJob?.cancel()
                                scope.launch {
                                    try {
                                        suggestions = repository.getAutosuggestions(suggestion)
                                    } catch (e: Exception) {
                                        suggestions = emptyList()
                                    }
                                }
                            }
                        )
                    }

                    if (isLoadingSuggestions) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = colors.textPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                } else {
                    // Recent Searches
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECENT SEARCHES",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMuted,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = "Clear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMuted,
                                    modifier = Modifier
                                        .clickable {
                                            preferences.clearRecentSearches()
                                            recentSearches = emptyList()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        items(recentSearches) { recent ->
                            RecentSearchCard(
                                text = recent,
                                colors = colors,
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

                    // Bookmarks Section
                    if (bookmarks.isNotEmpty()) {
                        item {
                            Text(
                                text = "SAVED BOOKMARKS",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
                            )
                        }

                        items(bookmarks.take(4)) { bookmark ->
                            UrlSuggestionCard(
                                icon = Icons.Default.Bookmark,
                                title = bookmark.title,
                                subtitle = bookmark.url,
                                colors = colors,
                                categoryBadge = "Bookmark",
                                onClick = { onNavigate(bookmark.url) }
                            )
                        }
                    }
                }
            }
        }

        // Voice Search Modal
        if (showVoiceModal) {
            com.azokle.search.search.VoiceSearchModal(
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
    }
}


@Composable
private fun PrimaryDirectActionCard(
    isUrl: Boolean,
    text: String,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    val shape = AzokleRadius.lg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.borderLight, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (isUrl) "Navigate to $text" else "Search for $text"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isUrl) colors.statusAccent.copy(alpha = 0.15f) else colors.cardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUrl) Icons.Default.Public else Icons.Default.Search,
                contentDescription = null,
                tint = if (isUrl) colors.statusAccent else colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isUrl) "Visit Website" else "Search Azokle",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUrl) colors.statusAccent else colors.textMuted
            )
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RecentSearchCard(
    text: String,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val shape = AzokleRadius.md

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBg)
            .border(1.dp, colors.border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Recent search: $text"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .sizeIn(minWidth = 36.dp, minHeight = 44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                    onClick = onRemove
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Remove from recent searches"
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
}

@Composable
private fun UrlSuggestionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit,
    onFill: (() -> Unit)? = null,
    categoryBadge: String? = null
) {
    val shape = AzokleRadius.md

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBg)
            .border(1.dp, colors.border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.12f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title, $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!categoryBadge.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surfaceElevated)
                            .border(0.5.dp, colors.border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = categoryBadge,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMuted
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textMuted,
                maxLines = 1
            )
        }

        if (onFill != null) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 36.dp, minHeight = 44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp, color = colors.textSecondary),
                        onClick = onFill
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Fill into search bar"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthWest,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
