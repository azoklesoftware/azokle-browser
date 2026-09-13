import SwiftUI

public struct BrowserOmnibar: View {
    @ObservedObject var state: BrowserState
    let colors: AzokleColors = .dark

    public var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                // 1. Security Lock & Shield Badge
                Button(action: {
                    state.showShieldSheet = true
                }) {
                    HStack(spacing: 4) {
                        Image(systemName: state.isShieldActive ? "shield.fill" : "shield.slash.fill")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(state.isShieldActive ? colors.statusSuccess : colors.textMuted)

                        if state.isShieldActive, let activeTab = state.activeTab, activeTab.blockedTrackers > 0 {
                            Text("\(activeTab.blockedTrackers)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.statusSuccess)
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 6)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.small)
                }

                // 2. Center Omnibar Capsule
                Button(action: {
                    state.showOmniboxSheet = true
                }) {
                    HStack(spacing: 6) {
                        if let activeTab = state.activeTab, activeTab.isTor {
                            Image(systemName: "network.badge.shield.half.filled")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(colors.statusTor)
                        } else if let activeTab = state.activeTab, activeTab.isPrivate {
                            Image(systemName: "eye.slash.fill")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(colors.statusWarning)
                        } else if let url = state.activeTab?.url, url.hasPrefix("https://") {
                            Image(systemName: "lock.fill")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(colors.textSecondary)
                        }

                        Text(displayHost)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(colors.textPrimary)
                            .lineLimit(1)
                            .truncationMode(.tail)

                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity)
                    .background(colors.omnibarBg)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )
                }

                // 3. Reader Mode Icon (if available)
                Button(action: {
                    state.readerModeActive.toggle()
                }) {
                    Image(systemName: state.readerModeActive ? "book.fill" : "book")
                        .font(.system(size: 15, weight: .regular))
                        .foregroundColor(state.readerModeActive ? colors.statusAccent : colors.textSecondary)
                        .frame(width: 32, height: 32)
                }

                // 4. Reload or Stop Button
                if let activeTab = state.activeTab, activeTab.isLoading {
                    Button(action: {
                        if let index = state.tabs.firstIndex(where: { $0.id == state.activeTabId }) {
                            state.tabs[index].isLoading = false
                        }
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 32, height: 32)
                    }
                } else {
                    Button(action: {
                        state.reloadActiveTab()
                    }) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 32, height: 32)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(colors.surface)

            // Progress Bar (Slim linear loader at bottom of Omnibar)
            if let activeTab = state.activeTab, activeTab.isLoading {
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color.clear)
                        Rectangle()
                            .fill(colors.statusAccent)
                            .frame(width: geometry.size.width * CGFloat(activeTab.progress))
                            .animation(.easeOut(duration: 0.2), value: activeTab.progress)
                    }
                }
                .frame(height: 2)
            } else {
                Divider()
                    .background(colors.border)
            }
        }
    }

    private var displayHost: String {
        guard let tab = state.activeTab, !tab.url.isEmpty else { return "Search or enter address" }
        if let url = URL(string: tab.url), let host = url.host {
            return host.replacingOccurrences(of: "www.", with: "")
        }
        return tab.url
    }
}
