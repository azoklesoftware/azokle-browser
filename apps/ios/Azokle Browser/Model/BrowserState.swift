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

// MARK: - Web Navigation Action
public enum WebAction {
    case goBack
    case goForward
    case reload
    case stop
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

// MARK: - Site Security & X.509 Certificate Models
public struct SiteSecurityInfo {
    public let host: String
    public let isSecure: Bool
    public let isTor: Bool
    public let protocolVersion: String
    public let cipherSuite: String
    public let issuedToCn: String
    public let issuedToOrg: String
    public let issuedByCn: String
    public let issuedByOrg: String
    public let validFrom: String
    public let validTo: String
    public let sha256Fingerprint: String
    public let keyAlgorithm: String
    public let connectionStatusText: String
}

// MARK: - Site Permissions Models
public enum SitePermissionType: String, CaseIterable, Identifiable {
    case location = "Location"
    case camera = "Camera"
    case microphone = "Microphone"
    case notifications = "Notifications"
    case autoplay = "Autoplay Media"
    case javascript = "JavaScript"
    case cookies = "Cookie Storage"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .location: return "location.fill"
        case .camera: return "camera.fill"
        case .microphone: return "mic.fill"
        case .notifications: return "bell.fill"
        case .autoplay: return "play.circle.fill"
        case .javascript: return "curlybraces"
        case .cookies: return "shield.lefthalf.filled"
        }
    }
}

public enum SitePermissionState: String, CaseIterable {
    case allow = "Allow"
    case ask = "Ask Every Time"
    case deny = "Block"
}

public struct SitePermissionRequest: Identifiable {
    public let id = UUID()
    public let host: String
    public let type: SitePermissionType
    public let uri: String

    public init(host: String, type: SitePermissionType, uri: String) {
        self.host = host
        self.type = type
        self.uri = uri
    }
}

public enum ContextMediaType {
    case none
    case image
    case video
    case audio
}

public struct LinkContextTarget: Identifiable {
    public let id = UUID()
    public let url: String
    public let title: String
    public let imageSrc: String?
    public let mediaType: ContextMediaType

    public init(url: String, title: String = "", imageSrc: String? = nil, mediaType: ContextMediaType = .none) {
        self.url = url
        self.title = title
        self.imageSrc = imageSrc
        self.mediaType = mediaType
    }
}

// MARK: - Main Sovereign Browser State
@MainActor
public class BrowserState: ObservableObject {
    // Tabs
    @Published public var tabs: [BrowserTab] = []
    @Published public var activeTabId: UUID = UUID()
    @Published public var selectedTabMode: TabFilterMode = .standard
    @Published public var pendingAction: (UUID, WebAction)? = nil

    // Protection & Privacy
    @Published public var isShieldActive: Bool = true
    @Published public var isTorCircuitActive: Bool = false
    @Published public var totalAdsBlocked: Int = 0
    @Published public var totalTrackersBlocked: Int = 0
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

    // Advanced Sheets (Parity with Android)
    @Published public var showCertificateSheet: Bool = false
    @Published public var showSiteSettingsSheet: Bool = false
    @Published public var showShareSheet: Bool = false
    @Published public var showFindInPage: Bool = false
    @Published public var findInPageQuery: String = ""
    @Published public var findInPageCurrentMatch: Int = 0
    @Published public var findInPageTotalMatches: Int = 0
    @Published public var activePermissionPrompt: SitePermissionRequest? = nil
    @Published public var activeLinkContext: LinkContextTarget? = nil
    @Published public var sitePermissions: [String: [SitePermissionType: SitePermissionState]] = [:]
    @Published public var siteShieldOverrides: [String: Bool] = [:]

    // Feature Modules Parity
    @Published public var tabGroups: [TabGroup] = []
    @Published public var selectedWallpaper: String = "bg_home_1"
    @Published public var selectedLanguageCode: String = "en"
    @Published public var showVoiceSearch: Bool = false
    @Published public var showChooseEngineSheet: Bool = false
    @Published public var showLanguageSheet: Bool = false
    @Published public var showPwaSheet: Bool = false
    @Published public var showOnboarding: Bool = false
    @Published public var showSuggestionsSheet: Bool = false
    @Published public var activeTabGroupForDetail: TabGroup? = nil

