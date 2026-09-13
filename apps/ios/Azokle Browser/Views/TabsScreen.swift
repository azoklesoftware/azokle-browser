import SwiftUI

public struct TabsScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var searchQuery: String = ""
    @State private var showCloseAllAlert: Bool = false

    let colors: AzokleColors = .dark

    private var standardCount: Int {
        state.tabs.filter { !$0.isPrivate && !$0.isTor }.count
    }

    private var privateCount: Int {
        state.tabs.filter { $0.isPrivate && !$0.isTor }.count
    }

    private var torCount: Int {
        state.tabs.filter { $0.isTor }.count
    }

    private var filteredTabs: [BrowserTab] {
        let baseList: [BrowserTab]
        switch state.selectedTabMode {
        case .standard:
            baseList = state.tabs.filter { !$0.isPrivate && !$0.isTor }
        case .privateMode:
            baseList = state.tabs.filter { $0.isPrivate && !$0.isTor }
        case .torMode:
            baseList = state.tabs.filter { $0.isTor }
        }

        if searchQuery.isEmpty {
            return baseList.reversed() // Newest tabs first, matching Android
        } else {
            return baseList.filter {
                $0.title.localizedCaseInsensitiveContains(searchQuery) ||
                $0.url.localizedCaseInsensitiveContains(searchQuery)
            }.reversed()
        }
    }

    public var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top Header (Segmented Mode Switcher + Done)
                HStack {
                    // Close All Tabs (Trash Sweep)
                    Button(action: {
                        showCloseAllAlert = true
                    }) {
                        Image(systemName: "trash")
                            .font(.system(size: 15))
                            .foregroundColor(colors.statusDestructive)
                            .padding(8)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                    }

                    // New Tab Group Button
                    Button(action: {
                        let newGroup = state.createTabGroup(title: "New Group")
                        state.activeTabGroupForDetail = newGroup
                    }) {
                        Image(systemName: "folder.badge.plus")
                            .font(.system(size: 14))
                            .foregroundColor(colors.statusAccent)
                            .padding(8)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                    }

                    Spacer()

                    // 3-Way Segmented Pill Switcher with Counts
                    HStack(spacing: 2) {
                        modePill(
                            title: "Tabs (\(standardCount))",
                            mode: .standard,
                            icon: "square.stack.3d.up.fill"
                        )
                        modePill(
                            title: "Private (\(privateCount))",
                            mode: .privateMode,
                            icon: "eye.slash.fill"
                        )
                        modePill(
                            title: "Tor (\(torCount))",
                            mode: .torMode,
                            icon: "network.badge.shield.half.filled"
                        )
                    }
                    .padding(3)
                    .background(colors.cardBg)
                    .cornerRadius(AzokleRadius.pill)
                    .overlay(
                        RoundedRectangle(cornerRadius: AzokleRadius.pill)
                            .stroke(colors.border, lineWidth: 1)
                    )

                    Spacer()

                    // Done Button
                    Button(action: {
                        isPresented = false
                    }) {
                        Text("Done")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(colors.statusAccent)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 14)
                .padding(.bottom, 10)
                .background(colors.surface)

                // 2. Search Tabs Filter Bar
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 14))
                        .foregroundColor(colors.textMuted)

                    TextField("Search open tabs...", text: $searchQuery)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(colors.textPrimary)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)

                    if !searchQuery.isEmpty {
                        Button(action: {
                            searchQuery = ""
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .font(.system(size: 14))
                                .foregroundColor(colors.textMuted)
                        }
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(colors.cardBg)
                .cornerRadius(AzokleRadius.medium)
                .overlay(
                    RoundedRectangle(cornerRadius: AzokleRadius.medium)
                        .stroke(colors.border, lineWidth: 0.8)
                )
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

                Divider()
                    .background(colors.border)

                // 3. Tabs Grid
                if filteredTabs.isEmpty {
                    VStack(spacing: 16) {
                        Spacer()
                        Image(systemName: emptyStateIcon)
                            .font(.system(size: 48))
                            .foregroundColor(colors.textMuted)

                        Text(emptyStateText)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(colors.textPrimary)

                        Button(action: {
                            openNewTabForCurrentMode()
                        }) {
                            Text("+ Open New Tab")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(colors.actionPrimaryText)
                                .padding(.horizontal, 22)
                                .padding(.vertical, 10)
                                .background(colors.actionPrimaryBg)
                                .cornerRadius(AzokleRadius.pill)
                        }
                        Spacer()
                    }
                } else if state.selectedTabMode == .standard && !state.tabGroups.isEmpty {
                    ScrollView {
                        VStack(spacing: 20) {
                            // Render Tab Groups (Mirroring Android ActiveTabsScreen.kt)
                            ForEach(state.tabGroups) { group in
                                let groupTabs = filteredTabs.filter { group.tabIds.contains($0.id) }
                                VStack(alignment: .leading, spacing: 10) {
                                    HStack(spacing: 8) {
                                        Circle()
                                            .fill(Color(hex: group.colorHex))
                                            .frame(width: 12, height: 12)

                                        Text(group.title)
                                            .font(.system(size: 15, weight: .bold))
                                            .foregroundColor(colors.textPrimary)

                                        Text("\(groupTabs.count)")
                                            .font(.system(size: 11, weight: .bold))
                                            .foregroundColor(colors.textSecondary)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(colors.surfaceElevated)
                                            .cornerRadius(8)

                                        Spacer()

                                        Button(action: {
                                            state.activeTabGroupForDetail = group
                                        }) {
                                            Image(systemName: "pencil")
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(colors.textSecondary)
                                                .padding(6)
                                                .background(colors.surfaceElevated)
                                                .clipShape(Circle())
                                        }

                                        Button(action: {
                                            withAnimation {
                                                state.toggleTabGroupCollapse(id: group.id)
                                            }
                                        }) {
                                            Image(systemName: group.isCollapsed ? "chevron.right" : "chevron.down")
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(colors.textMuted)
                                                .padding(6)
                                        }
                                    }
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .background(Color(hex: group.colorHex).opacity(0.14))
                                    .cornerRadius(AzokleRadius.small)

                                    if !group.isCollapsed {
                                        if groupTabs.isEmpty {
                                            Text("No tabs in this group. Tap the pencil icon to manage tabs.")
                                                .font(.system(size: 12))
                                                .foregroundColor(colors.textMuted)
                                                .padding(.horizontal, 14)
                                                .padding(.vertical, 6)
                                        } else {
                                            LazyVGrid(columns: [
                                                GridItem(.flexible(), spacing: 14),
                                                GridItem(.flexible(), spacing: 14)
                                            ], spacing: 14) {
                                                ForEach(groupTabs) { tab in
                                                    tabCard(tab: tab, groupColorHex: group.colorHex)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Ungrouped Tabs Section
                            let groupedIds = Set(state.tabGroups.flatMap { $0.tabIds })
                            let ungroupedTabs = filteredTabs.filter { !groupedIds.contains($0.id) }
                            if !ungroupedTabs.isEmpty {
                                VStack(alignment: .leading, spacing: 10) {
                                    Text("Ungrouped Tabs")
                                        .font(.system(size: 13, weight: .semibold))
                                        .foregroundColor(colors.textMuted)
                                        .padding(.horizontal, 4)

                                    LazyVGrid(columns: [
                                        GridItem(.flexible(), spacing: 14),
                                        GridItem(.flexible(), spacing: 14)
                                    ], spacing: 14) {
                                        ForEach(ungroupedTabs) { tab in
                                            tabCard(tab: tab)
                                        }
                                    }
                                }
                            }
                        }
                        .padding(16)
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

                // 4. Bottom Floating New Tab Button
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
                                    .font(.system(size: 14, weight: .bold))
                            }
                            .foregroundColor(colors.actionPrimaryText)
                            .padding(.horizontal, 26)
                            .padding(.vertical, 11)
                            .background(colors.actionPrimaryBg)
                            .cornerRadius(AzokleRadius.pill)
                            .shadow(color: Color.black.opacity(0.4), radius: 8, x: 0, y: 4)
                        }

                        Spacer()
                    }
                    .padding(.vertical, 12)
                    .background(colors.surface)
                }
            }
        }
        .alert("Close All Tabs?", isPresented: $showCloseAllAlert) {
            Button("Close All", role: .destructive) {
                state.closeAllTabs(forMode: state.selectedTabMode)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to close all tabs in \(state.selectedTabMode.rawValue) mode?")
        }
    }

    private func tabCard(tab: BrowserTab, groupColorHex: UInt32? = nil) -> some View {
        let isSelected = tab.id == state.activeTabId

        return VStack(spacing: 0) {
            // Card Header (Favicon + Title + Close Button)
            HStack(spacing: 8) {
                if let groupColorHex = groupColorHex {
                    Circle()
                        .fill(Color(hex: groupColorHex))
                        .frame(width: 8, height: 8)
                } else {
                    Image(systemName: "globe")
                        .font(.system(size: 12))
                        .foregroundColor(colors.statusAccent)
                }

                Text(tab.title)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(colors.textPrimary)
                    .lineLimit(1)

                Spacer(minLength: 0)

                Button(action: {
                    state.closeTab(id: tab.id)
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(colors.textSecondary)
                        .frame(width: 22, height: 22)
                        .background(colors.surfaceElevated)
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(colors.surfaceElevated)

            // Card Body / Thumbnail Preview
            ZStack {
                colors.cardBg

                VStack(spacing: 6) {
                    if tab.isTor {
                        Image(systemName: "network.badge.shield.half.filled")
                            .font(.system(size: 30))
                            .foregroundColor(colors.statusTor)
                        Text("Tor Encrypted Circuit")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.statusTor)
                    } else if tab.isPrivate {
                        Image(systemName: "eye.slash.fill")
                            .font(.system(size: 30))
                            .foregroundColor(colors.statusWarning)
                        Text("Private Browsing")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.statusWarning)
                    } else {
                        Image(systemName: "safari")
                            .font(.system(size: 28))
                            .foregroundColor(colors.textMuted)
                        Text(displayDomain(for: tab.url))
                            .font(.system(size: 11))
                            .foregroundColor(colors.textMuted)
                            .lineLimit(1)
                    }
                }
                .padding(10)
            }
            .frame(height: 140)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            state.selectTab(id: tab.id)
            isPresented = false
        }
        .clipShape(RoundedRectangle(cornerRadius: AzokleRadius.medium))
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                .stroke(isSelected ? (groupColorHex != nil ? Color(hex: groupColorHex!) : colors.statusAccent) : colors.border, lineWidth: isSelected ? 2 : 1)
        )
        .shadow(color: Color.black.opacity(0.3), radius: 6, x: 0, y: 3)
    }

    private func modePill(title: String, mode: TabFilterMode, icon: String) -> some View {
        let isSelected = state.selectedTabMode == mode
        return Button(action: {
            state.selectedTabMode = mode
        }) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 11, weight: .semibold))
                Text(title)
                    .font(.system(size: 11, weight: isSelected ? .bold : .medium))
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(isSelected ? colors.actionPrimaryBg : Color.clear)
            .foregroundColor(isSelected ? colors.actionPrimaryText : colors.textMuted)
            .cornerRadius(AzokleRadius.pill)
        }
    }

    private func displayDomain(for urlString: String) -> String {
        if let url = URL(string: urlString), let host = url.host {
            let trimmed = host.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.hasPrefix("www.") {
                return String(trimmed.dropFirst(4))
            }
            return trimmed
        }
        return urlString
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
        case .standard: return "No regular tabs open"
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
