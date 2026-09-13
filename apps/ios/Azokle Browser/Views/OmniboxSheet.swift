import SwiftUI

public struct OmniboxSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool
    @State private var inputText: String = ""
    @FocusState private var isFocused: Bool

    let colors: AzokleColors = .dark

    let quickSuggestions = [
        "https://search.azokle.com",
        "https://duckduckgo.com",
        "https://en.wikipedia.org",
        "https://github.com",
        "https://news.ycombinator.com",
        "https://archive.org"
    ]

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Input Bar
                HStack(spacing: 10) {
                    // Search Engine Icon & Menu
                    Menu {
                        ForEach(SearchEngine.allCases) { engine in
                            Button(action: {
                                state.selectedSearchEngine = engine
                            }) {
                                HStack {
                                    Text(engine.name)
                                    if state.selectedSearchEngine == engine {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(state.selectedSearchEngine.assetName)
                                .resizable()
                                .scaledToFit()
                                .frame(width: 18, height: 18)
                            Image(systemName: "chevron.down")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(colors.textMuted)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 8)
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.small)
                    }

                    // Text Field
                    HStack {
                        TextField("Search or enter website address...", text: $inputText)
                            .font(.system(size: 15, weight: .regular))
                            .foregroundColor(colors.textPrimary)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                            .focused($isFocused)
                            .onSubmit {
                                handleSubmit()
                            }

                        if !inputText.isEmpty {
                            Button(action: {
                                inputText = ""
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 15))
                                    .foregroundColor(colors.textMuted)
                            }
                        }
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(colors.omnibarBg)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Cancel Button
                    Button(action: {
                        isPresented = false
                    }) {
                        Text("Cancel")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(colors.statusAccent)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 12)
                .background(colors.surface)

                Divider()
                    .background(colors.border)

                // 2. Suggestions & Quick Access List
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        // Action suggestions based on input
                        if !inputText.isEmpty {
                            Button(action: {
                                handleSubmit()
                            }) {
                                HStack(spacing: 12) {
                                    Image(systemName: "magnifyingglass")
                                        .font(.system(size: 16))
                                        .foregroundColor(colors.statusAccent)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(inputText)
                                            .font(.system(size: 15, weight: .medium))
                                            .foregroundColor(colors.textPrimary)
                                        Text("Search with \(state.selectedSearchEngine.name)")
                                            .font(.system(size: 12))
                                            .foregroundColor(colors.textSecondary)
                                    }
                                    Spacer()
                                    Image(systemName: "arrow.up.left")
                                        .font(.system(size: 13))
                                        .foregroundColor(colors.textMuted)
                                }
                                .padding(12)
                                .background(colors.surface)
                                .cornerRadius(AzokleRadius.medium)
                            }
                        }

                        // Quick Shortcuts Header
                        Text("QUICK ACCESS")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.top, 8)
                            .padding(.horizontal, 4)

                        // Quick Links
                        ForEach(quickSuggestions, id: \.self) { item in
                            Button(action: {
                                state.navigateActiveTab(to: item)
                                isPresented = false
                            }) {
                                HStack(spacing: 12) {
                                    Image(systemName: "globe")
                                        .font(.system(size: 15))
                                        .foregroundColor(colors.textSecondary)

                                    Text(item.replacingOccurrences(of: "https://", with: ""))
                                        .font(.system(size: 14, weight: .medium))
                                        .foregroundColor(colors.textPrimary)

                                    Spacer()

                                    Image(systemName: "arrow.up.left")
                                        .font(.system(size: 12))
                                        .foregroundColor(colors.textMuted)
                                }
                                .padding(12)
                                .background(colors.surface)
                                .cornerRadius(AzokleRadius.small)
                            }
                        }
                    }
                    .padding(16)
                }
            }
        }
        .onAppear {
            if let activeTab = state.activeTab {
                inputText = activeTab.url
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                isFocused = true
            }
        }
    }

    private func handleSubmit() {
        state.navigateActiveTab(to: inputText)
        isPresented = false
    }
}
