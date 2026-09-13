import SwiftUI

public struct DownloadsScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var selectedFilter: String = "All"
    let colors: AzokleColors = .dark

    let filterCategories = ["All", "Media", "Documents"]

    private var filteredDownloads: [DownloadItem] {
        switch selectedFilter {
        case "Media":
            return state.downloads.filter {
                let name = $0.fileName.lowercased()
                return name.hasSuffix(".mp4") || name.hasSuffix(".mp3") || name.hasSuffix(".jpg") || name.hasSuffix(".png") || name.hasSuffix(".mov")
            }
        case "Documents":
            return state.downloads.filter {
                let name = $0.fileName.lowercased()
                return name.hasSuffix(".pdf") || name.hasSuffix(".doc") || name.hasSuffix(".docx") || name.hasSuffix(".txt") || name.hasSuffix(".zip")
            }
        default:
            return state.downloads
        }
    }

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                VStack(spacing: 0) {
                    // Category Filter Pills
                    HStack(spacing: 8) {
                        ForEach(filterCategories, id: \.self) { category in
                            let isSelected = selectedFilter == category
                            Button(action: {
                                selectedFilter = category
                            }) {
                                Text(category)
                                    .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 6)
                                    .background(isSelected ? colors.actionPrimaryBg : colors.surfaceElevated)
                                    .foregroundColor(isSelected ? colors.actionPrimaryText : colors.textSecondary)
                                    .cornerRadius(AzokleRadius.pill)
                            }
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)

                    Divider().background(colors.border)

                    if filteredDownloads.isEmpty {
                        VStack(spacing: 12) {
                            Spacer()
                            Image(systemName: "arrow.down.circle")
                                .font(.system(size: 44))
                                .foregroundColor(colors.textMuted)
                            Text("No downloads yet")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(colors.textPrimary)
                            Text("Files downloaded while browsing will appear here.")
                                .font(.system(size: 12))
                                .foregroundColor(colors.textSecondary)
                            Spacer()
                        }
                    } else {
                        List {
                            ForEach(filteredDownloads) { item in
                                downloadRow(item)
                            }
                            .onDelete { indexSet in
                                let toDelete = indexSet.map { filteredDownloads[$0] }
                                state.downloads.removeAll { item in toDelete.contains { $0.id == item.id } }
                            }
                        }
                        .listStyle(.plain)
                    }
                }
            }
            .navigationTitle("Downloads")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if !state.downloads.isEmpty {
                        Button("Clear All") {
                            state.downloads.removeAll()
                        }
                        .font(.system(size: 13))
                        .foregroundColor(colors.statusDestructive)
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(colors.statusAccent)
                }
            }
        }
    }

    private func downloadRow(_ item: DownloadItem) -> some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(fileIconColor(for: item.fileName).opacity(0.15))
                    .frame(width: 38, height: 38)

                Image(systemName: fileIconName(for: item.fileName))
                    .font(.system(size: 18))
                    .foregroundColor(fileIconColor(for: item.fileName))
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(item.fileName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(colors.textPrimary)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Text(item.fileSize)
                        .font(.system(size: 11))
                        .foregroundColor(colors.textSecondary)

                    Text("•")
                        .foregroundColor(colors.textMuted)

                    Text(item.date.formatted(date: .abbreviated, time: .omitted))
                        .font(.system(size: 11))
                        .foregroundColor(colors.textMuted)

                    Spacer()

                    Text("Completed")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(colors.statusSuccess)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(colors.statusSuccess.opacity(0.12))
                        .cornerRadius(4)
                }
            }
        }
        .padding(.vertical, 4)
        .listRowBackground(colors.surface)
    }

    private func fileIconName(for fileName: String) -> String {
        let name = fileName.lowercased()
        if name.hasSuffix(".pdf") { return "doc.richtext" }
        if name.hasSuffix(".jpg") || name.hasSuffix(".png") || name.hasSuffix(".webp") { return "photo" }
        if name.hasSuffix(".mp4") || name.hasSuffix(".mov") { return "video" }
        if name.hasSuffix(".mp3") || name.hasSuffix(".wav") { return "music.note" }
        if name.hasSuffix(".zip") || name.hasSuffix(".tar") || name.hasSuffix(".gz") { return "doc.zipper" }
        return "doc.fill"
    }

    private func fileIconColor(for fileName: String) -> Color {
        let name = fileName.lowercased()
        if name.hasSuffix(".pdf") { return .red }
        if name.hasSuffix(".jpg") || name.hasSuffix(".png") { return .purple }
        if name.hasSuffix(".mp4") || name.hasSuffix(".mov") { return .blue }
        if name.hasSuffix(".zip") || name.hasSuffix(".tar") { return .orange }
        return colors.statusAccent
    }
}