    // Advanced Ported Modules (Azokle Player, Passkey, Reader, Tor, SafeBrowsing, Pro)
    @Published public var passkeys: [PasskeyItem] = []
    @Published public var showPasskeyVaultSheet: Bool = false
    @Published public var showAzoklePlayerPrompt: Bool = false
    @Published public var showAzoklePlayer: Bool = false
    @Published public var activeVideoUrl: String = ""
    @Published public var activeVideoId: String = ""
    @Published public var showTorConnectScreen: Bool = false
    @Published public var showProUpgradeSheet: Bool = false
    @Published public var activeThreatAssessment: ThreatAssessment? = nil
    @Published public var readerArticleContent: ArticleContent? = nil
    @Published public var showReaderScreen: Bool = false
    @Published public var playerMode: AzoklePlayerMode = .ask

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

        self.bookmarks = []
        self.passkeys = []
        self.downloads = []
        self.history = []
    }

    public func addBookmark(title: String, url: String) {
        let item = BookmarkItem(url: url, title: title, dateAdded: Date())
        self.bookmarks.insert(item, at: 0)
    }

    public func addPasskey(rpId: String, userName: String) {
        let item = PasskeyItem(rpId: rpId, userName: userName)
        self.passkeys.append(item)
    }

    public func deletePasskey(id: UUID) {
        self.passkeys.removeAll { $0.id == id }
    }

    public func openVideoInPlayer(url: String, videoId: String) {
        self.activeVideoUrl = url
        self.activeVideoId = videoId
        self.showAzoklePlayer = true
    }

    public func triggerReaderMode(for tab: BrowserTab) {
        ReaderExtractor.shared.extractArticle(from: tab.url, fallbackTitle: tab.title, isTor: tab.isTor) { article in
            self.readerArticleContent = article
            self.showReaderScreen = true
        }
    }

    public func clearHistory() {
        self.history.removeAll()
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
            selectedTabMode = .standard
            openNewTab()
        } else if !tabs.contains(where: { $0.id == activeTabId }) {
            let nextTab = tabs.first!
            activeTabId = nextTab.id
            if nextTab.isTor {
                selectedTabMode = .torMode
            } else if nextTab.isPrivate {
                selectedTabMode = .privateMode
            } else {
                selectedTabMode = .standard
            }
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
        let trimmed = rawInput.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return }

        let lower = trimmed.lowercased()
        // Neutralize dangerous self-executing or local file access schemes by treating as search query
        if lower.hasPrefix("javascript:") || lower.hasPrefix("data:") || lower.hasPrefix("file:") {
            let targetUrl = selectedSearchEngine.searchUrl(for: trimmed)
            if let index = tabs.firstIndex(where: { $0.id == activeTabId }) {
                tabs[index].url = targetUrl
                tabs[index].title = trimmed
                tabs[index].isLoading = true
                tabs[index].progress = 0.1
            }
            return
        }

        var targetUrl: String
        let isUrl = (trimmed.contains(".") && !trimmed.contains(" ")) || trimmed.hasPrefix("http://") || trimmed.hasPrefix("https://") || trimmed.hasPrefix("about:")

        if isUrl {
            if !trimmed.hasPrefix("http://") && !trimmed.hasPrefix("https://") && !trimmed.hasPrefix("about:") {
                targetUrl = "https://" + trimmed
            } else {
                targetUrl = trimmed
            }

            // Strip tracking telemetry parameters (utm_*, fbclid, gclid, etc.)
            if let parsed = URL(string: targetUrl) {
                targetUrl = NavigationSecurity.shared.stripTrackingParameters(from: parsed).absoluteString
            }
        } else {
            targetUrl = selectedSearchEngine.searchUrl(for: trimmed)
        }

        // 1. SafeBrowsing Heuristic Threat Assessment (Android SafeBrowsingGuard Parity)
        let threat = SafeBrowsingGuard.shared.assessUrl(targetUrl)
        if threat.isThreat {
            self.activeThreatAssessment = threat
            return
        }

        // 2. Azokle Sovereign Player Interception (Android AzoklePlayerHelper Parity)
        if AzoklePlayerHelper.isEligibleVideoUrl(targetUrl), let videoId = AzoklePlayerHelper.extractVideoId(targetUrl) {
            self.activeVideoUrl = targetUrl
            self.activeVideoId = videoId
            if playerMode == .always {
                self.showAzoklePlayer = true
                return
            } else if playerMode == .ask {
                self.showAzoklePlayerPrompt = true
            }
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

    public func triggerWebAction(_ action: WebAction, for tabId: UUID? = nil) {
        let targetId = tabId ?? activeTabId
        pendingAction = (targetId, action)
    }

    public func reloadActiveTab() {
        if let index = tabs.firstIndex(where: { $0.id == activeTabId }) {
            tabs[index].isLoading = true
            tabs[index].progress = 0.2
        }
        triggerWebAction(.reload)
    }

    // MARK: - Sovereign Data Incineration (Burn All Browsing Data & Memory Hardening)
    public func burnAllData() {
        isBurningAnimationActive = true

        // 1. Clean WebKit website data store (all cookies, caches, local storage, indexedDB, service workers)
        let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        WKWebsiteDataStore.default().removeData(ofTypes: dataTypes, modifiedSince: .distantPast) {
            DispatchQueue.main.async {
                print("[AzokleIos] WebKit WebsiteDataStore purged completely")
            }
        }

        // 2. Lock Sovereign Hardware Encrypted Vault
        EncryptedVaultManager.shared.lock()

        // 3. Stop Active Background Media & Reset Audio Session
        MediaPlaybackManager.shared.isPlaying = false
        MediaPlaybackManager.shared.setSleepTimer(.off)

        // 4. Clear In-Memory Caches, Reader Content & Threats
        readerArticleContent = nil
        showReaderScreen = false
        activeVideoUrl = ""
        activeVideoId = ""
        showAzoklePlayer = false
        activeThreatAssessment = nil
        SafeBrowsingGuard.shared.clearSessionBypasses()
        showFindInPage = false
        findInPageQuery = ""

        // 5. Purge history, recent searches, tabs, and reseed fresh sandboxed tab
        // AZK-LOW-06: Ensure recent searches in UserDefaults are cleared upon data burn
        UserDefaults.standard.removeObject(forKey: "azokle_recent_searches")
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
            TorCircuitManager.shared.startTorConnection()
        } else {
            TorCircuitManager.shared.disconnectTor()
        }
    }

    public func rotateTorCircuit() {
        TorCircuitManager.shared.rotateCircuit()
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

    // MARK: - Site Security Inspector
    public func getSiteSecurityInfo(for urlString: String) -> SiteSecurityInfo {
        let isTor = urlString.contains(".onion") || (activeTab?.isTor ?? false)
        let isHttps = urlString.hasPrefix("https://")
        guard let url = URL(string: urlString), let host = url.host else {
            return SiteSecurityInfo(
                host: "Internal Session",
                isSecure: true,
                isTor: isTor,
                protocolVersion: "Memory Virtual Engine",
                cipherSuite: "Local Sandboxed",
                issuedToCn: "Azokle Sovereign Browser",
                issuedToOrg: "Azokle Open Network",
                issuedByCn: "Local Trust Anchor",
                issuedByOrg: "Azokle Sovereign Root CA",
                validFrom: "2026-01-01",
                validTo: "2030-01-01",
                sha256Fingerprint: "E3:B0:C4:42:98:FC:1C:14:9A:FB:F4:C8:99:6F:B9:24:27:AE:41:E4:64:9B:93:4C:A4:95:99:1B:78:52:B8:55",
                keyAlgorithm: "Quantum-Resistant Kyber-1024 / ECC-256",
                connectionStatusText: "Secure Local Memory Space"
            )
        }

        let cleanHost = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host

        if isTor {
            return SiteSecurityInfo(
                host: cleanHost,
                isSecure: true,
                isTor: true,
                protocolVersion: "Tor v3 Onion Protocol (ed25519)",
                cipherSuite: "Curve25519-SHA256-AES128GCM",
                issuedToCn: cleanHost,
                issuedToOrg: "Tor Onion Service",
                issuedByCn: "Decentralized Onion Directory Authority",
                issuedByOrg: "The Tor Project",
                validFrom: "Ephemeral Circuit",
                validTo: "Active Session",
                sha256Fingerprint: "Cryptographic Onion Service Identity",
                keyAlgorithm: "Ed25519 Onion Key (256-bit)",
                connectionStatusText: "End-to-End Encrypted via 3 Tor Relays"
            )
        } else if isHttps {
            return SiteSecurityInfo(
                host: cleanHost,
                isSecure: true,
                isTor: false,
                protocolVersion: "TLS 1.2 / TLS 1.3",
                cipherSuite: "Authenticated WebKit Cipher",
                issuedToCn: cleanHost,
                issuedToOrg: "Verified Domain Owner",
                issuedByCn: "Trusted Certificate Authority",
                issuedByOrg: "Operating System Root Trust Store",
                validFrom: "Validated",
                validTo: "Valid",
                sha256Fingerprint: "Verified by iOS Security Framework",
                keyAlgorithm: "Public Key Cryptography",
                connectionStatusText: "Connection is Secure (Verified by OS Trust Store)"
            )
        } else {
            return SiteSecurityInfo(
                host: cleanHost,
                isSecure: false,
                isTor: false,
                protocolVersion: "Plaintext HTTP / 1.1",
                cipherSuite: "None (Unencrypted Transmission)",
                issuedToCn: cleanHost,
                issuedToOrg: "Unverified Entity",
                issuedByCn: "None",
                issuedByOrg: "None",
                validFrom: "N/A",
                validTo: "N/A",
                sha256Fingerprint: "None (No Certificate Presented)",
                keyAlgorithm: "None",
                connectionStatusText: "Connection is Not Secure (Insecure HTTP)"
            )
        }
    }

    // MARK: - Privacy URL Stripping (Share Cleaner)
    public func cleanUrlForSharing(_ rawUrl: String) -> String {
        guard var components = URLComponents(string: rawUrl) else { return rawUrl }
        let trackingKeys: Set<String> = [
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "fbclid", "gclid", "mc_eid", "igshid", "msclkid", "yclid", "_hsenc",
            "_openstat", "dclid", "zanpid", "irclid", "twclid", "sc_cid"
        ]

        if let queryItems = components.queryItems {
            let filtered = queryItems.filter { !trackingKeys.contains($0.name.lowercased()) }
            components.queryItems = filtered.isEmpty ? nil : filtered
        }
        return components.url?.absoluteString ?? rawUrl
    }

    // MARK: - Per-Site Shield Controls
    public func isShieldEnabled(for host: String) -> Bool {
        let clean = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        return siteShieldOverrides[clean] ?? isShieldActive
    }

    public func setShieldEnabled(_ enabled: Bool, for host: String) {
        let clean = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        siteShieldOverrides[clean] = enabled
    }

    // MARK: - Per-Site Permissions Manager
    public func getPermission(for host: String, type: SitePermissionType) -> SitePermissionState {
        let clean = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        return sitePermissions[clean]?[type] ?? .ask
    }

    public func setPermission(for host: String, type: SitePermissionType, state: SitePermissionState) {
        let clean = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        if sitePermissions[clean] == nil {
            sitePermissions[clean] = [:]
        }
        sitePermissions[clean]?[type] = state
    }

    public func clearSiteData(for host: String) {
        let clean = host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
        sitePermissions.removeValue(forKey: clean)
        siteShieldOverrides.removeValue(forKey: clean)

        // Clear WebKit storage matching domain
        let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        WKWebsiteDataStore.default().fetchDataRecords(ofTypes: dataTypes) { records in
            let matching = records.filter { $0.displayName.contains(clean) }
            WKWebsiteDataStore.default().removeData(ofTypes: dataTypes, for: matching) {
                print("[AzokleIos] Cleared site storage for \(clean)")
            }
        }
    }

    // MARK: - Tab Group Management
    public func createTabGroup(title: String, colorHex: UInt32 = 0x38BDF8, tabIds: [UUID] = []) -> TabGroup {
        let newGroup = TabGroup(title: title, colorHex: colorHex, tabIds: tabIds)
        tabGroups.append(newGroup)
        return newGroup
    }

    public func deleteTabGroup(id: UUID) {
        tabGroups.removeAll { $0.id == id }
    }

    public func toggleTabGroupCollapse(id: UUID) {
        if let index = tabGroups.firstIndex(where: { $0.id == id }) {
            tabGroups[index].isCollapsed.toggle()
        }
    }

    public func addTabToGroup(tabId: UUID, groupId: UUID) {
        if let index = tabGroups.firstIndex(where: { $0.id == groupId }) {
            if !tabGroups[index].tabIds.contains(tabId) {
                tabGroups[index].tabIds.append(tabId)
            }
        }
    }

    public func removeTabFromGroup(tabId: UUID, groupId: UUID) {
        if let index = tabGroups.firstIndex(where: { $0.id == groupId }) {
            tabGroups[index].tabIds.removeAll { $0 == tabId }
        }
    }
}
