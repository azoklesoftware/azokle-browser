import SwiftUI

public struct BrowserScreen: View {
    @StateObject private var state = BrowserState()
    let colors: AzokleColors = .dark

    private var isHomeTab: Bool {
        guard let tab = state.activeTab else { return true }
        return tab.url == "https://search.azokle.com" || tab.url == "about:blank" || tab.url.isEmpty
    }

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Omnibar
                BrowserOmnibar(state: state)

                // Find in Page Search Bar (when active)
                if state.showFindInPage {
                    FindInPageBar(state: state)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }

                // 2. Tor Circuit Active Banner (if in Tor Mode)
                if let tab = state.activeTab, tab.isTor {
                    HStack(spacing: 6) {
                        Image(systemName: "network.badge.shield.half.filled")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.statusTor)
                        Text("3-Hop Onion Circuit Protected")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(colors.statusTor)
                        Spacer()
                        Button("Inspect") {
                            state.showTorCircuitSheet = true
                        }
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(colors.statusAccent)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 5)
                    .background(colors.statusTor.opacity(0.15))
                }

                // 3. Main Surface (Home Screen vs Web View Surface)
                ZStack {
                    if isHomeTab {
                        HomeScreen(state: state)
                            .transition(.opacity)
                    } else if let activeTab = state.activeTab {
                        WebViewContainer(state: state, tabId: activeTab.id)
                            .id(activeTab.id)
                            .transition(.opacity)
                    } else {
                        VStack(spacing: 12) {
                            Image(systemName: "globe")
                                .font(.system(size: 40))
                                .foregroundColor(colors.textMuted)
                            Text("No Active Session")
                                .foregroundColor(colors.textSecondary)
                        }
                    }

                    // 4. Incineration Flame Burn Animation Overlay
                    if state.isBurningAnimationActive {
                        ZStack {
                            colors.background.opacity(0.92)

                            VStack(spacing: 14) {
                                Image(systemName: "flame.fill")
                                    .font(.system(size: 64))
                                    .foregroundColor(colors.statusFire)
                                    .scaleEffect(1.2)
                                    .animation(.easeInOut(duration: 0.6).repeatForever(), value: state.isBurningAnimationActive)

                                Text("Incinerating Browsing Traces...")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }
                        .transition(.opacity)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // 5. Bottom Navigation Bar
                BrowserBottomBar(state: state)
            }
        }
        // Sheets
        .sheet(isPresented: $state.showOmniboxSheet) {
            OmniboxSheet(state: state, isPresented: $state.showOmniboxSheet)
        }
        .sheet(isPresented: $state.showMenuSheet) {
            BrowserMenuSheet(state: state, isPresented: $state.showMenuSheet)
        }
        .sheet(isPresented: $state.showTabsGrid) {
            TabsScreen(state: state, isPresented: $state.showTabsGrid)
        }
        .sheet(isPresented: $state.showShieldSheet) {
            PrivacyShieldSheet(state: state, isPresented: $state.showShieldSheet)
        }
        .sheet(isPresented: $state.showBurnConfirmation) {
            BurnConfirmationSheet(state: state, isPresented: $state.showBurnConfirmation)
        }
        .sheet(isPresented: $state.showTorCircuitSheet) {
            TorCircuitSheet(state: state, isPresented: $state.showTorCircuitSheet)
        }
        .sheet(isPresented: $state.showHistorySheet) {
            HistoryScreen(state: state, isPresented: $state.showHistorySheet)
        }
        .sheet(isPresented: $state.showBookmarksSheet) {
            BookmarksScreen(state: state, isPresented: $state.showBookmarksSheet)
        }
        .sheet(isPresented: $state.showDownloadsSheet) {
            DownloadsScreen(state: state, isPresented: $state.showDownloadsSheet)
        }
        .sheet(isPresented: $state.showSettingsSheet) {
            SettingsScreen(state: state, isPresented: $state.showSettingsSheet)
        }
        .sheet(isPresented: $state.showDiscoverScreen) {
            DiscoverScreen(state: state, isPresented: $state.showDiscoverScreen)
        }
        .sheet(isPresented: $state.showCertificateSheet) {
            SiteSecurityCertificateSheet(state: state, isPresented: $state.showCertificateSheet)
        }
        .sheet(isPresented: $state.showSiteSettingsSheet) {
            SiteSettingsSheet(state: state, isPresented: $state.showSiteSettingsSheet)
        }
        .sheet(isPresented: $state.showShareSheet) {
            BrowserShareSheet(state: state, isPresented: $state.showShareSheet)
        }
        .sheet(item: $state.activePermissionPrompt) { prompt in
            SitePermissionPromptSheet(request: prompt, state: state)
        }
        .sheet(item: $state.activeLinkContext) { context in
            LinkContextMenuSheet(target: context, state: state)
        }
        .sheet(isPresented: $state.showVoiceSearch) {
            VoiceSearchModal(isPresented: $state.showVoiceSearch, state: state)
        }
        .sheet(isPresented: $state.showChooseEngineSheet) {
            ChooseSearchEngineSheet(isPresented: $state.showChooseEngineSheet, state: state)
        }
        .sheet(isPresented: $state.showLanguageSheet) {
            ChooseLanguageSheet(isPresented: $state.showLanguageSheet, state: state)
        }
        .sheet(isPresented: $state.showPwaSheet) {
            PwaInstallBottomSheet(isPresented: $state.showPwaSheet, state: state)
        }
        .sheet(isPresented: $state.showSuggestionsSheet) {
            SuggestionsSheet(isPresented: $state.showSuggestionsSheet, state: state)
        }
        .sheet(item: $state.activeTabGroupForDetail) { group in
            TabGroupDetailSheet(
                group: group,
                state: state,
                isPresented: Binding(
                    get: { state.activeTabGroupForDetail != nil },
                    set: { if !$0 { state.activeTabGroupForDetail = nil } }
                )
            )
        }
        .fullScreenCover(isPresented: $state.showOnboarding) {
            OnboardingScreen(isPresented: $state.showOnboarding, state: state)
        }
        .sheet(isPresented: $state.showPasskeyVaultSheet) {
            PasskeyVaultSheet(state: state, isPresented: $state.showPasskeyVaultSheet)
        }
        .sheet(isPresented: $state.showAzoklePlayerPrompt) {
            AzoklePlayerPromptSheet(
                videoUrl: state.activeVideoUrl,
                onWatchInPlayer: { remember in
                    if remember { state.playerMode = .always }
                    state.showAzoklePlayer = true
                },
                onWatchOnPlatform: { remember in
                    if remember { state.playerMode = .disabled }
                },
                isPresented: $state.showAzoklePlayerPrompt
            )
        }
        .fullScreenCover(isPresented: $state.showAzoklePlayer) {
            AzoklePlayerSheet(
                videoId: state.activeVideoId,
                originalUrl: state.activeVideoUrl,
                isPresented: $state.showAzoklePlayer
            )
        }
        .fullScreenCover(isPresented: $state.showReaderScreen) {
            if let article = state.readerArticleContent {
                ReaderModeScreen(article: article, isPresented: $state.showReaderScreen)
            }
        }
        .fullScreenCover(isPresented: $state.showTorConnectScreen) {
            TorConnectScreen(state: state, isPresented: $state.showTorConnectScreen)
        }
        .sheet(isPresented: $state.showProUpgradeSheet) {
            ProUpgradeSheet(isPresented: $state.showProUpgradeSheet)
        }
        .fullScreenCover(item: $state.activeThreatAssessment) { threat in
            SafeBrowsingWarningOverlay(
                threat: threat,
                onBackToSafety: {
                    state.activeThreatAssessment = nil
                },
                onBypass: {
                    SafeBrowsingGuard.shared.bypassThreatForSession(host: threat.host)
                    state.activeThreatAssessment = nil
                    state.navigateActiveTab(to: threat.url)
                }
            )
        }
        .onAppear {
            let hasOnboarded = UserDefaults.standard.bool(forKey: "hasCompletedOnboarding")
            if !hasOnboarded {
                state.showOnboarding = true
            }
        }
    }
}
