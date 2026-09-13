import SwiftUI

public struct SafeBrowsingWarningOverlay: View {
    let threat: ThreatAssessment
    let onBackToSafety: () -> Void
    let onBypass: () -> Void

    let colors: AzokleColors = .dark

    public var body: some View {
        ZStack {
            Color(hex: 0x1A0505).ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                // Threat Icon Badge
                ZStack {
                    Circle()
                        .fill(colors.statusDestructive.opacity(0.18))
                        .frame(width: 80, height: 80)

                    Image(systemName: "exclamationmark.shield.fill")
                        .font(.system(size: 44))
                        .foregroundColor(colors.statusDestructive)
                }

                // Threat Title & Host
                VStack(spacing: 8) {
                    Text(threat.threatType.rawValue)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)

                    Text(threat.host)
                        .font(.system(size: 14, weight: .semibold, design: .monospaced))
                        .foregroundColor(colors.statusDestructive)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.08))
                        .cornerRadius(6)
                }

                // Threat Explanation Card
                Text(threat.threatType.description)
                    .font(.system(size: 13.5))
                    .foregroundColor(Color.white.opacity(0.8))
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.horizontal, 28)

                Spacer()

                // Actions
                VStack(spacing: 12) {
                    Button(action: onBackToSafety) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 14, weight: .bold))
                            Text("Back to Safety")
                                .font(.system(size: 15, weight: .bold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.white)
                        .foregroundColor(.black)
                        .cornerRadius(AzokleRadius.pill)
                    }

                    Button(action: onBypass) {
                        Text("Continue to unsafe site (Not Recommended)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(colors.textMuted)
                            .underline()
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
