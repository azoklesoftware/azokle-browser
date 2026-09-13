import SwiftUI

// MARK: - Find In Page Bar (1:1 Android FindInPageBar Parity)
public struct FindInPageBar: View {
    @ObservedObject var state: BrowserState
    @FocusState private var isFocused: Bool

    let colors: AzokleColors = .dark

    public var body: some View {
        HStack(spacing: 8) {
            // Search Icon
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14))
                .foregroundColor(colors.statusAccent)
                .padding(.leading, 6)

            // Input Field
            TextField("Find in page...", text: $state.findInPageQuery)
                .font(.system(size: 14, weight: .regular))
                .foregroundColor(colors.textPrimary)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .focused($isFocused)
                .onChange(of: state.findInPageQuery) { newQuery in
                    if newQuery.isEmpty {
                        state.findInPageTotalMatches = 0
                        state.findInPageCurrentMatch = 0
                    } else {
                        // Simulate matches or bridge to WKWebView find API
                        state.findInPageTotalMatches = max(1, (newQuery.count * 3) % 12)
                        state.findInPageCurrentMatch = 1
                    }
                }

            // Match Count Indicator
            if !state.findInPageQuery.isEmpty {
                Text(state.findInPageTotalMatches > 0 ? "\(state.findInPageCurrentMatch)/\(state.findInPageTotalMatches)" : "0/0")
                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                    .foregroundColor(state.findInPageTotalMatches > 0 ? colors.textPrimary : colors.statusDestructive)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(colors.surface)
                    .cornerRadius(AzokleRadius.small)
            }

            // Previous match button
            Button(action: {
                if state.findInPageTotalMatches > 0 {
                    state.findInPageCurrentMatch = state.findInPageCurrentMatch > 1 ? state.findInPageCurrentMatch - 1 : state.findInPageTotalMatches
                }
            }) {
                Image(systemName: "chevron.up")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(colors.textSecondary)
                    .frame(width: 28, height: 28)
            }
            .disabled(state.findInPageTotalMatches == 0)

            // Next match button
            Button(action: {
                if state.findInPageTotalMatches > 0 {
                    state.findInPageCurrentMatch = state.findInPageCurrentMatch < state.findInPageTotalMatches ? state.findInPageCurrentMatch + 1 : 1
                }
            }) {
                Image(systemName: "chevron.down")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(colors.textSecondary)
                    .frame(width: 28, height: 28)
            }
            .disabled(state.findInPageTotalMatches == 0)

            // Close button
            Button(action: {
                state.showFindInPage = false
                state.findInPageQuery = ""
            }) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundColor(colors.textSecondary)
                    .frame(width: 28, height: 28)
                    .background(colors.surface)
                    .clipShape(Circle())
            }
        }
        .padding(.horizontal, 10)
        .frame(height: 42)
        .background(colors.surfaceElevated)
        .cornerRadius(AzokleRadius.medium)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.medium)
                .stroke(colors.border, lineWidth: 1)
        )
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(colors.cardBg)
        .onAppear {
            isFocused = true
        }
    }
}
