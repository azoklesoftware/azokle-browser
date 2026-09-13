import Foundation

public enum ThreatType: String, CaseIterable {
    case phishing = "Deceptive Site Ahead"
    case malware = "Harmful Software Detected"
    case cryptoDrainer = "Cryptojacking & Asset Drainer"
    case none = "Safe"

    public var description: String {
        switch self {
        case .phishing:
            return "Attackers on this site may attempt to trick you into revealing sensitive credentials, passwords, phone numbers, or private keys."
        case .malware:
            return "This website has been identified as a distribution endpoint for malicious payloads or device exploitation."
        case .cryptoDrainer:
            return "This site contains scripts designed to trigger unauthorized Web3 wallet signing and drain cryptocurrency assets."
        case .none:
            return "No known security threats detected."
        }
    }
}

public struct ThreatAssessment: Identifiable, Hashable {
    public let id = UUID()
    public let url: String
    public let host: String
    public let isThreat: Bool
    public let threatType: ThreatType
}

public class SafeBrowsingGuard {
    public static let shared = SafeBrowsingGuard()
    private var userAllowedThreats = Set<String>()
    private let lock = NSLock()

    private let knownMaliciousDomains: [String: ThreatType] = [
        "paypal-security-verification.com": .phishing,
        "appleid-support-recover.com": .phishing,
        "google-login-verify-account.com": .phishing,
        "chase-online-secure-auth.com": .phishing,
        "metamask-wallet-validate.org": .cryptoDrainer,
        "ledger-drainer-sync.io": .cryptoDrainer,
        "phantom-wallet-connect-airdrop.net": .cryptoDrainer,
        "android-security-system-update.apk-download.net": .malware,
        "cleaner-boost-battery-virus.xyz": .malware,
        "cracked-software-keygen-installer.ru": .malware
    ]

    private let phishingKeywords = [
        "account-verification-login",
        "update-billing-security",
        "wallet-drainer",
        "web3-airdrop-claim-free",
        "secure-banking-token-login"
    ]

    public func assessUrl(_ rawUrl: String) -> ThreatAssessment {
        guard !rawUrl.isEmpty, let url = URL(string: rawUrl), let host = url.host?.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) else {
            return ThreatAssessment(url: rawUrl, host: "", isThreat: false, threatType: .none)
        }

        lock.lock()
        let isBypassed = userAllowedThreats.contains(host)
        lock.unlock()

        if isBypassed {
            return ThreatAssessment(url: rawUrl, host: host, isThreat: false, threatType: .none)
        }

        // Exact match
        if let threat = knownMaliciousDomains[host] {
            return ThreatAssessment(url: rawUrl, host: host, isThreat: true, threatType: threat)
        }

        // Subdomain walk
        for (knownHost, threat) in knownMaliciousDomains {
            if host.hasSuffix(".\(knownHost)") {
                return ThreatAssessment(url: rawUrl, host: host, isThreat: true, threatType: threat)
            }
        }

        // Heuristic detection on deceptive keywords
        let lowerUrl = rawUrl.lowercased()
        for keyword in phishingKeywords {
            if host.contains(keyword) || lowerUrl.contains(keyword) {
                return ThreatAssessment(url: rawUrl, host: host, isThreat: true, threatType: .phishing)
            }
        }

        return ThreatAssessment(url: rawUrl, host: host, isThreat: false, threatType: .none)
    }

    public func bypassThreatForSession(host: String) {
        lock.lock()
        userAllowedThreats.insert(host.lowercased().trimmingCharacters(in: .whitespacesAndNewlines))
        lock.unlock()
    }

    public func clearSessionBypasses() {
        lock.lock()
        userAllowedThreats.removeAll()
        lock.unlock()
    }
}
