import SwiftUI

// MARK: - Language Model
public struct LanguageOption: Identifiable, Hashable {
    public let code: String
    public let name: String
    public let nativeName: String

    public var id: String { code }
}

// MARK: - Choose Language Sheet (1:1 Android ChooseLanguageSheet.kt Parity)
public struct ChooseLanguageSheet: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var searchQuery: String = ""

    let colors: AzokleColors = .dark

    let allLanguages: [LanguageOption] = [
        LanguageOption(code: "en", name: "English", nativeName: "English"),
        LanguageOption(code: "es", name: "Spanish", nativeName: "Español"),
        LanguageOption(code: "fr", name: "French", nativeName: "Français"),
        LanguageOption(code: "de", name: "German", nativeName: "Deutsch"),
        LanguageOption(code: "ja", name: "Japanese", nativeName: "日本語"),
        LanguageOption(code: "zh", name: "Chinese (Simplified)", nativeName: "简体中文"),
        LanguageOption(code: "ru", name: "Russian", nativeName: "Русский"),
        LanguageOption(code: "ar", name: "Arabic", nativeName: "العربية"),
        LanguageOption(code: "pt", name: "Portuguese", nativeName: "Português"),
        LanguageOption(code: "it", name: "Italian", nativeName: "Italiano"),
        LanguageOption(code: "ko", name: "Korean", nativeName: "한국어"),
        LanguageOption(code: "nl", name: "Dutch", nativeName: "Nederlands"),
        LanguageOption(code: "tr", name: "Turkish", nativeName: "Türkçe"),
        LanguageOption(code: "pl", name: "Polish", nativeName: "Polski"),
        LanguageOption(code: "uk", name: "Ukrainian", nativeName: "Українська"),
        LanguageOption(code: "hi", name: "Hindi", nativeName: "हिन्दी"),
        LanguageOption(code: "id", name: "Indonesian", nativeName: "Bahasa Indonesia"),
        LanguageOption(code: "sv", name: "Swedish", nativeName: "Svenska")
    ]

    private var filteredLanguages: [LanguageOption] {
        if searchQuery.isEmpty {
            return allLanguages
        }
        return allLanguages.filter {
            $0.name.localizedCaseInsensitiveContains(searchQuery) ||
            $0.nativeName.localizedCaseInsensitiveContains(searchQuery) ||
            $0.code.localizedCaseInsensitiveContains(searchQuery)
        }
    }

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
                    Text("Select Language")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(colors.textPrimary)
                    Text("Choose browser display & Accept-Language header")
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
            .padding(.bottom, 12)

            // Search Bar
            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(colors.textMuted)

                TextField("Search languages...", text: $searchQuery)
                    .font(.system(size: 14))
                    .foregroundColor(colors.textPrimary)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)

                if !searchQuery.isEmpty {
                    Button(action: { searchQuery = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(colors.textMuted)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(colors.surfaceElevated)
            .cornerRadius(AzokleRadius.medium)
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            Divider().background(colors.border)

            // Languages List
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(filteredLanguages) { lang in
                        languageRow(lang)
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .presentationDetents([.fraction(0.72), .large])
    }

    private func languageRow(_ lang: LanguageOption) -> some View {
        let isSelected = state.selectedLanguageCode == lang.code

        return Button(action: {
            state.selectedLanguageCode = lang.code
            let gen = UIImpactFeedbackGenerator(style: .light)
            gen.impactOccurred()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.18) {
                isPresented = false
            }
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(lang.nativeName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.textPrimary)

                    Text("\(lang.name) • \(lang.code.uppercased())")
                        .font(.system(size: 12))
                        .foregroundColor(colors.textSecondary)
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 18))
                        .foregroundColor(colors.statusAccent)
                }
            }
            .padding(14)
            .background(colors.surface)
            .cornerRadius(AzokleRadius.medium)
            .overlay(
                RoundedRectangle(cornerRadius: AzokleRadius.medium)
                    .stroke(isSelected ? colors.statusAccent : colors.border, lineWidth: 1)
            )
        }
    }
}
