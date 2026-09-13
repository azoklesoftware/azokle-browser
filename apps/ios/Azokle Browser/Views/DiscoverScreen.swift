import SwiftUI

public struct DiscoverScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    let colors: AzokleColors = .dark

    let trendingTopics = [
        ("Tor v3 Onion Services", "Cryptographic routing and decentralized censorship resistance.", "network.badge.shield.half.filled", "https://check.torproject.org"),
        ("Sovereign Search Privacy", "Why zero-logging search engines protect your digital footprint.", "magnifyingglass", "https://search.azokle.com"),
        ("WebRTC Local IP Leaks", "Understanding RFC 8828 and how browsers prevent LAN address exposure.", "lock.shield", "https://browserleaks.com/webrtc"),
        ("Canvas Fingerprinting Defense", "How noise injection neutralizes cross-origin tracking vectors.", "fingerprint", "https://browserleaks.com/canvas")
    ]

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        // Search Bar Shortcut
                        Button(action: {
                            isPresented = false
                            state.showOmniboxSheet = true
                        }) {
                            HStack {
                                Image(systemName: "magnifyingglass")
                                    .foregroundColor(colors.textMuted)
                                Text("Search with Azokle...")
                                    .foregroundColor(colors.textMuted)
                                Spacer()
                            }
                            .padding(12)
                            .background(colors.surfaceElevated)
                            .cornerRadius(AzokleRadius.medium)
                            .overlay(
                                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                    .stroke(colors.border, lineWidth: 1)
                            )
                        }

                        Text("DISCOVER & PRIVACY RADAR")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.top, 4)

                        // Trending Cards
                        ForEach(trendingTopics, id: \.0) { topic in
                            Button(action: {
                                state.navigateActiveTab(to: topic.3)
                                isPresented = false
                            }) {
                                HStack(alignment: .top, spacing: 14) {
                                    Image(systemName: topic.2)
                                        .font(.system(size: 24))
                                        .foregroundColor(colors.statusAccent)
                                        .frame(width: 32)
                                        .padding(.top, 2)

                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(topic.0)
                                            .font(.system(size: 15, weight: .semibold))
                                            .foregroundColor(colors.textPrimary)
                                            .multilineTextAlignment(.leading)

                                        Text(topic.1)
                                            .font(.system(size: 13))
                                            .foregroundColor(colors.textSecondary)
                                            .multilineTextAlignment(.leading)
                                    }

                                    Spacer()

                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 12))
                                        .foregroundColor(colors.textMuted)
                                        .padding(.top, 4)
                                }
                                .padding(16)
                                .background(colors.surface)
                                .cornerRadius(AzokleRadius.medium)
                                .overlay(
                                    RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                        .stroke(colors.border, lineWidth: 1)
                                )
                            }
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle("Discover")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                    .foregroundColor(colors.statusAccent)
                }
            }
        }
    }
}
