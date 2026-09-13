import SwiftUI

public struct BookmarksScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    let colors: AzokleColors = .dark

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                VStack(spacing: 0) {
                    if state.bookmarks.isEmpty {
                        VStack(spacing: 12) {
                            Spacer()
                            Image(systemName: "book")
                                .font(.system(size: 44))
                                .foregroundColor(colors.textMuted)
                            Text("No bookmarks saved")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(colors.textSecondary)
                            Spacer()
                        }
                    } else {
                        List {
                            ForEach(state.bookmarks) { item in
                                Button(action: {
                                    state.navigateActiveTab(to: item.url)
                                    isPresented = false
                                }) {
                                    HStack(spacing: 12) {
                                        Image(systemName: "bookmark.fill")
                                            .font(.system(size: 16))
                                            .foregroundColor(colors.statusAccent)

                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(item.title)
                                                .font(.system(size: 14, weight: .medium))
                                                .foregroundColor(colors.textPrimary)
                                                .lineLimit(1)
                                            Text(item.url)
                                                .font(.system(size: 12))
                                                .foregroundColor(colors.textSecondary)
                                                .lineLimit(1)
                                        }
                                        Spacer()
                                    }
                                }
                                .listRowBackground(colors.surface)
                            }
                            .onDelete { indexSet in
                                state.bookmarks.remove(atOffsets: indexSet)
                            }
                        }
                        .listStyle(.plain)
                        .background(colors.background)
                    }
                }
            }
            .navigationTitle("Bookmarks")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        if let tab = state.activeTab {
                            state.toggleBookmark(url: tab.url, title: tab.title)
                        }
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                            Text("Add Current")
                        }
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(colors.statusAccent)
                    }
                }

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
