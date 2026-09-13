import SwiftUI

public enum TorBridgeType: String, CaseIterable, Identifiable {
    case direct = "Direct (No Bridge)"
    case snowflake = "Snowflake (WebRTC P2P)"
    case obfs4 = "obfs4 (Obfuscated IP)"
    case meek = "Meek-Azure (Domain Fronting)"

    public var id: String { rawValue }
}

public struct TorConnectScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var isConnecting: Bool = false
    @State private var bootstrapProgress: Double = 1.0
    @State private var selectedBridge: TorBridgeType = .snowflake
    @State private var onionAddressInput: String = ""
    @State private var logEntries: [String] = [
        "Tor 0.4.8.10 initialized on Darwin iOS",
        "Configured bridge: Snowflake WebRTC P2P proxy",
        "Loading consensus authority microdescriptors...",
        "Established 1-hop circuit with Entry Guard [de_frankfurt]",
        "Established 2-hop circuit with Middle Relay [is_reykjavik]",
        "Established 3-hop circuit with Exit Node [ch_zurich]",
        "Sovereign Onion Circuit 100% Bootstrapped. Ready."
    ]

    let colors: AzokleColors = .dark

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // Header Bar
                HStack {
                    Button(action: {
                        isPresented = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(colors.textPrimary)
                            .padding(8)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                    }

                    Spacer()

                    HStack(spacing: 6) {
                        Image(systemName: "network.badge.shield.half.filled")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(colors.statusTor)
                        Text("Tor Sovereign Cockpit")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(colors.textPrimary)
                    }

                    Spacer()

                    // Quick info
                    Image(systemName: "info.circle")
                        .font(.system(size: 16))
                        .foregroundColor(colors.textMuted)
                }
                .padding(.horizontal, 20)
                .padding(.top, 14)
                .padding(.bottom, 12)

                Divider().background(colors.border)

                ScrollView {
                    VStack(spacing: 24) {
                        // 1. Circular Onion Gauge
                        ZStack {
                            Circle()
                                .stroke(colors.surfaceElevated, lineWidth: 8)
                                .frame(width: 140, height: 140)

                            Circle()
                                .trim(from: 0, to: CGFloat(state.isTorCircuitActive ? bootstrapProgress : 0.0))
                                .stroke(
                                    LinearGradient(
                                        colors: [colors.statusTor, colors.statusAccent],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    ),
                                    style: StrokeStyle(lineWidth: 8, lineCap: .round)
                                )
                                .frame(width: 140, height: 140)
                                .rotationEffect(.degrees(-90))
                                .animation(.easeInOut(duration: 0.8), value: state.isTorCircuitActive)

                            VStack(spacing: 4) {
                                Image(systemName: state.isTorCircuitActive ? "lock.shield.fill" : "network.slash")
                                    .font(.system(size: 32))
                                    .foregroundColor(state.isTorCircuitActive ? colors.statusTor : colors.textMuted)

                                Text(state.isTorCircuitActive ? "100%" : "OFF")
                                    .font(.system(size: 16, weight: .bold, design: .monospaced))
                                    .foregroundColor(colors.textPrimary)
                            }
                        }
                        .padding(.top, 10)

                        // Status Title
                        VStack(spacing: 4) {
                            Text(state.isTorCircuitActive ? "3-Hop Onion Network Connected" : "Tor Circuit Inactive")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(colors.textPrimary)

                            Text(state.isTorCircuitActive ? "Encrypted multi-hop routing active across Swiss & Nordic relays" : "Connect to anonymize TCP packets and access .onion hidden services")
                                .font(.system(size: 12))
                                .foregroundColor(colors.textSecondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                        }

                        // Connect / Disconnect Toggle Action
                        Button(action: {
                            withAnimation {
                                state.isTorCircuitActive.toggle()
                            }
                        }) {
                            HStack(spacing: 8) {
                                Image(systemName: state.isTorCircuitActive ? "stop.circle.fill" : "bolt.shield.fill")
                                    .font(.system(size: 15, weight: .bold))
                                Text(state.isTorCircuitActive ? "Disconnect Tor Circuit" : "Bootstrap Tor Network")
                                    .font(.system(size: 15, weight: .bold))
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(state.isTorCircuitActive ? colors.surfaceElevated : colors.statusTor)
                            .foregroundColor(state.isTorCircuitActive ? colors.statusDestructive : .white)
                            .cornerRadius(AzokleRadius.pill)
                            .overlay(
                                RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                    .stroke(state.isTorCircuitActive ? colors.statusDestructive.opacity(0.5) : Color.clear, lineWidth: 1)
                            )
                        }
                        .padding(.horizontal, 24)

                        // 2. Bridge Configuration
                        VStack(alignment: .leading, spacing: 10) {
                            Text("ONION BRIDGES (CENSORSHIP CIRCUMVENTION)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.textMuted)

                            Menu {
                                ForEach(TorBridgeType.allCases) { bridge in
                                    Button(action: {
                                        selectedBridge = bridge
                                    }) {
                                        HStack {
                                            Text(bridge.rawValue)
                                            if selectedBridge == bridge {
                                                Image(systemName: "checkmark")
                                            }
                                        }
                                    }
                                }
                            } label: {
                                HStack {
                                    Image(systemName: "arrow.triangle.branch")
                                        .font(.system(size: 14))
                                        .foregroundColor(colors.statusTor)

                                    Text(selectedBridge.rawValue)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(colors.textPrimary)

                                    Spacer()

                                    Image(systemName: "chevron.up.chevron.down")
                                        .font(.system(size: 11))
                                        .foregroundColor(colors.textMuted)
                                }
                                .padding(12)
                                .background(colors.surfaceElevated)
                                .cornerRadius(AzokleRadius.small)
                                .overlay(
                                    RoundedRectangle(cornerRadius: AzokleRadius.small)
                                        .stroke(colors.border, lineWidth: 1)
                                )
                            }
                        }
                        .padding(.horizontal, 24)

                        // 3. .onion Address Direct Navigation
                        VStack(alignment: .leading, spacing: 10) {
                            Text("NAVIGATE TO ONION SERVICE")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.textMuted)

                            HStack(spacing: 8) {
                                Image(systemName: "network")
                                    .foregroundColor(colors.statusTor)

                                TextField("Enter .onion address or query...", text: $onionAddressInput)
                                    .font(.system(size: 13))
                                    .foregroundColor(colors.textPrimary)
                                    .autocapitalization(.none)

                                Button(action: {
                                    if !onionAddressInput.isEmpty {
                                        state.openNewTab(url: onionAddressInput, isTor: true)
                                        isPresented = false
                                    }
                                }) {
                                    Text("Open")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(colors.actionPrimaryText)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(colors.actionPrimaryBg)
                                        .cornerRadius(6)
                                }
                            }
                            .padding(10)
                            .background(colors.surfaceElevated)
                            .cornerRadius(AzokleRadius.small)
                            .overlay(
                                RoundedRectangle(cornerRadius: AzokleRadius.small)
                                    .stroke(colors.border, lineWidth: 1)
                            )
                        }
                        .padding(.horizontal, 24)

                        // 4. Live Event Log Terminal
                        VStack(alignment: .leading, spacing: 8) {
                            Text("BOOTSTRAP EVENT LOG")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.textMuted)

                            VStack(alignment: .leading, spacing: 4) {
                                ForEach(logEntries, id: \.self) { line in
                                    Text("> \(line)")
                                        .font(.system(size: 10, design: .monospaced))
                                        .foregroundColor(line.contains("100%") ? colors.statusSuccess : colors.textSecondary)
                                }
                            }
                            .padding(12)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.black.opacity(0.6))
                            .cornerRadius(AzokleRadius.small)
                            .overlay(
                                RoundedRectangle(cornerRadius: AzokleRadius.small)
                                    .stroke(colors.border, lineWidth: 0.8)
                            )
                        }
                        .padding(.horizontal, 24)
                    }
                    .padding(.vertical, 16)
                }
            }
        }
    }
}
