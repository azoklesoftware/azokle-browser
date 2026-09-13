import SwiftUI

public struct PrivacyShieldSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool
    @State private var protectionLevel: String = "Strict"

    let colors: AzokleColors = .dark

    private var currentHost: String {
        guard let urlString = state.activeTab?.url, let host = URL(string: urlString)?.host else {
            return "Current Website"
        }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    private var isHostShieldEnabled: Bool {
        state.isShieldEnabled(for: currentHost)
    }

    private var blockedRecords: [BlockedTrackerRecord] {
        state.activeTab?.blockedTrackerRecords ?? []
    }

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
                                .fill(isHostShieldEnabled ? colors.statusSuccess.opacity(0.15) : colors.textMuted.opacity(0.15))
                                .frame(width: 70, height: 70)

                            Image(systemName: isHostShieldEnabled ? "shield.fill" : "shield.slash.fill")
                                .font(.system(size: 34))
                                .foregroundColor(isHostShieldEnabled ? colors.statusSuccess : colors.textMuted)
                        }

                        Text(isHostShieldEnabled ? "Azokle Shield Active" : "Shield Paused")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(colors.textPrimary)

                        Text("Protecting your session against third-party trackers, invasive ad networks, and fingerprinting vectors on \(currentHost).")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)
                    }

                    // 2. Per-Site & Global Toggle Card
                    VStack(spacing: 12) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Shield on \(currentHost)")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(colors.textPrimary)
                                Text("Block ads, trackers, and canvas fingerprinting")
                                    .font(.system(size: 12))
                                    .foregroundColor(colors.textSecondary)
                            }
                            Spacer()
                            Toggle("", isOn: Binding(
                                get: { isHostShieldEnabled },
                                set: { state.setShieldEnabled($0, for: currentHost) }
                            ))
                            .labelsHidden()
                            .tint(colors.statusSuccess)
                        }
                    }
                    .padding(16)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.medium)

                    // 3. Blocked Trackers by Entity Breakdown (EntityLookup Parity)
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("TRACKERS BLOCKED ON THIS PAGE")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.textMuted)
                            Spacer()
                            Text("\(state.activeTab?.blockedTrackers ?? blockedRecords.count) Blocked")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(colors.statusSuccess)
                        }
                        .padding(.horizontal, 4)

                        if blockedRecords.isEmpty {
                            HStack(spacing: 12) {
                                Image(systemName: "checkmark.shield.fill")
                                    .font(.system(size: 22))
                                    .foregroundColor(colors.statusSuccess)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text("No Trackers Detected")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(colors.textPrimary)
                                    Text("No invasive tracking or profiling scripts found on this page.")
                                        .font(.system(size: 11))
                                        .foregroundColor(colors.textSecondary)
                                }
                                Spacer()
                            }
                            .padding(14)
                            .background(colors.surfaceElevated)
                            .cornerRadius(AzokleRadius.small)
                        } else {
                            VStack(spacing: 8) {
                                ForEach(blockedRecords) { record in
                                    HStack(spacing: 10) {
                                        Image(systemName: record.entity.category.iconName)
                                            .font(.system(size: 14))
                                            .foregroundColor(colors.statusAccent)
                                            .frame(width: 28, height: 28)
                                            .background(colors.statusAccent.opacity(0.12))
                                            .clipShape(Circle())

                                        VStack(alignment: .leading, spacing: 1) {
                                            Text(record.entity.displayName)
                                                .font(.system(size: 13, weight: .semibold))
                                                .foregroundColor(colors.textPrimary)
                                            Text(record.entity.category.rawValue)
                                                .font(.system(size: 11))
                                                .foregroundColor(colors.textSecondary)
                                        }

                                        Spacer()

                                        Text("\(record.blockCount)x")
                                            .font(.system(size: 12, weight: .bold))
                                            .foregroundColor(colors.textMuted)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(colors.cardBg)
                                            .cornerRadius(4)
                                    }
                                    .padding(10)
                                    .background(colors.surfaceElevated)
                                    .cornerRadius(AzokleRadius.small)
                                }
                            }
                        }
                    }

                    // 4. Cumulative Shield Savings
                    VStack(alignment: .leading, spacing: 10) {
                        Text("CUMULATIVE SHIELD SAVINGS")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.horizontal, 4)

                        HStack(spacing: 12) {
                            statBox(title: "Trackers Blocked", count: "\(AdBlockStats.shared.lifetimeTrackersBlocked + state.totalTrackersBlocked)", color: colors.statusSuccess)
                            statBox(title: "Ads Suppressed", count: "\(AdBlockStats.shared.lifetimeAdsBlocked + state.totalAdsBlocked)", color: colors.statusAccent)
                        }

                        HStack(spacing: 12) {
                            statBox(title: "Data Saved", count: String(format: "%.1f MB", AdBlockStats.shared.estimatedDataSavedMB), color: colors.statusTor)
                            statBox(title: "Time Saved", count: String(format: "%.1f s", AdBlockStats.shared.estimatedTimeSavedSeconds), color: colors.statusWarning)
                        }
                    }
                }
                .padding(20)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.75), .large])
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
