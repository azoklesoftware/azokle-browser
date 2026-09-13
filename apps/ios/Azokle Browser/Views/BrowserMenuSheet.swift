import SwiftUI

public struct BrowserMenuSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool
    let colors: AzokleColors = .dark

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 10)
                .padding(.bottom, 12)

            // 1. Current Webpage Header
            HStack(spacing: 12) {
                Image(systemName: "globe")
                    .font(.system(size: 20))
                    .foregroundColor(colors.statusAccent)

                VStack(alignment: .leading, spacing: 2) {
                    Text(state.activeTab?.title ?? "Azokle Sovereign Browser")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.textPrimary)
                        .lineLimit(1)

                    Text(state.activeTab?.url ?? "search.azokle.com")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                        .lineLimit(1)
                }

                Spacer()

                // Shield badge
                if state.isShieldActive {
                    HStack(spacing: 4) {
                        Image(systemName: "shield.fill")
                            .font(.system(size: 11))
                            .foregroundColor(colors.statusSuccess)
                        Text("\(state.activeTab?.blockedTrackers ?? 0)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.statusSuccess)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.small)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)

            Divider()
                .background(colors.border)

            // 2. Quick Action Row (Back, Forward, Reload, Bookmark, Share)
            HStack(spacing: 0) {
                quickActionButton(
                    icon: "arrow.left",
                    label: "Back",
                    isEnabled: state.activeTab?.canGoBack ?? false
                ) {
                    state.triggerWebAction(.goBack)
                    isPresented = false
                }

                quickActionButton(
                    icon: "arrow.right",
                    label: "Forward",
                    isEnabled: state.activeTab?.canGoForward ?? false
                ) {
                    state.triggerWebAction(.goForward)
                    isPresented = false
                }

                quickActionButton(icon: "arrow.clockwise", label: "Reload") {
                    state.reloadActiveTab()
                    isPresented = false
                }

                quickActionButton(
                    icon: state.isBookmarked(url: state.activeTab?.url ?? "") ? "bookmark.fill" : "bookmark",
                    label: "Bookmark"
                ) {
                    if let tab = state.activeTab {
                        state.toggleBookmark(url: tab.url, title: tab.title)
                    }
                }

                quickActionButton(icon: "square.and.arrow.up", label: "Share") {
                    isPresented = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                        state.showShareSheet = true
                    }
                }
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 16)
            .background(colors.surfaceElevated)

            Divider()
                .background(colors.border)

            // 3. Main Browser Action Grid (4 Columns)
            ScrollView {
                LazyVGrid(columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ], spacing: 18) {
                    menuItem(icon: "plus.square", label: "New Tab") {
                        state.openNewTab()
                        isPresented = false
                    }

                    menuItem(icon: "eye.slash", label: "Private Tab") {
                        state.openNewTab(isPrivate: true)
                        isPresented = false
                    }

                    menuItem(icon: "network.badge.shield.half.filled", label: "Tor Tab", color: colors.statusTor) {
                        state.openNewTab(isTor: true)
                        isPresented = false
                    }

                    menuItem(icon: "shield.fill", label: "Shield", color: colors.statusSuccess) {
                        isPresented = false
                        state.showShieldSheet = true
                    }

                    menuItem(icon: "book", label: "Bookmarks") {
                        isPresented = false
                        state.showBookmarksSheet = true
                    }

                    menuItem(icon: "clock", label: "History") {
                        isPresented = false
                        state.showHistorySheet = true
                    }

                    menuItem(icon: "arrow.down.circle", label: "Downloads") {
                        isPresented = false
                        state.showDownloadsSheet = true
                    }

                    menuItem(icon: "lock.shield", label: "Tor Circuit", color: colors.statusTor) {
                        isPresented = false
                        state.showTorCircuitSheet = true
                    }

                    menuItem(
                        icon: "desktopcomputer",
                        label: state.desktopMode ? "Mobile Site" : "Desktop Site",
                        color: state.desktopMode ? colors.statusAccent : nil
                    ) {
                        state.desktopMode.toggle()
                        state.reloadActiveTab()
                        isPresented = false
                    }

                    menuItem(icon: "magnifyingglass", label: "Find in Page") {
                        isPresented = false
                        state.showFindInPage = true
                    }

                    menuItem(icon: "slider.horizontal.3", label: "Site Controls") {
                        isPresented = false
                        state.showSiteSettingsSheet = true
                    }

                    menuItem(icon: "gearshape", label: "Settings") {
                        isPresented = false
                        state.showSettingsSheet = true
                    }

                    menuItem(icon: "safari", label: "Discover") {
                        isPresented = false
                        state.showDiscoverScreen = true
                    }

                    menuItem(icon: "flame.fill", label: "Burn Data", color: colors.statusFire) {
                        isPresented = false
                        state.showBurnConfirmation = true
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 18)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.55), .large])
    }

    private func quickActionButton(icon: String, label: String, isEnabled: Bool = true, action: @escaping () -> Void) -> some View {
        Button(action: {
            if isEnabled {
                action()
            }
        }) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 18))
                    .foregroundColor(isEnabled ? colors.textPrimary : colors.textMuted.opacity(0.5))
                Text(label)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(isEnabled ? colors.textSecondary : colors.textMuted.opacity(0.5))
            }
            .frame(maxWidth: .infinity)
        }
        .disabled(!isEnabled)
    }

    private func menuItem(icon: String, label: String, color: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: AzokleRadius.medium)
                        .fill(colors.surfaceElevated)
                        .frame(width: 52, height: 52)
                        .overlay(
                            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                .stroke(colors.border, lineWidth: 1)
                        )

                    Image(systemName: icon)
                        .font(.system(size: 22))
                        .foregroundColor(color ?? colors.textPrimary)
                }

                Text(label)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(colors.textPrimary)
                    .lineLimit(1)
            }
        }
    }
}
