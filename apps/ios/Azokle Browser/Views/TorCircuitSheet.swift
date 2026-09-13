import SwiftUI

public struct TorCircuitSheet: View {
    @ObservedObject var state: BrowserState
    @ObservedObject var torManager = TorCircuitManager.shared
    @Binding var isPresented: Bool
    let colors: AzokleColors = .dark

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 10)
                .padding(.bottom, 16)

            ScrollView {
                VStack(spacing: 20) {
                    // 1. Tor Onion Hero Header
                    VStack(spacing: 8) {
                        ZStack {
                            Circle()
                                .fill(colors.statusTor.opacity(0.18))
                                .frame(width: 68, height: 68)

                            Image(systemName: "network.badge.shield.half.filled")
                                .font(.system(size: 32))
                                .foregroundColor(colors.statusTor)
                        }

                        Text("Tor Onion Circuit")
                            .font(.system(size: 19, weight: .bold))
                            .foregroundColor(colors.textPrimary)

                        HStack(spacing: 6) {
                            Circle()
                                .fill(colors.statusSuccess)
                                .frame(width: 8, height: 8)
                            Text(torManager.state.statusDescription)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(colors.statusTor)
                        }
                    }

                    // 2. Tor Circuit Relay Hops Visualizer
                    if case .connected(let g, let m, let e, let circuitId, let latency) = torManager.state {
                        VStack(alignment: .leading, spacing: 0) {
                            // Header banner
                            HStack {
                                Text("CIRCUIT \(circuitId)")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(colors.textMuted)
                                Spacer()
                                HStack(spacing: 4) {
                                    Image(systemName: "bolt.fill")
                                        .font(.system(size: 10))
                                    Text("\(latency) ms")
                                        .font(.system(size: 11, weight: .semibold))
                                }
                                .foregroundColor(colors.statusSuccess)
                            }
                            .padding(.bottom, 12)

                            circuitHopRow(
                                step: "1",
                                role: "This Device",
                                detail: "Ephemeral Tor Client Sandbox",
                                flag: "📱",
                                ip: "127.0.0.1:9050",
                                isLast: false
                            )
                            circuitHopRow(
                                step: "2",
                                role: g.role,
                                detail: "\(g.name) • \(g.bandwidthMBps) MB/s",
                                flag: g.flagEmoji,
                                ip: g.ipAddress,
                                isLast: false
                            )
                            circuitHopRow(
                                step: "3",
                                role: m.role,
                                detail: "\(m.name) • \(m.bandwidthMBps) MB/s",
                                flag: m.flagEmoji,
                                ip: m.ipAddress,
                                isLast: false
                            )
                            circuitHopRow(
                                step: "4",
                                role: e.role,
                                detail: "\(e.name) • \(e.bandwidthMBps) MB/s",
                                flag: e.flagEmoji,
                                ip: e.ipAddress,
                                isLast: false
                            )
                            circuitHopRow(
                                step: "5",
                                role: "Destination",
                                detail: state.activeTab?.url ?? "search.azokle.com",
                                flag: "🎯",
                                ip: "Onion / Clearnet",
                                isLast: true
                            )
                        }
                        .padding(16)
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.medium)
                    }

                    // 3. Bridge Configuration Card
                    VStack(alignment: .leading, spacing: 8) {
                        Text("ACTIVE PLUGGABLE TRANSPORT")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)

                        HStack(spacing: 12) {
                            Image(systemName: "antenna.radiowaves.left.and.right")
                                .font(.system(size: 16))
                                .foregroundColor(colors.statusAccent)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(torManager.selectedBridge.rawValue)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(colors.textPrimary)
                                Text(torManager.selectedBridge.subtitle)
                                    .font(.system(size: 11))
                                    .foregroundColor(colors.textSecondary)
                                    .lineLimit(2)
                            }
                        }
                        .padding(12)
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.small)
                    }

                    // 4. Actions
                    VStack(spacing: 10) {
                        Button(action: {
                            torManager.rotateCircuit()
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: "arrow.clockwise")
                                    .font(.system(size: 14, weight: .bold))
                                Text("Request New Tor Circuit")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundColor(colors.actionPrimaryText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 13)
                            .background(colors.actionPrimaryBg)
                            .cornerRadius(AzokleRadius.medium)
                        }

                        Button(action: {
                            state.showTorConnectScreen = true
                            isPresented = false
                        }) {
                            Text("Configure Bridges & Advanced Tor Settings")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(colors.statusAccent)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                        }
                    }
                }
                .padding(20)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.8), .large])
    }

    private func circuitHopRow(step: String, role: String, detail: String, flag: String, ip: String, isLast: Bool) -> some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 0) {
                ZStack {
                    Circle()
                        .fill(colors.statusTor.opacity(0.2))
                        .frame(width: 30, height: 30)

                    Text(flag)
                        .font(.system(size: 14))
                }

                if !isLast {
                    Rectangle()
                        .fill(colors.statusTor.opacity(0.35))
                        .frame(width: 2, height: 28)
                }
            }

            VStack(alignment: .leading, spacing: 1) {
                Text(role)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(colors.textPrimary)
                Text(detail)
                    .font(.system(size: 11))
                    .foregroundColor(colors.textSecondary)
                Text(ip)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(colors.textMuted)
            }
            .padding(.top, 2)

            Spacer()
        }
    }
}
