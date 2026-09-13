import Foundation

public enum AzoklePlayerMode: String, CaseIterable, Identifiable {
    case always = "always"
    case ask = "ask"
    case disabled = "disabled"

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .always: return "Always in Azokle Player"
        case .ask: return "Ask Every Time"
        case .disabled: return "Disabled (Standard Site)"
        }
    }
}

public struct AzoklePlayerHelper {
    private static let youtubeWatchRegex = try? NSRegularExpression(
        pattern: "^https?://(?:www\\.|m\\.)?youtube\\.com/watch\\?.*v=([a-zA-Z0-9_-]{11})",
        options: .caseInsensitive
    )

    private static let youtubeShortRegex = try? NSRegularExpression(
        pattern: "^https?://youtu\\.be/([a-zA-Z0-9_-]{11})",
        options: .caseInsensitive
    )

    private static let youtubeShortsRegex = try? NSRegularExpression(
        pattern: "^https?://(?:www\\.|m\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})",
        options: .caseInsensitive
    )

    private static let youtubeEmbedRegex = try? NSRegularExpression(
        pattern: "^https?://(?:www\\.|m\\.)?(?:youtube\\.com|youtube-nocookie\\.com)/embed/([a-zA-Z0-9_-]{11})",
        options: .caseInsensitive
    )

    private static let vimeoRegex = try? NSRegularExpression(
        pattern: "^https?://(?:www\\.|player\\.)?vimeo\\.com/(?:video/)?(\\d{6,12})",
        options: .caseInsensitive
    )

    public static func isEligibleVideoUrl(_ url: String?) -> Bool {
        guard let url = url, !url.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        return isYouTubeWatchUrl(url) || isVimeoUrl(url)
    }

    public static func isYouTubeWatchUrl(_ url: String) -> Bool {
        let range = NSRange(location: 0, length: url.utf16.count)
        if youtubeWatchRegex?.firstMatch(in: url, options: [], range: range) != nil { return true }
        if youtubeShortRegex?.firstMatch(in: url, options: [], range: range) != nil { return true }
        if youtubeShortsRegex?.firstMatch(in: url, options: [], range: range) != nil { return true }
        if youtubeEmbedRegex?.firstMatch(in: url, options: [], range: range) != nil { return true }
        return false
    }

    public static func isVimeoUrl(_ url: String) -> Bool {
        let range = NSRange(location: 0, length: url.utf16.count)
        return vimeoRegex?.firstMatch(in: url, options: [], range: range) != nil
    }

    public static func extractVideoId(_ url: String) -> String? {
        let range = NSRange(location: 0, length: url.utf16.count)

        if let match = youtubeWatchRegex?.firstMatch(in: url, options: [], range: range),
           let captureRange = Range(match.range(at: 1), in: url) {
            return String(url[captureRange])
        }

        if let match = youtubeShortRegex?.firstMatch(in: url, options: [], range: range),
           let captureRange = Range(match.range(at: 1), in: url) {
            return String(url[captureRange])
        }

        if let match = youtubeShortsRegex?.firstMatch(in: url, options: [], range: range),
           let captureRange = Range(match.range(at: 1), in: url) {
            return String(url[captureRange])
        }

        if let match = youtubeEmbedRegex?.firstMatch(in: url, options: [], range: range),
           let captureRange = Range(match.range(at: 1), in: url) {
            return String(url[captureRange])
        }

        if let match = vimeoRegex?.firstMatch(in: url, options: [], range: range),
           let captureRange = Range(match.range(at: 1), in: url) {
            return String(url[captureRange])
        }

        return nil
    }

    public static func buildZeroCookieEmbedUrl(videoId: String, isVimeo: Bool = false) -> String {
        if isVimeo {
            return "https://player.vimeo.com/video/\(videoId)?autoplay=1&dnt=1"
        }
        return "https://www.youtube-nocookie.com/embed/\(videoId)?autoplay=1&modestbranding=1&rel=0&iv_load_policy=3&playsinline=1"
    }
}
