import SwiftUI

// MARK: - Search Engine Dropdown Component (1:1 Android SearchEngineIconDropdown.kt Parity)
public struct SearchEngineDropdown: View {
    @ObservedObject var state: BrowserState
    var iconSize: CGFloat = 20
    var showDropdownArrow: Bool = true

    let colors: AzokleColors = .dark

    public init(state: BrowserState, iconSize: CGFloat = 20, showDropdownArrow: Bool = true) {
        self.state = state
        self.iconSize = iconSize
        self.showDropdownArrow = showDropdownArrow
    }

    public var body: some View {
        Menu {
            ForEach(SearchEngine.allCases) { engine in
                Button(action: {
                    let generator = UISelectionFeedbackGenerator()
                    generator.selectionChanged()
                    state.selectedSearchEngine = engine
                }) {
                    HStack {
                        Text(engine.name)
                        if state.selectedSearchEngine == engine {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
        } label: {
            HStack(spacing: 3) {
                Image(state.selectedSearchEngine.assetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize)
                    .clipShape(Circle())

                if showDropdownArrow {
                    Image(systemName: "chevron.down")
                        .font(.system(size: iconSize * 0.45, weight: .bold))
                        .foregroundColor(colors.textMuted)
                }
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 4)
            .background(colors.surfaceElevated.opacity(0.6))
            .cornerRadius(AzokleRadius.small)
            .contentShape(Rectangle())
        }
        .accessibilityLabel("Current search engine: \(state.selectedSearchEngine.name). Tap to change.")
    }
}
