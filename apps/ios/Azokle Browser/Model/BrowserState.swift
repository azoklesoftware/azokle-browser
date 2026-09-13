import Foundation
import SwiftUI
import Combine
import WebKit

// MARK: - Search Engine Options (Strict Parity with Android SEARCH_ENGINE_PROVIDERS)
public enum SearchEngine: String, CaseIterable, Identifiable {
    case azokle = "Azokle Search"
    case duckduckgo = "DuckDuckGo"
    case brave = "Brave Search"
    case startpage = "Startpage"
    case mojeek = "Mojeek"
    case qwant = "Qwant"

    public var id: String {
        switch self {
        case .azokle: return "azokle"
        case .duckduckgo: return "duckduckgo"
        case .brave: return "brave"
        case .startpage: return "startpage"
        case .mojeek: return "mojeek"
        case .qwant: return "qwant"
        }
    }

    public var name: String { rawValue }

    public var domain: String {
        switch self {
        case .azokle: return "search.azokle.com"
        case .duckduckgo: return "duckduckgo.com"
        case .brave: return "search.brave.com"
        case .startpage: return "startpage.com"
        case .mojeek: return "mojeek.com"
        case .qwant: return "qwant.com"
        }
    }

    public var subtitle: String {
        switch self {
        case .azokle: return "Decentralized AI & sovereign privacy search"
        case .duckduckgo: return "Privacy, simplified. No profiling or tracker tracking"
        case .brave: return "Independent search index with complete privacy"
        case .startpage: return "Private search engine with Google-grade indexing"
        case .mojeek: return "Independent web crawler with complete search privacy"
        case .qwant: return "European privacy search with zero profiling or ad-tracking"
        }
    }

    public var queryUrl: String {
        switch self {
        case .azokle: return "https://search.azokle.com/search?q="
        case .duckduckgo: return "https://duckduckgo.com/?q="
        case .brave: return "https://search.brave.com/search?q="
        case .startpage: return "https://www.startpage.com/sp/search?query="
        case .mojeek: return "https://www.mojeek.com/search?q="
        case .qwant: return "https://www.qwant.com/?q="
        }
    }

    public func searchUrl(for query: String) -> String {
        let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
        return "\(queryUrl)\(encoded)"
    }

    public var assetName: String {
        switch self {
        case .azokle: return "ic_engine_azokle"
        case .duckduckgo: return "ic_engine_duckduckgo"
        case .brave: return "ic_engine_brave"
        case .startpage: return "ic_engine_startpage"
        case .mojeek: return "ic_engine_mojeek"
        case .qwant: return "ic_engine_qwant"
        }
    }
}

// MARK: - Tab Filter Mode
public enum TabFilterMode: String, CaseIterable {
    case standard = "Standard"
    case privateMode = "Private"
    case torMode = "Tor Circuit"
}

// MARK: - Tor Connection State
public enum TorConnectionState: String {
    case disconnected = "Disconnected"
    case connecting = "Connecting Hops..."
    case connected = "Circuits Secured"
    case rotating = "Rotating Circuit..."
}

// MARK: - History & Bookmark Items
public struct HistoryItem: Identifiable, Hashable {
    public let id = UUID()
    public let url: String
    public let title: String
    public let timestamp: Date
}

public struct BookmarkItem: Identifiable, Hashable {
    public let id = UUID()
    public let url: String
    public let title: String
    public let dateAdded: Date
}

public struct DownloadItem: Identifiable, Hashable {
    public let id = UUID()
    public let fileName: String
    public let url: String
    public let fileSize: String
    public let date: Date
}

// MARK: - Main Sovereign Browser State
@MainActor
public class BrowserState: ObservableObject {
    // Tabs
    @Published public var tabs: [BrowserTab] = []
    @Published public var activeTabId: UUID = UUID()
    @Published public var selectedTabMode: TabFilterMode = .standard

    // Protection & Privacy
    @Published public var isShieldActive: Bool = true
    @Published public var isTorCircuitActive: Bool = false
    @Published public var torStatus: TorConnectionState = .connected
    @Published public var totalAdsBlocked: Int = 1842
    @Published public var totalTrackersBlocked: Int = 3491
    @Published public var desktopMode: Bool = false
    @Published public var readerModeActive: Bool = false
    @Published public var selectedSearchEngine: SearchEngine = .azokle

    // Sheets & Overlays
    @Published public var showTabsGrid: Bool = false
    @Published public var showOmniboxSheet: Bool = false
    @Published public var showMenuSheet: Bool = false
    @Published public var showShieldSheet: Bool = false
    @Published public var showBurnConfirmation: Bool = false
    @Published public var showTorCircuitSheet: Bool = false
    @Published public var showHistorySheet: Bool = false
    @Published public var showBookmarksSheet: Bool = false
    @Published public var showDownloadsSheet: Bool = false
    @Published public var showSettingsSheet: Bool = false
    @Published public var showDiscoverScreen: Bool = false
    @Published public var isBurningAnimationActive: Bool = false

    // History, Bookmarks, Downloads
    @Published public var history: [HistoryItem] = []
    @Published public var bookmarks: [BookmarkItem] = []
    @Published public var downloads: [DownloadItem] = []

    public var activeTab: BrowserTab? {
        get {
            tabs.first { $0.id == activeTabId }
        }
        set {
            if let newValue = newValue, let index = tabs.firstIndex(where: { $0.id == newValue.id }) {
                tabs[index] = newValue
            }
        }
    }

