import SwiftUI

// MARK: - Search Bar Capsule (1:1 Android SearchBarCapsule.kt Parity)
public struct SearchBarCapsule: View {
    @ObservedObject var state: BrowserState
    var placeholderText: String = "Search or enter address"
    var onCapsuleClick: (() -> Void)? = nil
    var onVoiceClick: (() -> Void)? = nil

    let colors: AzokleColors = .dark
    @State private var isPressed: Bool = false

    public init(
        state: BrowserState,
        placeholderText: String = "Search or enter address",
        onCapsuleClick: (() -> Void)? = nil,
        onVoiceClick: (() -> Void)? = nil
    ) {
        self.state = state
        self.placeholderText = placeholderText
        self.onCapsuleClick = onCapsuleClick
        self.onVoiceClick = onVoiceClick
    }

    public var body: some View {
        HStack(spacing: 8) {
            // 1. Search Engine Dropdown Menu (Direct Tap Selector)
            SearchEngineDropdown(
                state: state,
                iconSize: 20,
                showDropdownArrow: true
            )
            .padding(.leading, 2)

            // 2. Main Query Input / Placeholder Trigger Area
            Button(action: {
                triggerLightHaptic()
                if let onCapsuleClick = onCapsuleClick {
                    onCapsuleClick()
                } else {
                    state.showOmniboxSheet = true
                }
            }) {
                HStack {
                    Text(placeholderText)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(colors.textSecondary)
                        .lineLimit(1)
                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(PlainButtonStyle())

            // 3. Subtle Vertical Divider
            Rectangle()
                .fill(colors.border.opacity(0.8))
                .frame(width: 1, height: 18)
                .padding(.horizontal, 2)

            // 4. Voice Search Microphone Action
            Button(action: {
                triggerLightHaptic()
                if let onVoiceClick = onVoiceClick {
                    onVoiceClick()
                } else {
                    state.showVoiceSearch = true
                }
            }) {
                Image(systemName: "mic.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(colors.textPrimary)
                    .frame(width: 36, height: 36)
                    .background(colors.surfaceElevated.opacity(0.4))
                    .clipShape(Circle())
            }
            .buttonStyle(PlainButtonStyle())
            .accessibilityLabel("Voice search")
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(colors.surfaceElevated.opacity(0.92))
        .cornerRadius(AzokleRadius.pill)
        .overlay(
            RoundedRectangle(cornerRadius: AzokleRadius.pill)
                .stroke(colors.borderLight, lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.35), radius: 10, x: 0, y: 5)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Search Bar Capsule")
    }

    private func triggerLightHaptic() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }
}
