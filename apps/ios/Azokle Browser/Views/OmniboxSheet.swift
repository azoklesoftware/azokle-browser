import SwiftUI

// MARK: - Sovereign Omnibox Command Center (1:1 Android BrowserUrlEditSheet.kt Parity)
public struct OmniboxSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var inputText: String = ""
    @State private var suggestions: [String] = []
    @State private var isLoadingSuggestions: Bool = false
    @State private var debounceTask: Task<Void, Never>? = nil
    @State private var clipboardUrl: String? = nil
    @State private var recentSearches: [String] = []
    @FocusState private var isFocused: Bool

    let colors: AzokleColors = .dark

    let fallbackShortcuts = [
        "https://search.azokle.com",
        "https://duckduckgo.com",
        "https://en.wikipedia.org",
        "https://github.com",
        "https://news.ycombinator.com",
        "https://archive.org"
    ]

    private var isUrl: Bool {
        isDirectUrl(inputText)
    }

    public init(state: BrowserState, isPresented: Binding<Bool>) {
        self.state = state
        self._isPresented = isPresented
    }

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Omnibar Capsule Bar
                HStack(spacing: 8) {
                    // Omnibar Capsule (Search Engine Dropdown + Input + Clear + Mic)
                    HStack(spacing: 8) {
                        // Search Engine Selector Menu
                        SearchEngineDropdown(
                            state: state,
                            iconSize: 20,
                            showDropdownArrow: true
                        )
                        .padding(.leading, 2)

                        // Main Text Field
                        TextField("Search \(state.selectedSearchEngine.name) or type URL", text: $inputText)
                            .font(.system(size: 15, weight: .regular))
                            .foregroundColor(colors.textPrimary)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                            .keyboardType(isUrl ? .URL : .default)
                            .focused($isFocused)
                            .onSubmit {
                                handleSubmit(inputText)
                            }
                            .onChange(of: inputText) { newValue in
                                handleQueryChange(newValue)
                            }

                        // Clear Button
                        if !inputText.isEmpty {
                            Button(action: {
                                triggerLightHaptic()
                                inputText = ""
                                suggestions = []
                                isLoadingSuggestions = false
                            }) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 16))
                                    .foregroundColor(colors.textMuted)
                            }
                            .frame(width: 28, height: 28)
                        }

                        // Voice Search Button
                        Button(action: {
                            triggerLightHaptic()
                            isPresented = false
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                                state.showVoiceSearch = true
                            }
                        }) {
                            Image(systemName: "mic.fill")
                                .font(.system(size: 16))
                                .foregroundColor(colors.textSecondary)
                        }
                        .frame(width: 28, height: 28)
                        .accessibilityLabel("Voice search")
                    }
                    .padding(.horizontal, 10)
                    .frame(height: 48)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Cancel Action
                    Button(action: {
                        isPresented = false
                    }) {
                        Text("Cancel")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(colors.statusAccent)
                            .padding(.horizontal, 4)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 14)
                .padding(.bottom, 10)
                .background(colors.surface)

                // Clipboard Link Instant Pill (1-Tap Direct Navigation)
                if let clipLink = clipboardUrl {
                    Button(action: {
                        triggerLightHaptic()
                        handleSubmit(clipLink)
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "doc.on.clipboard.fill")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(colors.statusAccent)

                            Text("Paste & Go:")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(colors.statusAccent)

                            Text(cleanDomain(for: clipLink))
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(colors.textPrimary)
                                .lineLimit(1)
                                .truncationMode(.tail)

                            Spacer(minLength: 0)

                            Image(systemName: "arrow.up.right")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(colors.statusAccent)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(colors.statusAccent.opacity(0.12))
                        .cornerRadius(AzokleRadius.pill)
                        .overlay(
                            RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                .stroke(colors.statusAccent.opacity(0.35), lineWidth: 1)
                        )
                    }
                    .buttonStyle(PlainButtonStyle())
                    .padding(.horizontal, 14)
                    .padding(.top, 4)
                    .padding(.bottom, 6)
                }

                Divider()
                    .background(colors.border.opacity(0.8))

                // 2. Dynamic Discovery Feed (Suggestions / Recents / Bookmarks)
                ScrollView {
                    LazyVStack(spacing: 8) {
                        let cleanText = inputText.trimmingCharacters(in: .whitespacesAndNewlines)

                        if !cleanText.isEmpty {
                            // Primary Direct Action Card
                            PrimaryDirectActionCard(
                                isUrl: isUrl,
                                text: cleanText,
                                engineName: state.selectedSearchEngine.name,
                                colors: colors,
                                onClick: {
                                    handleSubmit(cleanText)
                                }
                            )

                            // Live Autosuggestions
                            ForEach(suggestions, id: \.self) { suggestion in
                                SuggestionRowCard(
                                    icon: "magnifyingglass",
                                    title: suggestion,
                                    subtitle: "Web Search Suggestion",
                                    categoryBadge: "Search",
                                    colors: colors,
                                    onClick: {
                                        handleSubmit(suggestion)
                                    },
                                    onFill: {
                                        triggerLightHaptic()
                                        inputText = suggestion
                                        handleQueryChange(suggestion)
                                    }
                                )
                            }

                            if isLoadingSuggestions {
                                HStack {
                                    Spacer()
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: colors.textPrimary))
                                        .padding(.vertical, 12)
                                    Spacer()
                                }
                            }
                        } else {
                            // Recent Searches Section
                            if !recentSearches.isEmpty {
                                HStack {
                                    Text("RECENT SEARCHES")
                                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                                        .foregroundColor(colors.textMuted)
                                        .tracking(1.0)

                                    Spacer()

                                    Button(action: {
                                        triggerLightHaptic()
                                        clearRecentSearches()
                                    }) {
                                        Text("Clear")
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(colors.textMuted)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 4)
                                    }
                                }
                                .padding(.horizontal, 4)
                                .padding(.top, 10)

                                ForEach(recentSearches, id: \.self) { recent in
                                    RecentSearchRowCard(
                                        text: recent,
                                        colors: colors,
                                        onClick: {
                                            handleSubmit(recent)
                                        },
                                        onRemove: {
                                            removeRecentSearch(recent)
                                        }
                                    )
                                }
                            }

                            // Saved Bookmarks Section
                            if !state.bookmarks.isEmpty {
                                HStack {
                                    Text("SAVED BOOKMARKS")
                                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                                        .foregroundColor(colors.textMuted)
                                        .tracking(1.0)
                                    Spacer()
                                }
                                .padding(.horizontal, 4)
                                .padding(.top, recentSearches.isEmpty ? 10 : 16)

                                ForEach(state.bookmarks.prefix(5)) { bookmark in
                                    SuggestionRowCard(
                                        icon: "bookmark.fill",
                                        title: bookmark.title,
                                        subtitle: bookmark.url,
                                        categoryBadge: "Bookmark",
                                        colors: colors,
                                        onClick: {
                                            handleSubmit(bookmark.url)
                                        },
                                        onFill: nil
                                    )
                                }
                            }

                            // Sovereign Quick Shortcuts
                            HStack {
                                Text("QUICK ACCESS")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                                    .foregroundColor(colors.textMuted)
                                    .tracking(1.0)
                                Spacer()
                            }
                            .padding(.horizontal, 4)
                            .padding(.top, 16)

                            ForEach(fallbackShortcuts, id: \.self) { item in
                                SuggestionRowCard(
                                    icon: "globe",
                                    title: cleanDomain(for: item),
                                    subtitle: item,
                                    categoryBadge: nil,
                                    colors: colors,
                                    onClick: {
                                        handleSubmit(item)
                                    },
                                    onFill: nil
                                )
                            }
                        }
                    }
                    .padding(14)
                }
            }
        }
        .onAppear {
            loadRecentSearches()
            if let activeTab = state.activeTab, !activeTab.url.isEmpty, activeTab.url != "https://search.azokle.com" && !activeTab.url.hasPrefix("about:") {
                inputText = activeTab.url
            }

            checkClipboardForUrl()

            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                isFocused = true
            }
        }
    }

    // MARK: - Navigation & Query Actions
    private func handleSubmit(_ rawTarget: String) {
        let trimmed = rawTarget.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        saveRecentSearch(trimmed)
        state.navigateActiveTab(to: trimmed)
        isPresented = false
    }

    private func handleQueryChange(_ query: String) {
        debounceTask?.cancel()
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)

        if trimmed.count < 2 || isDirectUrl(trimmed) {
            suggestions = []
            isLoadingSuggestions = false
            return
        }

        debounceTask = Task {
            try? await Task.sleep(nanoseconds: 140_000_000) // 140ms debounce
            if Task.isCancelled { return }

            await MainActor.run {
                self.isLoadingSuggestions = true
            }

            let results = await fetchAutosuggestions(query: trimmed)

            if !Task.isCancelled {
                await MainActor.run {
                    self.suggestions = results
                    self.isLoadingSuggestions = false
                }
            }
        }
    }

    private func fetchAutosuggestions(query: String) async -> [String] {
        guard let url = URL(string: "https://search.azokle.com/api/v1/autosuggest") else { return [] }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 4.0

        let payload: [String: String] = ["query": query]
        guard let httpBody = try? JSONSerialization.data(withJSONObject: payload) else { return [] }
        request.httpBody = httpBody

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                return []
            }
            if let json = try? JSONSerialization.jsonObject(with: data) as? [Any], json.count >= 2, let array = json[1] as? [String] {
                return array
            }
        } catch {
            // Graceful fallback on network error or offline
        }
        return []
    }

    // MARK: - Clipboard Detection
    private func checkClipboardForUrl() {
        guard let clip = UIPasteboard.general.string?.trimmingCharacters(in: .whitespacesAndNewlines),
              !clip.isEmpty,
              isDirectUrl(clip),
              clip != (state.activeTab?.url ?? "") else {
            return
        }
        self.clipboardUrl = clip
    }

    // MARK: - Recent Searches Persistence
    private func loadRecentSearches() {
        self.recentSearches = UserDefaults.standard.stringArray(forKey: "azokle_recent_searches") ?? []
    }

    private func saveRecentSearch(_ query: String) {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        var list = UserDefaults.standard.stringArray(forKey: "azokle_recent_searches") ?? []
        list.removeAll { $0.caseInsensitiveCompare(trimmed) == .orderedSame }
        list.insert(trimmed, at: 0)
        if list.count > 15 {
            list = Array(list.prefix(15))
        }
        UserDefaults.standard.set(list, forKey: "azokle_recent_searches")
        self.recentSearches = list
    }

    private func clearRecentSearches() {
        UserDefaults.standard.removeObject(forKey: "azokle_recent_searches")
        self.recentSearches = []
    }

    private func removeRecentSearch(_ query: String) {
        var list = self.recentSearches
        list.removeAll { $0 == query }
        UserDefaults.standard.set(list, forKey: "azokle_recent_searches")
        self.recentSearches = list
    }

    // MARK: - Helpers
    private func isDirectUrl(_ text: String) -> Bool {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return false }
        if trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") || trimmed.hasPrefix("about:") {
            return true
        }
        if trimmed.contains(" ") { return false }
        let parts = trimmed.split(separator: ".")
        return parts.count >= 2 && !parts.last!.isEmpty
    }

    private func cleanDomain(for urlString: String) -> String {
        guard let url = URL(string: urlString), let host = url.host else {
            return urlString.replacingOccurrences(of: "https://", with: "").replacingOccurrences(of: "http://", with: "")
        }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    private func triggerLightHaptic() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }
}

