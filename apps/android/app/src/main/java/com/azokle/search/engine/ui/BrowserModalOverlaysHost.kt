package com.azokle.search.engine.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azokle.search.R
import com.azokle.search.adblock.PrivacyShieldSheet
import com.azokle.search.adblock.ThreatAssessment
import com.azokle.search.browser.bookmarks.BookmarksScreen
import com.azokle.search.common.components.AzokleToastHost
import com.azokle.search.common.components.InfoDialog
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.tour.DriverTourOverlay
import com.azokle.search.common.tour.TourState
import com.azokle.search.downloads.DownloadManager
import com.azokle.search.downloads.DownloadsScreen
import com.azokle.search.engine.DownloadConfirmationPayload
import com.azokle.search.engine.TextSelectionPayload
import com.azokle.search.history.HistoryScreen
import com.azokle.search.incineration.BurnConfirmationSheet
import com.azokle.search.incineration.FireIncinerationOverlay
import com.azokle.search.media.AzoklePlayerPromptSheet
import com.azokle.search.permissions.SitePermissionPromptSheet
import com.azokle.search.permissions.SitePermissionRequest
import com.azokle.search.pwa.PwaInstallBottomSheet
import com.azokle.search.pwa.TabPwaState
import com.azokle.search.qr.QrScannerScreen
import com.azokle.search.reader.ArticleContent
import com.azokle.search.reader.ReaderModeScreen
import com.azokle.search.search.ChooseSearchEngineSheet
import com.azokle.search.search.HomeViewModel
import com.azokle.search.search.VoiceSearchModal
import com.azokle.search.settings.ChooseLanguageSheet
import com.azokle.search.settings.SettingsSheet
import com.azokle.search.tabs.ActiveTabsScreen
import com.azokle.search.tabs.TabGroup
import com.azokle.search.tabs.TabGroupDetailSheet
import com.azokle.search.tabs.TabManager
import com.azokle.search.tabs.TabSession
import com.azokle.search.tor.TorCircuitVisualizerSheet
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.util.Locale

