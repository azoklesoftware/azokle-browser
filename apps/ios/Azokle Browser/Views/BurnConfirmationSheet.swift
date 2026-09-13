import SwiftUI

public struct BurnConfirmationSheet: View {
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

            VStack(spacing: 20) {
                // 1. Glowing Flame Hero Icon
                ZStack {
                    Circle()
                        .fill(colors.statusFire.opacity(0.18))
                        .frame(width: 72, height: 72)
                        .overlay(
                            Circle()
                                .stroke(colors.statusFire.opacity(0.4), lineWidth: 1.5)
                        )

                    Image(systemName: "flame.fill")
                        .font(.system(size: 36))
                        .foregroundColor(colors.statusFire)
                }

                // 2. Title & Description
                VStack(spacing: 6) {
                    Text("Incinerate Browsing Data?")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundColor(colors.textPrimary)

                    Text("This will permanently close all tabs and purge all cookies, site cache, and browsing traces.")
                        .font(.system(size: 13))
                        .foregroundColor(colors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }

                // 3. Purged Items List
                VStack(spacing: 12) {
                    burnItemRow(icon: "square.stack.3d.up.fill", title: "All Active & Background Tabs", subtitle: "\(state.tabs.count) tabs will be closed")
                    burnItemRow(icon: "archivebox.fill", title: "Cookies & Website Storage", subtitle: "Active logins and persistent sessions wiped")
                    burnItemRow(icon: "clock.arrow.circlepath", title: "Temporary Cache & History", subtitle: "All cached media and browsing timeline purged")
                }
                .padding(14)
                .background(colors.surfaceElevated)
                .cornerRadius(AzokleRadius.medium)

                // 4. Action Buttons
                VStack(spacing: 10) {
                    Button(action: {
                        state.burnAllData()
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "flame.fill")
                                .font(.system(size: 16, weight: .bold))
                            Text("Burn Everything Now")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(colors.statusFire)
                        .cornerRadius(AzokleRadius.medium)
                    }

                    Button(action: {
                        isPresented = false
                    }) {
                        Text("Cancel")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(colors.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                }
                .padding(.top, 4)
            }
            .padding(20)
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.65)])
    }

    private func burnItemRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(colors.statusFire)
                .frame(width: 24)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(colors.textPrimary)
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundColor(colors.textMuted)
            }
            Spacer()
        }
    }
}
