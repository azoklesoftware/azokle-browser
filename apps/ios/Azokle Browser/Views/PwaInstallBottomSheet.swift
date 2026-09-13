import SwiftUI

// MARK: - PWA & Web App Install Sheet (1:1 Android PwaInstallBottomSheet.kt Parity)
public struct PwaInstallBottomSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var appTitle: String = ""
    @State private var isInstalled: Bool = false

    let colors: AzokleColors = .dark

    private var activeUrl: String {
        state.activeTab?.url ?? "https://search.azokle.com"
    }

    private var currentHost: String {
        guard let url = URL(string: activeUrl), let host = url.host else { return "website.com" }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)
                .padding(.bottom, 12)

            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Install Web App")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Run as standalone sovereign application")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                Button(action: {
                    isPresented = false
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(colors.textSecondary)
                        .frame(width: 32, height: 32)
                        .background(colors.surfaceElevated)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(colors.border, lineWidth: 1))
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)

            Divider().background(colors.border)

            ScrollView {
                VStack(spacing: 20) {
                    // App Icon & Info Card
                    HStack(spacing: 16) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 16)
                                .fill(colors.surfaceElevated)
                                .frame(width: 64, height: 64)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(colors.border, lineWidth: 1)
                                )

                            Image(systemName: "app.badge.fill")
                                .font(.system(size: 32))
                                .foregroundColor(colors.statusAccent)
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            TextField("App Name", text: $appTitle)
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(colors.textPrimary)

                            Text(currentHost)
                                .font(.system(size: 12, design: .monospaced))
                                .foregroundColor(colors.textSecondary)

                            HStack(spacing: 6) {
                                Text("STANDALONE")
                                    .font(.system(size: 9, weight: .bold))
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(colors.statusSuccess.opacity(0.18))
                                    .foregroundColor(colors.statusSuccess)
                                    .cornerRadius(4)

                                Text("OFFLINE READY")
                                    .font(.system(size: 9, weight: .bold))
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(colors.statusAccent.opacity(0.18))
                                    .foregroundColor(colors.statusAccent)
                                    .cornerRadius(4)
                            }
                        }

                        Spacer()
                    }
                    .padding(16)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)

                    // iOS Home Screen Instructions Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("HOW TO ADD TO HOME SCREEN")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)

                        stepRow(number: "1", text: "Tap the Share button in the browser menu")
                        stepRow(number: "2", text: "Scroll down and select 'Add to Home Screen'")
                        stepRow(number: "3", text: "Tap 'Add' to launch full-screen without URL bar")
                    }
                    .padding(16)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)

                    // Action Button
                    Button(action: {
                        isPresented = false
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                            state.showShareSheet = true
                        }
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "plus.app.fill")
                            Text("Open Share & Add to Home Screen")
                        }
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(colors.actionPrimaryText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(colors.actionPrimaryBg)
                        .cornerRadius(AzokleRadius.pill)
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.62)])
        .onAppear {
            appTitle = state.activeTab?.title ?? currentHost.capitalized
        }
    }

    private func stepRow(number: String, text: String) -> some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(colors.surfaceElevated)
                    .frame(width: 24, height: 24)
                Text(number)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(colors.statusAccent)
            }

            Text(text)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(colors.textPrimary)

            Spacer()
        }
    }
}