@Composable
fun BrowserModalOverlaysHost(
    activeArticleReader: ArticleContent?,
    onDismissArticleReader: () -> Unit,
    showShieldSheet: Boolean,
    onDismissShieldSheet: () -> Unit,
    onShieldToggled: () -> Unit,
    showSecurityCertSheet: Boolean,
    currentUrl: String,
    currentTitle: String,
    currentSecurityInfo: GeckoSession.ProgressDelegate.SecurityInformation?,
    onDismissSecurityCertSheet: () -> Unit,
    onReloadSecurityCert: () -> Unit,
    showTorCircuitSheet: Boolean,
    onDismissTorCircuitSheet: () -> Unit,
    showUrlEditSheet: Boolean,
    onNavigateFromUrlEdit: (String) -> Unit,
    onDismissUrlEditSheet: () -> Unit,
    selectedSearchEngine: String,
    onSelectSearchEngine: (String) -> Unit,
    showMenuSheet: Boolean,
    isDesktopMode: Boolean,
    isBookmarked: Boolean,
    blockedTrackersCount: Int,
    onReloadMenu: () -> Unit,
    onToggleBookmarkMenu: () -> Unit,
    onShareMenu: () -> Unit,
    onToggleDesktopModeMenu: () -> Unit,
    onSaveAsPdfMenu: () -> Unit,
    onOpenReaderModeMenu: () -> Unit,
    onOpenFindInPageMenu: () -> Unit,
    onOpenBookmarksMenu: () -> Unit,
    onOpenDownloadsMenu: () -> Unit,
    onOpenHistoryMenu: () -> Unit,
    onOpenShieldMenu: () -> Unit,
    onOpenNewTabMenu: () -> Unit,
    onOpenNewPrivateTabMenu: () -> Unit,
    onOpenTorWindowMenu: () -> Unit,
    onOpenSettingsMenu: () -> Unit,
    pwaState: TabPwaState?,
    showPwaInstallSheet: Boolean,
    onDismissPwaInstallSheet: () -> Unit,
    onInstallPwaMenu: () -> Unit,
    onBurnDataMenu: () -> Unit,
    onDismissMenuSheet: () -> Unit,
    showBookmarksScreen: Boolean,
    onDismissBookmarksScreen: () -> Unit,
    onNavigateFromBookmarks: (String) -> Unit,
    onOpenTorFromBookmarks: (String) -> Unit,
    showDownloadsScreen: Boolean,
    onDismissDownloadsScreen: () -> Unit,
    showHistoryScreen: Boolean,
    onDismissHistoryScreen: () -> Unit,
    onHistoryQueryClick: (String) -> Unit,
    onHistoryUrlClick: (String) -> Unit,
    viewModel: HomeViewModel?,
    showTabGridSheet: Boolean,
    forceStartTabsTour: Boolean,
    onTabSelected: (TabSession) -> Unit,
    onNewTabFromSwitcher: () -> Unit,
    onCloseTabsSwitcher: () -> Unit,
    onTabsTourConsumed: () -> Unit,
    showSettingsSheet: Boolean,
    onDismissSettingsSheet: () -> Unit,
    onReplayHomeTour: () -> Unit,
    onReplayTabsTour: () -> Unit,
    onOpenUrlFromSettings: (String) -> Unit,
    isSearchEngineSheetOpen: Boolean,
    onDismissSearchEngineSheet: () -> Unit,
    isLanguageSheetOpen: Boolean,
    selectedLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onDismissLanguageSheet: () -> Unit,
    isPrivacyModalOpen: Boolean,
    onDismissPrivacyModal: () -> Unit,
    isTermsModalOpen: Boolean,
    onDismissTermsModal: () -> Unit,
    isAboutModalOpen: Boolean,
    onDismissAboutModal: () -> Unit,
    activeLinkContextTarget: LinkContextTarget?,
    isCurrentTabPrivate: Boolean,
    isCurrentTabTor: Boolean,
    onOpenInNewTabGroup: (String, String) -> Unit,
    onOpenInNewTab: (String, String) -> Unit,
    onOpenInBackgroundTab: (String, String) -> Unit,
    onOpenInPrivateTab: (String, String) -> Unit,
    onDismissLinkContext: () -> Unit,
    activeTextSelection: TextSelectionPayload?,
    onSearchRequested: (String) -> Unit,
    onShareRequested: (String) -> Unit,
    showShareSheet: Boolean,
    activeSharePayload: SharePayload?,
    onDismissShareSheet: () -> Unit,
    onPrintToPdfShare: () -> Unit,
    showGroupDetailSheet: Boolean,
    activeGroup: TabGroup?,
    activeGroupTabs: List<TabSession>,
    activeTabId: String?,
    onAddTabInGroup: () -> Unit,
    onCloseTabInGroup: (String) -> Unit,
    onDismissGroupDetailSheet: () -> Unit,
    showBurnConfirmationSheet: Boolean,
    onConfirmBurnData: () -> Unit,
    onDismissBurnConfirmation: () -> Unit,
    isBurningData: Boolean,
    onCompleteBurnData: () -> Unit,
    activePermissionRequest: SitePermissionRequest?,
    onAllowAlwaysPermission: (SitePermissionRequest) -> Unit,
    onAllowOncePermission: (SitePermissionRequest) -> Unit,
    onDenyPermission: (SitePermissionRequest, Boolean) -> Unit,
    pendingAzoklePlayerVideoUrl: String?,
    onWatchInAzoklePlayer: (Boolean) -> Unit,
    onWatchOnPlatform: (Boolean) -> Unit,
    onDismissAzoklePlayerPrompt: () -> Unit,
    pendingThreatAssessment: ThreatAssessment?,
    onReturnToSafetyThreat: () -> Unit,
    onProceedUnsafeThreat: (ThreatAssessment) -> Unit,
    pendingDownloadPayload: DownloadConfirmationPayload?,
    onConfirmDownload: (DownloadConfirmationPayload) -> Unit,
    onDismissDownload: () -> Unit,
    showVoiceModal: Boolean,
    onVoiceSearchResult: (String) -> Unit,
    onDismissVoiceModal: () -> Unit,
    showQrScanner: Boolean,
    onQrScanResult: (String) -> Unit,
    onDismissQrScanner: () -> Unit,
    showDiscoverScreen: Boolean,
    homeTourState: TourState,
    geckoView: GeckoView?
) {
    val colors = AzokleTheme.colors
    val context = LocalContext.current

    // Reader Mode Full Screen View
    activeArticleReader?.let { article ->
        ReaderModeScreen(
            article = article,
            onClose = onDismissArticleReader
        )
    }

    // Shield HUD Sheet
    if (showShieldSheet) {
        PrivacyShieldSheet(
            onDismiss = onDismissShieldSheet,
            onShieldToggled = onShieldToggled
        )
    }

    // Luxury SSL / TLS Certificate Inspector Sheet with Real X.509 Security Information
    if (showSecurityCertSheet) {
        SiteSecurityCertificateSheet(
            url = currentUrl,
            securityInfo = currentSecurityInfo,
            onDismiss = onDismissSecurityCertSheet,
            onReload = onReloadSecurityCert
        )
    }

    // Live 3-Hop Tor Circuit Inspector & Node Visualizer
    if (showTorCircuitSheet) {
        TorCircuitVisualizerSheet(
            url = currentUrl,
            onDismiss = onDismissTorCircuitSheet
        )
    }

    // Interactive Omnibar URL / Search Editor Modal
    if (showUrlEditSheet) {
        BrowserUrlEditSheet(
            initialText = currentUrl,
            onNavigate = onNavigateFromUrlEdit,
            onDismiss = onDismissUrlEditSheet,
            initialSearchEngine = selectedSearchEngine,
            onEngineSelected = onSelectSearchEngine
        )
    }

    // Action Menu Sheet
    if (showMenuSheet) {
        BrowserMenuSheet(
            url = currentUrl,
            title = currentTitle,
            isDesktopMode = isDesktopMode,
            isBookmarked = isBookmarked,
            blockedTrackersCount = blockedTrackersCount,
            onReload = onReloadMenu,
            onToggleBookmark = onToggleBookmarkMenu,
            onShare = onShareMenu,
            onToggleDesktopMode = onToggleDesktopModeMenu,
            onSaveAsPdf = onSaveAsPdfMenu,
            onOpenReaderMode = onOpenReaderModeMenu,
            onOpenFindInPage = onOpenFindInPageMenu,
            isBrowserContext = true,
            onOpenBookmarks = onOpenBookmarksMenu,
            onOpenDownloads = onOpenDownloadsMenu,
            onOpenHistory = onOpenHistoryMenu,
            onOpenShield = onOpenShieldMenu,
            onOpenNewTab = onOpenNewTabMenu,
            onOpenNewPrivateTab = onOpenNewPrivateTabMenu,
            onOpenTorWindow = onOpenTorWindowMenu,
            onOpenSettings = onOpenSettingsMenu,
            pwaEligibility = pwaState?.eligibility,
            onInstallPwa = onInstallPwaMenu,
            onBurnData = onBurnDataMenu,
            onDismiss = onDismissMenuSheet
        )
    }

    // PWA Install / Add to Home Screen Bottom Sheet
    if (showPwaInstallSheet && pwaState?.eligibility != null) {
        PwaInstallBottomSheet(
            eligibility = pwaState.eligibility,
            onDismiss = onDismissPwaInstallSheet
        )
    }

    // Dedicated Bookmarks & Quick Links Screen Overlay
    if (showBookmarksScreen) {
        BookmarksScreen(
            onDismiss = onDismissBookmarksScreen,
            onNavigate = onNavigateFromBookmarks,
            onOpenTor = onOpenTorFromBookmarks
        )
    }

    // Dedicated Downloads Screen Overlay
    if (showDownloadsScreen) {
        DownloadsScreen(
            onDismiss = onDismissDownloadsScreen
        )
    }

    // Dedicated History Screen Overlay
    if (showHistoryScreen) {
        HistoryScreen(
            viewModel = viewModel ?: viewModel(),
            onHistoryQueryClick = onHistoryQueryClick,
            onOpenUrl = onHistoryUrlClick,
            onClose = onDismissHistoryScreen
        )
    }

    // Lifecycle-Aware Background Compositor Throttling: Pause Gecko rendering thread & detach SurfaceView while tab switcher is open
    LaunchedEffect(showTabGridSheet) {
        val activeSession = TabManager.getActiveSession()
        if (showTabGridSheet) {
            try {
                activeSession?.setActive(false)
                activeSession?.setFocused(false)
                activeSession?.setPriorityHint(GeckoSession.PRIORITY_DEFAULT)
                geckoView?.visibility = android.view.View.INVISIBLE
            } catch (e: Exception) {}
        } else {
            try {
                activeSession?.setActive(true)
                activeSession?.setFocused(true)
                activeSession?.setPriorityHint(GeckoSession.PRIORITY_HIGH)
                if (currentUrl.isNotBlank() && currentUrl != "about:home" && currentUrl != "about:blank" && currentUrl != "about:tor") {
                    geckoView?.visibility = android.view.View.VISIBLE
                    geckoView?.alpha = 1f
                }
            } catch (e: Exception) {}
        }
    }

    // Unified Active Tabs Screen Switcher (Butter-smooth 120/144Hz Transition)
    AnimatedVisibility(
        visible = showTabGridSheet,
        enter = fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.95f, animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150))
    ) {
        ActiveTabsScreen(
            onTabSelected = onTabSelected,
            onNewTabClick = onNewTabFromSwitcher,
            onClose = onCloseTabsSwitcher,
            forceStartTour = forceStartTabsTour,
            onTourConsumed = onTabsTourConsumed
        )
    }

    // Settings Sheet directly accessible from Browser Header
    if (showSettingsSheet) {
        val uiState = viewModel?.uiState?.collectAsState()?.value
        SettingsSheet(
            selectedThemeMode = uiState?.selectedThemeMode ?: "system",
            onThemeModeChange = { viewModel?.setThemeMode(it) },
            selectedDohProvider = uiState?.selectedDohProvider ?: "cloudflare",
            onDohProviderChange = { viewModel?.setDohProvider(it) },
            selectedSearchEngine = uiState?.selectedSearchEngine ?: "azokle",
            onOpenSearchEngine = { viewModel?.setSearchEngineSheetOpen(true) },
            selectedLanguageCode = uiState?.selectedLanguage ?: "system",
            onOpenLanguage = { viewModel?.setLanguageSheetOpen(true) },
            onDismiss = onDismissSettingsSheet,
            onReplayHomeTour = onReplayHomeTour,
            onReplayTabsTour = onReplayTabsTour,
            onOpenPrivacyPolicy = { viewModel?.setPrivacyModalOpen(true) },
            onOpenTerms = { viewModel?.setTermsModalOpen(true) },
            onOpenAbout = { viewModel?.setAboutModalOpen(true) },
            onOpenUrl = onOpenUrlFromSettings
        )
    }

    // Choose Search Engine Sheet
    if (isSearchEngineSheetOpen) {
        ChooseSearchEngineSheet(
            selectedEngineId = selectedSearchEngine,
            onSelectEngine = { engineId ->
                onSelectSearchEngine(engineId)
                onDismissSearchEngineSheet()
            },
            onDismiss = onDismissSearchEngineSheet
        )
    }

    // Choose Language Sheet
    if (isLanguageSheetOpen) {
        ChooseLanguageSheet(
            selectedLanguageCode = selectedLanguageCode,
            onSelectLanguage = { langCode ->
                onSelectLanguage(langCode)
                onDismissLanguageSheet()
            },
            onDismiss = onDismissLanguageSheet
        )
    }

    // Privacy Policy Dialog
    if (isPrivacyModalOpen) {
        InfoDialog(
            title = stringResource(R.string.privacy_policy),
            content = stringResource(R.string.privacy_policy_content),
            onDismiss = onDismissPrivacyModal
        )
    }

    // Terms of Service Dialog
    if (isTermsModalOpen) {
        InfoDialog(
            title = stringResource(R.string.terms_service),
            content = stringResource(R.string.terms_service_content),
            onDismiss = onDismissTermsModal
        )
    }

    // About Dialog
    if (isAboutModalOpen) {
        InfoDialog(
            title = stringResource(R.string.about_app),
            content = stringResource(R.string.about_app_content),
            onDismiss = onDismissAboutModal
        )
    }

    // Link Context Menu Sheet (Long-Press hyperlink)
    activeLinkContextTarget?.let { target ->
        LinkContextMenuSheet(
            target = target,
            isPrivate = isCurrentTabPrivate,
            isTor = isCurrentTabTor,
            onOpenInNewTabGroup = onOpenInNewTabGroup,
            onOpenInNewTab = onOpenInNewTab,
            onOpenInBackgroundTab = onOpenInBackgroundTab,
            onOpenInPrivateTab = onOpenInPrivateTab,
            onDismiss = onDismissLinkContext
        )
    }

    // Text Selection Floating Context Menu (Copy, Share, Web Search, Select All)
    TextSelectionFloatingToolbar(
        payload = activeTextSelection,
        onSearchRequested = onSearchRequested,
        onShareRequested = onShareRequested
    )

    // Sovereign In-App Share Sheet
    if (showShareSheet && activeSharePayload != null) {
        BrowserShareSheet(
            payload = activeSharePayload,
            onDismiss = onDismissShareSheet,
            onPrintToPdf = onPrintToPdfShare
        )
    }

    // Tab Group Detail Modal Sheet (Overview, reorder, rename, palette picker)
    if (showGroupDetailSheet && activeGroup != null) {
        TabGroupDetailSheet(
            group = activeGroup,
            tabs = activeGroupTabs,
            activeTabId = activeTabId,
            onTabSelected = onTabSelected,
            onAddTabInGroup = onAddTabInGroup,
            onCloseTab = onCloseTabInGroup,
            onDismiss = onDismissGroupDetailSheet
        )
    }

    // 🔥 Burn Data Confirmation Sheet
    if (showBurnConfirmationSheet) {
        BurnConfirmationSheet(
            onConfirm = onConfirmBurnData,
            onDismiss = onDismissBurnConfirmation
        )
    }

    // 🔥 Full Screen Cinema-Grade Fire Incineration Overlay
    if (isBurningData) {
        FireIncinerationOverlay(
            onComplete = onCompleteBurnData
        )
    }

    // 🛡️ Brave-Grade Site Permission Prompt Modal
    activePermissionRequest?.let { request ->
        SitePermissionPromptSheet(
            request = request,
            onAllowAlways = { onAllowAlwaysPermission(request) },
            onAllowOnce = { onAllowOncePermission(request) },
            onDeny = { rememberChoice -> onDenyPermission(request, rememberChoice) },
            onDismiss = { onDenyPermission(request, false) }
        )
    }

    // 🎬 Azokle Sovereign Player Prompt Modal (Inspired by Duck Player)
    pendingAzoklePlayerVideoUrl?.let { videoUrl ->
        AzoklePlayerPromptSheet(
            videoUrl = videoUrl,
            onWatchInAzoklePlayer = onWatchInAzoklePlayer,
            onWatchOnPlatform = onWatchOnPlatform,
            onDismiss = onDismissAzoklePlayerPrompt
        )
    }

    // 🚨 Sovereign Phishing & Malicious Site Warning Modal
    pendingThreatAssessment?.let { threat ->
        AlertDialog(
            onDismissRequest = onReturnToSafetyThreat,
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Alert",
                    tint = colors.statusDestructive,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = threat.threatType.label,
                    color = colors.statusDestructive,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = threat.host,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = threat.threatType.description,
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onReturnToSafetyThreat,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.actionPrimaryBg,
                        contentColor = colors.actionPrimaryText
                    )
                ) {
                    Text("Return to Safety")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onProceedUnsafeThreat(threat) }
                ) {
                    Text("Proceed Anyway (Unsafe)", color = colors.statusDestructive)
                }
            },
            containerColor = colors.cardBg
        )
    }

    // 📥 Sovereign Download Authorization Modal (Fix SEC-002 & Executable Warnings)
    pendingDownloadPayload?.let { payload ->
        val isExecutable = DownloadManager.isDangerousExecutable(payload.filename, payload.mimeType)
        AlertDialog(
            onDismissRequest = onDismissDownload,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExecutable) Icons.Default.Warning else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (isExecutable) colors.statusDestructive else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExecutable) "Download Executable?" else "Download File?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isExecutable) colors.statusDestructive else colors.textPrimary
                    )
                }
            },
            text = {
                Column {
                    if (isExecutable) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.md)
                                .background(colors.statusDestructive.copy(alpha = 0.12f))
                                .border(1.dp, colors.statusDestructive.copy(alpha = 0.35f), AzokleRadius.md)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Warning: This file is an application package or executable. Only install if you trust the source.",
                                fontSize = 12.sp,
                                color = colors.statusDestructive,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = "Do you want to download '${payload.filename}'?",
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    if (payload.contentLength > 0) {
                        val sizeMb = String.format(Locale.US, "%.2f MB", payload.contentLength / (1024.0 * 1024.0))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size: $sizeMb",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: ${payload.mimeType}",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                    if (payload.isTor) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shield: Routing via Tor Onion Circuit",
                            fontSize = 11.5.sp,
                            color = colors.statusAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (payload.isPrivate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Privacy: Stored in isolated sandbox vault",
                            fontSize = 11.5.sp,
                            color = colors.statusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDownload(payload) }
                ) {
                    Text(
                        text = if (isExecutable) "Download Anyway" else "Download",
                        fontWeight = FontWeight.Bold,
                        color = if (isExecutable) colors.statusDestructive else colors.actionPrimaryBg
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDownload) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Voice Search Modal in Browser Screen
    if (showVoiceModal) {
        VoiceSearchModal(
            onResult = onVoiceSearchResult,
            onDismiss = onDismissVoiceModal
        )
    }

    // QR Code Scanner Modal in Browser Screen
    if (showQrScanner) {
        QrScannerScreen(
            onScanResult = onQrScanResult,
            onDismiss = onDismissQrScanner
        )
    }

    // 🌟 Clean & Premium Floating Toast HUD (Positioned safely below the Omnibar)
    val extraToastOffset = if (isCurrentTabTor && !showDiscoverScreen) 34.dp else 0.dp
    AzokleToastHost(extraTopOffset = extraToastOffset)

    // 🚀 Driver.js-Grade Interactive Spotlight Tour Guide (Home Screen)
    DriverTourOverlay(tourState = homeTourState)
}
