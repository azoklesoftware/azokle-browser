import SwiftUI

public struct TabsScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool
    let colors: AzokleColors = .dark

    private var filteredTabs: [BrowserTab] {
        switch state.selectedTabMode {
        case .standard:
            return state.tabs.filter { !$0.isPrivate && !$0.isTor }
        case .privateMode:
            return state.tabs.filter { $0.isPrivate && !$0.isTor }
        case .torMode:
            return state.tabs.filter { $0.isTor }
        }
    }

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Header & Segmented Mode Switcher
                HStack {
                    // Close All Tabs
                    Button(action: {
                        state.closeAllTabs(forMode: state.selectedTabMode)
                    }) {
                        Image(systemName: "trash")
                            .font(.system(size: 16))
                            .foregroundColor(colors.statusDestructive)
                            .padding(8)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                    }

                    Spacer()

                    // Tab Mode Segmented Pills
                    HStack(spacing: 4) {
                        modeTabButton(title: "Standard", mode: .standard)
                        modeTabButton(title: "Private", mode: .privateMode, icon: "eye.slash.fill")
                        modeTabButton(title: "Tor", mode: .torMode, icon: "network.badge.shield.half.filled")
                    }
                    .padding(3)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.pill)

                    Spacer()

                    // Done Button
                    Button(action: {
                        isPresented = false
                    }) {
                        Text("Done")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(colors.statusAccent)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 12)
                .background(colors.surface)

                Divider()
                    .background(colors.border)

                // 2. Tabs Grid
                if filteredTabs.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: emptyStateIcon)
                            .font(.system(size: 48))
                            .foregroundColor(colors.textMuted)

                        Text(emptyStateText)
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(colors.textSecondary)

                        Button(action: {
                            openNewTabForCurrentMode()
                        }) {
                            Text("+ Open New Tab")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(colors.actionPrimaryText)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(colors.actionPrimaryBg)
                                .cornerRadius(AzokleRadius.medium)
                        }
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVGrid(columns: [
                            GridItem(.flexible(), spacing: 14),
                            GridItem(.flexible(), spacing: 14)
                        ], spacing: 14) {
                            ForEach(filteredTabs) { tab in
                                tabCard(tab: tab)
                            }
                        }
                        .padding(16)
                    }
                }

                // 3. Bottom Bar with New Tab (+) Button
                VStack(spacing: 0) {
                    Divider()
                        .background(colors.border)

                    HStack {
                        Spacer()

                        Button(action: {
                            openNewTabForCurrentMode()
                            isPresented = false
                        }) {
                            HStack(spacing: 6) {
                                Image(systemName: "plus")
                                    .font(.system(size: 14, weight: .bold))
                                Text("New Tab")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundColor(colors.actionPrimaryText)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 10)
                            .background(colors.actionPrimaryBg)
                            .cornerRadius(AzokleRadius.pill)
                        }

                        Spacer()
                    }
                    .padding(.vertical, 12)
                    .background(colors.surface)
                }
            }
        }
    }

    private func tabCard(tab: BrowserTab) -> some View {
        let isSelected = tab.id == state.activeTabId

        return Button(action: {
            state.selectTab(id: tab.id)
            isPresented = false
        }) {
            VStack(spacing: 0) {
                // Card Header (Favicon + Title + Close Button)
                HStack(spacing: 8) {
                    Image(systemName: "globe")
                        .font(.system(size: 12))
                        .foregroundColor(colors.statusAccent)

                    Text(tab.title)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(colors.textPrimary)
                        .lineLimit(1)

                    Spacer(minLength: 0)

                    Button(action: {
                        state.closeTab(id: tab.id)
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 22, height: 22)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                    }
                }
                .padding(10)
                .background(colors.surfaceElevated)

                // Card Body / Thumbnail Preview
                ZStack {
                    colors.cardBg

                    VStack(spacing: 6) {
                        if tab.isTor {
                            Image(systemName: "network.badge.shield.half.filled")
                                .font(.system(size: 28))
                                .foregroundColor(colors.statusTor)
                            Text("Tor Encrypted Circuit")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(colors.textMuted)
                        } else if tab.isPrivate {
                            Image(systemName: "eye.slash.fill")
                                .font(.system(size: 28))
                                .foregroundColor(colors.statusWarning)
                            Text("Private Browsing")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(colors.textMuted)
                        } else {
                            Image(systemName: "safari")
                                .font(.system(size: 28))
                                .foregroundColor(colors.textMuted)
                            Text(tab.url)
                                .font(.system(size: 10))
                                .foregroundColor(colors.textMuted)
                                .lineLimit(2)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 8)
                        }
                    }
                }
                .frame(height: 140)
            }
            .clipShape(RoundedRectangle(cornerRadius: AzokleRadius.medium))
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: isSelected ? 2 : 1)
            )
        }
    }

    private func modeTabButton(title: String, mode: TabFilterMode, icon: String? = nil) -> some View {
        let isSelected = state.selectedTabMode == mode
        return Button(action: {
            state.selectedTabMode = mode
        }) {
            HStack(spacing: 4) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 11))
                }
                Text(title)
                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(isSelected ? colors.actionPrimaryBg : Color.clear)
            .foregroundColor(isSelected ? colors.actionPrimaryText : colors.textSecondary)
            .cornerRadius(AzokleRadius.pill)
        }
    }

    private var emptyStateIcon: String {
        switch state.selectedTabMode {
        case .standard: return "square.stack.3d.up.slash"
        case .privateMode: return "eye.slash"
        case .torMode: return "network.badge.shield.half.filled"
        }
    }

    private var emptyStateText: String {
        switch state.selectedTabMode {
        case .standard: return "No standard tabs open"
        case .privateMode: return "No private tabs open"
        case .torMode: return "No Tor circuits open"
        }
    }

    private func openNewTabForCurrentMode() {
        switch state.selectedTabMode {
        case .standard:
            state.openNewTab()
        case .privateMode:
            state.openNewTab(isPrivate: true)
        case .torMode:
            state.openNewTab(isTor: true)
        }
    }
}
