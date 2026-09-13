import SwiftUI

public struct ProUpgradeSheet: View {
    @Binding var isPresented: Bool
    @State private var selectedPlan: String = "annual"

    let colors: AzokleColors = .dark

    let perks = [
        ("network.badge.shield.half.filled", "Multi-Hop Tor & Private Onion Relays", "Dedicated high-bandwidth entry guards with zero logging"),
        ("key.fill", "Encrypted Passkey Cloud Vault Sync", "Zero-knowledge end-to-end encrypted passkey synchronization"),
        ("shield.lefthalf.filled", "Full-Spectrum Ad & Tracker Annihilator", "Deep network cosmetic filtering and anti-circumvention rules"),
        ("lock.circle.fill", "Zero-Log Encrypted DNS (DoH)", "Custom sovereign upstream resolver with DNSSEC enforcement"),
        ("sparkles", "VIP Priority Sovereign Search", "Ad-free zero-telemetry index with instant neural results")
    ]

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
                    // Badge Header
                    HStack(spacing: 6) {
                        Image(systemName: "sparkles")
                            .font(.system(size: 13, weight: .bold))
                        Text("AZOKLE PRO")
                            .font(.system(size: 12, weight: .bold, design: .monospaced))
                    }
                    .foregroundColor(colors.actionPrimaryText)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(colors.actionPrimaryBg)
                    .cornerRadius(AzokleRadius.pill)

                    // Title
                    VStack(spacing: 6) {
                        Text("Upgrade to Sovereign Pro")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(colors.textPrimary)

                        Text("Unlock absolute sovereignty, unmetered multi-hop onion circuits, and encrypted credential vault synchronization.")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textSecondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)
                    }

                    // Perks List
                    VStack(spacing: 12) {
                        ForEach(perks, id: \.0) { perk in
                            HStack(spacing: 12) {
                                ZStack {
                                    Circle()
                                        .fill(colors.statusAccent.opacity(0.12))
                                        .frame(width: 36, height: 36)

                                    Image(systemName: perk.0)
                                        .font(.system(size: 15))
                                        .foregroundColor(colors.statusAccent)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(perk.1)
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(colors.textPrimary)
                                    Text(perk.2)
                                        .font(.system(size: 11))
                                        .foregroundColor(colors.textSecondary)
                                }

                                Spacer()
                            }
                        }
                    }
                    .padding(16)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Plan Selector
                    HStack(spacing: 12) {
                        planCard(
                            id: "annual",
                            title: "Annual Sovereign",
                            price: "$39.99 / yr",
                            savings: "Save 33%",
                            isBestValue: true
                        )

                        planCard(
                            id: "monthly",
                            title: "Monthly Sovereign",
                            price: "$4.99 / mo",
                            savings: nil,
                            isBestValue: false
                        )
                    }

                    // Upgrade Button
                    Button(action: {
                        isPresented = false
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "crown.fill")
                                .font(.system(size: 14, weight: .bold))
                            Text("Activate Sovereign Pro")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(colors.statusAccent)
                        .foregroundColor(.black)
                        .cornerRadius(AzokleRadius.pill)
                        .shadow(color: colors.statusAccent.opacity(0.3), radius: 8, x: 0, y: 4)
                    }

                    Text("Cancel anytime in App Store Subscriptions. Zero tracking.")
                        .font(.system(size: 11))
                        .foregroundColor(colors.textMuted)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.85), .large])
    }

    private func planCard(id: String, title: String, price: String, savings: String?, isBestValue: Bool) -> some View {
        let isSelected = selectedPlan == id

        return Button(action: {
            selectedPlan = id
        }) {
            VStack(spacing: 6) {
                if let savings = savings {
                    Text(savings)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(colors.statusSuccess)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(colors.statusSuccess.opacity(0.15))
                        .cornerRadius(4)
                } else {
                    Spacer().frame(height: 16)
                }

                Text(title)
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text(price)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(isSelected ? colors.statusAccent : colors.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(isSelected ? colors.surfaceElevated : colors.cardBg)
            .cornerRadius(AzokleRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: isSelected ? 2 : 1)
            )
        }
    }
}
