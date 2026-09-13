import Foundation

public struct BrowserTab: Identifiable, Equatable, Hashable {
    public let id: UUID
    public var url: String
    public var title: String
    public var favicon: String?
    public var isPrivate: Bool
    public var isTor: Bool
    public var lastAccessed: Date
    public var blockedTrackers: Int
    public var canGoBack: Bool
    public var canGoForward: Bool
    public var isLoading: Bool
    public var progress: Double

    public init(
        id: UUID = UUID(),
        url: String = "https://search.azokle.com",
        title: String = "Azokle Sovereign Search",
        favicon: String? = nil,
        isPrivate: Bool = false,
        isTor: Bool = false,
        lastAccessed: Date = Date(),
        blockedTrackers: Int = 0,
        canGoBack: Bool = false,
        canGoForward: Bool = false,
        isLoading: Bool = false,
        progress: Double = 0.0
    ) {
        self.id = id
        self.url = url
        self.title = title
        self.favicon = favicon
        self.isPrivate = isPrivate
        self.isTor = isTor
        self.lastAccessed = lastAccessed
        self.blockedTrackers = blockedTrackers
        self.canGoBack = canGoBack
        self.canGoForward = canGoForward
        self.isLoading = isLoading
        self.progress = progress
    }
}
