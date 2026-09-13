import SwiftUI

// MARK: - Discover Category
public enum DiscoverCategory: String, CaseIterable, Identifiable {
    case all = "All"
    case ai = "AI & Tools"
    case streaming = "Media & Video"
    case gaming = "Gaming"
    case shopping = "Shopping"
    case social = "Social"
    case web3 = "Web3 & Tor"
    case tech = "News & Tech"

    public var id: String { rawValue }
}

// MARK: - Discover Portal Model
public struct DiscoverPortal: Identifiable, Hashable {
    public let id = UUID()
    public let title: String
    public let domain: String
    public let url: String
    public let category: DiscoverCategory
    public let description: String
    public let iconName: String?
}

// MARK: - Discover Screen (Mirroring Android DiscoverScreen.kt)
public struct DiscoverScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var searchQuery: String = ""
    @State private var selectedCategory: DiscoverCategory = .all
    @State private var favoriteIds: Set<UUID> = []

    let colors: AzokleColors = .dark

    // Quick Access Dock Items (#1 ChatGPT, #2 Spotify, #3 Amazon, #4 YouTube)
    let quickDockItems: [DiscoverPortal] = [
        DiscoverPortal(title: "ChatGPT", domain: "chatgpt.com", url: "https://chatgpt.com", category: .ai, description: "Advanced conversational AI by OpenAI", iconName: "brain.head.profile"),
        DiscoverPortal(title: "Spotify", domain: "spotify.com", url: "https://open.spotify.com", category: .streaming, description: "Digital music, podcast, and video service", iconName: "music.note"),
        DiscoverPortal(title: "Amazon", domain: "amazon.com", url: "https://www.amazon.com", category: .shopping, description: "Global e-commerce and cloud computing", iconName: "cart.fill"),
        DiscoverPortal(title: "YouTube", domain: "youtube.com", url: "https://www.youtube.com", category: .streaming, description: "Video sharing and streaming platform", iconName: "play.rectangle.fill")
    ]

    // All Portals Feed
    let allPortals: [DiscoverPortal] = [
        DiscoverPortal(title: "ChatGPT", domain: "chatgpt.com", url: "https://chatgpt.com", category: .ai, description: "Leading conversational AI for assistance, coding, and writing.", iconName: "brain.head.profile"),
        DiscoverPortal(title: "Spotify", domain: "spotify.com", url: "https://open.spotify.com", category: .streaming, description: "Stream millions of songs and podcasts on the go.", iconName: "music.note"),
        DiscoverPortal(title: "Amazon", domain: "amazon.com", url: "https://www.amazon.com", category: .shopping, description: "Online shopping for books, electronics, apparel, and more.", iconName: "cart.fill"),
        DiscoverPortal(title: "YouTube", domain: "youtube.com", url: "https://www.youtube.com", category: .streaming, description: "Watch and share trending videos across the globe.", iconName: "play.rectangle.fill"),
        DiscoverPortal(title: "Claude AI", domain: "claude.ai", url: "https://claude.ai", category: .ai, description: "Next-generation AI assistant built by Anthropic.", iconName: "sparkles"),
        DiscoverPortal(title: "Twitch", domain: "twitch.tv", url: "https://www.twitch.tv", category: .gaming, description: "Interactive live streaming for gaming, sports, and entertainment.", iconName: "gamecontroller.fill"),
        DiscoverPortal(title: "GitHub", domain: "github.com", url: "https://github.com", category: .tech, description: "The world's leading platform for developer collaboration.", iconName: "chevron.left.forwardslash.chevron.right"),
        DiscoverPortal(title: "Reddit", domain: "reddit.com", url: "https://www.reddit.com", category: .social, description: "Community forums, discussions, and internet culture.", iconName: "bubble.left.and.bubble.right.fill"),
        DiscoverPortal(title: "Tor Project", domain: "torproject.org", url: "https://check.torproject.org", category: .web3, description: "Defend against tracking, surveillance, and censorship.", iconName: "network.badge.shield.half.filled"),
        DiscoverPortal(title: "Hacker News", domain: "news.ycombinator.com", url: "https://news.ycombinator.com", category: .tech, description: "Computer science, entrepreneurship, and tech news.", iconName: "newspaper.fill"),
        DiscoverPortal(title: "Wikipedia", domain: "wikipedia.org", url: "https://www.wikipedia.org", category: .all, description: "The free encyclopedia that anyone can edit.", iconName: "book.fill"),
        DiscoverPortal(title: "Steam", domain: "store.steampowered.com", url: "https://store.steampowered.com", category: .gaming, description: "The ultimate destination for playing and discussing games.", iconName: "gamecontroller")
    ]

    private var filteredPortals: [DiscoverPortal] {
        allPortals.filter { portal in
            let matchesCategory = (selectedCategory == .all) || (portal.category == selectedCategory)
            let matchesQuery = searchQuery.isEmpty ||
                portal.title.localizedCaseInsensitiveContains(searchQuery) ||
                portal.domain.localizedCaseInsensitiveContains(searchQuery) ||
                portal.description.localizedCaseInsensitiveContains(searchQuery)
            return matchesCategory && matchesQuery
        }
    }

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                VStack(spacing: 0) {
                    // 1. Integrated Search Bar Header
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 16))
                                .foregroundColor(colors.textSecondary)

                            TextField("Search portals, AI, games, music, shopping...", text: $searchQuery)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(colors.textPrimary)
                                .autocapitalization(.none)
                                .disableAutocorrection(true)

                            if !searchQuery.isEmpty {
                                Button(action: {
                                    searchQuery = ""
                                }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .font(.system(size: 15))
                                        .foregroundColor(colors.textMuted)
                                }
                            }
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(colors.cardBg)
                        .cornerRadius(AzokleRadius.medium)
                        .overlay(
                            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                .stroke(colors.border, lineWidth: 1)
                        )
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 10)
                    .padding(.bottom, 8)

                    // 2. Category Filter Pills (Horizontal Scroll)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(DiscoverCategory.allCases) { category in
                                categoryPill(category)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 6)
                    }

                    Divider()
                        .background(colors.border)

                    // 3. Content: Quick Access Dock + Portals Feed
                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            // Quick Access Dock (shown when in "All" view with empty search)
                            if selectedCategory == .all && searchQuery.isEmpty {
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack {
                                        Text("QUICK ACCESS")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(colors.textMuted)
                                        Spacer()
                                        Text("\(quickDockItems.count) PORTALS")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(colors.textMuted)
                                    }

                                    // 4-Column Quick Access Dock
                                    HStack(spacing: 12) {
                                        ForEach(quickDockItems) { item in
                                            quickDockTile(item)
                                        }
                                    }
                                    .padding(14)
                                    .background(colors.cardBg)
                                    .cornerRadius(AzokleRadius.medium)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                            .stroke(colors.border, lineWidth: 1)
                                    )
                                }
                                .padding(.top, 8)
                            }

                            // Section Eyebrow
                            HStack {
                                Text(sectionTitle)
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(colors.textMuted)
                                Spacer()
                                Text("\(filteredPortals.count) RESULTS")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(colors.textMuted)
                            }
                            .padding(.top, 4)

                            // Portals Feed
                            if filteredPortals.isEmpty {
                                VStack(spacing: 12) {
                                    Spacer().frame(height: 40)
                                    Image(systemName: "magnifyingglass")
                                        .font(.system(size: 40))
                                        .foregroundColor(colors.textMuted)
                                    Text("No portals found")
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundColor(colors.textPrimary)
                                    Text("Try searching for AI, games, music, or shopping.")
                                        .font(.system(size: 13))
                                        .foregroundColor(colors.textSecondary)
                                    Spacer().frame(height: 40)
                                }
                                .frame(maxWidth: .infinity)
                            } else {
                                VStack(spacing: 10) {
                                    ForEach(filteredPortals) { portal in
                                        luxuryPortalCard(portal)
                                    }
                                }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .navigationTitle("Discover")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(colors.statusAccent)
                }
            }
        }
    }

    private var sectionTitle: String {
        if !searchQuery.isEmpty {
            return "SEARCH RESULTS"
        } else if selectedCategory == .all {
            return "ALL PORTALS"
        } else {
            return selectedCategory.rawValue.uppercased()
        }
    }

    private func categoryPill(_ category: DiscoverCategory) -> some View {
        let isSelected = selectedCategory == category
        return Button(action: {
            selectedCategory = category
        }) {
            Text(category.rawValue)
                .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                .foregroundColor(isSelected ? colors.actionPrimaryText : colors.textSecondary)
                .padding(.horizontal, 14)
                .padding(.vertical, 7)
                .background(isSelected ? colors.actionPrimaryBg : colors.cardBg)
                .cornerRadius(AzokleRadius.pill)
                .overlay(
                    RoundedRectangle(cornerRadius: AzokleRadius.pill)
                        .stroke(isSelected ? Color.clear : colors.border, lineWidth: 1)
                )
        }
    }

    private func quickDockTile(_ item: DiscoverPortal) -> some View {
        Button(action: {
            state.navigateActiveTab(to: item.url)
            isPresented = false
        }) {
            VStack(spacing: 6) {
                ZStack {
                    RoundedRectangle(cornerRadius: 14)
                        .fill(colors.background)
                        .frame(width: 48, height: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(colors.border, lineWidth: 1)
                        )

                    if let icon = item.iconName {
                        Image(systemName: icon)
                            .font(.system(size: 20))
                            .foregroundColor(colors.statusAccent)
                    } else {
                        Image(systemName: "globe")
                            .font(.system(size: 18))
                            .foregroundColor(colors.textSecondary)
                    }
                }

                Text(item.title)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(colors.textPrimary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
        }
    }

    private func luxuryPortalCard(_ item: DiscoverPortal) -> some View {
        let isFavorite = favoriteIds.contains(item.id)

        return Button(action: {
            state.navigateActiveTab(to: item.url)
            isPresented = false
        }) {
            HStack(spacing: 14) {
                // Favicon Squircle
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(colors.surfaceElevated)
                        .frame(width: 44, height: 44)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(colors.border, lineWidth: 1)
                        )

                    if let icon = item.iconName {
                        Image(systemName: icon)
                            .font(.system(size: 20))
                            .foregroundColor(colors.statusAccent)
                    } else {
                        Image(systemName: "globe")
                            .font(.system(size: 18))
                            .foregroundColor(colors.textSecondary)
                    }
                }

                // Portal Details
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 8) {
                        Text(item.title)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(colors.textPrimary)

                        Text(item.category.rawValue)
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(colors.statusAccent)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(colors.statusAccent.opacity(0.12))
                            .cornerRadius(4)
                    }

                    Text(item.description)
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 0)

                // Favorite Heart Button
                Button(action: {
                    if isFavorite {
                        favoriteIds.remove(item.id)
                    } else {
                        favoriteIds.insert(item.id)
                    }
                }) {
                    Image(systemName: isFavorite ? "heart.fill" : "heart")
                        .font(.system(size: 16))
                        .foregroundColor(isFavorite ? colors.statusDestructive : colors.textMuted)
                        .padding(8)
                }
            }
            .padding(14)
            .background(colors.cardBg)
            .cornerRadius(AzokleRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(colors.border, lineWidth: 1)
            )
        }
    }
}
