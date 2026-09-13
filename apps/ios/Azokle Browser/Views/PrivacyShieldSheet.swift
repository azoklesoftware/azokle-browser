import SwiftUI

public struct PrivacyShieldSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool
    @State private var protectionLevel: String = "Strict"

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
                    // 1. Big Shield Hero Header
                    VStack(spacing: 10) {
                        ZStack {
                            Circle()
                                .fill(colors.statusSuccess.opacity(0.15))
                                .frame(width: 70, height: 70)

                            Image(systemName: state.isShieldActive ? "shield.fill" : "shield.slash.fill")
                                .font(.system(size: 34))
                                .foregroundColor(state.isShieldActive ? colors.statusSuccess : colors.textMuted)
                        }

                        Text(state.isShieldActive ? "Azokle Shield Active" : "Shield Paused")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(colors.textPrimary)

                        Text("Protecting your browsing session against cross-site trackers, behavioral ads, and fingerprinting.")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }

                    // 2. Global / Per-Site Toggle Card
                    VStack(spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Shield Protection")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)
                                Text("Block ads, trackers, and fingerprint vectors")
                                    .font(.system(size: 12))
                                    .foregroundColor(colors.textSecondary)
                            }
                            Spacer()
                            Toggle("", isOn: $state.isShieldActive)
                                .labelsHidden()
                                .tint(colors.statusSuccess)
                        }
                    }
                    .padding(16)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.medium)

                    // 3. Protection Metrics Grid
                    VStack(alignment: .leading, spacing: 12) {
                        Text("ACTIVE DEFENSES")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.horizontal, 4)

                        VStack(spacing: 1) {
                            defenseRow(
                                icon: "hand.raised.fill",
                                title: "Trackers & Analytics",
                                detail: "\(state.activeTab?.blockedTrackers ?? 0) blocked on this site",
                                color: colors.statusSuccess
                            )
                            defenseRow(
                                icon: "nosign",
                                title: "Ad Network Suppression",
                                detail: "Zero intrusive banner & video ads",
                                color: colors.statusAccent
                            )
                            defenseRow(
                                icon: "fingerprint",
                                title: "Canvas & Fingerprint Shield",
                                detail: "Spoofed hardware entropy & WebRTC leak defense",
                                color: colors.statusTor
                            )
                            defenseRow(
                                icon: "lock.shield.fill",
                                title: "HTTPS Everywhere",
                                detail: "Secure encrypted protocol upgrade",
                                color: colors.statusSuccess
                            )
                        }
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.medium)
                    }

                    // 4. Lifetime Stats Callout
                    HStack(spacing: 16) {
                        statBox(title: "Trackers Blocked", count: "\(state.totalTrackersBlocked)", color: colors.statusSuccess)
                        statBox(title: "Ads Suppressed", count: "\(state.totalAdsBlocked)", color: colors.statusAccent)
                    }
                }
                .padding(20)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.7), .large])
    }

    private func defenseRow(icon: String, title: String, detail: String, color: Color) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(color)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(colors.textPrimary)
                Text(detail)
                    .font(.system(size: 12))
                    .foregroundColor(colors.textSecondary)
            }

            Spacer()

            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 15))
                .foregroundColor(colors.statusSuccess)
        }
        .padding(14)
        .background(colors.surface)
    }

    private func statBox(title: String, count: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(count)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(color)
            Text(title)
                .font(.system(size: 12))
                .foregroundColor(colors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .background(colors.surfaceElevated)
        .cornerRadius(AzokleRadius.medium)
    }
}
