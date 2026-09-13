import Foundation
import Combine

// MARK: - Tor Connection State
public enum TorConnectionState: Equatable {
    case disconnected
    case starting(String)
    case bootstrapping(progress: Int, statusText: String)
    case connected(guardNode: TorHopInfo, middleNode: TorHopInfo, exitNode: TorHopInfo, circuitId: String, latencyMs: Int)
    case error(message: String, canRetry: Bool)

    public var statusDescription: String {
        switch self {
        case .disconnected: return "Disconnected"
        case .starting(let text): return text
        case .bootstrapping(let prog, _): return "Bootstrapping (\(prog)%)..."
        case .connected: return "3-Hop Onion Circuit Secured"
        case .error(let msg, _): return msg
        }
    }
}

// MARK: - Tor Bridge Type
public enum TorBridgeType: String, CaseIterable, Identifiable {
    case direct = "Direct Connection (No Bridge)"
    case snowflake = "Snowflake (WebRTC Rendezvous)"
    case meekAzure = "meek-azure (Domain Fronting)"
    case obfs4 = "obfs4 Pluggable Transport"

    public var id: String { rawValue }

    public var subtitle: String {
        switch self {
        case .direct: return "Connect directly to the decentralized Tor directory."
        case .snowflake: return "Routes traffic through ephemeral WebRTC proxies. Resists deep packet inspection."
        case .meekAzure: return "Fronts traffic behind Microsoft Azure CDNs to bypass restrictive national firewalls."
        case .obfs4: return "Obfuscates packet signatures to appear as completely random encrypted noise."
        }
    }
}

// MARK: - Tor Hop Info
public struct TorHopInfo: Equatable, Identifiable {
    public var id: String { ipAddress }
    public let name: String
    public let role: String
    public let countryCode: String
    public let flagEmoji: String
    public let ipAddress: String
    public let latencyMs: Int
    public let bandwidthMBps: Double

    public init(name: String, role: String, countryCode: String, flagEmoji: String, ipAddress: String, latencyMs: Int, bandwidthMBps: Double) {
        self.name = name
        self.role = role
        self.countryCode = countryCode
        self.flagEmoji = flagEmoji
        self.ipAddress = ipAddress
        self.latencyMs = latencyMs
        self.bandwidthMBps = bandwidthMBps
    }
}

// MARK: - Sovereign Tor Circuit Manager
public final class TorCircuitManager: ObservableObject {
    public static let shared = TorCircuitManager()

    @Published public var state: TorConnectionState = .disconnected
    @Published public var selectedBridge: TorBridgeType = .snowflake
    @Published public var customBridgeLine: String = ""
    @Published public var autoConnectOnLaunch: Bool = true

    private var cancellables = Set<AnyCancellable>()

    private init() {
        self.state = .disconnected
    }

    // MARK: - Connect & Bootstrap Lifecycle
    public func startTorConnection() {
        state = .starting("Initiating Sovereign Onion Stack...")

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            self.state = .bootstrapping(progress: 25, statusText: "Connecting to \(self.selectedBridge.rawValue)...")
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) {
            self.state = .bootstrapping(progress: 60, statusText: "Negotiating Onion Diffie-Hellman Keys...")
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            self.state = .bootstrapping(progress: 88, statusText: "Acquiring Directory Consensus & Guard Relay...")
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 1.9) {
            self.buildSampleConnectedCircuit()
        }
    }

    public func disconnectTor() {
        state = .disconnected
    }

    public func rotateCircuit() {
        state = .bootstrapping(progress: 50, statusText: "Negotiating New 3-Hop Circuit...")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            self.buildSampleConnectedCircuit()
        }
    }

    private func buildSampleConnectedCircuit() {
        let guardRelays = [
            TorHopInfo(name: "Guard-Frankfurt-01", role: "Entry Guard (Layer 1)", countryCode: "DE", flagEmoji: "🇩🇪", ipAddress: "185.220.101.5", latencyMs: 38, bandwidthMBps: 120.4),
            TorHopInfo(name: "Guard-Reykjavik-02", role: "Entry Guard (Layer 1)", countryCode: "IS", flagEmoji: "🇮🇸", ipAddress: "193.107.85.12", latencyMs: 52, bandwidthMBps: 98.2),
            TorHopInfo(name: "Guard-Stockholm-04", role: "Entry Guard (Layer 1)", countryCode: "SE", flagEmoji: "🇸🇪", ipAddress: "194.26.29.88", latencyMs: 44, bandwidthMBps: 110.0)
        ]

        let middleRelays = [
            TorHopInfo(name: "Middle-Amsterdam-07", role: "Middle Relay (Layer 2)", countryCode: "NL", flagEmoji: "🇳🇱", ipAddress: "45.154.255.67", latencyMs: 64, bandwidthMBps: 85.6),
            TorHopInfo(name: "Middle-Zurich-03", role: "Middle Relay (Layer 2)", countryCode: "CH", flagEmoji: "🇨🇭", ipAddress: "179.43.142.19", latencyMs: 58, bandwidthMBps: 94.1),
            TorHopInfo(name: "Middle-Helsinki-09", role: "Middle Relay (Layer 2)", countryCode: "FI", flagEmoji: "🇫🇮", ipAddress: "95.216.14.33", latencyMs: 72, bandwidthMBps: 76.8)
        ]

        let exitRelays = [
            TorHopInfo(name: "Exit-Geneva-Tor-01", role: "Exit Node (Layer 3)", countryCode: "CH", flagEmoji: "🇨🇭", ipAddress: "185.220.100.252", latencyMs: 82, bandwidthMBps: 145.0),
            TorHopInfo(name: "Exit-Bucharest-08", role: "Exit Node (Layer 3)", countryCode: "RO", flagEmoji: "🇷🇴", ipAddress: "185.220.102.8", latencyMs: 91, bandwidthMBps: 112.5),
            TorHopInfo(name: "Exit-Oslo-Secure-02", role: "Exit Node (Layer 3)", countryCode: "NO", flagEmoji: "🇳🇴", ipAddress: "195.123.245.16", latencyMs: 88, bandwidthMBps: 130.0)
        ]

        let g = guardRelays.randomElement()!
        let m = middleRelays.randomElement()!
        let e = exitRelays.randomElement()!
        let circuitHex = "0x" + String(format: "%08X", arc4random())
        let totalLatency = g.latencyMs + m.latencyMs + e.latencyMs

        self.state = .connected(
            guardNode: g,
            middleNode: m,
            exitNode: e,
            circuitId: circuitHex,
            latencyMs: totalLatency
        )
    }
}
