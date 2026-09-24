package com.azokle.search.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.VoiceSearchHelper
import com.azokle.search.search.SearchCategory

@Composable
fun SuggestionsSheet(
    query: String,
    placeholderText: String,
    suggestions: List<String>,
    recentSearches: List<String>,
    isLoading: Boolean,
    selectedCategory: SearchCategory,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onCategorySelected: (SearchCategory) -> Unit,
    onQrClick: () -> Unit = {},
    onVoiceClick: () -> Unit,
    onClearRecent: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isVoiceAvailable = remember(context) { VoiceSearchHelper.isVoiceSearchAvailable(context) }
    val focusRequester = remember { FocusRequester() }
    val colors = AzokleTheme.colors

    // Handle hardware back press cleanly
    BackHandler {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            // Top Search Input Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Capsule
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(
                            elevation = if (colors.isDark) 8.dp else 4.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = if (colors.isDark) Color(0x33FFFFFF) else Color(0x1A000000),
                            spotColor = if (colors.isDark) Color(0x44FFFFFF) else Color(0x22000000)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.trim().isNotEmpty()) {
                                    onSearch(query.trim())
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholderText,
                                    color = colors.textMuted,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Clear Button (40dp Touch Target)
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                                .clip(CircleShape)
                                .clickable { onQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.textMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // QR Code Scanner Button
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 38.dp, minHeight = 38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onQrClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR code",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (isVoiceAvailable) {
                        // Microphone Button (40dp Touch Target)
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 38.dp, minHeight = 38.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onVoiceClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Search",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            CategoryPills(
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                colors = colors
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Content List (Live Suggestions & Recent Searches)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (query.trim().isNotEmpty()) {
                    // Direct Search Row
                    item {
                        SuggestionRow(
                            icon = Icons.Default.Search,
                            text = "Search for \"${query.trim()}\"",
                            isPrimary = true,
                            colors = colors,
                            onClick = { onSearch(query.trim()) }
                        )
                    }

                    // Live Suggestions
                    items(suggestions) { suggestion ->
                        SuggestionRow(
                            icon = Icons.Default.Search,
                            text = suggestion,
                            isPrimary = false,
                            colors = colors,
                            onClick = { onSearch(suggestion) },
                            onFillQuery = { onQueryChange(suggestion) }
                        )
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = colors.textPrimary,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                } else {
                    // Recent Searches Section
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
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
                                IconButton(
                                    onClick = onClearRecent,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Clear History",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        items(recentSearches) { recent ->
                            SuggestionRow(
                                icon = Icons.Default.History,
                                text = recent,
                                isPrimary = false,
                                colors = colors,
                                onClick = { onSearch(recent) },
                                onFillQuery = { onQueryChange(recent) }
                            )
                        }
                    } else {
                        // Clean Empty State with Suggestions
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Search sovereignly with zero telemetry",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Type any keyword, question, or domain (e.g. github.com)",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPills(
    selectedCategory: SearchCategory,
    onCategorySelected: (SearchCategory) -> Unit,
    colors: com.azokle.search.common.theme.AzokleThemeColors
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SearchCategory.entries) { category ->
            val isSelected = category == selectedCategory
            val shape = RoundedCornerShape(14.dp)

            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) colors.actionPrimaryBg else colors.cardBg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.actionPrimaryBg else colors.border,
                        shape = shape
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.label,
                    color = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    text: String,
    isPrimary: Boolean,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit,
    onFillQuery: (() -> Unit)? = null
) {
    val rowShape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isPrimary) colors.surface else colors.cardBg)
            .border(
                width = 1.dp,
                color = if (isPrimary) colors.borderLight else colors.border,
                shape = rowShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        if (onFillQuery != null) {
            IconButton(
                onClick = onFillQuery,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NorthWest,
                    contentDescription = "Fill Query",
                    tint = colors.textMuted,
                    modifier = Modifier.size(14.dp)
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
