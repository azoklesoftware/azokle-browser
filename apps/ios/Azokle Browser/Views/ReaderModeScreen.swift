import SwiftUI
import AVFoundation

public enum ReaderThemeMode: String, CaseIterable, Identifiable {
    case light = "Light"
    case sepia = "Sepia"
    case solarized = "Solarized"
    case oled = "OLED"
    case cyberpunk = "Cyberpunk"

    public var id: String { rawValue }

    public var background: Color {
        switch self {
        case .light: return Color(hex: 0xFFFFFF)
        case .sepia: return Color(hex: 0xFBF0D9)
        case .solarized: return Color(hex: 0x002B36)
        case .oled: return Color(hex: 0x000000)
        case .cyberpunk: return Color(hex: 0x0A0012)
        }
    }

    public var textPrimary: Color {
        switch self {
        case .light: return Color(hex: 0x1F2328)
        case .sepia: return Color(hex: 0x433422)
        case .solarized: return Color(hex: 0x93A1A1)
        case .oled: return Color(hex: 0xFFFFFF)
        case .cyberpunk: return Color(hex: 0x00FFCC)
        }
    }

    public var textSecondary: Color {
        switch self {
        case .light: return Color(hex: 0x57606A)
        case .sepia: return Color(hex: 0x6E5A44)
        case .solarized: return Color(hex: 0x586E75)
        case .oled: return Color(hex: 0x8E8E93)
        case .cyberpunk: return Color(hex: 0xFF007F)
        }
    }

    public var cardBackground: Color {
        switch self {
        case .light: return Color(hex: 0xF6F8FA)
        case .sepia: return Color(hex: 0xF4E4C1)
        case .solarized: return Color(hex: 0x073642)
        case .oled: return Color(hex: 0x121212)
        case .cyberpunk: return Color(hex: 0x180026)
        }
    }

    public var border: Color {
        switch self {
        case .light: return Color(hex: 0xD0D7DE)
        case .sepia: return Color(hex: 0xDECBA4)
        case .solarized: return Color(hex: 0x586E75)
        case .oled: return Color(hex: 0x27272A)
        case .cyberpunk: return Color(hex: 0x3D005E)
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

// MARK: - Reader Speech Synthesizer Manager
public final class ReaderSpeechManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    @Published public var isSpeaking: Bool = false
    private let synthesizer = AVSpeechSynthesizer()

    public override init() {
        super.init()
        synthesizer.delegate = self
    }

    public func speak(paragraphs: [String]) {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        let fullText = paragraphs.joined(separator: "\n\n")
        let utterance = AVSpeechUtterance(string: fullText)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        DispatchQueue.main.async {
            self.isSpeaking = true
        }
        synthesizer.speak(utterance)
    }

    public func stop() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }

    public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }
}

public struct ReaderModeScreen: View {
    let article: ArticleContent
    @Binding var isPresented: Bool

    @State private var theme: ReaderThemeMode = .oled
    @State private var fontSize: CGFloat = 17
    @State private var selectedFont: ReaderFontFamily = .serif
    @State private var showSettingsMenu: Bool = false

    // Text to speech manager with delegate lifecycle
    @StateObject private var speechManager = ReaderSpeechManager()
    @State private var currentParagraphIndex: Int = 0

    public var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // Reader Top Control Bar
                HStack {
                    Button(action: {
                        speechManager.stop()
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

                    // Text-To-Speech (TTS) Button
                    Button(action: {
                        if speechManager.isSpeaking {
                            speechManager.stop()
                        } else {
                            speechManager.speak(paragraphs: article.paragraphs)
                        }
                    }) {
                        Image(systemName: speechManager.isSpeaking ? "speaker.wave.3.fill" : "speaker.wave.2")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(speechManager.isSpeaking ? Color.green : theme.textPrimary)
                            .padding(8)
                            .background(theme.cardBackground)
                            .clipShape(Circle())
                    }

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
                        // Theme Switcher (5 Themes)
                        HStack(spacing: 6) {
                            ForEach(ReaderThemeMode.allCases) { item in
                                Button(action: {
                                    theme = item
                                }) {
                                    HStack(spacing: 4) {
                                        Circle()
                                            .fill(item.background)
                                            .frame(width: 14, height: 14)
                                            .overlay(Circle().stroke(Color.gray, lineWidth: 0.5))
                                        Text(item.rawValue)
                                            .font(.system(size: 11, weight: .semibold))
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
                                .padding(6)
                                .background(speechManager.isSpeaking && currentParagraphIndex == idx ? theme.textSecondary.opacity(0.18) : Color.clear)
                                .cornerRadius(6)
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.vertical, 24)
                }
            }
        }
        .onDisappear {
            speechManager.stop()
        }
    }
}
