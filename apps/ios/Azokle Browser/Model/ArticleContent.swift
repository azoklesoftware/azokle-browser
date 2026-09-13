import Foundation

public struct ArticleContent: Identifiable, Hashable {
    public let id: UUID
    public var title: String
    public var byline: String
    public var domain: String
    public var excerpt: String
    public var paragraphs: [String]
    public var estimatedReadingTimeMinutes: Int

    public init(
        id: UUID = UUID(),
        title: String,
        byline: String = "",
        domain: String = "",
        excerpt: String = "",
        paragraphs: [String] = [],
        estimatedReadingTimeMinutes: Int = 3
    ) {
        self.id = id
        self.title = title
        self.byline = byline
        self.domain = domain
        self.excerpt = excerpt
        self.paragraphs = paragraphs
        self.estimatedReadingTimeMinutes = estimatedReadingTimeMinutes
    }
}
