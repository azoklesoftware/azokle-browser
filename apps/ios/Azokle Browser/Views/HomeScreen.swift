import SwiftUI

// MARK: - Browser Shortcut Model
public struct BrowserShortcut: Identifiable, Hashable {
    public let id = UUID()
    public let label: String
    public let url: String
    public let iconName: String?
    public let faviconUrl: String?
    public let isSponsored: Bool

    public init(label: String, url: String, iconName: String? = nil, faviconUrl: String? = nil, isSponsored: Bool = false) {
        self.label = label
        self.url = url
        self.iconName = iconName
        self.faviconUrl = faviconUrl
        self.isSponsored = isSponsored
    }
}

// MARK: - Sovereign Browser Home Screen (Mirroring Android BrowserHomeScreenContent)
public struct HomeScreen: View {
    @ObservedObject var state: BrowserState
    let colors: AzokleColors = .dark

    let shortcuts: [BrowserShortcut] = [
        BrowserShortcut(label: "Wikipedia", url: "https://www.wikipedia.org", faviconUrl: "https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128"),
        BrowserShortcut(label: "YouTube", url: "https://www.youtube.com", faviconUrl: "https://www.google.com/s2/favicons?domain=youtube.com&sz=128"),
        BrowserShortcut(label: "Amazon", url: "https://www.amazon.com", faviconUrl: "https://www.google.com/s2/favicons?domain=amazon.com&sz=128", isSponsored: true),
        BrowserShortcut(label: "Trip", url: "https://www.trip.com", faviconUrl: "https://www.google.com/s2/favicons?domain=trip.com&sz=128", isSponsored: true),
        BrowserShortcut(label: "Reddit", url: "https://www.reddit.com", faviconUrl: "https://www.google.com/s2/favicons?domain=reddit.com&sz=128"),
        BrowserShortcut(label: "Azokle Auth", url: "https://azokle.com/authenticator", iconName: "ic_azokle_auth"),
        BrowserShortcut(label: "Kick", url: "https://kick.com", faviconUrl: "https://www.google.com/s2/favicons?domain=kick.com&sz=128"),
        BrowserShortcut(label: "Azokle", url: "https://search.azokle.com", iconName: "ic_engine_azokle")
    ]

    public var body: some View {
        ZStack {
            // 1. High-Resolution Photographic Wallpaper Backdrop (Android WallpaperManager Parity)
            colors.background.ignoresSafeArea()

            Image("bg_home_1")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()

            // Status Bar & Navigation Scrims
            VStack {
                // Top Status Bar Scrim (100dp)
                LinearGradient(
                    colors: [
                        Color.black.opacity(0.60),
                        Color.black.opacity(0.20),
                        Color.clear
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 110)
                .ignoresSafeArea()

                Spacer()

                // Bottom Navigation Scrim (140dp)
                LinearGradient(
                    colors: [
                        Color.clear,
                        Color.black.opacity(0.35),
                        Color.black.opacity(0.70)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 150)
                .ignoresSafeArea()
            }
            .ignoresSafeArea()

            // 2. Center Content Stack
            VStack(spacing: 0) {
                Spacer()

                // Audiowide Brand Logo in Lowercase (Exact match to Android)
                Text("azokle")
                    .font(.audiowide(size: 52))
                    .foregroundColor(.white)
                    .tracking(-1.0)
                    .shadow(color: Color.black.opacity(0.5), radius: 8, x: 0, y: 4)

                Spacer().frame(height: 24)

                // Search Bar Capsule
                Button(action: {
                    state.showOmniboxSheet = true
                }) {
                    HStack(spacing: 10) {
                        // Search Engine Indicator
                        HStack(spacing: 4) {
                            if let asset = state.selectedSearchEngine.assetName {
                                Image(asset)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 18, height: 18)
                            } else {
                                Image(systemName: "magnifyingglass")
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(colors.statusAccent)
                            }
                            Image(systemName: "chevron.down")
                                .font(.system(size: 8, weight: .bold))
                                .foregroundColor(colors.textMuted)
                        }
                        .padding(.leading, 6)

                        Text("Search or enter address")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(colors.textSecondary)

                        Spacer()

                        Image(systemName: "mic.fill")
                            .font(.system(size: 16))
                            .foregroundColor(colors.textSecondary)
                            .padding(.trailing, 6)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(colors.surfaceElevated.opacity(0.85))
                    .cornerRadius(AzokleRadius.pill)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.pill)
                            .stroke(colors.borderLight, lineWidth: 1)
                    )
                    .shadow(color: Color.black.opacity(0.35), radius: 10, x: 0, y: 5)
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 32)

                // 3. Quick Speed Dial Shortcuts (2 Rows x 4 Columns)
                let chunked = shortcuts.chunked(into: 4)
                VStack(spacing: 16) {
                    ForEach(0..<chunked.count, id: \.self) { rowIndex in
                        HStack(spacing: 16) {
                            ForEach(chunked[rowIndex]) { shortcut in
                                shortcutTile(shortcut)
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)

                Spacer()
            }
        }
    }

    private func shortcutTile(_ shortcut: BrowserShortcut) -> some View {
        Button(action: {
            state.navigateActiveTab(to: shortcut.url)
        }) {
            VStack(spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(colors.surfaceElevated.opacity(0.85))
                        .frame(width: 54, height: 54)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(colors.borderLight, lineWidth: 1)
                        )
                        .shadow(color: Color.black.opacity(0.25), radius: 6, x: 0, y: 3)

                    if let iconName = shortcut.iconName {
                        if UIImage(named: iconName) != nil {
                            Image(iconName)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 28, height: 28)
                        } else {
                            Image(systemName: iconName)
                                .font(.system(size: 22))
                                .foregroundColor(colors.statusAccent)
                        }
                    } else if let faviconUrl = shortcut.faviconUrl, let url = URL(string: faviconUrl) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable()
                                    .scaledToFit()
                                    .frame(width: 26, height: 26)
                                    .clipShape(RoundedRectangle(cornerRadius: 6))
                            default:
                                Image(systemName: "globe")
                                    .font(.system(size: 20))
                                    .foregroundColor(colors.textSecondary)
                            }
                        }
                    } else {
                        Image(systemName: "globe")
                            .font(.system(size: 20))
                            .foregroundColor(colors.textSecondary)
                    }
                }

                Text(shortcut.label)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(colors.textPrimary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

// MARK: - Array Chunk Extension
extension Array {
    func chunked(into size: Int) -> [[Element]] {
        var chunks: [[Element]] = []
        for index in stride(from: 0, to: count, by: size) {
            let chunk = Array(self[index..<Swift.min(index + size, count)])
            chunks.append(chunk)
        }
        return chunks
    }
}