// MARK: - Primary Direct Action Card
private struct PrimaryDirectActionCard: View {
    let isUrl: Bool
    let text: String
    let engineName: String
    let colors: AzokleColors
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(isUrl ? colors.statusAccent.opacity(0.15) : colors.surfaceElevated)
                        .frame(width: 38, height: 38)

                    Image(systemName: isUrl ? "globe" : "magnifyingglass")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(isUrl ? colors.statusAccent : colors.textPrimary)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(isUrl ? "Visit Website" : "Search \(engineName)")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(isUrl ? colors.statusAccent : colors.textMuted)

                    Text(text)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.textPrimary)
                        .lineLimit(1)
                }

                Spacer()

                Image(systemName: "arrow.up.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(colors.textMuted)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(colors.surface)
            .cornerRadius(AzokleRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(colors.borderLight, lineWidth: 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Suggestion & Bookmark Row Card
private struct SuggestionRowCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let categoryBadge: String?
    let colors: AzokleColors
    let onClick: () -> Void
    let onFill: (() -> Void)?

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onClick) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(colors.surface)
                            .frame(width: 32, height: 32)

                        Image(systemName: icon)
                            .font(.system(size: 14))
                            .foregroundColor(colors.textSecondary)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 6) {
                            Text(title)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(colors.textPrimary)
                                .lineLimit(1)

                            if let badge = categoryBadge {
                                Text(badge)
                                    .font(.system(size: 9.5, weight: .semibold))
                                    .foregroundColor(colors.textMuted)
                                    .padding(.horizontal, 5)
                                    .padding(.vertical, 1)
                                    .background(colors.surfaceElevated)
                                    .cornerRadius(4)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 4)
                                            .stroke(colors.border, lineWidth: 0.5)
                                    )
                            }
                        }

                        Text(subtitle)
                            .font(.system(size: 11))
                            .foregroundColor(colors.textMuted)
                            .lineLimit(1)
                    }

                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(PlainButtonStyle())

            if let onFill = onFill {
                Button(action: onFill) {
                    Image(systemName: "arrow.up.left")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(colors.textMuted)
                        .frame(width: 36, height: 36)
                        .background(colors.surface.opacity(0.6))
                        .clipShape(Circle())
                }
                .buttonStyle(PlainButtonStyle())
                .accessibilityLabel("Fill into search bar")
            } else {
                Button(action: onClick) {
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textMuted)
                        .frame(width: 28, height: 28)
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(colors.cardBg)
        .cornerRadius(AzokleRadius.small)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.small)
                .stroke(colors.border, lineWidth: 1)
        )
    }
}

// MARK: - Recent Search Row Card
private struct RecentSearchRowCard: View {
    let text: String
    let colors: AzokleColors
    let onClick: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onClick) {
                HStack(spacing: 12) {
                    Image(systemName: "clock.arrow.circlepath")
                        .font(.system(size: 15))
                        .foregroundColor(colors.textMuted)

                    Text(text)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(colors.textPrimary)
                        .lineLimit(1)

                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(PlainButtonStyle())

            Button(action: onRemove) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(colors.textMuted)
                    .frame(width: 32, height: 32)
                    .background(colors.surface.opacity(0.5))
                    .clipShape(Circle())
            }
            .buttonStyle(PlainButtonStyle())
            .accessibilityLabel("Remove from recent searches")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(colors.cardBg)
        .cornerRadius(AzokleRadius.small)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.small)
                .stroke(colors.border, lineWidth: 1)
        )
    }
}