    public init() {
        // Initialize with default sovereign search tab
        let initialTab = BrowserTab(
            url: "https://search.azokle.com",
            title: "Azokle Sovereign Search"
        )
        self.tabs = [initialTab]
        self.activeTabId = initialTab.id

        // Populate standard sample bookmarks
        self.bookmarks = [
            BookmarkItem(url: "https://search.azokle.com", title: "Azokle Search", dateAdded: Date()),
            BookmarkItem(url: "https://duckduckgo.com", title: "DuckDuckGo", dateAdded: Date()),
            BookmarkItem(url: "https://en.wikipedia.org", title: "Wikipedia", dateAdded: Date()),
            BookmarkItem(url: "https://github.com", title: "GitHub", dateAdded: Date())
        ]
    }

    // MARK: - Tab Management
    public func openNewTab(
        url: String = "https://search.azokle.com",
        title: String = "New Tab",
        isPrivate: Bool = false,
        isTor: Bool = false
    ) {
        let newTab = BrowserTab(
            url: url,
            title: title,
            isPrivate: isPrivate,
            isTor: isTor
        )
        tabs.append(newTab)
        activeTabId = newTab.id

        if isTor {
            selectedTabMode = .torMode
        } else if isPrivate {
            selectedTabMode = .privateMode
        } else {
            selectedTabMode = .standard
        }
    }

    public func closeTab(id: UUID) {
        guard let index = tabs.firstIndex(where: { $0.id == id }) else { return }
        tabs.remove(at: index)

        if tabs.isEmpty {
            openNewTab()
        } else if activeTabId == id {
            let nextIndex = min(index, tabs.count - 1)
            activeTabId = tabs[nextIndex].id
        }
    }

    public func closeAllTabs(forMode mode: TabFilterMode) {
        switch mode {
        case .standard:
            tabs.removeAll { !$0.isPrivate && !$0.isTor }
        case .privateMode:
            tabs.removeAll { $0.isPrivate && !$0.isTor }
        case .torMode:
            tabs.removeAll { $0.isTor }
        }

        if tabs.isEmpty {
            openNewTab()
        } else if !tabs.contains(where: { $0.id == activeTabId }) {
            activeTabId = tabs.first!.id
        }
    }

    public func selectTab(id: UUID) {
        if let tab = tabs.first(where: { $0.id == id }) {
            activeTabId = id
            if tab.isTor {
                selectedTabMode = .torMode
            } else if tab.isPrivate {
                selectedTabMode = .privateMode
            } else {
                selectedTabMode = .standard
            }
        }
    }

    // MARK: - Navigation
    public func navigateActiveTab(to rawInput: String) {
        var trimmed = rawInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return }

        let targetUrl: String
        let isUrl = trimmed.contains(".") && !trimmed.contains(" ") || trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") || trimmed.hasPrefix("about:")

        if isUrl {
            if !trimmed.hasPrefix("http://") && !trimmed.hasPrefix("https://") && !trimmed.hasPrefix("about:") {
                targetUrl = "https://" + trimmed
            } else {
                targetUrl = trimmed
            }
        } else {
            targetUrl = selectedSearchEngine.searchUrl(for: trimmed)
        }

        if let index = tabs.firstIndex(where: { $0.id == activeTabId }) {
            tabs[index].url = targetUrl
            tabs[index].title = trimmed
            tabs[index].isLoading = true
            tabs[index].progress = 0.1

            // Record to history if not private or Tor
            if !tabs[index].isPrivate && !tabs[index].isTor {
                history.insert(HistoryItem(url: targetUrl, title: trimmed, timestamp: Date()), at: 0)
            }
        }
    }

    public func reloadActiveTab() {
        if let index = tabs.firstIndex(where: { $0.id == activeTabId }) {
            tabs[index].isLoading = true
            tabs[index].progress = 0.2
        }
    }

    // MARK: - Data Incineration (Burn Browsing Data)
    public func burnAllData() {
        isBurningAnimationActive = true

        // Clean WebKit website data store
        let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        WKWebsiteDataStore.default().removeData(ofTypes: dataTypes, modifiedSince: .distantPast) {
            DispatchQueue.main.async {
                print("[AzokleIos] WebKit WebsiteDataStore purged completely")
            }
        }

        // Reset history, downloads, tabs
        history.removeAll()
        tabs.removeAll()
        openNewTab(url: "https://search.azokle.com", title: "Azokle Sovereign Search")

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
            self.isBurningAnimationActive = false
            self.showBurnConfirmation = false
        }
    }

    // MARK: - Shield & Tor Controls
    public func toggleShield() {
        isShieldActive.toggle()
    }

    public func toggleTorCircuit() {
        isTorCircuitActive.toggle()
        if isTorCircuitActive {
            torStatus = .connecting
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                self.torStatus = .connected
            }
        } else {
            torStatus = .disconnected
        }
    }

    public func rotateTorCircuit() {
        torStatus = .rotating
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            self.torStatus = .connected
        }
    }

    public func toggleBookmark(url: String, title: String) {
        if let index = bookmarks.firstIndex(where: { $0.url == url }) {
            bookmarks.remove(at: index)
        } else {
            bookmarks.append(BookmarkItem(url: url, title: title, dateAdded: Date()))
        }
    }

    public func isBookmarked(url: String) -> Bool {
        bookmarks.contains { $0.url == url }
    }
}
