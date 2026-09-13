import SwiftUI

// MARK: - Search Suggestions Sheet (1:1 Android SuggestionsSheet.kt Parity)
public struct SuggestionsSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    let colors: AzokleColors = .dark

    let trendingSearches = [
        "Decentralized AI inference",
        "Tor onion circuit security",
        "WebRTC leak mitigation",
        "Zero knowledge proofs",
        "Sovereign browser architecture",
        "FIDO2 biometric passkeys",
        "End to end encrypted messaging"
    ]

    let quickCategories = [
        "AI & Agents", "Privacy & Tor", "Security", "Crypto & Web3", "Open Source"
    ]

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)
                .padding(.bottom, 12)

            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Explore & Suggestions")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Trending topics and sovereign search suggestions")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                Button(action: {
                    isPresented = false
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(colors.textSecondary)
                        .frame(width: 32, height: 32)
                        .background(colors.surfaceElevated)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(colors.border, lineWidth: 1))
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            Divider().background(colors.border)

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // Quick Category Pills
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(quickCategories, id: \.self) { cat in
                                Button(action: {
                                    state.navigateActiveTab(to: cat)
                                    isPresented = false
                                }) {
                                    Text(cat)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundColor(colors.textPrimary)
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 7)
                                        .background(colors.surfaceElevated)
                                        .cornerRadius(AzokleRadius.pill)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                                .stroke(colors.border, lineWidth: 0.8)
                                        )
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                    }
                    .padding(.top, 8)

                    // Trending Topics List
                    VStack(alignment: .leading, spacing: 8) {
                        Text("TRENDING SEARCHES")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.horizontal, 20)

                        VStack(spacing: 4) {
                            ForEach(trendingSearches, id: \.self) { query in
                                Button(action: {
                                    state.navigateActiveTab(to: query)
                                    isPresented = false
                                }) {
                                    HStack(spacing: 12) {
                                        Image(systemName: "arrow.up.right")
                                            .font(.system(size: 12))
                                            .foregroundColor(colors.statusAccent)

                                        Text(query)
                                            .font(.system(size: 14, weight: .medium))
                                            .foregroundColor(colors.textPrimary)

                                        Spacer()
                                    }
                                    .padding(.horizontal, 20)
                                    .padding(.vertical, 12)
                                }
                            }
                        }
                    }
                }
                .padding(.vertical, 12)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.58), .large])
    }
}
