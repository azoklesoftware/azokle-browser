import SwiftUI

public struct BrowserScreen: View {
    @StateObject private var state = BrowserState()
    let colors: AzokleColors = .dark

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Omnibar
                BrowserOmnibar(state: state)

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

                // 3. Web View Surface
                ZStack {
                    if let activeTab = state.activeTab {
                        WebViewContainer(state: state, tabId: activeTab.id)
                            .id(activeTab.id)
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
    }
}
