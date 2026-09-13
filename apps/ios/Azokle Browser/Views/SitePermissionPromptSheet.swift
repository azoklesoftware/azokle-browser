import SwiftUI

// MARK: - Site Permission Request Prompt Sheet (1:1 Android SitePermissionPromptSheet Parity)
public struct SitePermissionPromptSheet: View {
    let request: SitePermissionRequest
    @ObservedObject var state: BrowserState
    @State private var rememberChoice: Bool = true

    let colors: AzokleColors = .dark

    private var cleanHost: String {
        request.host.hasPrefix("www.") ? String(request.host.dropFirst(4)) : request.host
    }

    public var body: some View {
        VStack(spacing: 20) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)

            // Permission Icon
            ZStack {
                Circle()
                    .fill(colors.statusAccent.opacity(0.12))
                    .frame(width: 64, height: 64)

                Image(systemName: request.type.iconName)
                    .font(.system(size: 28, weight: .semibold))
                    .foregroundColor(colors.statusAccent)
            }

            // Title & Description
            VStack(spacing: 6) {
                Text("\(cleanHost)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(colors.textPrimary)

                Text("Requests access to your \(request.type.rawValue)")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(colors.textSecondary)

                Text("This allows the website to interact with hardware sensors while the tab is active.")
                    .font(.system(size: 12))
                    .foregroundColor(colors.textMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
            }

            // Remember Choice Toggle
            Toggle(isOn: $rememberChoice) {
                Text("Remember decision for this website")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(colors.textSecondary)
            }
            .tint(colors.statusAccent)
            .padding(.horizontal, 24)

            // Action Buttons
            VStack(spacing: 10) {
                // 1. Allow While Using Website
                Button(action: {
                    if rememberChoice {
                        state.setPermission(for: cleanHost, type: request.type, state: .allow)
                    }
                    state.activePermissionPrompt = nil
                }) {
                    Text("Allow While Using Website")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.actionPrimaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(colors.actionPrimaryBg)
                        .cornerRadius(AzokleRadius.pill)
                }

                // 2. Allow This Time Only
                Button(action: {
                    state.activePermissionPrompt = nil
                }) {
                    Text("Allow Once")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(colors.textPrimary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(colors.surfaceElevated)
                        .cornerRadius(AzokleRadius.pill)
                        .overlay(
                            RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                .stroke(colors.border, lineWidth: 1)
                        )
                }

                // 3. Don't Allow
                Button(action: {
                    if rememberChoice {
                        state.setPermission(for: cleanHost, type: request.type, state: .deny)
                    }
                    state.activePermissionPrompt = nil
                }) {
                    Text("Don't Allow")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(colors.statusDestructive)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 16)
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.48)])
    }
}
