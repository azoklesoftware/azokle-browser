import SwiftUI

// MARK: - Tab Group Detail & Edit Sheet (1:1 Android TabGroupDetailSheet.kt Parity)
public struct TabGroupDetailSheet: View {
    @ObservedObject var state: BrowserState
    let group: TabGroup
    @Binding var isPresented: Bool

    @State private var groupTitle: String = ""
    @State private var selectedColorHex: UInt32 = 0x38BDF8

    let colors: AzokleColors = .dark

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
                    Text("Tab Group")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Organize and color-code active sessions")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                Button("Done") {
                    saveChanges()
                    isPresented = false
                }
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(colors.statusAccent)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)

            Divider().background(colors.border)

            ScrollView {
                VStack(spacing: 20) {
                    // Group Title Input
                    VStack(alignment: .leading, spacing: 8) {
                        Text("GROUP NAME")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)

                        TextField("Enter group name...", text: $groupTitle)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(colors.textPrimary)
                            .padding(12)
                            .background(colors.surfaceElevated)
                            .cornerRadius(AzokleRadius.medium)
                            .overlay(
                                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                                    .stroke(Color(hex: selectedColorHex), lineWidth: 1.5)
                            )
                    }

                    // 8-Color Sovereign Palette Selector
                    VStack(alignment: .leading, spacing: 10) {
                        Text("COLOR TAG")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)

                        HStack(spacing: 12) {
                            ForEach(TabGroup.paletteHexes, id: \.self) { hex in
                                Button(action: {
                                    selectedColorHex = hex
                                    let gen = UIImpactFeedbackGenerator(style: .light)
                                    gen.impactOccurred()
                                }) {
                                    ZStack {
                                        Circle()
                                            .fill(Color(hex: hex))
                                            .frame(width: 34, height: 34)

                                        if selectedColorHex == hex {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 14, weight: .bold))
                                                .foregroundColor(.white)
                                        }
                                    }
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .center)
                    }
                    .padding(14)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)

                    // Tab Membership List
                    VStack(alignment: .leading, spacing: 10) {
                        Text("TABS IN THIS GROUP (\(group.tabIds.count))")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textMuted)

                        ForEach(state.tabs) { tab in
                            let isMember = group.tabIds.contains(tab.id)
                            Button(action: {
                                if isMember {
                                    state.removeTabFromGroup(tabId: tab.id, groupId: group.id)
                                } else {
                                    state.addTabToGroup(tabId: tab.id, groupId: group.id)
                                }
                            }) {
                                HStack(spacing: 12) {
                                    Image(systemName: isMember ? "checkmark.circle.fill" : "circle")
                                        .font(.system(size: 18))
                                        .foregroundColor(isMember ? Color(hex: selectedColorHex) : colors.textMuted)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(tab.title)
                                            .font(.system(size: 14, weight: .medium))
                                            .foregroundColor(colors.textPrimary)
                                            .lineLimit(1)
                                        Text(tab.url)
                                            .font(.system(size: 11))
                                            .foregroundColor(colors.textSecondary)
                                            .lineLimit(1)
                                    }

                                    Spacer()
                                }
                                .padding(10)
                                .background(colors.surfaceElevated)
                                .cornerRadius(AzokleRadius.small)
                            }
                        }
                    }
                    .padding(14)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.medium)

                    // Delete Group Action
                    Button(action: {
                        state.deleteTabGroup(id: group.id)
                        isPresented = false
                    }) {
                        HStack {
                            Image(systemName: "trash")
                            Text("Delete Tab Group")
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(colors.statusDestructive)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(colors.surface)
                        .cornerRadius(AzokleRadius.medium)
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.72), .large])
        .onAppear {
            groupTitle = group.title
            selectedColorHex = group.colorHex
        }
    }

    private func saveChanges() {
        if let index = state.tabGroups.firstIndex(where: { $0.id == group.id }) {
            state.tabGroups[index].title = groupTitle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "Group" : groupTitle
            state.tabGroups[index].colorHex = selectedColorHex
        }
    }
}
