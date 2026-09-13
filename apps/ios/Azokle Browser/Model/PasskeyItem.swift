import Foundation

public struct PasskeyItem: Identifiable, Codable, Hashable {
    public let id: UUID
    public var rpId: String           // Relying Party domain (e.g. github.com, google.com)
    public var userName: String       // Username or email
    public var userHandle: String     // User handle / account ID
    public var credentialId: String   // Base64 encoded credential identifier
    public var createdAt: Date
    public var lastUsedAt: Date?

    public init(
        id: UUID = UUID(),
        rpId: String,
        userName: String,
        userHandle: String = "",
        credentialId: String = UUID().uuidString,
        createdAt: Date = Date(),
        lastUsedAt: Date? = nil
    ) {
        self.id = id
        self.rpId = rpId
        self.userName = userName
        self.userHandle = userHandle
        self.credentialId = credentialId
        self.createdAt = createdAt
        self.lastUsedAt = lastUsedAt
    }
}
