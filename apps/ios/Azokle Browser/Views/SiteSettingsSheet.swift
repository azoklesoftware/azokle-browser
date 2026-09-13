import SwiftUI

// MARK: - Site Settings & Permissions Sheet (1:1 Android SiteSettingsSheet Parity)
public struct SiteSettingsSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var showClearDataAlert: Bool = false
    @State private var siteDataClearedFeedback: Bool = false

    let colors: AzokleColors = .dark

    private var currentHost: String {
        guard let activeTab = state.activeTab,
              let url = URL(string: activeTab.url),
              let host = url.host else {
            return "search.azokle.com"
        }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 3)
                .fill(colors.border)
                .frame(width: 36, height: 4)
                .padding(.top, 12)
                .padding(.bottom, 10)

            // Header
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Site Settings")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Permissions & storage for \(currentHost)")
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
                        .overlay(
                            Circle().stroke(colors.border, lineWidth: 1)
                        )
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)

            Divider().background(colors.border)

            ScrollView {
                VStack(spacing: 16) {
                    // Host Card
                    HStack(spacing: 10) {
                        Image(systemName: "globe")
                            .font(.system(size: 18))
                            .foregroundColor(colors.statusAccent)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(currentHost)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(colors.textPrimary)
                            Text("Active Origin Sandbox")
                                .font(.system(size: 11))
                                .foregroundColor(colors.textSecondary)
                        }

                        Spacer()
                    }
                    .padding(14)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Permissions Group
                    VStack(alignment: .leading, spacing: 0) {
                        Text("PERMISSIONS")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.horizontal, 14)
                            .padding(.top, 12)
                            .padding(.bottom, 8)

                        ForEach(SitePermissionType.allCases) { perm in
                            permissionRow(perm)
                            if perm != SitePermissionType.allCases.last {
                                Divider().background(colors.border.opacity(0.6)).padding(.horizontal, 14)
                            }
                        }
                    }
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.medium)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    // Data Storage & Privacy Zone
                    VStack(alignment: .leading, spacing: 12) {
                        Text("DATA & ISOLATION")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)
                            .padding(.horizontal, 4)

                        Button(action: {
                            showClearDataAlert = true
                        }) {
                            HStack {
                                Image(systemName: "trash")
                                    .font(.system(size: 15))
                                    .foregroundColor(colors.statusDestructive)

                                Text("Clear Cookies & Site Storage")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(colors.statusDestructive)

                                Spacer()

                                if siteDataClearedFeedback {
                                    Text("Cleared!")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(colors.statusSuccess)
                                }
                            }
                            .padding(14)
                            .background(colors.surface)
                            .cornerRadius(AzokleRadius.medium)
                            .overlay(
                                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                    .stroke(colors.border, lineWidth: 1)
                            )
                        }
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.75), .large])
        .alert("Clear Site Data?", isPresented: $showClearDataAlert) {
            Button("Clear Data", role: .destructive) {
                state.clearSiteData(for: currentHost)
                withAnimation {
                    siteDataClearedFeedback = true
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    withAnimation {
                        siteDataClearedFeedback = false
                    }
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will remove all cookies, localStorage, and cached files for \(currentHost). You may be signed out.")
        }
    }

    private func permissionRow(_ perm: SitePermissionType) -> some View {
        let currentSetting = state.getPermission(for: currentHost, type: perm)

        return HStack(spacing: 12) {
            Image(systemName: perm.iconName)
                .font(.system(size: 16))
                .foregroundColor(colors.statusAccent)
                .frame(width: 24)

            Text(perm.rawValue)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(colors.textPrimary)

            Spacer()

            Menu {
                ForEach(SitePermissionState.allCases, id: \.self) { permState in
                    Button(action: {
                        state.setPermission(for: currentHost, type: perm, state: permState)
                    }) {
                        HStack {
                            Text(permState.rawValue)
                            if currentSetting == permState {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            } label: {
                HStack(spacing: 4) {
                    Text(currentSetting.rawValue)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(currentSetting == .deny ? colors.statusDestructive : (currentSetting == .allow ? colors.statusSuccess : colors.textSecondary))

                    Image(systemName: "chevron.up.chevron.down")
                        .font(.system(size: 9))
                        .foregroundColor(colors.textMuted)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(colors.surfaceElevated)
                .cornerRadius(AzokleRadius.small)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }
}
