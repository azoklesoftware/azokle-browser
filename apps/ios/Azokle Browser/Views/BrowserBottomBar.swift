import SwiftUI

public struct BrowserBottomBar: View {
    @ObservedObject var state: BrowserState
    let colors: AzokleColors = .dark

    public var body: some View {
        VStack(spacing: 0) {
            Divider()
                .background(colors.border)

            HStack {
                // 1. Home
                Button(action: {
                    state.navigateActiveTab(to: "https://search.azokle.com")
                }) {
                    VStack(spacing: 3) {
                        Image(systemName: "house")
                            .font(.system(size: 20, weight: .regular))
                            .foregroundColor(colors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                }

                // 2. Discover / Explore
                Button(action: {
                    state.showDiscoverScreen = true
                }) {
                    VStack(spacing: 3) {
                        Image(systemName: "safari")
                            .font(.system(size: 20, weight: .regular))
                            .foregroundColor(colors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                }

                // 3. Active Tabs Counter Badge
                Button(action: {
                    state.showTabsGrid = true
                }) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(colors.textSecondary, lineWidth: 1.8)
                            .frame(width: 22, height: 22)

                        Text("\(state.tabs.count)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(colors.textPrimary)
                    }
                    .frame(maxWidth: .infinity)
                }

                // 4. Fire / Burn Data (Incinerate)
                Button(action: {
                    state.showBurnConfirmation = true
                }) {
                    VStack(spacing: 3) {
                        Image(systemName: "flame.fill")
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundColor(colors.statusFire)
                    }
                    .frame(maxWidth: .infinity)
                }

                // 5. 3-Dots Menu
                Button(action: {
                    state.showMenuSheet = true
                }) {
                    VStack(spacing: 3) {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 20, weight: .regular))
                            .foregroundColor(colors.textSecondary)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 50)
            .padding(.horizontal, 8)
            .background(colors.bottomNavBg)
        }
    }
}
