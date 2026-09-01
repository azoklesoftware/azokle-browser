package com.azokle.search.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.ui.components.InfoDialog
import com.azokle.search.ui.components.ProUpgradeDialog
import com.azokle.search.ui.components.SearchBarCapsule
import com.azokle.search.ui.components.SettingsSheet
import com.azokle.search.ui.components.SuggestionsSheet
import com.azokle.search.ui.components.VoiceSearchModal
import com.azokle.search.ui.theme.AudiowideFontFamily
import com.azokle.search.ui.theme.AzokleBorderLight
import com.azokle.search.ui.theme.AzokleGlassBg
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.AzokleWhite
import com.azokle.search.ui.theme.WindowHeightSize
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.ui.theme.rememberWindowSize
import com.azokle.search.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVoiceSearchRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    var showVoiceModal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. High-Resolution Wallpaper Backdrop
        Image(
            painter = painterResource(id = R.drawable.bg_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar Scrim (Guarantees system bar icon contrast)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Navigation Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )

        val windowSize = rememberWindowSize()
        val logoFontSize = if (windowSize.height == WindowHeightSize.COMPACT) 36.sp else 54.sp
        val logoBottomSpacer = if (windowSize.height == WindowHeightSize.COMPACT) 12.dp else 24.dp
        val topSpacer = if (windowSize.height == WindowHeightSize.COMPACT) 8.dp else 24.dp
        val bottomClearance = if (windowSize.height == WindowHeightSize.COMPACT) 64.dp else 96.dp

        // 2. Main Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Action Bar (Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(680.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings Button (WCAG 48dp Touch Target)
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 24.dp, color = AzokleWhite),
                            onClick = { viewModel.setSettingsOpen(true) }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Settings"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AzokleGlassBg)
                            .border(1.dp, AzokleBorderLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AzokleWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Center Hero & Search Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(640.dp)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topSpacer))

                // Audiowide Brand Logo
                Text(
                    text = "azokle",
                    color = AzokleWhite,
                    fontFamily = AudiowideFontFamily,
                    fontSize = logoFontSize,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-1.0).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(logoBottomSpacer))

                // Search / URL Capsule
                SearchBarCapsule(
                    query = uiState.searchQuery,
                    placeholderText = stringResource(R.string.search_placeholder),
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSearch = { input ->
                        viewModel.executeSearch(input)
                    },
                    onVoiceClick = { viewModel.setVoiceModalOpen(true) },
                    onCapsuleClick = { viewModel.setSearchExpanded(true) },
                    isReadOnly = true
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Bottom Space reserving clearance for floating nav bar
            Spacer(modifier = Modifier.height(bottomClearance))
        }

        // Voice Search Modal
        if (uiState.isVoiceModalOpen) {
            VoiceSearchModal(
                onResult = { recognizedQuery ->
                    viewModel.setVoiceModalOpen(false)
                    viewModel.onQueryChange(recognizedQuery)
                    viewModel.executeSearch(recognizedQuery)
                },
                onDismiss = { viewModel.setVoiceModalOpen(false) }
            )
        }

        // Suggestions Sheet
        if (uiState.isSearchExpanded) {
            SuggestionsSheet(
                query = uiState.searchQuery,
                placeholderText = stringResource(R.string.search_placeholder),
                suggestions = uiState.suggestions,
                recentSearches = uiState.recentSearches,
                isLoading = uiState.isLoadingSuggestions,
                selectedCategory = uiState.activeCategory,
                onQueryChange = { viewModel.onQueryChange(it) },
                onSearch = { query ->
                    viewModel.executeSearch(query)
                },
                onCategorySelected = { viewModel.selectCategory(it) },
                onVoiceClick = {
                    viewModel.setSearchExpanded(false)
                    showVoiceModal = true
                },
                onClearRecent = { viewModel.clearRecentSearches() },
                onDismiss = { viewModel.setSearchExpanded(false) }
            )
        }

        // Settings Sheet
        if (uiState.isSettingsOpen) {
            key(uiState.selectedLanguage) {
                SettingsSheet(
                    selectedThemeMode = uiState.selectedThemeMode,
                    selectedDohProvider = uiState.selectedDohProvider,
                    selectedSearchEngine = uiState.selectedSearchEngine,
                    selectedLanguageCode = uiState.selectedLanguage,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onDohProviderChange = { viewModel.setDohProvider(it) },
                    onOpenSearchEngine = { viewModel.setSearchEngineSheetOpen(true) },
                    onOpenLanguage = { viewModel.setLanguageSheetOpen(true) },
                    onDismiss = { viewModel.setSettingsOpen(false) },
                    onOpenPro = { viewModel.setProModalOpen(true) },
                    onOpenPrivacyPolicy = { viewModel.setPrivacyModalOpen(true) },
                    onOpenTerms = { viewModel.setTermsModalOpen(true) },
                    onOpenAbout = { viewModel.setAboutModalOpen(true) },
                    onOpenUrl = { url -> viewModel.openInAppBrowser(url) }
                )
            }
        }

        // Choose Search Engine Sheet
        if (uiState.isSearchEngineSheetOpen) {
            key(uiState.selectedLanguage) {
                com.azokle.search.ui.components.ChooseSearchEngineSheet(
                    selectedEngineId = uiState.selectedSearchEngine,
                    onSelectEngine = { viewModel.setSearchEngine(it) },
                    onDismiss = { viewModel.setSearchEngineSheetOpen(false) }
                )
            }
        }

        // Choose Language Sheet
        if (uiState.isLanguageSheetOpen) {
            key(uiState.selectedLanguage) {
                com.azokle.search.ui.components.ChooseLanguageSheet(
                    selectedLanguageCode = uiState.selectedLanguage,
                    onSelectLanguage = { viewModel.setAppLanguage(it) },
                    onDismiss = { viewModel.setLanguageSheetOpen(false) }
                )
            }
        }

        // Pro Upgrade Modal
        if (uiState.isProModalOpen) {
            ProUpgradeDialog(
                onDismiss = { viewModel.setProModalOpen(false) }
            )
        }

        // Info Modals
        if (uiState.isPrivacyModalOpen) {
            InfoDialog(
                title = stringResource(R.string.privacy_policy),
                content = stringResource(R.string.privacy_policy_content),
                onDismiss = { viewModel.setPrivacyModalOpen(false) }
            )
        }

        if (uiState.isTermsModalOpen) {
            InfoDialog(
                title = stringResource(R.string.terms_service),
                content = stringResource(R.string.terms_service_content),
                onDismiss = { viewModel.setTermsModalOpen(false) }
            )
        }

        if (uiState.isAboutModalOpen) {
            InfoDialog(
                title = stringResource(R.string.about_app),
                content = stringResource(R.string.about_app_content),
                onDismiss = { viewModel.setAboutModalOpen(false) }
            )
        }
    }
}
