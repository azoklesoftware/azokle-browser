import SwiftUI

// MARK: - Choose Search Engine Sheet (1:1 Android ChooseSearchEngineSheet.kt Parity)
public struct ChooseSearchEngineSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

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
                    Text("Search Engine")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Choose your sovereign search provider")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                Button(action: {
                    isPresented = false
                }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(colors.textSecondary)
                        .frame(width: 32, height: 32)
                        .background(colors.surfaceElevated)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(colors.border, lineWidth: 1))
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 14)

            Divider().background(colors.border)

            ScrollView {
                VStack(spacing: 12) {
                    ForEach(SearchEngine.allCases) { engine in
                        engineRow(engine)
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.68), .large])
    }

    private func engineRow(_ engine: SearchEngine) -> some View {
        let isSelected = state.selectedSearchEngine == engine

        return Button(action: {
            state.selectedSearchEngine = engine
            let gen = UIImpactFeedbackGenerator(style: .light)
            gen.impactOccurred()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                isPresented = false
            }
        }) {
            HStack(spacing: 14) {
                // Engine Logo
                Image(engine.assetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 36, height: 36)
                    .background(colors.surfaceElevated)
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 8) {
                        Text(engine.name)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(colors.textPrimary)

                        Text(engine.domain)
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(colors.textMuted)
                    }

                    Text(engine.subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                        .lineLimit(2)
                }

                Spacer()

                // Radio Checkmark
                ZStack {
                    Circle()
                        .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: 2)
                        .frame(width: 22, height: 22)

                    if isSelected {
                        Circle()
                            .fill(colors.statusAccent)
                            .frame(width: 12, height: 12)
                    }
                }
            }
            .padding(14)
            .background(colors.surface)
            .cornerRadius(AzokleRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: isSelected ? 1.5 : 1)
            )
        }
    }
}
