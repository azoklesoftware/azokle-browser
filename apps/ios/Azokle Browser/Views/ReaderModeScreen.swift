import SwiftUI

public enum ReaderThemeMode: String, CaseIterable, Identifiable {
    case light = "Light"
    case sepia = "Sepia"
    case dark = "Dark"

    public var id: String { rawValue }

    public var background: Color {
        switch self {
        case .light: return Color(hex: 0xFFFFFF)
        case .sepia: return Color(hex: 0xFBF0D9)
        case .dark: return Color(hex: 0x0A0A0E)
        }
    }

    public var textPrimary: Color {
        switch self {
        case .light: return Color(hex: 0x1F2328)
        case .sepia: return Color(hex: 0x433422)
        case .dark: return Color(hex: 0xEDEDED)
        }
    }

    public var textSecondary: Color {
        switch self {
        case .light: return Color(hex: 0x57606A)
        case .sepia: return Color(hex: 0x6E5A44)
        case .dark: return Color(hex: 0x8E8E93)
        }
    }

    public var cardBackground: Color {
        switch self {
        case .light: return Color(hex: 0xF6F8FA)
        case .sepia: return Color(hex: 0xF4E4C1)
        case .dark: return Color(hex: 0x14141A)
        }
    }

    public var border: Color {
        switch self {
        case .light: return Color(hex: 0xD0D7DE)
        case .sepia: return Color(hex: 0xDECBA4)
        case .dark: return Color(hex: 0x27272A)
        }
    }
}

public enum ReaderFontFamily: String, CaseIterable, Identifiable {
    case serif = "Serif"
    case sans = "Sans"
    case mono = "Mono"

    public var id: String { rawValue }

    public func font(size: CGFloat) -> Font {
        switch self {
        case .serif: return .system(size: size, design: .serif)
        case .sans: return .system(size: size, design: .default)
        case .mono: return .system(size: size, design: .monospaced)
        }
    }
}

public struct ReaderModeScreen: View {
    let article: ArticleContent
    @Binding var isPresented: Bool

    @State private var theme: ReaderThemeMode = .dark
    @State private var fontSize: CGFloat = 17
    @State private var selectedFont: ReaderFontFamily = .serif
    @State private var showSettingsMenu: Bool = false

    public var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // Reader Top Control Bar
                HStack {
                    Button(action: {
                        isPresented = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(theme.textPrimary)
                            .padding(8)
                            .background(theme.cardBackground)
                            .clipShape(Circle())
                    }

                    Spacer()

                    // Reading Time & Domain
                    HStack(spacing: 6) {
                        Image(systemName: "clock")
                            .font(.system(size: 11))
                        Text("\(article.estimatedReadingTimeMinutes) min read")
                            .font(.system(size: 12, weight: .medium))

                        if !article.domain.isEmpty {
                            Text("•")
                            Text(article.domain)
                                .font(.system(size: 12, weight: .semibold))
                        }
                    }
                    .foregroundColor(theme.textSecondary)

                    Spacer()

                    // Typography & Appearance Menu
                    Button(action: {
                        withAnimation {
                            showSettingsMenu.toggle()
                        }
                    }) {
                        Image(systemName: "textformat.size")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(theme.textPrimary)
                            .padding(8)
                            .background(theme.cardBackground)
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(theme.cardBackground)
                .overlay(
                    Rectangle()
                        .frame(height: 1)
                        .foregroundColor(theme.border),
                    alignment: .bottom
                )

                // Appearance Settings Drawer
                if showSettingsMenu {
                    VStack(spacing: 12) {
                        // Theme Switcher
                        HStack(spacing: 8) {
                            ForEach(ReaderThemeMode.allCases) { item in
                                Button(action: {
                                    theme = item
                                }) {
                                    HStack {
                                        Circle()
                                            .fill(item.background)
                                            .frame(width: 16, height: 16)
                                            .overlay(Circle().stroke(Color.gray, lineWidth: 0.5))
                                        Text(item.rawValue)
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(item.textPrimary)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 6)
                                    .background(theme == item ? item.cardBackground : Color.clear)
                                    .cornerRadius(8)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(theme == item ? Color.blue : theme.border, lineWidth: 1)
                                    )
                                }
                            }
                        }

                        // Font Style & Size
                        HStack(spacing: 14) {
                            // Font Style Picker
                            Picker("Font", selection: $selectedFont) {
                                ForEach(ReaderFontFamily.allCases) { family in
                                    Text(family.rawValue).tag(family)
                                }
                            }
                            .pickerStyle(SegmentedPickerStyle())

                            // Font Size Controls
                            HStack(spacing: 12) {
                                Button(action: {
                                    if fontSize > 13 { fontSize -= 1 }
                                }) {
                                    Image(systemName: "textformat.size.smaller")
                                        .font(.system(size: 14))
                                        .foregroundColor(theme.textPrimary)
                                }

                                Text("\(Int(fontSize))")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(theme.textPrimary)

                                Button(action: {
                                    if fontSize < 26 { fontSize += 1 }
                                }) {
                                    Image(systemName: "textformat.size.larger")
                                        .font(.system(size: 14))
                                        .foregroundColor(theme.textPrimary)
                                }
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(theme.cardBackground)
                            .cornerRadius(8)
                        }
                    }
                    .padding(12)
                    .background(theme.cardBackground)
                    .overlay(
                        Rectangle()
                            .frame(height: 1)
                            .foregroundColor(theme.border),
                        alignment: .bottom
                    )
                }

                // Article Content Scroll View
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        // Title
                        Text(article.title)
                            .font(selectedFont.font(size: fontSize + 8))
                            .fontWeight(.bold)
                            .foregroundColor(theme.textPrimary)
                            .lineSpacing(4)

                        // Byline
                        if !article.byline.isEmpty {
                            Text(article.byline)
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(theme.textSecondary)
                        }

                        Divider().background(theme.border)

                        // Paragraphs
                        ForEach(0..<article.paragraphs.count, id: \.self) { idx in
                            Text(article.paragraphs[idx])
                                .font(selectedFont.font(size: fontSize))
                                .foregroundColor(theme.textPrimary)
                                .lineSpacing(6)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 24)
                }
            }
        }
    }
}
