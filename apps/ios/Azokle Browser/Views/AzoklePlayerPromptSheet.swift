import SwiftUI

public struct AzoklePlayerPromptSheet: View {
    let videoUrl: String
    let onWatchInPlayer: (_ rememberChoice: Bool) -> Void
    let onWatchOnPlatform: (_ rememberChoice: Bool) -> Void
    @Binding var isPresented: Bool

    @State private var rememberChoice: Bool = false
    let colors: AzokleColors = .dark

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 10)
                .padding(.bottom, 16)

            VStack(spacing: 16) {
                // Player Icon Badge
                ZStack {
                    Circle()
                        .fill(colors.statusTor.opacity(0.15))
                        .frame(width: 60, height: 60)

                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 36))
                        .foregroundColor(colors.statusTor)
                }

                // Title & Subtitle
                VStack(spacing: 6) {
                    Text("Watch in Azokle Player?")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)

                    Text("Azokle Player strips tracking cookies, blocks pre-roll video ads, and keeps your watch history completely private.")
                        .font(.system(size: 13))
                        .foregroundColor(colors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                }

                // Feature Highlights Container
                VStack(spacing: 10) {
                    featureRow(icon: "shield.fill", text: "No invasive pre-roll or mid-roll video ads", tint: colors.statusSuccess)
                    featureRow(icon: "lock.fill", text: "Zero Google profile tracking or behavioral profiling", tint: colors.statusAccent)
                    featureRow(icon: "eye.slash.fill", text: "Private, distraction-free playback environment", tint: colors.statusTor)
                }
                .padding(14)
                .background(colors.surfaceElevated)
                .cornerRadius(AzokleRadius.medium)
                .overlay(
                    RoundedRectangle(cornerRadius: AzokleRadius.medium)
                        .stroke(colors.border, lineWidth: 1)
                )

                // Remember Choice Toggle
                Button(action: {
                    rememberChoice.toggle()
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: rememberChoice ? "checkmark.square.fill" : "square")
                            .font(.system(size: 16))
                            .foregroundColor(rememberChoice ? colors.statusTor : colors.textMuted)

                        Text("Always use this choice for video links")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(colors.textSecondary)
                    }
                }
                .padding(.vertical, 4)

                // Action Buttons
                VStack(spacing: 10) {
                    Button(action: {
                        isPresented = false
                        onWatchInPlayer(rememberChoice)
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 14, weight: .bold))
                            Text("Open in Azokle Player")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(colors.statusTor)
                        .foregroundColor(.white)
                        .cornerRadius(AzokleRadius.medium)
                    }

                    Button(action: {
                        isPresented = false
                        onWatchOnPlatform(rememberChoice)
                    }) {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.up.right.square")
                                .font(.system(size: 14))
                            Text("Continue to Standard Website")
                                .font(.system(size: 13, weight: .medium))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 11)
                        .background(colors.surfaceElevated)
                        .foregroundColor(colors.textSecondary)
                        .cornerRadius(AzokleRadius.medium)
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
        .background(colors.cardBg)
        .presentationDetents([.fraction(0.68)])
    }

    private func featureRow(icon: String, text: String, tint: Color) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: 20)

            Text(text)
                .font(.system(size: 12.5, weight: .medium))
                .foregroundColor(colors.textPrimary)

            Spacer()
        }
    }
}
