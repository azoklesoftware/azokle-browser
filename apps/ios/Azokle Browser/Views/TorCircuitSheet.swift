import SwiftUI

public struct TorCircuitSheet: View {
    @ObservedObject var state: BrowserState
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
                                .fill(state.isTorCircuitActive ? colors.statusSuccess : colors.textMuted)
                                .frame(width: 8, height: 8)
                            Text(state.torStatus.rawValue)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(state.isTorCircuitActive ? colors.statusTor : colors.textSecondary)
                        }
                    }

                    // 2. Tor Circuit Relay Hops Visualizer
                    VStack(alignment: .leading, spacing: 0) {
                        circuitHopRow(
                            step: "1",
                            role: "This Device",
                            detail: "Local encrypted client endpoint",
                            icon: "iphone",
                            isLast: false
                        )
                        circuitHopRow(
                            step: "2",
                            role: "Guard Relay (Entry)",
                            detail: "Frankfurt, Germany • 185.220.101.5",
                            icon: "lock.shield",
                            isLast: false
                        )
                        circuitHopRow(
                            step: "3",
                            role: "Middle Relay",
                            detail: "Amsterdam, Netherlands • 194.26.29.112",
                            icon: "arrow.triangle.swap",
                            isLast: false
                        )
                        circuitHopRow(
                            step: "4",
                            role: "Exit Relay",
                            detail: "Zurich, Switzerland • 178.17.170.89",
                            icon: "globe",
                            isLast: false
                        )
                        circuitHopRow(
                            step: "5",
                            role: "Destination",
                            detail: state.activeTab?.url ?? "search.azokle.com",
                            icon: "checkmark.shield.fill",
                            isLast: true
                        )
                    }
                    .padding(16)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.medium)

                    // 3. Actions
                    VStack(spacing: 10) {
                        Button(action: {
                            state.rotateTorCircuit()
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
                            state.toggleTorCircuit()
                        }) {
                            Text(state.isTorCircuitActive ? "Disconnect Tor Circuit" : "Connect Tor Circuit")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(colors.statusTor)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 11)
                                .background(colors.surfaceElevated)
                                .cornerRadius(AzokleRadius.medium)
                        }
                    }
                }
                .padding(20)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.75), .large])
    }

    private func circuitHopRow(step: String, role: String, detail: String, icon: String, isLast: Bool) -> some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 0) {
                ZStack {
                    Circle()
                        .fill(colors.statusTor.opacity(0.2))
                        .frame(width: 28, height: 28)

                    Image(systemName: icon)
                        .font(.system(size: 12))
                        .foregroundColor(colors.statusTor)
                }

                if !isLast {
                    Rectangle()
                        .fill(colors.statusTor.opacity(0.35))
                        .frame(width: 2, height: 32)
                }
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(role)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(colors.textPrimary)
                Text(detail)
                    .font(.system(size: 11))
                    .foregroundColor(colors.textSecondary)
                    .lineLimit(1)
            }
            .padding(.top, 4)

            Spacer()
        }
    }
}
