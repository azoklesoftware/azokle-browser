import SwiftUI

public struct HistoryScreen: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool
    @State private var searchText: String = ""

    let colors: AzokleColors = .dark

    private var filteredHistory: [HistoryItem] {
        if searchText.isEmpty {
            return state.history
        } else {
            return state.history.filter {
                $0.title.localizedCaseInsensitiveContains(searchText) ||
                $0.url.localizedCaseInsensitiveContains(searchText)
            }
        }
    }

    public var body: some View {
        NavigationView {
            ZStack {
                colors.background.ignoresSafeArea()

                VStack(spacing: 0) {
                    // Search Bar
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(colors.textMuted)
                        TextField("Search history...", text: $searchText)
                            .foregroundColor(colors.textPrimary)
                    }
                    .padding(10)
                    .background(colors.surfaceElevated)
                    .cornerRadius(AzokleRadius.medium)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)

                    Divider()
                        .background(colors.border)

                    if filteredHistory.isEmpty {
                        VStack(spacing: 12) {
                            Spacer()
                            Image(systemName: "clock")
                                .font(.system(size: 44))
                                .foregroundColor(colors.textMuted)
                            Text("No browsing history")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(colors.textSecondary)
                            Spacer()
                        }
                    } else {
                        List {
                            ForEach(filteredHistory) { item in
                                Button(action: {
                                    state.navigateActiveTab(to: item.url)
                                    isPresented = false
                                }) {
                                    HStack(spacing: 12) {
                                        Image(systemName: "globe")
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
                                state.history.remove(atOffsets: indexSet)
                            }
                        }
                        .listStyle(.plain)
                        .background(colors.background)
                    }
                }
            }
            .navigationTitle("History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    if !state.history.isEmpty {
                        Button(action: {
                            state.history.removeAll()
                        }) {
                            Text("Clear All")
                                .font(.system(size: 14))
                                .foregroundColor(colors.statusDestructive)
                        }
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
