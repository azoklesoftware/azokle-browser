import SwiftUI

public struct DownloadsScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    let colors: AzokleColors = .dark

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                VStack(spacing: 0) {
                    if state.downloads.isEmpty {
                        VStack(spacing: 12) {
                            Spacer()
                            Image(systemName: "arrow.down.circle")
                                .font(.system(size: 44))
                                .foregroundColor(colors.textMuted)
                            Text("No downloads yet")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(colors.textSecondary)
                            Text("Files downloaded while browsing will appear here.")
                                .font(.system(size: 12))
                                .foregroundColor(colors.textMuted)
                            Spacer()
                        }
                    } else {
                        List {
                            ForEach(state.downloads) { item in
                                HStack(spacing: 12) {
                                    Image(systemName: "doc.fill")
                                        .font(.system(size: 18))
                                        .foregroundColor(colors.statusAccent)

                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(item.fileName)
                                            .font(.system(size: 14, weight: .medium))
                                            .foregroundColor(colors.textPrimary)
                                        Text(item.fileSize)
                                            .font(.system(size: 12))
                                            .foregroundColor(colors.textSecondary)
                                    }
                                    Spacer()
                                }
                                .listRowBackground(colors.surface)
                            }
                        }
                        .listStyle(.plain)
                    }
                }
            }
            .navigationTitle("Downloads")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                    .foregroundColor(colors.statusAccent)
                }
            }
        }
    }
}
