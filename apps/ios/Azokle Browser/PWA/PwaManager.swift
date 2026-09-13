import Foundation

// MARK: - PWA Manifest Icon
public struct PwaManifestIcon: Hashable, Identifiable {
    public var id: String { src }
    public let src: String
    public let sizes: String
    public let type: String

    public init(src: String, sizes: String = "", type: String = "") {
        self.src = src
        self.sizes = sizes
        self.type = type
    }
}

// MARK: - PWA Web App Manifest
public struct PwaWebAppManifest: Identifiable, Hashable {
    public var id: String { startUrl }
    public let name: String
    public let shortName: String
    public let startUrl: String
    public let description: String
    public let themeColorHex: String?
    public let icons: [PwaManifestIcon]

    public init(name: String, shortName: String, startUrl: String, description: String = "", themeColorHex: String? = nil, icons: [PwaManifestIcon] = []) {
        self.name = name
        self.shortName = shortName
        self.startUrl = startUrl
        self.description = description
        self.themeColorHex = themeColorHex
        self.icons = icons
    }

    public var bestIconUrl: String? {
        icons.first?.src
    }
}

// MARK: - PWA Manager
public final class PwaManager: ObservableObject {
    public static let shared = PwaManager()

    @Published public var detectedManifest: PwaWebAppManifest? = nil

    private init() {}

    public func parseManifest(from jsonString: String, documentUrl: String) -> PwaWebAppManifest? {
        guard let data = jsonString.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }

        let name = json["name"] as? String ?? json["short_name"] as? String ?? (URL(string: documentUrl)?.host ?? "Web App")
        let shortName = json["short_name"] as? String ?? name
        let startUrl = json["start_url"] as? String ?? documentUrl
        let description = json["description"] as? String ?? ""
        let themeColor = json["theme_color"] as? String

        var iconsList: [PwaManifestIcon] = []
        if let iconsArr = json["icons"] as? [[String: Any]] {
            for iconObj in iconsArr {
                if let src = iconObj["src"] as? String {
                    let sizes = iconObj["sizes"] as? String ?? ""
                    let type = iconObj["type"] as? String ?? ""
                    iconsList.append(PwaManifestIcon(src: src, sizes: sizes, type: type))
                }
            }
        }

        let manifest = PwaWebAppManifest(
            name: name,
            shortName: shortName,
            startUrl: startUrl,
            description: description,
            themeColorHex: themeColor,
            icons: iconsList
        )
        self.detectedManifest = manifest
        return manifest
    }
}
