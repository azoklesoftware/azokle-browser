import SwiftUI

// MARK: - Redesigned Browser Omnibar (1:1 Android BrowserOmnibar.kt Parity)
public struct BrowserOmnibar: View {
    @ObservedObject var state: BrowserState
    let colors: AzokleColors = .dark

    @State private var copiedFeedbackActive: Bool = false

    private var activeUrl: String {
        state.activeTab?.url ?? ""
    }

    private var isHttp: Bool {
        activeUrl.hasPrefix("http://")
    }

    private var isInternal: Bool {
        activeUrl.hasPrefix("about:") || activeUrl.isEmpty || activeUrl == "https://search.azokle.com"
    }

    // RFC 3986 Anti-Spoofing Host Extraction:
    // Strips embedded credentials/user-info prefixes (e.g. https://brand.com@evil.com -> evil.com)
    private var cleanHost: String {
        guard !activeUrl.isEmpty, let url = URL(string: activeUrl) else { return "" }
        if let host = url.host {
            let trimmed = host.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.hasPrefix("www.") {
                return String(trimmed.dropFirst(4))
            }
            return trimmed
        }
        return ""
    }

    private var isOnion: Bool {
        cleanHost.hasSuffix(".onion") || (state.activeTab?.isTor ?? false)
    }

    private var displayHost: String {
        if isInternal || cleanHost.isEmpty {
            return "Search or type URL"
        }
        return cleanHost
    }

    private var urlPath: String {
        guard !isInternal, let url = URL(string: activeUrl) else { return "" }
        let path = url.path
        if path.isEmpty || path == "/" {
            return ""
        }
        return path
    }

    private var sslIcon: String {
        if isOnion {
            return "network.badge.shield.half.filled"
        } else if isHttp {
            return "exclamationmark.triangle.fill"
        } else {
            return "lock.fill"
        }
    }

    private var sslTint: Color {
        if isOnion {
            return colors.statusTor
        } else if isHttp {
            return colors.statusDestructive
        } else {
            return colors.statusSuccess
        }
    }

    public var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                // Main Address Capsule (42pt height, surfaceElevated background, 14pt radius)
                HStack(spacing: 8) {
                    // 1. Left Element: Security Padlock OR Search Engine Dropdown
                    if !isInternal && !cleanHost.isEmpty {
                        Button(action: {
                            triggerHaptic()
                            state.showCertificateSheet = true
                        }) {
                            Image(systemName: sslIcon)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(sslTint)
                                .frame(width: 24, height: 24)
                        }
                    } else {
                        // Search Engine Selector on Home (Tap opens ChooseSearchEngineSheet)
                        Button(action: {
                            state.showChooseEngineSheet = true
                        }) {
                            HStack(spacing: 3) {
                                Image(state.selectedSearchEngine.assetName)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 16, height: 16)
                                Image(systemName: "chevron.down")
                                    .font(.system(size: 7, weight: .bold))
                                    .foregroundColor(colors.textMuted)
                            }
                            .frame(height: 24)
                        }
                    }

                    // 2. Center Text: Anti-Spoofing Visual Hierarchy
                    Button(action: {
                        state.showOmniboxSheet = true
                    }) {
                        HStack(spacing: 0) {
                            if copiedFeedbackActive {
                                Text("URL copied to clipboard")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(colors.statusSuccess)
                            } else if isInternal || cleanHost.isEmpty {
                                Text(displayHost)
                                    .font(.system(size: 14, weight: .regular))
                                    .foregroundColor(colors.textSecondary)
                            } else {
                                // Bold Host in White
                                Text(displayHost)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)
                                    .lineLimit(1)

                                // Dimmed Path in Gray
                                if !urlPath.isEmpty {
                                    Text(urlPath)
                                        .font(.system(size: 13, weight: .regular))
                                        .foregroundColor(colors.textMuted)
                                        .lineLimit(1)
                                        .truncationMode(.tail)
                                }
                            }

                            Spacer(minLength: 0)
                        }
                    }
                    .simultaneousGesture(
                        LongPressGesture().onEnded { _ in
                            handleLongPressCopy()
                        }
                    )

                    // 3. Right Element: Live Shield Counter OR Voice Search
                    if !isInternal && !cleanHost.isEmpty {
                        let blockedCount = state.activeTab?.blockedTrackers ?? 0
                        let isShieldActive = state.isShieldActive && blockedCount > 0
                        let shieldTint = isShieldActive ? colors.statusSuccess : colors.textMuted

                        Button(action: {
                            triggerHaptic()
                            state.showShieldSheet = true
                        }) {
                            HStack(spacing: 4) {
                                Image(systemName: "shield.fill")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(shieldTint)

                                if isShieldActive {
                                    Text("\(blockedCount)")
                                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                                        .foregroundColor(shieldTint)
                                }
                            }
                            .padding(.horizontal, 6)
                            .padding(.vertical, 3)
                            .background(colors.surface)
                            .cornerRadius(AzokleRadius.small)
                        }
                    } else {
                        Button(action: {
                            triggerHaptic()
                            state.showVoiceSearch = true
                        }) {
                            Image(systemName: "mic.fill")
                                .font(.system(size: 14))
                                .foregroundColor(colors.textSecondary)
                                .frame(width: 24, height: 24)
                        }
                    }
                }
                .padding(.horizontal, 10)
                .frame(height: 42)
                .background(colors.surfaceElevated)
                .cornerRadius(AzokleRadius.medium)
                .overlay(
                    RoundedRectangle(cornerRadius: AzokleRadius.medium)
                        .stroke(colors.border, lineWidth: 1)
                )

                // 4. Quick Actions: Reader Mode
                if !isInternal {
                    Button(action: {
                        state.readerModeActive.toggle()
                    }) {
                        Image(systemName: state.readerModeActive ? "book.fill" : "book")
                            .font(.system(size: 15))
                            .foregroundColor(state.readerModeActive ? colors.statusAccent : colors.textSecondary)
                            .frame(width: 28, height: 28)
                    }
                }

                // 5. Quick Actions: Stop or Reload
                if let activeTab = state.activeTab, activeTab.isLoading {
                    Button(action: {
                        state.triggerWebAction(.stop)
                        if let index = state.tabs.firstIndex(where: { $0.id == state.activeTabId }) {
                            state.tabs[index].isLoading = false
                        }
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 28, height: 28)
                    }
                } else if !isInternal {
                    Button(action: {
                        state.reloadActiveTab()
                    }) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 28, height: 28)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(colors.cardBg)

            // 6. Smooth Linear Progress Indicator (2.5pt height)
            if let activeTab = state.activeTab, activeTab.isLoading && activeTab.progress < 1.0 {
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(colors.surfaceElevated)
                        Rectangle()
                            .fill(colors.textPrimary)
                            .frame(width: max(0, geometry.size.width * CGFloat(activeTab.progress)))
                            .animation(.easeOut(duration: 0.18), value: activeTab.progress)
                    }
                }
                .frame(height: 2.5)
            } else {
                Divider()
                    .background(colors.border.opacity(0.8))
            }
        }
    }

    private func triggerHaptic() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }

    private func handleLongPressCopy() {
        guard !activeUrl.isEmpty && !isInternal else { return }
        UIPasteboard.general.string = activeUrl
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()

        withAnimation {
            copiedFeedbackActive = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
            withAnimation {
                self.copiedFeedbackActive = false
            }
        }
    }
}
