import Foundation
import UIKit

// MARK: - Navigation Security Policy Decision
public enum NavigationPolicyDecision {
    case allow
    case cancel
    case openExternalApp(URL)
    case blockedDangerousScheme(String)
    case blockedTorClearnetLeak(URL)
}

// MARK: - Sovereign Navigation Security Guard
public final class NavigationSecurity {
    public static let shared = NavigationSecurity()

    // Schemes forbidden from untrusted web navigation to prevent sandbox escape or local file theft
    private let forbiddenSchemes: Set<String> = [
        "file", "content", "chrome", "resource", "jar", "blob", "javascript", "data"
    ]

    // Supported external app schemes (AZK-LOW-07: dropped x-callback-url to prevent unprompted IPC actions)
    private let safeExternalSchemes: Set<String> = [
        "tel", "mailto", "sms", "maps", "facetime", "facetime-audio",
        "upi", "whatsapp", "tg", "twitter"
    ]

    // Privacy tracking parameter blacklist
    private let trackingParameters: Set<String> = [
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "gclid", "mc_eid", "igshid", "msclkid", "yclid", "_hsenc",
        "_openstat", "dclid", "zanpid", "irclid", "twclid", "sc_cid"
    ]

    private init() {}

    // MARK: - Navigation Policy Check
    public func evaluateNavigation(url: URL, isTor: Bool = false) -> NavigationPolicyDecision {
        guard let scheme = url.scheme?.lowercased() else {
            return .cancel
        }

        // 1. Check for dangerous internal or execution schemes
        if forbiddenSchemes.contains(scheme) {
            return .blockedDangerousScheme(scheme)
        }

        // 2. Standard web browsing schemes
        if scheme == "http" || scheme == "https" || scheme == "about" {
            return .allow
        }

        // 3. Tor Clearnet Leak Check: Don't allow Tor sessions to launch external native apps silently
        if isTor && safeExternalSchemes.contains(scheme) {
            return .blockedTorClearnetLeak(url)
        }

        // 4. Safe External Scheme Dispatches (Phone, Mail, SMS, Maps, etc.)
        if safeExternalSchemes.contains(scheme) || scheme.hasPrefix("upi") {
            return .openExternalApp(url)
        }

        return .cancel
    }

    // MARK: - HTTPS Upgrader (HTTPS-Only Mode with LAN/Localhost Exemption)
    public func upgradeToHttps(url: URL) -> URL {
        guard url.scheme?.lowercased() == "http",
              let host = url.host,
              !isPrivateOrLocalHost(host),
              var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return url
        }
        components.scheme = "https"
        return components.url ?? url
    }

    private func isPrivateOrLocalHost(_ host: String) -> Bool {
        let clean = host.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)
        if clean == "localhost" || clean == "127.0.0.1" || clean == "::1" || clean.hasSuffix(".local") || clean.hasSuffix(".internal") || clean.hasSuffix(".onion") {
            return true
        }
        if clean.hasPrefix("192.168.") || clean.hasPrefix("10.") || clean.hasPrefix("169.254.") {
            return true
        }
        if clean.hasPrefix("172.") {
            let parts = clean.split(separator: ".")
            if parts.count >= 2, let second = Int(parts[1]), second >= 16 && second <= 31 {
                return true
            }
        }
        return false
    }

    // MARK: - Tracking Parameter Sanitizer
    public func stripTrackingParameters(from url: URL) -> URL {
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let queryItems = components.queryItems, !queryItems.isEmpty else {
            return url
        }

        let filtered = queryItems.filter { !trackingParameters.contains($0.name.lowercased()) }
        components.queryItems = filtered.isEmpty ? nil : filtered
        return components.url ?? url
    }
}
