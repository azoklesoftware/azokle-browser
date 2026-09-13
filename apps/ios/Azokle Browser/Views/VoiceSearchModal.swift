import SwiftUI

// MARK: - Voice Search Modal (1:1 Android VoiceSearchModal.kt Parity)
public struct VoiceSearchModal: View {
    @ObservedObject var state: BrowserState
    @Binding var isPresented: Bool

    @State private var waveScale1: CGFloat = 1.0
    @State private var waveScale2: CGFloat = 1.0
    @State private var statusText: String = "Listening..."
    @State private var recognizedText: String = ""
    @State private var isListening: Bool = true

    let colors: AzokleColors = .dark

    let samplePrompts = [
        "What is quantum computing?",
        "Latest tech privacy news",
        "How does Tor encryption work?",
        "Decentralized AI search"
    ]

    public var body: some View {
        ZStack {
            Color.black.opacity(0.85)
                .ignoresSafeArea()
                .onTapGesture {
                    isPresented = false
                }

            VStack(spacing: 28) {
                // Top Close Bar
                HStack {
                    Spacer()
                    Button(action: {
                        isPresented = false
                    }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(colors.textSecondary)
                            .frame(width: 36, height: 36)
                            .background(colors.surfaceElevated)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(colors.border, lineWidth: 1))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)

                Spacer()

                // Animated Concentric Pulsing Rings & Microphone
                ZStack {
                    // Outer Ring
                    Circle()
                        .stroke(colors.statusAccent.opacity(0.2), lineWidth: 2)
                        .frame(width: 140, height: 140)
                        .scaleEffect(waveScale2)
                        .opacity(2.0 - Double(waveScale2))

                    // Middle Ring
                    Circle()
                        .stroke(colors.statusAccent.opacity(0.4), lineWidth: 3)
                        .frame(width: 100, height: 100)
                        .scaleEffect(waveScale1)
                        .opacity(1.8 - Double(waveScale1))

                    // Center Microphone Core
                    Circle()
                        .fill(colors.actionPrimaryBg)
                        .frame(width: 76, height: 76)
                        .shadow(color: colors.statusAccent.opacity(0.5), radius: 16)

                    Image(systemName: "mic.fill")
                        .font(.system(size: 32, weight: .semibold))
                        .foregroundColor(colors.actionPrimaryText)
                }
                .padding(.vertical, 16)

                // Status & Recognized Query
                VStack(spacing: 10) {
                    Text(statusText)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(colors.statusAccent)

                    if !recognizedText.isEmpty {
                        Text("\"\(recognizedText)\"")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(colors.textPrimary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                    } else {
                        Text("Speak clearly to search sovereign web")
                            .font(.system(size: 13))
                            .foregroundColor(colors.textSecondary)
                    }
                }

                // Sample Query Pill Shortcuts
                VStack(spacing: 8) {
                    Text("TRY ASKING:")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(colors.textMuted)

                    VStack(spacing: 6) {
                        ForEach(samplePrompts, id: \.self) { prompt in
                            Button(action: {
                                submitQuery(prompt)
                            }) {
                                Text(prompt)
                                    .font(.system(size: 13, weight: .medium))
                                    .foregroundColor(colors.textPrimary)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                                    .background(colors.surfaceElevated.opacity(0.8))
                                    .cornerRadius(AzokleRadius.pill)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: AzokleRadius.pill)
                                            .stroke(colors.border, lineWidth: 1)
                                    )
                            }
                        }
                    }
                }

                Spacer()
            }
        }
        .onAppear {
            startAudioAnimation()
        }
    }

    private func startAudioAnimation() {
        withAnimation(
            Animation.easeInOut(duration: 1.2)
                .repeatForever(autoreverses: true)
        ) {
            waveScale1 = 1.35
        }

        withAnimation(
            Animation.easeInOut(duration: 1.6)
                .repeatForever(autoreverses: true)
                .delay(0.3)
        ) {
            waveScale2 = 1.6
        }

        // Simulate voice recognition prompt after 1.8s
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
            if isListening && recognizedText.isEmpty {
                withAnimation {
                    statusText = "Transcribing..."
                    recognizedText = "Azokle Sovereign Search"
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                    submitQuery(recognizedText)
                }
            }
        }
    }

    private func submitQuery(_ query: String) {
        let gen = UIImpactFeedbackGenerator(style: .medium)
        gen.impactOccurred()
        state.navigateActiveTab(to: query)
        isPresented = false
    }
}
