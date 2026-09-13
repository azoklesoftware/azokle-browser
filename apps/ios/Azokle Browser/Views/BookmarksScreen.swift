import SwiftUI

public struct BookmarksScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var searchQuery: String = ""
    @State private var showAddSheet: Bool = false
    @State private var newTitle: String = ""
    @State private var newUrl: String = ""

    let colors: AzokleColors = .dark

    private var filteredBookmarks: [BookmarkItem] {
        if searchQuery.isEmpty {
            return state.bookmarks
        } else {
            return state.bookmarks.filter {
                $0.title.localizedCaseInsensitiveContains(searchQuery) ||
                $0.url.localizedCaseInsensitiveContains(searchQuery)
            }
        }
    }

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                VStack(spacing: 0) {
                    // Search Filter Bar
                    HStack(spacing: 8) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textMuted)

                        TextField("Search bookmarks...", text: $searchQuery)
                            .font(.system(size: 13))
                            .foregroundColor(colors.textPrimary)
                            .autocapitalization(.none)

                        if !searchQuery.isEmpty {
                            Button(action: { searchQuery = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 13))
                                    .foregroundColor(colors.textMuted)
                            }
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.small)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)

                    Divider().background(colors.border)

                    if filteredBookmarks.isEmpty {
                        VStack(spacing: 12) {
                            Spacer()
                            Image(systemName: "bookmark")
                                .font(.system(size: 44))
                                .foregroundColor(colors.textMuted)
                            Text(state.bookmarks.isEmpty ? "No bookmarks saved" : "No matching bookmarks")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(colors.textPrimary)
                            Text("Bookmarks you save while browsing will appear here.")
                                .font(.system(size: 12))
                                .foregroundColor(colors.textSecondary)
                            Spacer()
                        }
                    } else {
                        List {
                            ForEach(filteredBookmarks) { item in
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
                                let itemsToDelete = indexSet.map { filteredBookmarks[$0] }
                                state.bookmarks.removeAll { item in itemsToDelete.contains { $0.id == item.id } }
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
                            newTitle = tab.title
                            newUrl = tab.url
                        } else {
                            newTitle = ""
                            newUrl = "https://"
                        }
                        showAddSheet = true
                    }) {
                        HStack(spacing: 4) {
                            Image(systemName: "plus")
                            Text("New")
                        }
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(colors.statusAccent)
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
            .sheet(isPresented: $showAddSheet) {
                NavigationView {
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Title")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(colors.textSecondary)
                            TextField("Bookmark Title", text: $newTitle)
                                .padding(10)
                                .background(colors.surfaceElevated)
                                .cornerRadius(8)
                        }

                        VStack(alignment: .leading, spacing: 6) {
                            Text("Address (URL)")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(colors.textSecondary)
                            TextField("https://example.com", text: $newUrl)
                                .padding(10)
                                .background(colors.surfaceElevated)
                                .cornerRadius(8)
                                .autocapitalization(.none)
                        }

                        Spacer()
                    }
                    .padding(20)
                    .background(colors.background.ignoresSafeArea())
                    .navigationTitle("Add Bookmark")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button("Cancel") { showAddSheet = false }
                        }
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button("Save") {
                                if !newUrl.isEmpty {
                                    state.addBookmark(title: newTitle.isEmpty ? newUrl : newTitle, url: newUrl)
                                    showAddSheet = false
                                }
                            }
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(colors.statusAccent)
                        }
                    }
                }
                .presentationDetents([.fraction(0.45)])
            }
        }
    }
}

